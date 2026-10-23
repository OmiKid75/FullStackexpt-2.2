package com.example.campusresources.repository;

import com.example.campusresources.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // ------------------------------------------------------------------
    // Experiment 2.2.2 (B): JPQL JOIN FETCH queries.
    // The category (a to-ONE relation) is loaded in the SAME SQL statement as the resource,
    // so reading resource.getCategory() later causes no extra query.
    // JOIN FETCH on a to-one relation is safe with pagination (LIMIT/OFFSET stay in SQL).
    // Each paginated query has an explicit countQuery WITHOUT the fetch join.
    // ------------------------------------------------------------------

    @Query(value = "SELECT r FROM Resource r JOIN FETCH r.category",
            countQuery = "SELECT COUNT(r) FROM Resource r")
    Page<Resource> findAllWithCategory(Pageable pageable);

    @Query(value = "SELECT r FROM Resource r JOIN FETCH r.category c WHERE c.id = :categoryId",
            countQuery = "SELECT COUNT(r) FROM Resource r WHERE r.category.id = :categoryId")
    Page<Resource> findByCategoryWithCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT r FROM Resource r JOIN FETCH r.category c WHERE "
            + "LOWER(r.title) LIKE :pattern OR LOWER(r.description) LIKE :pattern OR LOWER(r.author) LIKE :pattern",
            countQuery = "SELECT COUNT(r) FROM Resource r WHERE "
                    + "LOWER(r.title) LIKE :pattern OR LOWER(r.description) LIKE :pattern OR LOWER(r.author) LIKE :pattern")
    Page<Resource> searchWithCategory(@Param("pattern") String pattern, Pageable pageable);

    @Query(value = "SELECT r FROM Resource r JOIN FETCH r.category c WHERE c.id = :categoryId AND "
            + "(LOWER(r.title) LIKE :pattern OR LOWER(r.description) LIKE :pattern OR LOWER(r.author) LIKE :pattern)",
            countQuery = "SELECT COUNT(r) FROM Resource r WHERE r.category.id = :categoryId AND "
                    + "(LOWER(r.title) LIKE :pattern OR LOWER(r.description) LIKE :pattern OR LOWER(r.author) LIKE :pattern)")
    Page<Resource> searchByCategoryWithCategory(@Param("categoryId") Long categoryId,
                                                @Param("pattern") String pattern,
                                                Pageable pageable);

    @Query("SELECT r FROM Resource r JOIN FETCH r.category WHERE r.id = :id")
    Optional<Resource> findByIdWithCategory(@Param("id") Long id);

    long countByCategoryId(Long categoryId);

    // ------------------------------------------------------------------
    // Experiment 2.2.2 (D): native SQL query.
    // Plain SQL against the real table/column names (not entity names), executed as written.
    // Each row is Object[] { category id, category name, resource count, average rating }.
    // The service converts rows to CategoryStatisticsResponse. Mapping by position avoids
    // problems with column-alias capitalisation differences between databases.
    // ------------------------------------------------------------------
    @Query(value = "SELECT c.id, c.name, COUNT(r.id), ROUND(AVG(r.rating), 2) "
            + "FROM categories c LEFT JOIN resources r ON r.category_id = c.id "
            + "GROUP BY c.id, c.name "
            + "ORDER BY COUNT(r.id) DESC, c.name ASC",
            nativeQuery = true)
    List<Object[]> countResourcesGroupedByCategory();
}
