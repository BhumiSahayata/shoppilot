package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.demo.DemoDataSeeder;
import com.shoppilot.shoppilot.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoDataSeeder demoDataSeeder;

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seedData() {
        demoDataSeeder.seedDemoData();
        return ResponseEntity.ok(ApiResponse.ok("Demo data verified/seeded successfully in MongoDB Atlas", "SEEDED"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetData() {
        demoDataSeeder.resetAndSeed();
        return ResponseEntity.ok(ApiResponse.ok("All data wiped and freshly re-seeded for demo in MongoDB Atlas", "RESET_COMPLETE"));
    }
}
