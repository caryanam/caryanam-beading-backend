package com.bidding.repo;

import com.bidding.entity.Inspector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectorRepository extends JpaRepository<Inspector, Long> {

    Optional<Inspector> findByEmail(String email);

    Optional<Inspector> findByMobileNumber(String mobileNumber);

    Optional<Inspector> findByEmailOrMobileNumber(String email, String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

}
