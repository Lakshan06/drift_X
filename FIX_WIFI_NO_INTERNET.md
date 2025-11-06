## 🎯 For DriftGuardAI Specific

### Test Backend Connection

After fixing Wi-Fi:

```bash
# Test if emulator can reach your PC
adb shell ping -c 4 10.0.2.2

# Test backend health endpoint
adb shell curl http://10.0.2.2:8080/health
```

### Update App Configuration

If using local backend, update to use emulator's special IP:

**File:** `app/src/main/java/com/driftdetector/app/di/AppModule.kt`

```kotlin
single {
    try {
        Log.d("KOIN", "Creating RealtimeClient...")
        // Use emulator's special IP for localhost
        val serverUrl = "ws://10.0.2.2:8080"  // Points to PC's localhost
        
        // Or use port forwarding:
        // val serverUrl = "ws://localhost:8080"
        // (After running: adb reverse tcp:8080 tcp:8080)
        
        RealtimeClient(serverUrl)
    } catch (e: Exception) {
        Log.e("KOIN", "Failed to create RealtimeClient", e)
        throw e
    }
}
```

### Test File Access

After fixing Wi-Fi:

1. Open **Model Upload** screen
2. Select **Local Files**
3. You should now see folders ✅
4. Try uploading a file ✅
