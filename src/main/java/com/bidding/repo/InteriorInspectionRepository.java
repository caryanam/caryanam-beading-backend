package com.bidding.repo;

import com.bidding.entity.InteriorInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InteriorInspectionRepository extends JpaRepository<InteriorInspection, Long> {
    Optional<InteriorInspection> findByInspectionId(Long inspectionId);

    @Modifying
    @Query("DELETE FROM InteriorInspection ii WHERE ii.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
