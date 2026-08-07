package com.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientRole; // "ADMIN", "DEALER", "ALL_DEALERS"

    private String recipientEmail; // Null for ADMIN or ALL_DEALERS

    private Long inspectionId;

    private String title;

    @Column(length = 1000)
    private String message;

    private String type; // BID_PLACED, OUTBID, AUCTION_LIVE, AUCTION_ENDED, SELLER_RESPONSE, DEALER_REPLY, STATUS_UPDATE

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
