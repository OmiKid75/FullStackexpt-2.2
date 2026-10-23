package com.example.campusresources.dto;

/** Result of one run of the N+1 / JOIN FETCH demonstration. */
public record QueryDemoResult(
        String approach,
        String explanation,
        int resourcesReturned,
        long sqlStatementCount,
        double durationMs) {
}
