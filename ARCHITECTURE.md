# System Architecture

## High-Level Architecture

```
┌─────────────┐
│   Frontend  │
│  (Browser)  │
└──────┬──────┘
       │ HTTP/WebSocket
       ↓
┌─────────────────────────────────┐
│     Spring Boot API Server      │
│  ┌──────────────────────────┐  │
│  │  Controllers             │  │
│  │  ├─ AuthController       │  │
│  │  ├─ DocumentController   │  │
│  │  └─ JobController        │  │
│  └──────────┬───────────────┘  │
│             ↓                   │
│  ┌──────────────────────────┐  │
│  │  Services                │  │
│  │  ├─ UserService          │  │
│  │  ├─ DocumentService      │  │
│  │  ├─ JobService           │  │
│  │  └─ EmailService         │  │
│  └──────────┬───────────────┘  │
│             ↓                   │
│  ┌──────────────────────────┐  │
│  │  Repositories            │  │
│  └──────────┬───────────────┘  │
└─────────────┼───────────────────┘
              ↓
     ┌────────┴────────┐
     │                 │
┌────┴─────┐    ┌──────┴──────┐
│PostgreSQL│    │  RabbitMQ   │
│ Database │    │   Queue     │
└──────────┘    └──────┬──────┘
                       │
                       ↓
              ┌─────────────────┐
              │  Worker Process │
              │ (Async Consumer)│
              └─────────┬───────┘
                        │
                 ┌──────┴──────┐
                 │             │
          ┌──────┴─────┐  ┌────┴─────┐
          │File Storage│  │  Redis   │
          └────────────┘  │  Cache   │
                          └──────────┘
```

## Component Description

### API Server (Spring Boot)

The API Server is the main entry point for all client interactions. It handles:

- **HTTP Requests**: RESTful API endpoints for authentication, document management, and job tracking
- **JWT Authentication**: Validates user credentials and issues/validates JWT tokens
- **WebSocket Connections**: Real-time notifications via STOMP over SockJS
- **Input Validation**: Uses Jakarta Validation annotations to validate DTOs
- **Job Enqueueing**: Publishes processing jobs to RabbitMQ queue
- **Authorization**: Role-based access control (USER/ADMIN)

**Key Components:**
- **Controllers**: Handle HTTP requests, validate input, return responses
- **Services**: Business logic, transaction management
- **Repositories**: JPA interfaces for database access
- **Security**: JWT filters, password encoding, Spring Security configuration
- **Exception Handlers**: Global exception handling for consistent error responses

### Worker Process (Asynchronous Consumer)

The Worker Process is responsible for heavy document processing tasks:

- **Queue Listener**: Consumes jobs from RabbitMQ using `@RabbitListener`
- **Document Processing**: 
  - Text extraction from PDFs (Apache PDFBox)
  - Thumbnail generation (Thumbnailator)
  - Metadata extraction (Apache Tika)
  - OCR (placeholder for future implementation)
- **Result Storage**: Saves processing results to database
- **Error Handling**: Implements retry logic with exponential backoff
- **Notifications**: Sends WebSocket and email notifications on completion/failure

**Processing Types:**
- `TEXT_EXTRACTION`: Extract text content from documents
- `THUMBNAIL`: Generate preview thumbnails
- `OCR`: Optical character recognition (future)
- `METADATA`: Extract document metadata

### Database (PostgreSQL)

Relational database for persistent storage:

**Entities:**
- **User**: Authentication and user profile data
- **Document**: File metadata and storage information
- **Job**: Processing job status and history
- **ProcessingResult**: Extracted text, thumbnails, metadata

**Key Features:**
- ACID transactions
- Foreign key constraints
- Indexed queries for performance
- Audit timestamps (createdAt, updatedAt)

### Message Queue (RabbitMQ)

Asynchronous message broker for job distribution:

**Features:**
- **Decoupling**: Separates API from heavy processing
- **Job Queue**: Distributes work to multiple workers
- **Dead Letter Queue**: Handles permanently failed jobs
- **Retry Logic**: Automatic retry with configurable max attempts
- **Scalability**: Add more workers to handle load

**Queue Configuration:**
- Main queue: `document.processing.queue`
- Dead letter queue: `document.processing.dlq`
- Direct exchange with routing keys

### Cache (Redis)

In-memory cache for performance optimization:

**Use Cases:**
- Session storage (optional)
- Job status caching
- Rate limiting
- Temporary data storage
- Reduce database load

### File Storage

Document storage system:

**Current Implementation:**
- Local filesystem storage
- UUID-based filenames for uniqueness
- Configurable storage location

**Future Enhancements:**
- AWS S3 integration
- Azure Blob Storage
- Google Cloud Storage
- CDN integration for thumbnails

## Data Flow

### 1. Document Upload Flow

```
User → API Server → Database → RabbitMQ → Worker → Storage
  │         │          │           │          │        │
  │         │          │           │          │        │
  1────────→2─────────→3──────────→4         │        │
            │                                 │        │
            5←────────────────────────────────┘        │
            │                                          │
            6←─────────────────────────────────────────┘
```

**Steps:**
1. User uploads document via `POST /api/documents/upload`
2. API validates file (type, size, permissions)
3. API saves file to storage with UUID filename
4. API creates `Document` record in database
5. API creates `Job` record with status `QUEUED`
6. API publishes job message to RabbitMQ
7. API responds immediately with document ID and job ID
8. Worker picks up job from queue
9. Worker updates job status to `PROCESSING`
10. Worker processes document based on job type
11. Worker saves results to `ProcessingResult` table
12. Worker updates job status to `COMPLETED` or `FAILED`
13. Worker sends WebSocket notification to user
14. Worker sends email notification (if enabled)

### 2. Authentication Flow

```
User → API → Database → JWT Token → Subsequent Requests
  │      │       │          │              │
  │      │       │          │              │
  1─────→2──────→3         │              │
         │                 │              │
         4←────────────────┘              │
         │                                │
         5←───────────────────────────────┘
```

**Steps:**

**Registration:**
1. User sends credentials to `POST /api/auth/register`
2. API validates input (username, email, password)
3. API checks for duplicate username/email
4. API encrypts password using BCrypt
5. API saves user to database with default `USER` role
6. API returns user details (without password)

**Login:**
1. User sends credentials to `POST /api/auth/login`
2. API finds user by username
3. API validates password using `PasswordEncoder.matches()`
4. API generates JWT token with username and role
5. API returns token and user details
6. User includes token in `Authorization: Bearer <token>` header
7. `JwtAuthenticationFilter` intercepts each request
8. Filter validates token signature and expiration
9. Filter loads user details and sets `SecurityContext`
10. Spring Security authorizes based on role

### 3. Job Processing Flow

```
RabbitMQ Queue → Worker → Processing → Result Storage → Notification
      │            │          │              │               │
      │            │          │              │               │
      1───────────→2─────────→3─────────────→4──────────────→5
```

**Steps:**
1. Worker consumes job ID from RabbitMQ queue
2. Worker loads job and document from database
3. Worker updates job status to `PROCESSING`
4. Worker updates `startedAt` timestamp
5. Worker performs processing based on `jobType`:
   - **TEXT_EXTRACTION**: Use PDFBox to extract text
   - **THUMBNAIL**: Use Thumbnailator to generate image
   - **METADATA**: Use Tika to extract metadata
   - **OCR**: Placeholder for Tesseract integration
6. Worker saves results to `ProcessingResult` table
7. Worker updates job status to `COMPLETED`
8. Worker updates `completedAt` timestamp
9. Worker sends WebSocket notification via `SimpMessagingTemplate`
10. Worker sends email notification via `EmailService`

**Error Handling:**
- If processing fails, worker catches exception
- Worker increments `retryCount`
- If `retryCount < maxRetries`:
  - Update status back to `QUEUED`
  - Re-publish job to queue (with delay)
- If `retryCount >= maxRetries`:
  - Update status to `FAILED`
  - Save error message
  - Send failure notification

### 4. WebSocket Notification Flow

```
Worker → SimpMessagingTemplate → WebSocket Broker → Connected Clients
   │              │                      │                  │
   │              │                      │                  │
   1─────────────→2─────────────────────→3─────────────────→4
```

**Steps:**
1. Worker or API creates `JobNotificationDTO`
2. Service calls `SimpMessagingTemplate.convertAndSend()`
3. Message sent to `/topic/jobs` destination
4. All subscribed clients receive notification
5. Frontend updates UI in real-time

**WebSocket Configuration:**
- Endpoint: `/ws` with SockJS fallback
- Protocol: STOMP over WebSocket
- Application prefix: `/app`
- Topic prefix: `/topic`

## Security Architecture

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "username",
    "role": "USER",
    "iat": 1642345678,
    "exp": 1642432078
  },
  "signature": "HMACSHA256(...)"
}
```

### Security Layers

1. **Password Encryption**: BCrypt with salt
2. **JWT Tokens**: HS256 signed tokens
3. **Filter Chain**: `JwtAuthenticationFilter` → `UsernamePasswordAuthenticationFilter`
4. **Authorization**: `@PreAuthorize` annotations, role-based access
5. **CSRF**: Disabled (stateless API with JWT)
6. **Session Management**: Stateless (no server-side sessions)

### Protected Endpoints

- `/api/auth/**` → Public (register, login)
- `/api/documents/**` → Authenticated (USER role)
- `/api/jobs/**` → Authenticated (USER role)
- `/api/admin/**` → Authenticated (ADMIN role only)

## Scalability Considerations

### Horizontal Scaling

**API Servers:**
- Stateless design allows multiple instances
- Load balancer distributes traffic
- JWT tokens work across all instances
- No session affinity required

**Workers:**
- Add more worker instances to handle load
- RabbitMQ distributes jobs automatically
- Each worker processes independently
- Compete for jobs from queue

### Performance Optimization

1. **Database Indexing**: Indexes on userId, status, createdAt
2. **Connection Pooling**: HikariCP for database connections
3. **Caching**: Redis for frequently accessed data
4. **Async Processing**: Non-blocking operations
5. **Pagination**: Limit result sets for large data
6. **Lazy Loading**: JPA relationships with LAZY fetch

### Monitoring and Observability

**Recommended Tools:**
- **Logging**: SLF4J + Logback, structured JSON logs
- **Metrics**: Spring Boot Actuator + Micrometer
- **Tracing**: Spring Cloud Sleuth (if using microservices)
- **APM**: New Relic, Datadog, or Elastic APM
- **Health Checks**: `/actuator/health` endpoint

## Deployment Architecture

### Development Environment

```
Docker Compose → Postgres + RabbitMQ + Redis
        ↑
        │
   Spring Boot (local)
```

### Production Environment (Example)

```
Load Balancer (AWS ALB/ELB)
        ↓
   ┌────┴────┐
   │         │
API-1    API-2 (Auto-scaling)
   │         │
   └────┬────┘
        ↓
   RDS (Postgres)
        ↓
   ElastiCache (Redis)
        ↓
   Amazon MQ (RabbitMQ)
        ↓
   ┌────┴────┐
   │         │
Worker-1  Worker-2 (Auto-scaling)
   │         │
   └────┬────┘
        ↓
    S3 Bucket
```

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Framework | Spring Boot 3.x | Application framework |
| Language | Java 17 | Programming language |
| Database | PostgreSQL | Relational data storage |
| Message Queue | RabbitMQ | Async job processing |
| Cache | Redis | Performance optimization |
| Security | Spring Security + JWT | Authentication & authorization |
| ORM | Spring Data JPA | Database access |
| Validation | Jakarta Validation | Input validation |
| File Processing | PDFBox, Thumbnailator, Tika | Document processing |
| WebSocket | Spring WebSocket + STOMP | Real-time notifications |
| Email | Spring Mail | Email notifications |
| Testing | JUnit 5, Mockito, Testcontainers | Unit & integration tests |
| API Docs | Springdoc OpenAPI | API documentation |
| Build | Maven | Dependency management |

## Design Patterns Used

1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic encapsulation
3. **DTO Pattern**: Data transfer objects for API
4. **Factory Pattern**: Entity creation
5. **Observer Pattern**: WebSocket notifications
6. **Strategy Pattern**: Different processing types
7. **Builder Pattern**: Lombok builders for entities
8. **Dependency Injection**: Spring's IoC container
9. **Filter Chain**: Security filters
10. **Producer-Consumer**: RabbitMQ job queue

## Future Architecture Enhancements

1. **Microservices**: Split into separate services (Auth, Document, Processing)
2. **API Gateway**: Single entry point with routing
3. **Service Discovery**: Eureka or Consul
4. **Event Sourcing**: Track all state changes
5. **CQRS**: Separate read/write models
6. **GraphQL**: Alternative to REST API
7. **Kubernetes**: Container orchestration
8. **Elasticsearch**: Full-text search
9. **Object Storage**: S3/Azure Blob for files
10. **Message Streaming**: Kafka for event streaming

---

**Document Version**: 1.0  
**Last Updated**: January 15, 2026  
**Author**: Document Processing System Team

