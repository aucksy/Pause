# Pause — Play Store submission (do this in order)

Everything you upload is in this `play-store/` folder. Text fields are in `listing.txt`.
The deep-dive reference (service account, App-signing, etc.) is in the repo's `PLAY_STORE.md`.

> **Account note:** a personal Play account created after 13 Nov 2023 must run a **closed test
> with ≥12 testers for ≥14 continuous days** before you can apply for production. So the goal of
> this first pass is to get Pause onto a **closed testing** track and start that 14-day clock.

---

## Assets in this folder (already sized correctly)
| File | Play field |
|---|---|
| `icon-512.png` (512×512) | App icon |
| `feature-graphic-1024x500.png` (1024×500) | Feature graphic |
| `screenshot-1-home.png` … `screenshot-4-personalize.png` (1080×1920) | Phone screenshots |
| (download) `Pause-v1.0.9.aab` | App bundle — https://github.com/aucksy/Pause/releases/download/v1.0.9/Pause-v1.0.9.aab |

---

## Step 1 — Publish the privacy policy URL (2 min, do first)
GitHub → repo **aucksy/Pause** → **Settings → Pages** → Source **Deploy from a branch** →
Branch **main**, folder **/docs** → **Save**. After ~1 min it's live at
**https://aucksy.github.io/Pause/privacy.html** — confirm it loads, you'll paste it into Console.

## Step 2 — Create the app
Play Console → **Create app** → name **Pause**, **English (US)**, **App**, **Free** → tick the
declarations → **Create app**.

## Step 3 — Main store listing  (Grow → Store presence → Main store listing)
Paste from `listing.txt`: app name, short description, full description. Upload `icon-512.png`,
`feature-graphic-1024x500.png`, and the four screenshots. Save.

## Step 4 — Store settings (category + contact)
Category **Productivity**; contact email **aakashpahuja1990@gmail.com**. Save.

## Step 5 — App content (Policy → App content)
Fill each card using `listing.txt`:
- **Privacy policy** → paste the Step-1 URL.
- **Data safety** → "No data collected or shared" (answers in `listing.txt`).
- **Content rating** → run the questionnaire (Utility/Productivity; all "No") → Everyone.
- **Ads** → No ads. **Target audience** → 13+/18+, not for children.
- **Foreground service (special use)** → paste the justification from `listing.txt`.
- (No accessibility declaration — Pause doesn't use one.)

## Step 6 — Upload the build to a Closed testing track
**Test and release → Testing → Closed testing → Create track** (or use the default Alpha) →
**Create new release** → upload **Pause-v1.0.9.aab** → release notes: paste
`distribution/whatsnew/whatsnew-en-US` → Save → Review → **Start rollout to Closed testing**.
> Play App Signing is on by default — accept it. Pause has no Google sign-in, so there's no SHA-1
> to register anywhere.

## Step 7 — Add your 12 testers + start the 14-day clock
In the Closed track → **Testers** → create an email list with **≥12 Google accounts** → add it →
copy the **opt-in URL** → each tester taps **Become a tester** and **installs** Pause once.
Keep them opted in for **14 continuous days**.

## Step 8 — (optional, recommended) automate future updates
Do the one-time service-account setup in `PLAY_STORE.md` §8 and add the GitHub secret
**`PLAY_SERVICE_ACCOUNT_JSON`**. After that, `git tag v1.0.x && git push --tags` builds the signed
AAB and uploads it to **Internal testing** automatically with the changelog — you just promote it
to your Closed/Production track in Console.

## Step 9 — Apply for production
After 14 continuous days with ≥12 testers, Play shows **Apply for production access** → fill the
short form → submit for review.

---

### Regenerating the graphics
Edit `play-store/src/*.html`, then run:
`powershell -NoProfile -ExecutionPolicy Bypass -File play-store/render.ps1`
