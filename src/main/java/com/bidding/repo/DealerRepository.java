package com.bidding.repo;

import com.bidding.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long> {

    Optional<Dealer> findByEmail(String email);

    Optional<Dealer> findByMobileNumber(String mobileNumber);

    Optional<Dealer> findByEmailOrMobileNumber(String email, String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

}
