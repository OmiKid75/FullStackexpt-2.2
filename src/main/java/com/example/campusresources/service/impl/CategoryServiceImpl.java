package com.example.campusresources.service.impl;

import com.example.campusresources.dto.CategoryRequest;
import com.example.campusresources.dto.CategoryResponse;
import com.example.campusresources.entity.Category;
import com.example.campusresources.exception.ConflictException;
import com.example.campusresources.exception.ResourceNotFoundException;
import com.example.campusresources.repository.CategoryRepository;
import com.example.campusresources.repository.ResourceRepository;
import com.example.campusresources.service.CategoryService;
import com.example.campusresources.service.ResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final ResourceRepository resourceRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ResourceRepository resourceRepository) {
        this.categoryRepository = categoryRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Cacheable(cacheNames = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("CACHE MISS - loading all categories from the database");
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(ResourceMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "categoryById", key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.info("CACHE MISS - loading category {} from the database", id);
        return categoryRepository.findById(id)
                .map(ResourceMapper::toCategoryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " was not found"));
    }

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        Category saved = categoryRepository.saveAndFlush(new Category(name, request.description()));
        log.info("Created category {}", saved.getId());
        return ResourceMapper.toCategoryResponse(saved);
    }

    /**
     * A category name is also embedded in cached resource responses, so a rename must clear
     * the "resources" cache as well as both category caches. Otherwise old names would be served.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "categoryById", key = "#id"),
            @CacheEvict(cacheNames = "categories", allEntries = true),
            @CacheEvict(cacheNames = "resources", allEntries = true)
    })
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " was not found"));
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        Category saved = categoryRepository.saveAndFlush(category);
        log.info("Updated category {} (category and resource caches evicted)", id);
        return ResourceMapper.toCategoryResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "categoryById", key = "#id"),
            @CacheEvict(cacheNames = "categories", allEntries = true),
            @CacheEvict(cacheNames = "resources", allEntries = true)
    })
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category with id " + id + " was not found");
        }
        long resourceCount = resourceRepository.countByCategoryId(id);
        if (resourceCount > 0) {
            throw new ConflictException("Category " + id + " cannot be deleted because it still contains "
                    + resourceCount + " resource(s). Move or delete those resources first.");
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category {} (caches evicted)", id);
    }
}
