package com.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mechanical_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicalInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "engine_status")
    private String engineStatus;

    @Column(name = "engine_oil")
    private String engineOil;

    @Column(name = "brake_oil")
    private String brakeOil;

    @Column(name = "steering_oil")
    private String steeringOil;

    private String coolant;

    @Column(name = "brake_booster")
    private String brakeBooster;

    @Column(name = "brake_working")
    private String brakeWorking;

    private String apron;
    private String chassis;
    private String suspension;
    private String bush;
    private String leakage;
    private String transmission;
    private String gearbox;
    private String differential;
    private String axle;

    @Column(name = "engine_noise")
    private String engineNoise;

    private String smoke;

    @Column(name = "fluid_leakage")
    private String fluidLeakage;
}
