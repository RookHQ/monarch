# Monarch upgrade playbook

How to upgrade the toolchains and dependencies of Monarch for Android
(`platforms/android`) and Monarch for iOS (`platforms/ios`), adopt new platform
APIs, release, roll back, and move Rook applications to a new release. Every
command, file and key here exists in the repository as of Monarch native
`0.1.0` (unreleased). Where something cannot be done yet, this page says so
and describes what is done instead.

This page is the procedure. The policy it carries out is in:

- [Native release and maintenance](docs/governance/native-release-and-maintenance.md):
  versions, release tracks, the upgrade rule, the maintenance calendar.
- [Native platform evolution](docs/governance/native-platform-evolution.md):
  adopting platform APIs, compatibility boundaries, `MONARCH-COMPAT`
  workarounds, raising a minimum OS.
- [Native API stability](docs/governance/native-api-stability.md): what is
  public API, what is breaking, deprecation and migration guides.
- [NATIVE-COMPATIBILITY](docs/architecture/NATIVE-COMPATIBILITY.md): the
  tested versions. Every upgrade updates it.

The web packages (`packages/*`, npm) have their own rules:
[versioning](docs/governance/versioning.md),
[branching](docs/governance/branching.md#how-a-change-travels) and
[dependencies](docs/governance/dependencies.md#updating-dependencies).

## Before any upgrade

### Where versions live

Every version is written once.

| What                                                                       | File                                                              | Key                                                                                    |
| -------------------------------------------------------------------------- | ----------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| Monarch for Android                                                        | `platforms/android/gradle/libs.versions.toml`                     | `[versions] monarch`                                                                   |
| Kotlin, Kotlin Gradle plugin, Compose compiler plugin, `kotlin-test-junit` | `platforms/android/gradle/libs.versions.toml`                     | `[versions] kotlin`                                                                    |
| Android Gradle plugin                                                      | `platforms/android/gradle/libs.versions.toml`                     | `[versions] agp`                                                                       |
| Gradle                                                                     | `platforms/android/gradle/wrapper/gradle-wrapper.properties`      | `distributionUrl`, `distributionSha256Sum`                                             |
| JDK toolchain, bytecode target                                             | `platforms/android/gradle/libs.versions.toml`                     | `[versions] jdk`, `[versions] jvmTarget`                                               |
| Compose and Material 3                                                     | `platforms/android/gradle/libs.versions.toml`                     | `[versions] composeBom`                                                                |
| SDK levels                                                                 | `platforms/android/gradle/libs.versions.toml`                     | `[versions] compileSdk`, `targetSdk`, `minSdk`                                         |
| detekt, binary-compatibility-validator                                     | `platforms/android/gradle/libs.versions.toml`                     | `[versions] detekt`, `binaryCompatibilityValidator`                                    |
| Libraries and test libraries                                               | `platforms/android/gradle/libs.versions.toml`                     | `activityCompose`, `lifecycle`, `junit`, `robolectric`, `androidxTestCore`, `espresso` |
| Build workarounds                                                          | `platforms/android/gradle.properties`                             | `android.onlyEnableUnitTestForTheTestedBuildType`                                      |
| Monarch for iOS                                                            | `platforms/ios/Sources/MonarchUI/MonarchUI.swift`                 | `public static let version`                                                            |
| Sample app version                                                         | `platforms/ios/Sample/project.yml`                                | `MARKETING_VERSION`                                                                    |
| Swift tools version and language mode                                      | `platforms/ios/Package.swift`                                     | line 1, `// swift-tools-version: 6.0`                                                  |
| Swift language settings                                                    | `platforms/ios/Package.swift`                                     | `monarchSwiftSettings`                                                                 |
| iOS deployment target                                                      | `platforms/ios/Package.swift`, `platforms/ios/Sample/project.yml` | `platforms:` `.iOS(.v17)`; `deploymentTarget` `iOS: "17.0"`                            |
| Sample Swift version                                                       | `platforms/ios/Sample/project.yml`                                | `SWIFT_VERSION`                                                                        |
| Test simulator                                                             | `platforms/ios/Scripts/verify.sh`                                 | default of `MONARCH_IOS_DESTINATION`                                                   |
| Xcode used in CI                                                           | `.github/workflows/native.yml`                                    | `runs-on` and `DEVELOPER_DIR` of the `ios` job                                         |
| JDK used in CI                                                             | `.github/workflows/native.yml`                                    | `java-version` of the `android` job                                                    |
| Dependency update automation                                               | `.github/dependabot.yml`                                          | the `gradle` and `swift` entries                                                       |

### Known version couplings

These are the constraints found in this repository. Check each one that an
upgrade touches.

- **AGP and Gradle.** AGP 9.5 needs Gradle 9.6.0 or later (Google's
  "Update Gradle" table). The wrapper is at 9.9.1.
- **AGP and the compile SDK.** AGP 9.5 supports compile SDK up to API 37.
  A new API level needs an AGP that supports it.
- **AGP and Kotlin.** AGP 9 has built-in Kotlin and depends at runtime on
  Kotlin Gradle plugin 2.2.10, the minimum built-in Kotlin supports. The
  Android modules do not apply `org.jetbrains.kotlin.android`; they get
  Kotlin from AGP, and `build-logic/build.gradle.kts` puts the catalog's
  Kotlin Gradle plugin (`plugin-kotlin`) on the build classpath, which
  replaces AGP's 2.2.10. `monarch-tokens` applies `org.jetbrains.kotlin.jvm`
  directly.
- **Kotlin and the Compose compiler.** `plugin-compose-compiler` uses
  `version.ref = "kotlin"`. They cannot differ.
- **Kotlin and detekt.** detekt 1.23.8 parses code with its own embedded
  Kotlin compiler, 2.0.21 (the `kotlin-compiler-embeddable` dependency of
  `detekt-parser`). Syntax newer than Kotlin 2.0 may not parse.
- **Gradle 10 and 11 and the build plugins.** The Gradle 9.9.1 problems
  report (`platforms/android/build/reports/problems/problems-report.html`)
  shows detekt 1.23.8 calling `ReportingExtension.file(String)`, removed in
  Gradle 10, and detekt and binary-compatibility-validator 0.18.2 calling
  `Configuration.setVisible(boolean)`, removed in Gradle 11. Gradle 10
  cannot be adopted while detekt is on 1.23.x.
- **binary-compatibility-validator and AGP.** The plugin does not register
  its tasks for AGP's built-in Kotlin, so
  `build-logic/src/main/kotlin/monarch.android.library.gradle.kts` wires
  `apiBuild`, `apiCheck` and `apiDump` by hand, using the task name
  `compileReleaseKotlin` and the plugin's internal configuration
  `bcv-rt-jvm-cp-resolver`. An AGP or validator upgrade that renames either
  breaks the API check.
- **Compose BOM and transitive AndroidX.** `lifecycle` (2.11.0) is pinned
  because BOM 2026.09.00 pulls in `lifecycle-runtime` 2.9.4, whose lint
  checks the lint of AGP 9.5 cannot load. `espresso` (3.7.0) is pinned
  because `compose-ui-test-junit4` pulls in Espresso 3.5.0, which calls an
  API Android 14 removed and fails under Robolectric. Both are explained in
  comments in `libs.versions.toml`, and the lifecycle constraint is in
  `monarch-compose/build.gradle.kts` and `sample/build.gradle.kts`.
- **AGP 9.5.1 and unit tests.** `android.onlyEnableUnitTestForTheTestedBuildType=false`
  in `gradle.properties` keeps the release unit test component that AGP's
  Compose Preview task needs; without it `./gradlew tasks` fails for the
  release variant. The convention plugins switch `testReleaseUnitTest` off
  again so tests run once.
- **Robolectric and the SDK.** The tests run on Robolectric 4.18, which must
  support the SDK level the build compiles and targets.
- **Xcode, Swift and the iOS SDK.** One Xcode release fixes the Swift
  compiler, swift-format (used by `Scripts/lint.sh`), the iOS SDK and the
  simulator runtimes it can run. They move together.
- **Xcode and the developer directory.** On the maintainer's Mac,
  `xcode-select -p` points at the Command Line Tools, not Xcode. The scripts
  never change it: `Scripts/verify.sh` and `Scripts/merge-check.sh` use
  `DEVELOPER_DIR` when it is set, and fall back to
  `/Applications/Xcode.app/Contents/Developer` for their own process when
  no `xcodebuild` is found. Select an Xcode the same way:
  `DEVELOPER_DIR=/Applications/Xcode_26.6.app/Contents/Developer Scripts/verify.sh`.
- **The iOS API baseline and the compiler.** `Scripts/api.sh` builds the
  baseline from the compiler's symbol graph. A new Swift compiler can print
  a declaration differently with no source change.

### The validation pipeline

An upgrade is approved on all of these, never on a successful compile
alone. Run the commands from the directory named.

| Check                                                             | Command                                             | Where               |
| ----------------------------------------------------------------- | --------------------------------------------------- | ------------------- |
| Generated token files match `packages/tokens`                     | `npm run native:tokens:check`                       | repository root     |
| Android: inventory, detekt, lint, tests, API, sample, publish     | `./gradlew verifyMonarch`                           | `platforms/android` |
| iOS: inventory, lint, host and simulator tests, API, DocC, sample | `Scripts/verify.sh`                                 | `platforms/ios`     |
| Component counts on NATIVE-COMPONENTS are current                 | `node scripts/native/components-report.mjs --check` | repository root     |
| Markdown you changed is formatted                                 | `npx prettier --check <files>`                      | repository root     |

`verifyMonarch` runs: `checkInventory`; `detekt` on every module;
`:monarch-compose:lintDebug` and `:sample:lintDebug`; `:monarch-tokens:test`,
`:monarch-compose:testDebugUnitTest` and `:sample:testDebugUnitTest`;
`:sample:assembleDebug` and `:sample:assembleRelease` (minified);
`apiCheck` on both libraries; and
`publishAllPublicationsToLocalRepository` on both libraries. It is defined in
`platforms/android/build-logic/src/main/kotlin/monarch.root.gradle.kts`.

`Scripts/verify.sh` runs: `Scripts/generate.py --check`; `Scripts/lint.sh`;
`swift build` and `swift test` with warnings as errors; `Scripts/api.sh`; the
package build and tests on the simulator; `xcodebuild docbuild`; and the
sample app build. It prints the Xcode, Swift and swift-format versions first.

The native CI workflow `.github/workflows/native.yml` runs `verifyMonarch`,
`Scripts/verify.sh` and the token check on pull requests that touch
`platforms/**` or `packages/tokens/**`. It is not a required check; until the
repository owner adds it to the rulesets, a green run is evidence, not a gate,
and the upgrade report must still list the local results.

Also read, after any Android build, the Gradle problems report at
`platforms/android/build/reports/problems/problems-report.html`, and run once
with `./gradlew verifyMonarch --warning-mode all` to see every deprecation.

### The upgrade branch and report

1. `git switch uat && git pull && git switch -c chore/<platform>-<what>-<version>`,
   for example `chore/android-kotlin-2.5.0`.
2. One upgrade per pull request, except versions that must move together
   (listed above). One platform per pull request.
3. Make the change, run the validation pipeline, update
   [NATIVE-COMPATIBILITY](docs/architecture/NATIVE-COMPATIBILITY.md) and the
   platform's `CHANGELOG.md` under `Unreleased`.
4. Open the pull request into `uat` with the upgrade report the
   [upgrade process](docs/governance/native-release-and-maintenance.md#upgrade-process)
   requires: versions from and to, couplings checked, new deprecations and
   what was done about each, the API diff (expected: none), test results as
   counts, behaviour or rendering changes, how to roll back.
5. The platform owner and one maintainer review. Nothing merges on a green
   build alone, and nothing on a preview, beta or release-candidate version
   reaches `main`.

## 1. Upgrade Kotlin

Files: `platforms/android/gradle/libs.versions.toml`.

1. Read the Kotlin release notes and the compatibility guide for the new
   version. Note new deprecations and changed defaults (language version,
   JVM target defaults, compiler warnings).
2. Check the couplings: the AGP in `[versions] agp` supports this Kotlin
   Gradle plugin; detekt can still parse the code; the
   binary-compatibility-validator still reads the class files.
3. Change `[versions] kotlin`. This moves the Kotlin Gradle plugin, the
   Compose compiler plugin and `kotlin-test-junit` together.
4. From `platforms/android`:

   ```sh
   ./gradlew --version
   ./gradlew verifyMonarch --warning-mode all
   ```

5. Both modules compile with `allWarningsAsErrors`. Fix each new warning;
   do not suppress it. If a fix cannot be made yet, isolate it under a
   `MONARCH-COMPAT` comment
   ([workarounds](docs/governance/native-platform-evolution.md#compatibility-workarounds)).
6. `apiCheck` must pass unchanged. A Kotlin upgrade that changes the dump
   (for example, new synthetic members) is investigated: if the binary API
   really changed, it is a breaking change for apps and is decided as one.
7. If detekt fails to parse new syntax, either keep that syntax out of
   Monarch until detekt supports it, or upgrade detekt in a separate pull
   request first. Do not exclude files from detekt.
8. Update the Kotlin rows in NATIVE-COMPATIBILITY. If the Kotlin version an
   app needs to read Monarch's metadata rises, say so in the changelog: it is
   a breaking change under the
   [version rules](docs/governance/native-release-and-maintenance.md#versions).

## 2. Upgrade Swift

Files: `platforms/ios/Package.swift`, `platforms/ios/Sample/project.yml`,
`platforms/ios/.swift-format`.

The Swift compiler comes with Xcode; upgrading the compiler is
[step 5](#5-upgrade-xcode-and-apple-sdk-compatibility). This section covers
the tools version and the language mode, which the package declares.

1. Read the Swift release notes and the evolution proposals that change
   defaults in the new language mode or tools version.
2. To raise the tools version, edit line 1 of `Package.swift`
   (`// swift-tools-version: 6.0`). This raises the minimum Swift toolchain,
   and so the minimum Xcode, every app needs: a breaking change.
3. Language mode: Swift 6 follows from tools version 6.0. To adopt a new
   mode, set it in `Package.swift` (the `swiftLanguageModes:` argument of
   `Package`, or a per-target setting) and the matching `SWIFT_VERSION` in
   `Sample/project.yml`.
4. Upcoming features are listed once, in `monarchSwiftSettings` in
   `Package.swift` (today `ExistentialAny`). Add or remove them there.
5. From `platforms/ios`:

   ```sh
   export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer   # the Xcode under test
   Scripts/verify.sh
   ```

6. swift-format ships with the toolchain. If the new version adds or renames
   rules, update `.swift-format`, run `Scripts/lint.sh --fix`, and review the
   diff as its own commit.
7. Run `Scripts/api.sh`. If it reports a difference with no source change,
   compare the old and new text: a pure printing difference is accepted with
   `Scripts/api.sh --update` and named in the upgrade report; a real API
   change is handled as one.
8. Update the Swift rows in NATIVE-COMPATIBILITY and the "Requirements"
   table in `platforms/ios/README.md`.

## 3. Upgrade the Android Gradle plugin and Gradle

Files: `platforms/android/gradle/libs.versions.toml`,
`platforms/android/gradle/wrapper/gradle-wrapper.properties`,
`platforms/android/gradle.properties`, `platforms/android/build-logic/**`.

AGP and Gradle move together when the new AGP needs a newer Gradle;
otherwise upgrade Gradle on its own first.

1. Read the AGP release notes and Google's compatibility tables: the minimum
   Gradle, the minimum JDK, the maximum API level, the default build tools
   version, and the Android Studio version that supports it.
2. Read the Gradle release notes and the "upgrading" guide for every
   deprecation that the problems report lists today.
3. Gradle. Find the checksum of the `-bin` distribution on Gradle's
   release checksums page, then from `platforms/android`:

   ```sh
   ./gradlew wrapper --gradle-version <version> --distribution-type bin \
     --gradle-distribution-sha256-sum <sha256>
   # Run it again, with the same arguments: the second run uses the new Gradle and updates
   # gradle-wrapper.jar, gradlew and gradlew.bat.
   ./gradlew wrapper --gradle-version <version> --distribution-type bin \
     --gradle-distribution-sha256-sum <sha256>
   ./gradlew --version
   ```

   Check that `distributionUrl` and `distributionSha256Sum` in
   `gradle/wrapper/gradle-wrapper.properties` changed and that
   `validateDistributionUrl=true` is still set. Commit
   `gradle-wrapper.properties`, `gradle-wrapper.jar`, `gradlew` and
   `gradlew.bat` together.

4. AGP. Change `[versions] agp`. If the SDK build tools version it needs is
   not installed, install it with the SDK manager and record it in
   `platforms/android/README.md` ("Requirements").
5. Run the pipeline with all warnings:

   ```sh
   ./gradlew verifyMonarch --warning-mode all
   ```

   Lint runs with `warningsAsErrors = true`. A new AGP brings new lint
   checks; fix what they find.

6. Recheck every build workaround:
   - Remove `android.onlyEnableUnitTestForTheTestedBuildType=false` from
     `gradle.properties` and run `./gradlew tasks --all`. If it succeeds,
     delete the property and the two `testReleaseUnitTest` blocks in
     `build-logic/src/main/kotlin/monarch.android.library.gradle.kts` and
     `monarch.android.application.gradle.kts` in the same pull request.
   - Run `./gradlew apiDump` and `git diff -- '*/api/*.api'`: no change is
     expected. If `apiBuild` fails, the hand wiring in
     `monarch.android.library.gradle.kts` (task `compileReleaseKotlin`,
     configuration `bcv-rt-jvm-cp-resolver`) needs updating.
   - Remove the `lifecycle` constraint and run
     `./gradlew :monarch-compose:lintDebug :sample:lintDebug`; if lint now
     loads the transitive lifecycle checks, delete the pin.
7. Open the problems report and confirm that no new deprecation comes from
   Monarch's own build logic. Deprecations from third-party plugins are
   listed in the upgrade report with the plugin version that fixes them.
8. Update the AGP and Gradle rows in NATIVE-COMPATIBILITY, including the
   Android Studio version Google lists for the new AGP.

## 4. Upgrade Compose and its compiler integration

Files: `platforms/android/gradle/libs.versions.toml`.

The Compose compiler is the Kotlin Compose compiler plugin and has no version
of its own: it moves with `[versions] kotlin` ([section 1](#1-upgrade-kotlin)).
Compose and Material 3 libraries move with `[versions] composeBom`.

1. Read the BOM mapping for the new BOM and the release notes of Compose UI,
   Foundation, Material 3 and Material 3 adaptive between the old and new
   versions. Note anything moved from experimental to stable, deprecated,
   or removed, and any raised `minSdk`.
2. Change `[versions] composeBom`. Do not add versions to individual Compose
   libraries in `[libraries]`; they take them from the BOM.
3. See what the BOM resolves to:

   ```sh
   ./gradlew :monarch-compose:dependencies --configuration releaseRuntimeClasspath
   ./gradlew :monarch-compose:dependencyInsight --configuration debugRuntimeClasspath \
     --dependency androidx.lifecycle:lifecycle-runtime
   ./gradlew :monarch-compose:dependencyInsight --configuration debugUnitTestRuntimeClasspath \
     --dependency androidx.test.espresso:espresso-core
   ```

   Re-decide the `lifecycle` and `espresso` pins from the last two: remove a
   pin when the BOM already brings a version at or above it.

4. Run `./gradlew verifyMonarch --warning-mode all`. Pay attention to:
   - `MaterialMappingTest`, `MonarchThemeTest` and `ThemeResolutionTest`: a
     Material 3 change in colour scheme or typography slots shows up here;
     the fix is in `theme/MaterialMapping.kt`, the one mapping function;
   - `ComponentMatrixTest` and the per-component touch target and font scale
     assertions: a change in Material's minimum sizes or padding shows up
     here;
   - `ComponentPreviewsTest` and `InventoryExamplesTest`.
5. `apiCheck` must pass. Compose types appear in Monarch's public signatures,
   so check the dump if it changes.
6. Because `monarch-compose` exposes Compose UI, Foundation and Material 3 as
   `api` dependencies, raising the BOM raises what apps get. A BOM that
   forces apps to upgrade is a breaking change; say so in the changelog.
7. Update the Compose rows in NATIVE-COMPATIBILITY with the resolved Compose
   UI and Material 3 versions.

## 5. Upgrade Xcode and Apple SDK compatibility

Files: `platforms/ios/README.md`, `platforms/ios/Scripts/verify.sh`,
`.github/workflows/native.yml`, `docs/architecture/NATIVE-COMPATIBILITY.md`.

1. Install the new Xcode next to the current one (for example
   `/Applications/Xcode_<version>.app`). Do not change the system selection
   with `xcode-select`; select it per command with `DEVELOPER_DIR`.
2. Read the Xcode and iOS SDK release notes: deprecations, removed APIs, the
   lowest deployment target the new Xcode still builds for, and SwiftUI
   behaviour changes.
3. Check the simulator runtime and device the tests use:

   ```sh
   export DEVELOPER_DIR=/Applications/Xcode_<version>.app/Contents/Developer
   xcodebuild -version
   xcrun simctl list runtimes
   xcrun simctl list devices available | grep "iPhone 17 Pro"
   ```

   If the default device in `Scripts/verify.sh`
   (`platform=iOS Simulator,name=iPhone 17 Pro,OS=latest`) no longer exists,
   pass another with `MONARCH_IOS_DESTINATION` for this run, and change the
   default in `verify.sh` in the upgrade pull request.

4. From `platforms/ios`, with `DEVELOPER_DIR` still set:

   ```sh
   Scripts/verify.sh
   ```

5. Fix new warnings (warnings are errors in `verify.sh`). A deprecated API
   with no usable replacement at the deployment target is isolated behind
   `if #available` with a `MONARCH-COMPAT` comment.
6. DocC: `verify.sh` fails on any documentation warning except those from
   `appintentsmetadataprocessor`. If the new Xcode adds another tool's
   warnings that are not about Monarch's documentation, extend that filter in
   `verify.sh` with a comment saying why.
7. Public API: handle a baseline difference as in
   [section 2](#2-upgrade-swift), step 7.
8. Keep the previous Xcode working until the new one is the only one in the
   tested range; run `Scripts/verify.sh --host-only` with both.
9. CI: change `runs-on` and `DEVELOPER_DIR` of the `ios` job in
   `.github/workflows/native.yml` to an image that has the new Xcode
   (the image READMEs in `actions/runner-images` list each image's Xcode
   versions and paths).
10. Update the "Verified with" row in `platforms/ios/README.md` and the
    Xcode, Swift and iOS SDK rows in NATIVE-COMPATIBILITY from what
    `Scripts/verify.sh` printed in its `Toolchain` step.

## 6. Upgrade third-party dependencies

Android build and test dependencies, all in
`platforms/android/gradle/libs.versions.toml`: `detekt`,
`binaryCompatibilityValidator`, `activityCompose`, `lifecycle`, `junit`,
`robolectric`, `androidxTestCore`, `espresso`. The iOS package has no
dependencies.

Dependabot is configured in `.github/dependabot.yml` to propose minor and
patch updates for `platforms/android` (grouped: the AGP and Kotlin group, and
everything else) and for `platforms/ios`, weekly, into `uat`. Majors are
ignored and done by hand with this page. The entries have not produced a
pull request yet at the time of writing. Dependabot never merges anything;
each pull request still needs the full pipeline and review.

For each dependency:

1. Read its release notes between the two versions. Check that it is still
   maintained and has no open advisory for the target version.
2. Change its key in `[versions]` and nothing else.
3. Run `./gradlew verifyMonarch --warning-mode all`.
4. Dependency-specific checks:
   - **detekt**: new rules can report existing code. Fix the code or turn
     the rule off in `config/detekt/detekt.yml` with a comment saying why.
     There is no baseline file, and none is added. Moving to detekt 2 is a
     major upgrade: the plugin id, the configuration format and the Gradle
     integration change.
   - **binary-compatibility-validator**: run `./gradlew apiDump` and confirm
     no change to `monarch-*/api/*.api`, and that the hand-wired tasks in
     `monarch.android.library.gradle.kts` still compile.
   - **Robolectric**: check which SDK levels it supports against
     `[versions] compileSdk`. A new Robolectric can change rendering
     slightly; the matrix and per-component tests catch layout changes.
   - **Espresso, lifecycle**: these are pins, not direct dependencies. See
     [section 4](#4-upgrade-compose-and-its-compiler-integration), step 3.
5. A new dependency is not an upgrade. It needs the justification in
   [platform evolution](docs/governance/native-platform-evolution.md#rules-that-prevent-upgrade-lock-in)
   and its own pull request.

Not set up yet: Gradle dependency verification metadata, and automated
vulnerability scanning of native dependencies.

## 7. Adopt a new Android native UI API

Follow the nine steps in
[adopting a new platform API](docs/governance/native-platform-evolution.md#adopting-a-new-platform-api).
In this repository that means:

1. Open the evaluation issue first (steps 1 to 4 of the procedure).
2. If the API comes from a newer Compose or Material 3, upgrade the BOM first
   in its own pull request ([section 4](#4-upgrade-compose-and-its-compiler-integration)).
   If it needs a newer compile SDK, raise `[versions] compileSdk` first.
3. Put the boundary in one internal function inside
   `platforms/android/monarch-compose/src/main/kotlin/dev/monarchui/`,
   with the new branch in a function annotated `@RequiresApi(<level>)` and
   the decision as a pure function. `theme/SystemContrast.kt` is the model:
   `isSystemInHighContrast()` checks `Build.VERSION.SDK_INT`, the newer call
   is a `@RequiresApi` function, and `isHighContrastLevel` is tested without
   a device.
4. An experimental Compose or Material 3 API (`@Experimental…Api`) is used
   only inside one internal function with a `MONARCH-COMPAT` comment, and
   never in a public signature.
5. Tests: test the pure decision in a plain unit test, and the component on
   both sides of the boundary with Robolectric's
   `@Config(sdk = [<below>, <at or above>])` on the test class.
   Keep the existing tests for the old path. Run
   `./gradlew componentCheck -Pcomponent=<name>` while working.
6. Update the component's `platforms/android/inventory/<id>.json`
   (`platformNotes`, `accessibility`, `examples`) and run
   `./gradlew generateInventory`. Update the preview in `src/debug` and the
   catalogue page under `sample/src/main/kotlin/**/sections/`.
7. If the public API changed, run `./gradlew apiDump` and include the diff.
8. `./gradlew verifyMonarch`, a `Changed` entry in
   `platforms/android/CHANGELOG.md` that names the API level the new
   behaviour starts at, and the release as usual.

## 8. Adopt a new Apple native UI API

The same procedure, for iOS:

1. Open the evaluation issue first.
2. If the API needs a newer SDK, upgrade Xcode first
   ([section 5](#5-upgrade-xcode-and-apple-sdk-compatibility)).
3. Put the boundary in one internal or private function in
   `platforms/ios/Sources/MonarchUI/`, with `if #available(iOS <version>, *)`
   and the old behaviour in the `else` branch, and a comment saying it is
   removed when the deployment target reaches that version. A declaration
   that exists only on the newer OS is marked `@available(iOS <version>, *)`
   and is not public unless there is no reasonable fallback.
4. Experimental Monarch API for the new capability is marked
   `@_spi(Experimental)` and stays out of the baseline.
5. Tests: logic tests run on the host. Tests of the new path run on a
   simulator with the new OS (the default `OS=latest`). The old path needs a
   runtime at or below the deployment target, installed with Xcode's
   Components settings, and selected with, for example:

   ```sh
   MONARCH_IOS_DESTINATION='platform=iOS Simulator,name=<device>,OS=17.5' Scripts/verify.sh
   ```

   No iOS 17 or iOS 18 runtime has been used in this repository yet; the
   first adoption that needs one sets this up and records it in
   NATIVE-COMPATIBILITY.

6. Update `platforms/ios/inventory/<id>.json` and run
   `Scripts/generate.py`; update the `#Preview` and the sample page under
   `Sample/Sources/Sections/`.
7. If the public API changed, `Scripts/api.sh --update` and include the diff.
8. `Scripts/verify.sh`, a `Changed` entry in `platforms/ios/CHANGELOG.md`
   that names the iOS version the new behaviour starts at, and the release
   as usual.

## 9. Validate backward compatibility

Run for every upgrade and before every release.

Android, from `platforms/android`:

```sh
./gradlew apiCheck                    # public binary API against monarch-*/api/*.api
./gradlew verifyMonarch               # compiled examples, README examples, previews, sample
./gradlew publishAllPublicationsToLocalRepository
```

- `apiCheck` compares the release variant's binary API with the committed
  dumps. A difference fails. A removed or changed line is a breaking change
  ([what counts as breaking](docs/governance/native-api-stability.md#what-counts-as-breaking)).
- `InventoryExamplesTest` compiles every example in `inventory/*.json`, and
  `ReadmeExamplesTest` the README's Kotlin, so a source-breaking change fails
  where an app would.
- Consume the artefact the way an app does: point a scratch app at
  `platforms/android/build/repo` as in the README's "Install from source",
  depend on `dev.monarchui:monarch-compose:<version>`, and build it.
  There is no scratch consumer project in the repository; this is a manual
  step.
- Check the POM in `build/repo/dev/monarchui/monarch-compose/<version>/`
  names the Apache License 2.0 and only the intended dependencies.

iOS, from `platforms/ios`:

```sh
Scripts/api.sh                        # public declarations against api/*.txt
Scripts/verify.sh                     # compiled examples, README examples, sample, DocC
```

- `Scripts/api.sh` compares the symbol graph with `api/MonarchUI.txt` and
  `api/MonarchTokens.txt`; `Scripts/merge-check.sh` prints how many
  declarations were added and removed, and lists the removed ones.
- The generated examples in `Tests/MonarchUITests/Generated/Examples/` are
  the call sites an app would write; a source-breaking change fails to
  compile there.
- Consume the package the way an app does, from an empty package outside
  the repository:

  ```sh
  mkdir -p /tmp/monarch-consumer && cd /tmp/monarch-consumer
  swift package init --type library --name Consumer
  # In Package.swift: platforms [.iOS(.v17)], dependency .package(path: "<repo>/platforms/ios"),
  # and .product(name: "MonarchUI", package: "ios") in the Consumer target.
  xcodebuild build -scheme Consumer -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
  ```

Both: no Rook application consumes native Monarch yet, so there is no
application to test against. When one does, building it against the release
candidate is part of this step.

## 10. Release a new Monarch version

### Native releases

Native releases are manual. The release automation in
`.github/workflows/release.yml` publishes the npm packages only, and does not
build, test or publish anything under `platforms/`.

What is not possible yet, and what is done instead:

- **Maven Central**: not set up (no verified namespace, no signing keys).
  The artefacts are published to `platforms/android/build/repo` and apps
  build from source.
- **Swift Package Manager by URL**: not possible, because `Package.swift` is
  not at the repository root. Apps add `platforms/ios` from a local checkout.
- **CI-gated release**: the native workflow is not a required check and no
  release job depends on it. The release manager runs the pipeline locally
  and records the results.
- **Release tags**: `android-v<semver>` and `ios-v<semver>` are a proposed
  convention; no native tag has been pushed yet.

Steps for one platform (example: Android 0.2.0):

1. Branch `release/android-0.2.0` from `uat`.
2. Set the version:
   - Android: `[versions] monarch` in `platforms/android/gradle/libs.versions.toml`.
   - iOS: `public static let version` in
     `platforms/ios/Sources/MonarchUI/MonarchUI.swift` and
     `MARKETING_VERSION` in `platforms/ios/Sample/project.yml`.
3. In `platforms/<platform>/CHANGELOG.md`, rename the unreleased heading to
   the version and date, and add the lines the
   [changelog standard](docs/governance/native-release-and-maintenance.md#changelog-standard)
   requires (tokens package version, compatibility row, previous line).
4. Add the release section to NATIVE-COMPATIBILITY with the toolchain that
   builds it.
5. Run every check in [the validation pipeline](#the-validation-pipeline) and
   [section 9](#9-validate-backward-compatibility) from a clean checkout:

   ```sh
   git clone --branch release/android-0.2.0 https://github.com/RookHQ/monarch.git /tmp/monarch-release
   cd /tmp/monarch-release && npm ci && npm run native:tokens:check
   cd platforms/android && ./gradlew verifyMonarch
   ```

6. Pull request into `uat`; then the `uat` into `main` pull request, merged
   with a merge commit ([branching](docs/governance/branching.md#how-a-change-travels)).
7. On the merge commit on `main`, once the maintainers have confirmed the
   tag convention:

   ```sh
   git switch main && git pull
   git tag -a android-v0.2.0 -m "Monarch for Android 0.2.0"
   git push origin android-v0.2.0
   ```

   Check that the tag, the version in the build and the changelog heading
   are the same string. Tags are never moved or deleted.

8. Write the GitHub release notes from the changelog entry.

The full checklist and the ownership rules are in
[release artefacts and their verification](docs/governance/native-release-and-maintenance.md#release-artefacts-and-their-verification).

### Web releases

The npm packages are released by Changesets and `release.yml`:
`npm run version-packages` on a `release/<version>` branch, merged into `uat`
and then into `main`. See [branching](docs/governance/branching.md#how-a-change-travels).
A token change reaches native only when a platform releases with the
regenerated file.

## 11. Roll back a failed upgrade

A failed upgrade is reverted, not patched forward under time pressure.

**Before merge.** Close the pull request and delete the branch. Record in the
pull request why it failed and what would have to change.

**After merge into `uat`, before release.**

```sh
git switch uat && git pull
git switch -c chore/revert-<what>
git revert -m 1 <merge commit sha>   # or: git revert <sha> for a squash merge
```

Open the revert as a pull request into `uat` and run the full pipeline on it.
Reverting restores every file the upgrade changed: for Gradle, that is
`gradle-wrapper.properties` (URL and checksum), `gradle-wrapper.jar`,
`gradlew` and `gradlew.bat` together.

After switching back, clear local state that the newer toolchain wrote:

```sh
# from the repository root
(cd platforms/android && ./gradlew --stop && rm -rf build */build build-logic/build .gradle .kotlin)
(cd platforms/ios && rm -rf .build)
```

**After a release.** Never move or delete a tag and never reuse a version.
Release the previous behaviour as a new patch version with the revert, and
list the reverted release under `Known issues` with the version that fixes
it ([known issues](docs/governance/native-release-and-maintenance.md#known-issues)).
Applications that already took the bad release go back to the previous one
by pointing their checkout or local repository at the previous tag.

**Dependabot.** If a grouped update broke the build, close the pull request
and add an `ignore` entry for that dependency and version to
`.github/dependabot.yml` with a comment and the issue link, then remove the
entry when the fix is released.

## 12. Migrate Rook applications to a new Monarch release

No Rook application uses native Monarch yet. This is the procedure for when
one does.

1. Read the platform's `CHANGELOG.md` from the application's current version
   to the target, every `Deprecated`, `Removed` and breaking `Changed` entry,
   and the migration guides they link to
   ([template](docs/governance/native-api-stability.md#migration-guide-template)).
2. Check the application against the target's row in
   [NATIVE-COMPATIBILITY](docs/architecture/NATIVE-COMPATIBILITY.md):
   - Android: the app's `minSdk` is at least Monarch's `minSdk`, its
     `compileSdk` at least Monarch's, its Kotlin new enough to read
     Monarch's metadata, and its Compose BOM no older than Monarch's
     (`monarch-compose` brings Compose UI, Foundation and Material 3 as
     `api` dependencies).
   - iOS: the app's deployment target is at least Monarch's, and its Xcode
     reads Swift tools version 6.0.
3. Get the release:
   - Android: check out the release tag of this repository, run
     `./gradlew publishAllPublicationsToLocalRepository` in
     `platforms/android`, and change the version in the app's dependency on
     `dev.monarchui:monarch-compose`.
   - iOS: check out the release tag; the app's local package reference to
     `platforms/ios` then builds the new version.
4. Build the app. Fix every Monarch deprecation warning the compiler
   reports; deprecated API keeps working for at least one minor version, so
   this can be spread over releases, but not past the earliest removal
   version named in the changelog.
5. Run the app's own tests, then the
   [manual screen reader checklist](docs/guides/native-accessibility-and-testing.md#manual-screen-reader-checklist)
   on the screens that use changed components.
6. Ship the upgrade in its own application pull request, separate from
   feature work, so it can be reverted alone.

Applications pin an exact Monarch version and upgrade by pull request, the
same rule as on the web ([channels](docs/governance/versioning.md#channels)).

## Related

- [NATIVE-PLATFORMS](docs/architecture/NATIVE-PLATFORMS.md)
- [NATIVE-COMPATIBILITY](docs/architecture/NATIVE-COMPATIBILITY.md)
- [NATIVE-COMPONENTS](docs/architecture/NATIVE-COMPONENTS.md)
- [Native release and maintenance](docs/governance/native-release-and-maintenance.md)
- [Native platform evolution](docs/governance/native-platform-evolution.md)
- [Native API stability](docs/governance/native-api-stability.md)
- [Contributing to the native platforms](docs/guides/native-contributing.md)
- [Accessibility and testing](docs/guides/native-accessibility-and-testing.md)
- [`platforms/android/README.md`](platforms/android/README.md)
- [`platforms/ios/README.md`](platforms/ios/README.md)
