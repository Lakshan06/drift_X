# 📊 Generate Data Files for ONNX Models

## Quick Guide - 3 Easy Ways

You have **3 ways** to generate data files for your ONNX models:

---

## Method 1: Auto-Generate by Inspecting Your Model ✅ (Recommended)

**This automatically reads your ONNX model and creates matching data!**

### Step 1: Install dependencies

```bash
pip install onnx pandas numpy
```

### Step 2: Run the generator

```bash
# Basic usage - inspects model and generates data
python generate_onnx_data.py --model mobilenetv2-7.onnx

# Custom samples and drift
python generate_onnx_data.py --model mobilenetv2-7.onnx --samples 2000 --drift 0.3
```

**Output:**

```
============================================================
   ONNX Data Generator for DriftGuardAI
============================================================

🔍 Analyzing ONNX model: mobilenetv2-7.onnx
✅ Model is valid

📊 Model Structure:
   Inputs: 1
      - input: [1, 3, 224, 224]
   Outputs: 1
      - output: [1, 1000]

📊 Generating data:
   Samples: 1000
   Features: 150 (simplified from 150528)
   Drift amount: 0.2

✅ Generated: mobilenetv2-7_data.csv
   Shape: (1000, 150)
   Size: 234.56 KB

📈 Data Statistics:
   Reference data: rows 0-699 (mean: 0.001)
   Current data: rows 700-999 (mean: 0.201)
   Drift introduced: 0.200

============================================================
✅ Success!
============================================================

Generated file: mobilenetv2-7_data.csv

Next steps:
1. Transfer to Android:
   adb push mobilenetv2-7_data.csv /sdcard/Download/
2. Upload in DriftGuardAI app:
   Models → Upload → Local Files → Upload Dataset
```

---

## Method 2: Quick Generate for Common Models ⚡ (No Model File Needed)

**Don't have the model yet? Generate data based on model type!**

### Available Model Types:

- `mobilenet` - MobileNet image classifier (50 features)
- `resnet` - ResNet image classifier (100 features)
- `yolo` - YOLO object detector (80 features)
- `squeezenet` - SqueezeNet lightweight (30 features)
- `efficientnet` - EfficientNet classifier (120 features)
- `gpt2` - GPT-2 language model (768 features)
- `bert` - BERT language model (768 features)

### Usage:

```bash
# Generate for MobileNet
python generate_onnx_data.py --model-type mobilenet --samples 1000

# Generate for ResNet
python generate_onnx_data.py --model-type resnet --samples 500

# Generate for YOLO
python generate_onnx_data.py --model-type yolo --samples 1000
```

**Example output:**

```
📊 Generating data for mobilenet model...
   Model: MobileNet image classifier
   Features: 50
   Samples: 1000
   Drift: 0.2

✅ Generated: mobilenet_data.csv
   Shape: (1000, 50)
   Size: 78.23 KB
```

---

## Method 3: Manual Generation 📝 (Most Control)

**Write your own simple Python script:**

### Basic Template:

```python
import pandas as pd
import numpy as np

# Configuration
num_samples = 1000
num_features = 50  # Match your model's input features

# Generate reference data (70%)
ref_data = np.random.randn(700, num_features)

# Generate drifted data (30%)
drifted_data = np.random.randn(300, num_features) + 0.2  # Add drift

# Combine
all_data = np.vstack([ref_data, drifted_data])

# Create DataFrame
df = pd.DataFrame(all_data, columns=[f'feature_{i}' for i in range(num_features)])

# Save to CSV
df.to_csv('my_model_data.csv', index=False)

print(f"✅ Generated: my_model_data.csv with {len(df)} samples")
```

**Run it:**

```bash
python my_generator.py
```

---

## 🎯 Complete Examples

### Example 1: MobileNetV2 from GitHub

```bash
# 1. You have: mobilenetv2-7.onnx
# 2. Generate data
python generate_onnx_data.py --model mobilenetv2-7.onnx --samples 1000

# 3. Transfer to Android
adb push mobilenetv2-7.onnx /sdcard/Download/
adb push mobilenetv2-7_data.csv /sdcard/Download/

# 4. Upload in DriftGuardAI app
```

### Example 2: ResNet50 from GitHub

```bash
# Using model type (don't need actual model file)
python generate_onnx_data.py --model-type resnet --samples 2000

# OR using actual model
python generate_onnx_data.py --model resnet50-v2-7.onnx --samples 2000

# Transfer
adb push resnet50-v2-7.onnx /sdcard/Download/
adb push resnet_data.csv /sdcard/Download/
```

### Example 3: YOLOv3 Object Detection

```bash
# YOLO has 80 output classes
python generate_onnx_data.py --model-type yolo --samples 1000

# Transfer
adb push yolov3-10.onnx /sdcard/Download/
adb push yolo_data.csv /sdcard/Download/
```

---

## 🔍 Understanding the Generated Data

### What's in the CSV file?

```csv
feature_0,feature_1,feature_2,...,feature_49
-0.234,1.456,-0.789,...,0.123      ← Reference data (rows 0-699)
0.456,-0.234,1.234,...,-0.456
...
0.034,1.456,-0.289,...,0.523       ← Drifted data (rows 700-999)
0.256,0.134,1.534,...,0.123
```

**Structure:**

- **Columns**: Features (match model input dimensions)
- **Rows 0-69% (700)**: Reference data (normal distribution)
- **Rows 70-100% (300)**: Current data (with drift added)
- **Values**: Float numbers (usually between -3 and 3)

### Why 70/30 split?

The app automatically:

1. Uses first 70% as **reference data** (baseline)
2. Uses last 30% as **current data** (to detect drift against)
3. Compares them to calculate drift score
4. Synthesizes patches if drift is significant

---

## 💡 Pro Tips

### 1. Match Feature Count to Your Model

```bash
# Check your model's input shape first
pip install onnx

python << EOF
import onnx
model = onnx.load('mobilenetv2-7.onnx')
for input in model.graph.input:
    print(f"Input: {input.name}")
    print(f"Shape: {[dim.dim_value for dim in input.type.tensor_type.shape.dim]}")
EOF

# Then generate matching data
python generate_onnx_data.py --model mobilenetv2-7.onnx
```

### 2. Adjust Drift Amount

```bash
# Low drift (may not be detected)
python generate_onnx_data.py --model-type mobilenet --drift 0.1

# Medium drift (will be detected)
python generate_onnx_data.py --model-type mobilenet --drift 0.2

# High drift (definitely detected)
python generate_onnx_data.py --model-type mobilenet --drift 0.5
```

### 3. Generate More Samples for Better Detection

```bash
# Minimum
python generate_onnx_data.py --model-type mobilenet --samples 100

# Good
python generate_onnx_data.py --model-type mobilenet --samples 1000

# Best
python generate_onnx_data.py --model-type mobilenet --samples 5000
```

### 4. Batch Generate for Multiple Models

```bash
# Generate for all models at once
for model in mobilenet resnet yolo squeezenet; do
    python generate_onnx_data.py --model-type $model --samples 1000
done

# Transfer all to Android
adb push mobilenet_data.csv /sdcard/Download/
adb push resnet_data.csv /sdcard/Download/
adb push yolo_data.csv /sdcard/Download/
adb push squeezenet_data.csv /sdcard/Download/
```

---

## 🚀 Quick Start Commands

### For MobileNetV2 (from ONNX Model Zoo):

```bash
# Generate data
python generate_onnx_data.py --model-type mobilenet --samples 1000

# Transfer both files
adb push mobilenetv2-7.onnx /sdcard/Download/
adb push mobilenet_data.csv /sdcard/Download/

# Upload in app: Models → Upload → Local Files
```

### For ResNet50:

```bash
python generate_onnx_data.py --model-type resnet --samples 1000
adb push resnet50-v2-7.onnx /sdcard/Download/
adb push resnet_data.csv /sdcard/Download/
```

### For Any Model:

```bash
# Let the script inspect your model
python generate_onnx_data.py --model your_model.onnx --samples 1000
adb push your_model.onnx /sdcard/Download/
adb push your_model_data.csv /sdcard/Download/
```

---

## 🐛 Troubleshooting

### Issue: "onnx not installed"

```bash
pip install onnx pandas numpy
```

### Issue: "Model file not found"

```bash
# Check current directory
ls -la *.onnx

# Or provide full path
python generate_onnx_data.py --model /path/to/mobilenetv2-7.onnx
```

### Issue: "Too many features"

If your model expects raw image pixels (e.g., 224×224×3 = 150,528 features), the script
automatically reduces this to a manageable size (e.g., 150 features) while maintaining drift
detection capability.

### Issue: "Not enough drift detected"

```bash
# Increase drift amount
python generate_onnx_data.py --model your_model.onnx --drift 0.5
```

---

## 📊 Realistic Data Generation

### For Image Classification Models:

```bash
# Generate realistic image features
python generate_onnx_data.py --realistic-image --samples 1000

# This creates data that mimics CNN feature distributions
```

### For Custom Distributions:

```python
import pandas as pd
import numpy as np

# Simulate image features (0 to 1 range)
ref_data = np.random.beta(2, 2, (700, 128))
drifted_data = np.random.beta(5, 2, (300, 128))

all_data = np.vstack([ref_data, drifted_data])
df = pd.DataFrame(all_data, columns=[f'feature_{i}' for i in range(128)])
df.to_csv('realistic_image_data.csv', index=False)
```

---

## 📚 Summary

**3 Ways to Generate Data:**

1. **Auto-generate from model** (Recommended)
   ```bash
   python generate_onnx_data.py --model your_model.onnx
   ```

2. **Quick generate by type** (No model needed)
   ```bash
   python generate_onnx_data.py --model-type mobilenet
   ```

3. **Manual script** (Most control)
   ```python
   # Write your own 10-line Python script
   ```

**Next Steps:**

1. Generate CSV file ✅
2. Transfer to Android: `adb push data.csv /sdcard/Download/` ✅
3. Upload in app: Models → Upload → Dataset ✅
4. View drift results! ✅

---

## 🎉 You're Ready!

Choose the method that works best for you:

- **Have the ONNX model?** → Use Method 1
- **Don't have the model yet?** → Use Method 2
- **Want custom data?** → Use Method 3

All methods produce compatible CSV files for DriftGuardAI! 🚀

---

**Version:** 1.0.0  
**Last Updated:** January 2025
