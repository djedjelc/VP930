# ZentyApp - Build Instructions

## Compilation du projet

Pour compiler l'application ZentyApp :

```bash
cd c:\Users\JESUS\Desktop\Dev_projets\Zenty\VP930Pro
.\gradlew :ZentyApp:clean
.\gradlew :ZentyApp:assembleDebug
```

L'APK sera générée dans :
```
ZentyApp\build\outputs\apk\debug\ZentyApp-debug.apk
```

## Installation sur le terminal VP930Pro

```bash
adb install ZentyApp\build\outputs\apk\debug\ZentyApp-debug.apk
```

Ou via Android Studio :
1. Ouvrir le projet VP930Pro dans Android Studio
2. Sélectionner le module "ZentyApp"
3. Cliquer sur Run

## Structure du projet

```
ZentyApp/
├── build.gradle                    # Configuration Gradle
├── src/main/
│   ├── AndroidManifest.xml         # Manifest avec permissions
│   ├── assets/models/              # Modèles d'algorithme
│   ├── java/com/zenty/tpe/
│   │   ├── ZentyApp.java           # Application class
│   │   ├── api/
│   │   │   └── ApiClient.java      # Client backend
│   │   ├── activity/
│   │   │   ├── MainActivity.java    # Menu principal
│   │   │   ├── PalmActivity.java    # Capture/Enrollment
│   │   │   └── PaymentActivity.java # Saisie montant
│   │   ├── palm/
│   │   │   └── PalmDeviceManager.java # Gestionnaire device
│   │   └── utils/
│   │       ├── QRCodeGenerator.java
│   │       └── FileUtils.java       # Copie modèles
│   └── res/
│       ├── layout/                  # Layouts XML
│       ├── values/strings.xml       # Labels français
│       └── mipmap/                  # Icônes app
└── libs/                            # JARs du SDK
```

## Configuration requise

- Android SDK 26+
- Terminal VP930Pro connecté en USB
- Fichiers modèles dans `src/main/assets/models/`
- Backend Zenty accessible (https://zenty-ndjq.onrender.com)

## Troubleshooting

### Erreur "Device not found"
- Vérifier que le terminal VP930Pro est bien connecté en USB
- Vérifier dans les logs que `Device.create()` a été appelé

### Erreur "Algorithm enable failed"
- Vérifier que les fichiers modèles sont bien copiés dans `/sdcard/ZentyModels/`
- Vérifier les permissions STORAGE

### Erreur réseau
- Vérifier que le backend est accessible
- Vérifier les headers `X-TPE-ID` et `X-TPE-API-KEY`
