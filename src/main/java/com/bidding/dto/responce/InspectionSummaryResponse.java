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
    private String customerMobileNumber;
    private String brand;
    private String model;
    private String variant;
    private InspectionStatus status;
    private LocalDateTime submittedAt;
    private String inspectorName;
    private Double suggestedPrice;
    private String rejectionReason;
    private String vehicleImage;
    private Integer year;
    private Integer manufacturingYear;
    private Integer registrationYear;
    private String fuel;
    private String transmission;
    private Integer odometer;
    private String vehicleStatus;
    private Double currentHighestBid;
    private String currentHighestBidder;
    private Long auctionEndTime;
    private Integer totalBids;
    private Boolean sellerAgreed;
    private Double sellerCounterPrice;
    private String sellerMessage;
    private String adminDealerMessage;
    private String dealerReplyMessage;
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
