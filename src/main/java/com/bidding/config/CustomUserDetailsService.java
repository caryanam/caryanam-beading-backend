package com.bidding.config;

import com.bidding.entity.Admin;
import com.bidding.entity.Inspector;
import com.bidding.entity.Dealer;
import com.bidding.repo.AdminRepository;
import com.bidding.repo.InspectorRepository;
import com.bidding.repo.DealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final InspectorRepository inspectorRepository;
    private final DealerRepository dealerRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }
        String input = identifier.trim();

        // First search in Admin table by email or mobile number
        Optional<Admin> adminOptional = adminRepository.findByEmailOrMobileNumber(input, input);

        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    admin.getEmail() != null ? admin.getEmail() : input,
                    admin.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + admin.getRole().name())
                    )
            );
        }

        // Search in Inspector table by email or mobile number
        Optional<Inspector> inspectorOptional = inspectorRepository.findByEmailOrMobileNumber(input, input);

        if (inspectorOptional.isPresent()) {
            Inspector inspector = inspectorOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    inspector.getEmail() != null ? inspector.getEmail() : input,
                    inspector.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + inspector.getRole().name())
                    )
            );
        }

        // Search in Dealer table by email or mobile number
        Optional<Dealer> dealerOptional = dealerRepository.findByEmailOrMobileNumber(input, input);

        if (dealerOptional.isPresent()) {
            Dealer dealer = dealerOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    dealer.getEmail() != null ? dealer.getEmail() : input,
                    dealer.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + dealer.getRole().name())
                    )
            );
        }

        throw new UsernameNotFoundException("User not found with Email or Mobile Number: " + identifier);
    }
}