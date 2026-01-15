# Document Processing System

## 📋 Project Overview

The Document Processing System is a backend application for uploading, processing, and managing documents at scale. It accepts user uploads, enqueues processing jobs (text extraction, thumbnail generation, metadata extraction, OCR), and provides results via REST APIs and real-time WebSocket notifications. The system uses asynchronous workers for processing, sending email notifications when jobs complete or fail.

## ✨ Features

- User authentication with JWT
- Asynchronous document processing via RabbitMQ
- Real-time WebSocket notifications (STOMP over SockJS)
- Email notifications for important events
- Text extraction from PDFs (PDFBox)
- Thumbnail generation (Thumbnailator)
- Full-text search capability (pluggable)
- Admin dashboard endpoints
- RESTful API

## 🛠️ Tech Stack

- Backend: Spring Boot 3.x, Java 17
- Database: PostgreSQL (production), H2 (tests)
- Message Queue: RabbitMQ
- Cache: Redis
- Security: Spring Security + JWT
- File Processing: Apache PDFBox, Thumbnailator
- Testing: JUnit 5, Mockito, Testcontainers
- API Documentation: Swagger / OpenAPI (springdoc)

## 🏗️ Architecture

This project is structured as an API server plus asynchronous worker nodes:

- API Server (Spring Boot)
  - Handles authentication, file uploads, management APIs
  - Persists metadata to PostgreSQL
  - Publishes jobs to RabbitMQ
  - Sends real-time notifications via WebSocket

- Worker Nodes (Spring Boot components or separate services)
  - Consume jobs from RabbitMQ
  - Process documents (text extraction, thumbnail, OCR, metadata)
  - Save results to database / storage
  - Notify API server and users via WebSocket and email

This separation lets you scale API servers and workers independently.

## 📐 System Design (Asynchronous Processing Flow)

1. User uploads a document to the API Server.
2. API Server stores the file on disk (or object storage) and creates a `Document` record.
3. API Server creates one or more `Job` records (TEXT_EXTRACTION, THUMBNAIL, OCR, METADATA) with status QUEUED and publishes job messages to RabbitMQ.
4. Worker(s) listen on the processing queue, pick up a job, update job status to PROCESSING, and process the document.
5. Processing results are saved in `ProcessingResult` records (text, thumbnail path, metadata JSON).
6. Worker updates job status to COMPLETED or FAILED and notifies the API Server.
7. API Server broadcasts WebSocket notifications and sends email alerts when configured.

This design allows retries, dead-letter handling, and horizontal scaling of workers.

## 🚀 Getting Started

### Prerequisites

- JDK 17 or higher
- Docker & Docker Compose
- Maven (optional if using wrapper)
- IntelliJ IDEA (recommended)

### Installation Steps

1. Clone the repository

```bash
git clone https://github.com/your-org/document-processing-system.git
cd document-processing-system
```

2. Start Docker containers (Postgres, RabbitMQ, Redis)

Windows (PowerShell / CMD):

```powershell
# Using the docker-compose.yml at project root
docker-compose up -d
```

3. Configure `src/main/resources/application.properties` (or use environment variables). See the Configuration section below for required settings.

4. Run the application

Windows (using Maven wrapper):

```powershell
# Build and run
./mvnw.cmd spring-boot:run
# or build the jar and run
./mvnw.cmd package
java -jar target/document-processing-system-0.0.1-SNAPSHOT.jar
```

5. Access Swagger UI

- Open: http://localhost:8080/swagger-ui.html or http://localhost:8080/swagger-ui/index.html

## 🔧 Configuration

Important `application.properties` settings (examples):

```properties
# Server
server.port=8080

# Datasource (Postgres)
spring.datasource.url=jdbc:postgresql://localhost:5432/docprocessor
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=change_this_secret_to_a_strong_value
jwt.expiration=86400000 # milliseconds (24h)

# Storage
storage.location=uploads

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
rabbitmq.queue.name=document.processing.queue

# Redis (optional)
spring.redis.host=localhost
spring.redis.port=6379

# Email
app.email.enabled=false
app.email.from=no-reply@example.com
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=smtp-user
spring.mail.password=smtp-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# WebSocket
spring.web.socket.endpoint=/ws

# Logging
logging.level.root=INFO
logging.level.com.docprocessor=DEBUG
```

Tip: prefer environment variables in production over committing secrets.

## 📡 API Endpoints

Below are the main endpoints (prefix: `/api`):

- Auth
  - POST /api/auth/register — Register a user (UserRegistrationDTO)
  - POST /api/auth/login — Login and get JWT (LoginRequestDTO)

- Documents
  - POST /api/documents/upload — Upload a new document (multipart/form-data)
  - GET /api/documents — List user documents
  - GET /api/documents/{id} — Get document metadata
  - DELETE /api/documents/{id} — Delete document

- Jobs
  - GET /api/jobs/{id} — Get job status
  - GET /api/jobs/document/{documentId} — Get jobs for a document
  - POST /api/jobs/{id}/retry — Retry failed job

- Admin (examples)
  - GET /api/admin/users — List users (ADMIN only)
  - GET /api/admin/jobs — List all jobs (ADMIN only)

Authentication: add header `Authorization: Bearer <token>` for protected endpoints.

## 🧪 Running Tests

Run tests with the Maven wrapper (recommended):

Windows:

```powershell
./mvnw.cmd test
```

To run with code coverage using JaCoCo (if configured):

```powershell
./mvnw.cmd test jacoco:report
```

Note: The project includes H2 as a test dependency so JPA repository tests run in-memory.

## 📊 Database Schema (High-level)

Main entities and relationships:

- User
  - id (PK), username, email, password, role, createdAt, updatedAt
  - One-to-many -> Document

- Document
  - id (PK), user (FK), originalFilename, storedFilename (UUID), fileSize, mimeType, storagePath, uploadDate, status, createdAt, updatedAt
  - Many-to-one -> User
  - One-to-many -> Job

- Job
  - id (PK), document (FK), jobType, status, retryCount, maxRetries, errorMessage, createdAt, startedAt, completedAt, processedBy
  - Many-to-one -> Document
  - One-to-many -> ProcessingResult

- ProcessingResult
  - id (PK), job (FK), resultType, resultData (LOB), createdAt
  - Many-to-one -> Job

This schema supports multiple processing jobs per document and multiple results per job.

## 🔐 Security (JWT Flow)

1. User logs in via `/api/auth/login` with username/password.
2. Server validates credentials and returns a signed JWT containing the username and role.
3. Client stores the JWT (e.g., localStorage or secure cookie) and includes it in the `Authorization: Bearer <token>` header for subsequent requests.
4. `JwtAuthenticationFilter` validates the token on each request and sets the `SecurityContext` with an authenticated `UserDetails`.
5. Role-based authorization (USER/ADMIN) is applied using Spring Security.

Security notes:
- Use HTTPS in production.
- Keep `jwt.secret` safe and rotate periodically.

## 📧 Email Configuration

- Enable email: `app.email.enabled=true`
- Configure `spring.mail.*` properties in `application.properties`.
- Example using Gmail (not recommended for production):

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=youremail@gmail.com
spring.mail.password=app-specific-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.from=youremail@gmail.com
```

The `EmailService` provides helper methods:
- `sendWelcomeEmail(to, username)`
- `sendJobCompletedEmail(to, username, jobId, documentName)`
- `sendJobFailedEmail(to, username, jobId, documentName, errorMessage)`

## 🌐 WebSocket Integration

- STOMP endpoint: `/ws` (SockJS fallback enabled)
- Application prefix: `/app`
- Topic prefix for broadcast: `/topic`

Example client (JavaScript using STOMP.js + SockJS):

```js
const socket = new SockJS('/ws');
const client = Stomp.over(socket);
client.connect({}, function(frame) {
  client.subscribe('/topic/jobs', function(message) {
    const payload = JSON.parse(message.body);
    console.log('Job notification', payload);
  });
});
```

## 🐳 Docker Deployment

A `docker-compose.yml` is included to run dependencies (Postgres, RabbitMQ, Redis). To run locally:

```powershell
# Start services
docker-compose up -d

# Stop services
docker-compose down
```

Build and run the application in Docker (example Dockerfile required):

```powershell
# Build jar locally
./mvnw.cmd package -DskipTests

# Run the packaged jar (example)
docker build -t docprocessor:latest .
docker run -e SPRING_PROFILES_ACTIVE=prod -p 8080:8080 docprocessor:latest
```

## 📝 Future Enhancements

- OCR implementation (Tesseract integration)
- S3 / object storage integration for scalable file storage
- Advanced search filters and Elasticsearch integration
- Document versioning and change history
- Batch processing API and bulk uploads

## 🤝 Contributing

We welcome contributions — please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Run tests and linters
4. Submit a Pull Request with a clear description and tests

Please follow code style and write unit/integration tests for new features.

## 📄 License

This project is released under the MIT License. See `LICENSE` for details.

## 👤 Author

Your Name <your-email@example.com>

(Replace with your real name and contact information)

## 🙏 Acknowledgments

Thanks to the following libraries and resources:

- Spring Boot / Spring Security
- RabbitMQ and Spring AMQP
- Apache PDFBox
- Thumbnailator
- Testcontainers for integration tests
- Springdoc OpenAPI / Swagger

---

If you'd like, I can also:
- Add a small `README-DEV.md` with detailed local development steps for IntelliJ
- Generate a Postman collection from the controllers
- Create a sample `application.yml` for Docker-based deployments

Tell me which of those you'd like next and I will add them.
