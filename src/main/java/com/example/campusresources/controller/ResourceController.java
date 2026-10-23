package com.example.campusresources.controller;

import com.example.campusresources.dto.PageResponse;
import com.example.campusresources.dto.ResourceRequest;
import com.example.campusresources.dto.ResourceResponse;
import com.example.campusresources.dto.ResourceSummaryResponse;
import com.example.campusresources.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /** Experiment 2.2.1. page is zero-based; size 1-100; sortBy must be an approved field. */
    @GetMapping
    public PageResponse<ResourceSummaryResponse> getResources(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return resourceService.getResources(page, size, sortBy, direction, categoryId, keyword);
    }

    @GetMapping("/{id}")
    public ResourceResponse getResource(@PathVariable("id") Long id) {
        return resourceService.getResourceById(id);
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResourceResponse updateResource(@PathVariable("id") Long id, @Valid @RequestBody ResourceRequest request) {
        return resourceService.updateResource(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable("id") Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
