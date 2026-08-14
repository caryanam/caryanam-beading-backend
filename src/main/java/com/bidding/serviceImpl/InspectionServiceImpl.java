package com.bidding.serviceImpl;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.dto.responce.InspectorStatsResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.BidResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.entity.*;
import com.bidding.config.AuctionWebSocketHandler;
import com.bidding.enums.InspectionStatus;
import com.bidding.enums.PhotoType;
import com.bidding.enums.VideoType;
import com.bidding.exception.ResourceNotFoundException;
import com.bidding.repo.*;
import com.bidding.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final VehicleRepository vehicleRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectorRepository inspectorRepository;
    private final InspectionImageRepository inspectionImageRepository;
    private final InspectionPanelRepository inspectionPanelRepository;
    private final MechanicalInspectionRepository mechanicalInspectionRepository;
    private final TyreInspectionRepository tyreInspectionRepository;
    private final InteriorInspectionRepository interiorInspectionRepository;
    private final InspectionRemarksRepository inspectionRemarksRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final DealerRepository dealerRepository;
    private final BidRepository bidRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuctionWebSocketHandler webSocketHandler;
    private final com.bidding.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir}")
    private String uploadDir;

    @org.springframework.beans.factory.annotation.Value("${app.car-image-folder}")
    private String carImageFolder;

    @org.springframework.beans.factory.annotation.Value("${app.car-video-folder}")
    private String carVideoFolder;

    public String cleanRelativePath(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String clean = url.trim();

        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            int uploadsIdx = clean.indexOf("/uploads/");
            if (uploadsIdx != -1) {
                return clean.substring(uploadsIdx);
            }
            int apiIdx = clean.indexOf("/api/");
            if (apiIdx != -1) {
                return clean.substring(apiIdx);
            }
            int slashIdx = clean.indexOf('/', 8);
            if (slashIdx != -1) {
                return clean.substring(slashIdx);
            }
        }

        if (!clean.startsWith("/")) {
            return "/" + clean;
        }
        return clean;
    }

    public String buildFullImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }
        String relativePath = cleanRelativePath(rawUrl);
        if (relativePath == null) {
            return null;
        }
        if (relativePath.startsWith("/api/inspector/inspection/image/")) {
            String filename = relativePath.substring(relativePath.lastIndexOf("/") + 1);
            return baseUrl + "/" + uploadDir + "/" + carImageFolder + "/" + filename;
        }
        return baseUrl + relativePath;
    }

    @Override
    @Transactional
    public InspectionDetailsResponse saveDraft(InspectionDraftRequest request, Long inspectorId) {
        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found"));

        Inspection inspection;
        Vehicle vehicle;

        // Check by vehicle number
        String vehicleNumber = request.getVehicleDetails() != null ? 
                request.getVehicleDetails().getVehicleNumber() : null;

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle number is required to save a draft.");
        }

        Optional<Inspection> existingInspection = inspectionRepository.findByVehicleVehicleNumber(vehicleNumber);
        if (existingInspection.isPresent()) {
            inspection = existingInspection.get();
            vehicle = inspection.getVehicle();
        } else {
            vehicle = Vehicle.builder()
                    .vehicleNumber(vehicleNumber)
                    .build();
            vehicle = vehicleRepository.save(vehicle);

            inspection = Inspection.builder()
                    .vehicle(vehicle)
                    .inspector(inspector)
                    .status(InspectionStatus.DRAFT)
                    .build();
            inspection = inspectionRepository.save(inspection);
        }

        return saveOrUpdateInspection(inspection, vehicle, request);
    }

    private InspectionDetailsResponse saveOrUpdateInspection(Inspection inspection, Vehicle vehicle, InspectionDraftRequest request) {
        // 1. Update Vehicle Specs
        if (request.getVehicleDetails() != null) {
            InspectionDraftRequest.VehicleDraftDTO vDto = request.getVehicleDetails();
            if (vDto.getOwnerName() != null) vehicle.setOwnerName(vDto.getOwnerName());
            if (vDto.getCustomerName() != null) vehicle.setCustomerName(vDto.getCustomerName());
            if (vDto.getCustomerMobileNumber() != null) vehicle.setCustomerMobileNumber(vDto.getCustomerMobileNumber());
            if (vDto.getBrand() != null) vehicle.setBrand(vDto.getBrand());
            if (vDto.getModel() != null) vehicle.setModel(vDto.getModel());
            if (vDto.getVariant() != null) vehicle.setVariant(vDto.getVariant());
            if (vDto.getManufacturingYear() != null) vehicle.setManufacturingYear(vDto.getManufacturingYear());
            if (vDto.getFuelType() != null) vehicle.setFuelType(vDto.getFuelType());
            if (vDto.getTransmission() != null) vehicle.setTransmission(vDto.getTransmission());
            if (vDto.getOdometerReading() != null) vehicle.setOdometerReading(vDto.getOdometerReading());
            if (vDto.getInsuranceStatus() != null) vehicle.setInsuranceStatus(vDto.getInsuranceStatus());
            if (vDto.getInspectorCode() != null) vehicle.setInspectorCode(vDto.getInspectorCode());
            if (vDto.getInspectionDate() != null) vehicle.setInspectionDate(vDto.getInspectionDate());
            if (vDto.getSuggestedPrice() != null) vehicle.setSuggestedPrice(vDto.getSuggestedPrice());
            vehicleRepository.save(vehicle);
        }

        // Update Ratings
        if (request.getExteriorRating() != null) inspection.setExteriorRating(request.getExteriorRating());
        if (request.getMechanicalRating() != null) inspection.setMechanicalRating(request.getMechanicalRating());
        if (request.getTyreRating() != null) inspection.setTyreRating(request.getTyreRating());
        if (request.getInteriorRating() != null) inspection.setInteriorRating(request.getInteriorRating());
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);

        // 2. Update Panels
        if (request.getExteriorPanelDetails() != null) {
            inspectionPanelRepository.deleteByInspectionId(inspection.getId());
            for (InspectionDraftRequest.PanelDraftDTO pDto : request.getExteriorPanelDetails()) {
                if (pDto.getPanelName() != null && pDto.getCondition() != null) {
                    InspectionPanel panel = InspectionPanel.builder()
                            .inspection(inspection)
                            .panelName(pDto.getPanelName())
                            .condition(pDto.getCondition())
                            .imageUrl(cleanRelativePath(pDto.getImageUrl()))
                            .build();
                    inspectionPanelRepository.save(panel);
                }
            }
        }

        // 3. Update Mechanical Checks
        if (request.getMechanicalDetails() != null) {
            mechanicalInspectionRepository.deleteByInspectionId(inspection.getId());
            InspectionDraftRequest.MechanicalDraftDTO mDto = request.getMechanicalDetails();
            MechanicalInspection mechanical = MechanicalInspection.builder()
                    .inspection(inspection)
                    .engineStatus(mDto.getEngineStatus())
                    .engineOil(mDto.getEngineOil())
                    .brakeOil(mDto.getBrakeOil())
                    .steeringOil(mDto.getSteeringOil())
                    .coolant(mDto.getCoolant())
                    .brakeBooster(mDto.getBrakeBooster())
                    .brakeWorking(mDto.getBrakeWorking())
                    .apron(mDto.getApron())
                    .chassis(mDto.getChassis())
                    .suspension(mDto.getSuspension())
                    .bush(mDto.getBush())
                    .leakage(mDto.getLeakage())
                    .transmission(mDto.getTransmission())
                    .gearbox(mDto.getGearbox())
                    .differential(mDto.getDifferential())
                    .axle(mDto.getAxle())
                    .engineNoise(mDto.getEngineNoise())
                    .smoke(mDto.getSmoke())
                    .fluidLeakage(mDto.getFluidLeakage())
                    .build();
            mechanicalInspectionRepository.save(mechanical);
        }

        // 4. Update Tyre Details
        if (request.getTyreDetails() != null) {
            tyreInspectionRepository.deleteByInspectionId(inspection.getId());
            InspectionDraftRequest.TyreDraftDTO tDto = request.getTyreDetails();
            TyreInspection tyres = TyreInspection.builder()
                    .inspection(inspection)
                    .frontLeftBrand(tDto.getFrontLeftBrand())
                    .frontLeftYear(tDto.getFrontLeftYear())
                    .frontLeftTread(tDto.getFrontLeftTread())
                    .frontRightBrand(tDto.getFrontRightBrand())
                    .frontRightYear(tDto.getFrontRightYear())
                    .frontRightTread(tDto.getFrontRightTread())
                    .rearLeftBrand(tDto.getRearLeftBrand())
                    .rearLeftYear(tDto.getRearLeftYear())
                    .rearLeftTread(tDto.getRearLeftTread())
                    .rearRightBrand(tDto.getRearRightBrand())
                    .rearRightYear(tDto.getRearRightYear())
                    .rearRightTread(tDto.getRearRightTread())
                    .spareBrand(tDto.getSpareBrand())
                    .spareYear(tDto.getSpareYear())
                    .spareTread(tDto.getSpareTread())
                    .hasJack(tDto.getHasJack())
                    .hasHandle(tDto.getHasHandle())
                    .hasToolkit(tDto.getHasToolkit())
                    .hasTriangle(tDto.getHasTriangle())
                    .hasFirstAidBox(tDto.getHasFirstAidBox())
                    .build();
            tyreInspectionRepository.save(tyres);
        }

        // 5. Update Interior & Remarks
        if (request.getInteriorDetails() != null) {
            interiorInspectionRepository.deleteByInspectionId(inspection.getId());
            inspectionRemarksRepository.deleteByInspectionId(inspection.getId());
            InspectionDraftRequest.InteriorDraftDTO iDto = request.getInteriorDetails();

            InteriorInspection interior = InteriorInspection.builder()
                    .inspection(inspection)
                    .batteryBrand(iDto.getBatteryBrand())
                    .batterySerialNumber(iDto.getBatterySerialNumber())
                    .acCooling(iDto.getAcCooling())
                    .evaluatorValuation(iDto.getEvaluatorValuation())
                    .rightTailLamp(iDto.getRightTailLamp())
                    .leftTailLamp(iDto.getLeftTailLamp())
                    .rightHeadLamp(iDto.getRightHeadLamp())
                    .leftHeadLamp(iDto.getLeftHeadLamp())
                    .indicators(iDto.getIndicators())
                    .bootFloor(iDto.getBootFloor())
                    .dashboard(iDto.getDashboard())
                    .fogLamps(iDto.getFogLamps())
                    .powerWindows(iDto.getPowerWindows())
                    .musicSystem(iDto.getMusicSystem())
                    .steeringMountedControls(iDto.getSteeringMountedControls())
                    .wiper(iDto.getWiper())
                    .rearDefogger(iDto.getRearDefogger())
                    .rearWasher(iDto.getRearWasher())
                    .instrumentCluster(iDto.getInstrumentCluster())
                    .infotainment(iDto.getInfotainment())
                    .centralLock(iDto.getCentralLock())
                    .pushButton(iDto.getPushButton())
                    .sunroof(iDto.getSunroof())
                    .sensors(iDto.getSensors())
                    .remarks(iDto.getRemarks())
                    .build();
            interiorInspectionRepository.save(interior);

            if (iDto.getRemarks() != null && !iDto.getRemarks().trim().isEmpty()) {
                InspectionRemarks remarkObj = InspectionRemarks.builder()
                        .inspection(inspection)
                        .inspectorRemarks(iDto.getRemarks())
                        .build();
                inspectionRemarksRepository.save(remarkObj);
            }
        }

        return getInspection(inspection.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionDetailsResponse getInspection(Long id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        List<InspectionPanel> panels = inspectionPanelRepository.findByInspectionId(id);
        MechanicalInspection mechanical = mechanicalInspectionRepository.findByInspectionId(id).orElse(null);
        TyreInspection tyres = tyreInspectionRepository.findByInspectionId(id).orElse(null);
        InteriorInspection interior = interiorInspectionRepository.findByInspectionId(id).orElse(null);
        List<InspectionImage> images = inspectionImageRepository.findByInspectionId(id);

        return mapToDetailsResponse(inspection, panels, mechanical, tyres, interior, images);
    }

    @Override
    @Transactional
    public void submitInspection(Long id, Long inspectorId) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        // Validate specifications
        Vehicle vehicle = inspection.getVehicle();
        if (vehicle == null) {
            throw new IllegalArgumentException("Validation Failed: Vehicle specifications must be completed.");
        }
        if (vehicle.getBrand() == null || vehicle.getBrand().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Vehicle Brand / Make is required.");
        if (vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Vehicle Model is required.");
        if (vehicle.getVariant() == null || vehicle.getVariant().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Vehicle Variant is required.");
        if (vehicle.getOdometerReading() == null) throw new IllegalArgumentException("Validation Failed: Odometer Reading is required.");
        if (vehicle.getOwnerName() == null || vehicle.getOwnerName().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Owner Profile Status is required.");
        if (vehicle.getManufacturingYear() == null) throw new IllegalArgumentException("Validation Failed: Manufacturing Year is required.");
        if (vehicle.getFuelType() == null || vehicle.getFuelType().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Fuel Type is required.");
        if (vehicle.getTransmission() == null || vehicle.getTransmission().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Transmission is required.");
        if (vehicle.getInsuranceStatus() == null || vehicle.getInsuranceStatus().trim().isEmpty()) {
            vehicle.setInsuranceStatus("Comprehensive");
        }
        if (vehicle.getInspectorCode() == null || vehicle.getInspectorCode().trim().isEmpty()) {
            String defaultCode = (inspection.getInspector() != null && inspection.getInspector().getEmail() != null)
                    ? inspection.getInspector().getEmail()
                    : "INSP-" + (inspectorId != null ? inspectorId : "001");
            vehicle.setInspectorCode(defaultCode);
        }
        if (vehicle.getSuggestedPrice() == null) {
            vehicle.setSuggestedPrice(500000.0);
        }
        vehicleRepository.save(vehicle);

        // Validate Ratings
        if (inspection.getExteriorRating() == null) throw new IllegalArgumentException("Validation Failed: Exterior Rating is required.");
        if (inspection.getMechanicalRating() == null) throw new IllegalArgumentException("Validation Failed: Mechanical Rating is required.");
        if (inspection.getTyreRating() == null) throw new IllegalArgumentException("Validation Failed: Tyre Rating is required.");
        if (inspection.getInteriorRating() == null) throw new IllegalArgumentException("Validation Failed: Interior Rating is required.");

        // Validate 31 panels completed
        List<InspectionPanel> panels = inspectionPanelRepository.findByInspectionId(id);
        if (panels == null || panels.size() < 31) {
            throw new IllegalArgumentException("Validation Failed: All 31 exterior panels must be rated before submit.");
        }

        // Validate mechanical checklist
        MechanicalInspection mechanical = mechanicalInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Mechanical checklist must be completed."));

        if (mechanical.getEngineStatus() == null) throw new IllegalArgumentException("Validation Failed: Engine / Motor Status check is required.");
        if (mechanical.getEngineOil() == null) throw new IllegalArgumentException("Validation Failed: Engine Oil check is required.");
        if (mechanical.getBrakeOil() == null) throw new IllegalArgumentException("Validation Failed: Brakes Oil check is required.");
        if (mechanical.getSteeringOil() == null) throw new IllegalArgumentException("Validation Failed: Steering Oil check is required.");
        if (mechanical.getCoolant() == null) throw new IllegalArgumentException("Validation Failed: Coolant check is required.");
        if (mechanical.getBrakeBooster() == null) throw new IllegalArgumentException("Validation Failed: Brakes Booster check is required.");
        if (mechanical.getBrakeWorking() == null) throw new IllegalArgumentException("Validation Failed: Brakes Working check is required.");
        if (mechanical.getApron() == null) throw new IllegalArgumentException("Validation Failed: Apron Condition check is required.");
        if (mechanical.getChassis() == null) throw new IllegalArgumentException("Validation Failed: Chassis Alignment check is required.");
        if (mechanical.getSuspension() == null) throw new IllegalArgumentException("Validation Failed: Suspension check is required.");
        if (mechanical.getBush() == null) throw new IllegalArgumentException("Validation Failed: Suspension Bushing check is required.");
        if (mechanical.getLeakage() == null) throw new IllegalArgumentException("Validation Failed: Oil Leakage check is required.");
        if (mechanical.getTransmission() == null) throw new IllegalArgumentException("Validation Failed: Manual Transmission Fluid Level check is required.");
        if (mechanical.getGearbox() == null) throw new IllegalArgumentException("Validation Failed: Steering Gearbox & Linkage check is required.");
        if (mechanical.getDifferential() == null) throw new IllegalArgumentException("Validation Failed: Differential Fluid Level check is required.");
        if (mechanical.getAxle() == null) throw new IllegalArgumentException("Validation Failed: Driveline / Axle check is required.");
        if (mechanical.getEngineNoise() == null) throw new IllegalArgumentException("Validation Failed: Engine / Motor Noise check is required.");
        if (mechanical.getSmoke() == null) throw new IllegalArgumentException("Validation Failed: Exhaust Smoke Color check is required.");
        if (mechanical.getFluidLeakage() == null) throw new IllegalArgumentException("Validation Failed: Fluid Leakages check is required.");

        // Validate tyre checklist
        TyreInspection tyres = tyreInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Tyre specifications must be completed."));

        if (tyres.getFrontLeftBrand() == null || tyres.getFrontLeftTread() == null) throw new IllegalArgumentException("Validation Failed: Front Left Tyre details are required.");
        if (tyres.getFrontRightBrand() == null || tyres.getFrontRightTread() == null) throw new IllegalArgumentException("Validation Failed: Front Right Tyre details are required.");
        if (tyres.getRearLeftBrand() == null || tyres.getRearLeftTread() == null) throw new IllegalArgumentException("Validation Failed: Rear Left Tyre details are required.");
        if (tyres.getRearRightBrand() == null || tyres.getRearRightTread() == null) throw new IllegalArgumentException("Validation Failed: Rear Right Tyre details are required.");
        if (tyres.getSpareBrand() == null || tyres.getSpareTread() == null) throw new IllegalArgumentException("Validation Failed: Spare Tyre details are required.");
        if (tyres.getHasJack() == null) throw new IllegalArgumentException("Validation Failed: Mechanical Jack Present check is required.");
        if (tyres.getHasHandle() == null) throw new IllegalArgumentException("Validation Failed: Wrench & Handle Present check is required.");
        if (tyres.getHasToolkit() == null) throw new IllegalArgumentException("Validation Failed: Standard Tool Kit Present check is required.");
        if (tyres.getHasTriangle() == null) throw new IllegalArgumentException("Validation Failed: Reflective Hazard Triangle check is required.");
        if (tyres.getHasFirstAidBox() == null) throw new IllegalArgumentException("Validation Failed: First Aid Kit check is required.");

        // Validate interior checklist
        InteriorInspection interior = interiorInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Interior & Electronics checklist must be completed."));

        if (interior.getBatteryBrand() == null || interior.getBatteryBrand().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Battery Company is required.");
        if (interior.getBatterySerialNumber() == null || interior.getBatterySerialNumber().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: Full Battery Number is required.");
        if (interior.getAcCooling() == null || interior.getAcCooling().trim().isEmpty()) throw new IllegalArgumentException("Validation Failed: AC cooling performance description is required.");
        if (interior.getRightTailLamp() == null) throw new IllegalArgumentException("Validation Failed: Right Side Tail Lamp check is required.");
        if (interior.getLeftTailLamp() == null) throw new IllegalArgumentException("Validation Failed: Left Side Tail Lamp check is required.");
        if (interior.getRightHeadLamp() == null) throw new IllegalArgumentException("Validation Failed: Right Side Head Light check is required.");
        if (interior.getLeftHeadLamp() == null) throw new IllegalArgumentException("Validation Failed: Left Side Head Light check is required.");
        if (interior.getIndicators() == null) throw new IllegalArgumentException("Validation Failed: Indicators check is required.");
        if (interior.getBootFloor() == null) throw new IllegalArgumentException("Validation Failed: Boot Floor check is required.");
        if (interior.getDashboard() == null) throw new IllegalArgumentException("Validation Failed: Dashboard check is required.");
        if (interior.getFogLamps() == null) throw new IllegalArgumentException("Validation Failed: Fog Lamps check is required.");
        if (interior.getPowerWindows() == null) throw new IllegalArgumentException("Validation Failed: Power Windows check is required.");
        if (interior.getMusicSystem() == null) throw new IllegalArgumentException("Validation Failed: Music System check is required.");
        if (interior.getSteeringMountedControls() == null) throw new IllegalArgumentException("Validation Failed: Steering Mounted Controls check is required.");
        if (interior.getWiper() == null) throw new IllegalArgumentException("Validation Failed: Wiper check is required.");
        if (interior.getRearDefogger() == null) throw new IllegalArgumentException("Validation Failed: Rear Defogger check is required.");
        if (interior.getRearWasher() == null) throw new IllegalArgumentException("Validation Failed: Rear Washer check is required.");
        if (interior.getInstrumentCluster() == null) throw new IllegalArgumentException("Validation Failed: Instrument Cluster check is required.");
        if (interior.getInfotainment() == null) throw new IllegalArgumentException("Validation Failed: Infotainment check is required.");
        if (interior.getCentralLock() == null) throw new IllegalArgumentException("Validation Failed: Central Lock check is required.");
        if (interior.getPushButton() == null) throw new IllegalArgumentException("Validation Failed: Push Start Button check is required.");
        if (interior.getSunroof() == null) throw new IllegalArgumentException("Validation Failed: Sunroof check is required.");
        if (interior.getSensors() == null) throw new IllegalArgumentException("Validation Failed: Sensors check is required.");

        // Validate mandatory images uploaded
        List<InspectionImage> images = inspectionImageRepository.findByInspectionId(id);
        for (PhotoType pt : PhotoType.values()) {
            boolean hasMatch = images.stream().anyMatch(img -> isCategoryMatch(img.getImageCategory(), pt));
            if (!hasMatch) {
                throw new IllegalArgumentException("Validation Failed: Mandatory category image is missing: " + pt.getDisplayName());
            }
        }

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found"));

        inspection.setStatus(InspectionStatus.SUBMITTED);
        inspection.setSubmittedAt(LocalDateTime.now());
        inspection.setSubmittedBy(inspector);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionSummaryResponse> getAllInspections() {
        return inspectionRepository.findAll().stream()
                .filter(ins -> ins.getStatus() != InspectionStatus.DRAFT && ins.getStatus() != InspectionStatus.IN_PROGRESS)
                .map(ins -> {
                    Vehicle v = ins.getVehicle();
                    List<InspectionImage> images = inspectionImageRepository.findByInspectionId(ins.getId());
                    String imgUrl = (images != null && !images.isEmpty()) ? buildFullImageUrl(images.get(0).getImageUrl()) : null;
                    return InspectionSummaryResponse.builder()
                            .inspectionId(ins.getId())
                            .vehicleNumber(v != null ? v.getVehicleNumber() : "N/A")
                            .ownerName(v != null ? v.getOwnerName() : "N/A")
                            .customerMobileNumber(v != null ? v.getCustomerMobileNumber() : null)
                            .brand(v != null ? v.getBrand() : "N/A")
                            .model(v != null ? v.getModel() : "N/A")
                            .variant(v != null ? v.getVariant() : "N/A")
                            .status(ins.getStatus())
                            .submittedAt(ins.getSubmittedAt())
                            .inspectorName(ins.getInspector() != null ? ins.getInspector().getFullName() : "N/A")
                            .suggestedPrice(v != null ? v.getSuggestedPrice() : null)
                            .rejectionReason(ins.getRejectionReason())
                            .vehicleImage(imgUrl)
                            .year(v != null ? v.getManufacturingYear() : null)
                            .fuel(v != null ? v.getFuelType() : "N/A")
                            .transmission(v != null ? v.getTransmission() : "N/A")
                            .odometer(v != null ? v.getOdometerReading() : null)
                            .vehicleStatus(v != null ? v.getVehicleStatus() : null)
                            .currentHighestBid(v != null ? v.getCurrentHighestBid() : null)
                            .currentHighestBidder((v != null && v.getCurrentHighestBidder() != null) ? v.getCurrentHighestBidder().getDealershipName() : null)
                            .auctionEndTime((v != null && v.getAuctionEndTime() != null) ? v.getAuctionEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                            .totalBids(v != null ? v.getTotalBids() : null)
                            .sellerAgreed(v != null ? v.getSellerAgreed() : null)
                            .sellerCounterPrice(v != null ? v.getSellerCounterPrice() : null)
                            .sellerMessage(v != null ? v.getSellerMessage() : null)
                            .adminDealerMessage(v != null ? v.getAdminDealerMessage() : null)
                            .dealerReplyMessage(v != null ? v.getDealerReplyMessage() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionSummaryResponse> getInspectionsByInspector(Long inspectorId) {
        return inspectionRepository.findByInspectorId(inspectorId).stream()
                .map(ins -> {
                    Vehicle v = ins.getVehicle();
                    List<InspectionImage> images = inspectionImageRepository.findByInspectionId(ins.getId());
                    String imgUrl = (images != null && !images.isEmpty()) ? buildFullImageUrl(images.get(0).getImageUrl()) : null;
                    return InspectionSummaryResponse.builder()
                            .inspectionId(ins.getId())
                            .vehicleNumber(v != null ? v.getVehicleNumber() : "N/A")
                            .ownerName(v != null ? v.getOwnerName() : "N/A")
                            .customerMobileNumber(v != null ? v.getCustomerMobileNumber() : null)
                            .brand(v != null ? v.getBrand() : "N/A")
                            .model(v != null ? v.getModel() : "N/A")
                            .variant(v != null ? v.getVariant() : "N/A")
                            .status(ins.getStatus())
                            .submittedAt(ins.getSubmittedAt())
                            .inspectorName(ins.getInspector() != null ? ins.getInspector().getFullName() : "N/A")
                            .suggestedPrice(v != null ? v.getSuggestedPrice() : null)
                            .rejectionReason(ins.getRejectionReason())
                            .vehicleImage(imgUrl)
                            .year(v != null ? v.getManufacturingYear() : null)
                            .fuel(v != null ? v.getFuelType() : "N/A")
                            .transmission(v != null ? v.getTransmission() : "N/A")
                            .odometer(v != null ? v.getOdometerReading() : null)
                            .vehicleStatus(v != null ? v.getVehicleStatus() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveInspection(Long id) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (ins.getStatus() != InspectionStatus.SUBMITTED) {
            throw new IllegalStateException("Inspection must be in SUBMITTED status to approve.");
        }

        ins.setStatus(InspectionStatus.APPROVED);
        ins.setRejectionReason(null);
        ins.setUpdatedAt(LocalDateTime.now());
        
        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setVehicleStatus("READY_FOR_AUCTION");
            vehicleRepository.save(v);
        }

        inspectionRepository.save(ins);
    }

    @Override
    @Transactional
    public void rejectInspection(Long id, String reason) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (ins.getStatus() != InspectionStatus.SUBMITTED) {
            throw new IllegalStateException("Inspection must be in SUBMITTED status to reject.");
        }

        ins.setStatus(InspectionStatus.REJECTED);
        ins.setRejectionReason(reason);
        ins.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(ins);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdfReport(Long id) {
        InspectionDetailsResponse details = getInspection(id);
        return pdfGeneratorService.generateInspectionPdfFromDto(details);
    }

    @Override
    @Transactional
    public void uploadInspectionImage(Long id, String category, String originalName, String fileUrl, Long inspectorId) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found"));

        // Delete existing category image if any to avoid duplicates
        inspectionImageRepository.deleteByInspectionIdAndImageCategory(id, category);

        InspectionImage image = InspectionImage.builder()
                .inspection(inspection)
                .imageCategory(category)
                .imageUrl(cleanRelativePath(fileUrl))
                .originalName(originalName)
                .inspector(inspector)
                .build();
        
        inspectionImageRepository.save(image);

        // Update status to IN_PROGRESS if DRAFT
        if (inspection.getStatus() == InspectionStatus.DRAFT) {
            inspection.setStatus(InspectionStatus.IN_PROGRESS);
            inspectionRepository.save(inspection);
        }
    }

    private boolean isCategoryMatch(String cat, PhotoType pt) {
        if (cat == null) return false;
        String cleanCat = cat.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String cleanPt = pt.name().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        if (cleanCat.equals(cleanPt)) return true;
        
        if (pt == PhotoType.FRONT_VIEW && (cleanCat.equals("FRONT") || cleanCat.equals("FRONTVIEW"))) return true;
        if (pt == PhotoType.REAR_VIEW && (cleanCat.equals("REAR") || cleanCat.equals("REARVIEW") || cleanCat.equals("BACK"))) return true;
        if (pt == PhotoType.LEFT_FRONT_VIEW && (cleanCat.equals("LEFT") || cleanCat.equals("LEFTFRONT") || cleanCat.equals("LEFTFRONTVIEW"))) return true;
        if (pt == PhotoType.RIGHT_FRONT_VIEW && (cleanCat.equals("RIGHT") || cleanCat.equals("RIGHTFRONT") || cleanCat.equals("RIGHTFRONTVIEW"))) return true;
        if (pt == PhotoType.ROOF_VIEW && (cleanCat.equals("ROOF") || cleanCat.equals("ROOFVIEW"))) return true;
        if (pt == PhotoType.ENGINE_IMAGE && (cleanCat.equals("ENGINE") || cleanCat.equals("ENGINEIMAGE") || cleanCat.contains("ENGINE"))) return true;
        if (pt == PhotoType.BATTERY_IMAGE && (cleanCat.equals("BATTERY") || cleanCat.equals("BATTERYIMAGE") || cleanCat.equals("BATTERYBAY") || cleanCat.contains("BATTERY"))) return true;
        if (pt == PhotoType.FRONT_RIGHT_TYRE && (cleanCat.contains("RFTYRE") || cleanCat.equals("FRONTRIGHT") || cleanCat.equals("FRONTRIGHTTYRE") || cleanCat.equals("RF"))) return true;
        if (pt == PhotoType.REAR_RIGHT_TYRE && (cleanCat.contains("RRTYRE") || cleanCat.equals("REARRIGHT") || cleanCat.equals("REARRIGHTTYRE") || cleanCat.equals("RR"))) return true;
        if (pt == PhotoType.FRONT_LEFT_TYRE && (cleanCat.contains("LFTYRE") || cleanCat.equals("FRONTLEFT") || cleanCat.equals("FRONTLEFTTYRE") || cleanCat.equals("LF"))) return true;
        if (pt == PhotoType.REAR_LEFT_TYRE && (cleanCat.contains("LRTYRE") || cleanCat.equals("REARLEFT") || cleanCat.equals("REARLEFTTYRE") || cleanCat.equals("LR"))) return true;
        if (pt == PhotoType.SPARE_WHEEL && (cleanCat.contains("SPARE") || cleanCat.equals("SPAREWHEEL") || cleanCat.equals("SPAREWHEELIMG"))) return true;
        if (pt == PhotoType.TYRES_OVERVIEW && (cleanCat.contains("OVERVIEW") || cleanCat.equals("TYRES") || cleanCat.equals("TYRESOVERVIEW") || cleanCat.equals("TYRESGENERALIMG"))) return true;
        if (pt == PhotoType.ODOMETER_IMAGE && (cleanCat.equals("ODOMETER") || cleanCat.equals("ODOMETERIMAGE") || cleanCat.equals("ODOMETERIMG") || cleanCat.contains("ODOMETER"))) return true;
        if (pt == PhotoType.DASHBOARD_IMAGE && (cleanCat.equals("DASHBOARD") || cleanCat.equals("DASHBOARDIMAGE") || cleanCat.equals("INTERIOR"))) return true;
        if (pt == PhotoType.AC_CONTROL_IMAGE && (cleanCat.contains("AC") || cleanCat.equals("ACCONTROL") || cleanCat.equals("ACCONTROLIMAGE") || cleanCat.equals("ACIMG"))) return true;
        
        return false;
    }

    private InspectionDetailsResponse mapToDetailsResponse(Inspection ins, 
                                                           List<InspectionPanel> panels,
                                                           MechanicalInspection mechanical, 
                                                           TyreInspection tyres, 
                                                           InteriorInspection interior, 
                                                           List<InspectionImage> images) {
        Vehicle v = ins.getVehicle();
        
        // 1. Map Photos list, dynamically matching all PhotoType enum values
        List<InspectionDetailsResponse.PhotoResponseDTO> photoList = new ArrayList<>();
        Set<Long> mappedImageIds = new HashSet<>();

        for (PhotoType pt : PhotoType.values()) {
            InspectionImage matchingImg = null;
            for (InspectionImage img : images) {
                if (isCategoryMatch(img.getImageCategory(), pt)) {
                    matchingImg = img;
                    break;
                }
            }

            if (matchingImg != null) {
                mappedImageIds.add(matchingImg.getId());
                String rawUrl = matchingImg.getImageUrl();
                String finalUrl = buildFullImageUrl(rawUrl);
                photoList.add(InspectionDetailsResponse.PhotoResponseDTO.builder()
                        .id(matchingImg.getId())
                        .photoType(pt.name())
                        .displayName(pt.getDisplayName())
                        .imageCategory(matchingImg.getImageCategory())
                        .imageUrl(finalUrl)
                        .captured(true)
                        .build());
            } else {
                photoList.add(InspectionDetailsResponse.PhotoResponseDTO.builder()
                        .id(null)
                        .photoType(pt.name())
                        .displayName(pt.getDisplayName())
                        .imageCategory(null)
                        .imageUrl(null)
                        .captured(false)
                        .build());
            }
        }

        // Include any remaining images uploaded for custom categories or specific panels
        for (InspectionImage img : images) {
            if (img.getId() != null && !mappedImageIds.contains(img.getId())) {
                String rawUrl = img.getImageUrl();
                String finalUrl = buildFullImageUrl(rawUrl);
                photoList.add(InspectionDetailsResponse.PhotoResponseDTO.builder()
                        .id(img.getId())
                        .photoType(null)
                        .displayName(img.getImageCategory())
                        .imageCategory(img.getImageCategory())
                        .imageUrl(finalUrl)
                        .captured(true)
                        .build());
            }
        }

        // 2. Map Videos list, dynamically matching all VideoType enum values
        List<InspectionDetailsResponse.VideoResponseDTO> videoList = new ArrayList<>();
        for (VideoType vt : VideoType.values()) {
            if (vt == VideoType.VEHICLE_WALKAROUND) {
                // Return default/placeholder walkaround video URL
                String defaultVideoUrl = baseUrl + "/" + uploadDir + "/" + carVideoFolder + "/car.mp4";
                videoList.add(InspectionDetailsResponse.VideoResponseDTO.builder()
                        .id(1L)
                        .videoType(vt.name())
                        .displayName(vt.getDisplayName())
                        .videoUrl(defaultVideoUrl)
                        .captured(true)
                        .build());
            } else {
                // Not captured
                videoList.add(InspectionDetailsResponse.VideoResponseDTO.builder()
                        .id(null)
                        .videoType(vt.name())
                        .displayName(vt.getDisplayName())
                        .videoUrl(null)
                        .captured(false)
                        .build());
            }
        }

        // 3. Map Ratings sub-DTO
        InspectionDetailsResponse.RatingsResponseDTO ratingsDto = InspectionDetailsResponse.RatingsResponseDTO.builder()
                .exterior(ins.getExteriorRating())
                .mechanical(ins.getMechanicalRating())
                .tyre(ins.getTyreRating())
                .interior(ins.getInteriorRating())
                .exteriorRating(ins.getExteriorRating())
                .mechanicalRating(ins.getMechanicalRating())
                .tyreRating(ins.getTyreRating())
                .interiorRating(ins.getInteriorRating())
                .build();

        List<BidResponseDTO> bids = bidRepository.findByInspectionIdOrderByAmountDesc(ins.getId()).stream()
                .map(b -> BidResponseDTO.builder()
                        .dealer(maskDealerName(b.getDealer() != null && b.getDealer().getDealershipName() != null ? b.getDealer().getDealershipName() : (b.getDealer() != null ? b.getDealer().getOwnerName() : "Dealer")))
                        .dealerId(b.getDealer() != null ? b.getDealer().getId() : null)
                        .dealerEmail(b.getDealer() != null ? b.getDealer().getEmail() : null)
                        .dealerName(b.getDealer() != null ? b.getDealer().getDealershipName() : null)
                        .amount(b.getAmount())
                        .time(formatTime(b.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        return InspectionDetailsResponse.builder()
                .inspectionId(ins.getId())
                .inspectionStatus(ins.getStatus() != null ? ins.getStatus().name() : null)
                .status(ins.getStatus())
                .rejectionReason(ins.getRejectionReason())
                .submittedAt(ins.getSubmittedAt())
                .inspectorId(ins.getInspector() != null ? ins.getInspector().getId() : null)
                .inspectorName(ins.getInspector() != null ? ins.getInspector().getFullName() : null)

                .vehicleDetails(v == null ? null : InspectionDetailsResponse.VehicleResponseDTO.builder()
                        .id(v.getId())
                        .vehicleNumber(v.getVehicleNumber())
                        .ownerName(v.getOwnerName())
                        .customerName(v.getCustomerName())
                        .customerMobileNumber(v.getCustomerMobileNumber())
                        .brand(v.getBrand())
                        .model(v.getModel())
                        .variant(v.getVariant())
                        .manufacturingYear(v.getManufacturingYear())
                        .fuelType(v.getFuelType())
                        .transmission(v.getTransmission())
                        .odometerReading(v.getOdometerReading())
                        .insuranceStatus(v.getInsuranceStatus())
                        .inspectorCode(v.getInspectorCode())
                        .inspectionDate(v.getInspectionDate())
                        .vehicleStatus(v.getVehicleStatus())
                        .suggestedPrice(v.getSuggestedPrice())
                        .currentHighestBid(v.getCurrentHighestBid())
                        .currentHighestBidder(v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getDealershipName() : null)
                        .currentHighestBidderId(v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getId() : null)
                        .currentHighestBidderEmail(v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getEmail() : null)
                        .auctionEndTime(v.getAuctionEndTime() != null ? v.getAuctionEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                        .totalBids(v.getTotalBids())
                        .sellerAgreed(v.getSellerAgreed())
                        .sellerCounterPrice(v.getSellerCounterPrice())
                        .sellerMessage(v.getSellerMessage())
                        .adminDealerMessage(v.getAdminDealerMessage())
                        .dealerReplyMessage(v.getDealerReplyMessage())
                        .build())
                .exteriorPanelDetails(panels.stream().map(p -> {
                    String panelImg = buildFullImageUrl(p.getImageUrl());
                    return InspectionDetailsResponse.PanelResponseDTO.builder()
                            .id(p.getId())
                            .panelName(p.getPanelName())
                            .condition(p.getCondition())
                            .imageUrl(panelImg != null && !panelImg.trim().isEmpty() ? panelImg : null)
                            .build();
                }).collect(Collectors.toList()))
                .mechanicalDetails(mechanical == null ? null : InspectionDetailsResponse.MechanicalResponseDTO.builder()
                        .id(mechanical.getId())
                        .engineStatus(mechanical.getEngineStatus())
                        .engineOil(mechanical.getEngineOil())
                        .brakeOil(mechanical.getBrakeOil())
                        .steeringOil(mechanical.getSteeringOil())
                        .coolant(mechanical.getCoolant())
                        .brakeBooster(mechanical.getBrakeBooster())
                        .brakeWorking(mechanical.getBrakeWorking())
                        .apron(mechanical.getApron())
                        .chassis(mechanical.getChassis())
                        .suspension(mechanical.getSuspension())
                        .bush(mechanical.getBush())
                        .leakage(mechanical.getLeakage())
                        .transmission(mechanical.getTransmission())
                        .gearbox(mechanical.getGearbox())
                        .differential(mechanical.getDifferential())
                        .axle(mechanical.getAxle())
                        .engineNoise(mechanical.getEngineNoise())
                        .smoke(mechanical.getSmoke())
                        .fluidLeakage(mechanical.getFluidLeakage())
                        .build())
                .tyreDetails(tyres == null ? null : InspectionDetailsResponse.TyreResponseDTO.builder()
                        .id(tyres.getId())
                        .frontLeftBrand(tyres.getFrontLeftBrand())
                        .frontLeftYear(tyres.getFrontLeftYear())
                        .frontLeftTread(tyres.getFrontLeftTread())
                        .frontRightBrand(tyres.getFrontRightBrand())
                        .frontRightYear(tyres.getFrontRightYear())
                        .frontRightTread(tyres.getFrontRightTread())
                        .rearLeftBrand(tyres.getRearLeftBrand())
                        .rearLeftYear(tyres.getRearLeftYear())
                        .rearLeftTread(tyres.getRearLeftTread())
                        .rearRightBrand(tyres.getRearRightBrand())
                        .rearRightYear(tyres.getRearRightYear())
                        .rearRightTread(tyres.getRearRightTread())
                        .spareBrand(tyres.getSpareBrand())
                        .spareYear(tyres.getSpareYear())
                        .spareTread(tyres.getSpareTread())
                        .hasJack(tyres.getHasJack())
                        .hasHandle(tyres.getHasHandle())
                        .hasToolkit(tyres.getHasToolkit())
                        .hasTriangle(tyres.getHasTriangle())
                        .hasFirstAidBox(tyres.getHasFirstAidBox())
                        .build())
                .interiorDetails(interior == null ? null : InspectionDetailsResponse.InteriorResponseDTO.builder()
                        .id(interior.getId())
                        .batteryBrand(interior.getBatteryBrand())
                        .batterySerialNumber(interior.getBatterySerialNumber())
                        .acCooling(interior.getAcCooling())
                        .evaluatorValuation(interior.getEvaluatorValuation())
                        .rightTailLamp(interior.getRightTailLamp())
                        .leftTailLamp(interior.getLeftTailLamp())
                        .rightHeadLamp(interior.getRightHeadLamp())
                        .leftHeadLamp(interior.getLeftHeadLamp())
                        .indicators(interior.getIndicators())
                        .bootFloor(interior.getBootFloor())
                        .dashboard(interior.getDashboard())
                        .fogLamps(interior.getFogLamps())
                        .powerWindows(interior.getPowerWindows())
                        .musicSystem(interior.getMusicSystem())
                        .steeringMountedControls(interior.getSteeringMountedControls())
                        .wiper(interior.getWiper())
                        .rearDefogger(interior.getRearDefogger())
                        .rearWasher(interior.getRearWasher())
                        .instrumentCluster(interior.getInstrumentCluster())
                        .infotainment(interior.getInfotainment())
                        .centralLock(interior.getCentralLock())
                        .pushButton(interior.getPushButton())
                        .sunroof(interior.getSunroof())
                        .sensors(interior.getSensors())
                        .remarks(interior.getRemarks())
                        .build())
                .inspectionPhotos(photoList)
                .inspectionVideos(videoList)
                .ratings(ratingsDto)
                .bidHistory(bids)
                .createdAt(ins.getCreatedAt())
                .updatedAt(ins.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public InspectionDetailsResponse updateInspection(Long id, InspectionDraftRequest request, Long inspectorId) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        Vehicle vehicle = inspection.getVehicle();
        return saveOrUpdateInspection(inspection, vehicle, request);
    }

    @Override
    @Transactional
    public void deleteInspection(Long id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        // Delete all dependent child records
        inspectionPanelRepository.deleteByInspectionId(id);
        mechanicalInspectionRepository.deleteByInspectionId(id);
        tyreInspectionRepository.deleteByInspectionId(id);
        interiorInspectionRepository.deleteByInspectionId(id);
        inspectionRemarksRepository.deleteByInspectionId(id);

        // Delete files from disk for this inspection
        List<InspectionImage> images = inspectionImageRepository.findByInspectionId(id);
        for (InspectionImage img : images) {
            String url = img.getImageUrl();
            if (url != null && url.contains("/image/")) {
                String filename = url.substring(url.lastIndexOf("/image/") + 7);
                try {
                    java.nio.file.Path filePath = java.nio.file.Paths.get("uploads/inspections/").resolve(filename);
                    java.nio.file.Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Delete image DB records
        inspectionImageRepository.deleteByInspectionId(id);

        // Delete inspection itself
        inspectionRepository.delete(inspection);

        // Delete vehicle
        Vehicle vehicle = inspection.getVehicle();
        if (vehicle != null) {
            vehicleRepository.delete(vehicle);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InspectorStatsResponse getInspectorStats(Long inspectorId) {
        List<Inspection> inspections = inspectionRepository.findByInspectorId(inspectorId);

        long pendingUploads = inspections.stream()
                .filter(ins -> ins.getStatus() == InspectionStatus.DRAFT || ins.getStatus() == InspectionStatus.IN_PROGRESS)
                .count();

        long completedReports = inspections.stream()
                .filter(ins -> ins.getStatus() == InspectionStatus.SUBMITTED || ins.getStatus() == InspectionStatus.APPROVED || ins.getStatus() == InspectionStatus.REJECTED)
                .count();

        long vehiclesSubmitted = inspections.stream()
                .filter(ins -> ins.getStatus() == InspectionStatus.SUBMITTED || ins.getStatus() == InspectionStatus.APPROVED || ins.getStatus() == InspectionStatus.REJECTED)
                .count();

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayInspections = inspections.stream()
                .filter(ins -> {
                    if (ins.getSubmittedAt() == null) {
                        return ins.getStatus() == InspectionStatus.DRAFT || ins.getStatus() == InspectionStatus.IN_PROGRESS;
                    }
                    return ins.getSubmittedAt().isAfter(todayStart);
                })
                .count();

        return InspectorStatsResponse.builder()
                .todayInspections(todayInspections)
                .pendingUploads(pendingUploads)
                .completedReports(completedReports)
                .vehiclesSubmitted(vehiclesSubmitted)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectorResponseDTO> getAllInspectors() {
        return inspectorRepository.findAll().stream()
                .map(inspector -> {
                    long uploads = inspectionRepository.findByInspectorId(inspector.getId()).size();
                    return InspectorResponseDTO.builder()
                            .id(inspector.getId())
                            .fullName(inspector.getFullName())
                            .email(inspector.getEmail())
                            .mobileNumber(inspector.getMobileNumber())
                            .role(inspector.getRole())
                            .uploads((int) uploads)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteInspector(Long id) {
        if (!inspectorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inspector not found with id: " + id);
        }
        inspectorRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealerResponseDTO> getAllDealers() {
        return dealerRepository.findAll().stream()
                .map(d -> {
                    List<com.bidding.dto.responce.DealerWonBidDTO> wonBids = getWonBidsForDealer(d.getId());
                    return DealerResponseDTO.builder()
                            .id(d.getId())
                            .dealershipName(d.getDealershipName())
                            .ownerName(d.getOwnerName())
                            .email(d.getEmail())
                            .mobileNumber(d.getMobileNumber())
                            .role(d.getRole())
                            .address(d.getAddress())
                            .area(d.getArea())
                            .city(d.getCity())
                            .totalBids(bidRepository.countByDealerId(d.getId()))
                            .wonBidsCount((long) wonBids.size())
                            .wonBids(wonBids)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DealerResponseDTO updateDealer(Long id, DealerResponseDTO dto) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + id));

        if (dto.getDealershipName() != null && !dto.getDealershipName().trim().isEmpty()) {
            dealer.setDealershipName(dto.getDealershipName().trim());
        }
        if (dto.getOwnerName() != null && !dto.getOwnerName().trim().isEmpty()) {
            dealer.setOwnerName(dto.getOwnerName().trim());
        }
        if (dto.getMobileNumber() != null && !dto.getMobileNumber().trim().isEmpty()) {
            dealer.setMobileNumber(dto.getMobileNumber().trim());
        }
        if (dto.getCity() != null) dealer.setCity(dto.getCity().trim());
        if (dto.getArea() != null) dealer.setArea(dto.getArea().trim());
        if (dto.getAddress() != null) dealer.setAddress(dto.getAddress().trim());
        dealer.setUpdatedAt(LocalDateTime.now());

        Dealer saved = dealerRepository.save(dealer);
        List<com.bidding.dto.responce.DealerWonBidDTO> wonBids = getWonBidsForDealer(saved.getId());

        return DealerResponseDTO.builder()
                .id(saved.getId())
                .dealershipName(saved.getDealershipName())
                .ownerName(saved.getOwnerName())
                .email(saved.getEmail())
                .mobileNumber(saved.getMobileNumber())
                .role(saved.getRole())
                .address(saved.getAddress())
                .area(saved.getArea())
                .city(saved.getCity())
                .totalBids(bidRepository.countByDealerId(saved.getId()))
                .wonBidsCount((long) wonBids.size())
                .wonBids(wonBids)
                .build();
    }

    private List<com.bidding.dto.responce.DealerWonBidDTO> getWonBidsForDealer(Long dealerId) {
        LocalDateTime now = LocalDateTime.now();
        return vehicleRepository.findAll().stream()
                .filter(v -> {
                    if (v.getCurrentHighestBidder() == null || !v.getCurrentHighestBidder().getId().equals(dealerId)) {
                        return false;
                    }
                    String status = v.getVehicleStatus();
                    if (status == null) {
                        return false;
                    }
                    // Exclude active/live auctions that have not ended yet
                    if ("LIVE".equalsIgnoreCase(status) && (v.getAuctionEndTime() == null || now.isBefore(v.getAuctionEndTime()))) {
                        return false;
                    }
                    if ("READY_FOR_AUCTION".equalsIgnoreCase(status) || "UPCOMING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                        return false;
                    }
                    return true;
                })
                .map(v -> com.bidding.dto.responce.DealerWonBidDTO.builder()
                        .vehicleId(v.getId())
                        .vehicleNumber(v.getVehicleNumber())
                        .brand(v.getBrand())
                        .model(v.getModel())
                        .variant(v.getVariant())
                        .winningBidAmount(v.getCurrentHighestBid())
                        .status(v.getVehicleStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDealer(Long id) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + id));

        // 1. Unlink vehicles where this dealer is set as current highest bidder
        List<Vehicle> vehiclesWithDealer = vehicleRepository.findAll().stream()
                .filter(v -> v.getCurrentHighestBidder() != null && v.getCurrentHighestBidder().getId().equals(id))
                .collect(Collectors.toList());
        for (Vehicle v : vehiclesWithDealer) {
            v.setCurrentHighestBidder(null);
            vehicleRepository.save(v);
        }

        // 2. Remove bids placed by this dealer to prevent foreign key errors
        List<Bid> dealerBids = bidRepository.findAll().stream()
                .filter(b -> b.getDealer() != null && b.getDealer().getId().equals(id))
                .collect(Collectors.toList());
        if (!dealerBids.isEmpty()) {
            bidRepository.deleteAll(dealerBids);
        }

        // 3. Remove wishlist items saved by this dealer
        List<Wishlist> dealerWishlist = wishlistRepository.findByDealerId(id);
        if (dealerWishlist != null && !dealerWishlist.isEmpty()) {
            wishlistRepository.deleteAll(dealerWishlist);
        }

        // 4. Delete dealer record
        dealerRepository.delete(dealer);
    }

    @Override
    @Transactional
    public void goLive(Long id) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setVehicleStatus("LIVE");
            v.setCurrentHighestBid(v.getSuggestedPrice() != null ? v.getSuggestedPrice() : 0.0);
            v.setCurrentHighestBidder(null);
            v.setAuctionEndTime(LocalDateTime.now().plusMinutes(10));
            v.setTotalBids(0);
            v = vehicleRepository.save(v);

            // Broadcast websocket message so dealers receive the go live event instantly
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "GO_LIVE");
            wsMessage.put("inspectionId", id);
            wsMessage.put("currentHighestBid", v.getCurrentHighestBid());
            wsMessage.put("currentHighestBidder", null);
            wsMessage.put("totalBids", v.getTotalBids());
            wsMessage.put("auctionEndTime", v.getAuctionEndTime() != null 
                    ? v.getAuctionEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() 
                    : null);
            wsMessage.put("bidHistory", new ArrayList<>());

            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    @Override
    @Transactional
    public void stopAuction(Long id) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setVehicleStatus("ENDED");
            v.setAuctionEndTime(LocalDateTime.now());
            v = vehicleRepository.save(v);

            // Broadcast websocket message so admin and dealers receive auction ended event
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "AUCTION_ENDED");
            wsMessage.put("inspectionId", id);
            wsMessage.put("winningBid", v.getCurrentHighestBid() != null ? v.getCurrentHighestBid() : 0.0);
            wsMessage.put("winner", v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getDealershipName() : "No winner");
            wsMessage.put("totalBids", v.getTotalBids() != null ? v.getTotalBids() : 0);

            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    @Override
    @Transactional
    public void submitSellerResponse(Long id, Boolean agreed, Double counterPrice, String message) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setSellerAgreed(agreed);
            v.setSellerCounterPrice(counterPrice);
            v.setSellerMessage(message);
            vehicleRepository.save(v);

            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());
            String desc = Boolean.TRUE.equals(agreed) ? "Agreed to highest bid" : (counterPrice != null ? "Counter offer â‚¹" + String.format("%,.0f", counterPrice) : "Rejected bid");
            notificationService.createNotification(
                    "ADMIN",
                    null,
                    id,
                    "ðŸ’¬ Seller Decision: " + vehicleTitle,
                    "Seller responded: " + desc + (message != null && !message.isEmpty() ? " ('" + message + "')" : ""),
                    "SELLER_RESPONSE"
            );

            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "SELLER_RESPONSE");
            wsMessage.put("inspectionId", id);
            wsMessage.put("sellerAgreed", agreed);
            wsMessage.put("sellerCounterPrice", counterPrice);
            wsMessage.put("sellerMessage", message);
            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    @Override
    @Transactional
    public void submitAdminDealerMessage(Long id, String message) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setAdminDealerMessage(message);
            vehicleRepository.save(v);

            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());
            if (v.getCurrentHighestBidder() != null) {
                notificationService.createNotification(
                        "DEALER",
                        v.getCurrentHighestBidder().getEmail(),
                        id,
                        "ðŸ’¬ Admin Negotiation Message: " + vehicleTitle,
                        "Admin sent message regarding " + vehicleTitle + ": '" + message + "'",
                        "ADMIN_MESSAGE"
                );
            }

            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "ADMIN_DEALER_MESSAGE");
            wsMessage.put("inspectionId", id);
            wsMessage.put("adminDealerMessage", message);
            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    @Override
    @Transactional
    public void submitDealerReply(Long id, String reply) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setDealerReplyMessage(reply);
            vehicleRepository.save(v);

            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());
            String dealerName = v.getCurrentHighestBidder() != null ? v.getCurrentHighestBidder().getDealershipName() : "Dealer";
            notificationService.createNotification(
                    "ADMIN",
                    null,
                    id,
                    "âœ‰ï¸ Dealer Reply Received: " + vehicleTitle,
                    "Dealer " + dealerName + " replied for " + vehicleTitle + ": '" + reply + "'",
                    "DEALER_REPLY"
            );

            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "DEALER_REPLY");
            wsMessage.put("inspectionId", id);
            wsMessage.put("dealerReplyMessage", reply);
            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    @Override
    @Transactional
    public void updateVehicleStatus(Long id, String vehicleStatus) {
        Inspection ins = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        Vehicle v = ins.getVehicle();
        if (v != null) {
            v.setVehicleStatus(vehicleStatus);
            if ("SOLD OUT".equalsIgnoreCase(vehicleStatus) || "SOLD".equalsIgnoreCase(vehicleStatus)) {
                if (v.getCurrentHighestBidder() == null) {
                    com.bidding.entity.Bid topBid = bidRepository.findFirstByInspectionIdOrderByAmountDesc(id).orElse(null);
                    if (topBid != null && topBid.getDealer() != null) {
                        v.setCurrentHighestBidder(topBid.getDealer());
                        if (v.getCurrentHighestBid() == null || v.getCurrentHighestBid() == 0.0) {
                            v.setCurrentHighestBid(topBid.getAmount());
                        }
                    }
                }
            }
            vehicleRepository.save(v);

            String vehicleTitle = String.format("%s %s (%s)", v.getBrand(), v.getModel(), v.getVehicleNumber());

            // Create notification for winning dealer and admin
            if ("SOLD OUT".equalsIgnoreCase(vehicleStatus) || "SOLD".equalsIgnoreCase(vehicleStatus)) {
                if (v.getCurrentHighestBidder() != null) {
                    notificationService.createNotification(
                            "DEALER",
                            v.getCurrentHighestBidder().getEmail(),
                            id,
                            "ðŸ† Auction Won: " + vehicleTitle,
                            "Congratulations! Vehicle " + vehicleTitle + " has been marked SOLD OUT to you for â‚¹" + String.format("%,.0f", v.getCurrentHighestBid() != null ? v.getCurrentHighestBid() : 0.0) + ".",
                            "AUCTION_WON"
                    );
                }
                notificationService.createNotification(
                        "ADMIN",
                        null,
                        id,
                        "ðŸ Vehicle Marked SOLD OUT: " + vehicleTitle,
                        "Vehicle " + vehicleTitle + " has been marked SOLD OUT.",
                        "STATUS_UPDATE"
                );
            } else if ("LIVE".equalsIgnoreCase(vehicleStatus)) {
                notificationService.createNotification(
                        "ALL_DEALERS",
                        null,
                        id,
                        "ðŸ”¥ Live Auction Started: " + vehicleTitle,
                        "Bidding is now LIVE for " + vehicleTitle + "! Place your bids now.",
                        "AUCTION_LIVE"
                );
            }

            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "VEHICLE_STATUS_UPDATE");
            wsMessage.put("inspectionId", id);
            wsMessage.put("vehicleStatus", vehicleStatus);
            webSocketHandler.broadcast(id, wsMessage);
        }
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "Just now";
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        java.time.Duration duration = java.time.Duration.between(time, now);
        long seconds = Math.abs(duration.getSeconds());
        String clockTime = time.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a"));
        if (seconds < 10) {
            return "Just now (" + clockTime + ")";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return (minutes <= 1 ? "1 min ago" : minutes + " min ago") + " (" + clockTime + ")";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " h ago (" + clockTime + ")";
        }
        return time.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"));
    }



    @Override
    @Transactional
    public void importDealers(org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            java.util.Iterator<org.apache.poi.ss.usermodel.Row> rows = sheet.iterator();
            
            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }
            
            while (rows.hasNext()) {
                org.apache.poi.ss.usermodel.Row row = rows.next();
                
                String ownerName = getCellValueAsString(row.getCell(0));
                String dealershipName = getCellValueAsString(row.getCell(1));
                String email = getCellValueAsString(row.getCell(2));
                String mobile = getCellValueAsString(row.getCell(3));
                String password = getCellValueAsString(row.getCell(4));
                String address = getCellValueAsString(row.getCell(5));
                String area = getCellValueAsString(row.getCell(6));
                String city = getCellValueAsString(row.getCell(7));
                
                if (email == null || email.trim().isEmpty()) {
                    continue;
                }
                
                if (dealerRepository.existsByEmail(email)) {
                    continue;
                }
                
                if (mobile != null && !mobile.trim().isEmpty() && dealerRepository.existsByMobileNumber(mobile)) {
                    continue;
                }
                
                Dealer dealer = Dealer.builder()
                        .ownerName(ownerName)
                        .dealershipName(dealershipName)
                        .email(email)
                        .mobileNumber(mobile)
                        .password(passwordEncoder.encode(password != null && !password.trim().isEmpty() ? password : "pass@123"))
                        .role(com.bidding.enums.Role.DEALER)
                        .address(address)
                        .area(area)
                        .city(city)
                        .build();
                        
                dealerRepository.save(dealer);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                java.text.DecimalFormat df = new java.text.DecimalFormat("#");
                return df.format(cell.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private String maskDealerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Dealer";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 2) {
            return trimmed + "****";
        }
        return trimmed.substring(0, 2) + "****";
    }
}

