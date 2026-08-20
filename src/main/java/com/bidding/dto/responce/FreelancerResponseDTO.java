package com.bidding.dto.responce;

import com.bidding.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private Role role;
    private Integer uploads;
}
