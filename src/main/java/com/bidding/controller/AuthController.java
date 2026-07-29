package com.bidding.controller;

import com.bidding.dto.request.LoginRequest;
import com.bidding.dto.request.InspectorRegisterRequest;
import com.bidding.dto.request.DealerRegisterRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.AuthResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Authentication and Registration APIs")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}
