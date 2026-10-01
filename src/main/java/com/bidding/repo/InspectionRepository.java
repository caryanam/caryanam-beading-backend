package com.bidding.repo;

import com.bidding.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Optional<Inspection> findByVehicleVehicleNumber(String vehicleNumber);
    Optional<Inspection> findByVehicleId(Long vehicleId);
    java.util.List<Inspection> findByInspectorId(Long inspectorId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inspection i WHERE (i.inspector.id = :inspectorId OR i.submittedBy.id = :inspectorId)")
    java.util.List<Inspection> findAllByFreelancerInspectorId(@org.springframework.data.repository.query.Param("inspectorId") Long inspectorId);
}
