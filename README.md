# Xtremerpie Guardian V2 — Final source package

**Architecture:** Vercel serverless API + static PC dashboard + Supabase Postgres + native Android Kotlin foreground-service app.

This is a source project, not a precompiled APK. Build the APK with Android Studio (instructions below). The app requires a real HTTPS Vercel deployment and a Supabase project.

## Included
- `public/`: PC dashboard with activation, connection/battery/location status, overdue alert and event history.
- `api/`: Vercel functions; server-side Supabase access; admin/device shared-key authentication.
- `supabase/schema.sql`: database setup.
- `android/`: native Android app source. Visible ongoing notification; user-started foreground service; 30-second check-ins; optional location; stop button.
- `.env.example`: required Vercel environment variables.

## Part A — Supabase (about 5 minutes)
1. Create a project at https://supabase.com/dashboard.
2. Open **SQL Editor → New query**.
3. Paste all of `supabase/schema.sql` and run it.
4. Open **Project Settings → API** and copy the Project URL and `service_role` key. The service-role key is secret: only put it in Vercel environment variables. Never put it in Android or browser code.

## Part B — Deploy dashboard/API to Vercel (about 10 minutes)
1. Create a GitHub repository, e.g. `Xtremerpie/guardian`.
2. Upload the contents of this folder to the repository root (not the containing folder).
3. Import that repository at https://vercel.com/new.
4. Framework preset: **Other**. Build command: leave blank. Output directory: `public`. Install command: leave blank.
5. In Vercel → Project → Settings → Environment Variables, add:
   - `SUPABASE_URL` = Supabase project URL
   - `SUPABASE_SERVICE_ROLE_KEY` = Supabase service_role key
   - `ADMIN_KEY` = a long random secret used only on your PC dashboard
   - `DEVICE_KEY` = a different long random secret used only in the phone app
6. Add variables to Production (and Preview if you use previews), save, then redeploy.
7. Open `https://YOUR-PROJECT.vercel.app`. Enter ADMIN_KEY.
8. Use HTTPS URL only in the Android app.

Generate strong keys locally in PowerShell:
`[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`
Run twice and use different outputs. Keep them private.

## Part C — Build Android APK (about 10–20 minutes, first build may take longer)
1. Install Android Studio: https://developer.android.com/studio
2. Open the `android` folder in Android Studio.
3. Let Gradle sync. If Android Studio asks for a Gradle JDK, select its embedded JDK 17.
4. Install Android SDK Platform 35 and Build Tools through SDK Manager if prompted.
5. On the Android phone, enable Developer options and USB debugging (or transfer the APK after building).
6. Build → Build Bundle(s) / APK(s) → Build APK(s).
7. APK output is normally `android/app/build/outputs/apk/debug/app-debug.apk`.
8. Install it on the phone. For sideloading, Android may ask permission to install from that source.

## Part D — Connect phone
1. Open Xtremerpie Guardian on the phone.
2. Server URL: `https://YOUR-PROJECT.vercel.app` (no trailing slash required).
3. Device key: the exact `DEVICE_KEY` from Vercel.
4. Enable location sharing only if the phone owner agrees. Grant location permission when requested.
5. Tap **Save & start monitoring service**. Accept notification permission.
6. Android should show an ongoing Guardian notification. The owner can stop it from inside the app.
7. On PC, activate monitoring in the dashboard.

## How alerts work
- Phone checks the remote activation state and posts a heartbeat every ~30 seconds while the Android service runs.
- PC dashboard refreshes every 10 seconds. If monitoring is active and no heartbeat arrived in 120 seconds, it shows an overdue alert and requests a browser notification (permission required).
- Location is only requested when the owner has enabled sharing and granted location permission; updates are rate-limited to about once a minute.
- Dashboard can be open on another PC/phone browser too.

## Critical Android limitations (read before relying on it)
- This is a best-effort foreground service, not a guaranteed anti-tamper or emergency service. Android vendors may stop it; battery optimization, force-stop, reboot, lost network, permissions, and OS restrictions affect operation.
- Android 15 limits some foreground-service types such as `dataSync`; prolonged operation may be stopped by the OS. Test on the target phone and check current Android rules. Do not claim it can prove the phone was powered off or identify why check-ins stopped.
- This app does not secretly capture screenshots, audio, messages, or other private activity. It cannot bypass Android permissions or device-owner controls.
- Browser notifications require the dashboard to remain open and permission granted. Vercel does not send alerts to a closed PC browser. For reliable remote push, a future FCM setup and a trusted notification recipient are needed.
- The included simple shared-key model is a personal prototype, not a production-grade multi-user identity system. Rotate keys if exposed; restrict repository access; never commit secrets.
- Location is sensitive. Keep sharing opt-in, visible and limited to the intended safety purpose. Tell the phone owner who can see it and when.

## Troubleshooting
- Dashboard shows server configuration error: check all four Vercel environment variables and redeploy.
- Database errors: run `supabase/schema.sql`; verify URL and service-role key.
- Phone says unauthorized: compare DEVICE_KEY exactly; redeploy after changing Vercel env vars.
- No heartbeat: verify phone internet, app notification, permissions, battery settings and that service is running.
- No location: enable sharing in app, grant precise/approximate location, enable device location services; location may be stale or unavailable indoors.
- Vercel logs: Project → Deployments → Functions / Runtime Logs.
