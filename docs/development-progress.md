# AlertOps — Suivi d'avancement du développement

---

## Phase 0 — Socle technique local

**Statut :** Terminée

### Objectifs réalisés
- [x] Correction du `pom.xml` (Flyway, springdoc-openapi, Caffeine, tests Spring Boot standard)
- [x] Spring Boot 3.4.3 / Java 21
- [x] `docker-compose.yml` (PostgreSQL 16, Kafka KRaft) + `docker-compose.override.yml` (Kafka UI, Mailpit, Adminer)
- [x] `.env.local.example`
- [x] `application.yml` (profil `dev`) + configuration de test H2
- [x] Migration Flyway baseline (`V1__baseline.sql`)
- [x] Stubs Java minimaux pour compilation
- [x] `README.md` de démarrage
- [x] CI GitHub Actions (`.github/workflows/ci.yml` — build + tests)
- [x] Compilation et tests Maven (`BUILD SUCCESS`)

### Fichiers créés
- `docker-compose.yml`, `docker-compose.override.yml`, `.env.local.example`
- `README.md`, `.github/workflows/ci.yml`
- `src/main/resources/application.yml`, `src/test/resources/application.yml`
- `src/main/resources/db/migration/V1__baseline.sql`
- `docs/development-progress.md`

### Fichiers modifiés
- `pom.xml`, `.gitignore`

### Décisions d'architecture importantes
- Flyway + `ddl-auto=validate` : le schéma est versionné en SQL, pas par Hibernate.
- Kafka KRaft en local ; services de confort (Mailpit, Kafka UI) dans l’override Compose uniquement.

### Points restant à faire (validation manuelle locale)
- Exécuter `docker compose up -d` puis `./mvnw spring-boot:run` et vérifier `/actuator/health` → `UP` (nécessite Docker + Postgres démarrés).

### Prochaine étape recommandée
Phase 1 — Common & Sécurité (squelette).

---

## Phase 1 — Common & Sécurité (squelette)

**Statut :** Terminée

### Objectifs réalisés
- [x] Value objects partagés : `Priority`, `Severity`, `ChannelType`, `UnitId`, `Confidence`, `AudioClip`
- [x] `DomainEvent` (identifiant + horodatage)
- [x] Exceptions métier : `BusinessException`, `TechnicalException`, `ResourceNotFoundException` + `ApiErrorResponse`
- [x] `GlobalExceptionHandler` (422 / 404 / 500 / 400 validation)
- [x] Configurations : `JacksonConfig`, `CorsConfig`, `CacheConfig` (Caffeine), `OpenApiConfig`, `AsyncConfig`
- [x] `SecurityConfig` : JWT stateless (OAuth2 Resource Server, HS256)
- [x] `JwtTokenService`, `JwtAuthenticationConverter`, mapping rôle → `ROLE_*`
- [x] Endpoint `/dev/login` (profil `dev` uniquement) + persistance `user_role_mappings`
- [x] Migration Flyway `V2__user_role_mappings.sql`
- [x] Endpoints de sonde RBAC `/api/v1/security-probe/*` pour tests d’intégration
- [x] Tests : `GlobalExceptionHandlerTest`, `SecurityIntegrationTest` (401 sans jeton, 403 rôle insuffisant)

### Fichiers créés
- `src/main/java/FST/MST_RSI/PFA/common/domain/vo/*`
- `src/main/java/FST/MST_RSI/PFA/common/exception/ApiErrorResponse.java`
- `src/main/java/FST/MST_RSI/PFA/security/config/JwtProperties.java`
- `src/main/java/FST/MST_RSI/PFA/security/infrastructure/JwtTokenService.java`
- `src/main/java/FST/MST_RSI/PFA/security/api/rest/DevLoginController.java`, `DevLoginRequest.java`, `DevLoginResponse.java`, `SecurityProbeController.java`
- `src/main/resources/db/migration/V2__user_role_mappings.sql`
- `src/test/java/FST/MST_RSI/PFA/common/exception/GlobalExceptionHandlerTest.java`
- `src/test/java/FST/MST_RSI/PFA/security/SecurityIntegrationTest.java`

### Fichiers modifiés
- `pom.xml` — `spring-boot-starter-oauth2-resource-server`
- `src/main/resources/application.yml` — `app.security.jwt.*`
- `src/test/resources/application.yml` — profil test, secret JWT, exclusion Kafka auto-config
- Implémentations complètes des classes `common/*` et `security/*` auparavant vides

### Décisions d'architecture importantes
1. **JWT auto-signé en dev** via `/dev/login` : remplaçable en production par l’IdP entreprise sans toucher au domaine (Phase 16).
2. **`SecurityProbeController`** : sonde technique pour valider RBAC ; les endpoints métier seront sécurisés phase par phase.
3. **Encodage JWT** : en-tête JWS explicite `HS256` pour compatibilité `NimbusJwtEncoder`.

### Dépendances créées
- Tous les modules futurs peuvent s’appuyer sur `common` (VO, exceptions, events) et `security` (JWT + rôles).

### Points restants à faire
- Test manuel : `POST /dev/login` avec profil `dev` actif, puis appels API avec `Authorization: Bearer …`.

### Prochaine étape recommandée
**Phase 2 — Module Alerting** : ingestion Dynatrace, validation payload, persistance, API lecture.

---

## Phase 2 — Module Alerting

**Statut :** Terminée

### Objectifs réalisés
- [x] Modèle domaine : `Alert`, `AlertTimelineEntry`, `NotificationState`, `AlertId`, `RawAlertPayload`, `ValidationResult`
- [x] `AlertPayloadValidator` + `DefaultAlertPayloadValidator` (JSON, taille max, champs obligatoires)
- [x] `DynatraceAlertNormalizer` (mapping champs Dynatrace + `ProblemDetailsJSONv2`)
- [x] `AlertRepositoryPort` + adapter JPA (`AlertEntity`, `AlertTimelineEntryEntity`)
- [x] Migration Flyway `V3__alerts.sql`
- [x] `IngestAlertUseCase` (création/mise à jour, publication `AlertReceivedEvent`)
- [x] `ListRecentAlertsUseCase` (72h), `ListAlertHistoryUseCase`, `GetAlertDetailUseCase`
- [x] `DynatraceWebhookController` — `POST /api/v1/ingestion/dynatrace`
- [x] `AlertController` — `GET /api/v1/alerts/recent|history|{id}` (RBAC opérateur+)
- [x] Authentification ingestion par `X-Ingestion-Token` (`IngestionTokenFilter`)
- [x] Fixture JSON + script `scripts/simulate_dynatrace.py`
- [x] Tests unitaires et d'intégration (validation, webhook, API lecture)

### Fichiers créés
- Module `alerting/*` (domain, application, infrastructure, api, config)
- `src/main/resources/db/migration/V3__alerts.sql`
- `src/test/resources/fixtures/dynatrace-problem.json`
- `scripts/simulate_dynatrace.py`
- Tests : `DefaultAlertPayloadValidatorTest`, `DynatraceWebhookIntegrationTest`, `AlertControllerIntegrationTest`

### Fichiers modifiés
- `src/main/resources/application.yml` — `app.ingestion.dynatrace.token`
- `src/test/resources/application.yml` — token de test
- `SecurityConfig.java` — filtre ingestion avant JWT

### Décisions d'architecture importantes
1. **Validation synchrone → HTTP 400** pour payload invalide (rejet explicite, journalisé en WARN).
2. **Payload valide → HTTP 201** avec `alertId`, `externalProblemId`, `created`.
3. **Idempotence** : même `externalProblemId` = mise à jour + entrée timeline `UPDATED`.
4. **État notification** initial : `EN_ATTENTE` (responsabilité plateforme ; résolution Dynatrace = `dynatraceState`).
5. **`AlertReceivedEvent`** publié dès l'ingestion (consommé en Phase 9 par le pipeline).

### Dépendances créées
- Phase 3 (Classification) pourra écouter `AlertReceivedEvent`.
- Aucune dépendance vers `classification`, `routingengine` ou `notification`.

### Points restants à faire
- Test manuel avec Docker + Postgres : `python scripts/simulate_dynatrace.py`
- Remplacer la fixture par le vrai JSON entreprise quand `info/payload_dynatrace.txt` sera complété.

### Prochaine étape recommandée
**Phase 3 — Module Classification (Gemini)** : `AlertClassifierPort`, adapter Gemini, `ClassifyAlertUseCase`.
