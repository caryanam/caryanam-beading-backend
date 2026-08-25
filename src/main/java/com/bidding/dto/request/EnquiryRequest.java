package com.bidding.dto.request;

import lombok.Data;

@Data
public class EnquiryRequest {
    private String name;
    private String email;
    private String phone;
    private String message;
}
