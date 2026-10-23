package com.example.campusresources.dto;

/** One row of the native SQL statistics query. */
public record CategoryStatisticsResponse(
        Long categoryId,
        String categoryName,
        long resourceCount,
        double averageRating) {
}
