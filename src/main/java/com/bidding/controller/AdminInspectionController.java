package com.bidding.controller;

import com.bidding.dto.request.AdminRejectRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Vehicle Inspection Management API", description = "Endpoints for administrator inspection reviews")
public class AdminInspectionController {

    private final InspectionService inspectionService;

    @GetMapping("/api/admin/inspections")
    @Operation(summary = "Get list of all submitted vehicle inspections")
    public ResponseEntity<ApiResponse<List<InspectionSummaryResponse>>> getAllInspections() {
        List<InspectionSummaryResponse> response = inspectionService.getAllInspections();
        
        return ResponseEntity.ok(ApiResponse.<List<InspectionSummaryResponse>>builder()
                .success(true)
                .message("Inspections retrieved successfully.")
                .data(response)
                .build());
    }

    @GetMapping("/api/admin/inspection/{id}")
    @Operation(summary = "Get detailed vehicle inspection by ID")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getInspectionById(@PathVariable Long id) {
        InspectionDetailsResponse response = inspectionService.getInspection(id);
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Inspection details retrieved successfully.")
                .data(response)
                .build());
    }

    @PutMapping("/api/admin/inspection/{id}/approve")
    @Operation(summary = "Approve submitted vehicle inspection (Marks vehicle ready for auction)")
    public ResponseEntity<ApiResponse<Void>> approveInspection(@PathVariable Long id) {
        inspectionService.approveInspection(id);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inspection approved successfully. Vehicle status set to READY_FOR_AUCTION.")
                .build());
    }

    @PutMapping("/api/admin/inspection/{id}/reject")
    @Operation(summary = "Reject submitted vehicle inspection with reason")
    public ResponseEntity<ApiResponse<Void>> rejectInspection(
            @PathVariable Long id,
            @Valid @RequestBody AdminRejectRequest request) {
        
        inspectionService.rejectInspection(id, request.getReason());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inspection rejected successfully.")
                .build());
    }

}
