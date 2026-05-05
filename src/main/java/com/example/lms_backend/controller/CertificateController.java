package com.example.lms_backend.controller;

import com.example.lms_backend.model.Certificate;
import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.CertificateRepository;
import com.example.lms_backend.repository.UserRepo;
import com.example.lms_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin
public class CertificateController {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public Certificate saveCertificate(@RequestBody Certificate certificate) {
        if (certificate.getIssueDate() == null) {
            certificate.setIssueDate(LocalDate.now());
        }
        Certificate saved = certificateRepository.save(certificate);

        Optional<UserEntity> userOpt = userRepo.findById(saved.getUserId());
        if (userOpt.isPresent()) {
            notificationService.sendAndSaveNotification(
                userOpt.get(),
                "Certificate Earned!",
                "Congratulations! You have successfully earned a certificate for the course: " + saved.getCourseName() + "."
            );
        }

        return saved;
    }

    @GetMapping("/user/{userId}")
    public List<Certificate> getCertificatesByUser(@PathVariable("userId") Long userId) {
        return certificateRepository.findByUserId(userId);
    }
}
