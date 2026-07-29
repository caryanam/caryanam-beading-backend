package com.bidding.dto.responce;

import com.bidding.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerResponseDTO {

    private Long id;
    private String dealershipName;
    private String ownerName;
    private String email;
    private String mobileNumber;
    private Role role;
}
