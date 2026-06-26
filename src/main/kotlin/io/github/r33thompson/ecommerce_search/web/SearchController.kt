package io.github.r33thompson.ecommerce_search.web

import io.github.r33thompson.ecommerce_search.service.SearchRetrievalService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchRetrievalService: SearchRetrievalService,
) {
    @GetMapping
    fun search(
        @RequestParam("q") q: String,
    ): SearchResponse = searchRetrievalService.search(q)
}

data class SearchHitDto(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val price: Double,
    val score: Float,
)

data class SearchResponse(
    val query: String,
    val count: Int,
    val tookMs: Long,
    val hits: List<SearchHitDto>,
)
