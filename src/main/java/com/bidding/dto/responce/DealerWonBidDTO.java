package com.bidding.dto.responce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerWonBidDTO {
    private Long vehicleId;
    private String vehicleNumber;
    private String brand;
    private String model;
    private String variant;
    private Double winningBidAmount;
    private String status;
}
