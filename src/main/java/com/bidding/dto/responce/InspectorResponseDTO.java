package com.bidding.dto.responce;

import com.bidding.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectorResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private Role role;
}
