package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.entity.Dealer;
import com.bidding.repo.DealerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/dealer/profile")
@RequiredArgsConstructor
@Tag(name = "Dealer Profile API", description = "Endpoints for managing dealer profile and password settings")
public class DealerProfileController {

    private final DealerRepository dealerRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.bidding.repo.VehicleRepository vehicleRepository;
    private final com.bidding.repo.BidRepository bidRepository;

    @GetMapping
    @Operation(summary = "Get dealer profile")
    public ResponseEntity<ApiResponse<DealerResponseDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Dealer dealer = getDealer(userDetails);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        java.util.List<com.bidding.dto.responce.DealerWonBidDTO> wonBids = vehicleRepository.findAll().stream()
                .filter(v -> {
                    if (v.getCurrentHighestBidder() == null || !v.getCurrentHighestBidder().getId().equals(dealer.getId())) {
                        return false;
                    }
                    String status = v.getVehicleStatus();
                    if (status == null) return false;
                    if ("LIVE".equalsIgnoreCase(status) && (v.getAuctionEndTime() == null || now.isBefore(v.getAuctionEndTime()))) {
                        return false;
                    }
                    if ("READY_FOR_AUCTION".equalsIgnoreCase(status) || "UPCOMING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                        return false;
                    }
                    return true;
                })
                .map(v -> com.bidding.dto.responce.DealerWonBidDTO.builder()
                        .vehicleId(v.getId())
                        .vehicleNumber(v.getVehicleNumber())
                        .brand(v.getBrand())
                        .model(v.getModel())
                        .variant(v.getVariant())
                        .winningBidAmount(v.getCurrentHighestBid())
                        .status(v.getVehicleStatus())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        DealerResponseDTO dto = DealerResponseDTO.builder()
                .id(dealer.getId())
                .dealershipName(dealer.getDealershipName())
                .ownerName(dealer.getOwnerName())
                .email(dealer.getEmail())
                .mobileNumber(dealer.getMobileNumber())
                .address(dealer.getAddress())
                .area(dealer.getArea())
                .city(dealer.getCity())
                .role(dealer.getRole())
                .totalBids(bidRepository.countByDealerId(dealer.getId()))
                .wonBidsCount((long) wonBids.size())
                .wonBids(wonBids)
                .build();

        return ResponseEntity.ok(ApiResponse.<DealerResponseDTO>builder()
                .success(true)
                .message("Profile retrieved successfully.")
                .data(dto)
                .build());
    }

    @PutMapping
    @Operation(summary = "Update dealer profile details")
    public ResponseEntity<ApiResponse<DealerResponseDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Dealer dealer = getDealer(userDetails);
        if (request.getFullName() != null) dealer.setOwnerName(request.getFullName());
        if (request.getDealershipName() != null) dealer.setDealershipName(request.getDealershipName());
        if (request.getAddress() != null) dealer.setAddress(request.getAddress());
        if (request.getArea() != null) dealer.setArea(request.getArea());
        if (request.getCity() != null) dealer.setCity(request.getCity());
        dealerRepository.save(dealer);

        DealerResponseDTO dto = DealerResponseDTO.builder()
                .id(dealer.getId())
                .dealershipName(dealer.getDealershipName())
                .ownerName(dealer.getOwnerName())
                .email(dealer.getEmail())
                .mobileNumber(dealer.getMobileNumber())
                .address(dealer.getAddress())
                .area(dealer.getArea())
                .city(dealer.getCity())
                .role(dealer.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.<DealerResponseDTO>builder()
                .success(true)
                .message("Profile updated successfully.")
                .data(dto)
                .build());
    }

    @PutMapping("/password")
    @Operation(summary = "Change dealer password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {
        Dealer dealer = getDealer(userDetails);

        if (request.getCurrentPassword() != null && !request.getCurrentPassword().trim().isEmpty()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), dealer.getPassword())) {
                throw new IllegalArgumentException("Current password does not match.");
            }
        }

        dealer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        dealerRepository.save(dealer);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully.")
                .build());
    }

    private Dealer getDealer(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return dealerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Dealer not found with email: " + email));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        private String fullName;
        private String dealershipName;
        private String mobileNumber;
        private String address;
        private String area;
        private String city;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        private String currentPassword;

        @NotBlank(message = "New password cannot be blank")
        private String newPassword;
    }
}
