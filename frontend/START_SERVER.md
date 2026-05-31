# 🚨 Frontend Server Startup Guide

## Step 1: Open a New Terminal

In IntelliJ IDEA or your system terminal:
1. Press `Alt + F12` (IntelliJ) or open a new terminal window
2. Navigate to the frontend directory

## Step 2: Run These Commands

```bash
# Navigate to frontend directory
cd "C:\Users\LENOVO\Documento\Projects\SpringBoot\document-processing-system\frontend"

# Start the dev server
npm run dev
```

## Expected Output

You should see something like:

```
  VITE v7.x.x  ready in 500 ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

## Step 3: Check for Errors

### If you see an error about Tailwind:
Run this first:
```bash
npm install -D @tailwindcss/vite
```

Then try again:
```bash
npm run dev
```

### If you see "port 3000 already in use":
Kill the process:
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID_NUMBER> /F
```

Then restart:
```bash
npm run dev
```

### If you see "module not found":
Reinstall dependencies:
```bash
rm -rf node_modules package-lock.json
npm install
npm run dev
```

## Step 4: Open Browser

Once the server starts successfully:
1. Open your browser
2. Navigate to: **http://localhost:3000**
3. You should see the login page

## Troubleshooting Checklist

- [ ] Terminal shows "ready in X ms"
- [ ] URL shows http://localhost:3000
- [ ] No error messages in terminal
- [ ] Browser can reach localhost:3000

## Common Issues

### Issue: Blank page
**Solution:** Check browser console (F12) for JavaScript errors

### Issue: Styles not loading
**Solution:** Hard refresh (Ctrl + Shift + R)

### Issue: Connection refused
**Solution:** Server isn't running - check terminal output

---

**If the server starts but you still see errors, copy the terminal output and share it!**
