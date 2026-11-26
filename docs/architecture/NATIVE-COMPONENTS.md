# Native component classification

How every component of Monarch for Android (`platforms/android`) and Monarch
for iOS (`platforms/ios`) is classified, where the classification is recorded,
and how many entries exist today. The architecture is in
[NATIVE-PLATFORMS](NATIVE-PLATFORMS.md); the plan for all 515 web components
is explained in [NATIVE-PARITY](NATIVE-PARITY.md).

Status: both platforms are unreleased; their version is the
`@rookcom/monarch-react` version, in the `0.14.x` patch series. The counts at the end of
this page change as work proceeds; they are regenerated from the data, not
written by hand.

## The three classes

A component is classified before any code is written, separately on each
platform. Work down the list and stop at the first class that fits.

| Class | Name                     | When it applies                                                                                                       | What Monarch ships                                |
| ----- | ------------------------ | --------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| A     | Native adoption          | The platform control, inside the Monarch theme, already has the right behaviour and accessibility                     | Nothing but a documented inventory entry          |
| B     | Monarch style or wrapper | The platform control is right, but needs Monarch's tokens or one consistent set of options across products            | A style, a modifier or a thin wrapper             |
| C     | Custom Monarch component | There is a real gap: no platform control provides the behaviour and its accessibility, or a cross-product requirement | A component, built from foundations and the theme |

Rules that follow from the classes:

- A web component existing is not a reason to build a native one. A class C
  entry needs the gap stated in its inventory entry.
- The class can differ between Android and iOS. Radio buttons are an example:
  Android has a Material 3 `RadioButton`, which Monarch wraps in a labelled row
  (B); iOS has no radio button, and the answer is `Picker` (A).
- When a platform later provides a control for a class C component, the
  default is to move to it and reclassify the entry as A or B
  ([platform evolution](../governance/native-platform-evolution.md#adopting-a-new-platform-api)).
- The process for proposing a component, and the promotion rule it must meet,
  is in [contributing](../guides/native-contributing.md#classify-before-you-build).

## Coverage values

`platforms/parity.json` records, for every web component, how each platform
covers it. The coverage value and the class are related but not the same:
coverage says what kind of answer exists, the class says how much Monarch
builds.

| Coverage   | Meaning                                                           | Class  | `kind` in the inventory        |
| ---------- | ----------------------------------------------------------------- | ------ | ------------------------------ |
| `build`    | Monarch builds a component: custom, or a wrapper with its own API | B or C | `wrapper`, `custom`, `style`   |
| `style`    | Monarch ships a style or theme layer on the platform control      | B      | `style`, `modifier`, `wrapper` |
| `native`   | The platform component or API is the answer; Monarch documents it | A      | `native`                       |
| `compose`  | A documented composition of other Monarch native components       | none   | `recipe`                       |
| `web-only` | No meaning in a native app; the reason is in the entry's `note`   | none   | `none`                         |

The inventory generators enforce part of this mapping: a `native` entry must
have `kind` `native` and class A, a `compose` entry must have `kind`
`recipe`, a `web-only` entry must have `kind` `none`, and `native`,
`compose` and `web-only` entries must not name a Monarch type. The checks are
in `platforms/ios/Scripts/generate.py` and
`platforms/android/build-logic/src/main/kotlin/com/rookhq/monarch/buildlogic/inventory/Conventions.kt`.

## Where the classification is recorded

| File                                         | What it is                                                                                    | Written by                                        |
| -------------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `platforms/parity.json`                      | The plan: one entry per web component, with the intended coverage on each platform and a wave | Hand, under the rules in NATIVE-PARITY            |
| `platforms/android/inventory/<id>.json`      | What Android documents or ships for one id: class, coverage, kind, API, examples, tests       | Hand, one file per entry                          |
| `platforms/ios/inventory/<id>.json`          | The same for iOS                                                                              | Hand, one file per entry                          |
| `platforms/<platform>/inventory/schema.json` | The schema every inventory file is validated against                                          | Hand                                              |
| `platforms/<platform>/components.json`       | All inventory entries in one file, read by the documentation site                             | Generated from `inventory/`; never edited by hand |

The inventory files are the source of truth for what exists. Each entry
carries the reason for its class in `category`, `kind`, `wrapsNative` (the
exact platform API) and `platformNotes` (how it differs from the other
platform and the web, and why). `parity.json` is the plan and does not
change when something ships.

Commands that read these files:

| Task                                                    | Android (from `platforms/android`) | iOS (from `platforms/ios`)     |
| ------------------------------------------------------- | ---------------------------------- | ------------------------------ |
| Validate the inventory and regenerate `components.json` | `./gradlew generateInventory`      | `Scripts/generate.py`          |
| Validate and fail if `components.json` is stale         | `./gradlew checkInventory`         | `Scripts/generate.py --check`  |
| Compare the inventory with `parity.json` (report only)  | `./gradlew parityReport`           | `Scripts/generate.py --parity` |

The counts on this page are regenerated at the repository root, with Node and
no dependencies:

```sh
node scripts/native/components-report.mjs          # rewrite the tables below
node scripts/native/components-report.mjs --check  # fail if they are stale
```

`npm run verify` runs the check as `npm run native:components:check`. Run the
rewrite when inventory files are added or removed.

## The first set and why

The wave 0 foundations, as recorded in the inventory files today.

| Id             | Android                                     | Class | iOS                                      | Class | Why the classes differ                                                                 |
| -------------- | ------------------------------------------- | ----- | ---------------------------------------- | ----- | -------------------------------------------------------------------------------------- |
| `theme`        | `MonarchTheme` over `MaterialTheme`         | B     | `MonarchTheme`, `.monarchTheme()`        | B     | Same class                                                                             |
| `text`         | `MonarchText` over `Text`                   | B     | `.monarchTextStyle(_:)` on `Text`        | B     | Same class; a function on Android, a modifier on iOS                                   |
| `button`       | `MonarchButton` over `Button`               | B     | `MonarchButtonStyle`, `MonarchButton`    | B     | Same class                                                                             |
| `text-field`   | `MonarchTextField` over `OutlinedTextField` | B     | `MonarchTextField` over `TextField`      | B     | Same class                                                                             |
| `checkbox`     | `MonarchCheckboxRow` over `Checkbox`        | B     | `MonarchCheckboxToggleStyle` on `Toggle` | B     | Same class; iOS has no checkbox control, so it is a toggle style                       |
| `switch`       | `MonarchSwitchRow` over `Switch`            | B     | `Toggle`                                 | A     | Android needs a labelled row that toggles as a whole; the iOS `Toggle` already is one  |
| `radio-group`  | `MonarchRadioRow` over `RadioButton`        | B     | `Picker`                                 | A     | iOS has no radio button and Monarch does not add one                                   |
| `card`         | `MonarchCard` over `Surface`                | B     | `MonarchCard`                            | B     | Same class                                                                             |
| `separator`    | `MonarchDivider` over `HorizontalDivider`   | B     | `MonarchDivider` over `Divider`          | B     | Same class                                                                             |
| `progress-bar` | `MonarchProgress`                           | B     | `MonarchProgressViewStyle`               | B     | Same class                                                                             |
| `spinner`      | `MonarchProgress` (circular)                | B     | `ProgressView()`                         | A     | The circular `ProgressView` needs nothing from Monarch                                 |
| `icon`         | `MonarchIcon` over `Icon`                   | B     | `MonarchIcon` over `Image(systemName:)`  | B     | Same class                                                                             |
| `badge`        | `MonarchBadge`                              | C     | `MonarchBadge`                           | C     | Neither platform has a status label in tones; Material 3 `Badge` is a notification dot |
| `alert`        | `MonarchBanner`                             | C     | `MonarchBanner`                          | C     | Neither platform has an inline, persistent status message                              |

Navigation, dialogs, sheets, menus, tabs, lists and date pickers are class A
on both platforms: the Material 3 and SwiftUI components are used inside the
Monarch theme. Their inventory entries are added as each is documented.

## Counts

Generated by `node scripts/native/components-report.mjs` from
`platforms/parity.json` and `platforms/*/inventory/*.json`. Do not edit the
tables by hand. "In inventory" counts inventory files, including
documentation-only (class A) entries; it is not a count of components Monarch
has built. These numbers change as work proceeds.

<!-- components-report:start -->

| Count                                        | Android | iOS |
| -------------------------------------------- | ------: | --: |
| Entries in the plan (`parity.json`)          |     515 | 515 |
| Inventory entries today                      |     519 | 519 |
| … for a web component id in the plan         |     515 | 515 |
| … with no web id (foundations)               |       4 |   4 |
| Plan entries with no inventory entry yet     |       0 |   0 |
| Inventory entries in class A                 |      41 |  57 |
| Inventory entries in class B                 |     217 | 223 |
| Inventory entries in class C                 |     261 | 239 |
| Inventory entries with status `available`    |     454 | 454 |
| Inventory entries with status `experimental` |      65 |  65 |
| Inventory entries with status `planned`      |       0 |   0 |
| Inventory entries with status `deprecated`   |       0 |   0 |

Android: planned coverage per family (from `parity.json`) and inventory entries today.

| Family                  | `build` | `style` | `native` | `compose` | `web-only` | Planned | In inventory |
| ----------------------- | ------: | ------: | -------: | --------: | ---------: | ------: | -----------: |
| actions                 |       5 |       2 |        2 |         1 |          0 |      10 |           10 |
| billing                 |      12 |       0 |        0 |         0 |          0 |      12 |           12 |
| charts                  |      18 |       0 |        0 |         0 |          0 |      18 |           18 |
| collaboration           |      31 |       0 |        2 |         2 |          0 |      35 |           35 |
| crm                     |       0 |       0 |        0 |         1 |          0 |       1 |            1 |
| data-display            |      24 |       0 |        1 |         4 |          0 |      29 |           29 |
| data-grid               |      12 |       0 |        1 |         5 |          0 |      18 |           18 |
| feedback                |      12 |       2 |        1 |         2 |          0 |      17 |           17 |
| files                   |      22 |       0 |        2 |         0 |          0 |      24 |           24 |
| forms                   |      30 |       3 |        0 |         1 |          0 |      34 |           34 |
| geo                     |       8 |       0 |        0 |         0 |          0 |       8 |            8 |
| identity                |      26 |       0 |        0 |        22 |          0 |      48 |           48 |
| intelligence            |      90 |       0 |        1 |         5 |          0 |      96 |           96 |
| interaction             |       5 |       0 |        3 |         0 |          0 |       8 |            8 |
| layout                  |      15 |       0 |       11 |        14 |          0 |      40 |           40 |
| navigation              |       7 |       0 |        6 |         3 |          0 |      16 |           16 |
| overlays                |       4 |       4 |        7 |         5 |          0 |      20 |           20 |
| scheduling              |      14 |       0 |        2 |         2 |          0 |      18 |           18 |
| search                  |       5 |       1 |        1 |         3 |          0 |      10 |           10 |
| technical               |      15 |       0 |        0 |         4 |          0 |      19 |           19 |
| typography              |       3 |       4 |        0 |         0 |          0 |       7 |            7 |
| workflow                |      24 |       0 |        0 |         3 |          0 |      27 |           27 |
| foundations (no web id) |       0 |       0 |        0 |         0 |          0 |       0 |            4 |
| Total                   |     382 |      16 |       40 |        77 |          0 |     515 |          519 |

iOS: planned coverage per family (from `parity.json`) and inventory entries today.

| Family                  | `build` | `style` | `native` | `compose` | `web-only` | Planned | In inventory |
| ----------------------- | ------: | ------: | -------: | --------: | ---------: | ------: | -----------: |
| actions                 |       5 |       3 |        2 |         0 |          0 |      10 |           10 |
| billing                 |      12 |       0 |        0 |         0 |          0 |      12 |           12 |
| charts                  |      18 |       0 |        0 |         0 |          0 |      18 |           18 |
| collaboration           |      31 |       0 |        2 |         2 |          0 |      35 |           35 |
| crm                     |       0 |       0 |        0 |         1 |          0 |       1 |            1 |
| data-display            |      19 |       4 |        2 |         4 |          0 |      29 |           29 |
| data-grid               |      12 |       0 |        3 |         3 |          0 |      18 |           18 |
| feedback                |      11 |       1 |        2 |         3 |          0 |      17 |           17 |
| files                   |      21 |       0 |        3 |         0 |          0 |      24 |           24 |
| forms                   |      28 |       2 |        3 |         1 |          0 |      34 |           34 |
| geo                     |       8 |       0 |        0 |         0 |          0 |       8 |            8 |
| identity                |      26 |       0 |        1 |        21 |          0 |      48 |           48 |
| intelligence            |      90 |       0 |        1 |         5 |          0 |      96 |           96 |
| interaction             |       4 |       0 |        4 |         0 |          0 |       8 |            8 |
| layout                  |      16 |       0 |       10 |        14 |          0 |      40 |           40 |
| navigation              |       6 |       1 |        5 |         4 |          0 |      16 |           16 |
| overlays                |       3 |       0 |       14 |         3 |          0 |      20 |           20 |
| scheduling              |      14 |       0 |        2 |         2 |          0 |      18 |           18 |
| search                  |       6 |       0 |        1 |         3 |          0 |      10 |           10 |
| technical               |      15 |       0 |        0 |         4 |          0 |      19 |           19 |
| typography              |       2 |       5 |        0 |         0 |          0 |       7 |            7 |
| workflow                |      22 |       0 |        2 |         3 |          0 |      27 |           27 |
| foundations (no web id) |       0 |       0 |        0 |         0 |          0 |       0 |            4 |
| Total                   |     369 |      16 |       57 |        73 |          0 |     515 |          519 |

<!-- components-report:end -->

## Related

- [NATIVE-PLATFORMS](NATIVE-PLATFORMS.md)
- [NATIVE-PARITY](NATIVE-PARITY.md)
- [NATIVE-COMPATIBILITY](NATIVE-COMPATIBILITY.md)
- [Contributing to the native platforms](../guides/native-contributing.md)
- [Native platform evolution](../governance/native-platform-evolution.md)
- [Native API stability](../governance/native-api-stability.md)
