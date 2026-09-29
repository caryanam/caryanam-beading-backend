package com.bidding.serviceImpl;

import com.bidding.dto.responce.NotificationDTO;
import com.bidding.entity.Notification;
import com.bidding.repo.NotificationRepository;
import com.bidding.service.NotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
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
    private final com.bidding.repo.DealerRepository dealerRepository;

    private String sanitizeDealerText(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s*\\([A-Za-z0-9\\-\\s]+\\)", "")
                   .replaceAll("(?i)\\bvehicle\\s+[A-Za-z0-9\\-]{4,15}\\b", "vehicle")
                   .trim();
    }

    @Override
    @Transactional
    public void createNotification(String recipientRole, String recipientEmail, Long inspectionId, String title, String messageStr, String type) {
        String finalTitle = title;
        String finalMessage = messageStr;
        if ("DEALER".equalsIgnoreCase(recipientRole)) {
            finalTitle = sanitizeDealerText(title);
            finalMessage = sanitizeDealerText(messageStr);
        }

        Notification notification = Notification.builder()
                .recipientRole(recipientRole)
                .recipientEmail(recipientEmail)
                .inspectionId(inspectionId)
                .title(finalTitle)
                .message(finalMessage)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        // Firebase Push Notification Logic for DEALER only
        if ("DEALER".equalsIgnoreCase(recipientRole)) {
            try {
                String topic = "DEALER_ALL";
                if (recipientEmail != null && !recipientEmail.trim().isEmpty() && !"ALL".equalsIgnoreCase(recipientEmail)) {
                    topic = "dealer_" + recipientEmail.replaceAll("[^a-zA-Z0-9]", "_");
                }
                
                Message msg = Message.builder()
                        .setTopic(topic)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(finalTitle)
                                .setBody(finalMessage)
                                .build())
                        .build();
                FirebaseMessaging.getInstance().send(msg);
                System.out.println("FCM Sent successfully to topic: " + topic);
            } catch (Exception e) {
                System.err.println("FCM Push Error: " + e.getMessage());
            }
        }
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
    public List<NotificationDTO> getDealerNotifications(String dealerIdentifier) {
        String email = dealerIdentifier;
        String mobile = dealerIdentifier;
        if (dealerRepository != null && dealerIdentifier != null) {
            com.bidding.entity.Dealer d = dealerRepository.findByEmailOrMobileNumber(dealerIdentifier, dealerIdentifier).orElse(null);
            if (d != null) {
                if (d.getEmail() != null && !d.getEmail().trim().isEmpty()) {
                    email = d.getEmail().trim();
                }
                if (d.getMobileNumber() != null && !d.getMobileNumber().trim().isEmpty()) {
                    mobile = d.getMobileNumber().trim();
                }
            }
        }
        return notificationRepository.findForDealer(email, mobile).stream()
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
    public void markAllAsReadForDealer(String dealerIdentifier) {
        String email = dealerIdentifier;
        String mobile = dealerIdentifier;
        if (dealerRepository != null && dealerIdentifier != null) {
            com.bidding.entity.Dealer d = dealerRepository.findByEmailOrMobileNumber(dealerIdentifier, dealerIdentifier).orElse(null);
            if (d != null) {
                if (d.getEmail() != null && !d.getEmail().trim().isEmpty()) {
                    email = d.getEmail().trim();
                }
                if (d.getMobileNumber() != null && !d.getMobileNumber().trim().isEmpty()) {
                    mobile = d.getMobileNumber().trim();
                }
            }
        }
        notificationRepository.markAllAsReadForDealer(email, mobile);
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
        String title = n.getTitle();
        String message = n.getMessage();
        if ("DEALER".equalsIgnoreCase(n.getRecipientRole())) {
            title = sanitizeDealerText(title);
            message = sanitizeDealerText(message);
        }
        return NotificationDTO.builder()
                .id(n.getId())
                .inspectionId(n.getInspectionId())
                .recipientRole(n.getRecipientRole())
                .recipientEmail(n.getRecipientEmail())
                .title(title)
                .message(message)
                .type(n.getType())
                .isRead(n.getIsRead())
                .createdAt(formattedTime)
                .build();
    }

}
