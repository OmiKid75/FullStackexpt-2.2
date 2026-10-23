package com.example.campusresources;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Experiment 2.2.2 (A + B): the N+1 demo must execute more SQL statements than JOIN FETCH. */
class QueryOptimizationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("JOIN FETCH uses fewer SQL statements than the N+1 approach for the same page")
    void joinFetchExecutesFewerStatements() throws Exception {
        String json = mockMvc.perform(get("/api/demo/compare").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long nPlusOne = ((Number) JsonPath.read(json, "$.nPlusOne.sqlStatementCount")).longValue();
        long joinFetch = ((Number) JsonPath.read(json, "$.joinFetch.sqlStatementCount")).longValue();
        int rowsBad = ((Number) JsonPath.read(json, "$.nPlusOne.resourcesReturned")).intValue();
        int rowsGood = ((Number) JsonPath.read(json, "$.joinFetch.resourcesReturned")).intValue();

        assertThat(rowsBad).isEqualTo(10);
        assertThat(rowsGood).isEqualTo(10);
        // JOIN FETCH: 1 data query + 1 count query. N+1: the same two plus one query per distinct category (3 here).
        assertThat(joinFetch).isLessThanOrEqualTo(2);
        assertThat(nPlusOne).isGreaterThan(joinFetch);
    }

    @Test
    @DisplayName("Demo endpoints validate their parameters")
    void demoValidation() throws Exception {
        mockMvc.perform(get("/api/demo/n-plus-one").param("size", "500")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/demo/join-fetch").param("page", "-2")).andExpect(status().isBadRequest());
    }
}
