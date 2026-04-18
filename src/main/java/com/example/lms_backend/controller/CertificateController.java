package com.example.lms_backend.controller;

import com.example.lms_backend.model.Certificate;
import com.example.lms_backend.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin
public class CertificateController {

    @Autowired
    private CertificateRepository certificateRepository;

    @PostMapping
    public Certificate saveCertificate(@RequestBody Certificate certificate) {
        if (certificate.getIssueDate() == null) {
            certificate.setIssueDate(LocalDate.now());
        }
        return certificateRepository.save(certificate);
    }

    @GetMapping("/user/{userId}")
    public List<Certificate> getCertificatesByUser(@PathVariable("userId") Long userId) {
        return certificateRepository.findByUserId(userId);
    }
}
