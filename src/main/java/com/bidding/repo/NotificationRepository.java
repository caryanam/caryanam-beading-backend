package com.bidding.repo;

import com.bidding.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientRoleOrderByCreatedAtDesc(String role);

    @Query("SELECT n FROM Notification n WHERE n.recipientRole = 'ALL_DEALERS' OR (n.recipientRole = 'DEALER' AND n.recipientEmail = :email) ORDER BY n.createdAt DESC")
    List<Notification> findForDealer(@Param("email") String email);
}
