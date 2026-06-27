# Publishing Pause to Google Play

Everything needed to get **Pause** (`com.pauseapp.app`) onto the Play Store and to ship every
future update automatically from a `git tag`. Written for a **personal** developer account
created **after 13 Nov 2023**, which means a **closed test with ≥12 testers for ≥14 continuous
days** is required before you can apply for production access.

---

## 0. The one-time big picture

```
 you: git tag v1.0.x  ─┐
                       ├─► GitHub Actions builds a SIGNED .aab
                       │     • uploads it to the Play "Internal testing" track
                       │     • attaches the .apk/.aab to a GitHub Release
                       └─► you promote Internal → Closed → Production in Play Console
```

Local builds are never used (machine can't build Android) — **everything is cloud**.
You only ever touch the Play Console for store metadata and to promote tracks.

---

## 1. Create the app in Play Console

1. Go to <https://play.google.com/console> → **Create app**.
2. App name: **Pause**. Default language: **English (US)**. Type: **App**. Free.
3. Tick the developer-program and US-export declarations → **Create app**.

App package name is fixed by the build: **`com.pauseapp.app`** (you don't type it here — it's set
when the first AAB is uploaded).

---

## 2. Turn on Play App Signing (automatic)

Play App Signing is **on by default** for new apps. Google holds the *app signing key*; the
keystore in this repo's CI is the **upload key**. Nothing to do here except be aware:

- The **upload key** = `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` secrets already in GitHub.
- The **app signing SHA-1** (the one users' devices see) is shown later under
  **Test and release → Setup → App signing**. (Pause has no Google sign-in, so unlike the other
  apps you do **not** need to register this SHA-1 anywhere — there's no OAuth client to break.)

---

## 3. Store listing (Main store listing)

| Field | Value |
|---|---|
| App name | Pause |
| Short description | A gentle nudge to stop doom-scrolling. Pick your apps, set a timer, get a full-screen reminder. |
| Full description | (below) |
| App category | **Health & Fitness** (Digital Wellbeing) — alt: Productivity |
| Email | simpleapps108@gmail.com |

**Full description (draft):**

> Pause helps you take back control of your time. Choose the apps that tend to swallow your
> evenings — Instagram, TikTok, YouTube, Reddit, and more — set how many minutes is "too long,"
> and Pause shows a calm, full-screen reminder when you cross it. A simple beat to ask: *do I
> really want to keep going?*
>
> • Pick exactly which apps to watch — nothing else is touched.
> • Set your own time limit, including a custom number of minutes.
> • Personalise the reminder with your own image and message.
> • Runs quietly in the background — no nagging notification to dismiss.
>
> Pause is private by design. It reads only which app is open and for how long — never your
> screen, messages, or anything you type — and **nothing ever leaves your phone**. No account,
> no ads, no tracking.

**Graphics required:** app icon (512×512), feature graphic (1024×500), at least 2 phone
screenshots (the `Marketing/` folder has assets to source these from).

---

## 4. Privacy policy URL  ← do this once

The policy lives in the repo at `docs/privacy.html`. Publish it free via GitHub Pages:

1. GitHub → repo **aucksy/Pause** → **Settings → Pages**.
2. **Build and deployment → Source: Deploy from a branch**.
3. Branch: **main**, folder: **/docs** → **Save**.
4. After ~1 min the policy is live at:
   **<https://aucksy.github.io/Pause/privacy.html>**
5. Paste that URL into Play Console → **Policy → App content → Privacy policy**.

---

## 5. Data safety form (App content → Data safety)

Pause does all detection **on-device** and transmits nothing, so:

- **Does your app collect or share any of the required user data types?** → **No.**
  (Under Play's definitions, "collection" means data is *sent off the device*. Pause sends
  nothing, so on-device-only access is **not** collection.)
- Data encrypted in transit → N/A. Users can request deletion → N/A.
- This matches `docs/privacy.html` exactly — keep the two consistent if you ever change them.

---

## 6. Permission & feature declarations (App content)

| Item | What to declare | Notes |
|---|---|---|
| **Foreground service (special use)** | Justify: *"Detects the foreground app via Usage Access to interrupt long scrolling sessions; no data leaves the device."* | Android 14 requires this; Play asks for the justification at submission. Mirrors the manifest `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. |
| **Usage Access (`PACKAGE_USAGE_STATS`)** | No separate Play "sensitive permission" form. The user grants it in system Settings. Make sure the listing clearly states the digital-wellbeing purpose (it does). | Appropriate-use permission for a screen-time app. |
| **Display over other apps (`SYSTEM_ALERT_WINDOW`)** | No form; used only to draw the reminder. | — |

Pause is **Usage-Access-only** — there is **no AccessibilityService**, so none of Play's
accessibility declarations or reviews apply. It also does **not** request notification permission,
so on Android 13+ the foreground service runs with no visible notification.

### 6a. No accessibility declaration needed

As of v1.0.9 Pause uses **only** Usage Access — the AccessibilityService was removed because an
enabled accessibility service makes some finance/banking apps refuse to run. So there is **nothing
to declare for accessibility** and no manual accessibility review to clear. The in-app
`DetectionSetup` explainer still provides Prominent Disclosure for Usage Access (plain-language,
shown before the grant), which keeps the listing and the data-safety form consistent.

---

## 7. Content rating (App content → Content rating)

Fill the IARC questionnaire: category **Utility / Productivity**, no violence/sexual/etc.
content → comes out **Everyone / PEGI 3**.

---

## 8. Automated uploads — the service account (one-time)

This is what lets `git tag` push straight to Play.

1. **Google Cloud Console** (any project): **APIs & Services → Library →** enable
   **"Google Play Android Developer API"**.
2. **IAM & Admin → Service accounts → Create service account.** Name it e.g. `play-ci`.
   No roles needed. Create it.
3. Open the new service account → **Keys → Add key → Create new key → JSON** → download it.
4. **Play Console → Users and permissions → Invite new user.** Paste the service account's
   email (looks like `play-ci@…iam.gserviceaccount.com`). Under **App permissions** add **Pause**
   and grant **Release → Release to testing tracks** (and **Release apps to production** later).
   Send invite — it auto-accepts.
5. **GitHub → repo Settings → Secrets and variables → Actions → New repository secret**:
   - Name: **`PLAY_SERVICE_ACCOUNT_JSON`**
   - Value: paste the **entire contents** of the JSON key file.

Once that secret exists, the release workflow's "Publish to Google Play (internal testing)" step
activates automatically. Until then it's skipped (so earlier releases never fail on it).

---

## 9. The 12-tester closed test (the gate to production)

Personal accounts created after Nov 2023 must run a **closed test** with **≥12 testers opted in
for ≥14 continuous days** before Play unlocks production.

1. **Test and release → Testing → Closed testing → Create track** (or use the default
   *Alpha*).
2. **Testers:** create an email list with **at least 12 people** (friends/family Gmail
   accounts all count). Add the list to the track.
3. Share the **opt-in URL** with them; each must tap **Become a tester** and **install** Pause
   from the Play link at least once.
4. Keep the track live and the testers opted in for **14 continuous days**.
5. After 14 days, Play shows **Apply for production access** → fill the short form → wait for
   review.

The first build reaches this track automatically once §8 is done and you push a tag (it lands on
*Internal testing*; **promote Internal → your Closed track** in the Console, or change the
workflow `track:` to your closed track name).

---

## 10. Cutting a release (every update, from here)

```
# bump versionCode (+1) and versionName in app/build.gradle.kts, commit, then:
git tag v1.0.11
git push origin v1.0.11
```

GitHub Actions then:
1. runs unit tests, builds a **signed** `.apk` + `.aab`,
2. publishes a **GitHub Release** with both attached (direct `.apk` link posted in chat),
3. if `PLAY_SERVICE_ACCOUNT_JSON` is set, **uploads the `.aab` to Play Internal testing** with
   the changelog from `distribution/whatsnew/whatsnew-en-US`.

**Changelog:** edit `distribution/whatsnew/whatsnew-en-US` before tagging — that text becomes the
"What's new" on Play. (≤500 chars.)

**Promote a release:** Play Console → the track → **Promote release** → Closed/Production.

---

## Quick checklist

- [ ] §1 App created in Play Console
- [ ] §3 Store listing + graphics
- [ ] §4 GitHub Pages on → privacy URL pasted
- [ ] §5 Data safety: "no data collected/shared"
- [ ] §6a Decide accessibility path (A: usage-only on Play, recommended)
- [ ] §7 Content rating
- [ ] §8 Service account + `PLAY_SERVICE_ACCOUNT_JSON` secret
- [ ] §9 Closed test, 12 testers, 14 days
- [ ] §10 `git tag` → auto-build → promote
