package com.bidding.dto.responce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionMessageDTO {
    private Long id;
    private Long inspectionId;
    private String senderRole;
    private String senderName;
    private String message;
    private String createdAt;
}
