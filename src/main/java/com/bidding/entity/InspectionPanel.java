package com.bidding.entity;

import com.bidding.enums.PanelCondition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inspection_panels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "panel_name", nullable = false)
    private String panelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_value", nullable = false)
    private PanelCondition condition;

    @Column(name = "image_url")
    private String imageUrl;
}
