package com.example.campusresources.service.impl;

import com.example.campusresources.dto.CategoryStatisticsResponse;
import com.example.campusresources.repository.ResourceRepository;
import com.example.campusresources.service.StatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final ResourceRepository resourceRepository;

    public StatisticsServiceImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryStatisticsResponse> getResourcesByCategory() {
        return resourceRepository.countResourcesGroupedByCategory().stream()
                .map(row -> new CategoryStatisticsResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] == null ? 0.0 : ((Number) row[3]).doubleValue()))
                .toList();
    }
}
