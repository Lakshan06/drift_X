# 🎨 How to Change DriftGuardAI App Logo

**Your Logo:** `logo.svg` (Beautiful DG initials with sci-fi theme!)  
**Status:** Ready to use ✅

---

## 🚀 Quick Guide (3 Methods)

### **Method 1: Automated Script (Recommended - 2 minutes)**

### **Method 2: Android Studio (Easy - 5 minutes)**

### **Method 3: Manual (Complete Control - 10 minutes)**

---

## 📱 What You're Changing

### 1. **App Icon** (Launcher Icon)

- Shows on home screen
- Shows in app drawer
- Shows in recent apps

### 2. **Splash Screen** (Opening Logo)

- Shows when app first opens
- Displays for 1-2 seconds
- Optional: Can add to onboarding

---

## 🎯 Method 1: Automated Script (RECOMMENDED)

### Step 1: Install Node.js Tools

```bash
# Install sharp for image processing
npm install -g sharp-cli

# Or use Python with Pillow
pip install Pillow
```

### Step 2: Convert SVG to PNG (Multiple Sizes)

**Option A: Using ImageMagick** (Best quality)

```bash
# Install ImageMagick first
# Windows: choco install imagemagick
# Mac: brew install imagemagick

# Convert to all required sizes
magick convert -background none -density 600 logo.svg -resize 48x48 app/src/main/res/mipmap-mdpi/ic_launcher.png
magick convert -background none -density 600 logo.svg -resize 72x72 app/src/main/res/mipmap-hdpi/ic_launcher.png
magick convert -background none -density 600 logo.svg -resize 96x96 app/src/main/res/mipmap-xhdpi/ic_launcher.png
magick convert -background none -density 600 logo.svg -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
magick convert -background none -density 600 logo.svg -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# Round icon versions (same sizes)
magick convert -background none -density 600 logo.svg -resize 48x48 app/src/main/res/mipmap-mdpi/ic_launcher_round.png
magick convert -background none -density 600 logo.svg -resize 72x72 app/src/main/res/mipmap-hdpi/ic_launcher_round.png
magick convert -background none -density 600 logo.svg -resize 96x96 app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
magick convert -background none -density 600 logo.svg -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
magick convert -background none -density 600 logo.svg -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

**Option B: Using Python Script** (All in one)

```python
# save as generate_icons.py
from PIL import Image
import cairosvg
import os

# Convert SVG to PNG at high resolution
def svg_to_png_sizes():
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }
    
    for density, size in sizes.items():
        # Convert SVG to PNG
        png_data = cairosvg.svg2png(
            url='logo.svg',
            output_width=size,
            output_height=size
        )
        
        # Save to res/mipmap folders
        output_dir = f'app/src/main/res/mipmap-{density}/'
        os.makedirs(output_dir, exist_ok=True)
        
        # Save launcher icon
        with open(f'{output_dir}ic_launcher.png', 'wb') as f:
            f.write(png_data)
        
        # Save round icon (same image)
        with open(f'{output_dir}ic_launcher_round.png', 'wb') as f:
            f.write(png_data)
        
        print(f'✓ Generated {density} icons ({size}x{size})')

if __name__ == '__main__':
    svg_to_png_sizes()
    print('\n✅ All icons generated!')
```

```bash
# Install dependencies
pip install pillow cairosvg

# Run script
python generate_icons.py
```

### Step 3: Rebuild App

```bash
cd C:/drift_X
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Done!** Your logo is now the app icon! 🎉

---

## 🎯 Method 2: Android Studio (EASY)

### Step 1: Convert SVG to PNG

**Online Tool** (No installation needed):

1. Go to: https://svgtopng.com/
2. Upload `logo.svg`
3. Download PNG at **512x512** size
4. Save as `logo_512.png`

### Step 2: Use Android Studio Image Asset Studio

1. **Open Android Studio**
2. **Right-click** on `app` folder
3. Select **New → Image Asset**
4. **Configure:**
    - **Icon Type:** Launcher Icons (Adaptive and Legacy)
    - **Name:** ic_launcher
    - **Asset Type:** Image
    - **Path:** Browse to your `logo_512.png`
    - **Trim:** No (your logo is already perfect)
    - **Resize:** 100%
    - **Shape:** None (keep circular for round icon)
    - **Background Layer:** Use your background color or transparent

5. **Click Next → Finish**

Android Studio automatically generates all sizes!

### Step 3: Verify

Check these folders:

```
app/src/main/res/
├── mipmap-mdpi/ic_launcher.png (48x48)
├── mipmap-hdpi/ic_launcher.png (72x72)
├── mipmap-xhdpi/ic_launcher.png (96x96)
├── mipmap-xxhdpi/ic_launcher.png (144x144)
└── mipmap-xxxhdpi/ic_launcher.png (192x192)
```

### Step 4: Rebuild

Click **Build → Rebuild Project**

**Done!** ✅

---

## 🎯 Method 3: Manual (COMPLETE CONTROL)

### Required Icon Sizes

| Density | Size | File Path |
|---------|------|-----------|
| mdpi | 48x48 | `app/src/main/res/mipmap-mdpi/ic_launcher.png` |
| hdpi | 72x72 | `app/src/main/res/mipmap-hdpi/ic_launcher.png` |
| xhdpi | 96x96 | `app/src/main/res/mipmap-xhdpi/ic_launcher.png` |
| xxhdpi | 144x144 | `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` |
| xxxhdpi | 192x192 | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` |

**Round icons:** Same sizes, replace `ic_launcher.png` with `ic_launcher_round.png`

### Step 1: Convert SVG Manually

Use any of these tools:

**A. Online Converters:**

- https://svgtopng.com/
- https://www.aconvert.com/image/svg-to-png/
- https://convertio.co/svg-png/

**B. Desktop Tools:**

- **Inkscape** (Free, excellent quality)
    1. Open logo.svg
    2. File → Export PNG Image
    3. Set size to 512x512
    4. Export

- **GIMP** (Free)
    1. Open logo.svg
    2. Image → Scale Image → 512x512
    3. File → Export As → PNG

- **Adobe Illustrator** (If you have it)
    1. Open logo.svg
    2. File → Export → Export As PNG
    3. Set size to 512x512

### Step 2: Resize to All Required Sizes

Use online batch converter or Photoshop:

1. Start with 512x512 PNG
2. Resize to each required size
3. Save as `ic_launcher.png` in correct folder

### Step 3: Copy Files

```bash
# Copy all generated PNGs to correct folders
cp logo_48.png app/src/main/res/mipmap-mdpi/ic_launcher.png
cp logo_72.png app/src/main/res/mipmap-hdpi/ic_launcher.png
cp logo_96.png app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp logo_144.png app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp logo_192.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# Copy round versions (same files)
cp logo_48.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
cp logo_72.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
cp logo_96.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
cp logo_144.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
cp logo_192.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

---

## 🎨 Adding Splash Screen (Opening Logo)

### Option 1: Simple Splash (Quick)

**Step 1: Create Splash Drawable**

Create `app/src/main/res/drawable/splash_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Background color (Galaxy Charcoal) -->
    <item android:drawable="@color/galaxy_charcoal"/>
    
    <!-- Logo -->
    <item>
        <bitmap
            android:src="@mipmap/ic_launcher"
            android:gravity="center" />
    </item>
</layer-list>
```

**Step 2: Create Splash Theme**

Add to `app/src/main/res/values/themes.xml`:

```xml
<style name="SplashTheme" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:windowBackground">@drawable/splash_background</item>
    <item name="android:statusBarColor">@color/galaxy_charcoal</item>
    <item name="android:navigationBarColor">@color/galaxy_charcoal</item>
</style>
```

**Step 3: Update AndroidManifest.xml**

```xml
<activity
    android:name=".presentation.MainActivity"
    android:exported="true"
    android:theme="@style/SplashTheme">  <!-- Add this line -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Step 4: Switch to App Theme in MainActivity**

Add to `MainActivity.kt` before `setContent`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Switch from splash theme to app theme
    setTheme(R.style.Theme_ModelDriftDetector)
    
    setContent {
        // ... existing code
    }
}
```

---

### Option 2: Animated Splash (Advanced)

Create `SplashActivity.kt`:

```kotlin
package com.driftdetector.app.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.driftdetector.app.R
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SplashScreen {
                // Navigate to main activity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )
    
    // Alpha animation
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000) // Show for 2 seconds
        onTimeout()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181B24)), // Galaxy Charcoal
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Logo",
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}
```

**Update AndroidManifest.xml:**

```xml
<!-- Splash Activity (Launcher) -->
<activity
    android:name=".presentation.SplashActivity"
    android:exported="true"
    android:theme="@style/SplashTheme">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- Main Activity (No longer launcher) -->
<activity
    android:name=".presentation.MainActivity"
    android:exported="false"
    android:theme="@style/Theme_ModelDriftDetector">
</activity>
```

---

## 🎨 Update App Name (Optional)

### Method 1: Change in strings.xml

Edit `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">DriftGuardAI</string>
</resources>
```

### Method 2: Use Logo Text from SVG

Your logo already has "DriftGuard**AI**" text!

Keep as is or customize in SVG editor before converting.

---

## ✅ Verification Checklist

After changing logo:

- [ ] All mipmap folders have new icons
- [ ] Both `ic_launcher.png` and `ic_launcher_round.png` updated
- [ ] Clean build completed
- [ ] App reinstalled on device
- [ ] New icon shows on home screen
- [ ] New icon shows in app drawer
- [ ] New icon shows in recent apps
- [ ] Splash screen shows logo (if implemented)

---

## 🛠️ Troubleshooting

### Issue: Icon not changing after rebuild

**Solution:**

```bash
# Complete clean rebuild
./gradlew clean
./gradlew assembleDebug

# Uninstall old app completely
adb uninstall com.driftdetector.app

# Install fresh
adb install app/build/outputs/apk/debug/app-debug.apk

# Force launcher refresh (some devices)
adb shell cmd package compile -m speed com.driftdetector.app
```

### Issue: Icon looks blurry

**Solution:**

- Use higher resolution source (512x512 or 1024x1024)
- Ensure SVG export is at 600+ DPI
- Don't upscale small images
- Use vector graphics when possible

### Issue: Icon has white background

**Solution:**

- Export SVG with transparent background
- Use PNG-24 with alpha channel
- Check `android:background` in adaptive icon XML

### Issue: Adaptive icon not working (Android 8+)

**Solution:**
Update `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/galaxy_charcoal"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
```

---

## 🎨 Your Logo Features

Your `logo.svg` is perfectly designed for an app icon:

✅ **Clear "DG" initials** - Recognizable  
✅ **Sci-fi tech theme** - Matches app aesthetic  
✅ **Cyan & Purple gradients** - Eye-catching  
✅ **Grid background** - Tech feel  
✅ **"DriftGuardAI" text** - Clear branding  
✅ **512x512 optimized** - Perfect for all screens  
✅ **Scalable vector** - No quality loss

---

## 🚀 Quick Start Command

**Complete automation** (if you have ImageMagick):

```bash
# Save this as generate_app_icons.sh

#!/bin/bash

echo "🎨 Generating DriftGuardAI app icons..."

# Define sizes
declare -A sizes=(
    ["mdpi"]="48"
    ["hdpi"]="72"
    ["xhdpi"]="96"
    ["xxhdpi"]="144"
    ["xxxhdpi"]="192"
)

# Generate all icon sizes
for density in "${!sizes[@]}"; do
    size=${sizes[$density]}
    dir="app/src/main/res/mipmap-$density"
    
    echo "Generating $density ($size x $size)..."
    
    magick convert -background none -density 600 logo.svg \
        -resize ${size}x${size} "$dir/ic_launcher.png"
    
    magick convert -background none -density 600 logo.svg \
        -resize ${size}x${size} "$dir/ic_launcher_round.png"
done

echo "✅ All icons generated!"
echo "Now run: ./gradlew clean assembleDebug"
```

```bash
# Run it
chmod +x generate_app_icons.sh
./generate_app_icons.sh
```

---

## 📚 Resources

**Image Conversion Tools:**

- ImageMagick: https://imagemagick.org/
- Inkscape: https://inkscape.org/
- GIMP: https://www.gimp.org/
- Online: https://svgtopng.com/

**Android Icon Guidelines:**

- https://developer.android.com/google-play/resources/icon-design-specifications
- https://developer.android.com/studio/write/image-asset-studio

---

## 🎉 You're Done!

**Your beautiful DG logo is now:**

- ✅ The app icon on home screen
- ✅ Shown in app drawer
- ✅ Displayed in recent apps
- ✅ (Optional) Animated splash screen

**The sci-fi themed logo perfectly matches your app's design!** 🚀✨

---

**Questions?** Just ask! I can help with any step. 😊
