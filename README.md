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
Launcher, glass + refraction, Island, Quick Space (system-wide), Notification reply, Security, Boot, Overlay service.

### Phase 2 — Complete
Media/call/download sensing, Control Center glass UI, full Setup Wizard (hello → finish), system-wide Island + Quick Space.

### Phase 3 — Complete
| Feature | Status |
|---------|--------|
| Safe PIN persistence | SHA-256 + salt, never plaintext; set on first lock |
| Session unlock cache | In-memory only; cleared on screen-off |
| Face + Fingerprint icons | Bottom of lock screen; tap → biometric prompt |
| App icon | Adaptive + legacy vector icon |
| Multitasking | Split + freeform via public ActivityOptions bounds |
| Performance engine | Scales blur/animation by real memory class |
| Signing | CI uses AETHER_KEYSTORE_* env/secrets |

## Build & sign

CI (`.github/workflows/build-apk.yml`) expects:

```
AETHER_KEYSTORE_BASE64
AETHER_KEYSTORE_PASSWORD
AETHER_KEY_ALIAS
AETHER_KEY_PASSWORD
```

Locally:

```bash
export AETHER_KEYSTORE_PATH=aether-release.keystore
export AETHER_KEYSTORE_PASSWORD=...
export AETHER_KEY_ALIAS=...
export AETHER_KEY_PASSWORD=...
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

## Permissions for full experience
1. Display over other apps
2. Notification access
3. Default Home (recommended)
4. Accessibility (App Lock)
5. Biometric / device credential
