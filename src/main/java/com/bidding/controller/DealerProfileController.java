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
    private final com.bidding.repo.AdminRepository adminRepository;
    private final com.bidding.repo.InspectorRepository inspectorRepository;
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

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            dealer.setOwnerName(request.getFullName().trim());
        }
        if (request.getDealershipName() != null && !request.getDealershipName().trim().isEmpty()) {
            dealer.setDealershipName(request.getDealershipName().trim());
        }
        if (request.getAddress() != null) {
            dealer.setAddress(request.getAddress().trim());
        }
        if (request.getArea() != null) {
            dealer.setArea(request.getArea().trim());
        }
        if (request.getCity() != null) {
            dealer.setCity(request.getCity().trim());
        }

        // Update Mobile Number if provided
        if (request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty()) {
            String newMobile = request.getMobileNumber().trim();
            if (!newMobile.matches("^[6-9][0-9]{9}$")) {
                throw new IllegalArgumentException("Mobile number must be a 10-digit number starting with 6, 7, 8, or 9");
            }
            if (!newMobile.equals(dealer.getMobileNumber())) {
                if (adminRepository.existsByMobileNumber(newMobile) ||
                    inspectorRepository.existsByMobileNumber(newMobile) ||
                    dealerRepository.findByMobileNumber(newMobile).filter(d -> !d.getId().equals(dealer.getId())).isPresent()) {
                    throw new com.bidding.exception.ResourceAlreadyExistsException("Mobile number is already registered to another account");
                }
                dealer.setMobileNumber(newMobile);
            }
        }

        // Update Email Address if provided
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.isEmpty()) {
                if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new IllegalArgumentException("Invalid email address format");
                }
                if (!newEmail.equalsIgnoreCase(dealer.getEmail())) {
                    if (adminRepository.existsByEmail(newEmail) ||
                        inspectorRepository.existsByEmail(newEmail) ||
                        dealerRepository.findByEmail(newEmail).filter(d -> !d.getId().equals(dealer.getId())).isPresent()) {
                        throw new com.bidding.exception.ResourceAlreadyExistsException("Email is already registered to another account");
                    }
                    dealer.setEmail(newEmail);
                }
            } else {
                dealer.setEmail(null);
            }
        }

        dealer.setUpdatedAt(java.time.LocalDateTime.now());
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
        String identifier = userDetails.getUsername();
        return dealerRepository.findByEmailOrMobileNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Dealer not found with identifier: " + identifier));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        private String fullName;
        private String dealershipName;
        private String email;
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
