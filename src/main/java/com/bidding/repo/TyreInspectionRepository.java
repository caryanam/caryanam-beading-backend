package com.bidding.repo;

import com.bidding.entity.TyreInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TyreInspectionRepository extends JpaRepository<TyreInspection, Long> {
    Optional<TyreInspection> findByInspectionId(Long inspectionId);

    @Modifying
    @Query("DELETE FROM TyreInspection ti WHERE ti.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
