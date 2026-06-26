# ecommerce-search

A minimal Spring Boot + Kotlin project that demonstrates the basics of an e-commerce **search retrieval and ranking** pipeline backed by Elasticsearch. Built as a 2–3 hour learning exercise — small surface area, but enough moving parts to show how field boosting and fuzzy matching shape ranked results.

## What it does

On startup the service seeds an Elasticsearch index (`products`) with five sample retail items. A single REST endpoint runs a fuzzy, field-boosted `multi_match` query across the indexed documents and returns hits ordered by relevance score.

## Endpoints

| Method | Path          | Query params     | Description                                                                                                        |
|--------|---------------|------------------|--------------------------------------------------------------------------------------------------------------------|
| GET    | `/api/search` | `q` *(required)* | Fuzzy multi-field search across `name`, `brand`, `description`. Returns hits ranked by Elasticsearch's BM25 score. |

**Example**

```bash
curl 'http://localhost:9200/_cluster/health?pretty'   # sanity-check ES
curl 'http://localhost:8080/api/search?q=runing'      # note the typo — fuzziness handles it
```

**Response shape**

```json
{
  "query": "running",
  "count": 3,
  "tookMs": 27,
  "hits": [
    {
      "id": "1",
      "name": "XT-6 Trail Running Shoe",
      "brand": "Salomon",
      "description": "Trail running shoe with Contagrip outsole ...",
      "price": 199.99,
      "score": 1.42
    }
  ]
}
```

## How to run

You need JDK 21 installed and Docker running (Gradle handles the Kotlin and Spring Boot dependencies).

### Start

```bash
# 1. Start Elasticsearch (single-node, no auth, port 9200)
docker compose up -d

# 2. (Optional) Wait for the cluster to be green/yellow to verify ES is healthy
curl 'http://localhost:9200/_cluster/health?pretty'

# 3. Run the Spring Boot app (the seeder will retry until ES is reachable)
./gradlew bootRun
```

### Use

In another terminal:

```bash
# Hit the search endpoint
curl 'http://localhost:8080/api/search?q=running'
curl 'http://localhost:8080/api/search?q=salomon'
curl 'http://localhost:8080/api/search?q=stability'

# Hit it again with a deliberate typo to demonstrate fuzzy matching
curl 'http://localhost:8080/api/search?q=runing'
```

### Tests

The suite has two layers, with different speed/dependency trade-offs:

- **`SearchControllerTest`** — a sliced `@WebMvcTest` that mocks the service. Verifies HTTP wiring and JSON serialisation only. No infrastructure required; runs in well under a second.
- **`SearchIntegrationTest`** — a full `@SpringBootTest` backed by [Testcontainers](https://testcontainers.com/), which spins up a real Elasticsearch 9.4.2 container in-process, indexes a curated 5-product fixture, and asserts that fuzzy matching and field-boost ranking actually behave end-to-end. **Requires the Docker daemon to be running**

```bash
# Run everything (both layers)
./gradlew test

# Run only the fast mocked test (no Docker needed)
./gradlew test --tests SearchControllerTest

# Run only the Testcontainers integration test (Docker daemon must be running)
./gradlew test --tests SearchIntegrationTest
```

The integration test is intentionally isolated from the dev-loop Elasticsearch container started by `docker compose up -d` — Testcontainers spins up its own ephemeral instance on a random port, wired into Spring's context via `@ServiceConnection`. You can have both running simultaneously without conflict.

### Stop

In the terminal running `./gradlew bootRun`, press `Ctrl+C` to stop the app. Then tear down Elasticsearch:

```bash
# Stop the ES Docker container and wipe its data
docker compose down -v
```

## Search Retrieval & Ranking Concepts Used

This project leans on two core ideas from Elasticsearch's full-text retrieval model.

### Fuzziness (`fuzziness: AUTO`)

The `multi_match` query is configured with `fuzziness=AUTO`, which lets Elasticsearch tolerate typos and small spelling variants in the user's query. Under the hood it uses **Levenshtein edit distance**: terms within 1–2 edits (insert/delete/substitute/transpose) of an indexed token are still considered matches. `AUTO` scales the allowed edits with term length — short words must match exactly, longer words allow more slack. This is what lets `"runing"` still match `"running"`.

### Field boosting (ranking signal weighting)

Not all fields are equally meaningful when ranking a product. A query token appearing in a product's **name** is a much stronger relevance signal than the same token appearing inside a long marketing **description**. We encode that domain knowledge directly into the query via per-field boosts:

```kotlin
mm.fields("name^3", "brand^2", "description")
```

- `name^3` — matches in the product name contribute 3× to the BM25 score
- `brand^2` — brand matches count 2×
- `description` — baseline weight of 1

The result is that a user searching for `"salomon"` ranks documents where Salomon is the brand or part of the product name above ones where Salomon is only mentioned in body copy — even when raw term frequency would suggest otherwise. This is the simplest form of **learned-prior ranking**: hard-coded weights expressing what we believe matters. A real production system would replace these constants with a learning-to-rank model trained on click/conversion data, but the mechanism is the same — multiply per-field signals into a single combined score.

## Project layout

```
.
├── build.gradle.kts                       # Gradle build (Spring Boot 4.1, Kotlin, Testcontainers BOM)
├── docker-compose.yml                     # Single-node Elasticsearch for the dev loop
├── .github/workflows/ci.yml               # Runs ./gradlew test on push & PR
├── src/main/kotlin/io/github/r33thompson/ecommerce_search/
│   ├── EcommerceSearchApplication.kt      # Spring Boot entry point
│   ├── domain/ProductIndexModel.kt        # @Document index mapping
│   ├── service/SearchRetrievalService.kt  # multi_match query construction
│   ├── web/SearchController.kt            # REST endpoint + response DTOs
│   └── config/ProductSeeder.kt            # Boot-time index creation + sample data, with retry
├── src/main/resources/
│   └── application.properties             # spring.elasticsearch.uris=http://localhost:9200
└── src/test/kotlin/io/github/r33thompson/ecommerce_search/
    ├── web/SearchControllerTest.kt        # @WebMvcTest — controller wiring only (fast, mocked)
    └── SearchIntegrationTest.kt           # @SpringBootTest — real ES via Testcontainers
```
