package com.bidding.dto.responce;

import com.bidding.enums.InspectionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionSummaryResponse {

    private Long inspectionId;
    private String vehicleNumber;
    private String ownerName;
    private String brand;
    private String model;
    private String variant;
    private InspectionStatus status;
    private LocalDateTime submittedAt;
    private String inspectorName;
}
