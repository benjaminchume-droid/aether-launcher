# Aether Launcher

**Android launcher + system interaction layer.**

```
ANDROID
  ↓
SYSTEM BRIDGES
  ↓
AETHER ENGINES
  ↓
STATE + EVENTS
  ↓
PHYSICS + MATERIAL
  ↓
UI
```

**Rule:** Real Android APIs. Real state. No simulated / demo behavior.

## Phase Status

### Phase 1 — Complete
| Engine / System | Status |
|-----------------|--------|
| Launcher Engine | Real PackageManager discovery + launch |
| Layout / Dock / Folder | Working with persistence |
| Glass / Material | RenderEffect blur (API 31+) + refraction painter |
| Physics / Animation | SpringAnimation helpers |
| Island / Activity | System-wide overlay, cutout-aware |
| Notification Engine | Listener + RemoteInput reply bridge |
| Security / App Lock | Biometric + glass passcode surface |
| Overlay Service | Island + Quick Space over every app |
| Persistence | Settings, layout, folders, dock, protected apps |
| Boot Supervisor | BootReceiver + health start |

### Phase 2 — Complete
| Feature | Status |
|---------|--------|
| Media / Call / Download sensing | Aggressive classification from NotificationListener |
| Island activity types | MEDIA, CALL, DOWNLOAD, RECORDING, TIMER, NOTIFICATION, CHARGING, SYSTEM |
| Quick Reply | Real RemoteInput, stays until notification changes |
| Control Center | Glass cards, media, Wi-Fi/BT, circular toggles, brightness/volume sliders |
| Setup Wizard | Hello → Home mode → Grid → Island/Quick → Permissions → Finish |
| Refraction glass | Shared GlassPainter on Home, Dock, Island, Control, Setup |
| System-wide Island + Quick Space | OverlayService sticky, started from Activity + Boot |

## Build
```bash
./gradlew assembleRelease
```
Requires the stable release keystore secrets for CI.

## Permissions required for full experience
1. Display over other apps (Island + Quick Space)
2. Notification access (live activities + reply)
3. Default Home (optional but recommended)
4. Accessibility (App Lock)
