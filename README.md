# Event Service

Advanced event management microservice for Camp Connect.

## Domain features

- MongoDB-backed event CRUD
- Draft, scheduled, postponed, ongoing, completed, and cancelled lifecycle
- Publication controls and guarded status transitions
- Capacity management and computed occupancy
- Confirmed registrations and ordered waitlists
- Automatic waitlist promotion after cancellation
- Event cancellation and postponement scenarios
- Search, filtering, upcoming events, and availability
- Synchronous notification creation through OpenFeign

## Run

```bash
docker compose up -d mongodb
mvn spring-boot:run
```

The service starts on `http://localhost:8081`.

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Health: `http://localhost:8081/actuator/health`

## Event payload

```json
{
  "title": "Forest Discovery Camp",
  "description": "A guided weekend camping experience in the forest.",
  "category": "ADVENTURE",
  "startAt": "2026-07-10T09:00:00",
  "endAt": "2026-07-12T18:00:00",
  "location": "Ain Draham",
  "organizerId": "guide-42",
  "capacity": 2,
  "waitlistCapacity": 3,
  "price": 120.0,
  "published": true
}
```

## Main endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/events` | Create an event |
| `GET` | `/events` | Filter by category, status, location, or published |
| `GET` | `/events/search?keyword=forest` | Search event text |
| `GET` | `/events/upcoming` | List upcoming published events |
| `GET` | `/events/available` | List events accepting registrations |
| `PUT` | `/events/{id}` | Update editable event |
| `PATCH` | `/events/{id}/publish` | Publish a draft |
| `POST` | `/events/{id}/registrations` | Register or join waitlist |
| `DELETE` | `/events/{id}/registrations/{participantId}` | Cancel registration |
| `PATCH` | `/events/{id}/postpone` | Move event dates |
| `PATCH` | `/events/{id}/cancel` | Cancel event with reason |
| `PATCH` | `/events/{id}/status?status=ONGOING` | Apply lifecycle transition |
| `GET` | `/events/{id}/availability` | Capacity and occupancy metrics |

## Configuration

| Environment variable | Default |
| --- | --- |
| `MONGODB_URI` | `mongodb://localhost:27017/event_db` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |
| `NOTIFICATION_SERVICE_URL` | Empty; resolve through Eureka |
| `CONFIG_SERVER_URL` | `http://localhost:8099` |

Validation and business failures use a consistent response:

```json
{
  "timestamp": "2026-06-06T14:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/events",
  "validationErrors": {
    "title": "must not be blank"
  }
}
```
