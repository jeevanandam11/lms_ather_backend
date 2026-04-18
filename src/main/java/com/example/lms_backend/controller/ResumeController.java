package com.example.lms_backend.controller;

import com.example.lms_backend.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin
public class ResumeController {

    @Autowired
    private AIService aiService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> analyzeResume(@RequestParam("file") MultipartFile file) throws Exception {
        return aiService.analyzeResume(file);
    }

    @PostMapping(value = "/placement-roadmap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> generatePlacementRoadmap(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("targetRole") String targetRole) throws Exception {
        return aiService.generatePlacementRoadmap(file, targetRole);
    }

    @PostMapping(value = "/generate-quiz", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> generateQuiz(@RequestBody Map<String, Object> roadmap) throws Exception {
        return aiService.generateQuiz(roadmap);
    }
}
