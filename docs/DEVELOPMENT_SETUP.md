# Development Setup

Everything below was executed on the machine that built this project, so the commands are known to work as written. Adjust paths for your machine.

## 1. Prerequisites

| Tool | Version used | Notes |
|---|---|---|
| JDK | **Temurin 17.0.20.1** | AGP 8.7.3 requires JDK 17+. A JDK 21 also works. |
| Android SDK | Platform **android-36**, Build-Tools **36.0.0** | `platform-tools` gives you `adb`. |
| Gradle | **8.14.3** | The wrapper is committed — you do not need Gradle installed. |
| Git | 2.53+ | Any recent version. |
| Python (optional) | 3.11+ | Only for the benchmark scripts in `benchmarks/`. |

**Android Studio is not required** to build, test, or produce an APK from the command line.

### Install the Android SDK without administrator rights

Everything can live under your user profile. No admin rights, no system-wide changes:

```powershell
# 1. JDK (Temurin 17, zip - no installer)
$jdk = "$env:LOCALAPPDATA\Programs\disha-jdk17"
New-Item -ItemType Directory -Force -Path $jdk | Out-Null
curl.exe -L -o "$env:TEMP\jdk17.zip" `
  "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
Expand-Archive -Force "$env:TEMP\jdk17.zip" "$env:TEMP\jdkx"
$inner = (Get-ChildItem "$env:TEMP\jdkx" -Directory)[0].FullName
Move-Item $inner $jdk

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

# 2. Android command-line tools
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools" | Out-Null
curl.exe -L -o "$env:TEMP\clt.zip" `
  "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
Expand-Archive -Force "$env:TEMP\clt.zip" "$env:TEMP\cltx"
Move-Item "$env:TEMP\cltx\cmdline-tools" "$sdk\cmdline-tools\latest"

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

# 3. SDK packages (accepting licences non-interactively)
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="$sdk" --licenses
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="$sdk" `
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

> **Note on `curl` in Git Bash / MSYS:** use Windows-style `C:/...` paths for `curl -o`.
> MSYS paths such as `/c/Users/...` are silently mangled into `C:\c\Users\...` by
> native `curl.exe` and the download fails with `client returned ERROR on write`.

### Verify the environment

```powershell
.\scripts\check-environment.ps1
```

This reports Git, Java, ADB, `gh`, Node, the Android SDK platform and build-tools,
and lists attached devices. It exits non-zero if a prerequisite is missing.

## 2. Build and test

```powershell
# Pure domain tests (no Android SDK needed)
.\gradlew.bat :core:test

# App unit tests (needs the Android SDK)
.\gradlew.bat :android:app:testDebugUnitTest

# Everything
.\gradlew.bat test

# Debug APK -> android/app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :android:app:assembleDebug

# Lint
.\gradlew.bat :android:app:lintDebug
```

macOS / Linux / WSL equivalents: `./gradlew :core:test`, etc.

## 3. Run on a device

```powershell
adb devices                      # confirm the handset is visible
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n org.dishavoice.app/.MainActivity
```

Grant the camera permission when prompted. The app speaks Hindi when the device
has a Hindi voice installed and otherwise says so and falls back to English.

To check which voices the device actually has:

```powershell
adb shell pm list packages | findstr tts
```

## 4. Benchmarks (optional, CPU)

```powershell
python -m venv .venv
.\.venv\Scripts\pip install ai-edge-litert pillow numpy

cd benchmarks\scripts
..\..\.venv\Scripts\python.exe inspect_model.py `
  ..\..\android\app\src\main\assets\efficientdet_lite0_detection.tflite

..\..\.venv\Scripts\python.exe run_benchmark.py `
  --model ..\..\android\app\src\main\assets\efficientdet_lite0_detection.tflite `
  --images ..\testdata --repeats 7 --out ..\results\detector_benchmark.json
```

These measure the model on **your** CPU, not on a phone. Phone numbers must be
collected from the running app on real hardware.

## 5. Local website preview

```powershell
py -m http.server 5181 --bind 127.0.0.1
```

Then open <http://127.0.0.1:5181/website/>. The site is plain HTML/CSS with no
build step and makes no external requests.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `ERROR: JAVA_HOME is not set` | JDK not exported | Set `JAVA_HOME` and prepend `$JAVA_HOME\bin` to `PATH` |
| `SDK location not found` | `ANDROID_HOME` unset | Export it, or add `sdk.dir=` to `local.properties` |
| `Could not find androidx.compose.material3:material3` | version catalog missing a version | every entry in `gradle/libs.versions.toml` needs an explicit version |
| `android.useAndroidX property is not enabled` | `gradle.properties` missing | it is committed; do not delete it |
| `Failed to load model` in logcat | model asset missing or renamed | the asset must be at `android/app/src/main/assets/efficientdet_lite0_detection.tflite` |
| Model reports the wrong class | label offset not applied | `model_index + 1 == coco_id`; see `CocoLabels.kt` and `benchmarks/model/coco_labels.py` |
