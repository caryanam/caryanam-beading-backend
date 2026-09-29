package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.entity.Inspector;
import com.bidding.repo.AdminRepository;
import com.bidding.repo.DealerRepository;
import com.bidding.repo.InspectorRepository;
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
@RequestMapping("/api/freelancer/profile")
@RequiredArgsConstructor
@Tag(name = "Freelancer Profile API", description = "Endpoints for managing freelancer profile and password settings")
public class FreelancerProfileController {

    private final InspectorRepository inspectorRepository;
    private final AdminRepository adminRepository;
    private final DealerRepository dealerRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Get freelancer profile")
    public ResponseEntity<ApiResponse<InspectorResponseDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Inspector inspector = getFreelancer(userDetails);
        InspectorResponseDTO dto = InspectorResponseDTO.builder()
                .id(inspector.getId())
                .fullName(inspector.getFullName())
                .email(inspector.getEmail())
                .mobileNumber(inspector.getMobileNumber())
                .role(inspector.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.<InspectorResponseDTO>builder()
                .success(true)
                .message("Profile retrieved successfully.")
                .data(dto)
                .build());
    }

    @PutMapping
    @Operation(summary = "Update freelancer profile details")
    public ResponseEntity<ApiResponse<InspectorResponseDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Inspector inspector = getFreelancer(userDetails);

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            inspector.setFullName(request.getFullName().trim());
        }

        // Validate and update email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
            if (!newEmail.equalsIgnoreCase(inspector.getEmail())) {
                if (adminRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email is already registered as an admin.");
                }
                if (dealerRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email is already registered by a dealer.");
                }
                inspectorRepository.findByEmail(newEmail).ifPresent(other -> {
                    if (!other.getId().equals(inspector.getId())) {
                        throw new IllegalArgumentException("Email is already registered by another inspector/freelancer.");
                    }
                });
                inspector.setEmail(newEmail);
            }
        }

        // Validate and update mobile number
        if (request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty()) {
            String newMobile = request.getMobileNumber().trim();
            if (!newMobile.matches("^[6-9][0-9]{9}$")) {
                throw new IllegalArgumentException("Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9.");
            }
            if (!newMobile.equals(inspector.getMobileNumber())) {
                if (adminRepository.findByMobileNumber(newMobile).isPresent()) {
                    throw new IllegalArgumentException("Mobile number is already registered by an admin.");
                }
                if (dealerRepository.findByMobileNumber(newMobile).isPresent()) {
                    throw new IllegalArgumentException("Mobile number is already registered by a dealer.");
                }
                inspectorRepository.findByMobileNumber(newMobile).ifPresent(other -> {
                    if (!other.getId().equals(inspector.getId())) {
                        throw new IllegalArgumentException("Mobile number is already registered by another inspector/freelancer.");
                    }
                });
                inspector.setMobileNumber(newMobile);
            }
        }

        inspectorRepository.save(inspector);

        InspectorResponseDTO dto = InspectorResponseDTO.builder()
                .id(inspector.getId())
                .fullName(inspector.getFullName())
                .email(inspector.getEmail())
                .mobileNumber(inspector.getMobileNumber())
                .role(inspector.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.<InspectorResponseDTO>builder()
                .success(true)
                .message("Profile updated successfully.")
                .data(dto)
                .build());
    }

    @PutMapping("/password")
    @Operation(summary = "Change freelancer password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InspectorProfileController.PasswordChangeRequest request) {
        Inspector inspector = getFreelancer(userDetails);

        if (request.getCurrentPassword() != null && !request.getCurrentPassword().trim().isEmpty()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), inspector.getPassword())) {
                throw new IllegalArgumentException("Current password does not match.");
            }
        }

        inspector.setPassword(passwordEncoder.encode(request.getNewPassword()));
        inspectorRepository.save(inspector);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully.")
                .build());
    }

    private Inspector getFreelancer(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return inspectorRepository.findByEmail(email)
                .or(() -> inspectorRepository.findByMobileNumber(email))
                .orElseThrow(() -> new RuntimeException("Freelancer profile not found with identifier: " + email));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        @NotBlank(message = "Full name cannot be blank")
        private String fullName;

        private String email;

        @NotBlank(message = "Mobile number cannot be blank")
        @Size(min = 10, max = 10, message = "Mobile number must be exactly 10 digits")
        private String mobileNumber;
    }
}
