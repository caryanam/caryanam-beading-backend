package com.bidding.controller;

import com.bidding.dto.request.EnquiryRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.EnquiryResponse;
import com.bidding.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Enquiry API", description = "Endpoints for managing contact enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping("/public/enquiry")
    @Operation(summary = "Submit a new enquiry")
    public ResponseEntity<ApiResponse<Void>> submitEnquiry(@RequestBody EnquiryRequest request) {
        enquiryService.submitEnquiry(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Enquiry submitted successfully.")
                .build());
    }

    @GetMapping("/admin/enquiry")
    @Operation(summary = "Get all enquiries (Admin only)")
    public ResponseEntity<ApiResponse<List<EnquiryResponse>>> getAllEnquiries() {
        List<EnquiryResponse> responses = enquiryService.getAllEnquiries();
        return ResponseEntity.ok(ApiResponse.<List<EnquiryResponse>>builder()
                .success(true)
                .message("Enquiries retrieved successfully.")
                .data(responses)
                .build());
    }
}
