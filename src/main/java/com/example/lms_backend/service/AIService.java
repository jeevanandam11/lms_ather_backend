package com.example.lms_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

@Service
public class AIService {

    //  API KEYS (FIXED)

    @Value("${groq.api.key}")
    private String groqApiKey;

    //  URLs
    private final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    //  MAIN METHOD
    public Map<String, Object> generateLesson(String topic) throws Exception {

        System.out.println("GROQ KEY = " + groqApiKey);

        // 1 AI timeline
        Map<String, Object> timelineData = callGroq(topic);

        // 2 PDF
        byte[] pdf = generatePDF(timelineData);

        Map<String, Object> responseMap = new java.util.HashMap<>();
        responseMap.put("title", timelineData.get("title"));
        responseMap.put("timeline", timelineData.get("timeline"));
        responseMap.put("suggestions", timelineData.get("suggestions"));
        responseMap.put("pdf", Base64.getEncoder().encodeToString(pdf));

        return responseMap;
    }

    //  GROQ API
    public Map<String, Object> callGroq(String topic) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        String url = GROQ_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        String prompt = """
        Generate a comprehensive, highly detailed text lesson for the selected topic.
        Provide a LARGE amount of educational content.
        
        Rules:
        - Return ONLY JSON
        - Do NOT include any timestamps or time fields.
        - For headings of each topic, DO NOT use ### markdown. Use HTML <b> tags (e.g., <b>Heading Here</b>).
        - Break the lesson into many segments (at least 15 segments) to give a large amount of data.
        - include 'summary', 'code', or 'quote' types.
        - CRITICAL RULE: For any 'code' text, you MUST preserve line-by-line formatting and indentation! Do NOT output minified or single-line code. You MUST use explicit \\n newline escape characters within the JSON strings to break the code into readable lines (e.g., "public class Main {\\n    public static void main..." ).

        Format:
        {
          "title": "",
          "timeline": [
            {
              "type": "summary | code | quote",
              "text": ""
            }
          ],
          "suggestions": ["Follow-up topic 1", "Follow-up topic 2", "Follow-up topic 3"]
        }

        Topic: """ + topic;

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) responseBody.get("choices");

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            String result = (String) message.get("content");

            result = result.replace("```json", "")
                    .replace("```", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(result, Map.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("GROQ API ERROR: " + e.getResponseBodyAsString());
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("title", "API Rate Limit Exceeded");
            errorMap.put("timeline", java.util.List.of(
                java.util.Map.of("time", 0, "type", "quote", "text", "Groq API quota exceeded. Please wait around 60 seconds before generating a new lesson.")
            ));
            return errorMap;
        } catch (Exception e) {
            System.err.println("Groq JSON Mapping Error: " + e.getMessage());
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("title", "Internal Error");
            errorMap.put("timeline", java.util.List.of(
                java.util.Map.of("time", 0, "type", "summary", "text", "A server error occurred: " + e.getMessage())
            ));
            return errorMap;
        }
    }

    //  GROQ CHAT
    public String askQuestion(List<Map<String, Object>> contents) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = GROQ_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an AI learning assistant. At the absolute MUST-HAVE end of every response, provide exactly 3 suggested short follow-up questions formatted perfectly as: \\n\\nSUGGESTIONS: [\"Question 1\", \"Question 2\", \"Question 3\"]"));
        for (Map<String, Object> contentNode : contents) {
            String role = "user";
            if ("model".equals(contentNode.get("role"))) {
                role = "assistant";
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentNode.get("parts");
            String text = (String) parts.get(0).get("text");
            messages.add(Map.of("role", role, "content", text));
        }

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", messages
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errBody = e.getResponseBodyAsString();
            System.err.println("GROQ API ERROR: " + errBody);
            if (e.getStatusCode() == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS) {
                return "Groq AI API quota exceeded (Rate Limit). Please wait around 60 seconds before asking the next question.";
            }
            return "Aether AI API Error: " + e.getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
            return "Internal Server Error: " + e.getMessage();
        }
    }

    // EXTRACT TEXT
    private String extractText(org.springframework.web.multipart.MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".docx")) {
            try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(file.getInputStream());
                 org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        } else {
            com.itextpdf.text.pdf.PdfReader reader = new com.itextpdf.text.pdf.PdfReader(file.getInputStream());
            StringBuilder text = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                text.append(com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage(reader, i));
            }
            reader.close();
            return text.toString();
        }
    }

    //  PLACEMENT ROADMAP
    public Map<String, Object> generatePlacementRoadmap(org.springframework.web.multipart.MultipartFile file, String targetRole) throws Exception {
        String text = "";
        if (file != null && !file.isEmpty()) {
            text = extractText(file);
        }
        
        String prompt;
        if (text.isEmpty()) {
            prompt = "Target Role selected by user: " + targetRole + "\n\n" +
                "The user has not provided a resume. Based solely on the target role, generate a comprehensive learning roadmap.\n" +
                "Return ONLY JSON format with this strict schema:\n" +
                "{\n" +
                "  \"skills_identified\": [\"No resume provided\"],\n" +
                "  \"skill_gaps\": [\"Please upload a resume for personalized gap analysis.\"],\n" +
                "  \"roadmap\": {\n" +
                "    \"beginner\": [\"Topic1\", \"Topic2\"],\n" +
                "    \"intermediate\": [\"Topic3\", \"Topic4\"],\n" +
                "    \"advanced\": [\"Topic5\", \"Topic6\"]\n" +
                "  }\n" +
                "}";
        } else {
            prompt = "Analyze this resume text: " + text + "\n\n" +
                "Target Role selected by user: " + targetRole + "\n\n" +
                "Based on the resume and the target role, generate a learning roadmap.\n" +
                "Return ONLY JSON format with this strict schema:\n" +
                "{\n" +
                "  \"skills_identified\": [\"Skill1\", \"Skill2\"],\n" +
                "  \"skill_gaps\": [\"Gap1\", \"Gap2\"],\n" +
                "  \"roadmap\": {\n" +
                "    \"beginner\": [\"Topic1\", \"Topic2\"],\n" +
                "    \"intermediate\": [\"Topic3\", \"Topic4\"],\n" +
                "    \"advanced\": [\"Topic5\", \"Topic6\"]\n" +
                "  }\n" +
                "}";
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object")
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, entity, Map.class);
            Map<String, Object> message = (Map<String, Object>) ((List<Map<String, Object>>) response.getBody().get("choices")).get(0).get("message");
            String result = ((String) message.get("content")).replace("```json", "").replace("```", "").trim();
            return new ObjectMapper().readValue(result, Map.class);
        } catch (Exception e) {
            System.err.println("Groq JSON Mapping Error for Roadmap: " + e.getMessage());
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("skills_identified", java.util.List.of());
            err.put("skill_gaps", java.util.List.of("Error: " + e.getMessage()));
            err.put("roadmap", Map.of("beginner", java.util.List.of(), "intermediate", java.util.List.of(), "advanced", java.util.List.of()));
            return err;
        }
    }

    // GENERATE QUIZ
    public Map<String, Object> generateQuiz(Map<String, Object> roadmap) {
        String prompt = "Based on the following learning roadmap, generate exactly 10 multiple-choice questions to test the user's knowledge on these topics: " + roadmap.toString() + "\n\n" +
            "Return ONLY JSON format with this strict schema:\n" +
            "{\n" +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"question\": \"Question text here?\",\n" +
            "      \"options\": [\"Option 1\", \"Option 2\", \"Option 3\", \"Option 4\"],\n" +
            "      \"correctAnswer\": 0\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object")
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, entity, Map.class);
            Map<String, Object> message = (Map<String, Object>) ((List<Map<String, Object>>) response.getBody().get("choices")).get(0).get("message");
            String result = ((String) message.get("content")).replace("```json", "").replace("```", "").trim();
            return new ObjectMapper().readValue(result, Map.class);
        } catch (Exception e) {
            System.err.println("Groq JSON Mapping Error for Quiz: " + e.getMessage());
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("questions", java.util.List.of());
            return err;
        }
    }

    //  ANALYZE RESUME
    public Map<String, Object> analyzeResume(org.springframework.web.multipart.MultipartFile file) throws Exception {
        String extractedText = extractText(file);

        String prompt = "Analyze this resume text: " + extractedText + "\n\n" +
            "Return ONLY JSON format with this strict schema:\n" +
            "{\n" +
            "  \"score\": 85,\n" +
            "  \"summary\": \"Brief overview...\",\n" +
            "  \"improvements\": [\"Tip 1\", \"Tip 2\"],\n" +
            "  \"keywords\": [\"Java\", \"React\"],\n" +
            "  \"missing_skills\": [\"Docker\", \"AWS\"]\n" +
            "}";

        RestTemplate restTemplate = new RestTemplate();
        String url = GROQ_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            String result = (String) message.get("content");

            result = result.replace("```json", "").replace("```", "").trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(result, Map.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("GROQ API ERROR: " + e.getResponseBodyAsString());
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("score", 0);
            errorMap.put("summary", "Groq AI API quota exceeded. Please wait around 60 seconds before analyzing.");
            errorMap.put("improvements", java.util.List.of("Wait 60 seconds for your token bucket to refresh."));
            errorMap.put("keywords", java.util.List.of());
            errorMap.put("missing_skills", java.util.List.of());
            return errorMap;
        } catch (Exception e) {
            System.err.println("Groq JSON Mapping Error: " + e.getMessage());
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("score", 0);
            errorMap.put("summary", "A server error occurred reading your resume: " + e.getMessage());
            errorMap.put("improvements", java.util.List.of());
            errorMap.put("keywords", java.util.List.of());
            errorMap.put("missing_skills", java.util.List.of());
            return errorMap;
        }
    }

    //  PDF
    public byte[] generatePDF(Map<String, Object> data) {

        try {
            Document doc = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter.getInstance(doc, out);
            doc.open();

            // Add title
            if (data.containsKey("title")) {
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                Paragraph title = new Paragraph(data.get("title").toString(), titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                doc.add(title);
                doc.add(new Paragraph("\n\n"));
            }

            List<Map<String, Object>> timeline =
                    (List<Map<String, Object>>) data.get("timeline");

            for (Map<String, Object> item : timeline) {
                String text = item.get("text").toString();
                // Strip HTML tags for clean PDF output
                text = text.replaceAll("<[^>]*>", "");
                doc.add(new Paragraph(text));
                doc.add(new Paragraph("\n"));
            }

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}