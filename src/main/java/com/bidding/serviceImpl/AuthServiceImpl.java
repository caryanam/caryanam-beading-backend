package com.bidding.serviceImpl;

import com.bidding.config.JwtService;
import com.bidding.dto.request.LoginRequest;
import com.bidding.dto.request.InspectorRegisterRequest;
import com.bidding.dto.request.DealerRegisterRequest;
import com.bidding.dto.responce.AuthResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.entity.Admin;
import com.bidding.entity.Inspector;
import com.bidding.entity.Dealer;
import com.bidding.enums.Role;
import com.bidding.exception.ResourceAlreadyExistsException;
import com.bidding.repo.AdminRepository;
import com.bidding.repo.InspectorRepository;
import com.bidding.repo.DealerRepository;
import com.bidding.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final InspectorRepository inspectorRepository;
    private final DealerRepository dealerRepository;
    private final AdminRepository adminRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public InspectorResponseDTO registerInspector(InspectorRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password must match");
        }

        if (adminRepository.existsByEmail(request.getEmail()) || 
            inspectorRepository.existsByEmail(request.getEmail()) || 
            dealerRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        if (adminRepository.existsByMobileNumber(request.getMobile()) || 
            inspectorRepository.existsByMobileNumber(request.getMobile()) || 
            dealerRepository.existsByMobileNumber(request.getMobile())) {
            throw new ResourceAlreadyExistsException("Mobile number already exists");
        }

        Inspector inspector = Inspector.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.INSPECTOR)
                .build();

        Inspector saved = inspectorRepository.save(inspector);

        return InspectorResponseDTO.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .mobileNumber(saved.getMobileNumber())
                .role(saved.getRole())
                .build();
    }

    @Override
    public DealerResponseDTO registerDealer(DealerRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password must match");
        }

        if (request.getArea() == null || request.getArea().trim().length() < 3) {
            throw new IllegalArgumentException("Area must be at least 3 characters long");
        }

        if (request.getCity() == null || request.getCity().trim().length() < 3) {
            throw new IllegalArgumentException("City must be at least 3 characters long");
        }

        if (request.getAddress() == null || request.getAddress().trim().length() < 5) {
            throw new IllegalArgumentException("Address must be at least 5 characters long");
        }

        if (adminRepository.existsByEmail(request.getEmail()) || 
            inspectorRepository.existsByEmail(request.getEmail()) || 
            dealerRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        if (adminRepository.existsByMobileNumber(request.getMobile()) || 
            inspectorRepository.existsByMobileNumber(request.getMobile()) || 
            dealerRepository.existsByMobileNumber(request.getMobile())) {
            throw new ResourceAlreadyExistsException("Mobile number already exists");
        }

        Dealer dealer = Dealer.builder()
                .dealershipName(request.getDealershipName())
                .ownerName(request.getOwnerName())
                .email(request.getEmail())
                .mobileNumber(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DEALER)
                .address(request.getAddress())
                .area(request.getArea())
                .city(request.getCity())
                .build();

        Dealer saved = dealerRepository.save(dealer);

        return DealerResponseDTO.builder()
                .id(saved.getId())
                .dealershipName(saved.getDealershipName())
                .ownerName(saved.getOwnerName())
                .email(saved.getEmail())
                .mobileNumber(saved.getMobileNumber())
                .role(saved.getRole())
                .address(saved.getAddress())
                .area(saved.getArea())
                .city(saved.getCity())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // Check if Admin
        Optional<Admin> adminOptional = adminRepository.findByEmail(request.getEmail());
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );



            String token = jwtService.generateToken(admin.getEmail());

            return AuthResponse.builder()
                    .id(admin.getId())
                    .fullName(admin.getFullName())
                    .email(admin.getEmail())
                    .mobileNumber(admin.getMobileNumber())
                    .role(admin.getRole())
                    .token(token)
                    .build();
        }

        // Check if Inspector
        Optional<Inspector> inspectorOptional = inspectorRepository.findByEmail(request.getEmail());
        if (inspectorOptional.isPresent()) {
            Inspector inspector = inspectorOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(inspector.getEmail());

            return AuthResponse.builder()
                    .id(inspector.getId())
                    .fullName(inspector.getFullName())
                    .email(inspector.getEmail())
                    .mobileNumber(inspector.getMobileNumber())
                    .role(inspector.getRole())
                    .token(token)
                    .build();
        }

        // Check if Dealer
        Optional<Dealer> dealerOptional = dealerRepository.findByEmail(request.getEmail());
        if (dealerOptional.isPresent()) {
            Dealer dealer = dealerOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(dealer.getEmail());

            return AuthResponse.builder()
                    .id(dealer.getId())
                    .fullName(dealer.getOwnerName())
                    .email(dealer.getEmail())
                    .mobileNumber(dealer.getMobileNumber())
                    .role(dealer.getRole())
                    .token(token)
                    .build();
        }

        throw new BadCredentialsException("Invalid Email or Password");
    }
}
