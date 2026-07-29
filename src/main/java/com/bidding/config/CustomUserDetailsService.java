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
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // First search in Admin table
        Optional<Admin> adminOptional =
                adminRepository.findByEmail(email);

        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    admin.getEmail(),
                    admin.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + admin.getRole().name()
                            )
                    )
            );
        }

        // Search in Inspector table
        Optional<Inspector> inspectorOptional =
                inspectorRepository.findByEmail(email);

        if (inspectorOptional.isPresent()) {
            Inspector inspector = inspectorOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    inspector.getEmail(),
                    inspector.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + inspector.getRole().name()
                            )
                    )
            );
        }

        // Search in Dealer table
        Optional<Dealer> dealerOptional =
                dealerRepository.findByEmail(email);

        if (dealerOptional.isPresent()) {
            Dealer dealer = dealerOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    dealer.getEmail(),
                    dealer.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + dealer.getRole().name()
                            )
                    )
            );
        }

        throw new UsernameNotFoundException(
                "User not found with email: " + email
        );
    }
}