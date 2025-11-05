# 🤖 AI Assistant Knowledge Base

This document contains all the knowledge that the AI Assistant in DriftGuardAI uses to answer user
questions.

## 📱 App Features & Capabilities

### Core Features

1. **Real-Time Drift Detection**
    - PSI (Population Stability Index) calculation
    - KS (Kolmogorov-Smirnov) test
    - Feature-level drift analysis
    - Automatic drift type classification (Concept, Covariate, Prior)
    - Continuous monitoring with configurable frequency

2. **Model Upload & Management**
    - Multiple upload methods: Local files, Cloud storage (Drive/Dropbox/OneDrive), URL import
    - Supported formats: `.tflite`, `.onnx`, `.h5`, `.pb` (models), `.csv`, `.json`, `.parquet`,
      `.avro` (data)
    - Auto-detection of model metadata
    - Version tracking and management
    - Model activation/deactivation

3. **Auto-Patch System**
    - 6 patch types: Feature Clipping, Feature Reweighting, Threshold Tuning, Normalization Update,
      Outlier Removal, Model Update
    - Safety score calculation (0.0-1.0 scale)
    - One-tap application with easy rollback
    - Patch validation before application
    - History tracking

4. **Dashboard & Visualization**
    - Real-time drift scores with color coding (Green < 0.2, Yellow 0.2-0.5, Red > 0.5)
    - Model health status indicators
    - Recent drift events timeline
    - Feature-level heatmaps
    - Interactive charts and graphs
    - Trend analytics

5. **Background Monitoring**
    - WorkManager-based continuous monitoring
    - Configurable frequency (hourly, daily, weekly)
    - Push notifications for drift alerts
    - Low battery impact design
    - Runs even when app is closed

6. **AI Assistant** (This chat interface)
    - Natural language Q&A
    - Expert recommendations
    - Drift explanations
    - Best practices guidance
    - 100% offline, instant responses
    - No downloads required

7. **Privacy & Security**
    - 100% on-device processing
    - No cloud uploads unless explicitly chosen
    - Encrypted local storage (EncryptedSharedPreferences)
    - Secure file handling
    - Optional differential privacy support

8. **Settings & Customization**
    - Theme: Light/Dark/Auto mode
    - Monitoring frequency configuration
    - Alert threshold customization
    - Notification preferences
    - Privacy controls

9. **Export & Sharing**
    - Export drift reports (JSON format)
    - Export predictions (CSV format)
    - Share via email, Drive, or any installed app
    - Pull files using ADB for automation

## 🎯 How to Use the App

### Quick Start (5 Minutes)

**Step 1: Upload Model**

1. Tap "Models" tab → "+ Add Model"
2. Choose upload method (Local/Cloud/URL)
3. Select file → Auto-detection → Register

**Step 2: Upload Reference Data**

1. Select model → "Upload Reference Data"
2. Choose CSV/JSON file with training data
3. Processing happens automatically

**Step 3: Monitor Drift**

1. View Dashboard for real-time scores
2. Check color-coded health status
3. Review feature-level analysis

**Step 4: Apply Patches (If Needed)**

1. Navigate to Patches tab
2. Find RECOMMENDED patches
3. Review safety score (> 0.7 is safe)
4. Apply and monitor results

### Navigation

- **Dashboard**: Home screen with drift overview
- **Models**: Upload and manage models
- **Patches**: Review and apply patches
- **Settings**: Configure app behavior
- **AI Assistant (FAB)**: This chat interface

## 📊 Data Science Best Practices

### Drift Detection

**When to Check for Drift:**

- Production models: Check hourly
- Staging models: Check daily
- Development models: Check weekly

**Drift Thresholds:**

- PSI < 0.1: Insignificant (no action)
- PSI 0.1-0.2: Slight change (monitor)
- PSI 0.2-0.5: Moderate (investigate)
- PSI > 0.5: Significant (urgent action)

**KS Test Interpretation:**

- p-value < 0.05: Significant drift detected
- p-value ≥ 0.05: No significant drift

### Model Selection & Recommendations

**For Classification Tasks:**

- **Fraud Detection**: XGBoost, LightGBM (handles imbalanced data well)
- **Image Classification**: MobileNetV3, EfficientNet (mobile-optimized)
- **Text Classification**: DistilBERT, MobileBERT (lightweight transformers)
- **Anomaly Detection**: Isolation Forest, Autoencoder

**For Regression Tasks:**

- **Price Prediction**: Random Forest, Gradient Boosting
- **Time Series**: LSTM, GRU, Prophet
- **Demand Forecasting**: XGBoost, ARIMA

**Mobile Optimization:**

- Use TensorFlow Lite for conversion
- Quantization: Post-training quantization (8-bit) reduces size by 4x
- Pruning: Remove unnecessary connections
- Knowledge Distillation: Train smaller model from larger one

### Feature Engineering

**Best Practices:**

- Keep feature count reasonable (< 100 for mobile)
- Normalize/standardize features
- Handle missing values explicitly
- Document feature transformations
- Track feature importance

**Common Issues:**

- Data leakage: Future info in training
- Overfitting: Too many features
- Underfitting: Too few or irrelevant features
- Scale mismatch: Features on different scales

### Model Training

**Train/Val/Test Split:**

- Standard: 70/15/15 or 80/10/10
- Time series: Use chronological split
- Small data: Use k-fold cross-validation

**Hyperparameter Tuning:**

- Start with default parameters
- Use grid search for small search space
- Use random search for large search space
- Consider Bayesian optimization for complex models

**Regularization:**

- L1 (Lasso): Feature selection, sparse models
- L2 (Ridge): Handles multicollinearity
- Dropout: For neural networks (0.2-0.5)
- Early stopping: Prevent overfitting

### Model Evaluation

**Classification Metrics:**

- Accuracy: Overall correctness (use with balanced data)
- Precision: Of predicted positives, how many correct (minimize false positives)
- Recall: Of actual positives, how many found (minimize false negatives)
- F1-Score: Harmonic mean of precision and recall
- AUC-ROC: Threshold-independent performance
- Confusion Matrix: Detailed error analysis

**Regression Metrics:**

- MAE (Mean Absolute Error): Average error magnitude
- RMSE (Root Mean Square Error): Penalizes large errors
- R² (R-squared): Proportion of variance explained
- MAPE (Mean Absolute Percentage Error): Scale-independent

**When to Use Each:**

- Fraud detection: Prioritize Recall (catch all fraud)
- Spam filtering: Prioritize Precision (avoid marking legitimate as spam)
- Medical diagnosis: High Recall critical
- Recommendation systems: Focus on top-K accuracy

### Handling Imbalanced Data

**Techniques:**

1. **Resampling:**
    - Oversampling minority class (SMOTE)
    - Undersampling majority class
    - Combination approaches

2. **Algorithm-level:**
    - Class weights adjustment
    - Cost-sensitive learning
    - Ensemble methods

3. **Evaluation:**
    - Don't use accuracy alone
    - Use precision-recall curves
    - Focus on F1-score or AUC-ROC

### Drift Management Strategies

**Patches vs Retraining:**

**Use Patches When:**

- Drift is moderate (PSI 0.2-0.5)
- Limited features affected
- Quick fix needed
- Retraining not immediately feasible
- Business context hasn't fundamentally changed

**Use Retraining When:**

- High drift (PSI > 0.6)
- Multiple features drifting
- Fundamental business changes
- Patches ineffective
- 3+ months since last retrain

**Continuous Learning:**

- Online learning: Update with each new sample
- Batch learning: Periodic full retraining
- Incremental learning: Update with mini-batches
- Transfer learning: Adapt pre-trained model

### Data Quality

**Checks to Perform:**

- Missing values: Track percentage, patterns
- Outliers: Use IQR or Z-score methods
- Duplicates: Remove exact and fuzzy duplicates
- Data types: Ensure correct types
- Distributions: Check for unexpected changes

**Data Validation:**

- Schema validation: Column names, types
- Range validation: Min/max values
- Consistency checks: Relationships between features
- Freshness: Data recency

## 🔧 Patch Types Explained

### 1. Feature Clipping

**What it does:** Constrains feature values to specific ranges
**When to use:** Outliers causing drift, new data has wider range
**Safety:** High (0.7-0.9) - Conservative approach
**Trade-offs:** May lose information from legitimate extremes

### 2. Feature Reweighting

**What it does:** Adjusts importance of different features
**When to use:** Multiple features drifting at different rates
**Safety:** Medium (0.5-0.7) - Changes model behavior
**Trade-offs:** Requires thorough validation

### 3. Threshold Tuning

**What it does:** Adjusts classification decision threshold
**When to use:** Class distribution changed (prior drift)
**Safety:** High (0.7-0.9) - Very safe and reversible
**Trade-offs:** Only fixes output calibration

### 4. Normalization Update

**What it does:** Updates feature scaling parameters (mean/std)
**When to use:** Feature scales shifted, covariate drift
**Safety:** Very High (0.8-1.0) - Most conservative
**Trade-offs:** Limited impact on severe drift

### 5. Outlier Removal

**What it does:** Filters extreme values during inference
**When to use:** Data quality issues, anomalous inputs
**Safety:** Medium-High (0.6-0.8) - Generally safe
**Trade-offs:** May reject valid edge cases

### 6. Model Update

**What it does:** Fine-tunes model weights on new data
**When to use:** Persistent drift, concept changes
**Safety:** Low-Medium (0.4-0.6) - Most aggressive
**Trade-offs:** Requires validation data, can overfit

## 📈 Statistical Tests

### PSI (Population Stability Index)

**Formula:**

```
PSI = Σ (% current - % reference) × ln(% current / % reference)
```

**How it works:**

1. Bins data into groups (usually 10-20 bins)
2. Calculates percentage in each bin for reference and current data
3. Computes divergence score

**Advantages:**

- Fast computation
- Easy to interpret
- Good for continuous monitoring

**Limitations:**

- Sensitive to binning strategy
- Assumes features can be binned
- No statistical significance test

### KS Test (Kolmogorov-Smirnov)

**Formula:**

```
D = max |F_reference(x) - F_current(x)|
```

**How it works:**

1. Compares cumulative distribution functions (CDFs)
2. Finds maximum vertical distance between CDFs
3. Computes p-value for significance

**Advantages:**

- Non-parametric (no distribution assumptions)
- Provides p-value for statistical significance
- Sensitive to both location and shape changes

**Limitations:**

- Slower than PSI
- Less interpretable statistic
- Requires sufficient sample size

### Comparison

| Aspect | PSI | KS Test |
|--------|-----|---------|
| Type | Divergence measure | Statistical test |
| Output | Score (0 to ∞) | Statistic + p-value |
| Speed | Fast | Moderate |
| Use Case | Real-time monitoring | Validation |
| Interpretation | Thresholds (0.1, 0.2, 0.5) | p-value < 0.05 |

**Recommendation:** Use both! PSI for continuous monitoring and quick alerts, KS for validation and
confirmation.

## 🎯 Model Recommendations by Use Case

### Tabular Data

**Small datasets (< 10K rows):**

- Random Forest: Robust, handles mixed types
- Gradient Boosting: High accuracy
- Logistic Regression: Fast, interpretable

**Large datasets (> 100K rows):**

- XGBoost: Fast, accurate, handles missing values
- LightGBM: Even faster, lower memory
- Neural Networks: If non-linear patterns

**High-dimensional (> 100 features):**

- XGBoost with feature selection
- ElasticNet Regression: L1+L2 regularization
- Random Forest: Built-in feature importance

### Computer Vision (Mobile)

**Image Classification:**

- MobileNetV3: Best speed/accuracy trade-off
- EfficientNet-Lite: Higher accuracy, slower
- SqueezeNet: Smallest size

**Object Detection:**

- MobileNet SSD: Real-time detection
- YOLO-Lite: Fast, good accuracy
- EfficientDet-Lite: Best accuracy

**Segmentation:**

- DeepLabV3-MobileNet: Semantic segmentation
- U-Net Mobile: Medical imaging

### Natural Language Processing (Mobile)

**Text Classification:**

- DistilBERT: 6x faster than BERT, 97% performance
- MobileBERT: Optimized for mobile
- TinyBERT: Even smaller

**Named Entity Recognition:**

- DistilBERT-NER: Fast, accurate
- BiLSTM-CRF: Classical approach, interpretable

**Sentiment Analysis:**

- DistilRoBERTa: Current best for sentiment
- LSTM: Classical, works well

### Time Series

**Forecasting:**

- Prophet: Easy to use, handles seasonality
- ARIMA: Classical, works for stationary series
- LSTM/GRU: For complex patterns

**Anomaly Detection:**

- Isolation Forest: Fast, effective
- Autoencoder: Deep learning approach
- LSTM-based: For sequential anomalies

## ❓ Troubleshooting

### Common Issues

**1. App Crashes on Launch**

- Solution: Clear app data, reinstall
- Check: Logcat for FATAL EXCEPTION
- Command: `adb shell pm clear com.driftdetector.app`

**2. Model Upload Fails**

- Check: File format (.tflite recommended)
- Check: File size (< 100MB)
- Check: Sufficient storage space
- Try: Re-download model file

**3. High Drift Score Unexpectedly**

- Check: Data quality issues
- Check: Feature distributions
- Check: If seasonal pattern
- Action: Review feature-level drift

**4. Patch Application Fails**

- Check: Safety score (should be > 0.5)
- Check: Model isn't being used during application
- Try: Rollback previous patch first
- Action: Review patch configuration

**5. Background Monitoring Not Working**

- Check: Battery optimization disabled for app
- Check: Monitoring enabled in Settings
- Check: Notification permissions granted
- Action: Reinstall if persists

### Performance Optimization

**Reduce Model Size:**

- Post-training quantization (4x reduction)
- Pruning (remove 30-50% of weights)
- Knowledge distillation (train smaller model)
- Use MobileNet architectures

**Faster Inference:**

- Use GPU delegation (TFLite GPU)
- Batch predictions
- Reduce input resolution (images)
- Cache frequently used inputs

**Lower Memory Usage:**

- Unload unused models
- Use int8 quantization
- Process data in chunks
- Clear caches periodically

## 📚 Glossary

**Concept Drift:** P(Y|X) changes - relationship between inputs and outputs shifts
**Covariate Drift:** P(X) changes - input distribution shifts
**Prior Drift:** P(Y) changes - output distribution shifts
**PSI:** Population Stability Index - measures distribution divergence
**KS Test:** Kolmogorov-Smirnov test - statistical test for distribution equality
**TFLite:** TensorFlow Lite - mobile-optimized ML framework
**Quantization:** Reducing model precision (32-bit → 8-bit) to reduce size
**Feature Importance:** Measure of how much each feature contributes to predictions
**Safety Score:** Confidence level that a patch won't harm model performance
**Rollback:** Reverting a patch to restore previous model behavior

## 🔗 External Resources

**Learning:**

- TensorFlow Lite documentation: tensorflow.org/lite
- Model conversion guide: tensorflow.org/lite/convert
- Drift detection papers: Papers with Code - Drift Detection

**Tools:**

- TensorFlow Model Optimization: tensorflow.org/model_optimization
- ONNX: onnx.ai (cross-framework format)
- Netron: netron.app (visualize models)

**Community:**

- TensorFlow community: discuss.tensorflow.org
- ML on mobile: reddit.com/r/MLQuestions
- Model monitoring: reddit.com/r/MachineLearning

---

*This knowledge base is constantly being updated with new information and best practices.*
