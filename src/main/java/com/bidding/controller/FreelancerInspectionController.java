package com.bidding.controller;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.ApiResponse;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.FreelancerVehicleResponse;
import com.bidding.dto.responce.InspectorStatsResponse;
import com.bidding.entity.Dealer;
import com.bidding.entity.Inspector;
import com.bidding.enums.Role;
import com.bidding.exception.ResourceNotFoundException;
import com.bidding.repo.DealerRepository;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping({"/api/freelancer/inspection", "/api/freelancer/vehicles", "/api/freelancer"})
@RequiredArgsConstructor
@Tag(name = "Freelancer Vehicle Inspection API", description = "Endpoints for freelancer vehicle submission management")
public class FreelancerInspectionController {

    private final InspectionService inspectionService;
    private final InspectorRepository inspectorRepository;
    private final DealerRepository dealerRepository;
    private final com.bidding.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir}")
    private String uploadDir;

    @org.springframework.beans.factory.annotation.Value("${app.car-image-folder}")
    private String carImageFolder;

    @org.springframework.beans.factory.annotation.Value("${app.car-video-folder}")
    private String carVideoFolder;

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications for logged-in freelancer")
    public ResponseEntity<ApiResponse<List<com.bidding.dto.responce.NotificationDTO>>> getFreelancerNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Inspector inspector = getFreelancer(userDetails);
        List<com.bidding.dto.responce.NotificationDTO> list = notificationService.getInspectorNotifications(inspector.getEmail());
        return ResponseEntity.ok(ApiResponse.<List<com.bidding.dto.responce.NotificationDTO>>builder()
                .success(true)
                .message("Freelancer notifications retrieved successfully.")
                .data(list)
                .build());
    }

    @RequestMapping(value = "/notifications/{id}/read", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET})
    @Operation(summary = "Mark single freelancer notification as read")
    public ResponseEntity<ApiResponse<Void>> markFreelancerNotificationAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Notification marked as read successfully.")
                .build());
    }

    @RequestMapping(value = "/notifications/mark-all-read", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET})
    @Operation(summary = "Mark all freelancer notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllFreelancerNotificationsAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Inspector inspector = getFreelancer(userDetails);
        notificationService.markAllAsReadForInspector(inspector.getEmail());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("All freelancer notifications marked as read.")
                .build());
    }

    @GetMapping("")
    @Operation(summary = "Get list of freelancer vehicle submissions (Freelancer ID-wise, or all for admin/marketplace)")
    public ResponseEntity<ApiResponse<List<FreelancerVehicleResponse>>> getMyInspections(
            @RequestParam(value = "freelancerId", required = false) Long freelancerId,
            @RequestParam(value = "inspectorId", required = false) Long inspectorId,
            @RequestParam(value = "all", required = false) Boolean all,
            @RequestParam(value = "scope", required = false) String scope,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 1. Explicitly requesting all submissions (e.g., admin or dealer marketplace)
        if (Boolean.TRUE.equals(all) || "all".equalsIgnoreCase(scope)) {
            boolean isAdmin = userDetails != null && userDetails.getAuthorities() != null &&
                userDetails.getAuthorities().stream().anyMatch(a -> 
                    a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || 
                    a.getAuthority().equalsIgnoreCase("ADMIN")
                );
            List<FreelancerVehicleResponse> listResponse;
            if (isAdmin) {
                listResponse = inspectionService.getAllFreelancerSubmissionsForAdmin();
            } else {
                String username = userDetails != null ? userDetails.getUsername() : null;
                listResponse = inspectionService.getAllFreelancerSubmissionsForDealer(username);
            }
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("All freelancer submissions retrieved successfully.")
                    .data(listResponse)
                    .build());
        }

        // 2. Specific freelancer ID requested
        Long targetId = freelancerId != null ? freelancerId : inspectorId;
        if (targetId != null) {
            Long resolvedId = resolveFreelancerInspectorId(targetId);
            if (resolvedId != null) {
                List<FreelancerVehicleResponse> response = inspectionService.getFreelancerSubmissions(resolvedId);
                return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                        .success(true)
                        .message("Freelancer submissions retrieved successfully for id: " + targetId)
                        .data(response)
                        .build());
            } else {
                return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                        .success(true)
                        .message("No freelancer submissions found.")
                        .data(Collections.emptyList())
                        .build());
            }
        }

        // 3. Unauthenticated request -> default to public/admin live list
        if (userDetails == null) {
            List<FreelancerVehicleResponse> adminResponse = inspectionService.getAllFreelancerSubmissionsForAdmin();
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("All freelancer submissions retrieved successfully.")
                    .data(adminResponse)
                    .build());
        }

        // 4. If caller is strictly ADMIN (has ROLE_ADMIN or ADMIN, and not ROLE_FREELANCER)
        boolean isAdmin = userDetails.getAuthorities() != null &&
            userDetails.getAuthorities().stream().anyMatch(a -> 
                a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || 
                a.getAuthority().equalsIgnoreCase("ADMIN")
            );
        boolean hasFreelancerRole = userDetails.getAuthorities() != null &&
            userDetails.getAuthorities().stream().anyMatch(a -> 
                a.getAuthority().equalsIgnoreCase("ROLE_FREELANCER") || 
                a.getAuthority().equalsIgnoreCase("FREELANCER")
            );

        if (isAdmin && !hasFreelancerRole) {
            List<FreelancerVehicleResponse> adminResponse = inspectionService.getAllFreelancerSubmissionsForAdmin();
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("All freelancer submissions retrieved successfully.")
                    .data(adminResponse)
                    .build());
        }

        // 5. Authenticated freelancer (individual freelancer OR dealer made as freelancer)
        try {
            Inspector inspector = getFreelancer(userDetails);
            List<FreelancerVehicleResponse> response = inspectionService.getFreelancerSubmissions(inspector.getId());
            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("Submissions retrieved successfully.")
                    .data(response)
                    .build());
        } catch (Exception e) {
            // If user is a pure dealer who was not made freelancer and has no inspector profile,
            // fallback to public/admin marketplace view so dealer marketplace screen does not crash
            boolean isDealer = userDetails.getAuthorities() != null &&
                userDetails.getAuthorities().stream().anyMatch(a -> 
                    a.getAuthority().equalsIgnoreCase("ROLE_DEALER") || 
                    a.getAuthority().equalsIgnoreCase("DEALER")
                );
            if (isDealer) {
                List<FreelancerVehicleResponse> dealerResponse = inspectionService.getAllFreelancerSubmissionsForDealer(userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                        .success(true)
                        .message("All freelancer submissions retrieved successfully.")
                        .data(dealerResponse)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<List<FreelancerVehicleResponse>>builder()
                    .success(true)
                    .message("No submissions found.")
                    .data(Collections.emptyList())
                    .build());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get submission stats for logged-in freelancer")
    public ResponseEntity<ApiResponse<InspectorStatsResponse>> getFreelancerStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getFreelancer(userDetails);
        InspectorStatsResponse response = inspectionService.getInspectorStats(inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<InspectorStatsResponse>builder()
                .success(true)
                .message("Stats retrieved successfully.")
                .data(response)
                .build());
    }

    @PostMapping("")
    @Operation(summary = "Create a new vehicle draft for freelancer")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> createDraft(
            @RequestBody InspectionDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getFreelancer(userDetails);
        InspectionDetailsResponse response = inspectionService.saveDraft(request, inspector.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Vehicle draft created successfully.")
                .data(response)
                .build());
    }

    @GetMapping("/{inspectionId}")
    @Operation(summary = "Get vehicle submission details by ID")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> getInspectionDetails(
            @PathVariable Long inspectionId) {
        
        InspectionDetailsResponse response = inspectionService.getInspection(inspectionId);
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Inspection details loaded successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/{inspectionId}/submit")
    @Operation(summary = "Submit vehicle report for admin approval")
    public ResponseEntity<ApiResponse<Void>> submitInspection(
            @PathVariable Long inspectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getFreelancer(userDetails);
        inspectionService.submitFreelancerInspection(inspectionId, inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Vehicle submitted for admin approval successfully.")
                .build());
    }

    @PostMapping("/{inspectionId}/image")
    @Operation(summary = "Upload basic vehicle photo or walkaround video")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long inspectionId,
            @RequestParam("category") String category,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        Inspector inspector = getFreelancer(userDetails);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
        String lowerName = originalFilename.toLowerCase();
        boolean isVideo = category.equalsIgnoreCase("Walkaround Video") || category.toLowerCase().contains("video") || lowerName.matches(".*\\.(mp4|webm|mov|avi|mkv|3gp|flv|wmv)$");

        if (!isVideo) {
            boolean isAvif = lowerName.endsWith(".avif") || (file.getContentType() != null && file.getContentType().contains("avif"));
            boolean isAllowed = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")
                    || (file.getContentType() != null && (file.getContentType().contains("jpeg") || file.getContentType().contains("png") || file.getContentType().contains("webp")));

            if (isAvif || !isAllowed) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Invalid file format. Please upload only JPG, JPEG, or PNG format image.")
                        .build());
            }
        }
        
        String targetDir = uploadDir + "/" + carImageFolder;
        File uploadFolder = new File(targetDir);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        String ext = isVideo ? (originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".mp4") : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + ext;
        Path targetPath = Paths.get(targetDir).resolve(uniqueFilename);

        if (isVideo) {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            boolean converted = false;
            try (InputStream in = file.getInputStream()) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
                if (img != null) {
                    java.awt.image.BufferedImage rgbImage = new java.awt.image.BufferedImage(
                        img.getWidth(), img.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB
                    );
                    java.awt.Graphics2D g = rgbImage.createGraphics();
                    g.drawImage(img, 0, 0, java.awt.Color.WHITE, null);
                    g.dispose();
                    javax.imageio.ImageIO.write(rgbImage, "jpg", targetPath.toFile());
                    converted = true;
                }
            } catch (Exception ignored) {}

            if (!converted) {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        String fileUrl = baseUrl + "/" + uploadDir + "/" + carImageFolder + "/" + uniqueFilename;
        inspectionService.uploadInspectionImage(inspectionId, category, originalFilename, fileUrl, inspector.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<String>builder()
                .success(true)
                .message("Image uploaded successfully.")
                .data(fileUrl)
                .build());
    }

    @PutMapping("/{inspectionId}")
    @Operation(summary = "Update an existing vehicle draft by ID")
    public ResponseEntity<ApiResponse<InspectionDetailsResponse>> updateInspection(
            @PathVariable Long inspectionId,
            @RequestBody InspectionDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Inspector inspector = getFreelancer(userDetails);
        InspectionDetailsResponse response = inspectionService.updateInspection(inspectionId, request, inspector.getId());
        
        return ResponseEntity.ok(ApiResponse.<InspectionDetailsResponse>builder()
                .success(true)
                .message("Vehicle updated successfully.")
                .data(response)
                .build());
    }

    @DeleteMapping("/{inspectionId}")
    @Operation(summary = "Delete vehicle draft by ID")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(
            @PathVariable Long inspectionId) {
        
        inspectionService.deleteInspection(inspectionId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Vehicle draft deleted successfully.")
                .build());
    }

    private Long resolveFreelancerInspectorId(Long targetId) {
        if (targetId == null) return null;

        // 1. Direct search in inspector table
        Optional<Inspector> inspectorOpt = inspectorRepository.findById(targetId);
        if (inspectorOpt.isPresent()) {
            return inspectorOpt.get().getId();
        }

        // 2. Check if targetId is a Dealer ID who was made freelancer
        Optional<Dealer> dealerOpt = dealerRepository.findById(targetId);
        if (dealerOpt.isPresent()) {
            Dealer dealer = dealerOpt.get();
            String email = (dealer.getEmail() != null && !dealer.getEmail().trim().isEmpty())
                    ? dealer.getEmail().trim()
                    : null;
            String mobile = dealer.getMobileNumber();

            Optional<Inspector> linked = Optional.empty();
            if (email != null) {
                linked = inspectorRepository.findByEmailOrMobileNumber(email, mobile);
            } else if (mobile != null) {
                linked = inspectorRepository.findByMobileNumber(mobile);
            }
            if (!linked.isPresent() && dealer.getEmail() != null) {
                linked = inspectorRepository.findByEmail(dealer.getEmail());
            }
            if (!linked.isPresent() && dealer.getMobileNumber() != null) {
                linked = inspectorRepository.findByMobileNumber(dealer.getMobileNumber());
            }
            if (linked.isPresent()) {
                Inspector ins = linked.get();
                if (ins.getEmail() != null && ins.getEmail().endsWith("@caryanam.com")) {
                    ins.setEmail(null);
                    inspectorRepository.save(ins);
                }
                return ins.getId();
            }
            if (Boolean.TRUE.equals(dealer.getIsFreelancer())) {
                Inspector newFreelancer = Inspector.builder()
                        .fullName(dealer.getOwnerName() != null ? dealer.getOwnerName() : dealer.getDealershipName())
                        .email(email)
                        .mobileNumber(mobile)
                        .password(dealer.getPassword())
                        .role(Role.FREELANCER)
                        .createdAt(LocalDateTime.now())
                        .build();
                newFreelancer = inspectorRepository.save(newFreelancer);
                return newFreelancer.getId();
            }
        }

        return targetId;
    }

    private Inspector getFreelancer(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new ResourceNotFoundException("Freelancer account not authenticated");
        }
        String identifier = userDetails.getUsername().trim();

        // 1. Search in inspector table by email or mobile number
        Optional<Inspector> inspector = inspectorRepository.findByEmailOrMobileNumber(identifier, identifier);
        if (inspector.isPresent() && inspector.get().getRole() == Role.FREELANCER) {
            return inspector.get();
        }

        // 2. Search in dealer table (for dealer converted to freelancer)
        Optional<Dealer> dealerOpt = dealerRepository.findByEmailOrMobileNumber(identifier, identifier);
        if (dealerOpt.isPresent()) {
            Dealer dealer = dealerOpt.get();
            String email = (dealer.getEmail() != null && !dealer.getEmail().trim().isEmpty())
                    ? dealer.getEmail().trim()
                    : null;
            String mobile = dealer.getMobileNumber();

            Optional<Inspector> linkedInspector = Optional.empty();
            if (email != null) {
                linkedInspector = inspectorRepository.findByEmailOrMobileNumber(email, mobile);
            } else if (mobile != null) {
                linkedInspector = inspectorRepository.findByMobileNumber(mobile);
            }
            if (!linkedInspector.isPresent() && dealer.getEmail() != null) {
                linkedInspector = inspectorRepository.findByEmail(dealer.getEmail());
            }
            if (!linkedInspector.isPresent() && dealer.getMobileNumber() != null) {
                linkedInspector = inspectorRepository.findByMobileNumber(dealer.getMobileNumber());
            }
            if (linkedInspector.isPresent()) {
                Inspector ins = linkedInspector.get();
                if (ins.getEmail() != null && ins.getEmail().endsWith("@caryanam.com")) {
                    ins.setEmail(null);
                    inspectorRepository.save(ins);
                }
                return ins;
            }

            // If dealer is marked as freelancer, create inspector record if not yet created
            if (Boolean.TRUE.equals(dealer.getIsFreelancer())) {
                Inspector newFreelancer = Inspector.builder()
                        .fullName(dealer.getOwnerName() != null ? dealer.getOwnerName() : dealer.getDealershipName())
                        .email(email)
                        .mobileNumber(mobile)
                        .password(dealer.getPassword())
                        .role(Role.FREELANCER)
                        .createdAt(LocalDateTime.now())
                        .build();
                return inspectorRepository.save(newFreelancer);
            }
        }

        // 3. Fallback search in inspector table
        if (inspector.isPresent()) {
            return inspector.get();
        }

        return inspectorRepository.findByEmail(identifier)
                .orElseGet(() -> inspectorRepository.findByMobileNumber(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException("Freelancer account not found for: " + identifier)));
    }
}