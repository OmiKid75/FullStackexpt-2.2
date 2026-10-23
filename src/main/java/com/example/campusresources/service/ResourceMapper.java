package com.example.campusresources.service;

import com.example.campusresources.dto.CategoryResponse;
import com.example.campusresources.dto.CategorySummary;
import com.example.campusresources.dto.ResourceResponse;
import com.example.campusresources.dto.ResourceSummaryResponse;
import com.example.campusresources.entity.Category;
import com.example.campusresources.entity.Resource;

/**
 * Entity -> DTO conversion. Reading resource.getCategory().getName() is what triggers the
 * extra query when the category was NOT fetched together with the resource (the N+1 problem).
 */
public final class ResourceMapper {

    private ResourceMapper() {
    }

    public static ResourceSummaryResponse toSummary(Resource r) {
        return new ResourceSummaryResponse(
                r.getId(), r.getTitle(), r.getAuthor(), r.getResourceType(), r.getRating(),
                r.getCategory().getId(), r.getCategory().getName(), r.getCreatedAt());
    }

    public static ResourceResponse toResponse(Resource r) {
        return new ResourceResponse(
                r.getId(), r.getTitle(), r.getDescription(), r.getAuthor(), r.getResourceType(), r.getRating(),
                new CategorySummary(r.getCategory().getId(), r.getCategory().getName()),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    public static CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
