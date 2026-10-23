package com.example.campusresources.service.impl;

import com.example.campusresources.dto.PageResponse;
import com.example.campusresources.dto.ResourceRequest;
import com.example.campusresources.dto.ResourceResponse;
import com.example.campusresources.dto.ResourceSummaryResponse;
import com.example.campusresources.entity.Category;
import com.example.campusresources.entity.Resource;
import com.example.campusresources.exception.InvalidRequestException;
import com.example.campusresources.exception.ResourceNotFoundException;
import com.example.campusresources.performance.CacheDiagnostics;
import com.example.campusresources.repository.CategoryRepository;
import com.example.campusresources.repository.ResourceRepository;
import com.example.campusresources.service.ResourceMapper;
import com.example.campusresources.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ResourceServiceImpl implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);

    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_KEYWORD_LENGTH = 100;

    /** Only these fields may be used for sorting (prevents arbitrary / invalid sort fields). */
    public static final List<String> ALLOWED_SORT_FIELDS =
            List.of("id", "title", "author", "resourceType", "rating", "createdAt", "updatedAt");

    private final ResourceRepository resourceRepository;
    private final CategoryRepository categoryRepository;
    private final CacheDiagnostics cacheDiagnostics;

    public ResourceServiceImpl(ResourceRepository resourceRepository,
                               CategoryRepository categoryRepository,
                               CacheDiagnostics cacheDiagnostics) {
        this.resourceRepository = resourceRepository;
        this.categoryRepository = categoryRepository;
        this.cacheDiagnostics = cacheDiagnostics;
    }

    // =====================================================================
    // Experiment 2.2.1 - pagination, sorting, filtering
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceSummaryResponse> getResources(int page, int size, String sortBy, String direction,
                                                              Long categoryId, String keyword) {
        Sort.Direction sortDirection = parseDirection(direction);
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category with id " + categoryId + " was not found");
        }

        String pattern = null;
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            if (trimmed.length() > MAX_KEYWORD_LENGTH) {
                throw new InvalidRequestException("keyword must be at most " + MAX_KEYWORD_LENGTH + " characters");
            }
            pattern = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
        }

        // The JOIN FETCH repository methods keep pagination in SQL (LIMIT/OFFSET) AND load the
        // category in the same statement, so mapping to DTOs triggers no extra queries.
        Page<Resource> result;
        if (pattern == null) {
            result = (categoryId == null)
                    ? resourceRepository.findAllWithCategory(pageable)
                    : resourceRepository.findByCategoryWithCategory(categoryId, pageable);
        } else {
            result = (categoryId == null)
                    ? resourceRepository.searchWithCategory(pattern, pageable)
                    : resourceRepository.searchByCategoryWithCategory(categoryId, pattern, pageable);
        }

        log.debug("Listing resources page={} size={} sortBy={} direction={} categoryId={} keyword={} -> {} rows",
                page, size, sortBy, sortDirection, categoryId, keyword, result.getNumberOfElements());

        return PageResponse.from(result.map(ResourceMapper::toSummary), sortBy, sortDirection.name().toLowerCase(Locale.ROOT));
    }

    private Pageable buildPageable(int page, int size, String sortBy, Sort.Direction direction) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("Invalid sortBy value. Allowed values: " + String.join(", ", ALLOWED_SORT_FIELDS));
        }
        Sort sort = Sort.by(direction, sortBy);
        if (!"id".equals(sortBy)) {
            // Stable secondary sort: rows with equal sort values always come back in the same order,
            // so a row can never appear on two pages or be skipped between pages.
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    private Sort.Direction parseDirection(String direction) {
        if (direction != null) {
            if (direction.equalsIgnoreCase("asc")) {
                return Sort.Direction.ASC;
            }
            if (direction.equalsIgnoreCase("desc")) {
                return Sort.Direction.DESC;
            }
        }
        throw new InvalidRequestException("direction must be 'asc' or 'desc'");
    }

    // =====================================================================
    // Experiment 2.2.2 - caching
    // =====================================================================

    /**
     * @Cacheable: Spring checks the "resources" cache first (key = id).
     * - HIT : the method body is skipped, the cached object is returned.
     * - MISS: the body runs (one DB query), the result is stored in the cache.
     * The log line below therefore only appears on a cache MISS.
     */
    @Override
    @Cacheable(cacheNames = "resources", key = "#id")
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        log.info("CACHE MISS - loading resource {} from the database", id);
        cacheDiagnostics.recordResourceDbLoad();
        return loadResource(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceByIdUncached(Long id) {
        return loadResource(id);
    }

    private ResourceResponse loadResource(Long id) {
        Resource resource = resourceRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with id " + id + " was not found"));
        return ResourceMapper.toResponse(resource);
    }

    // =====================================================================
    // Create / update / delete (with cache invalidation)
    // =====================================================================
    @Override
    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Category category = findCategory(request.categoryId());
        Resource resource = new Resource();
        applyRequest(resource, request, category);
        Resource saved = resourceRepository.saveAndFlush(resource);
        log.info("Created resource {}", saved.getId());
        return ResourceMapper.toResponse(saved);
    }

    /** @CacheEvict removes the stale entry so the next GET reloads fresh data from the database. */
    @Override
    @CacheEvict(cacheNames = "resources", key = "#id")
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with id " + id + " was not found"));
        Category category = findCategory(request.categoryId());
        applyRequest(resource, request, category);
        Resource saved = resourceRepository.saveAndFlush(resource);
        log.info("Updated resource {} (cache entry evicted)", id);
        return ResourceMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = "resources", key = "#id")
    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource with id " + id + " was not found");
        }
        resourceRepository.deleteById(id);
        log.info("Deleted resource {} (cache entry evicted)", id);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + categoryId + " was not found"));
    }

    private void applyRequest(Resource resource, ResourceRequest request, Category category) {
        resource.setTitle(request.title().trim());
        resource.setDescription(request.description());
        resource.setAuthor(request.author());
        resource.setResourceType(request.resourceType());
        resource.setRating(request.rating() == null ? 0.0 : request.rating());
        resource.setCategory(category);
    }
}
