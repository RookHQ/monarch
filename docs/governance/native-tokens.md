# Native tokens

How Monarch's design tokens reach Android and iOS, what they are called there,
who owns them, and how to add, change or deprecate one so that web, Android
and iOS stay in step.

The token rules for the web (selectors, groups, guarantees) are in
[theming](../guides/theming.md). The native architecture is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md#tokens). This page
states what is specific to native.

## One source

`packages/tokens` is the only place a token value is written. Raw values are
in `packages/tokens/src/primitives.ts` and meaning is in
`packages/tokens/src/themes.ts`. Product accents come from
`registry/products/<id>.json`.

`packages/tokens/scripts/emit-native.ts` reads the same modules that produce
the CSS and the DTCG files, and writes two files:

- `platforms/android/monarch-tokens/src/main/kotlin/com/rookhq/monarch/tokens/MonarchTokens.kt`
- `platforms/ios/Sources/MonarchTokens/MonarchTokens.swift`

Semantic colours are read from the DTCG build, with aliases resolved, so a
native colour is the colour the CSS prints. The two files are committed, which
is why neither platform needs Node to build.

| Command                       | What it does                                              |
| ----------------------------- | --------------------------------------------------------- |
| `npm run native:tokens`       | Regenerates both files                                    |
| `npm run native:tokens:check` | Fails if either file differs from what the source implies |

The check is part of `npm run verify`. It currently reports 96 colour roles,
4 themes and 21 accents.

## The three layers

| Layer     | What it holds                                                                     | Where it lives on native                                                                                                                                         | Written by                                                                  |
| --------- | --------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Primitive | Raw values with no meaning: colour ramps, spacing, radii, type sizes, motion      | The nested groups of `MonarchTokens` (`Neutral`, `Status`, `Space`, `Radius`, …) in the generated file                                                           | The generator                                                               |
| Semantic  | Meaning per theme: surfaces, text, borders, intents, focus, data colours, shadows | `MonarchTokens.light`, `.dark`, `.hcLight`, `.hcDark` (each a `MonarchThemeTokens`) and `MonarchTokens.Accents`, in the generated file                           | The generator                                                               |
| Component | One component's sizes, paddings and role choices per variant, size and state      | A `Monarch<Name>Tokens` file in the component's folder, for example `Components/Button/MonarchButtonTokens.swift` and `components/button/MonarchButtonTokens.kt` | By hand, per platform, from primitives (for lengths) and roles (for colour) |

Primitive and semantic tokens are kept apart in the generated file: primitives
are constants in named groups, semantic tokens are the fields of a theme
value. Nothing semantic is written in a platform folder, and no component
token defines a raw colour.

The theme layer (`theme/` on Android, `Theme/` on iOS) is not a token layer.
It converts the generated values to platform types (`Color`, `Dp`, `TextUnit`,
`CGFloat`) and exposes them through `MonarchTheme`. It adds no values of its
own.

Component tokens are hand-written and internal to each platform. A generated
component layer will be added when a component token has to be shared with
the web, and not before: a pipeline for it would have nothing to carry today.
Platform-specific adjustments, such as the 48dp and 44pt minimum touch
targets, are constants in the platform's accessibility helpers, not tokens.

## Names

Names are the same in Kotlin and in Swift, and are derived from the web name
by fixed rules. Given a web name you can work out the native one, and the
reverse.

### Semantic colour roles

Drop `--mn-color-`, then join the remaining parts in lower camel case.

| Web (CSS variable)              | Native role        |
| ------------------------------- | ------------------ |
| `--mn-color-bg-canvas`          | `bgCanvas`         |
| `--mn-color-text-muted`         | `textMuted`        |
| `--mn-color-border-input`       | `borderInput`      |
| `--mn-color-accent-solid-hover` | `accentSolidHover` |
| `--mn-color-danger-on-solid`    | `dangerOnSolid`    |
| `--mn-color-ai-diff-add-strong` | `aiDiffAddStrong`  |
| `--mn-color-focus-ring`         | `focusRing`        |
| `--mn-color-data-1`             | `data1`            |

Every theme has the same set of roles; the generator stops if one theme has a
role another lacks.

### Primitive groups

Each web group becomes a nested group of `MonarchTokens` (a Kotlin `object`,
a Swift `enum`).

| Web group                      | Native group    | Example                                     |
| ------------------------------ | --------------- | ------------------------------------------- |
| `--mn-space-*`                 | `Space`         | `--mn-space-4` → `Space.space4`             |
| `--mn-radius-*`                | `Radius`        | `--mn-radius-md` → `Radius.md`              |
| `--mn-control-*`               | `Control`       | `--mn-control-lg` → `Control.lg`            |
| `--mn-text-*`                  | `FontSize`      | `--mn-text-sm` → `FontSize.sm`              |
| `--mn-weight-*`                | `FontWeight`    | `--mn-weight-medium` → `FontWeight.medium`  |
| `--mn-leading-*`               | `LineHeight`    | `--mn-leading-snug` → `LineHeight.snug`     |
| `--mn-tracking-*`              | `LetterSpacing` | `--mn-tracking-caps` → `LetterSpacing.caps` |
| `--mn-duration-*`              | `Duration`      | `--mn-duration-fast` → `Duration.fast`      |
| `--mn-ease-*`                  | `Easing`        | `--mn-ease-standard` → `Easing.standard`    |
| `--mn-breakpoint-*`            | `Breakpoint`    | `--mn-breakpoint-md` → `Breakpoint.md`      |
| Neutral ramp                   | `Neutral`       | step 100 → `Neutral.neutral100`             |
| Disabled text                  | `Disabled`      | `Disabled.light`, `Disabled.dark`           |
| Brand colours                  | `Brand`         | `Brand.red`                                 |
| Status ramps                   | `Status`        | red, step 500 → `Status.red500`             |
| Spectrum ramps                 | `Spectrum`      | blue, step 500 → `Spectrum.blue500`         |
| `--mn-shadow-{xs,sm,md,lg,xl}` | theme `shadows` | `MonarchTokens.light.shadows.md`            |
| `--mn-focus-ring-width`        | theme field     | `MonarchTokens.light.focusRingWidth`        |

### Rules for keys

Kotlin and Swift identifiers cannot contain `-` or `.` and cannot start with
a digit, so three rules apply.

1. **Hyphens and dots become camel case.** `solid-hover` → `solidHover`,
   `stream-caret` → `streamCaret`, `hc-light` → `hcLight`.
2. **A leading number moves to the end.** `2xl` → `xl2`, `2xs` → `xs2`,
   `3xl` → `xl3`. So `--mn-radius-2xl` is `Radius.xl2` and `--mn-text-2xs` is
   `FontSize.xs2`.
3. **Steps of a numeric scale take the scale's name as a prefix, and a
   decimal point becomes an underscore.** Space `4` → `space4`; space `0.5`
   (`--mn-space-0-5` in CSS) → `space0_5`; `1.5` → `space1_5`. Colour ramps
   work the same way: neutral `100` → `neutral100`, red `500` → `red500`. A
   key that is a word stays a word: space `px` → `px`.

The underscore in `space0_5` is deliberate: `space05` would read as five.
The hand-written spacing scales in the theme layer keep the same names.

Names are not translated into platform vocabulary. The role is `bgSurface`
on both platforms, not `surface` on Android and `secondarySystemBackground`
on iOS; mapping to Material's or Apple's own names happens once, in the theme
layer.

### Themes and accents

| Web                         | Native                                             |
| --------------------------- | -------------------------------------------------- |
| light (default)             | `MonarchTokens.light`, id `"light"`                |
| dark                        | `MonarchTokens.dark`, id `"dark"`                  |
| high-contrast light         | `MonarchTokens.hcLight`, id `"hc-light"`           |
| high-contrast dark          | `MonarchTokens.hcDark`, id `"hc-dark"`             |
| default accent (Rook Red)   | `MonarchTokens.Accents.rook`                       |
| `data-mn-product="neutral"` | `MonarchTokens.Accents.neutral`                    |
| `data-mn-product="<id>"`    | `MonarchTokens.Accents.<id>`, for example `.atlas` |

`MonarchTokens.themes` lists the four themes and `MonarchTokens.Accents.all`
lists every accent.

## Units

The source writes lengths in `rem` or `px`. The generator converts them once;
the generated numbers carry no unit type, because the token modules have no
UI framework to take one from. The theme layer attaches the platform unit.

| Token                         | Source            | Kotlin                               | Swift                                 |
| ----------------------------- | ----------------- | ------------------------------------ | ------------------------------------- |
| Space, radius, control height | `rem` or `px`     | `Float`, read as dp                  | `Double`, read as points              |
| Type size                     | `rem`             | `Float`, read as sp                  | `Double`, read as points              |
| Breakpoint                    | `px`              | `Float`, a window width in dp        | `Double`, a window width in points    |
| Colour                        | HSL               | `Long`, `0xAARRGGBB`                 | `MonarchRGBA`, sRGB, 0 to 1           |
| Line height                   | number            | `Float`, a multiple of the type size | `Double`, a multiple                  |
| Letter spacing                | `em`              | `Float`, em                          | `Double`, em                          |
| Font weight                   | number            | `Int`, 100 to 900                    | `Int`, 100 to 900                     |
| Duration                      | `ms`              | `Int`, milliseconds                  | `Int`, milliseconds                   |
| Easing                        | `cubic-bezier(…)` | `MonarchCubicBezier`                 | `MonarchCubicBezier`                  |
| Shadow                        | layers            | list of `MonarchShadowLayer`, dp     | array of `MonarchShadowLayer`, points |
| Focus ring width              | `px`              | `Float`, dp                          | `Double`, points                      |

- `1rem` is 16, so `1rem` is 16dp or 16sp on Android and 16 points on iOS.
  `1px` is 1dp or 1 point: a CSS pixel, a dp and a point are all
  density-independent units.
- Type sizes are the size before the person's text-size setting. On Android
  they are applied as sp, on iOS they are scaled with Dynamic Type. A
  component never converts a type size to a fixed dp or point value.
- Breakpoints are window widths, not screen widths. Use them, or the
  platform's window size classes, against the window
  ([native adaptive layout](../guides/native-adaptive-layout.md)).
- `Radius.full` is `9999`; the platform clamps it to a capsule.

## Themes and accents in use

A theme value (`MonarchThemeTokens`) holds its id, whether it is dark, whether
it is high contrast, the focus ring width, the colour roles and the five
shadow levels. The high-contrast themes are the accessibility-driven
appearance: the theme layer selects them when the system asks for more
contrast.

An accent (`MonarchAccent`) holds one scale of ten accent roles for each of
the four themes. `withAccent` returns a theme with another accent applied:

```kotlin
val theme = MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas)
```

```swift
let theme = MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas)
```

`withAccent` replaces the ten `accent…` roles and the roles that follow the
accent in that theme. Which roles follow differs by theme and is read from
the token source, not written by hand: links follow the accent in every
theme, and the focus ring follows it in the standard themes only, because the
high-contrast themes draw the focus ring in the text colour.

Applications normally do not call `withAccent`. They pass the accent to the
theme entry point, which resolves scheme and contrast and applies it:

```kotlin
MonarchTheme(accent = MonarchTokens.Accents.atlas) {
    // content
}
```

```swift
ContentView()
    .monarchTheme(accent: MonarchTokens.Accents.atlas)
```

The status intents (`danger`, `success`, `warning`, `info`) and `ai` do not
change with the accent. The accent is the product's brand and never carries a
status meaning, as on the web.

A product's accent is derived from `registry/products/<id>.json` by the
tokens package, which also proves its contrast in every theme. An accent
therefore exists on native only once the product is registered. To theme an
app with any other colour at run time, use `MonarchBrandTheme.derive(brand:)`
(Swift and Kotlin), the native port of the web's `deriveTheme`, or the
`brand` parameter of the theme entry points. Its output is checked against
the web's by `npm run native:brand:check`; see
`platforms/ios/docs/BRAND-THEMES.md` and `platforms/android/docs/BRAND-THEMES.md`.

## What is not generated

| Not generated                                                                 | Why                                                                                                                                 |
| ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| z-index scale                                                                 | Stacking on native is decided by the view hierarchy and the platform's presentation APIs (dialogs, sheets, popovers), not a number  |
| CSS font family stacks                                                        | They are CSS fallback lists. Native uses the system typeface, or a family the app passes to the typography                          |
| Fluid `clamp()` sizes (display type, lead, section spacing, the fluid gutter) | They interpolate against the browser viewport. Native type scales with the text-size setting, and layout with the window size class |
| Marketing content widths and app-shell layout measurements                    | They describe web page layout                                                                                                       |
| Gradients                                                                     | They are CSS expressions over other tokens, and exist in the CSS output only                                                        |
| Legacy bridge names                                                           | They exist for web applications migrating from older variable names                                                                 |
| Component tokens                                                              | Hand-written per platform for now (see [The three layers](#the-three-layers))                                                       |
| Platform unit types (`Dp`, `TextUnit`, `Color`, `CGFloat`)                    | The token modules import no UI framework, so they stay usable anywhere and never block a Compose or SwiftUI upgrade                 |

Three kinds of value are not in the token source at all, on any platform:
opacity steps, icon sizes and border widths. They are not missing from the
native output; there is nothing to generate. A native component that needs
one writes it in its component tokens, built from the spacing scale where it
is a length (for example a hairline border is `Space.px`). A shared primitive
is added to `packages/tokens` first, for all three platforms, if one is ever
needed.

If a native component needs one of these, the answer is the platform
mechanism named in the table, not a new token. The generator stops with an
error when the token source gains a semantic token of a kind it has no native
output for, so a new kind of token is a deliberate decision and never a
silent omission.

## Ownership

- **The tokens package owns every value and every name.** A token is added,
  changed or removed in `packages/tokens`, under the review of the Monarch
  maintainers (`.github/CODEOWNERS`).
- **Platforms never edit generated files.** Both begin with a
  `Generated by … Do not edit.` header. A hand edit is overwritten by the
  next run, and fails `npm run native:tokens:check` before that. Static
  analysis and formatting on each platform skip these files.
- **The generator owns the mapping.** Naming rules and unit conversion live
  in `emit-native.ts`. A change to them renames public native API and is a
  breaking change on both platforms.
- **Each platform owns its theme layer and its component tokens.** These are
  reviewed by the platform owner and may differ between platforms where the
  platform conventions differ.
- **Token values are design, not API.** A value may change in any release. A
  name is API ([native API stability](native-api-stability.md)).

## Add a token

1. Add the raw value to `primitives.ts`, or the meaning to `themes.ts`, as
   for the web ([CONTRIBUTING](../../CONTRIBUTING.md)). Never add a raw
   colour to a component on any platform.
2. Build the tokens package and run its tests. A new colour role must pass
   the contrast tests in all four themes and for every product accent.
3. Run `npm run native:tokens`. Check the diff of the two generated files:
   the new name appears in both, spelled the same, and nothing else changed.
4. If the new token belongs to a hand-written mirror in the theme layer (for
   example the spacing scale exposed through `MonarchTheme`), add it there on
   both platforms, and extend the test that compares the mirror with the
   generated scale.
5. Add a changeset for `@rookcom/monarch-tokens`, and an `Added` entry in the
   changelog of each native platform.
6. Commit the source change and both generated files in the same pull
   request. `npm run native:tokens:check` must pass.

A new token is additive on all three platforms and needs no deprecation.

A new **kind** of token (a new group or a new DTCG type) needs generator
support first. Decide whether it has a native meaning; if it does not, add it
to [What is not generated](#what-is-not-generated) with the reason.

A new **theme** adds a field to every accent and a case to `withAccent`. That
changes the constructors of the generated accent types, which is a breaking
change for any app that builds its own accent value, and is released as one.

## Change a value

1. Change the value in the source and run the token tests. The contrast
   guarantees must still hold.
2. Run `npm run native:tokens` and commit the regenerated files with the
   source change.
3. Add a changeset for the tokens package and a `Changed` entry in each
   native changelog, saying what looks different.

A value change is not a breaking change. It reaches each platform's
applications with that platform's next release, so web, Android and iOS can
show the old and the new value at the same time for a while. When that
matters, for example for a brand colour, plan the three releases together.

Component code does not change when a value changes. If a value change
requires editing a component, the component was using a literal or the wrong
token, and that is the defect to fix.

## Deprecate or remove a token

Renaming or removing a token is a breaking change on every platform, and the
generated files cannot hide it: when a token leaves the source, the next run
removes it from Kotlin and Swift.

1. **Propose** it under the web
   [deprecation policy](DEPRECATION-POLICY.md), naming the replacement. One
   decision covers web, Android and iOS.
2. **Add the replacement** first, as in [Add a token](#add-a-token), and
   release it on all three platforms.
3. **Keep the old token in the source** for the whole overlap period, so it
   stays in the generated files. For a rename, the old name is an alias of
   the new one.
4. **Mark it.** On the web, `registry/deprecations.json` and the conformance
   rule report it. On native, mark the hand-written accessor in the theme
   layer where there is one, list the token under `Deprecated` in each native
   changelog with the replacement and the earliest removal version, and move
   Monarch's own components to the replacement in the same release.
5. **Wait** for the overlap required by
   [native API stability](native-api-stability.md#deprecation-policy) on each
   platform, and for the web period.
6. **Remove** the token from the source, regenerate, and release the removal
   on each platform as a breaking change with a migration note. The removal
   from the source can merge only when every platform is ready to release it,
   because the generated files change for all of them at once.

Not yet set up: the generator cannot print a deprecation marker
(`@Deprecated` in Kotlin, `@available(*, deprecated)` in Swift) on a
generated constant, because the token source has no deprecation field it
reads. A deprecated token that applications read directly from `MonarchTokens`
therefore produces no compiler warning today. Add that support to
`emit-native.ts`, driven by one list shared with the web, before the first
token is deprecated; until then the changelog is the only notice, and the
overlap must be generous.

## The rule for components

A component never uses a literal or a primitive for colour.

- **Colour comes from the theme's roles only**: `MonarchTheme.colors.…` on
  Android, `@Environment(\.monarchTheme)` and `theme.colors.…` on iOS. Not
  from `MonarchTokens.Neutral`, `Disabled`, `Status`, `Spectrum` or `Brand`, not from the
  platform's named colours, and not from a hex or RGB literal. The primitive
  colour groups say so in their documentation comments.
- The reason is that only roles are covered by the contrast report, and only
  roles change with the theme, the contrast setting and the accent. A
  primitive colour is correct in one theme at most.
- **Lengths, type and motion come from tokens**, through the theme
  (`spacing`, `shapes` or `radius`, `typography`, `motion`) or a component
  token built from primitives. A component token may read a primitive length
  such as `MonarchTokens.Control.md`; it may not invent a number.
- **Opacity is not a way to make a colour.** Use the role that exists for the
  state (`…Subtle`, `…SolidHover`, `textDisabled`, `overlayScrim`). If no
  role fits, the missing role is a token proposal.
- The exceptions are fully transparent and the platform's own semantic
  behaviour that Monarch deliberately leaves alone, such as system chrome.
- Applications follow the same rule. An app that reads a primitive colour
  loses dark mode, high contrast and the accent for that element.

On Android a test that reads the source (`SourceRulesTest` in
`monarch-compose`) fails when a component, a foundation or the theme uses a
literal colour, when a component reads a primitive colour from the token
module, or when a component uses a literal dp or sp size. On iOS the rule is
checked in review; an automated check is planned and not yet set up.

## Related

- [NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md)
- [Native API stability](native-api-stability.md)
- [Native platform evolution](native-platform-evolution.md)
- [Native release and maintenance](native-release-and-maintenance.md)
- [Contributing to the native platforms](../guides/native-contributing.md)
- [Theming (web)](../guides/theming.md)
