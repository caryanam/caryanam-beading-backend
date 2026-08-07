package com.bidding.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponseRequest {
    private Boolean agreed;
    private Double counterPrice;
    private String message;
}
