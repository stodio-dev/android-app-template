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

## Lessons log (the template repo is the source)
- The lessons log (build errors and work methods from every app made from this template, in Hebrew) lives in the
  template repo, https://github.com/stodio-dev/android-app-template: `docs/lessons/LESSONS.md` (index and entries) and
  `docs/lessons/RULES.md` (format and rules). An app finds the template in `template.repo` in gradle.properties, next
  to `template.date` (when the app was made); an older app without them uses `stodio-dev/android-app-template`.
  This repo's own `docs/lessons/` is only a read-only snapshot (its README says from when). If this repo is the
  template itself (`app.id=com.stodio.app`), the log here is the source.
- Reading: at the start of a task, attach the template to the session with `add_repo` (`access: "push"`; the user may
  be asked to approve), clone it into a folder outside this repo, and read the index of its LESSONS.md. When a build,
  workflow or command fails, search the log there for words from the error before trying fixes. If the template cannot
  be attached, read the local snapshot instead.
- Writing: after solving a problem that took more than one try, or finding a method that saves builds or time, add an
  entry or update the existing one in the template's log, following RULES.md: search first, never duplicate, the exact
  error line, no secrets. A failure nobody could explain gets an open entry (RULES §2), not silence. Run
  `git pull --rebase` in the template clone right before editing and push right after. From an app session touch only
  `docs/lessons/` in the template; a fix to its code, scripts or workflow is for the user to ask for.
- An entry only this app needs goes in `docs/lessons/APP.md` here (same format, IDs `APP-001`...). If the template
  cannot be attached, write the entry in full to `docs/lessons/PENDING.md` here and tell the user; a later session
  that can attach the template moves it there and deletes it from PENDING.md. Never write in the snapshot files.
- At the end of every task, tell the user which entries were added or updated, by ID, and where each was written
  (template, APP.md or PENDING.md).

## Cloud sessions (Claude Code on the web, from the phone)
- There is no Android SDK there, so `gradlew assembleDebug` fails; do not try. Run `bash core-tests/run.sh`
  (plain JDK) before pushing, and read the code you changed carefully: compile errors only show up in Actions.
  To catch them earlier, compile the sources against an Android framework jar with a stub `R` class: the commands
  are in the lessons log, BUILD-001.
- Push straight to `main` unless the user asks for a branch. `.github/workflows/build.yml` then runs the core
  tests, builds the debug APK and replaces the `latest` release:
  `https://github.com/<owner>/<repo>/releases/download/latest/<app.apk>.apk` (`app.apk` in gradle.properties).
  Watch the run (`gh api repos/<owner>/<repo>/actions/runs?head_sha=...`) and tell the user when the APK is there.
  A push that changes only `docs/**` or `*.md` files starts no run (`paths-ignore`): do not wait for one.
- The workflow needs the `DEBUG_KEYSTORE_B64` secret: the user's shared debug key (alias androiddebugkey, passwords
  android), base64. `app.signer` in gradle.properties is its certificate SHA-256, checked after the build, so every
  build installs over the last. Never commit a keystore (.gitignore blocks them). A session cannot set secrets:
  ask the user to add it under Settings > Secrets and variables > Actions.
- Do not create tags; the workflow makes the release itself.
- `versionCode` is the commit count (`app/build.gradle`), so never rewrite history on `main`.
- No adb from the cloud: ask the user for screen recordings, or for the log (the share-log button).

## Starting a new app from the template
1. The user: "Use this template" on GitHub (https://github.com/stodio-dev/android-app-template/generate, private
   repo), then gives the repo to the session.
2. `bash tools/new-app.sh "<name on the phone>" com.stodio.<app> <ApkName>`: renames the package everywhere and
   sets the name, package id and APK name, and records `template.repo` and `template.date` in gradle.properties.
   Replace the icon (`res/mipmap-*/ic_launcher.png`) when the app has one.
3. The user adds the `DEBUG_KEYSTORE_B64` secret to the new repo (the same value for all their apps).
4. Rewrite this file's first paragraph for the new app, build the first screen, push, send the link.
