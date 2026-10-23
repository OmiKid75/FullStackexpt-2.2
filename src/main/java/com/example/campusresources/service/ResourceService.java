package com.example.campusresources.service;

import com.example.campusresources.dto.PageResponse;
import com.example.campusresources.dto.ResourceRequest;
import com.example.campusresources.dto.ResourceResponse;
import com.example.campusresources.dto.ResourceSummaryResponse;

public interface ResourceService {

    /** Experiment 2.2.1: paginated, sortable, filterable listing. */
    PageResponse<ResourceSummaryResponse> getResources(int page, int size, String sortBy, String direction,
                                                       Long categoryId, String keyword);

    /** Experiment 2.2.2: cached with Ehcache (cache name "resources", key = id). */
    ResourceResponse getResourceById(Long id);

    /** Same data as above but NEVER cached. Used only to compare timings with and without the cache. */
    ResourceResponse getResourceByIdUncached(Long id);

    ResourceResponse createResource(ResourceRequest request);

    ResourceResponse updateResource(Long id, ResourceRequest request);

    void deleteResource(Long id);
}
