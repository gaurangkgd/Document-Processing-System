# 🔍 Search Debugging - Enhanced Logging Added

## Changes Made

### 1. Added @EnableJpaAuditing
**File:** `DocumentProcessingSystemApplication.java`
- Enables automatic population of `@CreatedDate` and `@LastModifiedDate`
- Fixes "Invalid Date" issue for NEW uploads

### 2. Added Logging to DocumentProcessor
**File:** `DocumentProcessor.java`
- Logs how many characters of text were extracted
- Shows a preview of extracted text (first 100 characters)
- Helps debug if text extraction is working

### 3. Added Logging to SearchService  
**File:** `SearchService.java`
- Logs the search keyword
- Logs how many results were found
- Helps debug search query execution

## 🚀 Next Steps: Restart & Test

### Step 1: Restart Backend
**The backend MUST be restarted for all changes to take effect!**

**IntelliJ IDEA:**
1. Stop application (⏹️ button)
2. Run application (▶️ button)
3. Wait for "Started DocumentProcessingSystemApplication"

**Or Terminal:**
```bash
cd "C:\Users\LENOVO\Documento\Projects\SpringBoot\document-processing-system"
./mvnw spring-boot:run
```

### Step 2: Create a Test File

Create a file named `test-search.txt` with this content:
```
This is a test document for searching.
Keywords: Resume, software engineer, Java, Spring Boot.
John Doe is looking for a job.
Skills include programming and debugging.
```

### Step 3: Upload the Test File

1. Go to http://localhost:3000/documents
2. Click "Choose File"
3. Select `test-search.txt`
4. Click "Upload"
5. **Wait 5-10 seconds** for processing

### Step 4: Check the Logs

**Look at the backend console/logs for:**

```
INFO  c.d.s.messaging.DocumentProcessor - Extracted 123 characters of text from document 5
DEBUG c.d.s.messaging.DocumentProcessor - Extracted text preview: This is a test document for searching.
Keywords: Resume, software engineer, ...
```

**This tells you:**
- ✅ Text was extracted
- ✅ Shows how much text (123 characters)
- ✅ Shows what the text looks like

### Step 5: Search for Known Text

1. Go to http://localhost:3000/search
2. Search for: **"Resume"**
3. **Check backend logs for:**

```
INFO  c.d.s.service.SearchService - Searching for keyword: 'Resume'
INFO  c.d.s.service.SearchService - Found 1 processing results for keyword 'Resume'
```

**If you see:**
- `Found 1 processing results` → ✅ **Search is working!**
- `Found 0 processing results` → ❌ Text not in database or query issue

### Step 6: Try Different Searches

Try searching for:
- "software" → Should find the test document
- "Java" → Should find the test document
- "xyz123" → Should find 0 results (expected)

---

## 🔍 Debugging Guide

### Issue: Search Returns 0 Results

#### Check 1: Is Text Being Extracted?

**Look for in logs:**
```
INFO  c.d.s.messaging.DocumentProcessor - Extracted 0 characters
```

**If you see 0 characters:**
- PDF might be scanned image (needs OCR)
- PDF might be encrypted/protected
- File might not be a PDF

#### Check 2: Is Text Being Saved?

**Look for:**
```
INFO  c.d.s.messaging.DocumentProcessor - Job X completed successfully
```

If job completes, text should be in database.

#### Check 3: Is Search Query Executing?

**Look for:**
```
INFO  c.d.s.service.SearchService - Searching for keyword: 'your-keyword'
INFO  c.d.s.service.SearchService - Found 0 processing results
```

**If "Found 0 processing results":**
- The word isn't in any document
- Try searching for common words like "the", "and", "is"

#### Check 4: Are Documents Being Processed?

**Check Dashboard:**
- Total Documents: Should increase after upload
- Completed Jobs: Should increase after processing (5-10 seconds)

If "Pending Jobs" stays at 0 and "Completed" doesn't increase:
- RabbitMQ might not be running
- Worker might not be listening

---

## 🎯 Expected Log Output (Working System)

### When Uploading:
```
INFO  c.d.s.DocumentProcessingSystemApplication - Started DocumentProcessingSystemApplication
INFO  o.s.a.r.c.CachingConnectionFactory - Created new connection: rabbitConnectionFactory
```

### When Processing:
```
INFO  c.d.s.messaging.DocumentProcessor - Received job message for jobId=22
INFO  c.d.s.messaging.DocumentProcessor - Extracted 234 characters of text from document 6
DEBUG c.d.s.messaging.DocumentProcessor - Extracted text preview: This is a test...
INFO  c.d.s.messaging.DocumentProcessor - Job 22 completed successfully
```

### When Searching:
```
INFO  c.d.s.service.SearchService - Searching for keyword: 'Resume'
INFO  c.d.s.service.SearchService - Found 1 processing results for keyword 'Resume'
```

---

## 📊 Testing Checklist

After restart, verify each step:

- [ ] Backend started successfully
- [ ] RabbitMQ connected (see logs)
- [ ] Upload test file with known text
- [ ] Wait 10 seconds
- [ ] Check logs: "Extracted X characters"
- [ ] Check logs: "Job X completed"
- [ ] Dashboard shows increased counts
- [ ] Search for word from test file
- [ ] Check logs: "Searching for keyword"
- [ ] Check logs: "Found X processing results"
- [ ] Frontend shows search results

---

## 🐛 Common Issues & Solutions

### Issue: "Extracted 0 characters"
**Cause:** PDF is scanned image
**Solution:** OCR is not implemented yet, or try a text-based PDF

### Issue: "Found 0 processing results"  
**Cause:** Word not in any document
**Solution:** Search for text you KNOW is in the uploaded document

### Issue: RabbitMQ connection error
**Cause:** Docker not running
**Solution:** `docker-compose up -d`

### Issue: No processing logs
**Cause:** Worker not receiving messages
**Solution:** Check RabbitMQ connection, restart backend

---

## 📝 Why This Will Help

**Before (No Logging):**
- Upload file → Nothing happens → Search returns 0 → No idea why

**After (With Logging):**
- Upload file
- See: "Extracted 500 characters"  
- See: "Job completed successfully"
- Search
- See: "Searching for 'Resume'"
- See: "Found 1 processing results"
- **Now you know exactly what's happening!**

---

## 🎉 Next Action

**RESTART THE BACKEND NOW** and follow the testing steps above!

The new logging will show you exactly:
1. How much text is being extracted
2. What the extracted text looks like
3. How many results the search finds
4. Whether text is being saved to database

This will pinpoint the exact issue!
