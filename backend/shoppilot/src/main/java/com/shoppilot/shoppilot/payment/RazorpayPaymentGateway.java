package com.shoppilot.shoppilot.payment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.shoppilot.shoppilot.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@org.springframework.context.annotation.Primary
@Component
public class RazorpayPaymentGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final SimulatedPaymentGateway fallbackGateway;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RazorpayPaymentGateway(@Value("${razorpay.key.id:}") String keyId,
                                  @Value("${razorpay.key.secret:}") String keySecret,
                                  SimulatedPaymentGateway fallbackGateway,
                                  ObjectMapper objectMapper) {
        this.keyId = (keyId != null) ? keyId.trim() : "";
        this.keySecret = (keySecret != null) ? keySecret.trim() : "";
        this.fallbackGateway = fallbackGateway;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if (isConfigured()) {
            log.info("[RAZORPAY] Razorpay TEST MODE configured with Key ID: {}****",
                    this.keyId.substring(0, Math.min(6, this.keyId.length())));
        } else {
            log.warn("[RAZORPAY] No RAZORPAY_KEY_ID configured. Operating in [SIMULATED_OFFLINE_MODE] fallback.");
        }
    }

    public boolean isConfigured() {
        return !keyId.isEmpty() && !keySecret.isEmpty() && !keyId.equalsIgnoreCase("rzp_test_placeholder");
    }

    @Override
    public PaymentOrderResult createOrder(String receipt, double amountInInr) {
        if (!isConfigured()) {
            return fallbackGateway.createOrder(receipt, amountInInr);
        }

        try {
            long amountInPaise = Math.round(amountInInr * 100);
            String jsonPayload = objectMapper.writeValueAsString(Map.of(
                    "amount", amountInPaise,
                    "currency", "INR",
                    "receipt", receipt
            ));

            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String rzpOrderId = root.path("id").asText();
                String status = root.path("status").asText("created");

                log.info("[RAZORPAY_TEST] Successfully created order on Razorpay Test API: {}", rzpOrderId);
                return PaymentOrderResult.builder()
                        .orderId(rzpOrderId)
                        .amountInPaise(amountInPaise)
                        .currency("INR")
                        .status(status)
                        .gatewayMode("RAZORPAY_TEST")
                        .keyId(keyId)
                        .build();
            } else {
                log.error("[RAZORPAY_TEST] Failed creating Razorpay order. Status: {}, Body: {}",
                        response.statusCode(), response.body());
                throw new PaymentException("Razorpay test order creation failed: " + response.body());
            }
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception e) {
            log.error("[RAZORPAY_TEST] Exception calling Razorpay API", e);
            throw new PaymentException("Could not connect to Razorpay test API: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (!isConfigured()) {
            return fallbackGateway.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        }

        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            boolean matches = expectedSignature.equalsIgnoreCase(razorpaySignature);
            if (!matches) {
                log.warn("[RAZORPAY_TEST] Signature mismatch! Expected: {}, Received: {}", expectedSignature, razorpaySignature);
            }
            return matches;
        } catch (Exception e) {
            log.error("[RAZORPAY_TEST] Error computing HMAC signature", e);
            return false;
        }
    }

    @Override
    public String getGatewayMode() {
        return isConfigured() ? "RAZORPAY_TEST" : "SIMULATED_OFFLINE_MODE";
    }

    @Override
    public String getKeyId() {
        return isConfigured() ? keyId : "simulated_test_key";
    }
}
