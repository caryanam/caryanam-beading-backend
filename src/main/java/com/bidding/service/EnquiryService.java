package com.bidding.service;

import com.bidding.dto.request.EnquiryRequest;
import com.bidding.dto.responce.EnquiryResponse;
import java.util.List;

public interface EnquiryService {
    void submitEnquiry(EnquiryRequest request);
    List<EnquiryResponse> getAllEnquiries();
}
