package com.FMS.services;

import com.FMS.dto.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getRecentNotifications();
    Long getUnreadCount();
    void markAsRead(String id);
    void markAllAsRead();
    void createNotification(String title, String message, String type, String role);
    void scanAndGenerateAlerts();
}
