package com.bidding.serviceImpl;

import com.bidding.config.JwtService;
import com.bidding.dto.request.LoginRequest;
import com.bidding.dto.request.InspectorRegisterRequest;
import com.bidding.dto.request.DealerRegisterRequest;
import com.bidding.dto.request.FreelancerRegisterRequest;
import com.bidding.dto.responce.AuthResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;
import com.bidding.dto.responce.FreelancerResponseDTO;
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

import com.bidding.dto.request.DeleteAccountRequest;
import com.bidding.entity.Vehicle;
import com.bidding.entity.Bid;
import com.bidding.entity.Wishlist;
import com.bidding.entity.Inspection;
import com.bidding.entity.InspectionImage;
import com.bidding.repo.VehicleRepository;
import com.bidding.repo.BidRepository;
import com.bidding.repo.WishlistRepository;
import com.bidding.repo.InspectionRepository;
import com.bidding.repo.InspectionImageRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final InspectorRepository inspectorRepository;
    private final DealerRepository dealerRepository;
    private final AdminRepository adminRepository;
    private final VehicleRepository vehicleRepository;
    private final BidRepository bidRepository;
    private final WishlistRepository wishlistRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionImageRepository inspectionImageRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final com.bidding.service.OtpService otpService;

    @Override
    public InspectorResponseDTO registerInspector(InspectorRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password must match");
        }

        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new IllegalArgumentException("Email address is not verified. Please request and verify OTP first.");
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
    public FreelancerResponseDTO registerFreelancer(FreelancerRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password must match");
        }

        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new IllegalArgumentException("Email address is not verified. Please request and verify OTP first.");
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

        Inspector freelancer = Inspector.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.FREELANCER)
                .build();

        Inspector saved = inspectorRepository.save(freelancer);

        return FreelancerResponseDTO.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .mobileNumber(saved.getMobileNumber())
                .role(saved.getRole())
                .uploads(0)
                .build();
    }

    @Override
    public DealerResponseDTO registerDealer(DealerRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password must match");
        }

        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new IllegalArgumentException("Email address is not verified. Please request and verify OTP first.");
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
        String identifier = request.getEmail() != null ? request.getEmail().trim() : "";

        // Check if Admin
        Optional<Admin> adminOptional = adminRepository.findByEmailOrMobileNumber(identifier, identifier);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            identifier,
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(admin.getEmail() != null ? admin.getEmail() : identifier);

            return AuthResponse.builder()
                    .id(admin.getId())
                    .fullName(admin.getFullName())
                    .email(admin.getEmail())
                    .mobileNumber(admin.getMobileNumber())
                    .role(admin.getRole())
                    .token(token)
                    .build();
        }

        // Check if Inspector or Freelancer
        Optional<Inspector> inspectorOptional = inspectorRepository.findByEmailOrMobileNumber(identifier, identifier);
        if (inspectorOptional.isPresent()) {
            Inspector inspector = inspectorOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            identifier,
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(inspector.getEmail() != null ? inspector.getEmail() : identifier);

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
        Optional<Dealer> dealerOptional = dealerRepository.findByEmailOrMobileNumber(identifier, identifier);
        if (dealerOptional.isPresent()) {
            Dealer dealer = dealerOptional.get();

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            identifier,
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(dealer.getEmail() != null ? dealer.getEmail() : identifier);

            return AuthResponse.builder()
                    .id(dealer.getId())
                    .fullName(dealer.getOwnerName())
                    .dealershipName(dealer.getDealershipName())
                    .email(dealer.getEmail())
                    .mobileNumber(dealer.getMobileNumber())
                    .role(dealer.getRole())
                    .token(token)
                    .build();
        }

        throw new BadCredentialsException("Invalid credentials. Please check your email/mobile number and password.");
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        boolean verified = otpService.isEmailVerified(cleanEmail);
        if (!verified && otp != null && !otp.trim().isEmpty()) {
            verified = otpService.verifyOtp(cleanEmail, otp);
        }

        if (!verified) {
            throw new IllegalArgumentException("Email is not verified or OTP is invalid/expired. Please verify OTP first.");
        }

        Optional<Dealer> dealerOpt = dealerRepository.findByEmail(cleanEmail);
        if (dealerOpt.isPresent()) {
            Dealer dealer = dealerOpt.get();
            dealer.setPassword(passwordEncoder.encode(newPassword.trim()));
            dealerRepository.save(dealer);
            return;
        }

        Optional<Inspector> inspectorOpt = inspectorRepository.findByEmail(cleanEmail);
        if (inspectorOpt.isPresent()) {
            Inspector inspector = inspectorOpt.get();
            inspector.setPassword(passwordEncoder.encode(newPassword.trim()));
            inspectorRepository.save(inspector);
            return;
        }

        Optional<Admin> adminOpt = adminRepository.findByEmail(cleanEmail);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            admin.setPassword(passwordEncoder.encode(newPassword.trim()));
            adminRepository.save(admin);
            return;
        }

        throw new IllegalArgumentException("No registered account found with email: " + cleanEmail);
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address or mobile number is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required for account deletion.");
        }

        String cleanIdentifier = identifier.trim();

        // 1. Check if user is a Dealer
        Optional<Dealer> dealerOpt = dealerRepository.findByEmailOrMobileNumber(cleanIdentifier, cleanIdentifier);
        if (dealerOpt.isPresent()) {
            Dealer dealer = dealerOpt.get();
            if (!passwordEncoder.matches(request.getPassword(), dealer.getPassword())) {
                throw new BadCredentialsException("Invalid credentials. Incorrect password.");
            }

            // Unlink vehicles where this dealer is set as current highest bidder
            java.util.List<Vehicle> vehiclesWithDealer = vehicleRepository.findAll().stream()
                    .filter(v -> v.getCurrentHighestBidder() != null && v.getCurrentHighestBidder().getId().equals(dealer.getId()))
                    .collect(java.util.stream.Collectors.toList());
            for (Vehicle v : vehiclesWithDealer) {
                v.setCurrentHighestBidder(null);
                vehicleRepository.save(v);
            }

            // Remove bids placed by this dealer
            java.util.List<Bid> dealerBids = bidRepository.findAll().stream()
                    .filter(b -> b.getDealer() != null && b.getDealer().getId().equals(dealer.getId()))
                    .collect(java.util.stream.Collectors.toList());
            if (!dealerBids.isEmpty()) {
                bidRepository.deleteAll(dealerBids);
            }

            // Remove wishlist items saved by this dealer
            java.util.List<Wishlist> dealerWishlist = wishlistRepository.findByDealerId(dealer.getId());
            if (dealerWishlist != null && !dealerWishlist.isEmpty()) {
                wishlistRepository.deleteAll(dealerWishlist);
            }

            dealerRepository.delete(dealer);
            return;
        }

        // 2. Check if user is an Inspector or Freelancer
        Optional<Inspector> inspectorOpt = inspectorRepository.findByEmailOrMobileNumber(cleanIdentifier, cleanIdentifier);
        if (inspectorOpt.isPresent()) {
            Inspector inspector = inspectorOpt.get();
            if (!passwordEncoder.matches(request.getPassword(), inspector.getPassword())) {
                throw new BadCredentialsException("Invalid credentials. Incorrect password.");
            }

            // Unlink inspector from inspection records
            java.util.List<Inspection> inspections = inspectionRepository.findAll().stream()
                    .filter(i -> (i.getInspector() != null && i.getInspector().getId().equals(inspector.getId())) ||
                                 (i.getSubmittedBy() != null && i.getSubmittedBy().getId().equals(inspector.getId())))
                    .collect(java.util.stream.Collectors.toList());
            for (Inspection ins : inspections) {
                if (ins.getInspector() != null && ins.getInspector().getId().equals(inspector.getId())) {
                    ins.setInspector(null);
                }
                if (ins.getSubmittedBy() != null && ins.getSubmittedBy().getId().equals(inspector.getId())) {
                    ins.setSubmittedBy(null);
                }
                inspectionRepository.save(ins);
            }

            // Unlink inspector from inspection images
            java.util.List<InspectionImage> images = inspectionImageRepository.findAll().stream()
                    .filter(img -> img.getInspector() != null && img.getInspector().getId().equals(inspector.getId()))
                    .collect(java.util.stream.Collectors.toList());
            for (InspectionImage img : images) {
                img.setInspector(null);
                inspectionImageRepository.save(img);
            }

            inspectorRepository.delete(inspector);
            return;
        }

        throw new BadCredentialsException("Invalid credentials. No account found with the provided Email Address / Mobile Number.");
    }
}
