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

## Menu extraction proof of concept

New captured menu versions queue a background extraction job. The local provider uses Tesseract for
JPEG/PNG menu images and PDFBox for PDFs, falling back to Tesseract only for scanned PDF pages. Results
are schema-validated, retain field confidence and source provenance, preserve the original extraction,
and populate the queryable menu catalog.

Tesseract runs inside the provided application container. For direct host execution, install Tesseract
separately or set `TESSERACT_EXECUTABLE` to a trusted local binary. The Greater Boston parser accuracy
fixtures run as part of `./gradlew check` and require at least 90% section, item-name, and price accuracy.

## Firebase authentication

Versioned API routes require a valid Firebase ID token. Authentication is disabled by default until a
Firebase project is configured. To enable it, set:

```text
FIREBASE_AUTH_ENABLED=true
FIREBASE_PROJECT_ID=your-project-id
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

Keep the service-account file outside the repository. Mobile clients sign in through Firebase and send
the resulting ID token as `Authorization: Bearer <token>`. The backend verifies the token and maps the
Firebase user to a separate internal user ID.

Admin routes require an exact configured provider identity. For the sole Firebase administrator, set:

```text
ADMIN_IDENTITIES=firebase:your-firebase-uid
```

Do not use an email address unless it is the stable verified provider subject in the authentication token.

See the [official Firebase Admin setup guide](https://firebase.google.com/docs/admin/setup) for project
and credential setup.

## Ratings proof of concept

Authenticated users can manage their own 1–5 ratings through `/v1/ratings`. Each rating remains tied to
the exact menu item, keeps an immutable change history, and stores the original comment separately from
simple derived trait signals. Rating changes are recorded and refresh the user's taste profile transactionally.

## Taste-profile proof of concept

Authenticated users receive 12 Greater Boston onboarding questions from `/v1/onboarding/dishes`.
Their answers and later ratings create an explainable taste profile covering cuisines, ingredients,
preparations, and traits. Sparse preferences are kept near neutral until more evidence is collected.

## Recommendations proof of concept

Authenticated users can rank a restaurant's current menu with:

```text
POST /v1/restaurants/{restaurantId}/recommendations
{"mode":"SAFE_BET","limit":10}
```

`TRY_SOMETHING_NEW` is also supported. Dietary and allergy rules run before scoring. Responses include
confidence, short reasons, and the menu, ranking, and taste-feature versions used. Each response is saved
as an impression for later evaluation.

## ML improvement foundation

Recommendation impressions now keep the exact feature values used at ranking time. The Java backend can
export versioned JSONL training datasets with content hashes and code versions. `ml/baseline.py` evaluates
a free, dependency-free global-mean baseline across overall, new-user, new-dish, and sparse-profile cases:

```bash
python3 ml/baseline.py dataset.jsonl --output baseline-metrics.json
python3 -m unittest discover -s ml/tests -v
```

The model registry records datasets, artifacts, metrics, and promotion history. A candidate must beat its
stored baseline MAE before promotion, and a prior model can be promoted again for rollback. No learned
model is active yet; weighted rules remain the production recommender until enough first-party data exists.

See [operations](docs/OPERATIONS.md) for failure behavior, admin job recovery, and deferred launch controls.

See [the backend design plan](docs/BACKEND_DESIGN_PLAN.md) for scope, task IDs, and delivery phases.
