# Gradle JVM Compatibility Fix

## Problem

The project was experiencing an "Incompatible Gradle JVM" error because:

1. **System Java Version**: Your system has Java 25 installed
2. **Project Requirements**: This Android project requires Java 17 for compatibility with:
    - Android Gradle Plugin 8.2.2
    - Gradle 8.4
    - Kotlin 1.9.20
    - Android SDK build tools

## Root Cause

When running `gradlew` directly, it was using the system Java (version 25) instead of the
project-specific Java 17 installation located at `C:\drift_X\.java\jdk-17`.

## Solutions Applied

### 1. Updated Gradle Configuration

**File: `gradle.properties`**

- Changed `org.gradle.java.home` to use proper Windows path format with escaped backslashes
- Added `org.gradle.java.toolchain.version=17` for explicit toolchain configuration

**File: `gradle/wrapper/gradle-wrapper.properties`**

- Updated Gradle version from 8.2 to 8.4 for better Java 17 compatibility

**File: `build.gradle.kts`**

- Updated Android Gradle Plugin from 8.2.0 to 8.2.2 (more stable patch release)

### 2. Created Build Helper Scripts

**`build.ps1` (PowerShell)** and **`build.bat` (Command Prompt)**
These scripts automatically set `JAVA_HOME` to the correct Java 17 installation before running
Gradle commands.

Usage:

```powershell
# PowerShell
.\build.ps1 build
.\build.ps1 assembleDebug
.\build.ps1 --version

# Command Prompt
build.bat build
build.bat assembleDebug
```

### 3. Updated Documentation

**File: `README.md`**

- Added Java 17 as a prerequisite
- Added troubleshooting section for "Incompatible Gradle JVM" error
- Updated installation instructions to use build scripts
- Added Android Studio JDK configuration instructions

## Verification

Run this command to verify Java 17 is being used:

```powershell
.\build.ps1 --version
```

You should see:

```
JVM: 17.0.13 (Eclipse Adoptium 17.0.13+11)
```

## For Android Studio Users

If you're using Android Studio:

1. Open **File → Project Structure → SDK Location**
2. Set **Gradle JDK** to: `C:\drift_X\.java\jdk-17`
3. Click **Apply** and **OK**
4. Sync Gradle

Alternatively, Android Studio should automatically detect the `gradle.properties` configuration.

## Alternative: Set JAVA_HOME Manually

If you prefer not to use the build scripts:

```powershell
# PowerShell
$env:JAVA_HOME = "C:\drift_X\.java\jdk-17"
.\gradlew build

# Command Prompt
set JAVA_HOME=C:\drift_X\.java\jdk-17
gradlew.bat build
```

## Files Modified

1. ✅ `gradle.properties` - Fixed Java home path format
2. ✅ `gradle/wrapper/gradle-wrapper.properties` - Updated Gradle version
3. ✅ `build.gradle.kts` - Updated AGP version
4. ✅ `build.ps1` - Created PowerShell build script
5. ✅ `build.bat` - Created batch build script
6. ✅ `README.md` - Added troubleshooting documentation

## Status: ✅ RESOLVED

The "Incompatible Gradle JVM" error has been fixed. The project now correctly uses Java 17 for all
builds.
