package com.bidding.repo;

import com.bidding.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    boolean existsByRole(com.bidding.enums.Role role);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);
}
