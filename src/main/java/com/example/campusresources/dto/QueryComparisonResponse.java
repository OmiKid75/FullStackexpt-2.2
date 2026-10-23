package com.example.campusresources.dto;

/** Side-by-side result of the N+1 demo and the JOIN FETCH demo. */
public record QueryComparisonResponse(
        int page,
        int size,
        QueryDemoResult nPlusOne,
        QueryDemoResult joinFetch,
        String conclusion) {
}
