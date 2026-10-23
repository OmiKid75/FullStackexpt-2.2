package com.example.campusresources.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", "Campus Library Resource Explorer API");
        info.put("resources", "/api/resources?page=0&size=10&sortBy=createdAt&direction=desc");
        info.put("categories", "/api/categories");
        info.put("statistics", "/api/statistics/resources-by-category");
        info.put("diagnostics", "/api/demo/compare?size=50 , /api/diagnostics/cache");
        return info;
    }
}
