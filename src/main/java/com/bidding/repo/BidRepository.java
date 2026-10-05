package com.bidding.repo;

import com.bidding.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByInspectionIdOrderByAmountDesc(Long inspectionId);
    Optional<Bid> findFirstByInspectionIdOrderByAmountDesc(Long inspectionId);
    long countByInspectionId(Long inspectionId);
    List<Bid> findByDealerEmailOrderByCreatedAtDesc(String email);
    List<Bid> findByDealerIdOrderByCreatedAtDesc(Long dealerId);
    long countByDealerId(Long dealerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Bid b WHERE b.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
