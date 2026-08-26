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

To run the backend and PostgreSQL together:

```bash
cp .env.example .env
docker compose up --build
```

The Compose setup is for local development only. Production credentials must be supplied through the deployment environment.

## Greater Boston restaurant data

The proof of concept uses the free Overture Maps place snapshot. Download the Greater Boston extract with
the official Overture command-line client:

```bash
mkdir -p data
uvx --from overturemaps==1.0.2 overturemaps download \
  --bbox=-71.30,42.20,-70.90,42.55 \
  -f geojson \
  --type=place \
  -o data/greater-boston-places.geojson
```

Set `OVERTURE_IMPORT_ON_STARTUP=true` for one application start to import the snapshot. Imported source
records remain separate from canonical restaurants until matching is implemented.

The application must display the attribution required by the datasets included in Overture. See the
[Overture attribution guide](https://docs.overturemaps.org/attribution/) before distributing data.

See [the backend design plan](docs/BACKEND_DESIGN_PLAN.md) for scope, task IDs, and delivery phases.
