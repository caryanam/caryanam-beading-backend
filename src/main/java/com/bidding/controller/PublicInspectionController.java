package com.bidding.controller;

import com.bidding.dto.request.SellerResponseRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public Bidding", description = "Public endpoints for shared bidding rooms and seller confirmations")
@RequiredArgsConstructor
public class PublicInspectionController {

    private final InspectionService inspectionService;

    @GetMapping("/inspection/{id}")
    @Operation(summary = "Get detailed inspection by ID for public bidding link view")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getPublicInspectionDetails(@PathVariable Long id) {
        InspectionDetailsResponse response = inspectionService.getInspection(id);
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Public inspection details retrieved successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/inspection/{id}/seller-response")
    @Operation(summary = "Submit seller confirmation response via public link")
    public ResponseEntity<ApiResponse<Void>> submitPublicSellerResponse(
            @PathVariable Long id,
            @RequestBody SellerResponseRequest request) {
        inspectionService.submitSellerResponse(id, request.getAgreed(), request.getCounterPrice(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Seller response submitted successfully.")
                .build());
    }
}
