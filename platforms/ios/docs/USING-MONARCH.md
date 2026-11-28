# Using Monarch for iOS

For people who add Monarch to an app. For the commands to build and test this
repository, see the [README](../README.md); for adding a component, see
[ADDING-COMPONENTS](ADDING-COMPONENTS.md).

Monarch for iOS looks like Monarch and behaves like iOS. The rule, the finish
rules every component follows, and the icon, font, pattern and time zone
policies are shared with Android and are in
[the Monarch look on Android and iOS](../../../docs/guides/native-look-and-feel.md).
This page is what is specific to iOS.

## Installation

The native version is the `@rookcom/monarch-react` version, in the `0.14.x`
patch series. Each release is a plain `0.14.N` git tag on
`https://github.com/RookHQ/monarch`, which Swift Package Manager reads through
the `Package.swift` at the root of the repository (generated from
`platforms/ios/Package.swift`; a check fails when they disagree). Replace
`0.14.N` below with a tag listed in the [changelog](../CHANGELOG.md); the first
native release is `0.14.6`.

In Xcode: File > Add Package Dependencies, enter the repository URL, choose
"Up to Next Minor Version" from the tag, and add the products you use. In a
package:

```swift
dependencies: [
    .package(url: "https://github.com/RookHQ/monarch", .upToNextMinor(from: "0.14.N"))
],
targets: [
    .target(
        name: "App",
        dependencies: [
            .product(name: "MonarchUI", package: "monarch"),
        ]
    )
]
```

| Product               | What it is                                                                                   |
| --------------------- | -------------------------------------------------------------------------------------------- |
| `MonarchUI`           | The SwiftUI theme, foundations and components. Brings `MonarchTokens`                        |
| `MonarchTokens`       | Generated token constants. Foundation only                                                   |
| `MonarchFontsPoppins` | Optional. The Poppins typeface (SIL Open Font License 1.1). See [Poppins](#poppins-optional) |

iOS 17 or later, Swift tools 6.0. Before a tag exists, add the
`platforms/ios` folder as a local package ([README](../README.md#installing)).

Install the theme once at the root of the app:

```swift
WindowGroup {
    ContentView().monarchTheme()
}
```

## Poppins (optional)

The default typeface is the system font, San Francisco. Poppins is a separate
product, so an app that does not link it pays no size cost.

```swift
import MonarchFontsPoppins
import MonarchUI

WindowGroup {
    ContentView().monarchTheme(typography: .poppins)
}
```

`MonarchTypography.poppins` registers the bundled font files with the process
on first use, so no `UIAppFonts` entry is needed. The files are under the SIL
Open Font License 1.1; `MonarchPoppins.licenseURL` points at the licence text
in the bundle, and an app that ships the font should keep it with its own
notices. Rook products, the docs previews and the review sheets use Poppins.

## Icons

The libraries bundle no icon files. `MonarchIcon` draws SF Symbols by name,
and a component takes a symbol name or a view from the app. SF Symbols are
for app interfaces on Apple platforms only; the sample app and the previews
use them in the regular weight. Nothing in the package is an icon set.

## Experimental API

An experimental declaration is `@_spi(Experimental) public`. It is visible
only to a file that opts in:

```swift
@_spi(Experimental) import MonarchUI
```

It can change in any release without the deprecation window, never appears in
a stable signature, and is left out of the stable API baseline. The experimental
components are the inventory entries with `"status": "experimental"`
(`platforms/ios/inventory/<id>.json`; the counts are in
[NATIVE-COMPONENTS](../../../docs/architecture/NATIVE-COMPONENTS.md)), and their
declarations are listed in `platforms/ios/api/MonarchUI.experimental.txt`.
DocC hides them, so the docs site shows them on the component pages. They are
the domain composites of the patterns layer
([ADR 0017](../../../docs/adr/0017-native-patterns-layer.md)).

## Time zones

A component parameter that takes a time zone is a Foundation `TimeZone?`.
`nil`, or the default, means `TimeZone.autoupdatingCurrent`, read each time. A
value that is stored or sent between processes, such as
`MonarchBusinessHoursValue.timeZone`, keeps the IANA identifier as a `String`.
On Android the component parameter is the IANA `String?`; see
[the table](../../../docs/guides/native-look-and-feel.md#time-zones).

## Accessibility

Every component has a touch target of at least 44 by 44 points, scales with
Dynamic Type including the accessibility sizes, uses leading and trailing
edges, selects the high-contrast themes with Increase Contrast, and removes
motion with Reduce Motion. The contract and how it is tested are in
[accessibility and testing](../../../docs/guides/native-accessibility-and-testing.md).
Checks that need a device with VoiceOver are in
[device checks](../../../docs/guides/native-device-checks.md); until they have
been run, no VoiceOver output is claimed as verified.

## Reference

- The generated API reference (DocC) is published at `/ios/api/` on the docs
  site. It covers the stable API.
- Every component has a page on the docs site with Web, iOS and Android tabs.
  The iOS tab shows the preview image (light and dark) from
  `platforms/ios/previews/<id>.<theme>.png` when one has been recorded.
- The stable public API is recorded in `platforms/ios/api/` and checked on
  every build; the deprecation rules are in
  [native API stability](../../../docs/governance/native-api-stability.md).
