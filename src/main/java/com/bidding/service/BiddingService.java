package com.bidding.service;

import com.bidding.dto.responce.BidResponseDTO;
import com.bidding.dto.responce.DealerBidResponseDTO;
import java.util.List;

public interface BiddingService {
    void placeBid(Long inspectionId, String dealerEmail, Double amount);
    List<BidResponseDTO> getBidHistory(Long inspectionId);
    List<DealerBidResponseDTO> getDealerBidHistory(String dealerEmail);
}
