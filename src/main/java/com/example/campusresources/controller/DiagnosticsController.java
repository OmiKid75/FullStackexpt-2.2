package com.example.campusresources.controller;

import com.example.campusresources.performance.CacheDiagnostics;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final CacheDiagnostics cacheDiagnostics;

    public DiagnosticsController(CacheDiagnostics cacheDiagnostics) {
        this.cacheDiagnostics = cacheDiagnostics;
    }

    /** Cache hit/miss statistics and the number of real database loads. */
    @GetMapping("/cache")
    public Map<String, Object> cacheStatistics() {
        return cacheDiagnostics.snapshot();
    }

    /** Empties every cache (to start a benchmark with a cold cache). */
    @DeleteMapping("/cache")
    public Map<String, String> clearCaches() {
        cacheDiagnostics.clearAll();
        return Map.of("message", "All caches cleared");
    }
}
