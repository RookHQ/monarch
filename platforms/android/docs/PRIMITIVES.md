# Monarch primitives and the stock widgets they replace

Monarch for Android draws its own controls where the Material 3 widget cannot match the Monarch look (decision
D10): sizes, borders, the selected look and the states come from the web components, and the behaviour (roles,
focus, touch targets, TalkBack, IME) stays the platform's. A component of the library or an app uses the
Monarch primitive, not the stock widget. This page lists each primitive, the stock widget it replaces and how to
migrate a call. Each lane of the restyle appends its section; keep every section when merging.

Rule: looks like Monarch, behaves like the platform. A Monarch primitive draws its own anatomy (size, radius, border,
colour per state) and keeps the platform behaviour: `Modifier.toggleable` / `selectable` with the right `Role`, a 48dp
touch target around a smaller visible shape, the focus ring for keyboard and D-pad focus, text that wraps at font
scale 2.0, RTL.

## Containers, navigation and buttons

### MonarchButton and MonarchIconButton (replace `Button`, `OutlinedButton`, `TextButton`, `IconButton`)

- Visible heights follow the web: `Sm` 32dp, `Md` 36dp (the default), `Lg` 40dp, with a 48dp touch target
  reserved around the button. A tap in the reserved space presses the button.
- Variants: `Primary`, `Secondary` (outlined), `Ghost`, `Danger`, `DangerSubtle`, `Success`, `Warning`, `Link`.
  Hover and pressed use the `*SolidHover` and `*SolidActive` roles (muted and emphasis for neutral buttons).
- Disabled is the variant at half opacity, like the web. Loading keeps the size of the button: the spinner takes
  the place of the leading icon, or is drawn over the label when there is no icon. TalkBack reads "Loading".
- `TextButton { Text(..) }` becomes `MonarchButton(text = .., variant = MonarchButtonVariant.Ghost)` (or `Link`).
- `IconButton(onClick) { Icon(.., contentDescription) }` becomes
  `MonarchIconButton(label = "<what it does>", onClick = ..) { MonarchIcon(.., contentDescription = null) }`.
  `label` is the TalkBack name and is required; there is no unnamed icon button.

### MonarchTabs (replaces `PrimaryTabRow`, `SecondaryTabRow`, `PrimaryScrollableTabRow`, `Tab`)

- The line tabs of the web: the selected tab keeps the default text colour and has a 2dp accent underline, the
  other tabs are muted, a 1dp divider runs under the row. The row scrolls sideways when the labels do not fit.
- `PrimaryTabRow(selectedTabIndex = i) { Tab(selected = i == 0, onClick = { i = 0 }, text = { Text("A") }) }`
  becomes `MonarchTabs(selectedIndex = i, onSelectedIndexChange = { i = it }) { tab("A") }`. A count is
  `tab("Open", count = "6")`; the content of the selected tab can be passed as `panel = { index -> .. }`.
- The web's pill look of tabs is the segmented control; use `MonarchSegmentedControl`.

### MonarchListRow (replaces `ListItem`)

The slot names are the same as `ListItem`, so a call changes by its name:

| `ListItem(...)`                                                              | `MonarchListRow(...)`                                                                                     |
| ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `headlineContent`, `supportingContent`, `overlineContent`                    | the same names                                                                                            |
| `leadingContent`, `trailingContent`                                          | the same names (the leading slot is a 24dp box)                                                           |
| `modifier = Modifier.clickable(role = Role.Button) { open() }`               | `onClick = { open() }` (the row is one tap target with the button role, pressed, hovered and focus looks) |
| `colors = ListItemDefaults.colors(...)`, `tonalElevation`, `shadowElevation` | remove; a selected row is `selected = true`                                                               |
| a `HorizontalDivider()` after each row                                       | `divider = true`                                                                                          |

The row is at least 48dp high with 16dp at the sides; the headline is 14sp medium, the supporting line 14sp
muted. Text inside the slots inherits the row's styles, so `Text(text)` with no style is right; a call that
passes its own `style` keeps it.

### MonarchCard

- Fills the available width by default (`fillWidth = false` to wrap), 20dp content padding like the web.
- `header = { MonarchCardHeader(title = .., description = ..) }` and `footer = { .. }` are optional slots; the
  footer sits under a subtle divider.

### MonarchSpinner

- A 2dp ring with one open quarter, 14, 16 or 24dp, one turn in 0.7 seconds (still when animations are off).
  `CircularProgressIndicator()` with no progress becomes `MonarchSpinner()`; a known progress stays
  `MonarchProgress(progress = ..)`.

## Selection controls

| Stock call                                                                                                | Monarch call                                                                          | Notes                                                                                                                                                                                                        |
| --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Checkbox(checked, onCheckedChange, modifier, enabled, colors, interactionSource)`                        | `MonarchCheckbox(checked, onCheckedChange, modifier, enabled, interactionSource = s)` | Drop `colors`. `onCheckedChange = null` draws the box only, as in Material. Add `label = "..."` to make a labelled row.                                                                                      |
| `TriStateCheckbox(state, onClick, ...)`                                                                   | `MonarchCheckbox(state, onClick, ...)`                                                | `state` is `ToggleableState`; the dash is TalkBack "partially checked".                                                                                                                                      |
| `Row { Checkbox(...); Text(...) }` with `toggleable`                                                      | `MonarchCheckbox(checked, onCheckedChange, label = text)` or `MonarchCheckboxRow`     | One TalkBack element, 48dp row, label wraps.                                                                                                                                                                 |
| `RadioButton(selected, onClick, modifier, enabled, colors, interactionSource)`                            | `MonarchRadio(selected, onClick, modifier, enabled, interactionSource = s)`           | Drop `colors`. Add `label`.                                                                                                                                                                                  |
| `Column(Modifier.selectableGroup()) { ... }` of radios                                                    | `MonarchRadioGroup(label = "...") { ... }`                                            | Keeps the group semantics ("1 of 3"); the optional label is also the group name.                                                                                                                             |
| `Switch(checked, onCheckedChange, modifier, thumbContent, enabled, colors, interactionSource)`            | `MonarchSwitch(checked, onCheckedChange, modifier, enabled, interactionSource = s)`   | Drop `colors` and `thumbContent`. Same 52 by 32dp geometry. Add `label` for a row with the switch at the end.                                                                                                |
| `SingleChoiceSegmentedButtonRow { SegmentedButton(selected, onClick, shape = ..., label = { Text(t) }) }` | `MonarchSegmentedControl { Segment(selected, onClick, label = t) }`                   | Drop `shape`, `colors`, `border`. No check icon: the chosen segment is the raised pill. For a plain list: `MonarchSegmentedControl(options, selectedIndex, onSelect)`. Icons: `icon = { MonarchIcon(...) }`. |
| `FilterChip(selected, onClick = { set(!selected) }, label = { Text(t) }, leadingIcon = i)`                | `MonarchChip(text = t, selected = selected, onSelectedChange = set, leadingIcon = i)` | No check icon by default. 28dp, 48dp target.                                                                                                                                                                 |
| `InputChip` / `AssistChip` used as a removable chip                                                       | `MonarchChip(text, onRemove = ...)`                                                   | Draws a close mark; Delete and Backspace remove.                                                                                                                                                             |
| `Slider(value, onValueChange, ..., colors, thumb, track)`                                                 | `MonarchSlider(value, onValueChange, ...)`                                            | Drop `colors`, `thumb`, `track`. `label` and `valueLabel` add the label row and the TalkBack state.                                                                                                          |
| `RangeSlider(value, onValueChange, ...)`                                                                  | `MonarchRangeSlider(value, onValueChange, ...)`                                       | Same; `valueLabel` formats each end.                                                                                                                                                                         |

Rules for every call site:

- Keep the surrounding semantics (`contentDescription`, `testTag`) on the `modifier`.
- Do not wrap a Monarch control in your own `toggleable` / `selectable`: pass `label` (or use the row) instead, or pass
  `onCheckedChange = null` / `onClick = null` to draw the control only inside a row you made clickable.
- Colours come from the theme. A caller never passes colours.

### How they look and feel

Every selection control follows the premium checklist (`consolidation/visual-spec/PREMIUM.md`):

| Control                               | Visible shape                                                                                                                            | Motion                                                                                                  | Haptic                                        |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| `MonarchCheckbox`                     | 16dp box, 4dp corners, 1dp input border; on: accent fill with a 1.5dp check (round caps and joins) or a dash                             | the check is drawn on along its path with a short spring; colours cross-fade in 120ms                   | none (Android checkboxes give none)           |
| `MonarchRadio`                        | 16dp ring, 1dp input border; on: accent ring and a 10dp dot                                                                              | the dot grows in on a short spring with a small settle                                                  | none                                          |
| `MonarchSwitch`                       | 52 by 32dp track; thumb 16dp off, 24dp on, 28dp pressed                                                                                  | the thumb travels on a spring with a small settle (under 1dp, inside the track); the layout never moves | `ToggleOn` / `ToggleOff` on every user toggle |
| `MonarchSegmentedControl`             | muted track (radius md), one raised indicator (radius sm): the surface with a hairline in light themes, one surface step up in dark ones | the indicator slides to the chosen segment on a spring with no overshoot; the segments never move       | `SegmentTick` when the choice changes         |
| `MonarchSlider`, `MonarchRangeSlider` | 6dp track, 18dp surface thumb with a 1dp ring in the range colour                                                                        | follows the finger                                                                                      | none                                          |
| `MonarchChip`                         | 28dp (24dp small), radius md, hairline border; on: accent tint, accent text, accent border                                               | colours cross-fade in 120ms                                                                             | none                                          |

Shared rules:

- **Disabled is muted, never translucent**: a disabled box takes the subtle fill and the default border, a disabled
  radio a grey dot, a disabled switch a grey track and thumb; the label takes the disabled text colour.
- **No shadows** on controls. The one exception is a half-pixel hairline under a white thumb (the switch when it is
  on, the slider) in light themes; dark themes use surface steps instead.
- **Focus**: keyboard, D-pad and switch-access focus draw a 2dp ring (3dp in high contrast) with a gap outside the
  visible shape. Touch never shows it.
- **Hit area**: 48dp around every visible shape, through padding, never by making the shape bigger.
- **Reduced motion**: every spring becomes a snap when the system's animator duration scale is 0.

### Rows and groups built on them

| Component                                                   | Built on                                                             | Notes                                                                                                                                                                                            |
| ----------------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `MonarchCheckboxRow`, `MonarchRadioRow`, `MonarchSwitchRow` | the primitive with `label`                                           | One TalkBack element, 48dp row, wrapping label.                                                                                                                                                  |
| `MonarchCheckboxGroup`                                      | `MonarchCheckbox` rows inside `MonarchFormField`                     | With "Select all", the options are indented by the box and its gap, so they line up under the "Select all" label.                                                                                |
| `MonarchRadioCardGroup`                                     | `Modifier.selectable(Role.RadioButton)` cards in a `selectableGroup` | Hairline card (radius lg); chosen: accent tint and a 2dp accent edge drawn inside the card, so nothing moves. Title 14sp medium, meta 14sp regular with tabular figures, description 12sp muted. |
| `MonarchSwitchSettingsList`                                 | `MonarchSwitch` rows                                                 | Header: 14sp semibold title, 14sp muted description. Rows sit in one hairline card (radius lg) with a subtle divider between them, as on the web.                                                |

Counts of stock calls still outside these folders (the later caller migration, W5): `Checkbox` 16, `TriStateCheckbox`
8, `RadioButton` 20, `Switch` 2, `SegmentedButton` 17, `FilterChip` 15, `AssistChip` 2, `Slider` 1, `RangeSlider` 1
(counted on 2026-11-17, comments excluded).

## Gaps closed after the first composed preview pass (W7)

Each addition is optional and keeps every existing call compiling; the API dump was updated on purpose.

| Primitive                             | Addition                                                                                                                                                                                                      | Used by                                                                                                                                                                                                                                      |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MonarchCard` (both overloads)        | `selected` (subtle accent fill, 2dp accent edge drawn inside, TalkBack selected state) and `tone` (border in the tone's colour; selected wins)                                                                | `MonarchSectionCard` (tone), `MonarchIssueCard` (selected; the drag lift still uses `elevation.presented` on its modifier). `MonarchFileCard` stays on `Surface`: its fill also depends on pressed and disabled, which a card does not have. |
| `MonarchTabs` / `tab(...)`            | `secondary` (a second muted line under the label) and `accent` (the strong accent on the text, for "today"; the underline still shows the selection)                                                          | Shift roster day tabs (weekday over the day number, "Today" in the accent), timesheet day tabs (the day over "Today")                                                                                                                        |
| `MonarchListRow`                      | `onClickLabel`, what TalkBack says a double tap does                                                                                                                                                          | The audit log recipe ("Details")                                                                                                                                                                                                             |
| `MonarchChip`                         | `secondaryText` on the text chip; a second overload with a column `content` slot and `singleChoice` (radio role)                                                                                              | The slot picker (time over seats left, struck through when taken)                                                                                                                                                                            |
| `MonarchDialog`                       | `startActions`, a slot at the start of the action row opposite `actions`. `actions` already takes any number of buttons                                                                                       | A third action beside Cancel and Save. The platform date picker dialog used by the editable cell has `neutralLabel` / `onNeutral` for the same ("None")                                                                                      |
| `MonarchButton` / `MonarchIconButton` | variants `Inverse` (inverse background, for dark bars and photos) and `Ai`; toggle icon buttons take `tone` (on colour: accent, AI or a status) and `outlined` (hairline: default border off, tone border on) | Call controls. The call round control keeps its own anatomy for now (see below)                                                                                                                                                              |
| `MonarchPopover`                      | `anchorX`: open centred on a point of the anchor instead of at its start edge                                                                                                                                 | `MonarchUptimeBar` details, replacing the stock `TooltipBox` and `RichTooltip`                                                                                                                                                               |

Decisions:

- **Multi-choice segmented control: not added.** A set of independent on and off choices is the chip group
  (`MonarchChip` with `selected`), which is also what the web does; a segmented control stays one choice of a few.
- **Call controls** keep their round control (it has solid danger, inverse and toggled-off states and a menu
  arrow). `Inverse`, `Ai` and the outlined toggle are the pieces to move it onto `MonarchIconButton`; the move is a
  separate change because the control's TalkBack structure is tested.
- **`MonarchAiGhostText`** is the one caller still on a stock field; the reason is written in
  `StockWidgetRulesTest` next to its allow-list entry. The allow-list otherwise holds only the platform layer
  (dialog, sheet, menu, tooltip, date and time pickers, slider, select, divider, combobox).
- **Contrast test of fields:** `MatrixAxes` no longer skips editable fields. An editable node spans its whole
  decoration, so it is measured by the colour its text style draws in against the colour that fills the node.

Composed-recipe alignment rules used by this pass:

- A ghost (text) button at the start of a row is pulled left by its label padding
  (`Modifier.monarchGhostStartAlign`, internal) so the label sits on the text edge above it; a bordered first button
  already does. Only the empty strip at the outer edge leaves the parent.
- A control with a 48dp target and a 16dp visible shape is not inset by its target inside a list row: the box and
  the avatar are one toggle (as in a mail list), so the box sits on the same left edge as "Select all".
- A row of controls that scrolls sideways fades at an edge that has more (`Modifier.monarchScrollEdgeFade`,
  internal) instead of cutting a control in half.
