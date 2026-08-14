package com.bidding.serviceImpl;

import com.bidding.dto.responce.NotificationDTO;
import com.bidding.entity.Notification;
import com.bidding.repo.NotificationRepository;
import com.bidding.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void createNotification(String recipientRole, String recipientEmail, Long inspectionId, String title, String message, String type) {
        Notification notification = Notification.builder()
                .recipientRole(recipientRole)
                .recipientEmail(recipientEmail)
                .inspectionId(inspectionId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getAdminNotifications() {
        return notificationRepository.findByRecipientRoleOrderByCreatedAtDesc("ADMIN").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getDealerNotifications(String dealerEmail) {
        return notificationRepository.findForDealer(dealerEmail).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getInspectorNotifications(String inspectorEmail) {
        return notificationRepository.findForInspector(inspectorEmail).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsReadForAdmin() {
        notificationRepository.markAllAsReadForAdmin();
    }

    @Override
    @Transactional
    public void markAllAsReadForDealer(String dealerEmail) {
        notificationRepository.markAllAsReadForDealer(dealerEmail);
    }

    @Override
    @Transactional
    public void markAllAsReadForInspector(String inspectorEmail) {
        notificationRepository.markAllAsReadForInspector(inspectorEmail);
    }

    private NotificationDTO mapToDTO(Notification n) {
        String formattedTime = "Recently";
        if (n.getCreatedAt() != null) {
            java.time.ZonedDateTime istTime = n.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).withZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata"));
            formattedTime = istTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"));
        }
        return NotificationDTO.builder()
                .id(n.getId())
                .inspectionId(n.getInspectionId())
                .recipientRole(n.getRecipientRole())
                .recipientEmail(n.getRecipientEmail())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.getIsRead())
                .createdAt(formattedTime)
                .build();
    }

}
