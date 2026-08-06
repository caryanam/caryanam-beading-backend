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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/inspector/profile")
@RequiredArgsConstructor
@Tag(name = "Inspector Profile API", description = "Endpoints for managing inspector profile and password settings")
public class InspectorProfileController {

    private final InspectorRepository inspectorRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Get inspector profile")
    public ResponseEntity<ApiResponse<InspectorResponseDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Inspector inspector = getInspector(userDetails);
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
    @Operation(summary = "Update inspector profile details")
    public ResponseEntity<ApiResponse<InspectorResponseDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Inspector inspector = getInspector(userDetails);
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
    @Operation(summary = "Change inspector password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {
        Inspector inspector = getInspector(userDetails);

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

    private Inspector getInspector(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return inspectorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Inspector not found with email: " + email));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        @NotBlank(message = "Full name cannot be blank")
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
        private String currentPassword;

        @NotBlank(message = "New password cannot be blank")
        private String newPassword;
    }
}
