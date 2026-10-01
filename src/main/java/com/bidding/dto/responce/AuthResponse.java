package com.bidding.dto.responce;



import com.bidding.enums.Role;

import lombok.*;



@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class AuthResponse {



    private Long id;

    private String fullName;

    private String email;

    private String mobileNumber;

    private String dealershipName;

    private Role role;

    private java.util.List<Role> roles;

    private Boolean hasDualRole;

    private String token;

    private Long freelancerId;

}


