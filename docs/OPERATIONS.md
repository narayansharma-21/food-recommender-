# Backend Operations

## Health

- `/actuator/health/liveness` and `/actuator/health/readiness` are public for deployment probes.
- Other actuator routes are denied to normal users.
- Database failures make readiness fail and database-backed requests fail without partial commits.

## Background and OCR failures

- Background jobs retry up to three times by default with a 30-second delay.
- A final failure remains stored with its error and attempt count.
- The configured administrator can list failures at `GET /v1/admin/jobs/failed`.
- `POST /v1/admin/jobs/{jobId}/retry` resets a failed job and writes an immutable audit event.
- OCR failure does not replace or publish a partial menu extraction.

## Storage failures

- Menu uploads validate type and size before catalog changes are committed.
- If object storage or the database transaction fails, the upload fails and rollback cleanup removes the
  newly stored object when possible.
- Operators should investigate cleanup warnings because they can indicate an orphaned object.

## Recommendation failures

- The production path uses local weighted rules and does not depend on a live ML provider.
- Learned models cannot become active unless their recorded overall MAE beats the baseline.
- Promotion is explicit and audited; a previous model can be promoted again for rollback.
- Recommendation requests are limited to 30 per signed-in user per minute by default.
- The current limiter is process-local. Move it to shared storage before running multiple API instances.

## Authentication and authorization

- Invalid or missing tokens return `401 AUTHENTICATION_REQUIRED`.
- Signed-in users without the configured admin identity receive `403 AUTHORIZATION_DENIED` on admin routes.
- Admin access is an exact `provider:subject` allowlist supplied through `ADMIN_IDENTITIES`.

## Deferred launch controls

Database backup/restore automation, raw-image retention, malware scanning, and hosted encryption settings
depend on the selected deployment and storage providers. They must be completed and tested before a public
launch; they are not required to run the local proof of concept.
