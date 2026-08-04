package com.bidding.dto.responce;

import com.bidding.enums.Role;
import lombok.*;
import java.util.List;

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
    private String address;
    private String area;
    private String city;
    private Long totalBids;
    private Long wonBidsCount;
    private List<DealerWonBidDTO> wonBids;
}
