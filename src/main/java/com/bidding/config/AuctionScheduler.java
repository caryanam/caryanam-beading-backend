package com.bidding.config;

import com.bidding.entity.Inspection;
import com.bidding.entity.Vehicle;
import com.bidding.enums.InspectionStatus;
import com.bidding.repo.InspectionRepository;
import com.bidding.repo.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final InspectionRepository inspectionRepository;
    private final VehicleRepository vehicleRepository;
    private final AuctionWebSocketHandler webSocketHandler;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void checkExpiredAuctions() {
        List<Inspection> activeInspections = inspectionRepository.findAll().stream()
                .filter(ins -> ins.getStatus() == InspectionStatus.APPROVED &&
                        ins.getVehicle() != null &&
                        "LIVE".equalsIgnoreCase(ins.getVehicle().getVehicleStatus()))
                .toList();

        LocalDateTime now = LocalDateTime.now();

        for (Inspection ins : activeInspections) {
            Vehicle v = ins.getVehicle();
            if (v.getAuctionEndTime() != null && now.isAfter(v.getAuctionEndTime())) {
                log.info("Auction expired for inspectionId: {}. Ending auction...", ins.getId());

                // Set status to SOLD OUT
                v.setVehicleStatus("SOLD OUT");
                vehicleRepository.save(v);

                // Broadcast expiration
                Map<String, Object> endMessage = new HashMap<>();
                endMessage.put("type", "AUCTION_ENDED");
                endMessage.put("inspectionId", ins.getId());
                endMessage.put("winner", v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getDealershipName() : "No bids");
                endMessage.put("winningBid", v.getCurrentHighestBid() != null ? v.getCurrentHighestBid() : 0.0);

                webSocketHandler.broadcast(ins.getId(), endMessage);
                log.info("Auction ended broadcast sent for inspectionId: {}", ins.getId());
            }
        }
    }
}
