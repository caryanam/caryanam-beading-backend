package com.bidding.repo;

import com.bidding.entity.MechanicalInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MechanicalInspectionRepository extends JpaRepository<MechanicalInspection, Long> {
    Optional<MechanicalInspection> findByInspectionId(Long inspectionId);

    @Modifying
    @Query("DELETE FROM MechanicalInspection mi WHERE mi.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
