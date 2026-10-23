package com.example.campusresources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Experiment 2.2.1 - pagination, sorting, validation and filters. */
class ResourcePaginationAndSortingTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Default listing: page 0, size 10, newest first")
    void defaultListing() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false))
                .andExpect(jsonPath("$.content[0].title").value("SQL Handbook 25"));
    }

    @Test
    @DisplayName("Last page contains the remaining rows and has no next page")
    void lastPage() throws Exception {
        mockMvc.perform(get("/api/resources").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    @DisplayName("Custom page size")
    void customSize() throws Exception {
        mockMvc.perform(get("/api/resources").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("Sort by title ascending")
    void sortByTitleAsc() throws Exception {
        mockMvc.perform(get("/api/resources").param("sortBy", "title").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Guide 02"))
                .andExpect(jsonPath("$.content[1].title").value("Java Guide 04"));
    }

    @Test
    @DisplayName("Sort by rating descending: highest ratings first")
    void sortByRatingDesc() throws Exception {
        mockMvc.perform(get("/api/resources").param("sortBy", "rating").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5.0))
                .andExpect(jsonPath("$.content[4].rating").value(5.0))
                .andExpect(jsonPath("$.content[5].rating").value(4.0));
    }

    @Test
    @DisplayName("Equal sort values keep a stable order (secondary sort by id)")
    void stableOrderingAcrossPages() throws Exception {
        // 5 resources share rating 5.0, so ordering depends on the secondary sort.
        String first = mockMvc.perform(get("/api/resources").param("sortBy", "rating").param("direction", "desc").param("size", "3"))
                .andReturn().getResponse().getContentAsString();
        String again = mockMvc.perform(get("/api/resources").param("sortBy", "rating").param("direction", "desc").param("size", "3"))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(again).isEqualTo(first);
    }

    @Test
    @DisplayName("Invalid page size is rejected with 400")
    void invalidSize() throws Exception {
        mockMvc.perform(get("/api/resources").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/resources").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size must be between 1 and 100")));
    }

    @Test
    @DisplayName("Negative page and non-numeric page are rejected with 400")
    void invalidPage() throws Exception {
        mockMvc.perform(get("/api/resources").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/resources").param("page", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unapproved sort field is rejected with 400 (no arbitrary sorting)")
    void invalidSortField() throws Exception {
        mockMvc.perform(get("/api/resources").param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Allowed values")));
    }

    @Test
    @DisplayName("Invalid direction is rejected with 400")
    void invalidDirection() throws Exception {
        mockMvc.perform(get("/api/resources").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("asc")));
    }

    @Test
    @DisplayName("Filter by category")
    void filterByCategory() throws Exception {
        mockMvc.perform(get("/api/resources").param("categoryId", programming.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.content[0].categoryName").value("Programming"));
    }

    @Test
    @DisplayName("Filter by a category that does not exist returns 404")
    void filterByMissingCategory() throws Exception {
        mockMvc.perform(get("/api/resources").param("categoryId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Keyword filter is case-insensitive")
    void filterByKeyword() throws Exception {
        mockMvc.perform(get("/api/resources").param("keyword", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    @DisplayName("Category and keyword filters combine")
    void filterByCategoryAndKeyword() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .param("categoryId", programming.getId().toString())
                        .param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("Keyword with no matches returns an empty page, not an error")
    void keywordWithoutMatches() throws Exception {
        mockMvc.perform(get("/api/resources").param("keyword", "zzzzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
