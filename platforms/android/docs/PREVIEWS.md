# Docs previews (Android)

The preview of a component is its primary example, alone, on the theme canvas. It is not a regression baseline:
those stack every fixture and are stored in `src/test/screenshots/` (`src/test/screenshots/README.md`).

## What an image shows

- Every id is shown through its primary example, which reproduces the web primary example (CROSS-PLATFORM.md
  section 1). A curated showcase (`screenshots/showcase/`, `PreviewShowcases.all`) is used only for an id with no
  example.

- The first entry of `examples` in `inventory/<id>.json` (the code the docs page shows). A component with no
  example is shown through its first fixture.
- The window is painted with the theme canvas, with the system night mode of the theme, and an example with several
  top-level composables is stacked in a column (12dp apart), as on the docs page.
- The text is set in Poppins (the `monarch-fonts-poppins` pack, D10.4); the library default and the regression
  baselines stay on the system font.
- A phone window: 392dp wide at density 3.0 (xxhdpi), so 1176 px wide. The example is laid out at that width,
  24dp from the edge, on the canvas of `MonarchTheme`, with a height that wraps it (at most 760dp). The image is the
  measured frame: no fixed crop, no labels, no dividers.
- A recipe that fills the screen (`fillMaxSize()` in its code) gets a 760dp tall frame with 16dp margins and is ended
  at the last gap between blocks in its bottom 140dp, so a line is never cut in half.
- A dialog, sheet or menu is a window of its own: the whole screen is captured over its scrim and cropped to the rows
  that differ from the scrim plus 48dp.
- Themes: `light` and `dark` for every component; also `hc-light` and `hc-dark` for the core set
  (`src/test/screenshots/core-components.txt`). Files are named `<id>.<theme>.png`.

## Commands (from `platforms/android`)

```
./gradlew recordPreviews -Pmonarch.previews.dir=<folder> [-Pmonarch.previews.ids=a,b]
```

Without `-Pmonarch.previews.dir` the folder is `monarch-compose/build/outputs/previews`. The task runs
`PreviewCaptureTest` (Robolectric, native graphics, `@Config(qualifiers = "+w392dp-h2000dp-xxhdpi")`) and then
`scripts/native/compress-previews.py <folder>`: palette conversion when an image has 256 colours or fewer, `oxipng
-o4 --strip all`, and, only for an image still over 150,000 bytes, a 256-colour quantise without dithering that is
kept when it is smaller. `PreviewCaptureTest` does nothing in any other task (`verifyMonarch` is unchanged).

## Icons in examples

Examples, showcases, tests and the sample app draw their icons from `SampleIcons`
(`monarch-sample-icons`, never published and never a dependency of `monarch-compose`): the Material icons in
the Outlined style, 24dp, tinted with the content colour. Names follow the role (`SampleIcons.Search`,
`SampleIcons.Close`, `SampleIcons.ChevronRight`), so the icon source can change in one place. An inventory
example adds `dev.monarchui.sampleicons.SampleIcons` to its `imports`.

- An icon slot: `MonarchIcon(SampleIcons.Search, contentDescription = null)`.
- A slot that takes a painter: `rememberVectorPainter(SampleIcons.BulletList)`.
- A photo (avatar, lightbox, file preview, link card): `samplePainter()` from `dev.monarchui.testing` in
  examples, `CataloguePictures.landscape()` in the sample app.
- Directional icons (`ChevronLeft`, `ChevronRight`, `ArrowBack`, `Undo`, `Send`) mirror themselves in
  right-to-left layouts; do not add `mirrorInRtl` or a `graphicsLayer(scaleX = -1f)`.
- Only outlined glyphs: never `Icons.Default` or `Icons.Filled` (they are the filled set), never
  `android.R.drawable.*` (Android 1.x clip art, not tintable). AI uses `SampleIcons.Bot`, never a sparkle.
- `IconSourceRulesTest` (`monarch-compose/src/test/.../testing/`) fails on `android.R.drawable`,
  `Icons.Default`, `Icons.Filled` and direct `Icons.*` use in the inventory, the sample app and the showcases.

The mapping from the web (lucide) names is in the visual spec, `icons.md`. The old framework drawables map as
follows (`SampleIcons.ForLegacyDrawable` holds the default for each name):

| `android.R.drawable.`                             | `SampleIcons.`                                                                                         |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `ic_media_previous`, `ic_media_next`              | `ChevronLeft`, `ChevronRight` (paging, calendars, versions); `ArrowBack` for back                      |
| `ic_media_play`                                   | `ChevronRight` when it is an expander (tree, org chart, trace, goal tree, json viewer); `Play` to play |
| `ic_menu_close_clear_cancel`                      | `Close` (`EyeOff` to hide a password, `VideoOff` for a camera that is off)                             |
| `ic_menu_view`                                    | `Eye` to show; `Home`, `Inbox` or `Board` for navigation and views                                     |
| `ic_menu_gallery`, `ic_menu_mapmode` as a picture | `samplePainter()`                                                                                      |
| `ic_menu_search`                                  | `Search`                                                                                               |
| `arrow_down_float`, `arrow_up_float`              | `ChevronDown`, `ChevronUp` (`ArrowDown`, `ArrowUp` for deltas)                                         |
| `ic_dialog_info`, `ic_menu_info_details`          | `Info` (`Bot` for an AI launcher or panel)                                                             |
| `ic_dialog_alert`                                 | `Warning`                                                                                              |
| `ic_popup_reminder`                               | `Bell`                                                                                                 |
| `ic_menu_more`                                    | `More` (`MoreVertical` in a top app bar)                                                               |
| `ic_menu_revert`                                  | `ArrowBack` (`Undo` when it undoes)                                                                    |
| `ic_menu_edit`                                    | `Pencil`                                                                                               |
| `ic_btn_speak_now`, `ic_lock_silent_mode`         | `Mic`, `MicOff` (`BellOff` for muted notifications)                                                    |
| `ic_menu_agenda`                                  | `Agenda`, `List` or `BulletList`                                                                       |
| `ic_menu_add`, `ic_input_add`                     | `Plus`                                                                                                 |
| `ic_menu_share`                                   | `Share` (`ScreenShare` in a call)                                                                      |
| `star_on`, `star_big_on`, `btn_star`              | `Star`                                                                                                 |
| `ic_menu_save`                                    | `Copy` for copy fields and buttons                                                                     |
| `ic_menu_sort_by_size`                            | `Sort` (`Menu` to open navigation)                                                                     |
| `ic_delete`, `ic_menu_delete`                     | `Trash`                                                                                                |
| `ic_menu_call`                                    | `Call` (`CallEnd` to leave a call)                                                                     |
| `sym_call_incoming`                               | `CallIncoming`; a notification small icon is the app's own resource                                    |
| `ic_menu_camera`                                  | `Video`                                                                                                |
| `ic_menu_manage`                                  | `Settings` (`Devices` for a device list)                                                               |
| `ic_menu_send`                                    | `Send`                                                                                                 |
| `ic_menu_help`                                    | `Help` (`RaiseHand` in a call)                                                                         |
| `ic_lock_lock`                                    | `Lock`                                                                                                 |
| `ic_menu_mylocation`                              | `Pin`                                                                                                  |
| `ic_menu_month`                                   | `CalendarMonth`                                                                                        |
| `ic_menu_myplaces`                                | `Building`                                                                                             |
| `stat_sys_download`                               | `Download`                                                                                             |
