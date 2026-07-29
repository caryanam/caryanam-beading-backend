package com.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interior_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "battery_brand")
    private String batteryBrand;

    @Column(name = "battery_serial_number")
    private String batterySerialNumber;

    @Column(name = "ac_cooling")
    private String acCooling;

    @Column(name = "evaluator_valuation")
    private Double evaluatorValuation;

    @Column(name = "right_tail_lamp")
    private String rightTailLamp;

    @Column(name = "left_tail_lamp")
    private String leftTailLamp;

    @Column(name = "right_head_lamp")
    private String rightHeadLamp;

    @Column(name = "left_head_lamp")
    private String leftHeadLamp;

    private String indicators;

    @Column(name = "boot_floor")
    private String bootFloor;

    private String dashboard;

    @Column(name = "fog_lamps")
    private String fogLamps;

    @Column(name = "power_windows")
    private String powerWindows;

    @Column(name = "music_system")
    private String musicSystem;

    @Column(name = "steering_mounted_controls")
    private String steeringMountedControls;

    private String wiper;

    @Column(name = "rear_defogger")
    private String rearDefogger;

    @Column(name = "rear_washer")
    private String rearWasher;

    @Column(name = "instrument_cluster")
    private String instrumentCluster;

    private String infotainment;

    @Column(name = "central_lock")
    private String centralLock;

    @Column(name = "push_button")
    private String pushButton;

    private String sunroof;
    private String sensors;
    private String remarks;
}
