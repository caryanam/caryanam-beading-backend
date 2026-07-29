package com.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    @Column(name = "owner_name")
    private String ownerName;

    private String brand;
    private String model;
    private String variant;

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;

    @Column(name = "fuel_type")
    private String fuelType;

    private String transmission;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Column(name = "insurance_status")
    private String insuranceStatus;

    @Column(name = "inspector_code")
    private String inspectorCode;

    @Column(name = "inspection_date")
    private LocalDateTime inspectionDate;

    @Column(name = "vehicle_status")
    private String vehicleStatus; // e.g. READY_FOR_AUCTION
}
