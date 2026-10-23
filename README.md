# Campus Library Resource Explorer API

A beginner-friendly Spring Boot REST API for browsing academic resources (books, tutorials, study guides,
reference material). One application demonstrates **two** practical experiments:

| Experiment | Topic |
|---|---|
| **2.2.1** | Paginated and sortable REST APIs for efficient, scalable data retrieval |
| **2.2.2** | Optimising backend reads: caching, query optimisation (N+1 vs `JOIN FETCH`), native SQL, benchmarking |

---

## 1. Aims and objectives

- Return large data sets one page at a time, sorted safely, with the paging done **inside the database**.
- Filter by category and keyword.
- Show the **N+1 query problem** and fix it with `JOIN FETCH`.
- Cache frequently read data with **Ehcache 3** and invalidate it correctly after changes.
- Use a **native SQL** query for a grouped statistic.
- Measure with **Apache JMeter**, without inventing results.

## 2. Technologies

Java 17+, Spring Boot 3.5.0 (Maven), Spring Web, Spring Data JPA + Hibernate, Bean Validation,
MySQL 8 (main database), Spring Cache + Ehcache 3 via JCache (JSR-107), SLF4J/Logback,
JUnit 5 + MockMvc + H2 (tests only), Apache JMeter, Postman collection.

## 3. Prerequisites (Windows)

| Tool | Notes |
|---|---|
| JDK 17 or newer | `java -version` must show 17+. |
| Maven 3.8+ | `mvn -version`. **Or** use the included wrapper `.\mvnw.cmd` (downloads Maven on first use). |
| MySQL 8 | Local install (MySQL Installer). Docker is optional (`docker-compose.yml`). |
| VS Code | Extensions: *Extension Pack for Java*, *Spring Boot Extension Pack* (optional). |
| Apache JMeter 5.6+ | Only for benchmarking. Needs Java. |
| Postman (optional) | Import `docs/postman_collection.json`. |

## 4. Project structure

```
campus-library-resource-explorer-api/
├── pom.xml
├── README.md
├── .gitignore  .env.example  docker-compose.yml (optional)
├── mvnw  mvnw.cmd  .mvn/wrapper/            Maven wrapper
├── database/schema.sql                      MySQL setup script
├── docs/postman_collection.json             all endpoints for Postman
├── performance-tests/campus-resource-api.jmx   JMeter plan
└── src
    ├── main
    │   ├── java/com/example/campusresources
    │   │   ├── CampusResourcesApplication.java
    │   │   ├── config/       CacheConfig, WebConfig (CORS), DataInitializer (seed data)
    │   │   ├── controller/   Resource, Category, Statistics, Demo, Diagnostics, Root
    │   │   ├── dto/          request/response records, PageResponse, ApiError
    │   │   ├── entity/       Resource, Category
    │   │   ├── exception/    custom exceptions + GlobalExceptionHandler
    │   │   ├── repository/   ResourceRepository (JPQL + native), CategoryRepository
    │   │   ├── service/      interfaces + ResourceMapper
    │   │   ├── service/impl/ ResourceServiceImpl, CategoryServiceImpl, StatisticsServiceImpl
    │   │   └── performance/  QueryComparisonService (N+1 demo), CacheDiagnostics
    │   └── resources/        application.properties, application-sqllog.properties, ehcache.xml
    └── test
        ├── java/com/example/campusresources   5 test classes (H2, no MySQL needed)
        └── resources/application-test.properties
```

Architecture: **Controller → Service → Repository → Database**. Controllers contain no business logic;
entities are never returned as JSON (DTOs only), so there is no infinite recursion or Hibernate-proxy leakage.

## 5. Database setup (Windows)

1. Install MySQL 8 and remember the `root` password. Check the service is running (`services.msc` → *MySQL80*).
2. Create the database (either way works):
   - Run the script: `mysql -u root -p < database\schema.sql`, **or**
   - do nothing: the default JDBC URL has `createDatabaseIfNotExist=true`, so the app creates it.
3. Tables and indexes are created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

**About `ddl-auto`:** `update` only adds missing tables/columns/indexes and never drops anything, which is safe
for development. `create` / `create-drop` **delete data** (used only for the in-memory test database).
For a real production system use `validate` or `none` and manage the schema with migrations (Flyway/Liquibase).

## 6. Configure credentials

Credentials come from environment variables, never from source code:

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/campus_library?...` | JDBC URL |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `SERVER_PORT` | `8080` | HTTP port |
| `DDL_AUTO` | `update` | Hibernate schema mode |

PowerShell (current window only):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

`.env.example` documents the same names. Never commit a real `.env` (it is in `.gitignore`).

## 7. Build and run

```powershell
cd campus-library-resource-explorer-api
mvn clean package -DskipTests      # build (or: .\mvnw.cmd clean package -DskipTests)
mvn spring-boot:run                # run   (or: .\mvnw.cmd spring-boot:run)
```

Open <http://localhost:8080/api/resources>. On the first start the app inserts **10 categories and 800 sample
resources** (ids 1–800, deterministic). On later starts it detects existing data and inserts nothing.

In VS Code you can also open `CampusResourcesApplication.java` and click **Run** above `main`.

Quick checks in PowerShell (use `curl.exe`, not the `curl` alias; always quote URLs containing `&`):

```powershell
curl.exe "http://localhost:8080/api/resources?page=0&size=5"
Invoke-RestMethod "http://localhost:8080/api/categories"
```

## 8. API reference

All errors share one JSON shape (see §8.5). Dates are ISO-8601. Ids are numbers.

### 8.1 Resources

| Method & path | Description | Success |
|---|---|---|
| `GET /api/resources` | Paginated, sortable, filterable list (**Experiment 2.2.1**) | 200 |
| `GET /api/resources/{id}` | One resource with its category (**cached**) | 200 |
| `POST /api/resources` | Create | 201 + `Location` |
| `PUT /api/resources/{id}` | Update (evicts cache entry) | 200 |
| `DELETE /api/resources/{id}` | Delete (evicts cache entry) | 204 |

**Query parameters of `GET /api/resources`**

| Parameter | Default | Rules |
|---|---|---|
| `page` | `0` | zero-based, ≥ 0 |
| `size` | `10` | 1 – 100 |
| `sortBy` | `createdAt` | one of `id, title, author, resourceType, rating, createdAt, updatedAt` |
| `direction` | `desc` | `asc` or `desc` (case-insensitive) |
| `categoryId` | – | must exist (otherwise 404) |
| `keyword` | – | case-insensitive match on title, description or author; max 100 chars |

Examples:

```
GET /api/resources
GET /api/resources?page=0&size=10
GET /api/resources?page=1&size=20
GET /api/resources?sortBy=title&direction=asc
GET /api/resources?sortBy=rating&direction=desc
GET /api/resources?categoryId=1&page=0&size=10
GET /api/resources?keyword=java&page=0&size=10
```

Response of `GET /api/resources?page=0&size=2` on the fresh sample data (first item = newest):

```json
{
  "content": [
    {
      "id": 800,
      "title": "Number Theory Cheat Sheet #800",
      "author": "L. Brown",
      "resourceType": "REFERENCE",
      "rating": 1.2,
      "categoryId": 10,
      "categoryName": "Mathematics",
      "createdAt": "2025-06-16T19:00:00"
    },
    {
      "id": 799,
      "title": "Incident Response Cheat Sheet #799",
      "author": "N. Joshi",
      "resourceType": "REFERENCE",
      "rating": 1.6,
      "categoryId": 9,
      "categoryName": "Cyber Security",
      "createdAt": "2025-06-16T14:00:00"
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 800,
  "totalPages": 400,
  "hasNext": true,
  "hasPrevious": false,
  "sortBy": "createdAt",
  "direction": "desc"
}
```

`GET /api/resources/1`:

```json
{
  "id": 1,
  "title": "Java Programming Handbook #1",
  "description": "A handbook covering java programming for undergraduate students.",
  "author": "A. Sharma",
  "resourceType": "BOOK",
  "rating": 1.0,
  "category": { "id": 1, "name": "Programming" },
  "createdAt": "2025-01-01T08:00:00",
  "updatedAt": "2025-01-01T08:00:00"
}
```

`POST /api/resources` / `PUT /api/resources/{id}` body:

```json
{
  "title": "Spring Boot in Action",
  "description": "Practical guide",
  "author": "A. Student",
  "categoryId": 1,
  "resourceType": "BOOK",
  "rating": 4.5
}
```

Validation: `title` required (≤ 200), `categoryId` required and must exist (404 otherwise), `resourceType` one of
`BOOK, TUTORIAL, STUDY_GUIDE, REFERENCE, ARTICLE`, `rating` 0–5 (optional, defaults to 0).

### 8.2 Categories

| Method & path | Description | Success |
|---|---|---|
| `GET /api/categories` | All categories sorted by name (**cached**) | 200 |
| `GET /api/categories/{id}` | One category (**cached**) | 200 |
| `POST /api/categories` | Create (`name` required, unique, case-insensitive) | 201 |
| `PUT /api/categories/{id}` | Update | 200 |
| `DELETE /api/categories/{id}` | Delete. **409** if it still contains resources | 204 |

```json
{ "id": 1, "name": "Programming", "description": "Programming languages and software development" }
```

### 8.3 Statistics (native SQL – Experiment 2.2.2 D)

`GET /api/statistics/resources-by-category` (sample data, ordered by count then name; averages are rounded to 2 decimals):

```json
[
  { "categoryId": 3,  "categoryName": "Algorithms",              "resourceCount": 80, "averageRating": 2.97 },
  { "categoryId": 8,  "categoryName": "Artificial Intelligence", "resourceCount": 80, "averageRating": 3.02 },
  { "categoryId": 6,  "categoryName": "Computer Networks",       "resourceCount": 80, "averageRating": 3.0 }
]
```
(…10 rows in total; categories with no resources are included with `resourceCount` 0.)

### 8.4 Demonstration and diagnostic endpoints (Experiment 2.2.2)

| Endpoint | Purpose |
|---|---|
| `GET /api/demo/n-plus-one?page=0&size=50` | **Inefficient** lazy loading; returns the number of SQL statements |
| `GET /api/demo/join-fetch?page=0&size=50` | **Optimised** `JOIN FETCH`; returns the number of SQL statements |
| `GET /api/demo/compare?page=0&size=50` | Runs both and returns them side by side |
| `GET /api/demo/resources/{id}/uncached` | Same data as `/api/resources/{id}` but bypasses the cache |
| `GET /api/diagnostics/cache` | Ehcache hits/misses/puts per cache + count of real DB loads |
| `DELETE /api/diagnostics/cache` | Clears all caches (cold start for benchmarks) |

`GET /api/demo/compare?size=50` has this shape. **The numbers below are illustrative**: run it yourself to get
your own values (the statement count for N+1 is 2 + the number of distinct categories on the page; timings vary):

```json
{
  "page": 0, "size": 50,
  "nPlusOne":  { "approach": "N+1 (lazy loading)", "resourcesReturned": 50, "sqlStatementCount": 12, "durationMs": "..." },
  "joinFetch": { "approach": "JOIN FETCH",         "resourcesReturned": 50, "sqlStatementCount": 2,  "durationMs": "..." },
  "conclusion": "N+1 executed 12 SQL statements; JOIN FETCH executed 2 for the same 50 resources."
}
```

These endpoints exist only for teaching and benchmarking. Use them while no other traffic is hitting the API
(the statement counter is global to the JVM).

### 8.5 Error format

```json
{
  "timestamp": "2026-10-22T10:15:30.123",
  "status": 400,
  "error": "Bad Request",
  "message": "size must be between 1 and 100",
  "path": "/api/resources",
  "details": []
}
```

| Status | When |
|---|---|
| 400 | invalid `page`/`size`/`sortBy`/`direction`, bad JSON, validation errors (`details` lists each field) |
| 404 | resource/category not found, unknown `categoryId` filter, unknown path |
| 405 / 415 | wrong HTTP method / content type |
| 409 | duplicate category name, deleting a category that still has resources |
| 500 | unexpected error – generic message only; details are logged on the server, never returned |

## 9. Pagination and sorting concepts (Experiment 2.2.1)

| Class | Meaning |
|---|---|
| `Pageable` | The *request*: page number, page size, sort. |
| `PageRequest` | The usual implementation of `Pageable`: `PageRequest.of(page, size, sort)`. |
| `Sort` | One or more `(property, direction)` pairs: `Sort.by(Sort.Direction.DESC, "rating")`. |
| `Page<T>` | The *result*: the rows of one page plus total elements, total pages, `hasNext()`, `hasPrevious()`. |

How it works here (`ResourceServiceImpl.getResources`):

1. Validate `page`, `size` (max 100), `direction`, and check `sortBy` against an **allow-list**, so clients cannot
   sort by arbitrary or non-existent fields.
2. Build `PageRequest.of(page, size, sort)`, where `sort` = requested field **plus `id` ascending** as a stable
   secondary sort. Without it, rows with equal ratings could appear in a different order each query and move
   between pages.
3. Spring Data adds `ORDER BY … LIMIT … OFFSET …` to the SQL, plus a `COUNT` query for `totalElements`.
   Paging is done by **the database**, not by loading every row and slicing in Java.
4. The `Page` is converted into `PageResponse` (a fixed JSON shape) containing small `ResourceSummaryResponse`
   items (no long description).

Note: `OFFSET` pagination gets slower for very deep pages on huge tables; keyset ("seek") pagination is the usual
next step (see §20).

## 10. The N+1 query problem (Experiment 2.2.2 A)

`Resource.category` is `LAZY`. If you load 50 resources with a plain `findAll(pageable)` and then read
`resource.getCategory().getName()` for each, Hibernate loads categories **on demand, with extra SELECTs**:

```
select ... from resources ... limit ?        -- 1 query for the resources
select count(*) from resources               -- page total
select ... from categories where id=?        -- + one query per distinct category
select ... from categories where id=?
...
```

"1 query for the list + N queries for the related rows" = **N+1**. (Hibernate's session cache loads each distinct
category only once, so the extra queries equal the number of *different* categories on the page – 10 in the sample
data, and it would be one per row if every row had a different category.) With big pages, many categories and a
remote database, the extra round-trips add up.

**See it:**

- `GET /api/demo/compare?size=50` – shows the statement counts (from Hibernate `Statistics`).
- Start the app with SQL logging and count the `select` lines yourself:

```powershell
$env:SPRING_PROFILES_ACTIVE = "sqllog"
mvn spring-boot:run
```

(The first start with this profile also prints the seed `insert` statements.)

## 11. `JOIN FETCH` (Experiment 2.2.2 B)

```java
@Query(value = "SELECT r FROM Resource r JOIN FETCH r.category",
       countQuery = "SELECT COUNT(r) FROM Resource r")
Page<Resource> findAllWithCategory(Pageable pageable);
```

`JOIN FETCH` makes Hibernate load each resource **and its category in one SQL `JOIN`**. Reading the category
afterwards needs no query. The same page now costs 2 statements (data + count) no matter how many categories appear.

Rules followed here:

- Only the **to-one** relation (`Resource → Category`) is fetched. Fetch-joining a **collection** (e.g.
  `Category.resources`) together with pagination makes Hibernate fetch everything and page in memory (warning
  `HHH000104`), which defeats the purpose.
- Every paginated fetch-join query has an explicit `countQuery` without the fetch join.
- The default listing endpoint `GET /api/resources` uses these `JOIN FETCH` queries. The N+1 version exists only
  in the demo service (`QueryComparisonService.runNPlusOne`).

## 12. Caching with Ehcache (Experiment 2.2.2 C)

Chain: `@Cacheable` annotations → Spring Cache abstraction → JCache (JSR-107) → Ehcache 3.
Settings: `application.properties` (`spring.cache.type=jcache`, `spring.cache.jcache.config=classpath:ehcache.xml`)
and `src/main/resources/ehcache.xml`. Caching is switched on by `@EnableCaching` in `CacheConfig`.

| Cache name | Key | Used by | TTL | Max entries |
|---|---|---|---|---|
| `resources` | resource id | `GET /api/resources/{id}` | 10 min | 1000 |
| `categories` | `'all'` | `GET /api/categories` | 30 min | 10 |
| `categoryById` | category id | `GET /api/categories/{id}` | 30 min | 200 |

Terms:

- **Cache miss** – value not in the cache: the method body runs, the database is queried, the result is stored.
  The app logs `CACHE MISS - loading resource 1 from the database`.
- **Cache hit** – value found: the method body is **skipped**; no SQL, no log line.
- **Expiration (TTL)** – an entry is dropped automatically after its time-to-live, so even unnoticed changes
  (e.g. direct SQL edits) become visible eventually. **Max entries** – when full, Ehcache evicts entries.
- **Invalidation** – removing entries when data changes:
  - update/delete resource → `@CacheEvict(cacheNames="resources", key="#id")`
  - create category → evict the `categories` list
  - update/delete category → evict `categoryById`, `categories` **and all of `resources`** (a resource response
    embeds the category name, so a rename would otherwise serve stale names)
- **Proxy rule** – caching works because the controller calls the service **through Spring's proxy**. A method
  calling another `@Cacheable` method of the *same* class bypasses the proxy and the cache; this project avoids
  that pattern. Only DTOs are cached (never lazy entities).

**Demonstrate it:**

```powershell
curl.exe -s http://localhost:8080/api/resources/1 > $null    # MISS  -> log line + SQL
curl.exe -s http://localhost:8080/api/resources/1 > $null    # HIT   -> no log line, no SQL
curl.exe http://localhost:8080/api/diagnostics/cache         # resourceByIdDatabaseLoads = 1, hits >= 1
curl.exe -X PUT http://localhost:8080/api/resources/1 -H "Content-Type: application/json" `
  -d '{\"title\":\"Updated\",\"categoryId\":1,\"resourceType\":\"BOOK\",\"rating\":4}'
curl.exe http://localhost:8080/api/resources/1               # MISS again, shows "Updated" (no stale data)
```

(Or use Postman – it avoids PowerShell quoting issues.)
`ehcacheStatistics` in the diagnostics response comes from JMX; if your JDK/environment blocks it, that list can
be empty – `resourceByIdDatabaseLoads` and the log lines still work.

## 13. Native SQL query (Experiment 2.2.2 D)

```java
@Query(value = "SELECT c.id, c.name, COUNT(r.id), ROUND(AVG(r.rating), 2) "
             + "FROM categories c LEFT JOIN resources r ON r.category_id = c.id "
             + "GROUP BY c.id, c.name ORDER BY COUNT(r.id) DESC, c.name ASC",
       nativeQuery = true)
List<Object[]> countResourcesGroupedByCategory();
```

| | JPQL | Native SQL |
|---|---|---|
| Written against | entities and fields (`Resource`, `r.category`) | real tables and columns (`resources`, `category_id`) |
| Portability | database independent | may use database-specific SQL |
| Translated by | Hibernate | sent to MySQL as written |
| Best for | normal entity queries | grouped reports, vendor functions, hand-tuned SQL |

A grouped report with aggregates (`COUNT`, `AVG`) returns summary rows, not entities, so SQL expresses it
directly. The service maps each row to `CategoryStatisticsResponse`. Trade-off: native SQL is not checked at
compile time and ties you to table/column names.

## 14. Automated tests

```powershell
mvn test            # or: .\mvnw.cmd test
```

Tests use the `test` profile with **in-memory H2** (`MODE=MySQL`), so no MySQL server is needed. Before each test
a small known data set is inserted (3 categories, 25 resources) and caches are cleared.

| Class | Covers |
|---|---|
| `ResourcePaginationAndSortingTest` | pages, sizes, sorting, stable ordering, invalid size/page/sort/direction, category & keyword filters |
| `ResourceCrudAndCacheTest` | get/create/update/delete, validation errors, missing resource, **cache hit, eviction after update/delete**, uncached endpoint |
| `CategoryAndStatisticsTest` | category CRUD, 409 conflicts, cache refresh after category changes, **native SQL statistics** |
| `QueryOptimizationTest` | `JOIN FETCH` executes fewer SQL statements than N+1 |

Reports appear in `target/surefire-reports`.

## 15. Performance benchmarking with JMeter

Plan: `performance-tests/campus-resource-api.jmx`. Thread groups run **one after another**.

| Group | Enabled | What it tests |
|---|---|---|
| 1 Paginated listing | yes | `page`/`size` combinations |
| 2 Sorted listing | yes | title / rating / createdAt sorting |
| 3 Individual resource | yes | random ids 1–500 |
| 4 Repeated cacheable requests | yes | `/api/resources/1` and `/api/categories` repeated |
| 5 Concurrent listing | yes | 50 threads, random pages |
| 6a / 6b | **no** | N+1 vs `JOIN FETCH` comparison |
| 7a / 7b | **no** | repeated read without cache (`/api/demo/.../uncached`) vs with cache |

Every request has assertions: status 200, an expected text in the body, and a maximum response time
(`-JmaxMs`, default 3000 ms). Listeners: Summary Report and Aggregate Report (average, median, 90/95/99 percentiles,
throughput, error %). The base URL is configurable: `-Jhost`, `-Jport`, `-Jprotocol`. Thread counts and loops:
`-Jthreads1`…`-Jthreads7`, `-Jloops1`…`-Jloops7` (see the test-plan comment).

**GUI mode** (for building/inspecting only – the GUI itself slows measurements): start JMeter, *File → Open*
the `.jmx`, make sure the app is running, press the green **Start** button, read the Summary/Aggregate Report.

**Non-GUI mode** (use this for real numbers):

```powershell
mkdir performance-tests\results
jmeter -n -t performance-tests\campus-resource-api.jmx `
       -Jhost=localhost -Jport=8080 `
       -l performance-tests\results\results.jtl `
       -e -o performance-tests\results\report
```

`-l` saves the raw results file; `-e -o` generates an HTML dashboard (the `report` folder must not already exist
or must be empty). Open `performance-tests\results\report\index.html`.

### Comparison procedure

Use the same machine, same data, no other heavy programs, and **run each case several times** (the first run
includes JVM and connection warm-up).

1. **N+1 vs `JOIN FETCH`** – in the GUI right-click groups **6a** and **6b** → *Enable* (disable 1–5), *File →
   Save As* `compare-nplus1.jmx`. Run the plan twice or run 6a and 6b separately (disable the other one), saving
   results to different files (`-l n1.jtl`, `-l joinfetch.jtl`). Compare average, 95th percentile and throughput.
   Also call `/api/demo/compare?size=100` once to see the SQL **statement counts**.
2. **Repeated reads before and after caching** – enable **7a** (uncached) and **7b** (cached) the same way.
   Before 7b call `DELETE /api/diagnostics/cache` so the cache starts cold. Compare results, and check
   `/api/diagnostics/cache`: `resourceByIdDatabaseLoads` should stay tiny compared with the number of requests.

**Important:** no benchmark results are included in this project on purpose. Actual numbers depend on database size,
hardware, MySQL buffer cache, cache state, concurrency and network. Caching does not make every endpoint faster:
it helps repeated reads of the same data, adds a little overhead on misses, and can serve slightly old data until
TTL/invalidation. A page of 10 rows on a local MySQL may already be so fast that the difference is small – increase
`size`, thread counts and data volume to see differences, and report what you measured.

## 16. Experiment mapping

| Feature | Experiment | Where |
|---|---|---|
| `Pageable`/`PageRequest`/`Page`/`Sort`, size limit, allow-listed sort fields, stable secondary sort | 2.2.1 | `ResourceServiceImpl.getResources`, `PageResponse` |
| Category and keyword filters | 2.2.1 | `ResourceRepository` search methods |
| Validation and clear error messages | 2.2.1 | `GlobalExceptionHandler`, `InvalidRequestException` |
| N+1 demonstration | 2.2.2 (A) | `QueryComparisonService.runNPlusOne`, `/api/demo/n-plus-one` |
| `JOIN FETCH` | 2.2.2 (B) | `ResourceRepository.*WithCategory`, `/api/demo/join-fetch` |
| Ehcache, `@Cacheable`, `@CacheEvict` | 2.2.2 (C) | `ehcache.xml`, `ResourceServiceImpl`, `CategoryServiceImpl`, `CacheDiagnostics` |
| Native SQL | 2.2.2 (D) | `ResourceRepository.countResourcesGroupedByCategory`, `/api/statistics/resources-by-category` |
| JMeter benchmarking | 2.2.2 (E) | `performance-tests/campus-resource-api.jmx` |
| Indexes on `category_id`, `created_at`, `rating`, `title` | 2.2.1 + 2.2.2 | `@Table(indexes=…)` in `Resource` |

## 17. Troubleshooting

| Problem | Fix |
|---|---|
| `'mvn' is not recognized` | Install Maven and add its `bin` folder to `PATH`, or use `.\mvnw.cmd`. Reopen the terminal. |
| `JAVA_HOME is not set` / wrong Java | Install JDK 17+, set `JAVA_HOME` to the JDK folder, check `java -version`. |
| `release version 17 not supported` | Maven is running on an older JDK. Point `JAVA_HOME` to JDK 17+. |
| `Communications link failure` / `Connection refused` | MySQL is not running (start *MySQL80* in `services.msc`) or port/URL is wrong. |
| `Access denied for user 'root'` | Wrong `DB_PASSWORD`. Set the environment variable again **in the same PowerShell window** that starts the app. |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in the JDBC URL (already present by default). |
| `Port 8080 was already in use` | `$env:SERVER_PORT = "8081"`, or find the process: `netstat -ano \| findstr :8080`, then `taskkill /PID <pid> /F`. |
| Environment variable not seen | `$env:NAME` only lives in that terminal window. Restart VS Code after changing system variables. |
| Seed data missing / want a fresh start | Data is only inserted into an **empty** database: `DROP DATABASE campus_library;` and restart. |
| `Unable to find a valid CacheManager` / `No CachingProvider` | Check that `pom.xml` still contains both `org.ehcache:ehcache` (classifier `jakarta`) and `javax.cache:cache-api`. Do not downgrade Spring Boot to 2.x. |
| `/api/diagnostics/cache` shows empty `ehcacheStatistics` | JMX statistics unavailable; use the log lines and `resourceByIdDatabaseLoads`. |
| PowerShell JSON quoting problems with `curl.exe -d` | Use Postman, or `Invoke-RestMethod -Method Put -ContentType "application/json" -Body (@{...} \| ConvertTo-Json)`. |
| JMeter: all requests fail | App not running, wrong `-Jhost/-Jport`, or `maxMs` too low. |
| JMeter report folder error | The `-o` folder must not exist yet (or be empty). |

## 18. Demonstration checklist for the lecturer

**Experiment 2.2.1**
1. Start the app; show the log "Inserted 10 categories and 800 sample resources".
2. `GET /api/resources` → show `content`, `totalElements=800`, `totalPages`, `hasNext`.
3. `page=1&size=20`, then `size=100`; then `size=500` → 400 with a message.
4. `sortBy=title&direction=asc`, `sortBy=rating&direction=desc`; then `sortBy=password` → 400.
5. `categoryId=1`, `keyword=java`, both together.
6. Start with the `sqllog` profile and show `limit ?` / `offset ?` in the SQL: paging happens in the database.

**Experiment 2.2.2**
1. `GET /api/demo/compare?size=50` → N+1 statement count vs `JOIN FETCH`; explain why.
2. With `sqllog`: show many `select … from categories where id=?` for N+1.
3. Cache: `GET /api/resources/1` twice → one `CACHE MISS` line; show `/api/diagnostics/cache`.
4. `PUT` the resource, `GET` again → fresh data, new `CACHE MISS` (invalidation).
5. `GET /api/statistics/resources-by-category` → native SQL result; show the query in `ResourceRepository`.
6. Run `mvn test` and show the report.
7. Run JMeter (non-GUI), open the HTML report; explain why you do not claim fixed speed-ups.

## 19. Likely viva questions

1. **Why paginate?** To limit memory, network and database work; large lists would be slow and could crash clients.
2. **Difference between `Pageable` and `Page`?** `Pageable` is the request (page, size, sort); `Page` is the response (content + total counts).
3. **Is paging done in Java or SQL?** SQL (`LIMIT/OFFSET`) – the database returns only the page.
4. **Why a secondary sort on `id`?** Equal sort values would otherwise have an undefined order, so rows may repeat or vanish across pages.
5. **Why allow-list `sortBy`?** Prevents invalid fields (errors) and sorting by unintended columns.
6. **What is N+1?** One query for the list plus one extra query per related entity loaded lazily.
7. **How does `JOIN FETCH` fix it?** It loads the association in the same SQL statement using a join.
8. **Why not `JOIN FETCH` a collection with pagination?** The join multiplies rows, so Hibernate paginates in memory; fetch collections separately (batch fetching) instead.
9. **What is `@Cacheable`?** Spring checks the cache before running the method; on a hit the method is skipped.
10. **Why `@CacheEvict`?** To remove stale data after create/update/delete.
11. **Why does self-invocation break caching?** Annotations are applied by a proxy; calling a method on `this` bypasses it.
12. **What do TTL and max entries do?** Expire old entries and bound memory use.
13. **JPQL vs native SQL?** JPQL uses entities and is portable; native SQL uses tables/columns and database features.
14. **Why DTOs?** Avoid JSON recursion/lazy-proxy problems and control exactly what the API exposes.
15. **What does `ddl-auto=update` do and why not in production?** It alters the schema automatically; production should use controlled migrations.
16. **How do you benchmark honestly?** Warm up, repeat, same conditions, non-GUI mode, report measured values and conditions.
17. **Does caching always help?** No – it helps repeated reads; it adds memory use and staleness risk.
18. **Why `open-in-view=false`?** Keeps lazy loading inside the service transaction so hidden queries in the web layer cannot occur.

## 20. Limitations and future improvements

- No authentication/authorisation (intentionally out of scope); the demo/diagnostic endpoints should be removed or protected in production.
- The cache is **per application instance** (heap only); several instances would need a distributed cache such as Redis.
- Cache eviction on write happens in the same request thread; a concurrent read between commit and eviction can briefly re-cache old data (rare; acceptable for this practical).
- Keyword search uses `LIKE '%…%'`, which cannot use a normal index; a MySQL `FULLTEXT` index or a search engine would scale better. `%` and `_` typed by users are treated as wildcards.
- `OFFSET` pagination slows on very deep pages; keyset pagination would fix that.
- The statement counter in the N+1 demo is JVM-global, so it is only meaningful when no other requests run.
- Possible additions: Flyway migrations, OpenAPI/Swagger UI, Micrometer metrics, Testcontainers MySQL tests.
