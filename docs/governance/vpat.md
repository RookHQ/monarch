# Monarch Accessibility Conformance Report

WCAG edition, in the structure of the VPAT 2.5 template.

> **This is a self-assessment by the Monarch maintainers. It is not a
> third-party audit.** No outside auditor has reviewed Monarch, and the
> repository records no manual testing with a screen reader or other assistive
> technology. Where a row says "Partially Supports", read the remarks: in most
> cases the reason is that the evidence is automated only.

## Product information

| Item                 | Value                                                                                                                                                                           |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Name of product      | Monarch design system                                                                                                                                                           |
| Packages and version | `@rookcom/monarch-react` 0.14.4 and `@rookcom/monarch-tokens` 0.5.4                                                                                                             |
| Report date          | 13 November 2026                                                                                                                                                                |
| Product description  | An open-source (Apache-2.0) React component library and design tokens, made by Rook. It is used to build web applications; it is not an application itself.                     |
| Contact              | Rook Platforms (<https://rookhq.com>). Questions and problems: [GitHub issues](https://github.com/RookHQ/monarch/issues). The project publishes no accessibility email address. |
| Report author        | The Monarch maintainers (self-assessment)                                                                                                                                       |

## Notes

- **A component library cannot conform on behalf of an application.** This
  report says what the components provide. Whether a page built with them
  conforms depends on the application as well.
- **What stays with the app author:** page titles, the page language and the
  language of parts, headings and page structure, the wording of labels, link
  text, error messages and instructions, text alternatives for the app's own
  images, captions, transcripts and audio description for media, consistent
  navigation and help across pages, translations, and any colours the app
  adds outside the tokens.
- `@rookcom/monarch-conformance` (`npx monarch check`) is a separate tool and
  is not assessed here. It reads app source code and reports some mistakes
  (four accessibility rules: image alternatives, click handlers without a key
  handler, removed outlines, positive `tabindex`). It does not see the
  rendered page.
- The docs site (monarchui.dev) is where the components are tested in real
  browsers. It is evidence for this report; it is not the product assessed.
- The rich-text editor (`@rookcom/monarch-react/editor`) is built on Tiptap
  and ProseMirror. Its remarks are listed under the criteria it affects.
- Counts in this report were measured in the repository on the report date.

## Evaluation methods used

All of the methods below are automated and run on every change. No manual
audit with assistive technology is recorded in the repository.

1. **Component tests (Vitest, jsdom, axe-core).** There are 514 component
   test files. 512 of them run axe-core through `expectNoA11yViolations`
   (the other two are `consent-manager` and `ai-conversation`). All 514 also
   render on the server with `renderToString`. 451 use `user-event` for
   interaction, and 388 press keys or Tab explicitly. jsdom has no layout, so
   axe's colour-contrast rule is turned off in these tests, and so is its
   `region` rule.
2. **Browser tests of the docs site (Playwright 1.56.1, axe-core).** axe-core
   runs with its default rules, colour contrast included, on 23 listed pages
   and on one page for each of the 514 components, in the light and dark
   themes, in Chromium, Firefox and WebKit (projects `a11y-chromium`,
   `a11y-firefox`, `a11y-webkit`). The pages run in German and in a distant
   time zone. Keyboard flows are tested in the same three engines: the skip
   link and navigation, the search palette, and toasts (focus stays where it
   was; F8 reaches the toast region).
3. **Device shapes (Playwright).** Every component page is opened at five
   shapes: 320 by 640, 360 by 740, 740 by 360, 768 by 1024 and 1024 by 768
   CSS pixels. Nothing may stick out of its box or scroll the page sideways,
   and every control must have a tap area of at least 24 by 24 CSS pixels
   (hit-tested; links inside a sentence are exempt, as WCAG allows). Six
   findings on five pages are allowed, each with a written reason in
   `apps/docs/tests/devices.spec.ts`. `playwright.config.ts` and the CI
   workflow define this suite for Chromium, Firefox and WebKit.
4. **Colour contrast of the tokens.** `packages/tokens/test/contrast.test.ts`
   tests every theme (light, dark and two high-contrast themes) with every
   product accent and the neutral accent: text at least 4.5:1 (7:1 in the
   high-contrast themes), non-text UI and focus rings at least 3:1. The
   measured ratios are published in the
   [contrast matrix](../architecture/CONTRAST-MATRIX.md), where every pair
   meets its target.
5. **CSS build rules.** `packages/react/scripts/build-css.ts` fails the build
   when a component stylesheet has no forced-colours handling (a comment
   saying why none is needed also counts), or animates without a
   `prefers-reduced-motion` block. Of the 514 component stylesheets, 513 have
   a `@media (forced-colors: active)` block, and 211 animate and have a
   reduced-motion block.
6. **Visual baselines (Playwright, Chromium only).** Screenshots of the
   gallery and of open overlays, compared on every change.

What was **not** done: no testing with a screen reader, screen magnifier,
voice control or switch device is recorded; forced-colours mode is not
rendered in any test; reduced motion is not rendered in any test; browser
zoom and text-spacing overrides are not tested; older browser versions are
not tested ([browser support](browser-support.md)).

## Applicable standards and guidelines

| Standard                                                                          | Included in report                                  |
| --------------------------------------------------------------------------------- | --------------------------------------------------- |
| [Web Content Accessibility Guidelines 2.2](https://www.w3.org/TR/WCAG22/)         | Level A: yes. Level AA: yes. Level AAA: no.         |
| [Web Content Accessibility Guidelines 2.1](https://www.w3.org/TR/WCAG21/) and 2.0 | Covered by the same rows (see the paragraph below). |

Every WCAG 2.0 and 2.1 success criterion at Level A and AA is also in WCAG
2.2, so the tables below cover all three versions. The one exception is
4.1.1 Parsing, which WCAG 2.2 removed as obsolete; it is listed in Table 1
for readers who report against 2.0 or 2.1. The six criteria that are new in
2.2 are marked "(2.2)". Level AAA was not evaluated and has no table.

Table 1 has 32 rows: the 31 Level A criteria of WCAG 2.2, plus 4.1.1.
Table 2 has the 24 Level AA criteria.

## Terms

- **Supports**: the components meet the criterion, with no known defect.
- **Partially Supports**: some of the components do not meet the criterion,
  or the evidence does not cover all of it. In this report it most often
  means "checked by automated tests only; not manually audited with assistive
  technology".
- **Does Not Support**: most of the components do not meet the criterion.
- **Not Applicable**: the criterion concerns content or behaviour the library
  does not ship. The remarks say who is responsible.
- **Not Evaluated**: the criterion was not assessed. Used only for Level AAA,
  which this report leaves out.

## Table 1: Success criteria, Level A

| Criterion                                                  | Conformance level  | Remarks and explanations                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ---------------------------------------------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.1.1 Non-text Content                                     | Partially Supports | Decorative icons are hidden from assistive technology, and icon-only controls need a label by type (`IconButton.label` is required). Charts hide their SVG, are named by a summary, and `ChartFrame` gives the data as a table. axe checks names and alternatives in 512 component test files and on every docs page. The app supplies the alternative text for its own images. Not manually audited with assistive technology.                                                                      |
| 1.2.1 Audio-only and Video-only (Prerecorded)              | Not Applicable     | The library ships no audio or video content. `FilePreview` and `Lightbox` play media the app supplies; alternatives for that media are the app's job.                                                                                                                                                                                                                                                                                                                                                |
| 1.2.2 Captions (Prerecorded)                               | Not Applicable     | No media content is shipped. `Lightbox` (`tracks` on an item) and `FilePreview` (`captionsSrc`) accept caption tracks; the app supplies them.                                                                                                                                                                                                                                                                                                                                                        |
| 1.2.3 Audio Description or Media Alternative (Prerecorded) | Not Applicable     | No media content is shipped. The app supplies audio description or a transcript.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 1.3.1 Info and Relationships                               | Partially Supports | Components use native elements or Radix primitives; `Field` ties labels, descriptions and errors to their control; tables, lists and headings use the matching elements. axe checks this in the component tests and in three browser engines. Automated checks cannot judge every relationship. Not manually audited with assistive technology.                                                                                                                                                      |
| 1.3.2 Meaningful Sequence                                  | Partially Supports | Components render in reading order in the DOM, and layout uses CSS logical properties. Automated checks cannot judge whether a reading order is logical (a known limit), and the order of a page is the app's. Not manually audited with assistive technology.                                                                                                                                                                                                                                       |
| 1.3.3 Sensory Characteristics                              | Partially Supports | Instructions are mostly app content. The default English texts of the components (163 `*_MESSAGES` constants) have not been reviewed for wording that depends on shape, colour or position.                                                                                                                                                                                                                                                                                                          |
| 1.4.1 Use of Color                                         | Partially Supports | By design, state is not shown by colour alone: statuses, tool states, diffs and confidence carry icons or text, and disabled text is never the only cue. This is a written rule, not an automated check for each component.                                                                                                                                                                                                                                                                          |
| 1.4.2 Audio Control                                        | Partially Supports | The two components that start video by themselves (`CallTile`, `AiComputerView`) render it muted. `FilePreview` plays audio with native controls and does not start it. Call and voice components play streams the app supplies; pausing or muting those is wired by the app. Not audited for each voice component.                                                                                                                                                                                  |
| 2.1.1 Keyboard                                             | Partially Supports | The component contract requires native semantics or a Radix primitive: Enter and Space activate, Escape closes, arrow keys move inside composites. 451 component test files test interaction and 388 press keys explicitly; keyboard flows run in three browser engines. Known limits: in charts, keyboard inspection is announced but pointer hover is not; rich-text editing through `contenteditable` varies between browsers and screen readers. Not manually audited with assistive technology. |
| 2.1.2 No Keyboard Trap                                     | Partially Supports | Modal components trap focus on purpose, close with Escape and restore focus (71 component test files test focus return). The rich-text editor and embedded frames have not been audited for traps.                                                                                                                                                                                                                                                                                                   |
| 2.1.4 Character Key Shortcuts                              | Partially Supports | A source search found single-key shortcuts in four places: `AiEvalReview` (J, K, P, F), `AiDraftQueue` (J, K), the call shortcuts (M, V, S, H, F) and `OrgChart` (M). All act only while focus is inside the component and not in a text field, which is one of the ways the criterion allows. The first three can be turned off with a prop; none can be remapped. The search may not be complete.                                                                                                  |
| 2.2.1 Timing Adjustable                                    | Partially Supports | Toasts pause while hovered or focused, can be reached with F8, and their time can be lengthened or turned off (`duration`, `durationScale`). Other time limits (sessions, sign-in codes) are set by the app.                                                                                                                                                                                                                                                                                         |
| 2.2.2 Pause, Stop, Hide                                    | Partially Supports | Every animated stylesheet has a `prefers-reduced-motion` block (211; the build fails otherwise). Streaming AI answers always have a reachable Stop. The thread stops following when the person scrolls up. No test renders pages with reduced motion, and there is no per-component audit of moving content that lasts more than five seconds.                                                                                                                                                       |
| 2.3.1 Three Flashes or Below Threshold                     | Partially Supports | No component is designed to flash. This has not been measured with a flash analysis tool.                                                                                                                                                                                                                                                                                                                                                                                                            |
| 2.4.1 Bypass Blocks                                        | Partially Supports | The library provides `SkipLink` and landmark-based shell components. The docs site's skip link is tested in three browser engines. Using them on each page is the app's job.                                                                                                                                                                                                                                                                                                                         |
| 2.4.2 Page Titled                                          | Not Applicable     | Components do not set the document title. The app does.                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| 2.4.3 Focus Order                                          | Partially Supports | Focus follows DOM order; overlays move focus in and restore it on close; toasts never take focus (tested in three engines); focus does not move while an AI answer streams. Page-level focus order is the app's. Not manually audited.                                                                                                                                                                                                                                                               |
| 2.4.4 Link Purpose (In Context)                            | Partially Supports | `Link` renders a native anchor and icon-only links need a label. The link text is written by the app. axe's link-name rule runs in the tests.                                                                                                                                                                                                                                                                                                                                                        |
| 2.5.1 Pointer Gestures                                     | Partially Supports | No component is known to need a multi-point or path-based gesture as its only input. Components with drag, zoom or pan (maps, lightbox, boards) have not been audited one by one.                                                                                                                                                                                                                                                                                                                    |
| 2.5.2 Pointer Cancellation                                 | Partially Supports | Buttons, links and menu items act on the native click (up) event. Components that start an action on pointer down (drag and resize handles) have not been audited one by one for abort or undo.                                                                                                                                                                                                                                                                                                      |
| 2.5.3 Label in Name                                        | Partially Supports | Controls take their accessible name from their visible text through native semantics. Where the app passes a separate `aria-label`, keeping it in line with the visible text is the app's job. No automated check covers this criterion.                                                                                                                                                                                                                                                             |
| 2.5.4 Motion Actuation                                     | Not Applicable     | No component responds to device motion or user motion (a source search finds no motion or orientation listeners).                                                                                                                                                                                                                                                                                                                                                                                    |
| 3.1.1 Language of Page                                     | Not Applicable     | The app sets `lang` on `<html>`. Default component text is English; translations are passed in by the app ([i18n](i18n.md)).                                                                                                                                                                                                                                                                                                                                                                         |
| 3.2.1 On Focus                                             | Partially Supports | Components do not change context on focus by design; tooltips and hover cards open without moving focus. Not audited for each component.                                                                                                                                                                                                                                                                                                                                                             |
| 3.2.2 On Input                                             | Partially Supports | Form controls report changes through callbacks and do not submit or navigate by themselves. What the app does in those callbacks is the app's. Not audited for each component.                                                                                                                                                                                                                                                                                                                       |
| 3.2.6 Consistent Help (2.2)                                | Not Applicable     | Where help appears across pages is decided by the app.                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 3.3.1 Error Identification                                 | Partially Supports | `Field` marks the control `aria-invalid` and links the error text to it. Detecting the error and writing the message is the app's job. Checked in code and by axe; not tested with a screen reader.                                                                                                                                                                                                                                                                                                  |
| 3.3.2 Labels or Instructions                               | Partially Supports | `Field`, `FieldLabel`, `FieldDescription` and `FieldError` provide and link labels and instructions; axe's label rules run in the tests. Whether a label makes sense cannot be checked automatically (a known limit) and is the app's.                                                                                                                                                                                                                                                               |
| 3.3.7 Redundant Entry (2.2)                                | Not Applicable     | The library keeps no data between the steps of a process. Carrying earlier answers forward is the app's job.                                                                                                                                                                                                                                                                                                                                                                                         |
| 4.1.1 Parsing (obsolete, removed in WCAG 2.2)              | Not Applicable     | WCAG 2.2 removed this criterion. For WCAG 2.0 and 2.1, the W3C says it should be treated as always met for HTML. Components render through React, which produces well-formed markup; all 514 component test files render on the server.                                                                                                                                                                                                                                                              |
| 4.1.2 Name, Role, Value                                    | Partially Supports | Names, roles and states come from native elements, Radix primitives and ARIA attributes. axe checks them in 512 component test files and on every docs page in three browser engines. They are checked in code, not by running a screen reader. The rich-text editor's exposure to assistive technology varies between screen readers and browsers.                                                                                                                                                  |

## Table 2: Success criteria, Level AA

| Criterion                                       | Conformance level  | Remarks and explanations                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ----------------------------------------------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1.2.4 Captions (Live)                           | Not Applicable     | The library ships no live media. Call components show streams the app supplies; live captions are the app's job.                                                                                                                                                                                                                                                                                                                                                   |
| 1.2.5 Audio Description (Prerecorded)           | Not Applicable     | No media content is shipped.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 1.3.4 Orientation                               | Supports           | No component locks the orientation. Every component page is tested upright and on its side, on a phone shape and a tablet shape.                                                                                                                                                                                                                                                                                                                                   |
| 1.3.5 Identify Input Purpose                    | Partially Supports | Inputs are native and pass `autoComplete` through. Setting the right value for each field is the app's job; no test checks it.                                                                                                                                                                                                                                                                                                                                     |
| 1.4.3 Contrast (Minimum)                        | Supports           | For the colours Monarch ships. Token tests cover every theme with every product accent and the neutral accent (text at least 4.5:1; 7:1 in the high-contrast themes), and `deriveTheme` applies the same rules to a brand colour. axe's contrast rule also runs on every docs page in light and dark, in three browser engines (one docs-only selector, the colour swatches, is excluded). Disabled text is exempt under WCAG. Colours the app adds are the app's. |
| 1.4.4 Resize Text                               | Partially Supports | Font sizes are set in `rem`, so they follow the browser's text size. Browser zoom to twice the size is not tested as such; the 320 pixel reflow test covers narrow layouts only.                                                                                                                                                                                                                                                                                   |
| 1.4.5 Images of Text                            | Supports           | The library ships no images of text. Text in components and charts is real text.                                                                                                                                                                                                                                                                                                                                                                                   |
| 1.4.10 Reflow                                   | Supports           | Every component page is tested at a 320 pixel width and four other shapes: nothing may stick out of its box or scroll the page sideways. Six findings on five pages are allowed, each caused by the example and recorded with a reason. Wide tables and code scroll inside their own region, which the criterion permits.                                                                                                                                          |
| 1.4.11 Non-text Contrast                        | Partially Supports | Token tests hold input borders, focus rings and chart colours to at least 3:1 in every theme and accent. This is tested on the tokens, not measured on each rendered component and state. Forced-colours styles exist (513 stylesheets) but no test renders pages in forced colours.                                                                                                                                                                               |
| 1.4.12 Text Spacing                             | Partially Supports | Components size by content and line height is relative, but no test applies the text-spacing overrides of this criterion.                                                                                                                                                                                                                                                                                                                                          |
| 1.4.13 Content on Hover or Focus                | Partially Supports | Tooltips, hover cards and popovers are built on Radix primitives and close with Escape. Dismissable, hoverable and persistent behaviour has not been audited for each component.                                                                                                                                                                                                                                                                                   |
| 2.4.5 Multiple Ways                             | Not Applicable     | Ways to find pages are provided by the app. The library offers navigation and search components to build them.                                                                                                                                                                                                                                                                                                                                                     |
| 2.4.6 Headings and Labels                       | Partially Supports | Components with a heading take a heading-level prop so the page outline stays intact, and AI answers start their headings below the page's. The wording of headings and labels is the app's.                                                                                                                                                                                                                                                                       |
| 2.4.7 Focus Visible                             | Partially Supports | The contract requires a `:focus-visible` ring from `--mn-color-focus-ring` and forbids removing an outline without a replacement; 274 stylesheets set `:focus-visible` styles and the ring colour is contrast-tested. The build does not check this rule, and it has not been audited for each component.                                                                                                                                                          |
| 2.4.11 Focus Not Obscured (Minimum) (2.2)       | Partially Supports | Toasts never take focus, and overlays hold focus inside themselves. 32 stylesheets use sticky positioning; whether a sticky bar can fully cover a focused control is not tested. In an app, this also depends on the app's own fixed headers and footers.                                                                                                                                                                                                          |
| 2.5.7 Dragging Movements (2.2)                  | Partially Supports | Components with dragging (among them `ReorderableList`, `SortableGrid`, `KanbanBoard`, `ResizablePanels`, `DashboardGrid`, `GanttTimeline`, `TimeGrid`, `BacklogPlanner`, `Slider`) handle keyboard input for the same action. Whether each also offers a single-pointer way without dragging has not been audited one by one.                                                                                                                                     |
| 2.5.8 Target Size (Minimum) (2.2)               | Supports           | Every control on every component page must have a tap area of at least 24 by 24 CSS pixels, hit-tested at five device shapes. Smaller-looking controls get the area from the touch target floor in `foundation.css`. Links inside a sentence are exempt, as the criterion says. One allowed finding concerns a plain text field in an example page, not a component.                                                                                               |
| 3.1.2 Language of Parts                         | Not Applicable     | Components pass the native `lang` attribute through. Marking passages in another language is the app's job.                                                                                                                                                                                                                                                                                                                                                        |
| 3.2.3 Consistent Navigation                     | Not Applicable     | The order of navigation across pages is decided by the app.                                                                                                                                                                                                                                                                                                                                                                                                        |
| 3.2.4 Consistent Identification                 | Partially Supports | The same function uses the same component and the same default label across the library. This has not been reviewed across all components, and the app can change any label.                                                                                                                                                                                                                                                                                       |
| 3.3.3 Error Suggestion                          | Partially Supports | `Field` shows the error text the app passes. Suggesting a correction is the app's job.                                                                                                                                                                                                                                                                                                                                                                             |
| 3.3.4 Error Prevention (Legal, Financial, Data) | Partially Supports | The library provides `ConfirmDialog`, `TypeToConfirmInput` and guidance for destructive actions ([guide](../guides/destructive-actions.md)). Making a submission reversible, checked or confirmed is the app's job.                                                                                                                                                                                                                                                |
| 3.3.8 Accessible Authentication (Minimum) (2.2) | Partially Supports | The library ships no CAPTCHA or other cognitive test. `PasswordInput` and `OTPPinInput` are native inputs that accept paste and password managers. The sign-in flow itself is the app's. Not audited end to end.                                                                                                                                                                                                                                                   |
| 4.1.3 Status Messages                           | Partially Supports | Status and alert roles and live regions are used across the library (212 component source files). Toasts are announced once; streaming AI answers are announced at sentence or paragraph boundaries through one polite live region, then "Response complete". Chart keyboard inspection is announced; pointer hover is not. Checked in code, not with a screen reader.                                                                                             |

## Known limitations

- This is a self-assessment. No third party has audited Monarch.
- No manual testing with a screen reader or other assistive technology is
  recorded. Names, roles and live regions are checked in code.
- Automated checks find only part of the problems. They cannot judge whether
  a label makes sense or a reading order is logical.
- Forced-colours mode is not tested automatically. The styles exist, but no
  test renders pages in forced colours. The same is true of reduced motion.
- Visual baselines run in Chromium only. The axe and keyboard suite runs in
  Chromium, Firefox and WebKit.
- In jsdom, the component tests cannot check colour contrast; contrast is
  covered by the token tests and by axe on the docs site in the light and
  dark themes. The two high-contrast themes are covered by the token tests
  only.
- Charts: the SVG is hidden from assistive technology. Each chart is named
  by a summary and `ChartFrame` gives the data as a table. Keyboard
  inspection is announced; hovering with a pointer is not.
- Rich-text editor: built on Tiptap and ProseMirror, which edit through
  `contenteditable`. How well it works varies between screen readers and
  browsers.
- AI streaming: answers are announced at sentence or paragraph boundaries,
  not word by word ([AI accessibility](../guides/ai-accessibility.md)).
- Right-to-left: some components keep left-to-right arrow-key rules or
  physical positions ([i18n](i18n.md)).
- Conformance checks (`monarch check`) are lexical. They read source code and
  do not see the rendered page.
- Translations are the app's job. Default text is English.
- Older browser versions are not tested.

## How to report a problem

Open an issue at <https://github.com/RookHQ/monarch/issues>. Say which
component and package version, what you did (keyboard, screen reader and
browser, with versions), what happened and what you expected. The
[accessibility statement](accessibility-statement.md#report-a-problem) has
more detail. Report security problems privately, as described in
[SECURITY.md](../../SECURITY.md).
