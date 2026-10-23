package com.example.campusresources;

import com.example.campusresources.entity.Category;
import com.example.campusresources.entity.Resource;
import com.example.campusresources.performance.CacheDiagnostics;
import com.example.campusresources.repository.CategoryRepository;
import com.example.campusresources.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the full application with the "test" profile (H2 in-memory database, no MySQL needed)
 * and inserts the same small, predictable data set before every test:
 *   3 categories and 25 resources (i = 1..25)
 *   - category   = categories[i % 3]   (Programming 8, Databases 9, Algorithms 8 resources)
 *   - title      = "Java Guide NN" for even i (12 resources), "SQL Handbook NN" for odd i (13)
 *   - rating     = (i % 5) + 1   -> 1.0 .. 5.0
 *   - createdAt  = 2025-01-01 + i days
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ResourceRepository resourceRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected CacheManager cacheManager;
    @Autowired
    protected CacheDiagnostics cacheDiagnostics;

    protected Category programming;
    protected Category databases;
    protected Category algorithms;
    protected List<Resource> resources;

    @BeforeEach
    void resetData() {
        resourceRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        cacheDiagnostics.clearAll();

        programming = categoryRepository.save(new Category("Programming", "Programming topics"));
        databases = categoryRepository.save(new Category("Databases", "Database topics"));
        algorithms = categoryRepository.save(new Category("Algorithms", "Algorithm topics"));
        Category[] categories = {programming, databases, algorithms};

        List<Resource> toSave = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Resource r = new Resource();
            r.setTitle((i % 2 == 0 ? "Java Guide " : "SQL Handbook ") + String.format("%02d", i));
            r.setDescription("Description " + i);
            r.setAuthor("Author " + i);
            r.setResourceType("BOOK");
            r.setRating((i % 5) + 1.0);
            r.setCategory(categories[i % 3]);
            r.setCreatedAt(LocalDateTime.of(2025, 1, 1, 9, 0).plusDays(i));
            toSave.add(r);
        }
        resources = resourceRepository.saveAll(toSave);
    }
}
