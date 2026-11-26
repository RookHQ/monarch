# ``MonarchUI``

The SwiftUI implementation of the Monarch design system: a theme, foundations
and a set of components that give SwiftUI controls the Monarch look while
they keep the platform's behavior.

## Overview

MonarchUI shares its tokens, colour roles, type roles and component vocabulary
with Monarch on the web and on Android. It is not a port of the web
components. It looks like Monarch and behaves like the platform. The field
box, button, checkbox, radio, segmented control, tabs, chip, badge, card,
banner, progress bar and list row have Monarch's own anatomy. Presented
surfaces and text machinery stay the system's, themed with Monarch's colour,
type and shape: dialog, sheet, menu, date and time picker, tooltip and switch.

The library has four layers, and a lower layer never imports a higher one:

1. **Tokens.** Generated constants in the `MonarchTokens` module.
2. **Theme.** `MonarchTheme` in the environment, installed by `monarchTheme()`.
3. **Foundations.** Type roles, spacing, radii, elevation, motion and
   accessibility helpers.
4. **Components.** Styles and views that read the theme only. They are
   grouped by the families of the web catalogue.

MonarchUI is not released yet. Its version is the `@rookcom/monarch-react`
version, in the 0.14.x patch series. The supported platform is iOS 17 and
later.

Time zone parameters of components take a Foundation `TimeZone`; `nil` means
`TimeZone.autoupdatingCurrent`. Values that are stored or sent, such as
`MonarchBusinessHoursValue.timeZone`, keep the IANA identifier as a `String`.

## Topics

### Essentials

- <doc:GettingStarted>
- ``MonarchTheme``
- ``MonarchAppearance``
- ``MonarchSchemePreference``
- ``MonarchContrastPreference``

### Theme values

- ``MonarchColors``
- ``MonarchColorRole``
- ``MonarchTypography``
- ``MonarchSpacing``
- ``MonarchRadius``
- ``MonarchElevation``
- ``MonarchElevationLevel``
- ``MonarchMotion``
- ``MonarchMotionDuration``
- ``MonarchMotionEasing``

### Foundations

- ``MonarchTextStyle``
- ``MonarchTone``
- ``MonarchAccessibility``
- ``MonarchIcon``
- ``MonarchIconSize``

### Actions

- ``MonarchButtonStyle``
- ``MonarchButton``
- ``MonarchButtonVariant``
- ``MonarchButtonSize``

### Data display

- ``MonarchBadge``
- ``MonarchBadgeVariant``
- ``MonarchCard``
- ``MonarchProgressViewStyle``

### Feedback

- ``MonarchBanner``

### Forms

- ``MonarchTextField``
- ``MonarchCheckboxToggleStyle``

### Layout

- ``MonarchDivider``

### Library

- ``MonarchUI/MonarchUI``
