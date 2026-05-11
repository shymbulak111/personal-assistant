# Personal Assistant Backend

A backend system built with Spring Boot 3 that helps users manage tasks, notes, reminders, and chat with an AI assistant.

## Tech Stack

- **Java 17 + Spring Boot 3.2**
- **PostgreSQL** — primary database
- **Redis** — response caching
- **Apache Kafka** — event streaming (audit logs, reminder notifications)
- **JWT** — stateless authentication
- **MapStruct** — DTO mapping
- **Flyway** — database migrations
- **Testcontainers** — integration tests
- **k6** — load testing

## Architecture

```
Controller  →  Service  →  Repository  →  Domain (JPA Entities)
                ↓
           Kafka / Redis
```

Clean layered architecture:
- `controller/` — REST endpoints, input validation
- `service/` — business logic
- `repository/` — Spring Data JPA
- `domain/model` — JPA entities
- `domain/enums` — enumerations
- `dto/` — request/response objects
- `mapper/` — MapStruct mappers
- `security/` — JWT filter, UserDetails
- `config/` — Spring configuration beans
- `exception/` — global error handling

## Running locally

**Prerequisites:** Docker, Docker Compose, JDK 17+

```bash
# start infrastructure
docker-compose up -d postgres redis kafka

# run the app
./mvnw spring-boot:run
```

Or run everything with Docker Compose:

```bash
docker-compose up --build
```

Swagger UI: http://localhost:8080/swagger-ui.html

## API Endpoints

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, get JWT |

### Tasks
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/tasks` | List tasks (paginated, filterable) |
| POST | `/api/v1/tasks` | Create task |
| GET | `/api/v1/tasks/{id}` | Get task by ID |
| PUT | `/api/v1/tasks/{id}` | Update task |
| PATCH | `/api/v1/tasks/{id}/complete` | Mark as done |
| DELETE | `/api/v1/tasks/{id}` | Delete task |

### Notes
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/notes` | List notes |
| POST | `/api/v1/notes` | Create note |
| GET | `/api/v1/notes/pinned` | Get pinned notes |
| PATCH | `/api/v1/notes/{id}/pin` | Toggle pin |
| PATCH | `/api/v1/notes/{id}/archive` | Toggle archive |

### Reminders
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/reminders` | List reminders |
| POST | `/api/v1/reminders` | Create reminder |
| PATCH | `/api/v1/reminders/{id}/cancel` | Cancel reminder |

### Chat (AI)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/chat` | Send message to AI assistant |
| GET | `/api/v1/chat/sessions` | List chat sessions |
| GET | `/api/v1/chat/sessions/{id}/messages` | Get messages |

### Statistics
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/stats` | Dashboard stats |

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PASSWORD` | postgres | DB password |
| `REDIS_HOST` | localhost | Redis host |
| `KAFKA_BROKERS` | localhost:9092 | Kafka broker |
| `JWT_SECRET` | (see yml) | JWT signing key |
| `AI_API_KEY` | (empty) | OpenAI API key (optional) |
| `AI_MODEL` | gpt-3.5-turbo | LLM model |

## Tests

```bash
# unit tests
mvn test -Dgroups="unit"

# integration tests (requires Docker)
mvn verify -Dgroups="integration"

# all tests
mvn verify
```

## Load Testing

```bash
# requires k6
k6 run load-tests/tasks.js -e BASE_URL=http://localhost:8080
```

## Kubernetes

```bash
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
```

## Security

- JWT tokens (access 24h, refresh 7 days)
- BCrypt password hashing
- RBAC: `ROLE_USER`, `ROLE_PREMIUM`, `ROLE_ADMIN`
- AI request rate limiting per user
- Full audit log for all mutating operations
