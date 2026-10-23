package com.example.campusresources.controller;

import com.example.campusresources.dto.QueryComparisonResponse;
import com.example.campusresources.dto.QueryDemoResult;
import com.example.campusresources.dto.ResourceResponse;
import com.example.campusresources.performance.QueryComparisonService;
import com.example.campusresources.service.ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Experiment 2.2.2 demonstration endpoints. They exist for teaching and benchmarking only. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final QueryComparisonService queryComparisonService;
    private final ResourceService resourceService;

    public DemoController(QueryComparisonService queryComparisonService, ResourceService resourceService) {
        this.queryComparisonService = queryComparisonService;
        this.resourceService = resourceService;
    }

    /** The INEFFICIENT approach (N+1). */
    @GetMapping("/n-plus-one")
    public QueryDemoResult nPlusOne(@RequestParam(name = "page", defaultValue = "0") int page,
                                    @RequestParam(name = "size", defaultValue = "50") int size) {
        return queryComparisonService.runNPlusOne(page, size);
    }

    /** The OPTIMISED approach (JOIN FETCH). */
    @GetMapping("/join-fetch")
    public QueryDemoResult joinFetch(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "50") int size) {
        return queryComparisonService.runJoinFetch(page, size);
    }

    /** Runs both approaches on the same page and returns the SQL statement counts side by side. */
    @GetMapping("/compare")
    public QueryComparisonResponse compare(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "size", defaultValue = "50") int size) {
        QueryDemoResult bad = queryComparisonService.runNPlusOne(page, size);
        QueryDemoResult good = queryComparisonService.runJoinFetch(page, size);
        String conclusion = "N+1 executed " + bad.sqlStatementCount() + " SQL statements; JOIN FETCH executed "
                + good.sqlStatementCount() + " for the same " + good.resourcesReturned() + " resources.";
        return new QueryComparisonResponse(page, size, bad, good, conclusion);
    }

    /** Same data as GET /api/resources/{id} but bypassing the cache - the "before caching" baseline. */
    @GetMapping("/resources/{id}/uncached")
    public ResourceResponse uncachedResource(@PathVariable("id") Long id) {
        return resourceService.getResourceByIdUncached(id);
    }
}
