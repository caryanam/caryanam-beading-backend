package com.bidding.dto.responce;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponseDTO {
    private String dealer;
    private Long dealerId;
    private String dealerEmail;
    private String dealerName;
    private Double amount;
    private String time;
}
