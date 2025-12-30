# ADINT Diagnostic

Android application for advertising exposure surface diagnosis (ADINT - Advertising Intelligence).

## Purpose

Educational tool to visualize observable signals used for advertising tracking on Android:

- **Exposure score**: heuristic based on measurable signals
- **Detected signals**: LAT, GAID, app permissions
- **Hardening actions**: shortcuts to system settings
- **Identifiers**: ID display with copy option

## Features

### 1. Exposure Score (heuristic)
- Score from **0 to 10** with color coding (green/orange/red)
- Visual progress bar
- Score breakdown displayed (composition details)

### 2. Detected Signals
| Signal | Points |
|--------|--------|
| LAT (Limit Ad Tracking) not enabled | +3 |
| GAID accessible | +2 |
| Apps with location + internet | +1/app (max 3) |
| Apps with >10 permissions | +1/app (max 2) |

### 3. Available Actions
Buttons to system settings:
- Manage advertising ID
- App permissions
- Location settings
- Private DNS (Android 9+)

### 4. Identifiers
- **Android Device ID** — masked by default, Copy button
- **Google Advertising ID** — masked by default, Copy + Manage buttons
- **UUID/Firebase** — info only (not accessible without root)

### 5. Technical Limitations
Disclaimer card explaining what the app **does not detect** (ad SDKs, network traffic, fingerprinting, etc.)

### 6. UX
- Loading screen during scan
- "Rescan" button
- Protection against simultaneous scans

**In summary**: A privacy dashboard with score + shortcuts to system settings. No automatic modification, just diagnosis and guidance.

## Score Calculation

| Signal | Points | API Used |
|--------|--------|----------|
| LAT not enabled | +3 | `AdvertisingIdClient.isLimitAdTrackingEnabled()` |
| GAID accessible | +2 | `AdvertisingIdClient.getId()` |
| Apps loc+internet | +1/app (max 3) | `PackageManager.getInstalledPackages()` |
| Apps >10 perms | +1/app (max 2) | `PackageInfo.requestedPermissions` |

**Interpretation:**
- **0-2**: Low (green)
- **3-5**: Medium (orange)
- **6-10**: High (red)

## Technical Limitations

> **IMPORTANT**: This diagnostic is a **heuristic** based on observable signals.

What the application **DOES NOT detect**:
- Advertising SDKs embedded in apps
- Actual network traffic and destinations
- Fingerprinting (canvas, audio, WebGL, etc.)
- Actual permission usage by apps
- Server-side trackers

The LAT (Limit Ad Tracking) flag is **declarative**: it indicates a user preference, not a technical guarantee.

## Permissions

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

This permission is required to scan applications on Android 11+.

**Note**: Not Play Store compatible without justification. Prototype intended for sideload only.

## Architecture

```
app/src/main/java/lan/sit/id_editor/
├── MainActivity.kt      # Main UI, scan management
├── AdintScanner.kt      # Scan logic and scoring
└── UIComponents.kt      # UI components (cards, buttons, bars)
```

### Key Files

| File | Role |
|------|------|
| `AdintScanner.kt` | Retrieves GAID/LAT, scans apps, calculates score |
| `UIComponents.kt` | UI component factory (score card, problems, actions) |
| `MainActivity.kt` | Orchestrates async scan, displays results |

## Tech Stack

- **Language**: Kotlin 2.0.21
- **Target SDK**: 36 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **UI**: Programmatic Views (no XML layouts)
- **Threading**: Thread + runOnUiThread (with AtomicBoolean)

### Dependencies
```kotlin
implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")
implementation("androidx.core:core-ktx")
implementation("androidx.appcompat:appcompat")
implementation("com.google.android.material:material")
```

## Context

Project developed as part of a study on **ADINT** (Advertising Intelligence) - the exploitation of mobile advertising identifiers for surveillance or profiling purposes.

The goal is to raise user awareness about data exposed by their Android device and provide shortcuts to available hardening actions.

## Disclaimer

This prototype is intended for **educational and research use**. It does not provide active protection against tracking. The displayed scores and signals are heuristic indicators, not absolute measurements.

## License

Academic project - Educational use only.
