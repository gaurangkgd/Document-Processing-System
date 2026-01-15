# Quick Start Guide

## 🚀 Get Running in 5 Minutes

### Step 1: Start Docker Containers
```bash
docker-compose up -d
```

Wait 30 seconds for containers to be ready.

### Step 2: Configure Application
Copy `application-example.properties` to `application.properties`:
```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Update these values:
- JWT secret
- Email credentials (or disable: `app.email.enabled=false`)

### Step 3: Run Application
```bash
mvn spring-boot:run
```

Or in IntelliJ: Right-click `DocumentProcessingSystemApplication` → Run

### Step 4: Verify Setup
1. **API Health:** http://localhost:8080/actuator/health
2. **Swagger UI:** http://localhost:8080/swagger-ui.html
3. **RabbitMQ Management:** http://localhost:15672 (admin/admin123)

### Step 5: Test API

**Register User:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@email.com",
    "password": "password123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

Copy the JWT token from response.

**Upload Document:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/test.pdf"
```

**Check Job Status:**
```bash
curl -X GET http://localhost:8080/api/jobs/JOB_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 🎉 Done!

Your document is being processed in the background. Check job status or wait for email notification.

## 🛠️ Troubleshooting

**Database connection failed:**
- Verify PostgreSQL container is running: `docker ps`
- Check connection string in application.properties

**RabbitMQ connection failed:**
- Verify RabbitMQ container is running
- Check credentials match docker-compose.yml

**File upload fails:**
- Check uploads folder exists and has write permissions
- Verify file size is under 50MB

**Email not sending:**
- For development, set `app.email.enabled=false`
- Or use Mailtrap for testing

## 📚 Next Steps

- Explore Swagger UI for all API endpoints
- Import Postman collection for easy testing
- Check logs: `tail -f logs/application.log`
- Run tests: `mvn test`

