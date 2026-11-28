# Using Monarch for Android

For people who add Monarch to an app. For the commands to build and test this
repository, see the [README](../README.md); for adding a component, see
[ADDING-COMPONENTS](ADDING-COMPONENTS.md).

Monarch for Android looks like Monarch and behaves like Android. The rule, the
finish rules every component follows, and the icon, font, pattern and time zone
policies are shared with iOS and are in
[the Monarch look on Android and iOS](../../../docs/guides/native-look-and-feel.md).
This page is what is specific to Android.

## Installation

The native version is the `@rookcom/monarch-react` version, in the `0.14.x`
patch series. Maven uses the same `0.14.N`. Replace `0.14.N` below with the
version in the [changelog](../CHANGELOG.md) (the first native release is
`0.14.6`).

| Artifact                                          | What it is                                                                                   |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `dev.monarchui:monarch-compose:0.14.N`       | The theme, foundations, accessibility helpers and components. Brings `monarch-tokens`        |
| `dev.monarchui:monarch-tokens:0.14.N`        | Generated token constants. Pure Kotlin, no Android and no Compose                            |
| `dev.monarchui:monarch-fonts-poppins:0.14.N` | Optional. The Poppins typeface (SIL Open Font License 1.1). See [Poppins](#poppins-optional) |

```kts
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// the module's build.gradle.kts
dependencies {
    implementation("dev.monarchui:monarch-compose:0.14.N")
}
```

The requirements are `minSdk` 24, `compileSdk` 37 and Java 17 bytecode; see the
[README](../README.md#requirements). Until the artifacts are on Maven Central,
publish them to a local repository and add that repository
([README](../README.md#install-from-source)). Whether a release is on Maven
Central is stated in the changelog entry of the version.

Then wrap the content of the activity once:

```kotlin
MonarchTheme {
    // Monarch components
}
```

## Poppins (optional)

The default typeface is the system font, Roboto. Poppins is a separate
artifact, so an app that keeps the system font pays no size cost.

```kts
dependencies {
    implementation("dev.monarchui:monarch-fonts-poppins:0.14.N")
}
```

```kotlin
MonarchTheme(typography = monarchPoppinsTypography()) {
    // Monarch components, set in Poppins (regular, medium and semibold)
}
```

`MonarchPoppins` is the `FontFamily` itself, for text the app sets by hand.
The code roles keep the monospace face. The font files are under the SIL Open
Font License 1.1; the licence text is in the artifact as the raw resource
`monarch_poppins_ofl`, and an app that ships the font should keep it with its
own notices. Rook products, the docs previews and the review sheets use
Poppins.

## Icons

The libraries bundle no icons; a component takes an `ImageVector`, a `Painter`
or a slot from the app. The sample app and the previews draw Material Icons
(Outlined, Apache License 2.0) through the module `monarch-sample-icons`. It is
a sample and test module: it is not published, no library depends on it, and
an app should bring its own icons.

## Experimental API

Declarations marked `@ExperimentalMonarchApi` can change or be removed in any
release, without the deprecation window. Using one is a compile error until
the caller opts in:

```kotlin
@OptIn(ExperimentalMonarchApi::class)
@Composable
fun Home() {
    // an experimental component
}
```

The annotation is applied to the experimental components, which are the
inventory entries with `"status": "experimental"`
(`platforms/android/inventory/<id>.json`; the counts are in
[NATIVE-COMPONENTS](../../../docs/architecture/NATIVE-COMPONENTS.md)). They
are the domain composites of the patterns layer
([ADR 0017](../../../docs/adr/0017-native-patterns-layer.md)). A stable
component's public signature never mentions an experimental type.

## Time zones

A component parameter that takes a time zone is the IANA identifier as a
`String?`. `null` means the device zone, read each time (through
`currentIcuTimeZone()`). A value that is stored or sent between processes, such
as `MonarchBusinessHoursValue.timeZone`, is the same IANA `String`. On iOS the
component parameter is a Foundation `TimeZone`; see
[the table](../../../docs/guides/native-look-and-feel.md#time-zones).

## Accessibility

Every component has a touch target of at least 48 dp, exposes its role, state
and label to TalkBack, scales with the font size setting (checked at 2.0),
mirrors in right to left, and takes colour from the theme roles, with the
high-contrast themes selected by the system contrast setting. The contract and
how it is tested are in
[accessibility and testing](../../../docs/guides/native-accessibility-and-testing.md).
Checks that need a device with TalkBack are in
[device checks](../../../docs/guides/native-device-checks.md); until they have
been run, no TalkBack output is claimed as verified.

## Reference

- The generated API reference of both libraries (Dokka) is published at
  `/android/api/` on the docs site.
- Every component has a page on the docs site with Web, iOS and Android tabs.
  The Android tab shows the preview image (light and dark) from
  `platforms/android/previews/<id>.<theme>.png` when one has been recorded.
- The public API is recorded in `monarch-compose/api/` and checked on every
  build; the deprecation rules are in
  [native API stability](../../../docs/governance/native-api-stability.md).
