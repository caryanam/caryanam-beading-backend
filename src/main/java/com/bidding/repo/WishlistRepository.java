package com.bidding.repo;

import com.bidding.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByDealerId(Long dealerId);

    Optional<Wishlist> findByDealerIdAndInspectionId(Long dealerId, Long inspectionId);

    boolean existsByDealerIdAndInspectionId(Long dealerId, Long inspectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Wishlist w WHERE w.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Long inspectionId);
}
