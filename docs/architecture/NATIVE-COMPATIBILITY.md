# Native compatibility matrix

The toolchains, SDKs and operating systems that Monarch for Android
(`platforms/android`) and Monarch for iOS (`platforms/ios`) are built and
tested with. Monarch claims support only for what is in this page. A version
that is not listed is not supported, even if it happens to work.

Both platforms share one version, the `@rookcom/monarch-react` version, and
each has its own rows. The architecture is in
[NATIVE-PLATFORMS](NATIVE-PLATFORMS.md); the rules for changing these values
are in [native release and maintenance](../governance/native-release-and-maintenance.md#upgrade-process)
and [platform evolution](../governance/native-platform-evolution.md#raising-a-minimum-os-version).

## How to read this page

- **Declared** is what the build files state: the version a build uses, or
  the minimum an app needs. It is read from the file named in the row.
- **Tested here** is what the last full local run of the platform's checks
  actually used. It is narrower than "declared" and is the only range
  Monarch supports.
- **Not tested** lists what nobody has run yet. It is not a support claim in
  either direction.

## Monarch native 0.14.6 (unreleased)

Neither platform has been released. Both rows describe the development track
([release tracks](../governance/native-release-and-maintenance.md#release-tracks)).
The "tested here" values come from local runs on one Mac (Apple silicon,
macOS 26), made while the native version was still numbered `0.1.0`. The
native CI jobs become a second source once they have run green (see
[Updating this page](#updating-this-page)).

### Android

| Item                                | Declared                                                                                                         | Tested here                                        | Source                                                                            |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | --------------------------------------------------------------------------------- |
| Monarch for Android                 | `0.14.6` (the `@rookcom/monarch-react` version), group `dev.monarchui`                                      | `0.1.0`, the number before the version was unified | `[versions] monarch` in `platforms/android/gradle/libs.versions.toml`             |
| Kotlin (compiler and Gradle plugin) | `2.4.21`. Language and API version are the compiler's defaults; none is set                                      | `2.4.21`                                           | `[versions] kotlin` in `libs.versions.toml`                                       |
| Kotlin in Android modules           | The Android Gradle plugin's built-in Kotlin; `org.jetbrains.kotlin.android` is not applied                       | As declared                                        | `build-logic/src/main/kotlin/monarch.android.library.gradle.kts`                  |
| Compose compiler plugin             | `2.4.21` (same version as Kotlin, by construction)                                                               | `2.4.21`                                           | `plugin-compose-compiler` in `[libraries]`, `version.ref = "kotlin"`              |
| Android Gradle plugin               | `9.5.1`                                                                                                          | `9.5.1`                                            | `[versions] agp`                                                                  |
| Gradle                              | `9.9.1` (wrapper, distribution checksum pinned)                                                                  | `9.9.1`                                            | `distributionUrl` in `platforms/android/gradle/wrapper/gradle-wrapper.properties` |
| JDK toolchain                       | 21. The build does not download a JDK; one must be installed                                                     | OpenJDK 21.0.13 (Homebrew build)                   | `[versions] jdk`; `platforms/android/README.md` ("Requirements")                  |
| Bytecode target                     | Java 17                                                                                                          | Java 17                                            | `[versions] jvmTarget`                                                            |
| Compose BOM                         | `2026.09.00`                                                                                                     | `2026.09.00`                                       | `[versions] composeBom`                                                           |
| Compose UI, Foundation, Runtime     | From the BOM                                                                                                     | `1.12.1`                                           | Resolved from the `compose-bom` 2026.09.00 POM                                    |
| Material 3                          | From the BOM                                                                                                     | `1.4.0`                                            | Resolved from the `compose-bom` 2026.09.00 POM                                    |
| Material 3 adaptive                 | From the BOM                                                                                                     | `1.3.0`                                            | Resolved from the `compose-bom` 2026.09.00 POM                                    |
| Compile SDK                         | 37. Needs the `android-37.0` platform and build-tools 36.0.0 installed                                           | `android-37.0`, build-tools 36.0.0                 | `[versions] compileSdk`; `platforms/android/README.md`                            |
| Target SDK (sample app)             | 37                                                                                                               | 37                                                 | `[versions] targetSdk`                                                            |
| Minimum SDK                         | 24 (Android 7.0)                                                                                                 | Not run on API 24; see below                       | `[versions] minSdk`                                                               |
| Tests run on                        | Robolectric `4.18` on the JVM. No SDK level is configured, so Robolectric chooses one from the build             | JVM only. No emulator, no device                   | `[versions] robolectric`                                                          |
| Android Studio                      | Not set by the build. Google lists Android Studio Rabbit 2 (2026.2.2) as the first version that supports AGP 9.5 | Not tested. The checks run from the command line   | Google's AGP and Android Studio compatibility table, read on 2026-11-13           |

Pinned on purpose, not used directly by Monarch code:

| Key in `[versions]` | Value    | Why it is pinned                                                                                                                          |
| ------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `lifecycle`         | `2.11.0` | Compose pulls in `lifecycle-runtime` 2.9.4, whose bundled lint checks the lint of AGP 9.5 cannot load. Applied as a debug-only constraint |
| `espresso`          | `3.7.0`  | `compose-ui-test-junit4` pulls in Espresso 3.5.0, which calls an API Android 14 removed and fails under Robolectric                       |

### iOS

| Item                       | Declared                                                                                                                | Tested here                                          | Source                                                                                   |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Monarch for iOS            | `0.14.6` (the `@rookcom/monarch-react` version)                                                                         | `0.1.0`, the number before the version was unified   | `MonarchUI.version` in `platforms/ios/Sources/MonarchUI/MonarchUI.swift`                 |
| Swift tools version        | `6.0`                                                                                                                   | `6.0`                                                | `// swift-tools-version: 6.0`, line 1 of `platforms/ios/Package.swift`                   |
| Swift language mode        | Swift 6 (complete strict concurrency), selected by the tools version; upcoming feature `ExistentialAny` on every target | Swift 6                                              | `platforms/ios/Package.swift`; `SWIFT_VERSION` in `platforms/ios/Sample/project.yml`     |
| Swift compiler             | Any toolchain that reads tools version 6.0 (Swift 6.0 or later)                                                         | Swift 6.3.3                                          | `platforms/ios/README.md` ("Requirements")                                               |
| Xcode                      | No minimum is written in the repository                                                                                 | Xcode 26.6 (17F113)                                  | `platforms/ios/README.md` ("Requirements")                                               |
| iOS SDK                    | Whatever the Xcode in use provides                                                                                      | iOS Simulator SDK 26.5                               | `platforms/ios/README.md` ("Requirements")                                               |
| iOS deployment target      | iOS 17.0                                                                                                                | Built for iOS 17.0; run only on iOS 26.5             | `platforms:` `.iOS(.v17)` in `Package.swift`; `deploymentTarget` in `Sample/project.yml` |
| macOS                      | macOS 14 in the manifest, as a build convenience for `swift build` and `swift test`. Not supported                      | Host tests on macOS 26                               | `platforms:` `.macOS(.v14)` in `Package.swift`                                           |
| Tests run on               | Swift Testing on the Mac host, and on the simulator named by `MONARCH_IOS_DESTINATION`                                  | iPhone 17 Pro simulator, iOS 26.5                    | `platforms/ios/Scripts/verify.sh`; `platforms/ios/README.md`                             |
| Sample app tooling         | XcodeGen, any version that reads `Sample/project.yml`                                                                   | The XcodeGen installed with Homebrew on the test Mac | `platforms/ios/Sample/project.yml`                                                       |
| Swift Package dependencies | None                                                                                                                    | None                                                 | `dependencies` (absent) in `Package.swift`                                               |

## Known version couplings

Versions that cannot move on their own. The upgrade steps for each are in the
[upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md).

- **Android Gradle plugin and Gradle.** AGP 9.5 needs Gradle 9.6.0 or later
  and supports compile SDK up to API 37. Google's tables are the source.
- **Android Gradle plugin and Kotlin.** AGP 9 has built-in Kotlin and
  depends at runtime on Kotlin Gradle plugin 2.2.10, the minimum version
  built-in Kotlin supports. A newer Kotlin Gradle plugin on the build
  classpath, as here, replaces it.
- **Kotlin and the Compose compiler.** One catalog key, `kotlin`, sets both.
- **Compose BOM and AndroidX.** The BOM decides the Compose and Material 3
  versions, and their transitive AndroidX versions. The `lifecycle` and
  `espresso` pins above exist because of what BOM 2026.09.00 pulls in.
- **AGP 9.5 and unit tests.** `android.onlyEnableUnitTestForTheTestedBuildType=false`
  in `platforms/android/gradle.properties` keeps the release unit test
  component that the AGP 9.5.1 Compose Preview task needs; the convention
  plugins switch `testReleaseUnitTest` off again.
- **Build plugins and Gradle 10 and 11.** The Gradle 9.9.1 problems report
  shows detekt 1.23.8 calling `ReportingExtension.file(String)`, removed in
  Gradle 10, and both detekt and binary-compatibility-validator 0.18.2
  calling `Configuration.setVisible(boolean)`, removed in Gradle 11. Neither
  Gradle major can be adopted until those plugins are upgraded or replaced.
- **Xcode, Swift and the iOS SDK** move together: an Xcode release fixes the
  Swift compiler and the SDK.

## Not tested

Nothing in this list is claimed to work or to fail.

- **Android devices and emulators.** Every Android test runs on the JVM under
  Robolectric. No test has run on an emulator or a physical device, at any
  API level. API 24, the declared minimum, has not been exercised.
- **TalkBack and VoiceOver.** No screen reader has been used on either
  platform ([accessibility and testing](../guides/native-accessibility-and-testing.md#not-yet-verified)).
- **iOS versions other than 26.5.** The iOS 17 deployment target is compiled
  against but no test has run on an iOS 17 or iOS 18 runtime.
- **iPad, Mac Catalyst, visionOS and macOS apps.** Not tested. macOS appears
  in the manifest only so that host tests run.
- **Physical iPhones.** Not tested.
- **Xcode versions other than 26.6**, including the Xcode 16 line that the
  Swift 6.0 tools version would in principle allow.
- **JDKs other than 21**, and Android Studio of any version.
- **Consuming apps.** No Rook application builds against either platform yet.

## Support status of new releases

When a new OS, SDK, Xcode, Kotlin, Gradle or AGP version is released, its
status here is one of:

| Status          | Meaning                                                    |
| --------------- | ---------------------------------------------------------- |
| `not evaluated` | Nobody has built or tested Monarch with it yet             |
| `tested`        | The full checks passed with it; it is in a row above       |
| `known issues`  | It works with the issues linked next to it                 |
| `not supported` | It does not work, or Monarch has decided not to support it |

No new release is under evaluation.

## IDE versions

| IDE            | Supported                                                                                                                                                  |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Xcode          | The Xcode in the iOS "tested here" row (26.6). Other versions are not tested                                                                               |
| Android Studio | None is tested; the checks run from the command line. Use a version that Google lists as supporting the Android Gradle plugin in the Android row (AGP 9.5) |

An IDE is a tool for working on Monarch or on an app, not a dependency of the
libraries. An app's own IDE needs only to support the toolchain versions in
the rows above.

## Updating this page

- The pull request that changes a version in `libs.versions.toml`, the Gradle
  wrapper, `Package.swift`, `Sample/project.yml` or the Xcode used to test
  updates the matching row in the same pull request. The
  [upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md) lists the row for
  each change.
- "Tested here" is filled from what the run actually printed: the
  `Toolchain` step of `platforms/ios/Scripts/verify.sh` prints the Xcode,
  Swift and swift-format versions; `./gradlew --version` in
  `platforms/android` prints Gradle, Kotlin and the JVM. Never copy a value
  from release notes into "tested here".
- Each release adds a section with its own version and keeps the previous
  ones, so an app can find the toolchain of the release it uses.
- The quarterly compatibility check
  ([calendar](../governance/native-release-and-maintenance.md#maintenance-calendar))
  re-reads every row and updates the support status of new releases.
- When the native CI workflow (`.github/workflows/native.yml`) has run, its
  runner image and toolchain become a second "tested here" source; record
  them here only after a green run.

## Related

- [NATIVE-PLATFORMS](NATIVE-PLATFORMS.md)
- [NATIVE-COMPONENTS](NATIVE-COMPONENTS.md)
- [Native release and maintenance](../governance/native-release-and-maintenance.md)
- [Native platform evolution](../governance/native-platform-evolution.md)
- [Upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md)
