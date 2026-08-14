package com.bidding.controller;

import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.enums.InspectionStatus;
import com.bidding.dto.responce.BidResponseDTO;
import com.bidding.dto.responce.DealerBidResponseDTO;
import com.bidding.service.BiddingService;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dealer")
@Tag(name = "Dealer", description = "Dealer APIs")
@RequiredArgsConstructor
public class DealerController {

    private final InspectionService inspectionService;
    private final BiddingService biddingService;
    private final com.bidding.service.NotificationService notificationService;

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications for logged-in dealer")
    public ResponseEntity<ApiResponse<List<com.bidding.dto.responce.NotificationDTO>>> getDealerNotifications(Principal principal) {
        List<com.bidding.dto.responce.NotificationDTO> list = notificationService.getDealerNotifications(principal.getName());
        return ResponseEntity.ok(ApiResponse.<List<com.bidding.dto.responce.NotificationDTO>>builder()
                .success(true)
                .message("Dealer notifications retrieved successfully.")
                .data(list)
                .build());
    }

    @GetMapping("/inspections")
    @Operation(summary = "Get list of all approved inspections for dealer marketplace")
    public ResponseEntity<ApiResponse<List<InspectionSummaryResponse>>> getMarketplaceInspections() {
        List<InspectionSummaryResponse> approvedInspections = inspectionService.getAllInspections().stream()
                .filter(ins -> ins.getStatus() == InspectionStatus.APPROVED)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<InspectionSummaryResponse>>builder()
                .success(true)
                .message("Approved marketplace inspections retrieved successfully.")
                .data(approvedInspections)
                .build());
    }

    @GetMapping("/inspection/{id}")
    @Operation(summary = "Get detailed inspection by ID for dealer")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getInspectionDetails(@PathVariable Long id) {
        InspectionDetailsResponse response = inspectionService.getInspection(id);
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Inspection details retrieved successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/inspection/{id}/bid")
    @Operation(summary = "Place a bid on a live vehicle auction")
    public ResponseEntity<ApiResponse<Void>> placeBid(
            @PathVariable Long id,
            @RequestBody Map<String, Double> payload,
            Principal principal) {
        
        Double amount = payload.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Bid amount is required");
        }
        
        biddingService.placeBid(id, principal.getName(), amount);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Bid placed successfully.")
                .build());
    }

    @GetMapping("/inspection/{id}/bids")
    @Operation(summary = "Get bid history for a vehicle")
    public ResponseEntity<ApiResponse<List<BidResponseDTO>>> getBidHistory(@PathVariable Long id) {
        List<BidResponseDTO> history = biddingService.getBidHistory(id);
        
        return ResponseEntity.ok(ApiResponse.<List<BidResponseDTO>>builder()
                .success(true)
                .message("Bid history retrieved successfully.")
                .data(history)
                .build());
    }

    @GetMapping("/bids")
    @Operation(summary = "Get bid history for logged-in dealer")
    public ResponseEntity<ApiResponse<List<DealerBidResponseDTO>>> getDealerBidHistory(Principal principal) {
        List<DealerBidResponseDTO> history = biddingService.getDealerBidHistory(principal.getName());
        return ResponseEntity.ok(ApiResponse.<List<DealerBidResponseDTO>>builder()
                .success(true)
                .message("Dealer bid history retrieved successfully.")
                .data(history)
                .build());
    }

    @PostMapping("/inspection/{id}/seller-response")
    @Operation(summary = "Submit seller confirmation response (Agreed / Counter offer)")
    public ResponseEntity<ApiResponse<Void>> submitSellerResponse(
            @PathVariable Long id,
            @RequestBody com.bidding.dto.request.SellerResponseRequest request) {
        inspectionService.submitSellerResponse(id, request.getAgreed(), request.getCounterPrice(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Seller response submitted successfully.")
                .build());
    }

    @PostMapping("/inspection/{id}/reply")
    @Operation(summary = "Submit dealer reply to admin negotiation message")
    public ResponseEntity<ApiResponse<Void>> submitDealerReply(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reply = body.get("reply");
        inspectionService.submitDealerReply(id, reply);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Dealer reply submitted successfully.")
                .build());
    }
}

