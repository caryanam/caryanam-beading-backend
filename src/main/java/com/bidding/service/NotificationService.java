package com.bidding.service;

import com.bidding.dto.responce.NotificationDTO;

import java.util.List;

public interface NotificationService {
    void createNotification(String recipientRole, String recipientEmail, Long inspectionId, String title, String message, String type);
    List<NotificationDTO> getAdminNotifications();
    List<NotificationDTO> getDealerNotifications(String dealerEmail);
    List<NotificationDTO> getInspectorNotifications(String inspectorEmail);
    void markAsRead(Long notificationId);
    void markAllAsReadForAdmin();
    void markAllAsReadForDealer(String dealerEmail);
    void markAllAsReadForInspector(String inspectorEmail);
}
