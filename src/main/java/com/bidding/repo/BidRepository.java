package com.bidding.repo;

import com.bidding.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByInspectionIdOrderByAmountDesc(Long inspectionId);
    Optional<Bid> findFirstByInspectionIdOrderByAmountDesc(Long inspectionId);
    long countByInspectionId(Long inspectionId);
    List<Bid> findByDealerEmailOrderByCreatedAtDesc(String email);
    long countByDealerId(Long dealerId);
}
