package com.FMS.services.impl;

import com.FMS.dto.NotificationDto;
import com.FMS.entity.Invoice;

import com.FMS.entity.Notification;
import com.FMS.enums.InvoiceStatus;
import com.FMS.mapper.NotificationMapper;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.NotificationRepository;
import com.FMS.services.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    InvoiceRepository invoiceRepository;
    MaintenanceRepository maintenanceRepository;
    NotificationMapper notificationMapper;

    @Override
    public List<NotificationDto> getRecentNotifications() {
        // Run quick scan to ensure fresh alerts
        scanAndGenerateAlerts();
        return notificationRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Override
    public Long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Override
    @Transactional
    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void createNotification(String title, String message, String type, String role) {
        if (notificationRepository.existsByTitleAndMessage(title, message)) {
            return; // Avoid duplicates
        }

        Notification n = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .targetRole(role)
                .isRead(false)
                .build();

        notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void scanAndGenerateAlerts() {
        // 1. Scan Overdue Invoices
        List<Invoice> overdueInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PENDING && inv.getDueDate() != null && inv.getDueDate().isBefore(LocalDate.now()))
                .toList();

        for (Invoice inv : overdueInvoices) {
            String invCode = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : inv.getId().substring(0, 8);
            String title = "Hóa đơn quá hạn thanh toán";
            String msg = String.format("Hóa đơn %s đến hạn ngày %s chưa được thanh toán (Số tiền: %,.0f đ)",
                    invCode, inv.getDueDate(), inv.getAmountDue());
            createNotification(title, msg, "OVERDUE_INVOICE", "ACCOUNTANT");
        }
    }
}
