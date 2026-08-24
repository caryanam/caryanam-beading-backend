package com.bidding.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAccountRequest {

    private String emailOrMobile;
    private String email;
    private String mobile;

    @NotBlank(message = "Password is required")
    private String password;

    public String getIdentifier() {
        if (emailOrMobile != null && !emailOrMobile.trim().isEmpty()) {
            return emailOrMobile.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        if (mobile != null && !mobile.trim().isEmpty()) {
            return mobile.trim();
        }
        return "";
    }
}
