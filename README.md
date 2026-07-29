# AlertOps

Plateforme de gestion intelligente des alertes IT (Spring Boot 3.4, Java 21).

## Prérequis

- Java 21
- Docker Desktop
- Maven (ou `./mvnw`)

## Démarrage rapide

1. Copier les variables d'environnement :

```bash
cp .env.local.example .env.local
```

2. Démarrer l'infrastructure locale :

```bash
docker compose up -d
```

Services disponibles :

| Service    | URL / Port              |
|------------|-------------------------|
| PostgreSQL | `localhost:5432`        |
| Kafka      | `localhost:9092`        |
| Kafka UI   | http://localhost:8081   |
| Mailpit    | SMTP `1025`, UI `8025`  |
| Adminer    | http://localhost:8082   |

3. Lancer l'application :

```bash
./mvnw spring-boot:run
```

4. Vérifier la santé :

```bash
curl http://localhost:8080/actuator/health
```

5. (Profil `dev`) Obtenir un JWT de test :

```bash
curl -X POST http://localhost:8080/dev/login \
  -H "Content-Type: application/json" \
  -d '{"username":"operateur1","role":"OPERATEUR"}'
```

## Tests

```bash
./mvnw test
```

## Documentation API

Swagger UI : http://localhost:8080/swagger-ui.html
