# Changelog

All notable changes to Monarch for Android are recorded here. The format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Monarch for Android has the version of `@rookcom/monarch-react`, in the
`0.14.x` patch series, and shares it with Monarch for iOS; it is the Maven
version of both artifacts. The rules are in
[native release and maintenance](../../docs/governance/native-release-and-maintenance.md#changelog-standard).

## 0.14.6 (unreleased)

The first release of Monarch for Android. Not published to Maven Central yet.
Work done before the version was unified was numbered `0.1.0` and was never
released.

### Changed

- Parity round 9 with the web examples: the glyphs the web draws itself are now the default of the icon
  parameters of the AI, scheduling, collaboration, billing and data components (check, close, plus, retry, bell,
  chevrons, status and claim marks); pass `null` to draw none. An AI choice mark (agent, model, tone, knowledge scope)
  uses the AI accent. `MonarchKanbanBoard` no longer draws a tab strip: the columns sit side by side and the board
  scrolls sideways, with the next column peeking in on a phone. The draft queue shows draft and regenerating in the
  AI tone. A confidence badge, external link and toggle block draw their glyph by default. The burndown chart axis
  writes plain numbers; `formatValue` is for the values.

### Added

- Optional artifact `com.rookhq.monarch:monarch-fonts-poppins` (`MonarchPoppins`,
  `monarchPoppinsTypography()`): the Poppins typeface, SIL Open Font License 1.1,
  with the licence text. The default typeface stays the system font.
- Sample-only module `monarch-sample-icons` (outlined Material Icons, Apache
  License 2.0). It is not published and no library depends on it; the libraries
  bundle no icons.
- The Monarch visual layer, drawn from Compose foundation instead of stock
  Material widgets: the field box (`MonarchFieldSize` small, default and large,
  at 40, 48 and 52 dp), `MonarchDateField`, buttons, checkbox, radio and switch
  rows, segmented control, tabs, chips, badges, cards, banners, progress and
  list rows. Dialogs, sheets, menus, tooltips, pickers and the snackbar host
  stay platform surfaces, themed with Monarch's shape, colour and type. Material
  types are hidden from public signatures.
- `@ExperimentalMonarchApi` on the experimental components. Every inventory
  entry has a `status` (`available` or `experimental`) and a `layer`
  (`components` or `patterns`), checked by `npm run native:status:check`; the
  docs show a Pattern (experimental) badge.
- Primitive options: `MonarchCard` `selected` and `tone`, `MonarchTabs` rich
  tabs, `MonarchListRow.onClickLabel`, `MonarchChip` `secondaryText`,
  `MonarchDialog.startActions`, `MonarchButtonVariant.Inverse` and `Ai`,
  `MonarchIconButton` `tone` and `outlined`, `MonarchPopover.anchorX`.
- Screenshot tests (`verifyScreenshots`, Roborazzi on the JVM) and the docs
  previews (`recordPreviews`), at `previews/<id>.<theme>.png` in light and dark.
  The previews are set in Poppins.
- Dokka HTML of both libraries, published at `/android/api/` on the docs site.
- Release: signed publication to Maven Central through the Central Portal from
  `main` (skipped with a notice without its secrets), with a build provenance
  attestation of the published files.
- `monarch-tokens`: generated token constants (primitives, four themes with
  96 colour roles each, five shadow levels, 21 accents) as a pure Kotlin
  library.
- `monarch-compose`:
  - `MonarchTheme` with scheme, contrast and accent inputs, resolved by
    `resolveMonarchTheme`; system contrast detection; one mapping to the
    Material 3 colour scheme, typography and shapes; dynamic colour as an
    opt-in.
  - Foundations: `MonarchColors`, `MonarchTypography` and `MonarchTextStyle`,
    `MonarchSpacing`, `MonarchShapes`, `MonarchElevation` with
    `Modifier.monarchShadow`, `MonarchMotion`, `MonarchTone`.
  - Accessibility helpers: `Modifier.monarchMinimumTouchTarget`,
    `Modifier.monarchFocusRing`, `isReducedMotionEnabled`,
    `isSystemInHighContrast`.
  - Components: `MonarchText`, `MonarchButton`, `MonarchTextField`,
    `MonarchCheckboxRow`, `MonarchSwitchRow`, `MonarchRadioRow`,
    `MonarchCard`, `MonarchDivider`, `MonarchProgress`, `MonarchIcon`,
    `MonarchBadge`, `MonarchBanner`.
  - `@ExperimentalMonarchApi` opt-in annotation, on the experimental
    components.
- `sample`: catalogue app with a theme and accent switcher, and navigation
  by family: families, the pages of a family, one page per component.
- `inventory/<id>.json`: one documentation file per web component id, with
  the same schema as iOS. `components.json` is generated from them.
- Build: convention plugins, version catalog, unit and Compose UI tests on
  the JVM, public API check, detekt, Android lint, local Maven publication.
- Structure for adding components in parallel
  (`docs/ADDING-COMPONENTS.md`): family packages
  `com.rookhq.monarch.components.<family>.<name>`, per-component strings,
  tests, fixtures, previews, catalogue pages and inventory files; generated
  example sources, fixture registry and catalogue navigation; the tasks
  `componentCheck`, `verifyLocal`, `mergeCheck` and `verifyMonarch`.
- Shared internals for component authors (`com.rookhq.monarch.internal`):
  tone palette, surface modifier, control state, shared shapes, string
  lookup, image with placeholder, labelled control row.
- Dependencies available to components: Material 3 adaptive (`adaptive`,
  `adaptive-layout`, `adaptive-navigation`, `material3-adaptive-navigation-suite`)
  and `activity-compose`.

### Changed

- Shared names with iOS: the condition model is `MonarchCondition`,
  `MonarchConditionField`, `MonarchConditionFieldType`,
  `MonarchConditionOperator`, `MonarchConditionValue`,
  `MonarchConditionProblem` and `MonarchConditionMatch` (the query builder keeps
  `MonarchQueryGroup` and `MonarchQueryCombinator`), and the action editor's
  inputs are `parameters`, not `params`. MONARCH-COMPAT: source that used the
  earlier `MonarchQuery*` and `params` names does not compile; rename them.
- A component parameter that takes a time zone is the IANA identifier as
  `String?` (it was `java.util.TimeZone`); `null` is the device zone. Values
  that are stored or sent keep an IANA `String`. MONARCH-COMPAT: pass
  `zone.id` where a `TimeZone` was passed.
- The field label sits above the box, the focus ring alone shows focus (the
  border keeps its resting colour), disabled controls use opaque quiet colours,
  presented surfaces share one soft elevation, and every button label is
  14 sp. MONARCH-COMPAT: screenshot tests of an app that includes Monarch
  components need new baselines; no semantics, roles or parameters changed
  except where listed here.
- The dark theme's subtle background and default border are now distinct from
  the surface and muted fills (the tokens fix of `@rookcom/monarch-tokens`).
  MONARCH-COMPAT: dark screens show slightly different greys; no action needed.
- `MonarchNumberInput` and `MonarchCurrencyInput` pass every edit that reads
  as a number to `onValueChange` (and `onMinorUnitsChange`) at once, also a
  number outside `minimum`/`maximum`, so a button tapped right after typing
  reads what is on screen; the range message still appears when the field is
  left (C-021, same as iOS). MONARCH-COMPAT: a value out of range can now
  reach the app before the field is left; check the bounds before saving.
