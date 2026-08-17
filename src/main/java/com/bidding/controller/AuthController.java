package com.bidding.controller;

import com.bidding.dto.request.LoginRequest;
import com.bidding.dto.request.InspectorRegisterRequest;
import com.bidding.dto.request.DealerRegisterRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.AuthResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.service.AuthService;
import com.bidding.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Authentication, OTP Verification, and Registration APIs")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/auth/send-otp")
    @Operation(summary = "Send OTP to Email for Registration")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String mobile = request.get("mobile");
        otpService.sendOtp(email, mobile);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("OTP sent successfully to " + email)
                        .build()
        );
    }

    @PostMapping("/auth/send-password-otp")
    @Operation(summary = "Send OTP to Email for Password Change")
    public ResponseEntity<ApiResponse<Void>> sendPasswordOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        otpService.sendPasswordResetOtp(email);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password verification OTP sent successfully to " + email)
                        .build()
        );
    }

    @PostMapping("/auth/verify-otp")
    @Operation(summary = "Verify Email OTP for Registration")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        boolean isValid = otpService.verifyOtp(email, otp);
        if (isValid) {
            return ResponseEntity.ok(
                    ApiResponse.<Boolean>builder()
                            .success(true)
                            .message("OTP verified successfully!")
                            .data(true)
                            .build()
            );
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Boolean>builder()
                            .success(false)
                            .message("Invalid or expired OTP. Please try again.")
                            .data(false)
                            .build()
            );
        }
    }

    @PostMapping("/inspector/register")
    @Operation(summary = "Inspector Registration")
    public ResponseEntity<ApiResponse<InspectorResponseDTO>> registerInspector(
            @Valid @RequestBody InspectorRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<InspectorResponseDTO>builder()
                        .success(true)
                        .message("Registration successful.")
                        .data(authService.registerInspector(request))
                        .build()
        );
    }

    @PostMapping("/dealer/register")
    @Operation(summary = "Dealer Registration")
    public ResponseEntity<ApiResponse<DealerResponseDTO>> registerDealer(
            @Valid @RequestBody DealerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DealerResponseDTO>builder()
                        .success(true)
                        .message("Registration successful.")
                        .data(authService.registerDealer(request))
                        .build()
        );
    }

    @PostMapping("/auth/login")
    @Operation(summary = "All Login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Login Successfully")
                        .data(authService.login(request))
                        .build()
        );
    }

    @PostMapping("/auth/reset-password")
    @Operation(summary = "Reset Password after OTP verification")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            newPassword = request.get("password");
        }
        authService.resetPassword(email, otp, newPassword);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successfully. Please login with your new password.")
                        .build()
        );
    }

    @PostMapping("/auth/forgot-password")
    @Operation(summary = "Forgot Password / Reset Password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> request) {
        return resetPassword(request);
    }
}
