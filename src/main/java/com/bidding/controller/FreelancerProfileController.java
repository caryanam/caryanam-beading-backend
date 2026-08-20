package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.entity.Inspector;
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

@RestController
@RequestMapping("/api/freelancer/profile")
@RequiredArgsConstructor
@Tag(name = "Freelancer Profile API", description = "Endpoints for managing freelancer profile and password settings")
public class FreelancerProfileController {

    private final InspectorRepository inspectorRepository;
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
            @Valid @RequestBody InspectorProfileController.ProfileUpdateRequest request) {
        Inspector inspector = getFreelancer(userDetails);
        inspector.setFullName(request.getFullName());
        inspector.setMobileNumber(request.getMobileNumber());
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
                .orElseThrow(() -> new RuntimeException("Freelancer profile not found with email: " + email));
    }
}
