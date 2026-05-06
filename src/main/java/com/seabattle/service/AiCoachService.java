package com.seabattle.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiCoachService {
    private final WebClient webClient;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    public AiCoachService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public String strategicReview(String gameLog) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return "Gemini API key not configured. Add APP_GEMINI_API_KEY to enable AI coach.";
        }
        String prompt = "Analyze this battleship game log: " + gameLog +
                ". Give 3 tactical tips in a professional admiral tone.";

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            var response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return extractText(response);
        } catch (Exception ex) {
            return "AI coach unavailable right now. " + ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) return "No AI analysis returned.";
        var candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "No AI analysis returned.";
        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "No AI analysis returned.";
        return String.valueOf(parts.get(0).get("text"));
    }
}
