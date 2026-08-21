package com.bidding.repo;

import com.bidding.entity.InspectionRemarks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectionRemarksRepository extends JpaRepository<InspectionRemarks, Long> {
    Optional<InspectionRemarks> findByInspectionId(Long inspectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InspectionRemarks ir WHERE ir.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
