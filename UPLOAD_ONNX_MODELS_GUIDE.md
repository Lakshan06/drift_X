# 📥 Upload ONNX Models from GitHub to DriftGuardAI

## Quick Guide - You Downloaded ONNX Models from GitHub!

You've downloaded models from https://github.com/onnx/models - here's exactly what to do next.

---

## ✅ Step-by-Step Instructions

### Step 1: Extract the Model Files

**If you downloaded a ZIP file:**

```bash
# Windows (PowerShell)
Expand-Archive -Path onnx-models.zip -DestinationPath ./onnx-models

# Or unzip using Windows Explorer
# Right-click → "Extract All..."
```

**Find your `.onnx` files:**

- Look for files ending in `.onnx`
- Common locations:
    - `vision/classification/mobilenet/model/mobilenetv2-7.onnx`
    - `vision/classification/resnet/model/resnet50-v2-7.onnx`
    - `vision/object_detection_segmentation/yolov3/model/yolov3-10.onnx`

### Step 2: Choose Your Model

**Popular models and their locations:**

```
📦 Downloaded Folder
├── vision/
│   ├── classification/
│   │   ├── mobilenet/model/
│   │   │   └── mobilenetv2-7.onnx          ← Good for image classification
│   │   ├── resnet/model/
│   │   │   └── resnet50-v2-7.onnx          ← Good for image classification
│   │   └── shufflenet/model/
│   │       └── shufflenet-v2-10.onnx       ← Lightweight model
│   └── object_detection_segmentation/
│       └── yolov3/model/
│           └── yolov3-10.onnx              ← Object detection
└── text/
    └── language_model/
        └── gpt2/model/
            └── gpt2-10.onnx                ← Text generation
```

**For this example, let's use MobileNetV2:**

```bash
# Navigate to the model folder
cd onnx-models/vision/classification/mobilenet/model/

# You should see: mobilenetv2-7.onnx
```

### Step 3: Generate Data File

Create a CSV file with sample data for drift detection:

**Option A: Quick Python Script**

```python
# save this as generate_data.py
import pandas as pd
import numpy as np

# MobileNetV2 expects 224x224x3 images
# For demo, we'll use a simplified feature set
num_samples = 1000
num_features = 50  # Simplified features

# Generate reference data (70%)
ref_data = np.random.randn(700, num_features)

# Generate current data (30%) with slight drift
current_data = np.random.randn(300, num_features) + 0.2

# Combine
all_data = np.vstack([ref_data, current_data])

# Save to CSV
df = pd.DataFrame(all_data, columns=[f'feature_{i}' for i in range(num_features)])
df.to_csv('mobilenet_data.csv', index=False)

print(f"✅ Generated mobilenet_data.csv with {len(df)} samples")
```

**Run it:**

```bash
python generate_data.py
```

**Option B: Use the Example Script**

```bash
# Use the script I provided earlier
python example_download_and_prepare.py --samples 1000 --features 50 --no-transfer
```

### Step 4: Transfer to Android Device

**Connect your Android device and transfer both files:**

```bash
# Check device is connected
adb devices

# Should show something like:
# List of devices attached
# ABC123456789    device

# Push model file
adb push mobilenetv2-7.onnx /sdcard/Download/

# Push data file
adb push mobilenet_data.csv /sdcard/Download/

# Verify files were transferred
adb shell ls /sdcard/Download/
```

**Expected output:**

```
mobilenetv2-7.onnx
mobilenet_data.csv
```

### Step 5: Upload in DriftGuardAI App

**Now on your Android device:**

1. **Open DriftGuardAI app**
   ```
   [Tap app icon]
   ```

2. **Navigate to Models tab**
   ```
   Bottom Navigation Bar → Tap "Models" 📊
   ```

3. **Tap Upload button**
   ```
   Top-right corner → Tap cloud icon ☁️
   ```

4. **Select Local Files**
   ```
   ┌───────────────────┬───────────────────┐
   │  📁 Local Files   │  ☁️ Cloud Storage │
   │  [TAP THIS]       │                   │
   └───────────────────┴───────────────────┘
   ```

5. **Upload the model**
   ```
   ┌─────────────────────────────────┐
   │ 🧠 Upload ML Model              │ ← Tap this
   └─────────────────────────────────┘
   
   → Browse to /sdcard/Download/
   → Select "mobilenetv2-7.onnx"
   → Wait for upload (~5 seconds)
   ```

6. **Upload the data**
   ```
   ┌─────────────────────────────────┐
   │ 📊 Upload Dataset               │ ← Tap this
   └─────────────────────────────────┘
   
   → Browse to /sdcard/Download/
   → Select "mobilenet_data.csv"
   → Wait for processing (~10 seconds)
   ```

7. **View Results!**
   ```
   ✅ Processing Complete!
   
   📊 Model: mobilenetv2-7.onnx
   📈 Data: 1000 samples
   
   Drift Status: [Will show here]
   Drift Score: [Will show here]
   
   [View Dashboard] [View Patches]
   ```

---

## 🎯 Specific Examples

### Example 1: Upload MobileNetV2 (Image Classification)

```bash
# 1. Extract and locate model
cd vision/classification/mobilenet/model/
ls -la  # Should see: mobilenetv2-7.onnx

# 2. Generate data (Python)
python << EOF
import pandas as pd
import numpy as np

data = np.random.randn(1000, 50)
df = pd.DataFrame(data, columns=[f'feature_{i}' for i in range(50)])
df.to_csv('mobilenet_data.csv', index=False)
EOF

# 3. Transfer to Android
adb push mobilenetv2-7.onnx /sdcard/Download/
adb push mobilenet_data.csv /sdcard/Download/

# 4. Upload in app (see Step 5 above)
```

### Example 2: Upload ResNet50 (Image Classification)

```bash
# 1. Navigate to ResNet folder
cd vision/classification/resnet/model/
ls -la  # Should see: resnet50-v2-7.onnx

# 2. Generate data
python << EOF
import pandas as pd
import numpy as np

# ResNet50 has more complex architecture, use more features
data = np.random.randn(1000, 100)
df = pd.DataFrame(data, columns=[f'feature_{i}' for i in range(100)])
df.to_csv('resnet_data.csv', index=False)
EOF

# 3. Transfer
adb push resnet50-v2-7.onnx /sdcard/Download/
adb push resnet_data.csv /sdcard/Download/

# 4. Upload in app
```

### Example 3: Upload YOLOv3 (Object Detection)

```bash
# 1. Navigate to YOLO folder
cd vision/object_detection_segmentation/yolov3/model/
ls -la  # Should see: yolov3-10.onnx

# 2. Generate data (detection models need different features)
python << EOF
import pandas as pd
import numpy as np

# YOLO outputs bounding boxes and class probabilities
# Simulate detection features
data = np.random.rand(1000, 80)  # 80 classes in YOLO
df = pd.DataFrame(data, columns=[f'class_{i}' for i in range(80)])
df.to_csv('yolo_data.csv', index=False)
EOF

# 3. Transfer
adb push yolov3-10.onnx /sdcard/Download/
adb push yolo_data.csv /sdcard/Download/

# 4. Upload in app
```

---

## 📋 Complete Workflow Summary

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Download from GitHub                                     │
│    → https://github.com/onnx/models                         │
│    → Download ZIP or clone repo                             │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Extract and Locate .onnx File                            │
│    → Unzip downloaded file                                  │
│    → Find model in vision/classification/mobilenet/model/   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Generate Data CSV                                        │
│    → Run Python script to generate sample data              │
│    → Or use example_download_and_prepare.py                 │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Transfer to Android                                      │
│    → adb push model.onnx /sdcard/Download/                  │
│    → adb push data.csv /sdcard/Download/                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Upload in DriftGuardAI                                   │
│    → Open app → Models → Upload                             │
│    → Local Files → Upload Model → Upload Data               │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. View Results                                             │
│    → Processing complete                                    │
│    → Drift detection results                                │
│    → Synthesized patches (if drift detected)                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🐛 Troubleshooting

### Issue: "Can't find .onnx file"

**Solution:**

```bash
# Search for all .onnx files in the extracted folder
# Windows PowerShell:
Get-ChildItem -Recurse -Filter *.onnx

# Linux/Mac:
find . -name "*.onnx"
```

### Issue: "adb: command not found"

**Solution:**

```bash
# Download Android Platform Tools
# Windows: https://dl.google.com/android/repository/platform-tools-latest-windows.zip
# Mac: https://dl.google.com/android/repository/platform-tools-latest-darwin.zip
# Linux: https://dl.google.com/android/repository/platform-tools-latest-linux.zip

# Extract and add to PATH
# Or navigate to the folder and run:
./adb devices
```

### Issue: "adb: no devices/emulators found"

**Solution:**

1. Enable USB Debugging on Android:
    - Settings → About Phone → Tap "Build Number" 7 times
    - Settings → Developer Options → Enable "USB Debugging"
2. Connect device via USB
3. Accept debugging prompt on phone
4. Run: `adb devices`

### Issue: "Model file too large"

**Solution:**

```bash
# Check file size
# Windows PowerShell:
Get-Item mobilenetv2-7.onnx | Select-Object Name, Length

# If > 100 MB, try quantized version or compress
# Look for models with "quantized" in name
```

---

## 💡 Pro Tips for ONNX Models

### 1. **Model Selection**

Start with these recommended models:

| Model | Size | Use Case | File |
|-------|------|----------|------|
| **MobileNetV2** | ~14 MB | Image classification | `mobilenetv2-7.onnx` |
| **SqueezeNet** | ~5 MB | Lightweight classification | `squeezenet1.1-7.onnx` |
| **ShuffleNet** | ~5 MB | Mobile-friendly | `shufflenet-v2-10.onnx` |

### 2. **Data Generation Tips**

```python
# Match feature count to model complexity:
# - Lightweight models (MobileNet, SqueezeNet): 20-50 features
# - Standard models (ResNet): 50-100 features
# - Large models (YOLOv3): 80+ features

# Always include some drift:
reference_data = np.random.randn(700, num_features)
drifted_data = np.random.randn(300, num_features) + 0.3  # Add offset
```

### 3. **Batch Upload Multiple Models**

```bash
# Transfer all at once
adb push mobilenetv2-7.onnx /sdcard/Download/
adb push resnet50-v2-7.onnx /sdcard/Download/
adb push squeezenet1.1-7.onnx /sdcard/Download/

# Generate data for each
python generate_mobilenet_data.py
python generate_resnet_data.py
python generate_squeezenet_data.py

# Push all data files
adb push *_data.csv /sdcard/Download/

# Upload one by one in the app
```

### 4. **Verify Model Before Upload**

```python
# Install ONNX
pip install onnx

# Check model
import onnx

model = onnx.load('mobilenetv2-7.onnx')
print("Model is valid:", onnx.checker.check_model(model))

# View model info
print("Inputs:", [input.name for input in model.graph.input])
print("Outputs:", [output.name for output in model.graph.output])
```

---

## 🎬 Video Tutorial (Steps Visualization)

```
Step 1: Extract ZIP
┌─────────────────────────────────────┐
│ onnx-models.zip                     │
│ Right-click → Extract All           │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│ 📁 onnx-models/                     │
│   📁 vision/                        │
│     📁 classification/              │
│       📁 mobilenet/                 │
│         📁 model/                   │
│           📄 mobilenetv2-7.onnx ✅  │
└─────────────────────────────────────┘

Step 2: Generate Data
┌─────────────────────────────────────┐
│ > python generate_data.py           │
│ ✅ Generated mobilenet_data.csv     │
└─────────────────────────────────────┘

Step 3: Transfer
┌─────────────────────────────────────┐
│ > adb devices                       │
│ ABC123456789    device              │
│                                     │
│ > adb push mobilenetv2-7.onnx /...│
│ mobilenetv2-7.onnx: 1 file pushed  │
│                                     │
│ > adb push mobilenet_data.csv /... │
│ mobilenet_data.csv: 1 file pushed  │
└─────────────────────────────────────┘

Step 4: Upload in App
┌─────────────────────────────────────┐
│ [Phone Screen]                      │
│                                     │
│  DriftGuardAI                       │
│  ─────────────                      │
│  Models → Upload → Local Files      │
│                                     │
│  ✅ Upload ML Model                 │
│     mobilenetv2-7.onnx              │
│                                     │
│  ✅ Upload Dataset                  │
│     mobilenet_data.csv              │
│                                     │
│  🎉 Processing Complete!            │
└─────────────────────────────────────┘
```

---

## 🚀 Next Steps After Upload

Once you've uploaded your ONNX model:

1. **View Dashboard:**
    - Navigate to Dashboard tab
    - See drift metrics visualization
    - Monitor model performance

2. **Check Patches:**
    - Go to Patches tab
    - Review synthesized patches
    - Deploy patches if needed

3. **Enable Notifications:**
    - Settings → Notifications
    - Enable drift alerts
    - Set alert thresholds

4. **Add More Models:**
    - Repeat the process with other ONNX models
    - Monitor multiple models simultaneously

---

## 📞 Need Help?

**Common Questions:**

Q: "Which ONNX model should I start with?"
A: Start with `mobilenetv2-7.onnx` - it's small, well-tested, and easy to work with.

Q: "How much data do I need?"
A: Minimum 100 rows, but 500-1000 rows is better for accurate drift detection.

Q: "Can I use real image data?"
A: Yes! Extract features from images first (using feature extractors), then create a CSV with those
features.

Q: "What if I don't have ADB?"
A: Use the URL Import method - upload your .onnx file to a cloud service (Dropbox, Google Drive),
get a direct link, and import via URL.

---

**You're all set! Happy ML monitoring! 🎉**

**Version:** 1.0.0  
**Last Updated:** January 2025
