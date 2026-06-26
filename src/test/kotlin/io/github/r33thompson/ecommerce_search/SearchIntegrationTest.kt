package io.github.r33thompson.ecommerce_search

import io.github.r33thompson.ecommerce_search.domain.ProductIndexModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SearchIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val operations: ElasticsearchOperations,
) {
    @BeforeEach
    fun resetIndex() {
        val indexOps = operations.indexOps(ProductIndexModel::class.java)
        if (indexOps.exists()) indexOps.delete()
        indexOps.createWithMapping()
        operations.save(testProducts())
        indexOps.refresh()
    }

    @Test
    fun `fuzzy search matches a deliberately misspelled query`() {
        // 'runing' is one edit away from 'running' — fuzziness=AUTO should still match
        // products that contain 'running' in either name (id 1, 2) or description (id 5).
        mockMvc.perform(get("/api/search").param("q", "runing"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.hits[?(@.id=='1')]").exists())
            .andExpect(jsonPath("$.hits[?(@.id=='2')]").exists())
            .andExpect(jsonPath("$.hits[?(@.id=='5')]").exists())
    }

    @Test
    fun `name boost ranks name-matches above description-only matches`() {
        // 'running' appears in the name of id 1 and 2, and only in the description of id 5.
        // With name^3 vs description^1, the description-only match must rank last.
        mockMvc.perform(get("/api/search").param("q", "running"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.hits[2].id").value("5"))
    }

    private fun testProducts(): List<ProductIndexModel> = listOf(
        ProductIndexModel(
            id = "1",
            name = "XT-6 Trail Running Shoe",
            description = "Trail running shoe with Contagrip outsole.",
            brand = "Salomon",
            price = 199.99,
        ),
        ProductIndexModel(
            id = "2",
            name = "Ultraboost 22 Running Shoe",
            description = "Energy-returning Boost midsole.",
            brand = "Adidas",
            price = 179.99,
        ),
        ProductIndexModel(
            id = "3",
            name = "Fresh Foam X 1080v13",
            description = "Plush daily trainer.",
            brand = "New Balance",
            price = 164.99,
        ),
        ProductIndexModel(
            id = "4",
            name = "Classic Leather Sneaker",
            description = "Iconic lifestyle sneaker.",
            brand = "Reebok",
            price = 79.99,
        ),
        ProductIndexModel(
            id = "5",
            name = "Gel-Kayano 30 Stability Shoe",
            description = "Stability running shoe for overpronators.",
            brand = "Asics",
            price = 159.99,
        ),
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val elasticsearch: ElasticsearchContainer = ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:9.4.2",
        )
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
    }
}
