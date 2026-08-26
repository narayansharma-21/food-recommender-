# Food Recommender Backend Design Plan

## 1. Goal

Build a backend for a one-city launch that can:

- Find a restaurant.
- Import an official menu or accept a menu image upload.
- Extract and verify menu items.
- Let users rate dishes and leave short comments.
- Learn a basic taste profile.
- Rank a restaurant's menu for a user.
- Explain recommendations and show confidence.

## 2. Recommended Architecture

Start with a Java modular monolith, PostgreSQL, and separate Python ML jobs.

```text
Web or Mobile Client
        |
        v
Java 21 + Spring Boot API
  - Identity
  - Restaurant Catalog
  - Menu Ingestion
  - Ratings
  - Taste Profiles
  - Recommendations
  - Administration
        |
        v
PostgreSQL + PostGIS + pgvector
        ^
        |
Python training and evaluation jobs
```

Use background jobs for slow work such as menu fetching, OCR, extraction, and model training.

### Main tradeoff

Java and Python create two runtimes to maintain. This is acceptable because Java handles product logic well and Python keeps ML development simple.

## 3. Confirmed V1 Decisions

- Launch in one city.
- Use official restaurant menu pages and user-uploaded menu images.
- Use menu images only for text extraction, not food-image analysis.
- Do not ingest third-party reviews until licensing permits storage and ML use.
- Start recommendations with explainable weighted rules.
- Ask new users to rate 10–15 familiar dishes.
- Treat allergies as hard user-defined filters, never inferred facts.
- Keep restaurant locations, menu items, and general dish concepts separate.
- Keep old menu versions instead of overwriting them.

## 4. Backend Modules

Keep these as modules inside one Spring Boot application. Each module owns its business rules and exposes a small internal interface.

### 4.1 Platform Foundation

Status: Complete.

Owns configuration, database access, errors, security helpers, background jobs, and observability.

Tasks:

- `PLAT-01`: Create the Java 21 Spring Boot project and module structure.
- `PLAT-02`: Add PostgreSQL and Flyway database migrations.
- `PLAT-03`: Add local Docker configuration for the application and database.
- `PLAT-04`: Define consistent API errors and request validation.
- `PLAT-05`: Add structured logging, health checks, metrics, and request IDs.
- `PLAT-06`: Add a background-job table and worker process.
- `PLAT-07`: Add object storage for raw menu images, PDFs, and extraction artifacts.
- `PLAT-08`: Add CI checks for compilation, tests, migrations, and formatting.

Done when:

- The application starts locally.
- Database migrations run automatically.
- A sample background job can be queued and completed.
- Health and readiness endpoints work.

### 4.2 Identity and User Preferences

Owns users, authentication, privacy settings, allergies, and dietary restrictions.

Tasks:

- `USER-01`: Choose and integrate an authentication provider.
- `USER-02`: Create user and profile tables.
- `USER-03`: Add endpoints to read and update a profile.
- `USER-04`: Add user-declared allergies and dietary restrictions.
- `USER-05`: Add account deletion and data export workflows.
- `USER-06`: Record consent for location and recommendation data.

Done when:

- A user can sign in and manage preferences.
- Allergies are available to the recommendation filter.
- A user can request export or deletion.

### 4.3 Restaurant Catalog

Status: Complete for the Greater Boston proof of concept.

Owns restaurant locations and source identifiers.

Tasks:

- `REST-01`: Define the restaurant and restaurant-location schema.
- `REST-02`: Add a provider-neutral restaurant search interface.
- `REST-03`: Implement the first city data source.
- `REST-04`: Add restaurant matching using name, address, phone, and coordinates.
- `REST-05`: Add duplicate detection and an administrator merge workflow.
- `REST-06`: Store external provider IDs without making them internal primary keys.
- `REST-07`: Add restaurant correction and removal requests.

Done when:

- Users can search supported restaurants in the launch city.
- Duplicate locations can be detected and resolved.
- A restaurant can exist before its menu is available.

### 4.4 Menu Sources and Versioning

Status: Next milestone.

Owns menu sources, snapshots, versions, freshness, and publication state.

Tasks:

- `MENU-01`: Create menu, menu-source, menu-version, section, and menu-item tables.
- `MENU-02`: Support official HTML and PDF menu URLs.
- `MENU-03`: Support user-uploaded menu images.
- `MENU-04`: Store the raw source, capture time, source type, and content hash.
- `MENU-05`: Create a new menu version only when source content changes.
- `MENU-06`: Add menu states: processing, verified, likely-current, stale, disputed, and unavailable.
- `MENU-07`: Add scheduled refresh rules based on popularity and age.
- `MENU-08`: Add user reports for missing, changed, or unavailable items.
- `MENU-09`: Add an administrator menu review screen API.

Done when:

- A source can produce multiple historical menu versions.
- Only one verified version is active at a time.
- The API reports source and freshness information.

### 4.5 Menu Extraction Pipeline

Owns OCR, text parsing, structured extraction, and confidence.

Tasks:

- `EXT-01`: Define a provider-neutral OCR interface.
- `EXT-02`: Extract raw text from uploaded images and PDFs.
- `EXT-03`: Extract sections, dish names, descriptions, prices, and modifiers.
- `EXT-04`: Validate extracted data against a strict schema.
- `EXT-05`: Assign field-level confidence and provenance.
- `EXT-06`: Send low-confidence items to user or administrator verification.
- `EXT-07`: Keep the original extraction and corrected result.
- `EXT-08`: Build a small test set of real menus from the launch city.
- `EXT-09`: Measure item, price, and section extraction accuracy.

Done when:

- The pipeline can process an uploaded menu without blocking the request.
- Users can correct uncertain values.
- Extraction quality is measured against a fixed test set.

### 4.6 Dish Knowledge

Owns reusable dish concepts, ingredients, cuisines, and traits.

Tasks:

- `DISH-01`: Define dish, ingredient, cuisine, preparation, and trait tables.
- `DISH-02`: Link each menu item to zero or one general dish concept.
- `DISH-03`: Extract likely ingredients and traits from menu text.
- `DISH-04`: Mark each attribute as declared, inferred, or user-corrected.
- `DISH-05`: Generate and store dish embeddings with a version number.
- `DISH-06`: Add a review queue for uncertain dish matches.
- `DISH-07`: Prevent inferred attributes from being used as allergy guarantees.

Done when:

- Similar menu items can share a general dish concept.
- Every extracted trait has provenance and confidence.
- Embeddings can be regenerated without changing dish identity.

### 4.7 Ratings and Comments

Owns explicit user feedback and structured signals extracted from comments.

Tasks:

- `RATE-01`: Create rating, would-order-again, tag, and comment tables.
- `RATE-02`: Add create, update, delete, and history endpoints.
- `RATE-03`: Link feedback to both the menu item and general dish when available.
- `RATE-04`: Extract simple trait sentiment from comments.
- `RATE-05`: Store original comments separately from extracted signals.
- `RATE-06`: Add moderation and abuse-reporting hooks.
- `RATE-07`: Publish a feedback-changed event for profile updates.

Done when:

- Users can rate an actual menu item.
- Corrections do not erase audit history.
- Comment-derived signals show their source and confidence.

### 4.8 Onboarding and Taste Profiles

Owns cold-start questions and interpretable user preferences.

Tasks:

- `TASTE-01`: Create an onboarding set of common dishes for the launch city.
- `TASTE-02`: Add an endpoint that selects 10–15 useful onboarding questions.
- `TASTE-03`: Calculate cuisine, ingredient, preparation, and trait preferences.
- `TASTE-04`: Apply shrinkage so one rating does not create an extreme preference.
- `TASTE-05`: Recalculate affected profile features after feedback changes.
- `TASTE-06`: Store profile calculation and feature versions.
- `TASTE-07`: Add a user-facing taste-profile endpoint.

Done when:

- A new user receives an initial profile after onboarding.
- New ratings update the profile.
- Every displayed preference can be traced to supporting ratings.

### 4.9 Recommendation Engine

Owns filtering, scoring, ranking modes, confidence, and explanations.

Tasks:

- `REC-01`: Define a versioned recommendation request and response contract.
- `REC-02`: Filter unavailable dishes and hard dietary restrictions.
- `REC-03`: Build the first weighted scoring model.
- `REC-04`: Add global popularity and Bayesian cold-start priors.
- `REC-05`: Add Safe Bet and Try Something New modes.
- `REC-06`: Add high, medium, and low confidence categories.
- `REC-07`: Generate explanations from structured evidence.
- `REC-08`: Store recommendation impressions, scores, ranks, and versions.
- `REC-09`: Add deterministic tests using fixed users and menus.
- `REC-10`: Define fallback behavior when a menu or taste profile is incomplete.

Done when:

- A verified menu can be ranked for a user.
- Allergies and restrictions are applied before scoring.
- The same inputs and version produce the same ranking.
- Every result includes confidence and at least one evidence-based reason.

### 4.10 ML Training and Evaluation

Owns datasets, model training, evaluation, and versioned model artifacts.

Tasks:

- `ML-01`: Define the event and feature schema shared by Java and Python.
- `ML-02`: Build reproducible training-dataset generation.
- `ML-03`: Create a non-personalized baseline for comparison.
- `ML-04`: Train an initial boosted-tree rating model after enough data exists.
- `ML-05`: Evaluate overall, new-user, new-dish, and sparse-menu performance.
- `ML-06`: Calibrate confidence before showing numeric percentages.
- `ML-07`: Store model artifacts, metrics, data version, and code version.
- `ML-08`: Add a controlled model-promotion process.
- `ML-09`: Export a Java-compatible model or expose a stable inference contract.
- `ML-10`: Monitor prediction and rating drift.

Done when:

- A model can be reproduced from a known dataset version.
- It must beat the simple baseline before release.
- The backend can roll back to the previous model.

Do not block V1 on a trained ML model. The weighted scoring model is the initial production model.

### 4.11 Administration and Data Quality

Owns human review and operational correction workflows.

Tasks:

- `ADMIN-01`: Add secure administrator roles.
- `ADMIN-02`: Review and correct low-confidence menu extractions.
- `ADMIN-03`: Merge duplicate restaurants and dishes safely.
- `ADMIN-04`: Resolve user freshness reports.
- `ADMIN-05`: View ingestion failures and retry jobs.
- `ADMIN-06`: Keep an audit log of administrative changes.

Done when:

- Data issues can be fixed without direct database edits.
- Every administrative change is attributable and reversible where practical.

### 4.12 Security and Reliability

Tasks:

- `SEC-01`: Add authorization tests for user-owned data.
- `SEC-02`: Encrypt sensitive data in transit and at rest.
- `SEC-03`: Validate file type, size, and malware risk for uploads.
- `SEC-04`: Add rate limits for uploads, search, and recommendations.
- `SEC-05`: Back up PostgreSQL and test restoration.
- `SEC-06`: Define retention rules for raw images and location data.
- `SEC-07`: Add dependency and container security scans.
- `SEC-08`: Document failure behavior for unavailable OCR or model providers.

Done when:

- Users cannot access another user's private data.
- Uploads are restricted and safely processed.
- The database can be restored from backup.

## 5. Core Database Relationships

```text
User
  |-- UserRestriction
  |-- Rating -- MenuItem
  |-- TasteProfileFeature
  `-- RecommendationRequest -- RecommendationResult

Restaurant
  `-- RestaurantLocation
        `-- Menu
              |-- MenuSource
              `-- MenuVersion
                    `-- MenuSection
                          `-- MenuItem -- Dish

Dish
  |-- DishIngredient -- Ingredient
  |-- DishTrait -- Trait
  `-- DishEmbedding
```

Important rules:

- A `MenuItem` describes one item in one menu version.
- A `Dish` is a reusable food concept.
- A rating keeps its original `MenuItem` link even after the menu changes.
- External provider IDs are mappings, not primary keys.
- Extracted fields include source, confidence, and model version.

## 6. Initial API Groups

```text
/v1/users/me
/v1/users/me/restrictions
/v1/users/me/taste-profile
/v1/onboarding/dishes

/v1/restaurants/search
/v1/restaurants/{restaurantId}
/v1/restaurants/{restaurantId}/menus/current
/v1/restaurants/{restaurantId}/recommendations

/v1/menus/import
/v1/menus/upload
/v1/menus/{menuId}/status
/v1/menus/{menuId}/corrections

/v1/ratings
/v1/ratings/{ratingId}
```

Use REST for V1. It is simple and fits the product operations.

## 7. Delivery Phases

### Phase 0: Technical foundation

Tasks: `PLAT-01` through `PLAT-08`.

Result: deployable backend, database, jobs, and CI.

### Phase 1: Restaurant and menu catalog

Tasks: `REST-*`, `MENU-*`, `EXT-*`, and essential `ADMIN-*` tasks.

Result: users can find a restaurant and obtain a verified structured menu.

### Phase 2: Feedback and taste profile

Tasks: `USER-*`, `RATE-*`, `DISH-*`, and `TASTE-*`.

Result: users can onboard, rate dishes, and see a basic taste profile.

### Phase 3: Personalized ranking

Tasks: `REC-*` and recommendation event collection.

Result: users receive ranked menus with confidence and explanations.

### Phase 4: ML improvement

Tasks: `ML-*` after enough first-party ratings have been collected.

Result: a trained model can replace or complement weighted scoring when it proves better.

### Phase 5: Launch hardening

Tasks: remaining `ADMIN-*` and `SEC-*` work, load tests, backup tests, and launch-city data checks.

Result: the system is ready for a controlled city launch.

## 8. Task Dependency Guide

```text
PLAT
  |-- USER
  |-- REST -- MENU -- EXT -- DISH
  |                           |
  |-- RATE -------------------|
  |       |
  |       `-- TASTE -- REC -- ML
  |
  `-- ADMIN and SECURITY support every phase
```

Tasks with the same prefix can usually be assigned to one owner. After the schema and API contracts are agreed, modules can be implemented independently.

## 9. V1 Success Measures

- At least 95% of searched launch-city restaurants can be identified.
- At least 60% of attempted restaurants have an immediately rankable menu.
- Menu extraction correctly identifies at least 90% of dish names and prices after verification.
- A normal menu finishes processing within two minutes.
- Every recommendation records its model and feature versions.
- Every recommendation has a confidence category and evidence-based explanation.
- No allergy claim is based only on inferred menu data.

These are initial targets. Adjust them after testing with real launch-city menus.

## 10. Decisions Still Needed During Planning

- Launch city.
- Authentication provider.
- Cloud and object-storage provider.
- Restaurant search source.
- OCR provider.
- Text-enrichment model provider.
- Raw menu image retention period.
- Minimum amount of feedback required before training the first ML model.

These choices do not prevent work on the core database and module contracts.
