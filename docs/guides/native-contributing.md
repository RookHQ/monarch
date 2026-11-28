# Contributing to the native platforms

How to contribute to Monarch for Android (`platforms/android`) and Monarch
for iOS (`platforms/ios`).

Start with [CONTRIBUTING](../../CONTRIBUTING.md): the licence of
contributions, the Code of Conduct, how to report a vulnerability and the
general pull request rules apply here unchanged. This page adds what is
specific to native. The architecture is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md); read it first.

Both platforms cover the 515 web components (built, styled, adopted from the
platform, composed, or documented as web only) and are not released yet.
Their version is the `@rookcom/monarch-react` version, in the `0.14.x` patch
series ([versions](../governance/native-release-and-maintenance.md#versions)).

## Required tools

You need the tools for the platform you change, and Node only if you change
tokens or documentation.

| For             | Tool                                                                                                       | Where the version is stated                                                       |
| --------------- | ---------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| Android         | A JDK, and the Android SDK with the compile SDK platform. Gradle comes from the wrapper; do not install it | `jdk`, `compileSdk` and `minSdk` in `platforms/android/gradle/libs.versions.toml` |
| iOS             | Xcode with an iOS Simulator runtime. swift-format ships with the toolchain                                 | "Requirements" in `platforms/ios/README.md`                                       |
| iOS sample app  | [XcodeGen](https://github.com/yonaskolb/XcodeGen)                                                          | `platforms/ios/Sample/project.yml`                                                |
| Tokens and docs | Node, the version in `.nvmrc`, then `npm ci` at the repository root                                        | `.nvmrc`                                                                          |

The toolchain and OS versions that Monarch has actually been tested with are
in [NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md). Use a
version in that table. A newer Xcode, Kotlin or Android Gradle Plugin is an
upgrade, and goes through the
[upgrade process](../governance/native-release-and-maintenance.md#upgrade-process),
not into a feature pull request.

Neither platform needs the other's tools, and neither needs Node to build:
the generated token files are committed.

## Repository layout

```text
packages/tokens/                 token source; scripts/emit-native.ts writes the native files
platforms/
  parity.json                    the plan: how each web component is covered on native
  android/                       Gradle build of its own
    gradle/libs.versions.toml    the only place a version is written
    build-logic/                 convention plugins shared by the modules
    config/                      static analysis configuration
    monarch-tokens/              generated constants. Pure Kotlin, no Android, no Compose
    monarch-compose/             the library: theme, foundation, accessibility, components
    sample/                      catalogue app
  ios/                           Swift package of its own
    Package.swift                the manifest; the root Package.swift is generated from it
    Sources/MonarchTokens/       generated constants
    Sources/MonarchUI/           the library: Theme, Foundations, Accessibility, Components
    Tests/
    Sample/                      catalogue app (XcodeGen)
    Scripts/                     verify.sh and the checks it runs
    api/                         the public API baseline
    inventory/<id>.json          one inventory file per web component id
    components.json              generated from inventory/
    CHANGELOG.md
  (android has the same inventory/, components.json, README.md and CHANGELOG.md)
docs/architecture/NATIVE-*.md    architecture, classification, compatibility, parity
docs/governance/native-*.md      release, API stability, platform evolution, tokens
docs/guides/native-*.md          this guide, adaptive layout, accessibility and testing
```

Each platform's `README.md` lists its folders in full, and
`docs/ADDING-COMPONENTS.md` in each platform is the recipe for adding a
component.

## Layer and source rules

Dependencies point one way. A lower layer never imports a higher one.

| Layer          | What it is                                                           | Android                         | iOS                              |
| -------------- | -------------------------------------------------------------------- | ------------------------------- | -------------------------------- |
| 1. Tokens      | Generated constants. No UI framework                                 | `monarch-tokens`                | `Sources/MonarchTokens`          |
| 2. Theme       | Maps tokens to the platform's theme types                            | `monarch-compose`, `theme/`     | `Sources/MonarchUI/Theme`        |
| 3. Foundations | Type roles, spacing, shape, elevation, motion, accessibility helpers | `foundation/`, `accessibility/` | `Foundations/`, `Accessibility/` |
| 4. Components  | Read the theme only                                                  | `components/<name>/`            | `Components/<Name>/`             |

Source rules:

- **One public component per file**, in its own folder with its options
  (variant, size), its component tokens and its previews.
- **Public API is explicit.** Kotlin modules use `explicitApi()`; in Swift,
  `public` is written out. Everything else is `internal` or `private`. Every
  public declaration has a documentation comment.
- **Colour comes from the theme.** Never from a `MonarchTokens` primitive and
  never from a literal
  ([the rule](../governance/native-tokens.md#the-rule-for-components)).
- **Sizes come from tokens**, through the theme or the component's token
  file. No literal dp, sp or point values in a component.
- **Generated files are never edited.** To change a token, change
  `packages/tokens` and run `npm run native:tokens` at the repository root
  ([native tokens](../governance/native-tokens.md)).
- **Strings that Monarch itself supplies are localised resources** and can be
  replaced through a parameter. Titles and messages are passed in by the app,
  already localised.
- **No new dependency** beyond the platform SDK and, on Android, AndroidX and
  Compose, without the justification in
  [platform evolution](../governance/native-platform-evolution.md#rules-that-prevent-upgrade-lock-in).
- **No version outside the catalog or the manifest.**
- **Warnings are errors, and are not suppressed.** Fix the cause. The one
  case that allows a scoped suppression is a recorded compatibility
  workaround
  ([platform evolution](../governance/native-platform-evolution.md#compatibility-workarounds)).
- **Prefer the platform.** Compose and Material 3 on Android, SwiftUI on iOS.
  Views or UIKit interoperability needs a reason that the platform has no
  equivalent.
- **Experimental API is opt-in** and never appears in a stable signature
  ([API stability](../governance/native-api-stability.md#experimental-api)).

On Android several of these are checked by a test that reads the source
(`SourceRulesTest` in `monarch-compose`): no literal colours, no token
primitive colours in components, no literal dp or sp sizes in components, a
preview file in every component folder, and one public component per file.
Static analysis is detekt, configured in
`platforms/android/config/detekt/detekt.yml`, with no baseline file: a
finding fails the build until it is fixed. On iOS, `swift format lint
--strict` enforces the documentation comment and the style rules; the layer
and colour rules are checked in review.

## Classify before you build

Every component is classified before any code is written. The full list and
the reasons are in [NATIVE-COMPONENTS](../architecture/NATIVE-COMPONENTS.md).

| Class | Meaning                                                                                             | What Monarch ships                         |
| ----- | --------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| A     | **Native adoption.** The platform control, inside the Monarch theme, already does the job           | Nothing. A documented inventory entry only |
| B     | **Monarch style or wrapper.** A thin layer over the platform control that applies tokens and an API | A style, modifier or small wrapper         |
| C     | **Custom.** Built by Monarch because the platform has no equivalent                                 | A component                                |

Work down the list and stop at the first class that fits:

1. Can the platform control be used as it is, themed by `MonarchTheme`?
   Then it is A. Navigation, dialogs, sheets, menus, tabs, lists and date
   pickers are A.
2. Does it only need Monarch's tokens, or one consistent set of options
   across products? Then it is B. Prefer the platform's own extension point:
   a `ButtonStyle` or `ToggleStyle` on iOS, Material component parameters on
   Android.
3. Is there a real gap, with no platform control that provides the behaviour
   and its accessibility? Only then is it C.

The class can differ between the platforms, and a component can exist on one
platform only. A web component existing is not a reason to build a native
one.

### Proposing a new component

1. **Check the plan.** Look the component up in
   [`platforms/parity.json`](../../platforms/parity.json), explained in
   [NATIVE-PARITY](../architecture/NATIVE-PARITY.md). It records the intended
   coverage on each platform and the wave it belongs to. An entry in the plan
   is not permission to build ahead of its wave.
2. **Check the need.** The promotion rule is the same as on the web: three
   or more Rook products need it, or two or more at P0
   ([component governance](../governance/COMPONENT-GOVERNANCE.md#promotion-rule)).
   A component one product needs stays in that product, built from Monarch
   foundations.
3. **Open a proposal issue** with the component proposal template. State,
   for each platform:
   - the class (A, B or C) and why the classes above it do not fit;
   - the platform control it adopts or wraps;
   - the proposed API, using the shared vocabulary (variants `primary`,
     `secondary`, `ghost`, `danger`; sizes `sm`, `md`, `lg`; tones `neutral`,
     `info`, `success`, `warning`, `danger`) where it applies;
   - the accessibility model: role, states, label, focus and touch target;
   - what differs between Android and iOS, and why.
4. **A maintainer decides** the class and the wave, and records them in the
   plan and in NATIVE-COMPONENTS.
5. **Build it on one platform or both.** The platforms do not have to ship
   together. If the API shape is uncertain, ship it as experimental first.

## What every component ships with

A component is not done until all of these are in the same pull request.

### Code

- The component in its own folder, with its options and a
  `Monarch<Name>Tokens` file for its component tokens.
- A documentation comment on every public declaration, covering what it is,
  each parameter, and the accessibility behaviour.
- Localised resources for any string Monarch supplies.

### Tests

- **Logic**: anything that can be tested as a pure function is (which roles
  a variant resolves to, which theme a set of inputs gives).
- **UI**: the component renders in all four themes, at the default and the
  largest text size, left to right and right to left.
- **Interaction**: tap, toggle, disabled and loading states behave as
  documented.
- **Accessibility** (below).
- **API record**: the Kotlin API dump or the Swift API baseline is updated
  deliberately, in the same pull request, when public API changes
  ([API stability](../governance/native-api-stability.md#compatibility-checks)).

Screenshot or snapshot tests are not set up on either platform. Rendering
tests check that a component lays out and draws, not what it looks like.

### Previews

- Android: a `<Name>Previews.kt` file in the component folder, using the
  shared `@MonarchPreviews` annotation (light, dark and the largest font
  scale).
- iOS: a `#Preview` in the component file, using the shared preview matrix
  (light, dark and the largest Dynamic Type size).
- Previews cover every variant, size and state, with realistic but neutral
  content.
- The component is added to the platform's catalogue sample app.

### Accessibility behaviours

Required of every component
([accessibility and testing](native-accessibility-and-testing.md)):

- a touch target of at least 48dp on Android and 44 points on iOS, even when
  the visible control is smaller;
- role, state and label exposed to TalkBack or VoiceOver, with the platform's
  own semantics kept wherever a platform control is wrapped;
- text and layout that scale with the text-size setting and stay usable at
  the largest size;
- right-to-left layout;
- colour only from roles the contrast report covers, and no meaning carried
  by colour alone;
- reduced motion respected, and the system contrast setting followed;
- a visible focus indicator for keyboard and switch navigation.

Each of these has a test where a test can express it. State in the pull
request what was checked by hand with TalkBack or VoiceOver, and on which
device or simulator. Do not claim a screen reader check that was not done.

### Inventory entry with a compiled example

Each platform has one inventory file per web component id,
`platforms/<platform>/inventory/<id>.json`, validated against
`inventory/schema.json`. The documentation site reads these files.
`components.json` is generated from them and never edited by hand. Add the
file for the component, including entries that Monarch documents but does
not ship (platform controls and web-only entries).

The fields are listed in the schema and explained, with a worked example, in
each platform's `docs/ADDING-COMPONENTS.md`.

- `status` is one of `available`, `experimental`, `planned` or `deprecated`.
  Never mark something `available` that is not implemented and tested.
- `examples` are real code. On iOS, `Scripts/generate.py` writes every
  example into `Tests/MonarchUITests/Generated/Examples/`, which the test suite
  compiles and renders; `--check` fails when a generated file is stale. On
  Android, the build generates the example sources under
  `monarch-compose/build/generated/monarch/test/` and `InventoryExamplesTest`
  composes them.
  An example that does not compile fails the build.
- `accessibility` lists what the code actually does, not what is intended.

### Documentation fields

Every implemented component must document the following. The inventory
carries some of these today; the rest go in the documentation comment of the
public declaration, which the API reference is built from, until the
inventory has a field for them.

| Field                                 | Where it goes                                                    |
| ------------------------------------- | ---------------------------------------------------------------- |
| Purpose                               | Inventory `summary`                                              |
| Native platform behaviour             | Inventory `category` and `wrapsNative`                           |
| When to use                           | Inventory `whenToUse`                                            |
| When not to use                       | Inventory `whenNotToUse`                                         |
| API reference                         | Inventory `api`, and the documentation comments                  |
| Configuration options                 | Documentation comments on the parameters and option types        |
| State descriptions                    | Documentation comment: enabled, disabled, loading, error, focus  |
| Accessibility behaviour               | Inventory `accessibility`                                        |
| Light and dark appearance             | Documentation comment; previews in both, and in high contrast    |
| Native platform differences           | Documentation comment, and `webEquivalent` for the web component |
| Examples                              | Inventory `example`, compiled                                    |
| Edge cases                            | Documentation comment: long text, largest text size, empty value |
| Testing coverage                      | Stated as what the tests cover; no percentages                   |
| Version and compatibility information | The changelog entry, and the minimum OS if above the platform's  |

Rules for all documentation:

- Examples are real and compile against the current API. No placeholder
  examples.
- Use realistic but neutral content. Never invent product statistics,
  pricing or copy.
- Say plainly what is available, experimental, planned or deprecated. Never
  describe an unimplemented capability as ready.
- Report counts, never a score or a percentage.

### Changelog

Add an entry under `Unreleased` in `platforms/<platform>/CHANGELOG.md`
([the standard](../governance/native-release-and-maintenance.md#changelog-standard)).
Changesets are for the npm packages only; add one as well if your change
touches `packages/tokens`.

## Commands

### Tokens (repository root)

| Task                                        | Command                        |
| ------------------------------------------- | ------------------------------ |
| Regenerate the Kotlin and Swift token files | `npm run native:tokens`        |
| Check that they match the token source      | `npm run native:tokens:check`  |
| Format the Markdown you changed             | `npx prettier --write <files>` |

`npm run verify` includes the token check.

### iOS

From `platforms/ios`. These are the commands in `platforms/ios/README.md`;
that file is the reference if the two ever differ.

| Task                              | Command                         |
| --------------------------------- | ------------------------------- |
| Every check, in order             | `Scripts/verify.sh`             |
| The checks that need no simulator | `Scripts/verify.sh --host-only` |
| Build on the Mac host             | `swift build`                   |
| Test on the Mac host              | `swift test`                    |
| Lint (`--fix` reformats first)    | `Scripts/lint.sh`               |
| Check the public API              | `Scripts/api.sh`                |
| Accept a public API change        | `Scripts/api.sh --update`       |
| Regenerate the compiled examples  | `Scripts/generate.py`           |

`Scripts/verify.sh` also runs the tests on a simulator, builds the
documentation and builds the sample app. The simulator it uses is set with
`MONARCH_IOS_DESTINATION`. If Xcode is not the selected developer directory,
set `DEVELOPER_DIR` for the command rather than changing the system setting.
The README has the full `xcodebuild` commands for running those steps one at
a time.

### Android

From `platforms/android`. These are the commands in
`platforms/android/README.md` and the tasks registered in
`platforms/android/build-logic`; the README is the reference if the two ever
differ. Gradle comes from the wrapper, and the build needs a JDK 21 and the
Android SDK (see the README's "Requirements").

Build the modules:

```sh
./gradlew :monarch-tokens:assemble :monarch-compose:assemble
```

Run the unit and UI tests:

```sh
./gradlew :monarch-tokens:test :monarch-compose:testDebugUnitTest :sample:testDebugUnitTest
# one class
./gradlew :monarch-compose:testDebugUnitTest --tests "com.rookhq.monarch.components.actions.MonarchButtonTest"
```

The tests run on the JVM with Robolectric; no emulator or device is needed.

Run static analysis (detekt) and Android lint:

```sh
./gradlew detekt
./gradlew :monarch-compose:lintDebug :sample:lintDebug
```

Check the public API against the committed dump, and accept an intended
change:

```sh
./gradlew apiCheck
./gradlew apiDump   # after an intended change; commit monarch-*/api/*.api
```

Run every check before a pull request:

```sh
./gradlew verifyMonarch
```

While working on one component, `./gradlew componentCheck -Pcomponent=<type name, title or id>`
is the fast loop, and `./gradlew verifyLocal` runs every gate except the API
check and the merge-time gates. `verifyMonarch` is the complete gate and is
what the `android` job in `.github/workflows/native.yml` runs.

Publish to the local Maven repository for verification:

```sh
./gradlew publishAllPublicationsToLocalRepository
```

This writes both libraries to `platforms/android/build/repo`.

Build and run the catalogue sample app:

```sh
./gradlew :sample:assembleDebug
./gradlew :sample:installDebug   # on a connected device or a running emulator
```

## Commits and pull requests

The rules in [CONTRIBUTING](../../CONTRIBUTING.md) and
[branching](../governance/branching.md) apply. In short:

- Branch from `uat`. Name the branch `feat/…`, `fix/…`, `docs/…` or
  `chore/…`. Toolchain and dependency upgrades are `chore/…`.
- **One pull request per job, into `uat`.** Keep all the steps of one job on
  one branch and open one pull request at the end. Do not split a component
  into a pull request for code, one for tests and one for documentation.
- A pull request may cover both platforms when it is one job, such as the
  same component on Android and iOS, or a token change with both regenerated
  files. A toolchain upgrade covers one platform.
- Commit subjects are short and imperative and start with the type, with an
  optional scope: `feat(android): add MonarchBadge`,
  `fix(ios): keep the 44pt target on small buttons`,
  `chore(android): upgrade the Compose BOM`.
- Never commit build output, local configuration, generated Xcode projects
  or anything secret. The root `.gitignore` covers the usual files.
- For anything larger than a small fix, open an issue first.

In the pull request description, state:

- the platform or platforms, and the class (A, B or C) of any component;
- which checks you ran locally and their result, as counts. The native CI
  jobs are required checks as well
  ([required CI checks](../governance/native-release-and-maintenance.md#required-ci-checks));
- any public API change, with the diff of the API record;
- what was checked by hand for accessibility, and on what;
- for a visual change, screenshots in light and dark;
- anything not done, plainly.

The pull request template's checklist is written for the web packages. Tick
what applies; `npm run verify` is needed when you changed tokens,
documentation or anything outside `platforms/`.

A breaking change needs the decision described in
[API stability](../governance/native-api-stability.md#how-a-breaking-change-is-decided-and-released)
before the pull request, not during review.

## Design review checklist

Reviewers use this list for any change that people can see. It is short on
purpose. Monarch should feel like it belongs to Monarch and to the operating
system it runs on; it does not have to look identical on both.

**It should be**

- [ ] **Minimal.** Nothing is there that the task does not need.
- [ ] **Precise.** Spacing, alignment and sizes come from the scale and line
      up with their neighbours.
- [ ] **Consistent.** Same names, variants, tones and behaviour as the rest
      of Monarch on this platform.
- [ ] **Functional.** Every element has a job. States are distinguishable.
- [ ] **Accessible.** Touch target, screen reader, text scaling, contrast,
      right to left and reduced motion all hold.
- [ ] **Calm.** Colour and motion draw attention only where attention is
      needed.
- [ ] **Modern.** It uses the platform's current controls and conventions,
      not last year's imitation of them.
- [ ] **Scalable.** It works from the smallest supported window to a tablet,
      and at the largest text size.
- [ ] **Native-first.** A person who knows the platform already knows how it
      behaves.

**It should not have**

- [ ] Decoration that carries no information.
- [ ] Gradients without a reason.
- [ ] Colours that are not theme roles.
- [ ] Rounded cards around everything, or cards inside cards.
- [ ] Spacing that is off the scale or differs between similar places.
- [ ] A web layout or web control copied onto a phone.
- [ ] More options, variants or parts than a product has asked for.
- [ ] Animation that is not explaining a change of state.
- [ ] The look of another company's design system.

If a change is identical on Android and iOS down to the pixel, check that
neither platform's conventions were given up to make it so.

## Related

- [NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md)
- [NATIVE-COMPONENTS](../architecture/NATIVE-COMPONENTS.md)
- [NATIVE-PARITY](../architecture/NATIVE-PARITY.md)
- [Native tokens](../governance/native-tokens.md)
- [Native API stability](../governance/native-api-stability.md)
- [Native platform evolution](../governance/native-platform-evolution.md)
- [Native release and maintenance](../governance/native-release-and-maintenance.md)
- [Native adaptive layout](native-adaptive-layout.md)
- [Native accessibility and testing](native-accessibility-and-testing.md)
- [Upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md)
