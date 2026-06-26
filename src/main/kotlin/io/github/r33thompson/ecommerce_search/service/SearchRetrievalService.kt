package io.github.r33thompson.ecommerce_search.service

import co.elastic.clients.elasticsearch._types.query_dsl.Query
import io.github.r33thompson.ecommerce_search.domain.ProductIndexModel
import io.github.r33thompson.ecommerce_search.web.SearchHitDto
import io.github.r33thompson.ecommerce_search.web.SearchResponse
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service

@Service
class SearchRetrievalService(
    private val operations: ElasticsearchOperations,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(rawQuery: String): SearchResponse {
        val q = rawQuery.trim()
        log.info("search received: query='{}'", q)
        val started = System.currentTimeMillis()

        val esQuery = NativeQuery.builder()
            .withQuery(
                Query.of { qb ->
                    qb.multiMatch { mm ->
                        mm.query(q)
                            .fields("name^3", "brand^2", "description")
                            .fuzziness("AUTO")
                    }
                },
            )
            .build()

        val hits = operations.search(esQuery, ProductIndexModel::class.java)
        val tookMs = System.currentTimeMillis() - started
        log.info("search complete: query='{}', hits={}, tookMs={}", q, hits.totalHits, tookMs)

        return SearchResponse(
            query = q,
            count = hits.totalHits.toInt(),
            tookMs = tookMs,
            hits = hits.searchHits.map { hit ->
                val p = hit.content
                SearchHitDto(
                    id = p.id,
                    name = p.name,
                    brand = p.brand,
                    description = p.description,
                    price = p.price,
                    score = hit.score,
                )
            },
        )
    }
}
