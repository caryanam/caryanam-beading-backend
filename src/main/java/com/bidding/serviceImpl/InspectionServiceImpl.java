package com.bidding.serviceImpl;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.entity.*;
import com.bidding.enums.InspectionStatus;
import com.bidding.enums.PanelCondition;
import com.bidding.exception.ResourceNotFoundException;
import com.bidding.repo.*;
import com.bidding.service.InspectionService;
import com.bidding.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir}")
    private String uploadDir;

    @org.springframework.beans.factory.annotation.Value("${app.car-image-folder}")
    private String carImageFolder;

    @org.springframework.beans.factory.annotation.Value("${app.car-video-folder}")
    private String carVideoFolder;

    @Override
    @Transactional
    public InspectionDetailsResponse saveDraft(InspectionDraftRequest request, Long inspectorId) {
        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found"));

        Inspection inspection;
        Vehicle vehicle;

        if (request.getInspectionId() != null) {
            inspection = inspectionRepository.findById(request.getInspectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

            if (inspection.getStatus() != InspectionStatus.DRAFT && 
                inspection.getStatus() != InspectionStatus.IN_PROGRESS) {
                throw new IllegalStateException("Inspection is locked and cannot be updated.");
            }
            vehicle = inspection.getVehicle();
        } else {
            // Check by vehicle number
            String vehicleNumber = request.getVehicleDetails() != null ? 
                    request.getVehicleDetails().getVehicleNumber() : null;

            if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Vehicle number is required to save a draft.");
            }

            Optional<Inspection> existingInspection = inspectionRepository.findByVehicleVehicleNumber(vehicleNumber);
            if (existingInspection.isPresent()) {
                inspection = existingInspection.get();
                if (inspection.getStatus() != InspectionStatus.DRAFT && 
                    inspection.getStatus() != InspectionStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Inspection for this vehicle is locked.");
                }
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
        }

        // 1. Update Vehicle Specs
        if (request.getVehicleDetails() != null) {
            InspectionDraftRequest.VehicleDraftDTO vDto = request.getVehicleDetails();
            if (vDto.getOwnerName() != null) vehicle.setOwnerName(vDto.getOwnerName());
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

        // 4. Update Tyres
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

        if (inspection.getStatus() != InspectionStatus.DRAFT && 
            inspection.getStatus() != InspectionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Inspection has already been submitted or finalized.");
        }

        // Validate specifications
        Vehicle vehicle = inspection.getVehicle();
        if (vehicle == null || vehicle.getBrand() == null || vehicle.getModel() == null || 
            vehicle.getVariant() == null || vehicle.getOdometerReading() == null) {
            throw new IllegalArgumentException("Validation Failed: Vehicle specifications must be completed before final submit.");
        }

        // Validate Ratings
        if (inspection.getExteriorRating() == null || inspection.getMechanicalRating() == null || 
            inspection.getTyreRating() == null || inspection.getInteriorRating() == null) {
            throw new IllegalArgumentException("Validation Failed: Ratings for all sections must be set.");
        }

        // Validate 31 panels completed
        List<InspectionPanel> panels = inspectionPanelRepository.findByInspectionId(id);
        if (panels == null || panels.size() < 31) {
            throw new IllegalArgumentException("Validation Failed: All 31 exterior panels must be rated before submit.");
        }

        // Validate mechanical checklist
        MechanicalInspection mechanical = mechanicalInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Mechanical checklist must be completed."));

        if (mechanical.getEngineStatus() == null || mechanical.getEngineOil() == null || 
            mechanical.getBrakeWorking() == null || mechanical.getSuspension() == null) {
            throw new IllegalArgumentException("Validation Failed: Mandatory mechanical check options are empty.");
        }

        // Validate tyre checklist
        TyreInspection tyres = tyreInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Tyre specifications must be completed."));

        if (tyres.getFrontLeftBrand() == null || tyres.getFrontLeftTread() == null ||
            tyres.getFrontRightBrand() == null || tyres.getFrontRightTread() == null ||
            tyres.getRearLeftBrand() == null || tyres.getRearLeftTread() == null ||
            tyres.getRearRightBrand() == null || tyres.getRearRightTread() == null ||
            tyres.getSpareBrand() == null || tyres.getSpareTread() == null) {
            throw new IllegalArgumentException("Validation Failed: Tyre details for all positions are mandatory.");
        }

        // Validate interior checklist
        InteriorInspection interior = interiorInspectionRepository.findByInspectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Validation Failed: Interior & Electronics checklist must be completed."));

        if (interior.getBatteryBrand() == null || interior.getAcCooling() == null) {
            throw new IllegalArgumentException("Validation Failed: Battery and AC details must be completed.");
        }

        // Validate mandatory images uploaded
        List<InspectionImage> images = inspectionImageRepository.findByInspectionId(id);
        Set<String> uploadedCategories = images.stream().map(InspectionImage::getImageCategory).collect(Collectors.toSet());
        List<String> mandatoryCategories = List.of(
                "Front", "Rear", "Left", "Right", "Roof", "Dashboard", "Engine", "Tyres", "Interior",
                "Front Left", "Front Right", "Rear Left", "Rear Right", "Spare",
                "Instrument Cluster", "AC Control", "Music System", "Odometer"
        );

        for (String mCat : mandatoryCategories) {
            if (!uploadedCategories.contains(mCat)) {
                throw new IllegalArgumentException("Validation Failed: Mandatory category image is missing: " + mCat);
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
                .map(ins -> {
                    Vehicle v = ins.getVehicle();
                    return InspectionSummaryResponse.builder()
                            .inspectionId(ins.getId())
                            .vehicleNumber(v != null ? v.getVehicleNumber() : "N/A")
                            .ownerName(v != null ? v.getOwnerName() : "N/A")
                            .brand(v != null ? v.getBrand() : "N/A")
                            .model(v != null ? v.getModel() : "N/A")
                            .variant(v != null ? v.getVariant() : "N/A")
                            .status(ins.getStatus())
                            .submittedAt(ins.getSubmittedAt())
                            .inspectorName(ins.getInspector() != null ? ins.getInspector().getFullName() : "N/A")
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
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        List<InspectionPanel> panels = inspectionPanelRepository.findByInspectionId(id);
        MechanicalInspection mechanical = mechanicalInspectionRepository.findByInspectionId(id).orElse(null);
        TyreInspection tyres = tyreInspectionRepository.findByInspectionId(id).orElse(null);
        InteriorInspection interior = interiorInspectionRepository.findByInspectionId(id).orElse(null);
        List<InspectionImage> images = inspectionImageRepository.findByInspectionId(id);
        InspectionRemarks remarks = inspectionRemarksRepository.findByInspectionId(id).orElse(null);

        return pdfGeneratorService.generateInspectionPdf(inspection, panels, mechanical, tyres, interior, images, remarks);
    }

    @Override
    @Transactional
    public void uploadInspectionImage(Long id, String category, String originalName, String fileUrl, Long inspectorId) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (inspection.getStatus() != InspectionStatus.DRAFT && 
            inspection.getStatus() != InspectionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Inspection is closed and cannot upload more images.");
        }

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found"));

        // Delete existing category image if any to avoid duplicates
        inspectionImageRepository.deleteByInspectionIdAndImageCategory(id, category);

        InspectionImage image = InspectionImage.builder()
                .inspection(inspection)
                .imageCategory(category)
                .imageUrl(fileUrl)
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

    private InspectionDetailsResponse mapToDetailsResponse(Inspection ins, 
                                                           List<InspectionPanel> panels,
                                                           MechanicalInspection mechanical, 
                                                           TyreInspection tyres, 
                                                           InteriorInspection interior, 
                                                           List<InspectionImage> images) {
        Vehicle v = ins.getVehicle();
        
        String defaultVideoUrl = baseUrl + "/" + uploadDir + "/" + carVideoFolder + "/car.mp4";
        List<InspectionDetailsResponse.VideoResponseDTO> videoList = Collections.singletonList(
                InspectionDetailsResponse.VideoResponseDTO.builder()
                        .id(1L)
                        .videoUrl(defaultVideoUrl)
                        .build()
        );

        return InspectionDetailsResponse.builder()
                .inspectionId(ins.getId())
                .status(ins.getStatus())
                .inspectionStatus(ins.getStatus() != null ? ins.getStatus().name() : null)
                .rejectionReason(ins.getRejectionReason())
                .submittedAt(ins.getSubmittedAt())
                .inspectorId(ins.getInspector() != null ? ins.getInspector().getId() : null)
                .inspectorName(ins.getInspector() != null ? ins.getInspector().getFullName() : null)
                
                // Flat vehicle fields
                .vehicleNumber(v == null ? null : v.getVehicleNumber())
                .ownerName(v == null ? null : v.getOwnerName())
                .brand(v == null ? null : v.getBrand())
                .model(v == null ? null : v.getModel())
                .variant(v == null ? null : v.getVariant())
                .manufacturingYear(v == null ? null : v.getManufacturingYear())
                .fuelType(v == null ? null : v.getFuelType())
                .transmission(v == null ? null : v.getTransmission())
                .odometerReading(v == null ? null : v.getOdometerReading())
                .insuranceStatus(v == null ? null : v.getInsuranceStatus())

                .exteriorRating(ins.getExteriorRating())
                .interiorRating(ins.getInteriorRating())
                .engineRating(ins.getMechanicalRating())
                .mechanicalRating(ins.getMechanicalRating())
                .tyreRating(ins.getTyreRating())
                .createdAt(ins.getCreatedAt())
                .updatedAt(ins.getUpdatedAt())

                .vehicleDetails(v == null ? null : InspectionDetailsResponse.VehicleResponseDTO.builder()
                        .id(v.getId())
                        .vehicleNumber(v.getVehicleNumber())
                        .ownerName(v.getOwnerName())
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
                        .build())
                .exteriorPanelDetails(panels.stream().map(p -> InspectionDetailsResponse.PanelResponseDTO.builder()
                        .id(p.getId())
                        .panelName(p.getPanelName())
                        .condition(p.getCondition())
                        .build()).collect(Collectors.toList()))
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
                .images(images.stream().map(img -> {
                    String rawUrl = img.getImageUrl();
                    String finalUrl = rawUrl;
                    if (rawUrl != null && !rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                        if (rawUrl.startsWith("/api/inspector/inspection/image/")) {
                            String filename = rawUrl.substring(rawUrl.lastIndexOf("/") + 1);
                            finalUrl = baseUrl + "/" + uploadDir + "/" + carImageFolder + "/" + filename;
                        } else if (rawUrl.startsWith("uploads/")) {
                            finalUrl = baseUrl + "/" + rawUrl;
                        } else {
                            finalUrl = baseUrl + "/" + uploadDir + "/" + carImageFolder + "/" + rawUrl;
                        }
                    }
                    return InspectionDetailsResponse.ImageResponseDTO.builder()
                            .id(img.getId())
                            .category(img.getImageCategory())
                            .imageUrl(finalUrl)
                            .build();
                }).collect(Collectors.toList()))
                .videos(videoList)
                .build();
    }

    @Override
    @Transactional
    public InspectionDetailsResponse updateInspection(Long id, InspectionDraftRequest request, Long inspectorId) {
        request.setInspectionId(id);
        return saveDraft(request, inspectorId);
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
}
