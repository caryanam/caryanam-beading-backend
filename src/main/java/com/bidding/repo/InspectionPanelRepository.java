package com.bidding.repo;

import com.bidding.entity.InspectionPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionPanelRepository extends JpaRepository<InspectionPanel, Long> {
    List<InspectionPanel> findByInspectionId(Long inspectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InspectionPanel ip WHERE ip.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
