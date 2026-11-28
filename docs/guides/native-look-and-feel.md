# The Monarch look on Android and iOS

What a Monarch screen on Android or iOS looks like, what it leaves to the
platform, and the finish rules every component follows. Written for people who
build apps with Monarch. The architecture is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md); the component checklist
for contributors is in [contributing](native-contributing.md).

## The rule: looks like Monarch, behaves like the platform

The look comes from the web design: the same tokens, colour roles, type roles,
radii and spacing. The behaviour comes from the platform: touch targets, focus,
haptics, keyboard, text selection, scrolling, system settings, and the
accessibility services (TalkBack, VoiceOver).

| Layer                                                   | Who draws it                                                                                                                                                             |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Fields, buttons, checkboxes, radio buttons              | Monarch, with its own anatomy (a field box, a button, a check). Android: Compose foundation (`BasicTextField`, `toggleable`, `selectable`). iOS: custom styles and views |
| Segmented controls, tabs, chips, badges, cards, banners | Monarch, the same way                                                                                                                                                    |
| Progress indicators and list rows                       | Monarch                                                                                                                                                                  |
| The iOS switch                                          | The system `Toggle`, tinted with the accent                                                                                                                              |
| Dialogs, sheets, menus, date and time pickers, tooltips | The platform's surface, themed with Monarch's shape, colour and type                                                                                                     |
| The snackbar host, the keyboard, text selection, scroll | The platform                                                                                                                                                             |

Material types do not appear in Monarch's public Android signatures, so an app
does not need Material 3 to call a Monarch component. A Monarch screen is not
meant to be pixel-identical to the web or to the other platform: a sheet with
detents belongs to iOS and a back gesture to Android.

Measures that follow from the rule:

- Fields are 48 dp (Android) and 44 pt (iOS) high at the default size, with a
  small size of 40 and a large size of 52.
- Buttons show the web's heights (32, 36 and 40), and keep a 48 dp or 44 pt
  interactive area by padding the hit area, never by making the control bigger.
- Control text is 14, in sp or scaled points, so Dynamic Type and the font
  scale work. Nothing is clipped at font scale 2.0.

## Finish rules

Every component, showcase and composed pattern follows these rules, which
come from the design direction "clean, minimal, modern". They are checked on
rendered images, not only in code.

**Type**

- One hierarchy per surface: title, then body, then meta. A body is never
  larger or heavier than its title.
- At most three text sizes and two weights (regular and medium or semibold) in
  one component. No bold body text. Numbers in metrics use tabular figures.
- Secondary text uses the muted token, never a lighter grey chosen locally.

**Space**

- A 4 grid, from the spacing tokens only. Component padding is 16 (cards 16 to
  20, compact rows 12). Gaps between label, field and helper text are 6 to 8.
- Everything in a container aligns to one start edge. No dead space under the
  last element of a card: top and bottom padding are equal.

**Surface**

- Hairline borders and radii from the tokens, the same across a screen.
- No shadows on buttons, fields, chips, list rows or inline cards. Only
  presented surfaces (menu, popover, dialog, sheet, toast) are elevated, with
  one soft shadow. The dark theme uses surface steps, not shadows.
- Colour is quiet. The accent appears on the primary action, selection, the
  focus ring, progress and links. Status colours are a soft tint with strong
  text, never a saturated block.

**Controls**

- Icons are outlined, 16, 20 or 24 by size, in the text colour or the muted
  colour.
- States are visible and subtle: hover and pressed are one surface step;
  focus is a 2 px ring with an offset, and the border keeps its resting colour;
  disabled uses muted text and a quiet fill, not low opacity; an error shows a
  red border, an icon and a message.
- Touch targets are 48 dp (Android) and 44 pt (iOS), through the hit area.

**Motion**

- Platform motion: Compose springs and SwiftUI `.smooth` or `.snappy`, 150 to
  250 ms, with reduced motion respected. Haptics only where the platform's own
  apps use them: toggles, selection and success.
- A change of content (a number, a tab, a segment) moves the indicator, not
  the whole layout.

## Fonts

The default typeface is the system font: Roboto on Android, San Francisco on
iOS. Apps pay no size cost for a font they do not use.

Poppins, the brand typeface on the web, is an optional separate package:

- Android: the artifact `dev.monarchui:monarch-fonts-poppins`.
- iOS: the SwiftPM product `MonarchFontsPoppins`.

The font files are licensed under the SIL Open Font License 1.1, and the licence
text ships with each package. Rook products, the docs previews and the review
sheets use Poppins, so a native preview compares fairly with the web image.
Usage is in [Android](../../platforms/android/docs/USING-MONARCH.md#poppins-optional)
and [iOS](../../platforms/ios/docs/USING-MONARCH.md#poppins-optional).

## Icons

- The libraries bundle no icons. A component that shows an icon takes it from
  the app (an `ImageVector`, a `Painter` or a slot on Android; a symbol name
  or a view on iOS). Icon-only controls always carry a label.
- The sample apps, the examples and the previews use real outlined icons:
  Material Icons (Outlined, Apache License 2.0) on Android, through the
  sample-only module `monarch-sample-icons`, which no library depends on; SF
  Symbols on iOS. These are sample and test icons. Do not copy the module into
  a product, and do not treat the icon set as part of Monarch. The credit is in
  [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md).
- AI features use a robot glyph, never sparkles.

## Experimental components and patterns

Every component has a status, `available` or `experimental`, and a layer,
`components` or `patterns`. The domain composites (AI panels, product screens
and similar) are experimental and sit in the patterns layer; general
navigation, form, search, data, builder, chat-control and layout components
are available. The split is in the docs and the inventory today; moving the
patterns to a module of their own waits for the triggers in
[ADR 0017](../adr/0017-native-patterns-layer.md).

- A stable component's public API never exposes an experimental type.
- Experimental API can change in any release, without the deprecation window.
  It needs an opt-in: `@_spi(Experimental) import MonarchUI` on iOS and
  `@OptIn(ExperimentalMonarchApi::class)` on Android.
- The docs show a Pattern (experimental) badge and an Experimental badge from
  the inventory, and the catalogue has a group for the patterns.

The list of experimental components is not repeated in prose, so it cannot go
stale. Read it from the inventory (`"status": "experimental"` in
`platforms/<platform>/inventory/<id>.json`), from the count table in
[NATIVE-COMPONENTS](../architecture/NATIVE-COMPONENTS.md), and, on iOS, from
`platforms/ios/api/MonarchUI.experimental.txt`.

## Time zones

| Where                                                                                                                 | Android                         | iOS                                            |
| --------------------------------------------------------------------------------------------------------------------- | ------------------------------- | ---------------------------------------------- |
| A component parameter                                                                                                 | the IANA identifier, `String?`  | Foundation `TimeZone?`                         |
| The default (`null` or `nil`)                                                                                         | the device zone, read each time | `TimeZone.autoupdatingCurrent`, read each time |
| A value that is stored or sent between processes (for example `MonarchBusinessHoursValue.timeZone`, slot-picker data) | the IANA `String`               | the IANA `String`                              |

The web uses an IANA string everywhere. A `String` keeps a Compose parameter
stable and immutable; `TimeZone` is what Foundation calls take.

## Accessibility and checks on a device

Every component keeps the contract in
[accessibility and testing](native-accessibility-and-testing.md): roles, states
and labels, touch targets, font scale and Dynamic Type, right to left, contrast
and reduced motion. Automated tests check these on the JVM and in the
simulator. Some behaviours need a physical device with TalkBack or VoiceOver
and cannot be proven in a test; they are listed in
[device checks](native-device-checks.md).
