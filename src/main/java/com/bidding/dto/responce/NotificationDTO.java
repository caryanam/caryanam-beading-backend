package com.bidding.dto.responce;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private Long inspectionId;
    private String recipientRole;
    private String recipientEmail;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private String createdAt;
}
