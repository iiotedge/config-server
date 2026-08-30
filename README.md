# config-server

Git-backed centralized config server (Spring Cloud Config) for the
IoTMining/IIoTEdge platform - serves **non-secret** cross-cutting
configuration (shared defaults, feature flags, timeouts) that benefits
from a real git audit trail and doesn't need a redeploy to change. Actual
secrets (DB passwords, JWT keys, API tokens) stay in environment variables
on every consuming service, exactly as they already do - this service was
never meant to serve those, and its git-backed content is designed with
`${VAR}` placeholders for exactly that reason.

## Why this pass happened - two severe, pre-existing gaps

1. **Zero authentication.** No `spring-boot-starter-security` dependency
   was even present - every config-serving endpoint
   (`/{application}/{profile}`, `/{name}-{profile}.yml`, ...) served
   whatever the backing git repo contained for any application/profile
   name a caller supplied, to anyone who could reach the port.
2. **A live-shaped GitHub PAT committed in plaintext** in
   `application-dev.yml`, in this repo's history since 2025-04-03. Removed
   from the file (see that file's own comment) - the token itself still
   needs revoking on GitHub's side, which removing it from this file
   doesn't undo.

Also fixed: a profile-default comment that disagreed with what the code
actually did (`prod` is the real default, not `dev` as the comment
claimed), Java 17/local-parent drift against the platform's Java 21/
`iiotedge-parent` standard, and a missing Eureka client dependency
(`eureka:` config existed in `application.yml` with nothing to actually
use it).

## Architecture

- **`config/SecurityConfig`** - Basic Auth required on every config-
  serving endpoint. Only `/actuator/health` and `/error` are open.
  Dedicated credential (`SPRING_SECURITY_CONFIG_USERNAME`/`PASSWORD`), not
  shared with eureka-server's or anything else's.
- **Git backend** - `spring.cloud.config.server.git.*`, real credentials
  via env vars in every profile (dev's own fallback is blank on purpose -
  no safe placeholder exists for a real token the way a shared dev DB
  password can have one).

## Deployment - three supported paths, same image

Same as eureka-server - `iiotedge-cli.sh` (bare VM), plain Docker/
`docker-compose.yml` (this repo, standalone, or
`data-ingestion-service/docker-compose.yml` for the whole stack), and
Kubernetes (`infrastructure/eureka/config-server-deployment.yaml` at the
platform root, with readiness/liveness probes and resource limits).

**Note on production status**: `iiotedge-cli.sh`'s current deployment
script explicitly disables config import for every consuming service
(`SPRING_CLOUD_CONFIG_ENABLED=false`) - this service is now correctly
securable and deployable, but actually switching the platform's ~20 other
services over to consuming it in production is a separate, larger
follow-up, not bundled into this pass.

## Local development

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Default port: `8888`. Needs a real `SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD`
(a GitHub PAT with read access to the backing config repo) to actually
clone anything - no dev fallback exists for that one, unlike the Basic
Auth credential (`admin`/`config-dev-only` by default).

## Build

```bash
./mvnw clean verify
```

## Docker

```bash
./mvnw clean package -DskipTests
docker build -t config-server .
docker run -p 8888:8888 \
  -e SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD=<real-token> \
  -e SPRING_SECURITY_CONFIG_PASSWORD=<real-value> \
  config-server
```

## License

MIT - see `LICENSE`.
