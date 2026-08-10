package com.bidding.controller;

import com.bidding.dto.request.AdminRejectRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.service.InspectionService;
import com.bidding.service.BiddingService;
import com.bidding.dto.responce.BidResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Vehicle Inspection Management API", description = "Endpoints for administrator inspection reviews")
public class AdminInspectionController {

    private final InspectionService inspectionService;
    private final BiddingService biddingService;
    private final com.bidding.service.NotificationService notificationService;

    @GetMapping("/api/admin/notifications")
    @Operation(summary = "Get notifications for admin")
    public ResponseEntity<ApiResponse<List<com.bidding.dto.responce.NotificationDTO>>> getAdminNotifications() {
        List<com.bidding.dto.responce.NotificationDTO> list = notificationService.getAdminNotifications();
        return ResponseEntity.ok(ApiResponse.<List<com.bidding.dto.responce.NotificationDTO>>builder()
                .success(true)
                .message("Admin notifications retrieved successfully.")
                .data(list)
                .build());
    }

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

    @GetMapping("/api/admin/inspectors")
    @Operation(summary = "Get list of all registered inspectors")
    public ResponseEntity<ApiResponse<List<InspectorResponseDTO>>> getAllInspectors() {
        List<InspectorResponseDTO> response = inspectionService.getAllInspectors();
        
        return ResponseEntity.ok(ApiResponse.<List<InspectorResponseDTO>>builder()
                .success(true)
                .message("Inspectors retrieved successfully.")
                .data(response)
                .build());
    }

    @DeleteMapping("/api/admin/inspector/{id}")
    @Operation(summary = "Delete inspector (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteInspector(@PathVariable Long id) {
        inspectionService.deleteInspector(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inspector deleted successfully.")
                .build());
    }

    @GetMapping("/api/admin/dealers")
    @Operation(summary = "Get list of all registered dealers")
    public ResponseEntity<ApiResponse<List<DealerResponseDTO>>> getAllDealers() {
        List<DealerResponseDTO> response = inspectionService.getAllDealers();
        
        return ResponseEntity.ok(ApiResponse.<List<DealerResponseDTO>>builder()
                .success(true)
                .message("Dealers retrieved successfully.")
                .data(response)
                .build());
    }

    @PutMapping("/api/admin/dealer/{id}")
    @Operation(summary = "Update dealer details (Admin)")
    public ResponseEntity<ApiResponse<DealerResponseDTO>> updateDealer(
            @PathVariable Long id,
            @RequestBody DealerResponseDTO request) {
        DealerResponseDTO response = inspectionService.updateDealer(id, request);
        return ResponseEntity.ok(ApiResponse.<DealerResponseDTO>builder()
                .success(true)
                .message("Dealer updated successfully.")
                .data(response)
                .build());
    }

    @DeleteMapping("/api/admin/dealer/{id}")
    @Operation(summary = "Delete dealer (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteDealer(@PathVariable Long id) {
        inspectionService.deleteDealer(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Dealer deleted successfully.")
                .build());
    }

    @PostMapping("/api/admin/dealers/import")
    @Operation(summary = "Import dealers from Excel sheet")
    public ResponseEntity<ApiResponse<Void>> importDealers(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        inspectionService.importDealers(file);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Dealers imported successfully.")
                .build());
    }

    @PutMapping("/api/admin/inspection/{id}/go-live")
    @Operation(summary = "Start live auction for the vehicle")
    public ResponseEntity<ApiResponse<Void>> goLive(@PathVariable Long id) {
        inspectionService.goLive(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Auction is now live.")
                .build());
    }

    @PutMapping({"/api/admin/inspection/{id}/stop-auction", "/api/admin/inspection/{id}/stop"})
    @Operation(summary = "Manually stop/end live auction for the vehicle")
    public ResponseEntity<ApiResponse<Void>> stopAuction(@PathVariable Long id) {
        inspectionService.stopAuction(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Auction has been stopped.")
                .build());
    }

    @GetMapping("/api/admin/inspection/{id}/bids")
    @Operation(summary = "Get bid history for a vehicle (Admin)")
    public ResponseEntity<ApiResponse<List<BidResponseDTO>>> getBidHistory(@PathVariable Long id) {
        List<BidResponseDTO> history = biddingService.getBidHistory(id);
        return ResponseEntity.ok(ApiResponse.<List<BidResponseDTO>>builder()
                .success(true)
                .message("Bid history retrieved successfully.")
                .data(history)
                .build());
    }

    @PostMapping("/api/admin/inspection/{id}/dealer-message")
    @Operation(summary = "Send negotiation message to winning dealer (Admin)")
    public ResponseEntity<ApiResponse<Void>> sendDealerMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        inspectionService.submitAdminDealerMessage(id, message);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Message sent to winning dealer successfully.")
                .build());
    }

    @PutMapping("/api/admin/inspection/{id}/vehicle-status")
    @Operation(summary = "Manually update vehicle status (e.g. SOLD OUT)")
    public ResponseEntity<ApiResponse<Void>> updateVehicleStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String vehicleStatus = body.get("vehicleStatus");
        inspectionService.updateVehicleStatus(id, vehicleStatus);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Vehicle status updated successfully.")
                .build());
    }

    @GetMapping("/api/admin/inspection/{id}/pdf")
    @Operation(summary = "Download inspection summary report PDF (Admin)")
    public ResponseEntity<byte[]> downloadPdfAdmin(@PathVariable Long id) {
        byte[] pdfBytes = inspectionService.generatePdfReport(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inspection_report_" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
