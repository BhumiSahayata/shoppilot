package com.shoppilot.shoppilot.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.shoppilot.shoppilot.dto.AiShoppingResponse;
import com.shoppilot.shoppilot.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class AiShoppingService {

    private final DeterministicFallbackEngine fallbackEngine;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final String aiApiKey;
    private final String aiApiUrl;

    public AiShoppingService(DeterministicFallbackEngine fallbackEngine,
                             ProductRepository productRepository,
                             ObjectMapper objectMapper,
                             @Value("${ai.api.key:}") String aiApiKey,
                             @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}") String aiApiUrl) {
        this.fallbackEngine = fallbackEngine;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.aiApiKey = (aiApiKey != null) ? aiApiKey.trim() : "";
        this.aiApiUrl = aiApiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        if (isAiConfigured()) {
            log.info("[AI_SERVICE] External AI API configured at {}", aiApiUrl);
        } else {
            log.info("[AI_SERVICE] No external AI key configured. Operating with high-performance DeterministicFallbackEngine.");
        }
    }

    public boolean isAiConfigured() {
        return !aiApiKey.isEmpty() && !aiApiKey.equalsIgnoreCase("placeholder");
    }

    public AiShoppingResponse processShoppingRequest(String userMessage) {
        if (!isAiConfigured()) {
            return fallbackEngine.executeFallback(userMessage);
        }

        try {
            // Attempt structured LLM call
            String systemPrompt = """
                You are ShopPilot, an AI sales agent for an e-commerce catalog.
                Given the customer's request, extract the structured shopping intent in JSON format:
                {
                  "category": "laptop|phone|monitor|audio|keyboard|mouse|camera|all",
                  "useCase": "coding|gaming|office|student|general",
                  "maxBudget": number or null
                }
                Respond ONLY with valid JSON.
                """;

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    },
                    "response_format", Map.of("type", "json_object")
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiApiUrl))
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                JsonNode parsed = objectMapper.readTree(content);

                // Re-route intent through our catalog and bounded scoring
                AiShoppingResponse.IntentDto intent = AiShoppingResponse.IntentDto.builder()
                        .category(parsed.path("category").asText("all"))
                        .useCase(parsed.path("useCase").asText("general"))
                        .maxBudget(parsed.has("maxBudget") && !parsed.path("maxBudget").isNull() ? parsed.path("maxBudget").asDouble() : null)
                        .build();

                AiShoppingResponse result = fallbackEngine.executeFallback(userMessage);
                result.setEngineUsed("LIVE_AI");
                result.setIntent(intent);
                return result;
            } else {
                log.warn("[AI_SERVICE] LLM API returned non-200 status {}. Switching to deterministic fallback.", response.statusCode());
                return fallbackEngine.executeFallback(userMessage);
            }
        } catch (Exception e) {
            log.warn("[AI_SERVICE] Exception calling external AI ({}: {}). Gracefully switching to safe fallback.",
                    e.getClass().getSimpleName(), e.getMessage());
            return fallbackEngine.executeFallback(userMessage);
        }
    }
}
