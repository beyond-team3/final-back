# Application Config Guide (For AI Agents)

This document summarizes the current Spring Boot configuration files under `src/main/resources` so any agent can parse the setup quickly and accurately.

## 1) Config Files and Purpose

- `application.yml`: common defaults shared across all profiles
- `application-local.yml`: local/dev runtime settings and sensitive placeholders
- `application-test.yml`: test-only DB behavior (H2 + schema reset)
- `application-prod.yml`: production settings via environment variables

## 2) Active Profile

- Default active profile: `local`
- Source: `application.yml`

## 3) Common Settings (`application.yml`)

```yaml
spring:
  application.name: seedflowplus
  profiles.active: local
  jpa.open-in-view: false
  jpa.hibernate.ddl-auto: update
  jpa.show-sql: false
  jpa.properties.hibernate.format_sql: true
server.port: 8080
logging.level.root: info
springdoc.api-docs.path: /v3/api-docs
springdoc.swagger-ui.path: /swagger-ui.html
springdoc.swagger-ui.operations-sorter: method
springdoc.swagger-ui.tags-sorter: alpha
springdoc.swagger-ui.display-request-duration: true
```

## 4) Profile Matrix

| Profile | File | Database | JPA DDL | Notes |
|---|---|---|---|---|
| `local` | `application-local.yml` | MariaDB (`jdbc:mariadb://100.81.161.92:3031/nexeed_db`) | inherits `update` from common | Redis/Mail/JWT values are configured with env fallback defaults |
| `test` | `application-test.yml` | H2 in-memory (`jdbc:h2:mem:testdb;MODE=MySQL`) | `create-drop` | isolated test schema lifecycle |
| `prod` | `application-prod.yml` | `${DB_URL}` (MariaDB driver) | inherits `update` from common unless overridden | all critical values from env vars |

## 5) Environment Variables

### Used in `local`

- `DB_USERNAME` (default: `nexeed_dev`)
- `DB_PASSWORD` (default: `nexeed12`)
- `REDIS_HOST` (default: `100.81.161.92`)
- `REDIS_PORT` (default: `3032`)
- `REDIS_PASSWORD` (default: `nexeed12`)
- `MAIL_APP_PASSWORD` (default is configured in file)
- `JWT_SECRET` (default: empty)
- `JWT_ACCESS_TOKEN_EXPIRATION_MS` (default: `3600000`)
- `JWT_REFRESH_TOKEN_EXPIRATION_MS` (default: `1209600000`)

### Required/used in `prod`

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT` (no default in file; empty if unset)
- `REDIS_PASSWORD` (optional)
- `MAIL_HOST` (default: `smtp.gmail.com`)
- `MAIL_PORT` (default: `587`)
- `MAIL_USERNAME` (default: `nexeed21@gmail.com`)
- `MAIL_APP_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION_MS` (default: `3600000`)
- `JWT_REFRESH_TOKEN_EXPIRATION_MS` (default: `1209600000`)

## 6) Production Monitoring Endpoint Config

From `application-prod.yml`:

- `management.endpoints.web.exposure.include: health,info,prometheus`
- `management.endpoint.health.probes.enabled: true`

This is intended for infra observability stacks (for example: Grafana + Prometheus scraping).

## 7) Important Repository Note

`.gitignore` contains `application-*.yml`, so profile files may not be tracked by default.

- Pattern location: `.gitignore` line containing `application-*.yml`
- Impact: `application-local.yml`, `application-test.yml`, `application-prod.yml` can exist locally but remain untracked.

## 8) Quick Run Expectations

- No explicit profile: app starts with `local`
- Test profile: use `test` to force H2 + `create-drop`
- Prod deployment: set profile `prod` and inject env vars via CI/CD (Jenkins/ArgoCD)
