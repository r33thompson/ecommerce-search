package io.github.r33thompson.ecommerce_search.web

import io.github.r33thompson.ecommerce_search.service.SearchRetrievalService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SearchController::class)
class SearchControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var service: SearchRetrievalService

    @Test
    fun `returns 200 and hits for a matching query`() {
        given(service.search("running")).willReturn(
            SearchResponse(
                query = "running",
                count = 1,
                tookMs = 5L,
                hits = listOf(
                    SearchHitDto(
                        id = "1",
                        name = "XT-6 Trail Running Shoe",
                        brand = "Salomon",
                        description = "Trail running shoe with Contagrip outsole, EnergyCell midsole, and Quicklace closure — equal parts technical and streetwear.",
                        price = 199.99,
                        score = 1.42f,
                    ),
                ),
            ),
        )

        mockMvc.perform(get("/api/search").param("q", "running"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.query").value("running"))
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.hits[0].brand").value("Salomon"))
    }

    @Test
    fun `returns 200 with empty hits for a no-match query`() {
        given(service.search("zzzz")).willReturn(
            SearchResponse(query = "zzzz", count = 0, tookMs = 3L, hits = emptyList()),
        )

        mockMvc.perform(get("/api/search").param("q", "zzzz"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.hits").isEmpty)
    }
}
