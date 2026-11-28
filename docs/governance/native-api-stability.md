# Native API stability

Rook applications depend on Monarch for Android and Monarch for iOS as
libraries. This page defines what those applications can rely on: what is
public API, how it is checked, how an API is deprecated and removed, and how a
breaking change is decided.

The web rules are in [versioning](versioning.md) and the
[deprecation policy](DEPRECATION-POLICY.md). The stages are the same on
native; the tools differ, and this page says how. Release tracks and version
numbers are in
[native release and maintenance](native-release-and-maintenance.md).

## What is public API

### Android

Public API is every declaration in the published modules `monarch-tokens` and
`monarch-compose` that Kotlin treats as `public` or `protected`, and that is
not marked `@ExperimentalMonarchApi`. Both modules compile with
`explicitApi()`, so nothing is public by accident: visibility and return types
are always written out.

That includes:

- composable functions, classes, objects, enums and their entries, properties,
  constructors, extension functions and modifiers (`Modifier.monarch…`);
- parameter names, parameter order and default values of public functions
  (Compose call sites use named arguments, so a rename breaks source);
- the names in the generated `MonarchTokens` object and in the token classes;
- what the modules expose to an app's build: the Maven coordinates, the
  `api` dependencies (the Compose BOM, `ui`, `foundation`, `material3`), the
  minimum SDK, and the Kotlin metadata version an app's compiler must read.

Not public API:

- anything `internal` or `private`;
- anything marked `@ExperimentalMonarchApi`;
- composition locals (they are internal; read values through `MonarchTheme`);
- the sample app, tests, previews and build logic;
- rendered pixels, default token values, animation timing and the exact
  structure of the semantics tree beyond role, state and label.

### iOS

Public API is every declaration in the products `MonarchTokens` and
`MonarchUI` that is written `public` (the package does not use `open`), and
that is not marked `@_spi(Experimental)`.

That includes:

- types, their cases, initialisers, properties and methods; view modifiers on
  `View`; styles (`ButtonStyle`, `ToggleStyle` and the others); environment
  keys;
- argument labels, parameter order and default values;
- protocol conformances a caller can use, such as `Sendable`, `Hashable` and
  `CaseIterable`;
- the names in the generated `MonarchTokens` enum and the token structs;
- what the package exposes to an app's build: the product names, the
  deployment target and the Swift tools version in `Package.swift`.

Not public API:

- anything `internal`, `fileprivate` or `private`, and everything under
  `Sources/MonarchUI/Internal`;
- anything marked `@_spi(Experimental)`;
- the sample app, tests, previews and scripts;
- rendered pixels, default token values, animation timing, and the concrete
  type behind `some View`.

Monarch for iOS is distributed as source and does not enable library
evolution, so it promises **source compatibility**, not a stable binary
interface. Monarch for Android promises both source and binary compatibility
within the rules below, because apps and other libraries consume compiled
artefacts.

### On both platforms

Behaviour that an app can observe and reasonably depends on is treated as API
even though no tool records it: a component's accessibility role, state and
label; its minimum touch target; which theme a set of appearance inputs
resolves to; whether a control is the platform's own control. Changing any of
these needs the same decision as a signature change.

## What counts as breaking

- Removing or renaming a public declaration, an enum entry or case, a
  parameter or an argument label.
- Changing a parameter's type, a return type or a property's type.
- Reordering parameters, or adding a parameter without a default value.
- Changing a default value so that existing calls behave differently.
- Adding an enum entry or case that apps switch over exhaustively. Enums that
  are expected to grow say so in their documentation, and apps are told to
  include a default branch.
- Removing a protocol conformance or interface, or making a type no longer
  `Sendable`.
- Android: a change that keeps source compatibility but breaks binaries, such
  as adding a parameter with a default value to a public function (the JVM
  signature changes). Keep the old overload, deprecated and hidden, for the
  overlap period.
- Raising the minimum SDK or deployment target, the Swift tools version, or a
  dependency floor that apps must follow.
- Moving a declaration to another module or product.
- Renaming or removing a token. Changing a token's value is not breaking,
  unless it breaks a contrast guarantee, which is a bug.

Additive changes are not breaking: a new declaration, a new parameter with a
default value on iOS, a new overload, a new token.

## Compatibility checks

A change to public API is never accidental, because the recorded API is a
file in the repository and a difference fails the check.

### Android: API dumps

Both convention plugins (`monarch.kotlin.library`, `monarch.android.library`
under `platforms/android/build-logic`) apply the binary-compatibility-validator
plugin (`org.jetbrains.kotlinx.binary-compatibility-validator`, version in
`gradle/libs.versions.toml`). It records the public binary API of each module
in a dump under `<module>/api/` (for example
`platforms/android/monarch-compose/api/monarch-compose.api`) and compares the
build against it in `apiCheck`, which is part of Gradle's `check`.

- The dump files are committed. A pull request that changes public API
  therefore contains a visible diff of the dump, and reviewers read it.
- A build whose API differs from the committed dump fails. Updating the dump
  is a deliberate step; the command is in the
  [upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md) and in the Android
  section of [contributing](../guides/native-contributing.md).
- `./gradlew apiDump` rewrites the dumps after an intended change.
- If the plugin is replaced, for example by the Kotlin Gradle plugin's own ABI
  validation once that is stable, the replacement lands in the same pull
  request that removes the plugin; the check is never simply turned off.

### iOS: API baseline and compiled examples

Two checks together stand in for binary validation:

- **API baseline.** `platforms/ios/Scripts/api.sh` asks the compiler for the
  symbol graph of `MonarchTokens` and `MonarchUI`, flattens the public
  declarations to text and compares them with the committed files in
  `platforms/ios/api/` (`MonarchTokens.txt`, `MonarchUI.txt`).
  `Scripts/api.sh --update` rewrites the baseline after an intended change.
  Experimental declarations are not part of the baseline.
- **Compiled examples.** Every inventory entry (`platforms/ios/inventory/<id>.json`)
  has `examples`. `Scripts/generate.py` writes those examples into
  `Tests/MonarchUITests/Generated/Examples/`, which the test suite compiles and
  renders, and its `--check` mode fails when a generated file is stale. An example is the call site an
  app would write, so a source-breaking change fails to compile there.

Both run as part of `platforms/ios/Scripts/verify.sh`.

### Both platforms

- The sample app builds against the public API only.
- Warnings are errors in both builds, so a Monarch implementation that calls
  a deprecated platform API fails until the call is fixed or isolated
  ([platform evolution](native-platform-evolution.md#compatibility-workarounds)).
- The native CI jobs are required checks on `uat` and `main`
  ([required CI checks](native-release-and-maintenance.md#required-ci-checks)).
  The reviewer confirms that the API record in the diff matches the
  changelog.
- Testing a released Monarch version against real Rook applications before a
  release is planned. No Rook product ships a native app on Monarch yet.

## Deprecation policy

An API is deprecated before it is removed. No public API is removed without a
recorded decision.

Every deprecation states, in the declaration itself and in the changelog:

1. **Why** it is deprecated.
2. **The replacement**, as code the tools can offer where the language allows
   (`ReplaceWith` in Kotlin, `renamed:` in Swift).
3. **A before-and-after example**, in the migration guide.
4. **The earliest removal date**, written "removed no earlier than <date>
   (90-day window)". Removal can happen later than that date, never earlier.

Rules:

- **The window.** A deprecated API is removed no sooner than **90 days and two
  patch releases** after the release that deprecates it. Both conditions hold:
  an API deprecated in `0.14.6` on 2026-11-20 can be removed at the earliest in
  `0.14.8`, and only if that release ships on or after 2027-02-18.
- **The series.** Monarch stays in the `0.14.x` patch series, which semantic
  versioning allows before 1.0. A removal therefore ships in a patch; there is
  no `0.15.0` to wait for, and no message says "removed in 0.15.0". From 1.0,
  removal also waits for the next major.
- **The deprecated API keeps working** for the whole period, with the same
  behaviour and the same tests. It is implemented by calling the replacement
  wherever that is possible.
- **Warnings stay visible.** A deprecation is a compiler warning in the app
  (`DeprecationLevel.WARNING` in Kotlin, `deprecated` in Swift). Monarch does
  not ship a deprecation as documentation only, and does not downgrade one to
  keep builds quiet.
- **Warnings are never silenced.** Inside Monarch, a deprecation warning from
  Kotlin, Swift, Compose, SwiftUI or an SDK is fixed, or isolated as a
  recorded workaround. It is not suppressed to make a build pass, and
  warnings-as-errors is not turned off. The one permitted suppression is in
  the test that keeps covering a deprecated Monarch API until it is removed,
  scoped to that test and naming the removal version.
- **Applications** do not suppress Monarch deprecation warnings either. They
  migrate, or record a time-limited exception as for the web
  ([exceptions](exceptions.md)).
- **Escalation on Android.** In the last release before removal the level may
  be raised to `DeprecationLevel.ERROR`, then `HIDDEN` to keep binaries
  linking, when that helps apps find remaining uses. This is announced in the
  changelog of the release before it happens.
- **Automated help.** Provide `ReplaceWith` so Android Studio can apply the
  change, and `renamed:` so Xcode offers a fix-it. Anything that cannot be
  expressed that way gets written steps. There is no native codemod and no
  native conformance rule; `registry/deprecations.json` and `api/deprecated`
  cover the web packages only.
- **The record.** Each platform's changelog lists the deprecation under
  `Deprecated`, and the inventory (`components.json`) marks a deprecated
  component with `"status": "deprecated"` in `inventory/<id>.json`. Migration guides for the native
  platforms are collected in one place per platform; the location is decided
  with the first real deprecation, and until then the template below is the
  standard.

Deprecating a token follows [native tokens](native-tokens.md#deprecate-or-remove-a-token),
because the token source and three platforms are involved.

## Migration guide template

One guide per deprecation or breaking change. Copy the structure.

```markdown
## <Old API> → <new API> (<platform> <version>)

- Status: deprecated in <version> (<date>). Removed no earlier than <date>
  (90-day window).
- Affects: <who has to act, and how to find the uses>.

### Why

<One or two sentences. The reason, not the history.>

### What to change

<Numbered steps. Say what the tools do for you.>

### Before

<Code that compiles against the old API.>

### After

<Code that compiles against the new API.>

### Behaviour differences

<Anything that renders, reads to a screen reader or behaves differently.
Write "None" if there are none.>

### If you cannot migrate yet

<What keeps working until which version, and how to ask for an exception.>
```

### Worked example of the format

Everything in this section is an **illustration of the format**. `MonarchTag`,
its parameters and the version numbers are invented for the example. They are
not real Monarch API and no such deprecation exists.

> #### `MonarchTag(label:, color:)` → `MonarchTag(text:, tone:)` (Android and iOS 0.14.9)
>
> - Status: deprecated in 0.14.9 (2027-01-05). Removed no earlier than
>   2027-04-05 (90-day window), and not before 0.14.11.
> - Affects: every call that passes a colour to a tag. The compiler warns at
>   each one.
>
> **Why.** A tag that takes a colour can be given one with too little
> contrast, and does not follow the theme. A tone maps to colour roles that
> the contrast report covers in all four themes.
>
> **What to change.** Replace the colour with the tone that carries the same
> meaning. Android Studio offers the replacement for the parameter rename;
> choose the tone by hand.

How the deprecation is declared in Kotlin:

```kotlin
@Deprecated(
    message = "Pass a tone instead of a colour, so the tag follows the theme. " +
        "Removed no earlier than 2027-04-05 (90-day window).",
    replaceWith = ReplaceWith("MonarchTag(text = label, tone = MonarchTone.Neutral)"),
    level = DeprecationLevel.WARNING,
)
@Composable
public fun MonarchTag(label: String, color: Color, modifier: Modifier = Modifier) {
    // Kept working for the overlap period.
}
```

Before and after in an Android app:

```kotlin
// Before
MonarchTag(label = "Overdue", color = Color(0xFFD62434))

// After
MonarchTag(text = "Overdue", tone = MonarchTone.Danger)
```

How the deprecation is declared in Swift:

```swift
extension MonarchTag {
    @available(
        *, deprecated,
        message: "Pass a tone instead of a colour, so the tag follows the theme. Removed no earlier than 2027-04-05 (90-day window)."
    )
    public init(label: String, color: Color) {
        // Kept working for the overlap period.
        self.init(text: label, tone: .neutral)
    }
}
```

Before and after in an iOS app:

```swift
// Before
MonarchTag(label: "Overdue", color: .red)

// After
MonarchTag(text: "Overdue", tone: .danger)
```

> **Behaviour differences.** The tag's colours now change with the theme and
> the contrast setting. A colour that had no matching tone becomes `neutral`.
>
> **If you cannot migrate yet.** The old initialiser keeps working until at
> least 2027-04-05.

`MonarchTone` is real and has the cases `neutral`, `info`, `success`,
`warning` and `danger`; everything else in the example is not.

## Experimental API

An experimental API is one Monarch wants products to try before promising to
keep it. It may change or be removed in any release, without a deprecation
period.

| Rule               | Android                                                                                       | iOS                                                             |
| ------------------ | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Marker             | `@ExperimentalMonarchApi` (package `dev.monarchui`), a `@RequiresOptIn` at level `ERROR` | `@_spi(Experimental)` on the declaration                        |
| How an app opts in | `@OptIn(ExperimentalMonarchApi::class)` at the use site                                       | `@_spi(Experimental) import MonarchUI` in the file that uses it |
| Without the opt-in | The call does not compile                                                                     | The declaration is not visible                                  |
| In the API record  | A change to a marked declaration in the dump is reviewed, and is not a breaking change        | Left out of the baseline                                        |
| In the inventory   | `"status": "experimental"`                                                                    | `"status": "experimental"`                                      |

Rules on both platforms:

- **Never in a stable signature.** An experimental type does not appear as a
  parameter, return type, property type, supertype or default value of a
  stable declaration. A stable component does not require an opt-in to be
  used in its documented way.
- Experimental API is opted into at the narrowest scope. Applications do not
  opt in for a whole module through compiler flags, and Monarch's own
  modules never do.
- An experimental API has the same quality bar as a stable one: tests,
  accessibility behaviour, previews, documentation and an inventory entry.
  Experimental describes the stability of the API, not of the implementation.
- Each one records, in its documentation comment, what has to be learned
  before it can become stable. It is reviewed every quarter and is either
  promoted, changed or removed. Experimental is not a place to park API.
- Promotion to stable removes the marker in a release and is listed
  under `Added`. Removal of an experimental API is listed under `Removed`
  with a note that it was experimental; it needs no deprecation period, and
  products that opted in are told directly where they are known.
- A platform API that its vendor marks experimental is a separate matter. It
  may be used only inside an implementation and never surfaces in Monarch's
  public API in any form
  ([platform evolution](native-platform-evolution.md#rules-that-prevent-upgrade-lock-in)).

The iOS marker relies on `@_spi`, an underscored Swift attribute that the
language does not formally guarantee. It is used because Swift has no
supported opt-in mechanism. If Swift gains one, Monarch moves to it; the rule
above stays the same. The experimental components are the inventory entries
with `"status": "experimental"`; the documentation site marks each one with an
Experimental badge, and a domain composite among them with a Pattern
(experimental) badge ([ADR 0017](../adr/0017-native-patterns-layer.md)).

## How a breaking change is decided and released

1. **Propose.** Open an issue that names the API, the reason, the replacement
   and which products use it. Prefer an additive change with a deprecation
   over a break whenever one exists.
2. **Decide.** A Monarch maintainer and the platform owner decide. A change
   to the shared vocabulary (component names, variants, tones, sizes, type
   roles, token names) is decided once for web, Android and iOS, and recorded
   as an ADR in `docs/adr`. A change inside one platform is recorded in the
   issue and the changelog.
3. **Deprecate first.** Ship the replacement and the deprecation together,
   under the [deprecation policy](#deprecation-policy), with the migration
   guide.
4. **Wait** for the overlap period, and check the known consumers. Removal is
   postponed while a registered product still uses the API, unless the
   decision records why it cannot wait.
5. **Remove** in a `0.14.N` patch once the window has passed before 1.0, or
   in a major release from 1.0. The
   pull request updates the API record deliberately (the Kotlin dump, the
   Swift baseline), the changelog (`Removed`, with the link to the guide) and
   the inventory.
6. **Release** through the normal release steps. A release with a breaking
   change says so in the first line of its changelog entry and its release
   notes. Where a preview release is practical, a breaking release has one
   first so that a product can try the upgrade.

A break that cannot be preceded by a deprecation, for example because a
platform removed the API underneath, is still recorded, announced in the
changelog and given a migration guide. It is the exception, and the decision
says why.

Both platforms release together under one version number, but a break is
decided per platform: a break on Android does not force a break on iOS. A
release in which only one platform breaks says which in the first line of
each changelog entry.

## Related

- [Native release and maintenance](native-release-and-maintenance.md)
- [Native platform evolution](native-platform-evolution.md)
- [Native tokens](native-tokens.md)
- [NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md)
- [Contributing to the native platforms](../guides/native-contributing.md)
- [Upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md)
