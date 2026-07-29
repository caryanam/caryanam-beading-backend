package com.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
