# Event Service

Spring Boot microservice for managing events in the Camp Connect project.

## Features

- Create, read, update, and delete events
- MySQL persistence with Spring Data JPA
- Eureka service discovery
- OpenFeign communication with `notification-service`

## Requirements

- Java 17
- Maven
- MySQL
- Eureka server running on port `8761`

## Run

Create a MySQL database named `event_db`, configure the datasource in
`src/main/resources/application.yml`, then run:

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8081`.

