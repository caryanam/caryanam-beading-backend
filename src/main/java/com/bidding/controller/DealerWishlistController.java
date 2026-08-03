package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.entity.Dealer;
import com.bidding.entity.Inspection;
import com.bidding.entity.Wishlist;
import com.bidding.repo.DealerRepository;
import com.bidding.repo.InspectionRepository;
import com.bidding.repo.WishlistRepository;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    private final InspectionService inspectionService;

    @GetMapping
    @Operation(summary = "Get list of all inspections on dealer's wishlist")
    public ResponseEntity<ApiResponse<List<InspectionSummaryResponse>>> getWishlist(Principal principal) {
        Dealer dealer = dealerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));

        List<Wishlist> wishlistItems = wishlistRepository.findByDealerId(dealer.getId());
        Set<Long> wishlistInspectionIds = wishlistItems.stream()
                .map(item -> item.getInspection().getId())
                .collect(Collectors.toSet());

        List<InspectionSummaryResponse> wishlistVehicles = inspectionService.getAllInspections().stream()
                .filter(ins -> wishlistInspectionIds.contains(ins.getInspectionId()))
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
