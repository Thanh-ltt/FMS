package com.FMS.controllers;

import com.FMS.dto.NotificationDto;
import com.FMS.response.ApiResponse;
import com.FMS.services.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    @GetMapping
    ApiResponse<List<NotificationDto>> getRecentNotifications() {
        return ApiResponse.<List<NotificationDto>>builder()
                .result(notificationService.getRecentNotifications())
                .build();
    }

    @GetMapping("/unread-count")
    ApiResponse<Long> getUnreadCount() {
        return ApiResponse.<Long>builder()
                .result(notificationService.getUnreadCount())
                .build();
    }

    @PatchMapping("/{id}/read")
    ApiResponse<String> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ApiResponse.<String>builder()
                .result("Đã đánh dấu đã đọc")
                .build();
    }

    @PatchMapping("/read-all")
    ApiResponse<String> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<String>builder()
                .result("Đã đánh dấu tất cả là đã đọc")
                .build();
    }
}
