# FastShop

A Java + Spring Boot REST API built as a performance optimization challenge. The constraints were deliberately strict:

- **Single node** — no load balancer, no horizontal scaling. One machine runs both the API and the database.
- **GET requests under 100ms** — the target for all read endpoints under sustained load.

The goal wasn't to build a production-ready system but to understand exactly where the limits are and what each optimization actually buys you.

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25  |
| Framework | Spring Boot 4.0.3 |
| Database | PostgreSQL 18 |
| Cache / Queue | Redis 7 |
| Connection Pool | HikariCP |
| Migrations | Flyway |
| Containerization | Docker Compose |
| Load Testing | Locust + Faker |

---

## API

### Products
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/products` | List products with optional fuzzy search |
| `GET` | `/api/products/{id}` | Get product by ID |
| `POST` | `/api/products` | Create product |
| `PUT` | `/api/products/{id}` | Update product (async) |
| `DELETE` | `/api/products/{id}` | Delete product (async) |

### Orders
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/orders` | Paginated order history |
| `GET` | `/api/orders/{id}` | Get order with items |
| `POST` | `/api/orders` | Place an order |
| `PATCH` | `/api/orders/{id}/cancel` | Cancel a pending order (async) |

---

## Optimizations

### 1. HikariCP Tuning
The default pool of 10 connections was the first bottleneck. Under concurrent load, requests queued waiting for a free connection, which inflated latency across every endpoint. Increased the pool size and disabled `open-in-view`, which was keeping connections open during response serialization — long after the actual queries had finished.

### 2. Redis Cache
Product and order lookups were hitting the database on every request. Added caching via Redis with TTL-based expiration and explicit eviction on writes (`@CachePut` on update, `@CacheEvict` on delete). Read endpoints dropped to sub-10ms response times after the cache warmed up.

### 3. PostgreSQL Trigram Search (`pg_trgm`)
The original search used exact `LIKE` matching which required a full table scan. Replaced it with `pg_trgm` — a PostgreSQL extension that enables partial and fuzzy matching using GIN indexes. Supports typo tolerance and partial terms without sacrificing index usage.

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_products_name_trgm ON products USING gin(name gin_trgm_ops);
```

### 4. Async Writes with Redis Streams
`PUT`, `DELETE`, and `PATCH` operations were blocking DB connections for 1800–1900ms under load. Moved them to Redis Streams — controllers return `202 Accepted` immediately and a background worker processes the actual DB write. This freed the connection pool to focus on the higher-volume `POST` and `GET` operations.

```
Before: PUT /products/{id} → p50 = 1900ms
After:  PUT /products/{id} → p50 = 2ms
```

### 5. N+1 Query Fix on Paginated Endpoints
Fetching paginated order lists with `JOIN FETCH` caused Hibernate to ignore `LIMIT/OFFSET` and load everything into memory before paginating in Java. Fixed with a two-query approach: fetch only the IDs for the current page first, then fetch full data for those IDs only.

```java
// Query 1 — paginate IDs at DB level (fast)
List<UUID> ids = orderRepository.findPagedIds(pageable);

// Query 2 — fetch full data for those IDs only
List<Order> orders = orderRepository.findWithItemsByIds(ids);
```

### 6. JVM Tuning for Containers
Switched from the default GC to ZGC with generational mode for lower pause times. Fixed heap and metaspace bounds to avoid memory pressure affecting PostgreSQL and Redis on the same machine.

```dockerfile
ENV JAVA_OPTS="\
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Xms256m \
  -Xmx1g \
  -XX:MaxMetaspaceSize=256m \
  -XX:+AlwaysPreTouch \
  -Djava.security.egd=file:/dev/./urandom"
```

---

## Load Test Results

Tests run with a stepped load shape — users ramped from 50 to the target number over time, holding each level for 60 seconds. Each virtual user seeds their own products and orders on startup (no shared state between users).

### 5,000 Concurrent Users — 0 failures

| Endpoint | p50 | p95 | p99 | RPS |
|---|---|---|---|---|
| `GET /api/orders` | 2ms | 53ms | 86ms | 728.9 |
| `GET /api/products/{id}` | 2ms | 53ms | 85ms | 637.0 |
| `GET /api/orders/{id}` | 2ms | 54ms | 86ms | 546.3 |
| `POST /api/orders` | 4ms | 68ms | 100ms | 377.2 |
| `POST /api/products` | 5ms | 87ms | 140ms | 93.2 |
| `GET /api/products?name=` | 13ms | 100ms | 140ms | 36.4 |
| **Aggregated** | **3ms** | **58ms** | **92ms** | **2,574** |

### 10,000 Concurrent Users — 0 failures

| Endpoint | p50 | p95 | p99 | RPS |
|---|---|---|---|---|
| `GET /api/orders` | 13ms | 480ms | 570ms | 865.0 |
| `GET /api/products/{id}` | 13ms | 480ms | 570ms | 757.9 |
| `GET /api/orders/{id}` | 14ms | 480ms | 570ms | 648.7 |
| `POST /api/orders` | 21ms | 490ms | 600ms | 454.2 |
| `GET /api/products?name=` | 23ms | 520ms | 720ms | 42.9 |
| `POST /api/products` | 30ms | 600ms | 5,900ms | 116.9 |
| **Aggregated** | **15ms** | **480ms** | **590ms** | **3,071** |

At 10k users the bottleneck is PostgreSQL connection exhaustion — expected for a single machine. The p99 spike on `POST /api/products` reflects occasional requests hitting the connection pool ceiling.

---

## Running Locally

```bash
# Clone and build
git clone https://github.com/your-username/fastshop.git
cd fastshop

# Start everything (app + postgres + redis)
docker compose up --build

# API available at
http://localhost:8080
```

Environment variables (with defaults):

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/mydatabase` | PostgreSQL URL |
| `DB_USERNAME` | `myuser` | DB username |
| `DB_PASSWORD` | `secret` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `HIKARI_MAX_POOL_SIZE` | `80` | Max DB connections |

---

## Running the Load Test

```bash
pip install locust faker
 locust --process 4 -f products_insert_search.py --host=http://localhost:8080
```

Open `http://localhost:8089` to start the test. The stepped load shape will automatically ramp from 50 to 5,000 users over ~8 minutes.

---

## What's Next

The natural next steps to push beyond a single machine:

- **PgBouncer** — connection pooler in front of PostgreSQL, dramatically reduces actual DB connections needed
- **Multiple app instances + Nginx** — horizontal scaling, linear throughput increase
- **PostgreSQL read replicas** — route ~70% read traffic to replicas, writes to primary
- **WebFlux + R2DBC** — fully reactive stack, better resource utilization under extreme concurrency
