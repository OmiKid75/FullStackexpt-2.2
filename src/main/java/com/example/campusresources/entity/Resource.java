package com.example.campusresources.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An academic resource (book, tutorial, study guide ...).
 * The indexes speed up the common sort/filter columns (query optimisation).
 */
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resources_category", columnList = "category_id"),
        @Index(name = "idx_resources_created_at", columnList = "created_at"),
        @Index(name = "idx_resources_rating", columnList = "rating"),
        @Index(name = "idx_resources_title", columnList = "title")
})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "author", length = 150)
    private String author;

    /**
     * LAZY on purpose: the category is only loaded when it is accessed.
     * That is what makes the N+1 problem possible - and JOIN FETCH the cure.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(name = "rating", nullable = false)
    private Double rating = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;          // seed data may set its own deterministic value
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
