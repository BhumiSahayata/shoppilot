package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.ai.AiShoppingService;
import com.shoppilot.shoppilot.dto.ApiResponse;
import com.shoppilot.shoppilot.dto.AiShoppingResponse;
import com.shoppilot.shoppilot.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AiShoppingService aiShoppingService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiShoppingResponse>> chat(@Valid @RequestBody ChatRequest request) {
        AiShoppingResponse response = aiShoppingService.processShoppingRequest(request.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
