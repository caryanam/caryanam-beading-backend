package com.bidding.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerRegisterRequest {

    @NotBlank(message = "Dealership name is required")
    @Size(min = 2, max = 100, message = "Dealership name must be between 2 and 100 characters")
    private String dealershipName;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 80, message = "Owner name must be between 2 and 80 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Owner name must contain only letters and spaces")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid mobile number. Must be a 10-digit number starting with 6-9")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
