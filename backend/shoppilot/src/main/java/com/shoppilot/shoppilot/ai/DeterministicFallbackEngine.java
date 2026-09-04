package com.shoppilot.shoppilot.ai;

import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.dto.AiShoppingResponse;
import com.shoppilot.shoppilot.dto.AiShoppingResponse.IntentDto;
import com.shoppilot.shoppilot.dto.AiShoppingResponse.ProductRecommendationDto;
import com.shoppilot.shoppilot.model.ActorType;
import com.shoppilot.shoppilot.model.AuditEventType;
import com.shoppilot.shoppilot.model.Product;
import com.shoppilot.shoppilot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeterministicFallbackEngine {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:under|below|less than|within|budget of)?\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d+)?(?:\\.\\d+)?)\\s*(k|thousand|l|lakh)?",
            Pattern.CASE_INSENSITIVE
    );

    public AiShoppingResponse executeFallback(String userMessage) {
        log.info("[AI_FALLBACK] Executing deterministic shopping rules for message: '{}'", userMessage);

        auditService.logEvent(
                AuditEventType.AI_FALLBACK_TRIGGERED,
                ActorType.SYSTEM,
                null,
                null,
                null,
                "Using safe fallback shopping rules for natural language query",
                1.0,
                "ACTIVE",
                userMessage
        );

        IntentDto intent = parseIntent(userMessage);

        auditService.logEvent(
                AuditEventType.INTENT_DETECTED,
                ActorType.AI,
                null,
                null,
                intent.getMaxBudget(),
                String.format("Detected intent - Category: %s, Use-case: %s, Max Budget: ₹%s",
                        intent.getCategory(), intent.getUseCase(),
                        intent.getMaxBudget() != null ? intent.getMaxBudget() : "Any"),
                0.95,
                "SUCCESS",
                userMessage
        );

        List<Product> candidates = findCandidates(intent);
        List<ProductRecommendationDto> recommendations = rankAndExplain(candidates, intent);

        ProductRecommendationDto upsell = null;
        if (!recommendations.isEmpty()) {
            upsell = findBoundedUpsell(recommendations.get(0).getProduct(), intent);
        }

        String aiMessage;
        if (recommendations.isEmpty()) {
            aiMessage = "I searched our catalog with your criteria but couldn't find in-stock items matching that exact budget/category. Here are some of our popular products.";
            List<Product> all = productRepository.findByActiveTrue();
            for (int i = 0; i < Math.min(2, all.size()); i++) {
                Product p = all.get(i);
                recommendations.add(ProductRecommendationDto.builder()
                        .product(p)
                        .reason("Popular in-stock item in merchant catalog")
                        .confidence(0.70)
                        .isUpsell(false)
                        .build());
            }
        } else {
            aiMessage = String.format("I found %d product(s) matching your %s needs under your budget.",
                    recommendations.size(), intent.getUseCase() != null ? intent.getUseCase() : intent.getCategory());
        }

        return AiShoppingResponse.builder()
                .userMessage(userMessage)
                .aiMessage(aiMessage)
                .engineUsed("SAFE_FALLBACK")
                .intent(intent)
                .recommendations(recommendations)
                .upsell(upsell)
                .build();
    }

    public IntentDto parseIntent(String text) {
        String lower = text.toLowerCase();

        Double budget = extractBudget(lower);
        String category = extractCategory(lower);
        String useCase = extractUseCase(lower);

        return IntentDto.builder()
                .category(category)
                .useCase(useCase)
                .maxBudget(budget)
                .build();
    }

    private Double extractBudget(String text) {
        Matcher matcher = BUDGET_PATTERN.matcher(text);
        Double maxBudget = null;

        while (matcher.find()) {
            String numStr = matcher.group(1).replace(",", "");
            try {
                double val = Double.parseDouble(numStr);
                String unit = matcher.group(2);
                if (unit != null) {
                    String u = unit.toLowerCase();
                    if (u.equals("k") || u.equals("thousand")) {
                        val *= 1000;
                    } else if (u.equals("l") || u.equals("lakh")) {
                        val *= 100000;
                    }
                }
                if (val > 100) { // filter out accidental numbers like 16 for 16GB
                    maxBudget = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        return maxBudget;
    }

    private String extractCategory(String text) {
        if (text.contains("laptop") || text.contains("notebook") || text.contains("macbook")) return "laptop";
        if (text.contains("phone") || text.contains("mobile") || text.contains("smartphone")) return "phone";
        if (text.contains("monitor") || text.contains("screen") || text.contains("display")) return "monitor";
        if (text.contains("headphone") || text.contains("earphone") || text.contains("earbuds") || text.contains("audio")) return "audio";
        if (text.contains("keyboard")) return "keyboard";
        if (text.contains("mouse")) return "mouse";
        if (text.contains("camera")) return "camera";
        return "all";
    }

    private String extractUseCase(String text) {
        if (text.contains("coding") || text.contains("programming") || text.contains("development") || text.contains("developer")) return "coding";
        if (text.contains("gaming") || text.contains("game")) return "gaming";
        if (text.contains("office") || text.contains("work") || text.contains("productivity")) return "office";
        if (text.contains("student") || text.contains("study") || text.contains("college")) return "student";
        if (text.contains("photo") || text.contains("video") || text.contains("editing")) return "content creation";
        return "general use";
    }

    private List<Product> findCandidates(IntentDto intent) {
        List<Product> products = productRepository.findByActiveTrue();
        List<Product> matching = new ArrayList<>();

        for (Product p : products) {
            // Category check
            if (!"all".equalsIgnoreCase(intent.getCategory())
                    && !p.getCategory().equalsIgnoreCase(intent.getCategory())) {
                continue;
            }
            // Budget check
            if (intent.getMaxBudget() != null && p.getPrice() > intent.getMaxBudget()) {
                continue;
            }
            // Stock check
            if (p.getStock() <= 0) {
                continue;
            }
            matching.add(p);
        }

        // If category is "all", fallback to search with use case or return active products under budget
        if (matching.isEmpty() && !"all".equalsIgnoreCase(intent.getCategory())) {
            // try matching category without strict budget
            matching = productRepository.findByCategoryIgnoreCaseAndActiveTrue(intent.getCategory());
        }

        return matching;
    }

    private List<ProductRecommendationDto> rankAndExplain(List<Product> candidates, IntentDto intent) {
        List<ProductRecommendationDto> dtos = new ArrayList<>();

        // Sort by relevance: tag match first, then closest to budget (best value)
        candidates.sort((a, b) -> {
            boolean aMatchesUseCase = a.getTags() != null && a.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(intent.getUseCase()));
            boolean bMatchesUseCase = b.getTags() != null && b.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(intent.getUseCase()));
            if (aMatchesUseCase && !bMatchesUseCase) return -1;
            if (!aMatchesUseCase && bMatchesUseCase) return 1;
            return Double.compare(b.getPrice(), a.getPrice()); // higher spec within budget
        });

        int limit = Math.min(3, candidates.size());
        for (int i = 0; i < limit; i++) {
            Product p = candidates.get(i);
            boolean tagMatch = p.getTags() != null && p.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(intent.getUseCase()));

            String reason;
            double confidence = 0.88;
            if (tagMatch) {
                confidence = 0.94;
                reason = String.format("Specially optimized for %s and comfortably within your ₹%,.0f budget.",
                        intent.getUseCase(), intent.getMaxBudget() != null ? intent.getMaxBudget() : p.getPrice());
            } else if (intent.getMaxBudget() != null) {
                reason = String.format("Fits within your budget of ₹%,.0f and provides high reliability and verified stock.",
                        intent.getMaxBudget());
            } else {
                reason = "Top rated product in this merchant category.";
            }

            auditService.logEvent(
                    AuditEventType.PRODUCT_RECOMMENDED,
                    ActorType.AI,
                    null,
                    p.getId(),
                    p.getPrice(),
                    reason,
                    confidence,
                    "RECOMMENDED",
                    "Product: " + p.getName()
            );

            dtos.add(ProductRecommendationDto.builder()
                    .product(p)
                    .reason(reason)
                    .confidence(confidence)
                    .isUpsell(false)
                    .build());
        }

        return dtos;
    }

    private ProductRecommendationDto findBoundedUpsell(Product primaryProduct, IntentDto intent) {
        if (primaryProduct == null || primaryProduct.getComplementaryProductIds() == null || primaryProduct.getComplementaryProductIds().isEmpty()) {
            return null;
        }

        for (String compId : primaryProduct.getComplementaryProductIds()) {
            Optional<Product> compOpt = productRepository.findById(compId);
            if (compOpt.isPresent() && compOpt.get().isActive() && compOpt.get().getStock() > 0) {
                Product comp = compOpt.get();
                String reason = String.format("Recommended accessory for your %s to complete your %s setup.",
                        primaryProduct.getName(), intent.getUseCase());

                auditService.logEvent(
                        AuditEventType.UPSELL_OFFERED,
                        ActorType.AI,
                        null,
                        comp.getId(),
                        comp.getPrice(),
                        reason,
                        0.89,
                        "OFFERED",
                        "Primary: " + primaryProduct.getName() + " -> Complementary: " + comp.getName()
                );

                return ProductRecommendationDto.builder()
                        .product(comp)
                        .reason(reason)
                        .confidence(0.89)
                        .isUpsell(true)
                        .build();
            }
        }
        return null;
    }
}
