package com.bidding.repo;

import com.bidding.entity.InspectionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionImageRepository extends JpaRepository<InspectionImage, Long> {
    List<InspectionImage> findByInspectionId(Long inspectionId);

    @Modifying
    @Query("DELETE FROM InspectionImage im WHERE im.inspection.id = :inspectionId AND im.imageCategory = :category")
    void deleteByInspectionIdAndImageCategory(@Param("inspectionId") Long inspectionId, @Param("category") String category);

    @Modifying
    @Query("DELETE FROM InspectionImage im WHERE im.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
