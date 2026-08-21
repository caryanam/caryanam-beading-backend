package com.bidding.dto.responce;

import com.bidding.enums.InspectionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerVehicleResponse {

    private Long id;
    private Long inspectionId;
    private String registrationNumber;
    private String vehicleNumber;
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
    private String ownerName;
    private String insuranceStatus;
    private Double suggestedPrice;
    private String location;
    private String underHypothecation;
    private String accidental;
    private String rtoInformation;
    private InspectionStatus status;
    private String rejectionReason;
    private String vehicleImage;
    private List<String> photos;
    private String videoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private String freelancerName;
    private String inspectorName;
}