package com.bidding.dto.responce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerBidResponseDTO {
    private String id;
    private String vehicleId;
    private String brand;
    private String model;
    private String regNo;
    private Double myBid;
    private Double highestBid;
    private Integer totalBids;
    private String timestamp;
    private String status;
    private String auction; // live or completed
}
