# ADINT Diagnostic

**[English](#english) | [Français](#français)**

---

## English

Android application for advertising exposure surface diagnosis (ADINT - Advertising Intelligence).

### Purpose

Educational tool to visualize observable signals used for advertising tracking on Android:

- **Exposure score**: heuristic based on measurable signals
- **Detected signals**: LAT, GAID, app permissions
- **Hardening actions**: shortcuts to system settings
- **Identifiers**: ID display with copy option

### Features

#### 1. Exposure Score (heuristic)
- Score from **0 to 10** with color coding (green/orange/red)
- Visual progress bar
- Score breakdown displayed

#### 2. Detected Signals
| Signal | Points |
|--------|--------|
| LAT (Limit Ad Tracking) not enabled | +3 |
| GAID accessible | +2 |
| Apps with location + internet | +1/app (max 3) |
| Apps with >10 permissions | +1/app (max 2) |

#### 3. Available Actions
Buttons to system settings:
- Manage advertising ID
- App permissions
- Location settings
- Private DNS (Android 9+)

#### 4. Identifiers
- **Android Device ID** — masked by default, Copy button
- **Google Advertising ID** — masked by default, Copy + Manage buttons
- **UUID/Firebase** — info only (not accessible without root)

#### 5. Technical Limitations
Disclaimer card explaining what the app **does not detect** (ad SDKs, network traffic, fingerprinting, etc.)

#### 6. UX
- Loading screen during scan
- "Rescan" button
- Protection against simultaneous scans

### Score Calculation

| Signal | Points | API Used |
|--------|--------|----------|
| LAT not enabled | +3 | `AdvertisingIdClient.isLimitAdTrackingEnabled()` |
| GAID accessible | +2 | `AdvertisingIdClient.getId()` |
| Apps loc+internet | +1/app (max 3) | `PackageManager.getInstalledPackages()` |
| Apps >10 perms | +1/app (max 2) | `PackageInfo.requestedPermissions` |

**Interpretation:** 0-2 Low (green) | 3-5 Medium (orange) | 6-10 High (red)

### Technical Limitations

> **IMPORTANT**: This diagnostic is a **heuristic** based on observable signals.

What the application **DOES NOT detect**:
- Advertising SDKs embedded in apps
- Actual network traffic and destinations
- Fingerprinting (canvas, audio, WebGL, etc.)
- Actual permission usage by apps
- Server-side trackers

The LAT flag is **declarative**: it indicates a user preference, not a technical guarantee.

### Architecture

```
app/src/main/java/lan/sit/id_editor/
├── MainActivity.kt      # Main UI, scan management
├── AdintScanner.kt      # Scan logic and scoring
└── UIComponents.kt      # UI components (cards, buttons, bars)
```

### Tech Stack

- **Language**: Kotlin 2.0.21
- **Target SDK**: 36 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **UI**: Programmatic Views (no XML layouts)

### Disclaimer

This prototype is intended for **educational and research use**. It does not provide active protection against tracking.

---

## Français

Application Android de diagnostic de surface d'exposition publicitaire (ADINT - Advertising Intelligence).

### Objectif

Outil pédagogique permettant de visualiser les signaux observables utilisés pour le tracking publicitaire sur Android :

- **Score d'exposition** : heuristique basée sur des signaux mesurables
- **Signaux détectés** : LAT, GAID, permissions des applications
- **Actions de durcissement** : raccourcis vers les paramètres système
- **Identifiants** : affichage des IDs avec option de copie

### Fonctionnalités

#### 1. Score d'exposition (heuristique)
- Note de **0 à 10** avec code couleur (vert/orange/rouge)
- Barre de progression visuelle
- Détail du calcul affiché

#### 2. Signaux détectés
| Signal | Points |
|--------|--------|
| LAT (Limit Ad Tracking) non activé | +3 |
| GAID accessible | +2 |
| Apps avec localisation + internet | +1/app (max 3) |
| Apps avec >10 permissions | +1/app (max 2) |

#### 3. Actions possibles
Boutons vers les paramètres système :
- Gérer l'ID publicitaire
- Permissions des applications
- Paramètres de localisation
- DNS privé (Android 9+)

#### 4. Identifiants
- **Android Device ID** — masqué par défaut, bouton Copier
- **Google Advertising ID** — masqué par défaut, boutons Copier + Gérer
- **UUID/Firebase** — information seulement (non accessible sans root)

#### 5. Limites techniques
Carte disclaimer expliquant ce que l'app **ne détecte pas** (SDK pub, trafic réseau, fingerprinting, etc.)

#### 6. UX
- Écran de chargement pendant le scan
- Bouton "Relancer l'analyse"
- Protection contre les scans multiples simultanés

### Calcul du score

| Signal | Points | API utilisée |
|--------|--------|--------------|
| LAT non activé | +3 | `AdvertisingIdClient.isLimitAdTrackingEnabled()` |
| GAID accessible | +2 | `AdvertisingIdClient.getId()` |
| Apps loc+internet | +1/app (max 3) | `PackageManager.getInstalledPackages()` |
| Apps >10 perms | +1/app (max 2) | `PackageInfo.requestedPermissions` |

**Interprétation :** 0-2 Faible (vert) | 3-5 Moyen (orange) | 6-10 Élevé (rouge)

### Limites techniques

> **IMPORTANT** : Ce diagnostic est une **heuristique** basée sur des signaux observables.

Ce que l'application **NE détecte PAS** :
- Les SDK publicitaires intégrés aux applications
- Le trafic réseau réel et les destinations
- Le fingerprinting (canvas, audio, WebGL, etc.)
- L'usage réel des permissions par les apps
- Les trackers côté serveur

Le flag LAT est **déclaratif** : il indique une préférence utilisateur, pas une garantie technique.

### Architecture

```
app/src/main/java/lan/sit/id_editor/
├── MainActivity.kt      # UI principale, gestion du scan
├── AdintScanner.kt      # Logique de scan et scoring
└── UIComponents.kt      # Composants UI (cards, boutons, barres)
```

### Stack technique

- **Langage** : Kotlin 2.0.21
- **Target SDK** : 36 (Android 15)
- **Min SDK** : 24 (Android 7.0)
- **UI** : Views programmatiques (pas de XML layouts)

### Avertissement

Ce prototype est destiné à un usage **éducatif et de recherche**. Il n'offre aucune protection active contre le tracking.

---

**License / Licence** : Academic project - Educational use only / Projet académique - Usage éducatif uniquement.
