package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.entity.Dealer;
import com.bidding.entity.Inspection;
import com.bidding.entity.InspectionImage;
import com.bidding.entity.Vehicle;
import com.bidding.entity.Wishlist;
import com.bidding.repo.DealerRepository;
import com.bidding.repo.InspectionImageRepository;
import com.bidding.repo.InspectionRepository;
import com.bidding.repo.WishlistRepository;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dealer/wishlist")
@Tag(name = "Dealer Wishlist", description = "Dealer Wishlist APIs")
@RequiredArgsConstructor
public class DealerWishlistController {

    private final WishlistRepository wishlistRepository;
    private final DealerRepository dealerRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionImageRepository inspectionImageRepository;
    private final InspectionService inspectionService;

    @Value("${app.base-url}")
    private String baseUrl;

    private String buildFullImageUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            return null;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        return baseUrl + relativeUrl;
    }

    @GetMapping
    @Operation(summary = "Get list of all inspections on dealer's wishlist")
    public ResponseEntity<ApiResponse<List<InspectionSummaryResponse>>> getWishlist(Principal principal) {
        Dealer dealer = dealerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));

        List<Wishlist> wishlistItems = wishlistRepository.findByDealerId(dealer.getId());
        Set<Long> wishlistInspectionIds = wishlistItems.stream()
                .map(item -> item.getInspection().getId())
                .collect(Collectors.toSet());

        List<InspectionSummaryResponse> wishlistVehicles = inspectionRepository.findAllById(wishlistInspectionIds).stream()
                .map(ins -> {
                    Vehicle v = ins.getVehicle();
                    List<InspectionImage> images = inspectionImageRepository.findByInspectionId(ins.getId());
                    String imgUrl = (images != null && !images.isEmpty()) ? buildFullImageUrl(images.get(0).getImageUrl()) : null;
                    return InspectionSummaryResponse.builder()
                            .inspectionId(ins.getId())
                            .vehicleNumber(v != null ? v.getVehicleNumber() : "N/A")
                            .ownerName(v != null ? v.getOwnerName() : "N/A")
                            .customerMobileNumber(v != null ? v.getCustomerMobileNumber() : null)
                            .brand(v != null ? v.getBrand() : "N/A")
                            .model(v != null ? v.getModel() : "N/A")
                            .variant(v != null ? v.getVariant() : "N/A")
                            .status(ins.getStatus())
                            .submittedAt(ins.getSubmittedAt())
                            .inspectorName((ins.getInspector() != null && ins.getInspector().getFullName() != null) ? ins.getInspector().getFullName() : (ins.getSubmittedBy() != null ? ins.getSubmittedBy().getFullName() : "Certified Inspector"))
                            .freelancerName((ins.getInspector() != null && ins.getInspector().getFullName() != null) ? ins.getInspector().getFullName() : (ins.getSubmittedBy() != null ? ins.getSubmittedBy().getFullName() : "Certified Inspector"))
                            .suggestedPrice(v != null ? v.getSuggestedPrice() : null)
                            .rejectionReason(ins.getRejectionReason())
                            .vehicleImage(imgUrl)
                            .year(v != null ? (v.getRegistrationYear() != null ? v.getRegistrationYear() : v.getManufacturingYear()) : 2021)
                            .manufacturingYear(v != null ? v.getManufacturingYear() : null)
                            .registrationYear(v != null ? v.getRegistrationYear() : null)
                            .fuel(v != null ? v.getFuelType() : "N/A")
                            .transmission(v != null ? v.getTransmission() : "N/A")
                            .odometer(v != null ? v.getOdometerReading() : null)
                            .vehicleStatus(v != null ? v.getVehicleStatus() : null)
                            .currentHighestBid(v != null ? v.getCurrentHighestBid() : null)
                            .currentHighestBidder((v != null && v.getCurrentHighestBidder() != null) ? v.getCurrentHighestBidder().getDealershipName() : null)
                            .auctionEndTime((v != null && v.getAuctionEndTime() != null) ? v.getAuctionEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                            .totalBids(v != null ? v.getTotalBids() : null)
                            .location(v != null ? v.getLocation() : null)
                            .rtoInformation(v != null ? v.getRtoInformation() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<InspectionSummaryResponse>>builder()
                .success(true)
                .message("Wishlist retrieved successfully.")
                .data(wishlistVehicles)
                .build());
    }

    @PostMapping("/add/{inspectionId}")
    @Operation(summary = "Add an inspection to dealer's wishlist")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(@PathVariable Long inspectionId, Principal principal) {
        Dealer dealer = dealerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));

        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));

        if (!wishlistRepository.existsByDealerIdAndInspectionId(dealer.getId(), inspectionId)) {
            Wishlist item = Wishlist.builder()
                    .dealer(dealer)
                    .inspection(inspection)
                    .build();
            wishlistRepository.save(item);
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Added to wishlist successfully.")
                .build());
    }

    @DeleteMapping("/remove/{inspectionId}")
    @Operation(summary = "Remove an inspection from dealer's wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Long inspectionId, Principal principal) {
        Dealer dealer = dealerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));

        wishlistRepository.findByDealerIdAndInspectionId(dealer.getId(), inspectionId)
                .ifPresent(wishlistRepository::delete);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Removed from wishlist successfully.")
                .build());
    }
}
