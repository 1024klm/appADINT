# ADINT Diagnostic

Application Android de diagnostic de surface d'exposition publicitaire (ADINT - Advertising Intelligence).

## Objectif

Outil pédagogique permettant de visualiser les signaux observables utilisés pour le tracking publicitaire sur Android :

- **Score d'exposition** : heuristique basée sur des signaux mesurables
- **Signaux détectés** : LAT, GAID, permissions des applications
- **Actions de durcissement** : raccourcis vers les paramètres système
- **Identifiants** : affichage des IDs avec option de copie

## Fonctionnalités

| Section | Description |
|---------|-------------|
| Score heuristique | Note de 0 à 10 avec code couleur (vert/orange/rouge) |
| Signaux détectés | Liste des alertes avec niveau de sévérité |
| Actions possibles | Boutons vers les paramètres système (ID pub, localisation, DNS...) |
| Identifiants | ANDROID_ID, GAID (masqués, copiables) |
| Limites | Disclaimer sur ce que l'app ne détecte pas |

## Installation

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- Android SDK 36
- JDK 11+

### Build
```bash
cd id_editor/AndroidStudioProjects/ID_EDITOR
chmod +x gradlew
./gradlew assembleDebug
```

L'APK sera généré dans `app/build/outputs/apk/debug/`.

### Installation sur device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Calcul du score

| Signal | Points | API utilisée |
|--------|--------|--------------|
| LAT non activé | +3 | `AdvertisingIdClient.isLimitAdTrackingEnabled()` |
| GAID accessible | +2 | `AdvertisingIdClient.getId()` |
| Apps loc+internet | +1/app (max 3) | `PackageManager.getInstalledPackages()` |
| Apps >10 perms | +1/app (max 2) | `PackageInfo.requestedPermissions` |

**Interprétation :**
- **0-2** : Faible (vert)
- **3-5** : Moyen (orange)
- **6-10** : Élevé (rouge)

## Limites techniques

> **IMPORTANT** : Ce diagnostic est une **heuristique** basée sur des signaux observables.

Ce que l'application **NE détecte PAS** :
- Les SDK publicitaires intégrés aux applications
- Le trafic réseau réel et les destinations
- Le fingerprinting (canvas, audio, WebGL, etc.)
- L'usage réel des permissions par les apps
- Les trackers côté serveur

Le flag LAT (Limit Ad Tracking) est **déclaratif** : il indique une préférence utilisateur, pas une garantie technique.

## Permissions

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

Cette permission est requise pour scanner les applications sur Android 11+.

**Note** : Non compatible Play Store sans justification. Prototype destiné au sideload uniquement.

## Architecture

```
app/src/main/java/lan/sit/id_editor/
├── MainActivity.kt      # UI principale, gestion du scan
├── AdintScanner.kt      # Logique de scan et scoring
└── UIComponents.kt      # Composants UI (cards, boutons, barres)
```

### Fichiers clés

| Fichier | Rôle |
|---------|------|
| `AdintScanner.kt` | Récupère GAID/LAT, scanne les apps, calcule le score |
| `UIComponents.kt` | Factory de composants UI (score card, problèmes, actions) |
| `MainActivity.kt` | Orchestre le scan async, affiche les résultats |

## Stack technique

- **Langage** : Kotlin 2.0.21
- **Target SDK** : 36 (Android 15)
- **Min SDK** : 24 (Android 7.0)
- **UI** : Views programmatiques (pas de XML layouts)
- **Threading** : Thread + runOnUiThread (avec AtomicBoolean)

### Dépendances
```kotlin
implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")
implementation("androidx.core:core-ktx")
implementation("androidx.appcompat:appcompat")
implementation("com.google.android.material:material")
```

## Contexte

Projet développé dans le cadre d'une étude sur l'**ADINT** (Advertising Intelligence) - l'exploitation des identifiants publicitaires mobiles à des fins de surveillance ou de profilage.

L'objectif est de sensibiliser les utilisateurs aux données exposées par leur appareil Android et de fournir des raccourcis vers les actions de durcissement disponibles.

## Avertissement

Ce prototype est destiné à un usage **éducatif et de recherche**. Il n'offre aucune protection active contre le tracking. Les scores et signaux affichés sont des indicateurs heuristiques, pas des mesures absolues.

## Licence

Projet académique - Usage éducatif uniquement.
