# 🚀 Quick Start Guide - Document Processing System

## Prerequisites Check ✅

Before starting, ensure you have:
- ✅ Node.js 16+ installed
- ✅ Java 17+ installed
- ✅ Docker Desktop running (for PostgreSQL, Redis, RabbitMQ)

## Step-by-Step Launch Instructions

### 1️⃣ Start Docker Services

Open terminal in project root and run:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- RabbitMQ (port 5672, management: 15672)

Verify services are running:
```bash
docker ps
```

### 2️⃣ Start Spring Boot Backend

#### Option A: Using IntelliJ IDEA
1. Open `DocumentProcessingSystemApplication.java`
2. Click the green play button ▶️ next to the main method
3. Wait until you see "Started DocumentProcessingSystemApplication"

#### Option B: Using Terminal
```bash
# In project root directory
./mvnw spring-boot:run
```

Backend will start on: **http://localhost:8080**

### 3️⃣ Start React Frontend

Open a NEW terminal window:

```bash
cd frontend
npm run dev
```

Frontend will start on: **http://localhost:3000**

You should see output like:
```
  VITE v7.x.x  ready in xxx ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### 4️⃣ Open Your Browser

Navigate to: **http://localhost:3000**

You should see the login page!

---

## 🎯 First-Time Usage

### Step 1: Register a New Account
1. Click **"Register"** button in the navbar
2. Fill in the form:
   - Full Name: `John Doe`
   - Username: `john`
   - Email: `john@example.com`
   - Password: `password123`
3. Click **"Register"**
4. You'll be automatically logged in and redirected to Dashboard

### Step 2: Upload Your First Document
1. Click **"Documents"** in the navbar
2. Click **"Choose File"** button
3. Select a PDF, DOC, DOCX, or TXT file
4. Click **"Upload"**
5. Wait for processing to complete (status will update automatically)

### Step 3: View Dashboard
1. Click **"Dashboard"** in the navbar
2. See your statistics:
   - Total Documents
   - Pending Jobs
   - Completed Jobs
3. View recent documents

### Step 4: Search Documents
1. Click **"Search"** in the navbar
2. Enter a search term (e.g., "invoice", "contract")
3. Click **"Search"**
4. View results with matching content
5. Click **"View Document"** to download

---

## 🔍 Verify Everything is Working

### Check Backend (Port 8080)
Open browser: http://localhost:8080/actuator/health

Should return:
```json
{"status":"UP"}
```

### Check Frontend (Port 3000)
Open browser: http://localhost:3000

Should show the login/register page with:
- "DocProcessor" logo
- Login form or navigation to register

### Check Docker Services
```bash
docker ps
```

Should show 3 running containers:
- postgres
- redis
- rabbitmq

---

## 🛠️ Troubleshooting

### Frontend Won't Start

**Error: "Cannot find module 'react-router-dom'"**
```bash
cd frontend
npm install
```

**Error: "Port 3000 is already in use"**
```bash
# Kill the process using port 3000
# Windows:
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Then restart:
npm run dev
```

### Backend Won't Start

**Error: "Connection refused: localhost:5432"**
```bash
# Start Docker services
docker-compose up -d

# Verify PostgreSQL is running
docker ps | grep postgres
```

**Error: "Port 8080 already in use"**
```bash
# Windows: Find and kill the process
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### CORS Errors

If you see CORS errors in browser console:
1. Check that backend CORS configuration includes `http://localhost:3000`
2. Restart the backend application

### Tailwind Styles Not Loading

```bash
cd frontend
# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
npm run dev
```

---

## 📦 API Endpoints Overview

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Documents
- `POST /api/documents/upload` - Upload document (multipart/form-data)
- `GET /api/documents` - Get all documents
- `GET /api/documents/{id}` - Download document
- `DELETE /api/documents/{id}` - Delete document

### Jobs
- `GET /api/jobs` - Get all processing jobs
- `GET /api/jobs/{id}/status` - Get job status

### Search
- `GET /api/search?query={searchTerm}` - Search documents

---

## 🎨 UI Features

### Responsive Design
- Works on desktop, tablet, and mobile
- Responsive navigation menu
- Adaptive layouts

### Real-time Updates
- Document upload progress
- Processing status updates
- Dynamic statistics

### Clean UI
- Tailwind CSS styling
- Primary color: Blue (#3b82f6)
- Modern, professional design

---

## 📊 Testing the Complete Flow

### Test 1: User Registration & Login
1. ✅ Register new user
2. ✅ Verify redirect to dashboard
3. ✅ Logout
4. ✅ Login with same credentials
5. ✅ Verify dashboard loads

### Test 2: Document Upload
1. ✅ Navigate to Documents page
2. ✅ Upload a PDF file
3. ✅ Verify success message
4. ✅ Check document appears in list
5. ✅ Verify file size and type are displayed

### Test 3: Document Processing
1. ✅ Upload a document
2. ✅ Check Dashboard for pending jobs
3. ✅ Wait for processing to complete
4. ✅ Verify completed jobs count increases

### Test 4: Document Search
1. ✅ Go to Search page
2. ✅ Enter a keyword from your document
3. ✅ Verify search results appear
4. ✅ Click "View Document" to download

### Test 5: Document Management
1. ✅ View all documents
2. ✅ Download a document
3. ✅ Delete a document
4. ✅ Verify it's removed from list

---

## 🎯 Architecture Overview

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Browser   │────────▶│ React (3000) │────────▶│ Spring Boot  │
│             │◀────────│   Frontend   │◀────────│   (8080)     │
└─────────────┘         └──────────────┘         └──────┬───────┘
                                                          │
                                                          ▼
                        ┌────────────────────────────────────┐
                        │         Docker Services            │
                        │  ┌──────────┐  ┌───────┐  ┌─────┐│
                        │  │PostgreSQL│  │ Redis │  │RMQ  ││
                        │  │  (5432)  │  │(6379) │  │(5672)││
                        │  └──────────┘  └───────┘  └─────┘│
                        └────────────────────────────────────┘
```

---

## 📝 Development Tips

### Hot Reload
- **Frontend**: Changes to React components reload automatically
- **Backend**: Use Spring Boot DevTools for automatic restart

### Debugging
- **Frontend**: Open browser DevTools (F12) → Console tab
- **Backend**: Check console output in IntelliJ or terminal

### API Testing
Use Postman collection in project root:
```bash
postman_collection.json
```

---

## 🎉 You're Ready!

Everything is set up and ready to use. Follow the steps above to launch your Document Processing System.

**Happy Processing! 🚀**

---

## 📞 Need Help?

If you encounter any issues:
1. Check the troubleshooting section above
2. Verify all services are running
3. Check console logs for error messages
4. Ensure all dependencies are installed

## 🔗 Quick Links

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Health Check: http://localhost:8080/actuator/health
