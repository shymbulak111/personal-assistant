# Персональный Ассистент

Полнофункциональный бэкенд-сервис с веб-интерфейсом для управления задачами, заметками, напоминаниями и интеграцией с ИИ-ассистентом.

## Технологический стек

| Слой | Технология |
|------|-----------|
| Язык | Java 17 |
| Фреймворк | Spring Boot 3.2.3 |
| База данных | PostgreSQL 15 |
| Кэш | Redis 7 |
| Очередь сообщений | Apache Kafka |
| Безопасность | JWT + BCrypt + AES-256-GCM |
| Маппинг | MapStruct |
| Миграции БД | Flyway |
| Тесты | JUnit 5 + Mockito + Testcontainers |
| Нагрузочное тестирование | k6 |
| Контейнеры | Docker + Kubernetes |
| CI/CD | GitHub Actions |

## Архитектура

Приложение построено по принципам **Clean Architecture** с чётким разделением слоёв:

```
HTTP Request
     ↓
Controller Layer    — REST API, валидация входных данных
     ↓
Service Layer       — бизнес-логика, оркестрация
     ↓
Repository Layer    — Spring Data JPA, работа с БД
     ↓
Domain Layer        — JPA-сущности
     ↓
PostgreSQL
```

Инфраструктурные компоненты:
- **Redis** — кэширование и rate limiting AI-запросов
- **Kafka** — асинхронные audit-события
- **ИИ** — поддержка любого OpenAI-совместимого API (Groq, Ollama, LM Studio, OpenAI)

## Быстрый старт

**Требования:** Docker Desktop, JDK 17+, Maven 3.9+

```bash
# Клонировать репозиторий
git clone https://github.com/shymbulak111/personal-assistant.git
cd personal-assistant

# Запустить инфраструктуру (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Запустить приложение
mvn spring-boot:run
```

Открыть в браузере: **http://localhost:8080**

Swagger UI: http://localhost:8080/swagger-ui.html

## Функционал

### Управление задачами
- Создание, редактирование, удаление задач
- Статусы: `TODO`, `IN_PROGRESS`, `DONE`, `CANCELLED`
- Приоритеты: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- Фильтрация по статусу, приоритету, категории

### Управление заметками
- Создание и редактирование заметок
- Закрепление и архивирование
- Шифрование содержимого (AES-256-GCM)

### Напоминания
- Создание напоминаний с датой/временем
- Повторяющиеся напоминания
- Отмена и удаление

### ИИ-ассистент
- Чат с AI через OpenAI-совместимые API
- Function Calling: создание задач/заметок/напоминаний через натуральный язык
- Контекст из реальных данных пользователя
- История чат-сессий

## API

### Аутентификация
```
POST /api/v1/auth/register   — регистрация
POST /api/v1/auth/login      — вход (возвращает JWT)
```

### Задачи
```
GET    /api/v1/tasks              — список задач
POST   /api/v1/tasks              — создать задачу
GET    /api/v1/tasks/{id}         — получить задачу
PUT    /api/v1/tasks/{id}         — обновить задачу
PATCH  /api/v1/tasks/{id}/complete — завершить задачу
DELETE /api/v1/tasks/{id}         — удалить задачу
```

### Заметки
```
GET    /api/v1/notes              — список заметок
POST   /api/v1/notes              — создать заметку
GET    /api/v1/notes/pinned       — закреплённые заметки
PUT    /api/v1/notes/{id}         — обновить заметку
PATCH  /api/v1/notes/{id}/pin     — закрепить/открепить
PATCH  /api/v1/notes/{id}/archive — архивировать
DELETE /api/v1/notes/{id}         — удалить заметку
```

### Напоминания
```
GET    /api/v1/reminders          — список напоминаний
POST   /api/v1/reminders          — создать напоминание
PATCH  /api/v1/reminders/{id}/cancel — отменить
DELETE /api/v1/reminders/{id}        — удалить
```

### ИИ чат
```
POST /api/v1/chat                          — отправить сообщение
GET  /api/v1/chat/sessions                 — список сессий
GET  /api/v1/chat/sessions/{id}/messages   — история сообщений
```

### Прочее
```
GET    /api/v1/stats              — статистика пользователя
GET    /api/v1/users/me/export    — экспорт данных (GDPR)
DELETE /api/v1/users/me           — удаление аккаунта (GDPR)
GET    /actuator/health           — статус системы
```

## Безопасность

- **JWT** — access token (24ч) + refresh token (7 дней), алгоритм HS256
- **BCrypt** — хэширование паролей (cost factor 10)
- **RBAC** — роли `ROLE_USER`, `ROLE_PREMIUM`, `ROLE_ADMIN`
- **ABAC** — проверка владельца ресурса через `@PreAuthorize`
- **AES-256-GCM** — прозрачное шифрование поля `Note.content`
- **Audit Log** — асинхронная запись всех действий в БД и Kafka
- **GDPR** — экспорт и каскадное удаление данных пользователя

## Тестирование

```bash
# Unit-тесты (без Docker)
mvn test -Dgroups="unit"

# Интеграционные тесты (требуется Docker для Testcontainers)
mvn verify -Dgroups="integration"

# Все тесты
mvn verify
```

Покрытие:
- **Unit**: `TaskServiceTest`, `NoteServiceTest`, `UserServiceTest`, `AuthControllerTest`
- **Integration**: `TaskIntegrationTest`, `SecurityIntegrationTest`, `RedisIntegrationTest`, `KafkaIntegrationTest`
- **Context**: `PersonalAssistantApplicationTests`

## Нагрузочное тестирование

```bash
# Требуется k6 (https://k6.io)
k6 run load-tests/tasks.js -e BASE_URL=http://localhost:8080
```

Конфигурация: 0 → 10 → 50 → 100 VU, пороги: p95 < 500ms, error rate < 1%.

## Kubernetes

```bash
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
```

2 реплики с liveness/readiness пробами.

## Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|-------------|---------|
| `DB_HOST` | localhost | Хост PostgreSQL |
| `DB_PASSWORD` | postgres | Пароль БД |
| `REDIS_HOST` | localhost | Хост Redis |
| `KAFKA_BROKERS` | localhost:9092 | Kafka брокер |
| `JWT_SECRET` | (встроенный) | Секрет для подписи JWT |
| `AI_API_KEY` | — | API ключ для LLM провайдера |
| `AI_BASE_URL` | Groq API | URL LLM провайдера |
| `AI_MODEL` | openai/gpt-oss-120b | Модель |
| `ENCRYPTION_KEY` | (встроенный) | Ключ шифрования заметок |
