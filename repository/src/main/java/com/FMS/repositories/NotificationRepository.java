package com.FMS.repositories;

import com.FMS.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findTop20ByOrderByCreatedAtDesc();
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();
    Long countByIsReadFalse();
    boolean existsByTitleAndMessage(String title, String message);
}
