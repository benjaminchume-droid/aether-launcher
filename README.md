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

## Phase 1 Status (in progress)

| Engine | Status |
|--------|--------|
| Launcher Engine | Real PackageManager discovery + launch |
| Layout / Dock / Folder | Working with persistence |
| Glass / Material | RenderEffect blur (API 31+) + painted fallback |
| Physics / Animation | SpringAnimation helpers |
| Island / Activity | Real notification + media + call + recording sensing |
| Notification Engine | Listener + RemoteInput reply bridge |
| Security / App Lock | Biometric + glass passcode surface |
| Overlay | Island + edge + security overlays |
| Persistence | Settings, layout, folders, dock, protected apps |
| Boot Supervisor | BootReceiver + health start |

## Visual references
Island capsules, Quick Reply, Music controls, Call alerts, Control Center glass, Hello setup, glass passcode, and home grids are being matched to the provided reference set.

## Build
```bash
./gradlew assembleRelease
```
Requires the stable release keystore secrets for CI.
