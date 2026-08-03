package com.bidding.controller;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.dto.responce.InspectorStatsResponse;
import com.bidding.entity.Inspector;
import com.bidding.exception.ResourceNotFoundException;
import com.bidding.repo.InspectorRepository;
import com.bidding.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inspector/inspection")
@RequiredArgsConstructor
@Tag(name = "Inspector Vehicle Inspection API", description = "Endpoints for step-by-step vehicle inspection management")
public class InspectorInspectionController {

    private final InspectionService inspectionService;
    private final InspectorRepository inspectorRepository;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir}")
    private String uploadDir;

    @org.springframework.beans.factory.annotation.Value("${app.car-image-folder}")
    private String carImageFolder;

    @org.springframework.beans.factory.annotation.Value("${app.car-video-folder}")
    private String carVideoFolder;

    @GetMapping("")
    @Operation(summary = "Get list of all inspections for the logged-in inspector")
    public ResponseEntity<ApiResponse<List<InspectionSummaryResponse>>> getMyInspections(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getInspector(userDetails);
        List<InspectionSummaryResponse> response = inspectionService.getInspectionsByInspector(inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<List<InspectionSummaryResponse>>builder()
                .success(true)
                .message("Inspections retrieved successfully.")
                .data(response)
                .build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get inspection stats for the logged-in inspector")
    public ResponseEntity<ApiResponse<InspectorStatsResponse>> getInspectorStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getInspector(userDetails);
        InspectorStatsResponse response = inspectionService.getInspectorStats(inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<InspectorStatsResponse>builder()
                .success(true)
                .message("Stats retrieved successfully.")
                .data(response)
                .build());
    }

    @PostMapping("")
    @Operation(summary = "Create a new vehicle inspection draft (Step 1-5)")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> saveDraft(
            @RequestBody InspectionDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getInspector(userDetails);
        InspectionDetailsResponse response = inspectionService.saveDraft(request, inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Draft saved successfully.")
                .data(response)
                .build());
    }

    @GetMapping("/{inspectionId}")
    @Operation(summary = "Load inspection details by ID to continue")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getInspection(
            @PathVariable Long inspectionId) {
        
        InspectionDetailsResponse response = inspectionService.getInspection(inspectionId);
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Inspection draft loaded successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/{inspectionId}/submit")
    @Operation(summary = "Final submit vehicle inspection (Locks edits)")
    public ResponseEntity<ApiResponse<Void>> submitInspection(
            @PathVariable Long inspectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getInspector(userDetails);
        inspectionService.submitInspection(inspectionId, inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inspection submitted successfully.")
                .build());
    }

    @PostMapping("/{inspectionId}/image")
    @Operation(summary = "Upload inspection step image")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long inspectionId,
            @RequestParam("category") String category,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        Inspector inspector = getInspector(userDetails);
        
        // Create directory if not exists
        String targetDir = uploadDir + "/" + carImageFolder;
        File uploadFolder = new File(targetDir);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path targetPath = Paths.get(targetDir).resolve(uniqueFilename);

        // Copy file to disk
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Construct serving URL
        String fileUrl = baseUrl + "/" + uploadDir + "/" + carImageFolder + "/" + uniqueFilename;

        inspectionService.uploadInspectionImage(inspectionId, category, originalFilename, fileUrl, inspector.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<String>builder()
                .success(true)
                .message("Image uploaded successfully.")
                .data(fileUrl)
                .build());
    }

    @GetMapping("/image/{filename:.+}")
    @Operation(summary = "Retrieve static uploaded inspection images")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = Paths.get(uploadDir).resolve(carImageFolder).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{inspectionId}/pdf")
    @Operation(summary = "Download inspection summary report PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long inspectionId) {
        byte[] pdfBytes = inspectionService.generatePdfReport(inspectionId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inspection_report_" + inspectionId + ".pdf\"")
                .body(pdfBytes);
    }

    @PutMapping("/{inspectionId}")
    @Operation(summary = "Update an existing vehicle inspection draft by ID")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> updateInspection(
            @PathVariable Long inspectionId,
            @RequestBody InspectionDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getInspector(userDetails);
        InspectionDetailsResponse response = inspectionService.updateInspection(inspectionId, request, inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Inspection updated successfully.")
                .data(response)
                .build());
    }

    @DeleteMapping("/{inspectionId}")
    @Operation(summary = "Delete vehicle inspection draft by ID")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(
            @PathVariable Long inspectionId) {
        
        inspectionService.deleteInspection(inspectionId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Inspection draft deleted successfully.")
                .build());
    }

    private Inspector getInspector(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return inspectorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile not found for email: " + email));
    }
}
