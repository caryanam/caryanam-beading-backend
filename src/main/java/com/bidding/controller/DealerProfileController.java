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

    @GetMapping
    @Operation(summary = "Get dealer profile")
    public ResponseEntity<ApiResponse<DealerResponseDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Dealer dealer = getDealer(userDetails);
        DealerResponseDTO dto = DealerResponseDTO.builder()
                .id(dealer.getId())
                .dealershipName(dealer.getDealershipName())
                .ownerName(dealer.getOwnerName())
                .email(dealer.getEmail())
                .mobileNumber(dealer.getMobileNumber())
                .role(dealer.getRole())
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
        dealer.setOwnerName(request.getFullName());
        dealer.setMobileNumber(request.getMobileNumber());
        dealerRepository.save(dealer);

        DealerResponseDTO dto = DealerResponseDTO.builder()
                .id(dealer.getId())
                .dealershipName(dealer.getDealershipName())
                .ownerName(dealer.getOwnerName())
                .email(dealer.getEmail())
                .mobileNumber(dealer.getMobileNumber())
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

        if (!passwordEncoder.matches(request.getCurrentPassword(), dealer.getPassword())) {
            throw new IllegalArgumentException("Current password does not match.");
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
        @NotBlank(message = "Owner name cannot be blank")
        private String fullName;

        @NotBlank(message = "Mobile number cannot be blank")
        @Size(min = 10, max = 10, message = "Mobile number must be exactly 10 digits")
        private String mobileNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        @NotBlank(message = "Current password cannot be blank")
        private String currentPassword;

        @NotBlank(message = "New password cannot be blank")
        private String newPassword;
    }
}
