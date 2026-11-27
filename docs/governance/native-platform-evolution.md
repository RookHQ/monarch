# Native platform evolution

Android and Apple change their UI APIs, interaction patterns and accessibility
features every year. This page is the procedure for taking those changes into
Monarch for Android and Monarch for iOS without rebuilding either, and the
rules that keep that possible.

Monarch does not promise that a release built today works unchanged on an OS
released later. It promises a process: each new OS and toolchain is tested,
its support status is published in
[NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md), and changes
arrive through normal releases. The architecture this relies on is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md).

## Adopting a new platform API

Use this procedure when Android, Jetpack Compose, Material 3, iOS or SwiftUI
introduces a capability that Monarch could use: a new control, a new modifier,
a new accessibility setting, a new navigation or window behaviour.

Record the outcome of steps 1 to 4 in an issue before writing code. The issue
is the evidence that the adoption was evaluated, whatever the decision.

1. **Investigate the official documentation.** Read the platform's API
   reference, release notes and design guidance for the capability (Android
   Developers and the Material 3 guidelines; Apple's developer documentation
   and the Human Interface Guidelines). Note its stability: stable, or
   experimental, beta or preview. Blog posts and samples are leads, not
   sources.
2. **Decide whether it improves an existing Monarch implementation.** Name
   the component, style or helper it would change and what gets better for
   the person using the app. If Monarch has a class C (custom) component and
   the platform now provides the control, the default answer is to move to
   the platform control and reclassify the component as A or B
   ([classification](../architecture/NATIVE-PLATFORMS.md#components)). If
   nothing in Monarch improves, stop here (see
   [When not to adopt](#when-not-to-adopt)).
3. **Evaluate the minimum OS.** Find the first OS version and SDK level that
   has the API, and the toolchain needed to compile against it. Compare with
   Monarch's minimum versions. If the API is newer than the minimum, a
   fallback is required (step 7); raising the minimum to avoid one is a
   separate decision ([Raising a minimum OS version](#raising-a-minimum-os-version)).
4. **Assess accessibility and performance.** Check the new behaviour with
   TalkBack or VoiceOver, at the largest text size, right to left, with
   reduced motion and with the contrast settings, and with a keyboard or
   switch where the component is focusable. Check that it does not add
   recompositions, layout passes or allocations on a hot path. An API that is
   worse for accessibility than the current implementation is not adopted.
5. **Test the new implementation.** Add tests for the new path and keep the
   tests for the old one. Both paths run in the test suite, on an OS version
   on each side of the boundary. Add or update previews.
6. **Introduce it behind a compatibility boundary.** The public Monarch API
   does not change. Inside, an availability check chooses the new
   implementation where the OS has it
   ([examples](#compatibility-boundaries)). If the public API has to change,
   that is a separate, additive change under
   [native API stability](native-api-stability.md), and it starts as
   experimental when its shape is uncertain.
7. **Preserve fallback behaviour for older supported OS versions.** Every OS
   version in the supported range keeps working, with the previous
   implementation or an equivalent one. The fallback is the same component to
   the app: same API, same accessibility contract. It may look or animate
   differently, and the documentation says so.
8. **Update the documentation and examples.** The component's inventory entry
   and documentation state the platform difference and from which OS version
   it applies. Update the compiled examples, the changelog (`Changed`) and,
   if the tested range changed, the compatibility table.
9. **Release through the normal process.** No special release. The change
   ships in the next release of that platform under the usual version rules
   ([native release and maintenance](native-release-and-maintenance.md)).
   Adopting an API on one platform never requires a release of the other.

The same nine steps apply to a new accessibility setting or system behaviour
that has no API to call but changes what a correct component does.

Do not keep imitating old platform behaviour once the platform has a better
approach and the supported range allows it. A fallback exists for the OS
versions that need it, not as the permanent implementation.

## When not to adopt

A new API existing is not a reason to use it. Do not adopt when:

- **Nothing improves for the person using the app.** The result would be the
  same control with newer code.
- **The API is not stable.** Alpha, beta, preview and vendor-experimental
  APIs stay out of stable Monarch. They can be evaluated on a branch, or
  offered as experimental Monarch API if they are isolated and recorded.
- **It is worse for accessibility**, or its accessibility behaviour cannot be
  verified yet.
- **It would change the public Monarch API** for a small gain. Prefer the
  platform's control inside the existing API.
- **It serves one product.** A capability one product needs stays in that
  product, built from Monarch foundations, until the promotion rule is met
  ([component governance](COMPONENT-GOVERNANCE.md#promotion-rule)).
- **It needs a new dependency** that the platform SDK, AndroidX or Compose
  does not already provide. That needs its own justification
  ([Rules that prevent upgrade lock-in](#rules-that-prevent-upgrade-lock-in)).
- **Only a small part of the supported range would get it**, and the fallback
  would be most of the work with little of the benefit. Record it and revisit
  when the minimum OS rises.
- **It would make Android look like iOS, or iOS like Android,** or copy a web
  pattern onto either.
- **It exists on the web.** A web component existing is not a reason to build
  a native one.

A decision not to adopt is recorded in the issue with the reason and the
condition that would reopen it, so the evaluation is not repeated every year.

## Compatibility boundaries

A compatibility boundary is the single place where Monarch chooses between
two implementations by OS version. Callers above it see one API.

Rules:

- The check lives in one function, as low in the layers as possible. It is
  not repeated at call sites.
- The newer branch is in a function annotated with the API level it needs
  (`@RequiresApi` on Android, `@available` on iOS), so the compiler and lint
  verify that it is only reached through the check.
- The check tests the OS version that introduced the API. It does not test
  device models, manufacturers or version strings.
- The decision logic is separated from the platform call where that makes it
  testable without a device.
- The boundary is `internal` or `private`. A public Monarch API never requires
  an app to write an availability check to use it in its documented way.
- Every boundary names the condition under which it disappears: when the
  minimum OS reaches the version in the check.

Android, with `Build.VERSION.SDK_INT`. This is how Monarch reads the system
contrast setting, which exists from Android 14 (API 34); the code is
`theme/SystemContrast.kt` in `monarch-compose`, shortened:

```kotlin
/**
 * Whether the user has asked the system for more contrast. The setting exists
 * from Android 14 (API 34); on earlier versions this returns `false`.
 */
@Composable
public fun isSystemInHighContrast(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isHighContrastLevel(systemContrast())
    } else {
        false
    }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun systemContrast(): Float {
    // Reads UiModeManager.getContrast() and follows changes.
}

/** Pure, so the rule is tested without a device. */
internal fun isHighContrastLevel(contrast: Float): Boolean = contrast >= 0.5f
```

iOS, with `if #available`. This example illustrates the pattern and is not
Monarch source: a sheet sizing API that exists from iOS 18, with the earlier
behaviour kept for iOS 17.

```swift
extension View {
    /// Sizes a sheet as a form where the system supports it.
    /// Compatibility boundary: remove the check when the deployment target
    /// is iOS 18 or later.
    @ViewBuilder
    func formSheetSizing() -> some View {
        if #available(iOS 18.0, *) {
            self.presentationSizing(.form)
        } else {
            self
        }
    }
}
```

A declaration that only exists on newer systems is marked instead of checked:
`@available(iOS 18.0, *)` on the Swift declaration, `@RequiresApi(35)` on the
Kotlin one. Such a declaration is not public Monarch API unless there is no
reasonable fallback, and then the documentation states the requirement.

Where the two implementations are large, each lives in its own file behind a
common internal function. This is how Monarch keeps more than one
implementation version of a component alive during a transition without
exposing either.

## Compatibility workarounds

A workaround is code that exists only because of a defect or gap in a
platform, a toolchain or a dependency: a Compose bug in one BOM, a SwiftUI
layout defect in one iOS release, a Gradle plugin flag. A fallback for an
older supported OS is not a workaround; it is a boundary (above).

### Isolate

- One workaround, one place. It is not copied into each component that needs
  it; components call a shared internal helper.
- It is `internal` or `private` and never part of public API.
- It is as narrow as the defect: limited to the affected OS or library
  versions with an availability check where the defect is version-specific.
- Build-level workarounds (a Gradle property, a compiler flag) sit next to a
  comment in the file that sets them. `platforms/android/gradle.properties`
  shows the form: the property that works around an Android Gradle Plugin
  behaviour is preceded by the reason and the plugin version.

### Document

Every workaround carries a comment in this form, directly above the code:

```text
MONARCH-COMPAT: <what this works around, in one line>
Reason: <the defect or gap, with a link to the vendor issue or release note>
Applies to: <OS, toolchain or dependency versions affected>
Remove when: <the condition, stated so that it can be checked>
```

The `MONARCH-COMPAT` marker is the register: searching the platform folders
for it lists every workaround. The convention is accepted for both platforms;
existing comments are brought to this form as their files are touched. A
workaround that apps can observe is also listed as a known issue in the
changelog ([known issues](native-release-and-maintenance.md#known-issues)).

The same marker names behaviour changes in the changelog: every entry that
changes what an app can observe carries a one-line `MONARCH-COMPAT:` note
saying what changed and what an app does about it
([changelog standard](native-release-and-maintenance.md#changelog-standard)).

A "Remove when" condition is one of: a minimum OS version is reached; a
dependency version at or above the fix is the minimum Monarch supports; a
toolchain version is the oldest supported. "When no longer needed" is not a
condition.

### Remove

- The quarterly compatibility check
  ([calendar](native-release-and-maintenance.md#maintenance-calendar)) lists
  every marker and tests each removal condition.
- Every upgrade pull request and every minimum-OS change does the same for
  the markers it could affect, and removes the ones whose condition is now
  met in the same pull request or the next one.
- Removal deletes the code, the comment and any test that existed only for
  the workaround, and is listed in the changelog only if apps can observe it.
- A workaround whose vendor fix never arrives is reviewed once a year: it is
  either accepted as the implementation, and rewritten as ordinary code with
  a normal comment, or the feature that needs it is reconsidered.

## Deprecated platform APIs

- Both builds treat warnings as errors, so a platform deprecation that
  reaches Monarch's code fails the build when the toolchain that introduces
  it is adopted.
- The fix is to move to the replacement, behind a compatibility boundary if
  the replacement is newer than the minimum OS.
- If the replacement cannot be adopted yet, the deprecated call is isolated
  in one function with a `MONARCH-COMPAT` comment whose removal condition is
  the replacement becoming usable. A suppression, where the language needs
  one to compile that single function, is scoped to it and sits under the
  comment. Suppressing at file, module or build level is not allowed.
- Deprecations with a removal date announced by the vendor are tracked as
  known issues with that date, and the upgrade process plans for them before
  the removal ([upgrade process](native-release-and-maintenance.md#upgrade-process)).

## Rules that prevent upgrade lock-in

These rules are why a Kotlin upgrade does not touch Swift, a new SwiftUI
control does not need a token release, and a token change does not need a
component rewrite.

**Platform independence**

- `platforms/android` and `platforms/ios` build, test and release on their
  own. Neither imports, generates or reads anything from the other.
- Neither needs Node to build, because the generated token files are
  committed.
- One platform's upgrade never requires a change to the other. A pull request
  that has to touch both for a toolchain reason is a sign that something is
  coupled that should not be.
- Versions, changelogs and release tags are per platform.

**Tokens separate from rendering**

- The token modules contain constants only and import no UI framework:
  `monarch-tokens` is pure Kotlin for the JVM with no Android or Compose
  dependency, and the Swift `MonarchTokens` target imports no UI framework.
- Tokens are mapped to platform types in exactly one layer, the theme.
  Material 3 is reached through one mapping function, so a Material change is
  absorbed in one file.
- Components read the theme, never the generated primitives for colour, and
  never literals ([native tokens](native-tokens.md#the-rule-for-components)).
- A design change is a token change; a platform change is a rendering change.
  Neither forces the other.

**No cross-platform runtime**

- There is no shared runtime, no shared UI framework and no code shared
  between Kotlin and Swift. What the platforms share is data (tokens) and
  rules (names, vocabulary, accessibility requirements, governance).
- No web technology is embedded in a native component.
- Kotlin Multiplatform, and any other technology that would make one
  platform's toolchain a dependency of the other, is not introduced without
  an ADR that shows how both platforms stay independently upgradable.

**Minimal dependencies**

- Android depends on the platform SDK, AndroidX and Compose only. iOS depends
  on the Apple SDK only; the Swift package has no third-party dependency.
- A new dependency needs the justification in the
  [dependency policy](dependencies.md#adding-a-dependency): what it does, why
  Monarch should not do it itself, how it is maintained, and an
  licence compatible with the Apache License 2.0. For native, add: what happens to Monarch if the
  dependency stops being maintained or lags behind a platform release.
- Build and test dependencies are held to the same question, because a build
  plugin that lags a Kotlin or Xcode release blocks the upgrade as surely as
  a runtime dependency does.
- Versions are written once: in `platforms/android/gradle/libs.versions.toml`
  and in `platforms/ios/Package.swift`. No version appears in a module's
  build file or in source.

**Stable abstractions only**

- Components are built on the platform's stable public API. Compiler
  internals, reflection into platform classes, private selectors and
  undocumented behaviour are not used.
- An API that its vendor marks experimental (an `@Experimental…Api` opt-in in
  Compose or Material 3, an underscored Swift attribute) is used only when
  there is no stable alternative, inside one internal function, with a
  `MONARCH-COMPAT` comment whose removal condition is the API becoming stable
  or being replaced. It never appears in a public Monarch signature. Monarch
  currently has one such use, recorded where it is made: `@_spi` as the iOS
  experimental marker. The Android public API check uses the stable
  binary-compatibility-validator plugin, not the Kotlin Gradle plugin's
  experimental ABI validation.
- Views interoperability on Android and UIKit interoperability on iOS are
  used only where the platform has no Compose or SwiftUI equivalent, behind
  an internal boundary, with the reason recorded.
- Experimental Monarch API is opt-in and never appears in a stable signature
  ([native API stability](native-api-stability.md#experimental-api)).

## Raising a minimum OS version

The minimum supported versions are written in one place per platform: `minSdk`
in `platforms/android/gradle/libs.versions.toml`, and the `platforms:` entry
in `platforms/ios/Package.swift`. They are Android API 24 and iOS 17. The authoritative statement, with the tested range, is
[NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md).

Raising a minimum is a breaking change, because an app that supports the old
version can no longer take the new Monarch release.

When to consider it:

- at the yearly review after both OS releases
  ([calendar](native-release-and-maintenance.md#maintenance-calendar));
- when a platform dependency Monarch needs raises its own minimum (Compose
  and AndroidX do this from time to time; a new Xcode can drop older
  deployment targets);
- when the fallbacks and workarounds kept for the oldest version cost more
  than the users on it justify.

How it is decided and done:

1. **Evidence.** Collect the minimum OS of every Rook application that uses
   or plans to use Monarch on that platform, and the vendor's support
   position for the version. Monarch's minimum is never higher than the
   lowest minimum among the applications it serves, unless those applications
   have agreed a date.
2. **Decide.** A Monarch maintainer and the platform owner decide, in an
   issue that records the evidence, the version, and the release that will
   carry it.
3. **Announce first.** The release before the change states, in its
   changelog, the new minimum and the date from which a release may require
   it, under the same 90-day and two-patch-release window as a deprecation
   ([native API stability](native-api-stability.md#deprecation-policy)).
4. **Change it in one pull request.** Update the one place the minimum is
   written. Remove every compatibility boundary and workaround whose
   condition the new minimum satisfies; this is the main benefit, and leaving
   them behind is not allowed. Update the tests that ran on the old version,
   the compatibility table, the changelog and the documentation.
5. **Release** in a `0.14.N` patch once the window has passed before 1.0, or
   in a major from 1.0, with the change in the first line of the changelog
   entry.

Each platform's minimum is raised on its own schedule. Raising the compile
SDK or the Xcode version used to build is not the same thing and is not
breaking by itself: it follows the
[upgrade process](native-release-and-maintenance.md#upgrade-process). Raising
the Android target SDK changes behaviour and is tested as an adoption of
those behaviour changes.

## Related

- [Native release and maintenance](native-release-and-maintenance.md)
- [Native API stability](native-api-stability.md)
- [Native tokens](native-tokens.md)
- [Native adaptive layout](../guides/native-adaptive-layout.md)
- [Contributing to the native platforms](../guides/native-contributing.md)
- [NATIVE-PARITY](../architecture/NATIVE-PARITY.md)
- [Upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md)
