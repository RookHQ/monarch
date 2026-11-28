# Monarch on Android and iOS: architecture

Status: both platforms cover the 515 web components (see
[Components](#components)) and are not released yet. Nothing is published to
Maven Central or tagged for Swift Package Manager yet; see
[Release status](#release-status).

Monarch has one design language and three implementations: React on the web,
Jetpack Compose on Android and SwiftUI on iOS. The native implementations are
not ports of the React components. They share the tokens, the role names, the
component vocabulary and the accessibility rules, and they render through each
platform's own controls.

## What is shared and what is native

| Shared (one source)                                  | Native (per platform)                          |
| ---------------------------------------------------- | ---------------------------------------------- |
| Token values and names (`packages/tokens`)           | Rendering, layout and motion                   |
| The four themes and every accent                     | Navigation, system bars, back behaviour        |
| Semantic colour roles (`bgCanvas`, `accentSolid`, …) | Dialogs, sheets, menus, pickers                |
| Type roles (`MonarchTextStyle`)                      | Text scaling (sp and font scale, Dynamic Type) |
| Component names, variants, tones and sizes           | Touch targets (48dp on Android, 44pt on iOS)   |
| The adoption rule and the component classification   | Accessibility services (TalkBack, VoiceOver)   |
| Versioning, deprecation and release policy           | Build, test and publishing tooling             |

The two platforms are not required to look identical. The rule is: looks like
Monarch, behaves like the platform. Monarch draws its own fields, buttons,
selection controls, tabs, chips, badges, cards, banners, progress indicators and
list rows in Monarch's colours, type and shape, and the platform keeps presented
surfaces (dialogs, sheets, menus, pickers), text machinery, scrolling and the
accessibility services. The rule and the finish rules are in
[the Monarch look on Android and iOS](../guides/native-look-and-feel.md).

## Repository layout

```text
Package.swift                     generated from platforms/ios/Package.swift, for SwiftPM URLs
packages/tokens/                  token source (TypeScript) and scripts/emit-native.ts
platforms/
  parity.json                     the plan: how each web component is covered on native
  android/                        Gradle build of its own
    gradle/libs.versions.toml     the only place a version is written
    build-logic/                  convention plugins and the inventory checks
    monarch-tokens/               pure Kotlin (JVM). Generated constants, no Android or Compose
    monarch-compose/              Android library: theme, foundation, accessibility, components
      api/monarch-compose.api     the committed public API dump
    sample/                       catalogue app, one page per component
    config/                       static analysis configuration
    inventory/<id>.json           one documentation file per web component id
    components.json               generated from inventory/
    docs/                         ADDING-COMPONENTS.md, BRAND-THEMES.md
    README.md, CHANGELOG.md
  ios/                            Swift package of its own
    Package.swift                 the manifest to edit
    Sources/MonarchTokens/        generated constants, Foundation only
    Sources/MonarchUI/            Theme, Foundations, Accessibility, Internal,
                                  Components/<Family>/<Name>/, Resources, Documentation.docc
    Tests/MonarchTokensTests/
    Tests/MonarchUITests/
    Sample/                       catalogue app (XcodeGen), one page per component
    Scripts/                      verify.sh, generate.py, api.sh and the other checks
    api/                          the public API baseline
    inventory/<id>.json           one documentation file per web component id
    components.json               generated from inventory/
    docs/                         ADDING-COMPONENTS.md, BRAND-THEMES.md
    README.md, CHANGELOG.md
docs/architecture/NATIVE-PLATFORMS.md      this file
docs/architecture/NATIVE-COMPONENTS.md     classification of every component
docs/architecture/NATIVE-PARITY.md         the parity plan explained
docs/architecture/NATIVE-COMPATIBILITY.md  tested toolchain and OS matrix
docs/guides/native-*.md                    adaptive layout, accessibility and testing, contributing
docs/governance/native-*.md                API stability, platform evolution, release, tokens
MONARCH_UPGRADE_PLAYBOOK.md       how to upgrade toolchains and release
```

Each component has its own folder on each platform, grouped by the web
family (`components/<family>/<name>/` on Android,
`Components/<Family>/<Name>/` on iOS), and its own inventory file. Adding a
component adds files only: every shared list (the inventory roll-up, the
compiled examples, the test fixture registry, the sample's navigation) is
generated.

Each platform builds and tests on its own: `platforms/android` needs only a
JDK and the Android SDK, `platforms/ios` needs only Xcode. Neither depends on
Node at build time, because the generated token files are committed.

## Layers

Dependencies point one way. A lower layer never imports a higher one.

```text
1. Tokens        generated constants. No UI framework.      monarch-tokens / MonarchTokens
2. Theme         maps tokens to the platform's theme types. theme/  /  Theme/
3. Foundations   type styles, spacing, shape, elevation,
                 motion, accessibility helpers.             foundation/ / Foundations/
4. Components    read the theme only. Never a raw token
                 colour, never a literal colour or size.    components/<family>/<name>/ / Components/<Family>/<Name>/
```

Patterns (experimental domain composites, such as AI panels and product
screens) are a documentation-level layer above the components: their
inventory files have `"layer": "patterns"`, and the documentation site marks
them "Pattern (experimental)". They live in the same module or target as the
components for now; the module split and its triggers are recorded in
[ADR 0017](../adr/0017-native-patterns-layer.md).

Source rules, checked in review and by static analysis where the tool allows:

- One public component per file, in its own folder with its variants.
- Public API is explicit: `explicitApi()` in Kotlin, `public` written out in
  Swift. Everything else is `internal` or `private`.
- Components take colours from `MonarchTheme`, never from `MonarchTokens`
  primitives and never as literals.
- Built-in strings are localised resources and can be overridden by a parameter.
- No dependency beyond the platform SDK and, on Android, AndroidX and Compose.

## Tokens

`packages/tokens` stays the single source. `packages/tokens/scripts/emit-native.ts`
reads the same modules that produce the CSS and the DTCG files and writes:

- `platforms/android/monarch-tokens/src/main/kotlin/dev/monarchui/tokens/MonarchTokens.kt`
- `platforms/ios/Sources/MonarchTokens/MonarchTokens.swift`

Both files hold the primitives, the four themes (`light`, `dark`, `hcLight`,
`hcDark`) with 96 colour roles each, five shadow levels, and 21 accents (the
default, the neutral accent and one per Rook product), each with a scale for
every theme. Names are the same in both languages.

- `npm run native:tokens` regenerates both files.
- `npm run native:tokens:check` fails if either is stale. It is part of
  `npm run verify`.

Three token layers exist. Primitives and semantic roles are generated.
Component tokens are small, hand-written and live beside the component (for
example the button's heights and paddings per size), built only from
primitives and roles. A generated component layer will be added when a
component token needs to be shared with the web; not before.

Units: `1rem` is 16dp or 16sp on Android and 16 points on iOS. Not generated,
because they have no native meaning: z-index, CSS font stacks and the fluid
`clamp()` display sizes.

Accents: `MonarchThemeTokens.withAccent(accent)` returns the theme with the
accent roles replaced, plus the roles that follow the accent in that theme
(links everywhere; the focus ring in the standard themes only, because the
high-contrast themes draw it in the text colour).

## Theme

Appearance has three independent inputs: scheme (system, light, dark),
contrast (system, standard, more) and accent. A pure function resolves them to
one of the four themes, so the rule is testable without a device.

Android: `MonarchTheme { }` provides Monarch's colours, typography, shapes and
spacing through composition locals, and wraps `MaterialTheme` with a colour
scheme, typography and shapes derived from them in one mapping function. Any
Material 3 component an app places inside is therefore themed, and so are the
platform layers Monarch uses. Dynamic colour is off
by default, because the product accent is part of the brand; it can be turned
on per app.

iOS: `.monarchTheme()` puts a `MonarchTheme` value in the SwiftUI environment
and sets `tint`. Components read `@Environment(\.monarchTheme)`. System
controls pick up the tint (the switch is the system `Toggle`, tinted), and
Monarch provides styles (`ButtonStyle`, `ToggleStyle`, `ProgressViewStyle`,
field and select styles) and custom views for the rest.

Brand themes: an app can derive a complete Monarch theme from its own brand
colour, as `deriveTheme` does on the web
([Android](../../platforms/android/docs/BRAND-THEMES.md),
[iOS](../../platforms/ios/docs/BRAND-THEMES.md)).

Typography: both platforms use the system typeface by default (Roboto or the
device font on Android, San Francisco on iOS) with Monarch's sizes, weights
and tracking. A product can pass its own font family. Poppins is an optional separate package
(`monarch-fonts-poppins` on Android, the `MonarchFontsPoppins` product on iOS,
SIL Open Font License 1.1), not part of the core libraries. Type always scales with the
user's text-size setting.

## Components

Every component is classified before it is built
([NATIVE-COMPONENTS.md](NATIVE-COMPONENTS.md)):

- **A. Native adoption.** Use the platform control directly inside the Monarch
  theme. Monarch ships nothing.
- **B. Monarch style or wrapper.** A thin layer over the platform control that
  applies tokens and a consistent API.
- **C. Custom.** Built by Monarch because the platform has no equivalent.

A web component existing is not a reason to build a native one. Each of the
515 web components has an inventory entry on both platforms, with one of
these coverage types ([NATIVE-PARITY](NATIVE-PARITY.md)):

| Coverage   | Meaning                                                               |
| ---------- | --------------------------------------------------------------------- |
| `build`    | Monarch builds the component: custom, or a wrapper with its own API   |
| `style`    | Monarch ships a style or theme layer on the platform control          |
| `native`   | The platform control is the answer; the entry documents how to use it |
| `compose`  | A documented composition of other Monarch components                  |
| `web-only` | Has no meaning in a native app; the entry gives the reason            |

The per-platform counts are in [NATIVE-COMPONENTS](NATIVE-COMPONENTS.md),
which is generated from the inventories.

### Shared vocabulary

A design reads the same on every platform because the names are shared, even
where the code follows each language's conventions.

- Button variants: `primary`, `secondary`, `ghost`, `danger`. Sizes: `sm`,
  `md`, `lg`. The web's `xs` size is not offered on touch platforms.
- Tones: `neutral`, `info`, `success`, `warning`, `danger`.
- Type roles: `display`, `title`, `heading`, `subheading`, `body`,
  `bodyStrong`, `label`, `caption`, `code`.
- Component names: `Monarch` plus the web name (`MonarchDataTable` for
  `DataTable`). Spelling is US English (`behavior`, `color`).
- Booleans follow each language, by one rule per platform:
  - Swift names a Boolean as an assertion with an `is`, `shows`, `allows` or
    `uses` prefix, as Apple's API Design Guidelines ask: `isEnabled`,
    `isLoading`, `isSelected`, `isOptional`, `isComplete`, `showsErrors`.
    Two names cover on and off, and each has one job. `isDisabled` marks a
    choice that cannot be picked, on an option or item type (a
    `MonarchActionType`, a `MonarchAiModelOption`), usually with a
    `disabledReason`. `isEnabled` marks a capability that is switched on or
    off (`MonarchAiConnector.isEnabled`, `MonarchAutosave.isEnabled`). A
    view itself is disabled with SwiftUI's `.disabled(_:)`, never with a
    parameter. A type never has both.
  - Kotlin with Compose uses the adjective: `enabled`, `loading`, `selected`,
    `optional`, `complete`, `showErrors`, as Material 3's composables do.
  - The meaning is the same. The mapping of the names that differ is in
    [NATIVE-NAMES](NATIVE-NAMES.md); the variant names are the same on both.
- Other names that differ by idiom: Android `firstDayOfWeek` and iOS
  `firstWeekday` (`nil` or `null` is the locale's first day on both), Android
  `showErrors` and iOS `showsErrors`, Android `createId` and iOS `makeID`.
- The shared model names (`MonarchCondition*`, `MonarchActionParameter*`,
  `MonarchAi*`) are the same on both platforms and are checked: see
  [NATIVE-NAMES](NATIVE-NAMES.md).

### Time zones

Time zones follow each platform's idiom:

| Where                                        | iOS                                                                                      | Android                                                                                                                         |
| -------------------------------------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| A component parameter                        | Foundation `TimeZone`. `nil` or the default means `.autoupdatingCurrent`, read each time | The IANA id, `String?`. `null` means the device's zone, read each time, so the parameter stays stable and immutable for Compose |
| A value or model type that is stored or sent | The IANA id, `String`, as on the web                                                     | The IANA id, `String`, as on the web                                                                                            |

Examples of the second row are `MonarchBusinessHoursValue.timeZone` and the
slot data of the slot picker.

### Experimental API

Experimental API is opt-in on both platforms:
`@ExperimentalMonarchApi` (a `RequiresOptIn` at level `ERROR`) on Android,
and `@_spi(Experimental)` on iOS, imported with
`@_spi(Experimental) import MonarchUI`. A stable declaration never exposes an
experimental type. The rules are in
[native API stability](../governance/native-api-stability.md#experimental-api).

## Accessibility

Accessibility is part of the component contract, not a later pass
([guide](../guides/native-accessibility-and-testing.md)). Every component:

- has a touch target of at least 48dp (Android) or 44pt (iOS), even when the
  visible control is smaller;
- exposes its role, state and label to TalkBack or VoiceOver;
- scales with the text-size setting and stays usable at the largest size;
- works right to left;
- uses only colour pairs that the token contrast report covers;
- respects reduced motion, and on iOS Increase Contrast, and on Android the
  system contrast setting.

## Versioning and compatibility

- The native version is the `@rookcom/monarch-react` version, in the `0.14.x`
  patch series. SwiftPM reads a plain `0.14.N` tag; the Maven version is the
  same `0.14.N`
  ([native release and maintenance](../governance/native-release-and-maintenance.md#versions)).
- Public API is recorded and checked: the Kotlin API dump in
  `platforms/android/monarch-compose/api/`, and the Swift API baseline in
  `platforms/ios/api/` plus compiled examples on iOS.
- A deprecated API keeps working, names its replacement, and is removed no
  sooner than 90 days and two patch releases after the release that
  deprecates it.
- Every change in behaviour that an app can observe has a `MONARCH-COMPAT:`
  note in the changelog.
- The tested toolchain and OS range is in
  [NATIVE-COMPATIBILITY.md](NATIVE-COMPATIBILITY.md). Monarch claims support
  only for what is in that table.

## Release status

- **Android publishing.** Decided: Maven Central through the Central Portal,
  from `main` only, signed in the release job. Until the first release has
  been published, the library is published to a local Maven repository for
  verification.
- **iOS distribution.** Decided: a root `Package.swift`, generated from
  `platforms/ios/Package.swift` and checked for drift, so apps add Monarch by
  the repository URL from the first `0.14.N` tag.
- **CI.** The native jobs are required checks on `uat` and `main`
  ([required CI checks](../governance/native-release-and-maintenance.md#required-ci-checks)).

## Platform terms

Monarch's own code is under the Apache License 2.0. Apps built with it also
use Apple's and Google's SDKs, which come with their own terms. This section
states what Monarch does with those SDKs; it is not legal advice, and the
official texts below are what apply.

**Apple (iOS)**

- Monarch for iOS builds against Apple's SDKs (SwiftUI, Foundation) and
  ships no Apple code. The SDKs and Xcode are covered by the
  [Xcode and Apple SDKs Agreement](https://www.apple.com/legal/sla/docs/xcode.pdf);
  distribution on the App Store is covered by the
  [Apple Developer Program License Agreement](https://developer.apple.com/support/terms/apple-developer-program-license-agreement/)
  and the [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/).
- SF Symbols are referred to by name from Monarch for iOS and drawn by the
  system. Monarch ships no symbol files and uses no symbol that depicts an
  Apple product or mark. Apple's
  [SF Symbols terms](https://developer.apple.com/sf-symbols/) limit their use
  to app interfaces on Apple platforms, so Monarch uses them on iOS only, and
  iOS screenshots show them only as part of an app's interface.
- The San Francisco fonts are the system font. Monarch bundles no Apple font
  file; text uses the system font through SwiftUI
  ([Apple fonts](https://developer.apple.com/fonts/)).
- `npm run legal:check` fails when an Apple font file is in the repository,
  when SF Symbols are named outside the iOS library, or when a symbol that
  depicts an Apple product or mark is named.

**Google (Android)**

- Monarch for Android depends on AndroidX libraries (Jetpack Compose,
  Material 3, the adaptive layout artifacts and `activity-compose`), which
  Google publishes under the
  [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). An app
  resolves them from Google's Maven repository; Monarch does not copy them.
- The Android SDK is covered by the
  [Android Software Development Kit License Agreement](https://developer.android.com/studio/terms),
  and distribution on Google Play by the
  [Google Play Developer Distribution Agreement](https://play.google.com/about/developer-distribution-agreement.html)
  and its [policies](https://play.google.com/about/developer-content-policy/).
- Material Symbols and Roboto, where an app uses them, are published by
  Google under the Apache License 2.0 and the SIL Open Font License; Monarch
  bundles neither.
- "Android" and "Google Play" are trademarks of Google LLC; "Apple", "iOS",
  "SwiftUI" and "SF Symbols" are trademarks of Apple Inc. Monarch uses the
  names only to say what it runs on.

Third-party code and data in Monarch itself are listed in
[THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md).

## Known limitations

- Poppins, the brand typeface on the web, is not in the core libraries. Native
  uses the system typeface unless the app adds the Poppins package or supplies
  a family.
- The fluid display sizes used by marketing pages have no native equivalent.
- No Rook product ships a native app on Monarch yet, so the API has not been
  proven in production.
- Component tokens are hand-written per platform.
