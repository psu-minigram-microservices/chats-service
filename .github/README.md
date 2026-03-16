<div align="center">
 <h1>Minigram Chats Service</h1>
 <div>
  Chats & Messages service for Minigram built with Spring Framework 7.
 </div>
</div>

## Deployment

### Prerequisites

- Docker & Docker Compose
- External Docker network `minigram`:
  ```bash
  docker network create minigram
  ```

### Configuration

Copy the environment template and fill in the required values:

```bash
cp .env.example .env
```

| Variable                | Description                       | Default          |
|-------------------------|-----------------------------------|------------------|
| `JWT_SECRET`            | Secret key for JWT authentication | *(required)*     |
| `CHATS_DB_PORT`         | PostgreSQL exposed port           | `5432`           |
| `CHATS_DB_NAME`         | Database name                     | `minigram_chats` |
| `CHATS_DB_USERNAME`     | Database user                     | `minigram`       |
| `CHATS_DB_PASSWORD`     | Database password                 | `minigram`       |
| `CHATS_SERVER_PORT`     | Application exposed port          | `8080`           |
| `CHATS_SPRING_PROFILES` | Active Spring profiles            | `pg`             |

### Run service

```bash
docker compose up --build
```

The service will be available at [localhost:8080](http://localhost:8080).

### Stop service

```bash
docker compose down

# to also remove the database volume:
docker compose down -v
```
