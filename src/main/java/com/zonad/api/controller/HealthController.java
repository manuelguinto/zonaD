package com.zonad.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> estado() {
        return Map.of(
                "service", "Zona D API",
                "status", "UP"
        );
    }
}
