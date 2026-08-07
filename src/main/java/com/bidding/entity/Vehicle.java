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

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_mobile_number")
    private String customerMobileNumber;

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

    @Column(name = "suggested_price")
    private Double suggestedPrice;

    @Column(name = "current_highest_bid")
    private Double currentHighestBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_highest_bidder_id")
    private Dealer currentHighestBidder;

    @Column(name = "auction_end_time")
    private LocalDateTime auctionEndTime;

    @Column(name = "total_bids")
    private Integer totalBids;
}
