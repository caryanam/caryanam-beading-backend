package com.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tyre_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TyreInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    private String frontLeftBrand;
    private Integer frontLeftYear;
    private Integer frontLeftTread;

    private String frontRightBrand;
    private Integer frontRightYear;
    private Integer frontRightTread;

    private String rearLeftBrand;
    private Integer rearLeftYear;
    private Integer rearLeftTread;

    private String rearRightBrand;
    private Integer rearRightYear;
    private Integer rearRightTread;

    private String spareBrand;
    private Integer spareYear;
    private Integer spareTread;

    private Boolean hasJack;
    private Boolean hasHandle;
    private Boolean hasToolkit;
    private Boolean hasTriangle;
    private Boolean hasFirstAidBox;
}
