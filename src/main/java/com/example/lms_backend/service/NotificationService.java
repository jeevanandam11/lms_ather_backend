package com.example.lms_backend.service;

import com.example.lms_backend.model.Notification;
import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    public void sendAndSaveNotification(UserEntity user, String title, String message) {
        // Save to DB
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);

        // Send Email
        if (user.getEmail() != null) {
            emailService.sendSimpleMessage(user.getEmail(), title, message);
        }
    }
}
