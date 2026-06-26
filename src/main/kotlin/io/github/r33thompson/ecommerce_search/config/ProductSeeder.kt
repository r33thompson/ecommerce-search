package io.github.r33thompson.ecommerce_search.config

import io.github.r33thompson.ecommerce_search.domain.ProductIndexModel
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class ProductSeeder(
    private val operations: ElasticsearchOperations,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        waitForElasticsearch()
        val indexOps = operations.indexOps(ProductIndexModel::class.java)
        if (!indexOps.exists()) {
            indexOps.createWithMapping()
            log.info("created Elasticsearch index 'products'")
        }
        val products = sampleProducts()
        operations.save(products)
        log.info("indexed {} sample products", products.size)
    }

    private fun waitForElasticsearch() {
        val deadline = System.currentTimeMillis() + WAIT_BUDGET_MS
        var attempt = 0
        while (System.currentTimeMillis() < deadline) {
            try {
                operations.indexOps(ProductIndexModel::class.java).exists()
                log.info("Elasticsearch reachable after {} attempt(s)", attempt + 1)
                return
            } catch (e: Exception) {
                attempt++
                log.warn("Elasticsearch not yet reachable (attempt {}): {}", attempt, e.message)
                Thread.sleep(RETRY_DELAY_MS)
            }
        }
        error("Elasticsearch unreachable after ${WAIT_BUDGET_MS / 1000}s — is the docker-compose container up?")
    }

    private fun sampleProducts(): List<ProductIndexModel> = listOf(
        ProductIndexModel(
            id = "1",
            name = "XT-6 Trail Running Shoe",
            description = "Trail running shoe with Contagrip outsole, EnergyCell midsole, and Quicklace closure — equal parts technical and streetwear.",
            brand = "Salomon",
            price = 199.99,
        ),
        ProductIndexModel(
            id = "2",
            name = "Ultraboost 22 Running Shoe",
            description = "Energy-returning Boost midsole paired with a Primeknit upper for long-distance comfort.",
            brand = "Adidas",
            price = 179.99,
        ),
        ProductIndexModel(
            id = "3",
            name = "Fresh Foam X 1080v13",
            description = "Plush daily trainer with Fresh Foam X cushioning and Hypoknit upper.",
            brand = "New Balance",
            price = 164.99,
        ),
        ProductIndexModel(
            id = "4",
            name = "Classic Leather Sneaker",
            description = "Iconic lifestyle sneaker with a soft leather upper — built for everyday wear.",
            brand = "Reebok",
            price = 79.99,
        ),
        ProductIndexModel(
            id = "5",
            name = "Gel-Kayano 30 Stability Shoe",
            description = "Stability running shoe with 4D Guidance System designed for overpronators.",
            brand = "Asics",
            price = 159.99,
        ),
    )

    private companion object {
        const val WAIT_BUDGET_MS = 60_000L
        const val RETRY_DELAY_MS = 2_000L
    }
}
