package com.example.lms_backend.controller;

import com.example.lms_backend.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> req) throws Exception {
        return aiService.generateLesson(req.get("topic"));
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> req) throws Exception {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) req.get("contents");
        String responseText = aiService.askQuestion(contents);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("text", responseText);
        return response;
    }

}