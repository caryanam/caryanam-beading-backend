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

    @Column(name = "registration_year")
    private Integer registrationYear;

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

        @Column(name = "location")
    private String location;

    @Column(name = "rto_information")
    private String rtoInformation;

    @Column(name = "rs_availability")
    private String rsAvailability;

    @Column(name = "duplicate_key")
    private String duplicateKey;

    @Column(name = "rto_noc_issued")
    private String rtoNocIssued;

    @Column(name = "under_hypothecation")
    private String underHypothecation;

    @Column(name = "mismatch_in_rc")
    private String mismatchInRc;

    @Column(name = "road_tax_paid")
    private String roadTaxPaid;

    @Column(name = "fitness_upto")
    private String fitnessUpto;

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

    @Column(name = "seller_agreed")
    private Boolean sellerAgreed;

    @Column(name = "seller_counter_price")
    private Double sellerCounterPrice;

    @Column(name = "seller_message", length = 1000)
    private String sellerMessage;

    @Column(name = "admin_dealer_message", length = 1000)
    private String adminDealerMessage;

    @Column(name = "dealer_reply_message", length = 1000)
    private String dealerReplyMessage;
}
