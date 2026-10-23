package com.example.campusresources.controller;

import com.example.campusresources.dto.CategoryStatisticsResponse;
import com.example.campusresources.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** Experiment 2.2.2 (D): native SQL GROUP BY query. */
    @GetMapping("/resources-by-category")
    public List<CategoryStatisticsResponse> getResourcesByCategory() {
        return statisticsService.getResourcesByCategory();
    }
}
