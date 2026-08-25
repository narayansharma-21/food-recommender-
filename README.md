# Food Recommender Backend

Backend for a personalized, dish-first food recommender. The first release targets one city and ranks a restaurant's current menu for each user.

## Technology

- Java 21
- Spring Boot
- Gradle
- PostgreSQL
- Python for later model training and evaluation

## Modules

- `identity`: users, privacy, and dietary restrictions
- `catalog`: restaurants and reusable dish knowledge
- `menu`: menu sources, versions, ingestion, and extraction
- `feedback`: ratings and comments
- `taste`: onboarding and taste profiles
- `recommendation`: filtering, scoring, confidence, and explanations
- `administration`: data-quality workflows
- `platform`: shared infrastructure and operational concerns

The modules run in one application for V1. Module boundaries make it possible to split them later if there is a proven need.

## Local commands

```bash
./gradlew test
./gradlew bootRun
```

See [the backend design plan](docs/BACKEND_DESIGN_PLAN.md) for scope, task IDs, and delivery phases.
