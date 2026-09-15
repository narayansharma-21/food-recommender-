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

See [the backend design plan](docs/BACKEND_DESIGN_PLAN.md) for scope, task IDs, and delivery phases.
