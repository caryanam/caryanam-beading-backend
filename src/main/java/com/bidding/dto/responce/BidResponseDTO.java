package com.bidding.dto.responce;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponseDTO {
    private String dealer;
    private Double amount;
    private String time;
}
