package com.example.campusresources;

import com.example.campusresources.entity.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Resource CRUD, error handling and Ehcache behaviour (Experiment 2.2.2 C). */
class ResourceCrudAndCacheTest extends AbstractIntegrationTest {

    private String body(String title, Long categoryId, String type, String rating) {
        return """
                {"title":"%s","description":"Some description","author":"Tester",
                 "categoryId":%d,"resourceType":"%s","rating":%s}
                """.formatted(title, categoryId, type, rating);
    }

    @Test
    @DisplayName("GET /api/resources/{id} returns the resource with its category")
    void getResourceById() throws Exception {
        Resource first = resources.get(0);
        mockMvc.perform(get("/api/resources/{id}", first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.getId()))
                .andExpect(jsonPath("$.title").value(first.getTitle()))
                .andExpect(jsonPath("$.category.name").value(first.getCategory().getName()));
    }

    @Test
    @DisplayName("Missing resource returns 404 with a clear message")
    void missingResource() throws Exception {
        mockMvc.perform(get("/api/resources/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    @DisplayName("Non-numeric id returns 400")
    void nonNumericId() throws Exception {
        mockMvc.perform(get("/api/resources/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create returns 201 with a Location header")
    void createResource() throws Exception {
        mockMvc.perform(post("/api/resources").contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Spring Guide", databases.getId(), "TUTORIAL", "4.5")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Spring Guide"))
                .andExpect(jsonPath("$.category.id").value(databases.getId()))
                .andExpect(jsonPath("$.createdAt").exists());
        assertThat(resourceRepository.count()).isEqualTo(26);
    }

    @Test
    @DisplayName("Create with invalid data returns 400 and lists the problems")
    void createInvalidResource() throws Exception {
        mockMvc.perform(post("/api/resources").contentType(MediaType.APPLICATION_JSON)
                        .content(body("", databases.getId(), "MAGAZINE", "9")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasItem(containsString("title"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("rating"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("resourceType"))));
    }

    @Test
    @DisplayName("Create with an unknown category returns 404")
    void createWithUnknownCategory() throws Exception {
        mockMvc.perform(post("/api/resources").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Orphan", 999999L, "BOOK", "3")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Malformed JSON returns 400 without leaking details")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/resources").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not valid JSON")));
    }

    @Test
    @DisplayName("Update changes the resource")
    void updateResource() throws Exception {
        Long id = resources.get(0).getId();
        mockMvc.perform(put("/api/resources/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Updated Title", algorithms.getId(), "STUDY_GUIDE", "2.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.resourceType").value("STUDY_GUIDE"))
                .andExpect(jsonPath("$.category.name").value("Algorithms"));
    }

    @Test
    @DisplayName("Updating a missing resource returns 404")
    void updateMissingResource() throws Exception {
        mockMvc.perform(put("/api/resources/{id}", 999999).contentType(MediaType.APPLICATION_JSON)
                        .content(body("X", databases.getId(), "BOOK", "3")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cache: second read is a HIT (no database load); update evicts it; next read is a MISS with fresh data")
    void cacheHitThenInvalidationAfterUpdate() throws Exception {
        Long id = resources.get(0).getId();
        long before = cacheDiagnostics.getResourceDbLoads();

        mockMvc.perform(get("/api/resources/{id}", id)).andExpect(status().isOk());      // MISS -> database
        mockMvc.perform(get("/api/resources/{id}", id)).andExpect(status().isOk());      // HIT  -> cache
        assertThat(cacheDiagnostics.getResourceDbLoads() - before).isEqualTo(1);

        mockMvc.perform(put("/api/resources/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Changed After Cache", databases.getId(), "BOOK", "4.0")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resources/{id}", id))                                  // MISS again (evicted)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Changed After Cache"));            // fresh, not stale
        assertThat(cacheDiagnostics.getResourceDbLoads() - before).isEqualTo(2);
    }

    @Test
    @DisplayName("Delete returns 204, evicts the cache entry, and later reads return 404")
    void deleteResource() throws Exception {
        Long id = resources.get(0).getId();
        mockMvc.perform(get("/api/resources/{id}", id)).andExpect(status().isOk());      // fills the cache
        mockMvc.perform(delete("/api/resources/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/resources/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/resources/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Uncached demo endpoint never touches the cache")
    void uncachedEndpointBypassesCache() throws Exception {
        Long id = resources.get(0).getId();
        long before = cacheDiagnostics.getResourceDbLoads();
        mockMvc.perform(get("/api/demo/resources/{id}/uncached", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/demo/resources/{id}/uncached", id)).andExpect(status().isOk());
        assertThat(cacheDiagnostics.getResourceDbLoads()).isEqualTo(before);
    }

    @Test
    @DisplayName("Diagnostics endpoint responds and the cache can be cleared")
    void diagnosticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/diagnostics/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceByIdDatabaseLoads").exists());
        mockMvc.perform(delete("/api/diagnostics/cache")).andExpect(status().isOk());
    }
}
