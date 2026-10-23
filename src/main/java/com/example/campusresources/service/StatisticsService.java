package com.example.campusresources.service;

import com.example.campusresources.dto.CategoryStatisticsResponse;

import java.util.List;

public interface StatisticsService {

    /** Uses the native SQL query. */
    List<CategoryStatisticsResponse> getResourcesByCategory();
}
