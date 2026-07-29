package com.bidding.dto.responce;

import com.bidding.enums.InspectionStatus;
import com.bidding.enums.PanelCondition;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionDetailsResponse {

    private Long inspectionId;
    
    // Vehicle details flat fields
    private String vehicleNumber;
    private String ownerName;
    private String brand;
    private String model;
    private String variant;
    private Integer manufacturingYear;
    private String fuelType;
    private String transmission;
    private Integer odometerReading;
    private String insuranceStatus;
    
    // Status flat field
    private String inspectionStatus;

    private InspectionStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private Long inspectorId;
    private String inspectorName;

    private VehicleResponseDTO vehicleDetails;
    private List<PanelResponseDTO> exteriorPanelDetails;
    private MechanicalResponseDTO mechanicalDetails;
    private TyreResponseDTO tyreDetails;
    private InteriorResponseDTO interiorDetails;
    private List<ImageResponseDTO> images;
    @Builder.Default
    private List<VideoResponseDTO> videos = java.util.Collections.emptyList();

    private Double exteriorRating;
    private Double interiorRating;
    private Double engineRating;
    private Double mechanicalRating;
    private Double tyreRating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleResponseDTO {
        private Long id;
        private String vehicleNumber;
        private String ownerName;
        private String brand;
        private String model;
        private String variant;
        private Integer manufacturingYear;
        private String fuelType;
        private String transmission;
        private Integer odometerReading;
        private String insuranceStatus;
        private String inspectorCode;
        private LocalDateTime inspectionDate;
        private String vehicleStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PanelResponseDTO {
        private Long id;
        private String panelName;
        private PanelCondition condition;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MechanicalResponseDTO {
        private Long id;
        private String engineStatus;
        private String engineOil;
        private String brakeOil;
        private String steeringOil;
        private String coolant;
        private String brakeBooster;
        private String brakeWorking;
        private String apron;
        private String chassis;
        private String suspension;
        private String bush;
        private String leakage;
        private String transmission;
        private String gearbox;
        private String differential;
        private String axle;
        private String engineNoise;
        private String smoke;
        private String fluidLeakage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TyreResponseDTO {
        private Long id;
        
        private String frontLeftBrand;
        private Integer frontLeftYear;
        private Integer frontLeftTread;

        private String frontRightBrand;
        private Integer frontRightYear;
        private Integer frontRightTread;

        private String rearLeftBrand;
        private Integer rearLeftYear;
        private Integer rearLeftTread;

        private String rearRightBrand;
        private Integer rearRightYear;
        private Integer rearRightTread;

        private String spareBrand;
        private Integer spareYear;
        private Integer spareTread;

        private Boolean hasJack;
        private Boolean hasHandle;
        private Boolean hasToolkit;
        private Boolean hasTriangle;
        private Boolean hasFirstAidBox;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteriorResponseDTO {
        private Long id;
        private String batteryBrand;
        private String batterySerialNumber;
        private String acCooling;
        private Double evaluatorValuation;
        private String rightTailLamp;
        private String leftTailLamp;
        private String rightHeadLamp;
        private String leftHeadLamp;
        private String indicators;
        private String bootFloor;
        private String dashboard;
        private String fogLamps;
        private String powerWindows;
        private String musicSystem;
        private String steeringMountedControls;
        private String wiper;
        private String rearDefogger;
        private String rearWasher;
        private String instrumentCluster;
        private String infotainment;
        private String centralLock;
        private String pushButton;
        private String sunroof;
        private String sensors;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageResponseDTO {
        private Long id;
        private String category;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoResponseDTO {
        private Long id;
        private String videoUrl;
    }
}
