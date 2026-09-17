# Aether Launcher

**Android launcher + system interaction layer.**

```
ANDROID → SYSTEM BRIDGES → AETHER ENGINES → STATE + EVENTS → PHYSICS + MATERIAL → UI
```

**Rule:** Real Android APIs. Real state. No simulated / demo behavior.

## Phase status

| Phase | Status |
|-------|--------|
| 1 — Engines, glass, Island, Quick Space system-wide | Complete |
| 2 — Media/call sensing, Control Center, Setup Wizard | Complete |
| 3 — PIN hash, biometrics, icon, multitasking, performance | Complete |
| 4 — MediaSession bridge, privacy redaction, diagnostics | Complete |

## Build

CI uses secrets: `AETHER_KEYSTORE_BASE64`, `AETHER_KEYSTORE_PASSWORD`, `AETHER_KEY_ALIAS`, `AETHER_KEY_PASSWORD`.

```bash
./gradlew assembleRelease
```

## Permissions
1. Display over other apps
2. Notification access
3. Default Home (recommended)
4. Accessibility (App Lock)
5. Biometric / device credential
