package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.FreelancerVehicleResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.entity.Inspector;
import com.bidding.repo.InspectorRepository;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management API", description = "Endpoints for vehicle CRUD operations")
public class VehicleController {

    private final InspectionService inspectionService;
    private final InspectorRepository inspectorRepository;

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Delete a vehicle permanently from database by ID")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long vehicleId) {
        inspectionService.deleteVehicle(vehicleId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Vehicle deleted successfully")
                .build());
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle details by database ID")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getVehicleDetails(@PathVariable Long vehicleId) {
        InspectionDetailsResponse response = inspectionService.getVehicleDetailsByVehicleId(vehicleId);

        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Vehicle details retrieved successfully.")
                .data(response)
                .build());
    }

    @GetMapping("")
    @Operation(summary = "Get list of vehicles")
    public ResponseEntity<ApiResponse<List<FreelancerVehicleResponse>>> getVehicles(
            @RequestParam(value = "freelancerId", required = false) Long freelancerId,
            @RequestParam(value = "inspectorId", required = false) Long inspectorId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long targetId = freelancerId != null ? freelancerId : inspectorId;
        if (targetId != null) {
            List<FreelancerVehicleResponse> response = inspectionService.getFreelancerSubmissions(targetId);
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("Vehicles retrieved successfully for id: " + targetId)
                    .data(response)
                    .build());
        }

        if (userDetails != null && userDetails.getAuthorities() != null &&
                userDetails.getAuthorities().stream().anyMatch(a ->
                        a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") ||
                        a.getAuthority().equalsIgnoreCase("ADMIN"))) {
            List<FreelancerVehicleResponse> adminResponse = inspectionService.getAllFreelancerSubmissionsForAdmin();
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("All vehicles retrieved successfully.")
                    .data(adminResponse)
                    .build());
        }

        if (userDetails != null) {
            String username = userDetails.getUsername();
            Inspector inspector = inspectorRepository.findByEmail(username).orElse(null);
            if (inspector != null) {
                List<FreelancerVehicleResponse> response = inspectionService.getFreelancerSubmissions(inspector.getId());
                return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                        .success(true)
                        .message("Vehicles retrieved successfully.")
                        .data(response)
                        .build());
            }

            List<FreelancerVehicleResponse> dealerResponse = inspectionService.getAllFreelancerSubmissionsForDealer(username);
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("Vehicles retrieved successfully.")
                    .data(dealerResponse)
                    .build());
        }

        List<FreelancerVehicleResponse> defaultResponse = inspectionService.getAllFreelancerSubmissionsForAdmin();
        return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                .success(true)
                .message("All vehicles retrieved successfully.")
                .data(defaultResponse)
                .build());
    }
}
