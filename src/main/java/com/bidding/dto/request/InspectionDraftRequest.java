package com.bidding.dto.request;

import com.bidding.enums.PanelCondition;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionDraftRequest {

    private VehicleDraftDTO vehicleDetails;
    private List<PanelDraftDTO> exteriorPanelDetails;
    private MechanicalDraftDTO mechanicalDetails;
    private TyreDraftDTO tyreDetails;
    private InteriorDraftDTO interiorDetails;

    private Double exteriorRating;
    private Double mechanicalRating;
    private Double tyreRating;
    private Double interiorRating;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleDraftDTO {
        private String vehicleNumber;
        private String ownerName;
        private String customerName;
        private String customerMobileNumber;
        private String brand;
        private String model;
        private String variant;
        private Integer manufacturingYear;
        private Integer registrationYear;
        private String fuelType;
        private String transmission;
        private Integer odometerReading;
        private String insuranceStatus;
        private String inspectorCode;
        private LocalDateTime inspectionDate;
                private Double suggestedPrice;
        private String location;
        private String rtoInformation;
        private String rsAvailability;
        private String duplicateKey;
        private String rtoNocIssued;
        private String underHypothecation;
        private String accidental;
        private String mismatchInRc;
        private String roadTaxPaid;
        private String fitnessUpto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PanelDraftDTO {
        private String panelName;
        private PanelCondition condition;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MechanicalDraftDTO {
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
    public static class TyreDraftDTO {
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
    public static class InteriorDraftDTO {
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
}
