package com.example.campusresources.performance;

import com.example.campusresources.dto.QueryDemoResult;
import com.example.campusresources.entity.Resource;
import com.example.campusresources.exception.InvalidRequestException;
import com.example.campusresources.repository.ResourceRepository;
import com.example.campusresources.service.ResourceMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Experiment 2.2.2 (A + B): the N+1 query problem next to its JOIN FETCH fix.
 *
 * Both methods load the same page of resources and convert them to DTOs (which reads the
 * category name). The only difference is HOW the resources are loaded. Hibernate statistics
 * count the SQL statements, so the difference is visible as a number.
 *
 * Each method is @Transactional so the Hibernate session stays open while the lazy category
 * is read. The controller calls the two methods separately through the Spring proxy.
 * NOTE: the counters are global to the JVM, so run this demo while no other traffic is hitting the API.
 */
@Service
public class QueryComparisonService {

    private static final int MAX_SIZE = 100;

    private final ResourceRepository resourceRepository;
    private final EntityManagerFactory entityManagerFactory;

    public QueryComparisonService(ResourceRepository resourceRepository, EntityManagerFactory entityManagerFactory) {
        this.resourceRepository = resourceRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    /** INEFFICIENT on purpose: plain findAll(Pageable) leaves the category LAZY, so each distinct category is loaded by its own query. */
    @Transactional(readOnly = true)
    public QueryDemoResult runNPlusOne(int page, int size) {
        validate(page, size);
        Statistics stats = startStatistics();
        long start = System.nanoTime();

        Page<Resource> result = resourceRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
        int mapped = result.getContent().stream().map(ResourceMapper::toSummary).toList().size();

        return finish("N+1 (lazy loading)",
                "1 query loads the resources, 1 query counts them, then 1 extra query runs for every "
                        + "distinct category touched while reading resource.getCategory().getName().",
                mapped, stats, start);
    }

    /** OPTIMISED: JOIN FETCH loads resource + category in one SQL statement. */
    @Transactional(readOnly = true)
    public QueryDemoResult runJoinFetch(int page, int size) {
        validate(page, size);
        Statistics stats = startStatistics();
        long start = System.nanoTime();

        Page<Resource> result = resourceRepository.findAllWithCategory(PageRequest.of(page, size, Sort.by("id")));
        int mapped = result.getContent().stream().map(ResourceMapper::toSummary).toList().size();

        return finish("JOIN FETCH",
                "1 query loads resources together with their categories, 1 query counts them. "
                        + "Reading the category causes no further queries.",
                mapped, stats, start);
    }

    private Statistics startStatistics() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }

    private QueryDemoResult finish(String approach, String explanation, int count, Statistics stats, long startNanos) {
        double millis = (System.nanoTime() - startNanos) / 1_000_000.0;
        return new QueryDemoResult(approach, explanation, count, stats.getPrepareStatementCount(),
                Math.round(millis * 100.0) / 100.0);
    }

    private void validate(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
