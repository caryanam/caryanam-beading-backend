package com.bidding.repo;

import com.bidding.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Optional<Inspection> findByVehicleVehicleNumber(String vehicleNumber);
    Optional<Inspection> findByVehicleId(Long vehicleId);
}
