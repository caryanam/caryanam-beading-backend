package com.bidding.repo;

import com.bidding.entity.AuctionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionMessageRepository extends JpaRepository<AuctionMessage, Long> {
    List<AuctionMessage> findByInspectionIdOrderByCreatedAtAsc(Long inspectionId);
}
