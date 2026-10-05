package com.bidding.repo;

import com.bidding.entity.AuctionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuctionMessageRepository extends JpaRepository<AuctionMessage, Long> {
    List<AuctionMessage> findByInspectionIdOrderByCreatedAtAsc(Long inspectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AuctionMessage am WHERE am.inspectionId = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
