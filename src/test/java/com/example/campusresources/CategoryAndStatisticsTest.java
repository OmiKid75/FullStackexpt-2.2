package com.example.campusresources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Category endpoints, category cache invalidation, and the native SQL statistics endpoint. */
class CategoryAndStatisticsTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("List categories (sorted by name)")
    void listCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Algorithms"));
    }

    @Test
    @DisplayName("Get category by id, and 404 for a missing one")
    void getCategory() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", databases.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Databases"));
        mockMvc.perform(get("/api/categories/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create category: 201, then the cached category list shows it (cache evicted)")
    void createCategoryEvictsList() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$", hasSize(3)));    // fills the cache
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathematics\",\"description\":\"Maths\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("Duplicate category name returns 409; blank name returns 400")
    void createInvalidCategory() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"programming\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Renaming a category refreshes cached category AND cached resource responses")
    void updateCategoryEvictsRelatedCaches() throws Exception {
        Long resourceId = resources.stream().filter(r -> r.getCategory().getId().equals(programming.getId()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/categories/{id}", programming.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/api/resources/{id}", resourceId))
                .andExpect(jsonPath("$.category.name").value("Programming"));

        mockMvc.perform(put("/api/categories/{id}", programming.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Software Development\",\"description\":\"Renamed\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories/{id}", programming.getId()))
                .andExpect(jsonPath("$.name").value("Software Development"));
        mockMvc.perform(get("/api/resources/{id}", resourceId))
                .andExpect(jsonPath("$.category.name").value("Software Development"));
    }

    @Test
    @DisplayName("Deleting a category that still has resources returns 409")
    void deleteCategoryWithResources() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", programming.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("still contains 8 resource")));
        assertThat(categoryRepository.existsById(programming.getId())).isTrue();
    }

    @Test
    @DisplayName("Deleting an empty category returns 204; deleting a missing one returns 404")
    void deleteEmptyCategory() throws Exception {
        String json = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temporary\"}"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        mockMvc.perform(delete("/api/categories/{id}", id.longValue())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/categories/{id}", id.longValue())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/categories/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Native SQL statistics: resource count and average rating per category")
    void nativeStatistics() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Empty Category\"}")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/statistics/resources-by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].categoryName").value("Databases"))     // 9 resources, largest
                .andExpect(jsonPath("$[0].resourceCount").value(9))
                .andExpect(jsonPath("$[1].resourceCount").value(8))
                .andExpect(jsonPath("$[3].categoryName").value("Empty Category"))  // LEFT JOIN keeps empty categories
                .andExpect(jsonPath("$[3].resourceCount").value(0))
                .andExpect(jsonPath("$[0].averageRating").exists());
    }
}
