# 🚀 Quick Fix: Emulator Internet Connection

## 🔍 Is Internet Working?

### Quick Test (30 seconds)

**Run this script:**

```bash
check_emulator_internet.bat
```

**Or manually test:**

```bash
adb shell ping -c 4 8.8.8.8
```

**✅ Working:** You see ping responses  
**❌ Not working:** "Network unreachable" or timeout

---

## 🛠️ Quick Fix (1 minute)

### Automatic Fix

**Run this script:**

```bash
fix_emulator_internet.bat
```

This automatically:

- Resets network
- Restarts Wi-Fi
- Configures DNS
- Tests connectivity

### Manual Fix (If script doesn't work)

**3 quick commands:**

```bash
adb shell cmd connectivity airplane-mode enable
timeout 2
adb shell cmd connectivity airplane-mode disable
```

Then test:

```bash
adb shell ping -c 4 8.8.8.8
```

---

## 🎯 If Still Not Working

### 1. Restart Emulator (90% success rate)

**In Android Studio:**

- Tools → Device Manager
- Stop emulator
- Wait 5 seconds
- Start emulator

### 2. Check Your PC's Internet

```bash
ping 8.8.8.8
```

If your PC doesn't have internet, emulator won't either!

### 3. Check Windows Firewall

1. Open **Windows Security**
2. **Firewall & network protection**
3. **Allow an app through firewall**
4. Enable:
    - Android Emulator
    - qemu-system-x86_64.exe

---

## 🌐 For Your DriftGuardAI App

### Special Emulator IP for Backend

Your app connects to backend server. Use this special IP:

**Instead of:**

```kotlin
val serverUrl = "ws://192.168.1.100:8080"
```

**Use:**

```kotlin
val serverUrl = "ws://10.0.2.2:8080"
```

`10.0.2.2` = Your PC's localhost from emulator's perspective

### Setup Backend Connection

1. **Start backend on PC:**
   ```bash
   cd backend
   npm start
   ```

2. **Use port forwarding:**
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```

3. **In app, connect to:**
    - `ws://localhost:8080` or
    - `ws://10.0.2.2:8080`

---

## ✅ Verification Checklist

After fixing, check:

- [ ] `adb shell ping 8.8.8.8` works
- [ ] Can open Chrome on emulator
- [ ] Can browse websites
- [ ] DriftGuardAI connects to backend
- [ ] WebSocket status shows "Connected"

---

## 📊 Quick Commands Reference

| Command | Purpose |
|---------|---------|
| `check_emulator_internet.bat` | Test internet connectivity |
| `fix_emulator_internet.bat` | Auto-fix internet issues |
| `adb shell ping -c 4 8.8.8.8` | Manual internet test |
| `adb reverse tcp:8080 tcp:8080` | Forward backend port |
| `adb shell svc wifi disable/enable` | Restart Wi-Fi |

---

## 🆘 Still Broken?

### Nuclear Option: Reset Emulator

1. Android Studio → Tools → Device Manager
2. Click **⋮** (three dots) on your emulator
3. **Wipe Data**
4. Restart emulator
5. Internet should work now

### Alternative: Use ADB Port Forwarding

If emulator internet never works, bypass it:

```bash
# Forward all connections through ADB
adb reverse tcp:8080 tcp:8080
adb reverse tcp:3000 tcp:3000
adb reverse tcp:5000 tcp:5000

# Now use localhost in your app
```

---

## 💡 Pro Tips

1. **Always restart emulator first** - Fixes 90% of issues
2. **Check PC internet** - Emulator depends on it
3. **Use 10.0.2.2** - Special IP for localhost
4. **Use port forwarding** - Bypass network issues
5. **Check firewall** - Often blocks emulator

---

## 📖 More Info

For detailed troubleshooting, see:

- **`CHECK_EMULATOR_INTERNET.md`** - Complete guide
- **`BACKEND_SETUP_GUIDE.md`** - Backend connection setup

---

**Quick Summary:**

1. Run `check_emulator_internet.bat` to test
2. Run `fix_emulator_internet.bat` to fix
3. If still broken, restart emulator
4. Use `10.0.2.2` for connecting to backend on PC
