# Event Service

Spring Boot microservice for managing events in the Camp Connect project.

## Features

- Create, read, update, and delete events
- MongoDB persistence with Spring Data MongoDB
- Eureka service discovery
- OpenFeign communication with `notification-service`

## Requirements

- Java 17
- Maven
- Docker Desktop or MongoDB
- Eureka server running on port `8761`

## Run MongoDB

Using Docker Compose:

```bash
docker compose up -d mongodb
```

The default connection is `mongodb://localhost:27017/event_db`. To use
MongoDB Atlas or another server, set `MONGODB_URI`.

## Run the service

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8081`.

MongoDB generates string IDs for new events. Use the `id` returned by the
create endpoint for subsequent GET, PUT, and DELETE requests.

## Configuration

| Environment variable | Default |
| --- | --- |
| `MONGODB_URI` | `mongodb://localhost:27017/event_db` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |

