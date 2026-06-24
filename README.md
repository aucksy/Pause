# Pause

**A minimal doom-scrolling interrupter.**

Pause watches which app is in the foreground and, after you've spent a configurable amount of
continuous time in a chosen app (Instagram, Reddit, X, Facebook, TikTok, YouTube, Snapchat), it
shows a brief, full-screen, glassmorphic overlay. The overlay exists only to break attention
momentum and let you *consciously* decide whether to keep scrolling.

It is **not** a blocker, **not** a productivity app, **not** parental control. There are no
accounts, no analytics, no logins, and nothing ever leaves the device.

> Interrupt the autopilot. Let the user choose.

---

## How it works

```
Detection (user picks one):
  • Usage Access  ──►  foreground service polls UsageStatsManager (~1×/sec)   ← default
  • Accessibility ──►  TYPE_WINDOW_STATE_CHANGED events
        │            (both report the package name only)
        ▼
  SessionController  ──►  shared timing state machine (one absolute deadline per session)
        │  reaches the chosen interval while still in the app
        ▼
  OverlayController  ──►  full-screen WindowManager overlay (TYPE_APPLICATION_OVERLAY)
        │
        ▼
  PauseOverlay (Compose)  ──►  blurred scrim · floating character · session text ·
                               Continue button armed after a 3-second countdown
```

* Leaving the monitored app — including turning the screen off or locking the device — **cancels
  and resets** the timer. Returning starts a fresh count.
* The keyboard and the status-bar shade do **not** count as leaving (they're filtered out).
* After you tap **Continue**, Pause re-arms for another interval so it keeps gently checking in.
* Detection reads **only the foreground package name** — in Accessibility mode
  `canRetrieveWindowContent` is `false`, and Usage Access exposes only package + timing — so Pause
  never sees the content of your screen.

## Architecture

Clean Architecture + MVVM, 100% Kotlin + Jetpack Compose (Material 3), Hilt for DI, DataStore for
persistence. No database and no WorkManager. Detection runs one of two ways behind a shared
`SessionController`: the default **Usage Access** mode uses a lightweight foreground service
(`foregroundServiceType="specialUse"`) that polls `UsageStatsManager` about once a second; the
optional **Accessibility** mode is purely event-driven. Either way nothing is persisted beyond the
DataStore settings and the battery cost stays low.

```
com.pause.app
├── core/                 Constants, permission helpers
├── domain/
│   ├── model/            PauseSettings, AppCatalog, IntervalOptions, MonitoringStatus
│   └── repository/       SettingsRepository (interface)
├── data/
│   └── repository/       SettingsRepositoryImpl  (Preferences DataStore)
├── di/                   Hilt modules (DataStore, scope, bindings)
├── service/              SessionController (shared engine) · UsageAccessMonitorService (default)
│                         · PauseAccessibilityService (optional)
├── overlay/              OverlayController · OverlayLifecycleOwner · PauseOverlay (Compose)
└── ui/
    ├── theme/            Color · Type · Shape · Spacing · Theme
    ├── components/       AppMonogram, PauseCard
    ├── permissions/      rememberSystemPermissions()
    ├── onboarding/       OnboardingScreen (3 steps)
    ├── home/             HomeScreen (status, large toggle, edit sheets)
    └── navigation/       PauseNavHost
```

## Permissions

| Permission | Why | Declared / requested |
|---|---|---|
| Usage Access (`PACKAGE_USAGE_STATS`) | **Default** detector: which app is foreground and for how long (package + timing only) | granted in system Settings → *Usage access* |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Run the Usage-Access detector while monitoring | declared in manifest; subtype justified via `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` |
| Notifications (`POST_NOTIFICATIONS`) | Show the ongoing foreground-service status notification on Android 13+ | requested at runtime |
| Accessibility Service | **Optional** alternative detector (package name only) | `BIND_ACCESSIBILITY_SERVICE` service + `res/xml/accessibility_service_config.xml` |
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | Draw the interruption overlay above the app | requested in onboarding |

The detector you pick and the overlay permission are guided through onboarding and re-checked every
time the app resumes.

---

## Build

The project is a standard Gradle Android app (AGP 8.7.3, Kotlin 2.0.21, JDK 17). It ships its own
Gradle wrapper, so you don't need Gradle installed.

### Option A — get an APK from GitHub (no local Android SDK needed)

This repo includes two workflows under `.github/workflows/`:

1. **`android-debug.yml` — fastest path to an installable APK.**
   Runs on every push to `main` (or manually from the **Actions** tab). Needs **no secrets**.
   Download the `pause-debug-apk` artifact, copy the `.apk` to your phone, and install
   (you may need to allow "install from unknown sources").

2. **`android-release.yml` — signed release APK + AAB.**
   Triggered by pushing a tag like `v1.0.0`, or manually. Add these repository secrets
   (**Settings → Secrets and variables → Actions**) — the same setup you already use for
   Notification Digest:

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
   | `KEYSTORE_PASSWORD` | keystore password |
   | `KEY_ALIAS` | key alias |
   | `KEY_PASSWORD` | key password |

   Without the secrets the release build still succeeds but is **unsigned**.

   Create a keystore once with:
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias pause \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

### Option B — build locally

```bash
# from the project root
./gradlew assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest     # run unit tests
./gradlew assembleRelease       # unsigned unless keystore.properties is present
```

For a signed local release, create `keystore.properties` in the project root (it's git-ignored):

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=********
keyAlias=pause
keyPassword=********
```

Requires the Android SDK (set `sdk.dir` in `local.properties`) and JDK 17.

---

## Replacing the placeholder character

The overlay currently uses a placeholder vector face at
`res/drawable/ic_pause_character.xml` (a flat-design "unimpressed" expression on a transparent
background). To use your own art:

1. Drop a transparent **PNG** named `pause_character.png` into `app/src/main/res/drawable/`.
2. In `overlay/PauseOverlay.kt`, change `R.drawable.ic_pause_character` to
   `R.drawable.pause_character`.

Because the image is rendered as a plain `Image` with a transparent background, it appears as a
floating cutout with no rectangle behind it — exactly as intended.

## Design language

Material 3, glassmorphism, soft shadows, generous radii, spring/easing animations at 60fps. A
calm, slightly humorous palette of off-white canvases, deep-ink text, and a single restrained
indigo-violet accent. No bright colors, no charts, no streaks, no gamification — the app should
never feel like it's punishing or shaming you.
