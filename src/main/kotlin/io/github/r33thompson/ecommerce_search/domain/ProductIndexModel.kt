package io.github.r33thompson.ecommerce_search.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "products")
data class ProductIndexModel(
    @Id
    val id: String,

    @Field(type = FieldType.Text)
    val name: String,

    @Field(type = FieldType.Text)
    val description: String,

    @Field(type = FieldType.Text)
    val brand: String,

    @Field(type = FieldType.Double)
    val price: Double,
)
