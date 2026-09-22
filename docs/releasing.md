# Cutting a release build

Debug builds are signed with a throwaway key that Android generates on its own. Anything that goes on a real device someone else uses needs the release key instead.

## Why the key matters more than anything else here

On Android the signing certificate *is* the app's identity. An update is only accepted if it's signed by the same key as the install it's replacing. Two consequences follow, and both are permanent:

- **Lose the keystore and every install already out there is stranded.** There is no recovery, no reissue, no appeal. The only way forward would be a new `applicationId` and a clean install on every device, losing whatever the app had saved.
- **Anyone holding the keystore can ship a build devices trust as genuine.** The password is a second factor, not the protection — see below.

So: back `wled-climb-release.jks` up somewhere durable and offline before distributing anything signed with it. That single action matters more than every other precaution in this document.

## One-time setup

### 1. Generate the keystore

Keep it outside the repo. `*.jks` is gitignored, but ignored files are exactly what `git clean -xfd` deletes, and that is not a command you want one keystroke away from an unrecoverable app.

```
mkdir D:\keys
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore D:\keys\wled-climb-release.jks -alias wled-climb -keyalg RSA -keysize 2048 -validity 10000
```

The validity is deliberately absurd. A certificate that expires can't sign updates, and there's no renewal path for an installed app, so it's set well past the point where it could plausibly matter.

When keytool asks for a key password, pressing Return reuses the keystore password. That's fine — just note whether you did, because both values are needed below.

### 2. Store the credentials outside the project

In `~/.gradle/gradle.properties` (on Windows, `C:\Users\<you>\.gradle\gradle.properties`):

```properties
WLED_CLIMB_STORE_FILE=D:/keys/wled-climb-release.jks
WLED_CLIMB_STORE_PASSWORD=<keystore password>
WLED_CLIMB_KEY_ALIAS=wled-climb
WLED_CLIMB_KEY_PASSWORD=<key password, same as above if you pressed Return>
```

**Why not a gitignored file inside the project?** Because gitignore only defends against git. A secret in the project folder is still reachable by a zip of the directory, a backup tool pointed at the repo, a stray `git add -f`, or any tool that walks the tree and uploads it. Outside the tree, none of those reach it.

To be clear about what this does and doesn't buy: on your own machine, anything that can read `gradle.properties` can also read the `.jks`. The password adds nothing against a local attacker. It earns its keep when the keystore travels *without* it — a cloud backup, a handover to CI, a drive you sell.

Backslashes are escape characters in `.properties` files. If a password contains one, double it (`\\`) or it will be silently mangled into a different string and you'll get `keystore password was incorrect` with nothing visibly wrong.

The same names work as `-PWLED_CLIMB_STORE_PASSWORD=…` flags or `ORG_GRADLE_PROJECT_WLED_CLIMB_STORE_PASSWORD` environment variables, so CI needs no separate mechanism.

### 3. JAVA_HOME

Android Studio uses its bundled JDK internally without exporting it, so Gradle from a terminal can't find it:

```
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "User")
```

## Cutting a release

1. **Bump `versionCode` and `versionName`** in `app/build.gradle.kts`.

   `versionCode` is what decides "is there a newer build?" — Android rejects an update whose code is lower than what's installed, and Obtainium and Play won't offer one that isn't higher. Only builds that actually go out need it to move; `adb install -r` reinstalls an unchanged code happily. Forgetting presents as an update that silently doesn't apply, which is why the version is on screen in the app.

2. **Build:**

   ```
   ./gradlew assembleRelease
   ```

   Output lands at `app/build/outputs/apk/release/app-release.apk`. If it comes out named `app-release-unsigned.apk`, the credentials weren't found — the build deliberately falls back rather than failing, so a fresh clone can still build.

3. **Verify the signature**, rather than trusting the filename:

   ```
   & "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs -v app\build\outputs\apk\release\app-release.apk
   ```

   Expect `v2` and `v3` both true. v1 stays off (minSdk is 26; v2 covers API 24+). v3 is what makes key rotation possible at all on API 28+ — the only recourse if the key is ever compromised. It does nothing for a key that's simply *lost*, since rotation needs the old key to authorise the new.

## Installing the first signed build

A release-signed APK will not install over a debug-signed one. The certificate differs, so you get:

```
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.wledclimb.app signatures do not match newer version
```

Uninstall first. **This wipes app data**, so the saved controller IP has to be retyped. Every device makes this transition exactly once; afterwards, signed builds update in place normally.

```
adb uninstall com.wledclimb.app
adb install app\build\outputs\apk\release\app-release.apk
```
