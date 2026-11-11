# Android app template

A template for Android apps that are written, built and installed without a PC: Claude edits this repo from a cloud
session, GitHub Actions builds the APK, and the phone installs it from a fixed link. Make a new app with GitHub's
"Use this template", then `bash tools/new-app.sh "<name>" <package> <ApkName>` (see README.he.md).

- The user writes Hebrew: answer in Hebrew. The app's UI text is Hebrew (RTL): `setLayoutDirection(RTL)` on the
  root views, `android:supportsRtl="true"`.
- Framework APIs only (no AndroidX, no dependencies). Java 8 source level, and no lambdas or method references:
  anonymous classes. Keep it that way unless the user asks: it keeps cloud builds fast and stable.
- `app/src/main/java/<package>/core` must not import `android.*`, so `core-tests` runs on a plain JDK. Put logic
  there (parsing, calculations, formats, network protocols) and test it; keep the screens thin.
- `App.log(...)` writes to an in-memory log; the main screen's "שיתוף יומן תקלות" shares it with the last crash
  report. Log what helps debugging a phone you cannot reach; never log keys or personal data.
- Keys the user enters (API keys and the like) go in the app's private preferences (`App.prefs`), never in the code.

## Cloud sessions (Claude Code on the web, from the phone)
- There is no Android SDK there, so `gradlew assembleDebug` fails; do not try. Run `bash core-tests/run.sh`
  (plain JDK) before pushing, and read the code you changed carefully: compile errors only show up in Actions.
  To catch them earlier, compile the sources against an `android.jar` of API 29+ with a stub `R` class, if one is at
  hand.
- Push straight to `main` unless the user asks for a branch. `.github/workflows/build.yml` then runs the core
  tests, builds the debug APK and replaces the `latest` release:
  `https://github.com/<owner>/<repo>/releases/download/latest/<app.apk>.apk` (`app.apk` in gradle.properties).
  Watch the run (`gh api repos/<owner>/<repo>/actions/runs?head_sha=...`) and tell the user when the APK is there.
- The workflow needs the `DEBUG_KEYSTORE_B64` secret: the user's shared debug key (alias androiddebugkey, passwords
  android), base64. `app.signer` in gradle.properties is its certificate SHA-256, checked after the build, so every
  build installs over the last. Never commit a keystore (.gitignore blocks them). A session cannot set secrets:
  ask the user to add it under Settings > Secrets and variables > Actions.
- Do not create tags; the workflow makes the release itself.
- `versionCode` is the commit count (`app/build.gradle`), so never rewrite history on `main`.
- No adb from the cloud: ask the user for screen recordings, or for the log (the share-log button).

## Starting a new app from the template
1. The user: "Use this template" on GitHub (private repo), then gives the repo to the session.
2. `bash tools/new-app.sh "<name on the phone>" com.stodio.<app> <ApkName>`: renames the package everywhere and
   sets the name, package id and APK name. Replace the icon (`res/mipmap-*/ic_launcher.png`) when the app has one.
3. The user adds the `DEBUG_KEYSTORE_B64` secret to the new repo (the same value for all their apps).
4. Rewrite this file's first paragraph for the new app, build the first screen, push, send the link.
