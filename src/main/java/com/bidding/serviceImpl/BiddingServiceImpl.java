package com.bidding.serviceImpl;

import com.bidding.config.AuctionWebSocketHandler;
import com.bidding.dto.responce.BidResponseDTO;
import com.bidding.dto.responce.DealerBidResponseDTO;
import com.bidding.entity.Bid;
import com.bidding.entity.Dealer;
import com.bidding.entity.Inspection;
import com.bidding.entity.Vehicle;
import com.bidding.exception.ResourceNotFoundException;
import com.bidding.repo.BidRepository;
import com.bidding.repo.DealerRepository;
import com.bidding.repo.InspectionRepository;
import com.bidding.repo.VehicleRepository;
import com.bidding.service.BiddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiddingServiceImpl implements BiddingService {

    private final BidRepository bidRepository;
    private final VehicleRepository vehicleRepository;
    private final InspectionRepository inspectionRepository;
    private final DealerRepository dealerRepository;
    private final AuctionWebSocketHandler webSocketHandler;
    private final com.bidding.service.NotificationService notificationService;

    @Override
    @Transactional
    public void placeBid(Long inspectionId, String dealerEmail, Double amount) {
        log.info("Received bid request. InspectionId: {}, Dealer: {}, Amount: {}", inspectionId, dealerEmail, amount);

        // 1. Find inspection
        Inspection ins = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        Vehicle v = ins.getVehicle();
        if (v == null) {
            throw new ResourceNotFoundException("Vehicle not found for this inspection");
        }

        // 2. Lock the vehicle row pessimistic write lock
        Vehicle lockedVehicle = vehicleRepository.findByIdForUpdate(v.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle row could not be locked"));

        // 3. Validate auction is live
        if (!"LIVE".equalsIgnoreCase(lockedVehicle.getVehicleStatus())) {
            log.warn("Bid rejected: Auction is not live. Status: {}", lockedVehicle.getVehicleStatus());
            throw new IllegalStateException("Auction is not live");
        }

        // 4. Validate expiry
        if (lockedVehicle.getAuctionEndTime() != null && LocalDateTime.now().isAfter(lockedVehicle.getAuctionEndTime())) {
            log.warn("Bid rejected: Auction has expired. EndTime: {}", lockedVehicle.getAuctionEndTime());
            throw new IllegalStateException("Auction has already expired");
        }

        // 5. Find dealer placing the bid
        Dealer dealer = dealerRepository.findByEmail(dealerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found"));

        Dealer previousHighestBidder = lockedVehicle.getCurrentHighestBidder();

        // 6. Validate bid amount
        double currentBid = (lockedVehicle.getCurrentHighestBid() != null) ? lockedVehicle.getCurrentHighestBid() : 0.0;
        double baseValuation = (lockedVehicle.getSuggestedPrice() != null) ? lockedVehicle.getSuggestedPrice() : 0.0;
        double minimumBid = currentBid > 0 ? currentBid : baseValuation;

        if (amount <= minimumBid) {
            log.warn("Bid rejected: Amount {} is lower than or equal to minimum required {}", amount, minimumBid);
            throw new IllegalArgumentException("Bid amount must exceed the current highest bid");
        }

        // 7. Save bid
        Bid bid = Bid.builder()
                .inspection(ins)
                .dealer(dealer)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
        bidRepository.save(bid);

        // 8. Update vehicle highest bid stats
        lockedVehicle.setCurrentHighestBid(amount);
        lockedVehicle.setCurrentHighestBidder(dealer);
        lockedVehicle.setTotalBids((lockedVehicle.getTotalBids() == null ? 0 : lockedVehicle.getTotalBids()) + 1);
        vehicleRepository.save(lockedVehicle);

        log.info("Bid successfully saved. New highest bid: {} by Dealer: {}", amount, dealer.getDealershipName());

        // Create Notifications
        String vehicleTitle = String.format("%s %s (%s)", lockedVehicle.getBrand(), lockedVehicle.getModel(), lockedVehicle.getVehicleNumber());
        String dealerName = dealer.getDealershipName() != null ? dealer.getDealershipName() : dealer.getOwnerName();

        // Admin Notification
        notificationService.createNotification(
                "ADMIN",
                null,
                inspectionId,
                "🚨 New Bid Placed: ₹" + String.format("%,.0f", amount),
                "Dealer " + dealerName + " placed a bid of ₹" + String.format("%,.0f", amount) + " on " + vehicleTitle + ".",
                "BID_PLACED"
        );

        // Dealer Notification
        notificationService.createNotification(
                "DEALER",
                dealer.getEmail(),
                inspectionId,
                "✅ Bid Confirmed: ₹" + String.format("%,.0f", amount),
                "Your bid of ₹" + String.format("%,.0f", amount) + " for " + vehicleTitle + " has been successfully submitted.",
                "BID_PLACED"
        );

        // Outbid Alert Notification for Previous Bidder
        if (previousHighestBidder != null && !previousHighestBidder.getId().equals(dealer.getId())) {
            notificationService.createNotification(
                    "DEALER",
                    previousHighestBidder.getEmail(),
                    inspectionId,
                    "⚡ Outbid Alert: " + vehicleTitle,
                    "Another dealer placed a higher bid of ₹" + String.format("%,.0f", amount) + " on " + vehicleTitle + ". Outbid now to reclaim highest bidder status!",
                    "OUTBID"
            );
        }

        // 9. Fetch updated bid history and broadcast
        List<BidResponseDTO> history = getBidHistory(inspectionId);
        
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "BID_UPDATE");
        wsMessage.put("inspectionId", inspectionId);
        wsMessage.put("currentHighestBid", amount);
        wsMessage.put("currentHighestBidder", dealer.getDealershipName());
        wsMessage.put("totalBids", lockedVehicle.getTotalBids());
        wsMessage.put("auctionEndTime", lockedVehicle.getAuctionEndTime() != null 
                ? lockedVehicle.getAuctionEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() 
                : null);
        wsMessage.put("bidHistory", history);

        webSocketHandler.broadcast(inspectionId, wsMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponseDTO> getBidHistory(Long inspectionId) {
        return bidRepository.findByInspectionIdOrderByAmountDesc(inspectionId).stream()
                .map(b -> BidResponseDTO.builder()
                        .dealer(b.getDealer().getDealershipName())
                        .amount(b.getAmount())
                        .time(formatTime(b.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealerBidResponseDTO> getDealerBidHistory(String dealerEmail) {
        List<Bid> bids = bidRepository.findByDealerEmailOrderByCreatedAtDesc(dealerEmail);
        
        Map<Long, Bid> highestBidPerInspection = new HashMap<>();
        for (Bid b : bids) {
            Long inspectionId = b.getInspection().getId();
            if (!highestBidPerInspection.containsKey(inspectionId) || 
                b.getAmount() > highestBidPerInspection.get(inspectionId).getAmount()) {
                highestBidPerInspection.put(inspectionId, b);
            }
        }

        return highestBidPerInspection.values().stream()
                .map(b -> {
                    Inspection ins = b.getInspection();
                    Vehicle v = ins.getVehicle();
                    
                    String statusStr = v != null ? v.getVehicleStatus() : "N/A";
                    String auctionStr = "completed";
                    if ("LIVE".equalsIgnoreCase(statusStr)) {
                        auctionStr = "live";
                    } else if ("UPCOMING".equalsIgnoreCase(statusStr) || "READY_FOR_AUCTION".equalsIgnoreCase(statusStr) || "PENDING".equalsIgnoreCase(statusStr)) {
                        auctionStr = "scheduled";
                    }

                    return DealerBidResponseDTO.builder()
                            .id(String.valueOf(b.getId()))
                            .vehicleId(String.valueOf(ins.getId()))
                            .brand(v != null ? v.getBrand() : "N/A")
                            .model(v != null ? v.getModel() : "N/A")
                            .regNo(v != null ? v.getVehicleNumber() : "N/A")
                            .myBid(b.getAmount())
                            .highestBid(v != null ? (v.getCurrentHighestBid() != null && v.getCurrentHighestBid() > 0.0 ? v.getCurrentHighestBid() : bidRepository.findFirstByInspectionIdOrderByAmountDesc(ins.getId()).map(Bid::getAmount).orElse(v.getSuggestedPrice() != null ? v.getSuggestedPrice() : 0.0)) : 0.0)
                            .totalBids(v != null ? v.getTotalBids() : 0)
                            .timestamp(formatTime(b.getCreatedAt()))
                            .status(statusStr)
                            .auction(auctionStr)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "Just now";
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        java.time.Duration duration = java.time.Duration.between(time, now);
        long seconds = Math.abs(duration.getSeconds());
        String clockTime = time.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a"));
        if (seconds < 10) {
            return "Just now (" + clockTime + ")";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return (minutes <= 1 ? "1 min ago" : minutes + " min ago") + " (" + clockTime + ")";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " h ago (" + clockTime + ")";
        }
        return time.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"));
    }


}
