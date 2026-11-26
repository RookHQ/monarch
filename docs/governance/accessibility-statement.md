# Accessibility statement

Monarch aims to meet the Web Content Accessibility Guidelines (WCAG) 2.2 at
level AA in every component, theme and product accent. This page says how we
check that, where the checks stop, and how to tell us about a problem.

A design system cannot make an app accessible on its own. The app still has
to give controls their labels, structure its pages and write clear content.
`npx monarch check` reports some of those mistakes in app code
([conformance](../guides/conformance.md)).

The [Accessibility Conformance Report](vpat.md) lists every WCAG 2.2 level A
and AA success criterion, with what the components provide and what the app
must do.

## How it is tested

**Every component, on every change (Vitest, jsdom).** Each component's test
file renders it and runs axe-core (`expectNoA11yViolations`) and a server
render with `renderToString`. Most also test keyboard and focus behaviour
with user-event (450 of the 514 component test files). jsdom has no layout, so axe's colour-contrast check is
turned off there; contrast is covered by the token tests below.

**Colour contrast (token tests).** Every theme (light, dark and the two
high-contrast themes) is tested with every product accent and the neutral
accent: text at least 4.5:1 (7:1 in high contrast), non-text UI and focus
rings at least 3:1. `deriveTheme` applies the same rules to a brand colour of
your own.

**The docs site in three browsers (Playwright).** axe-core runs on every docs
route, including one page per component, in light and dark, in Chromium,
Firefox and WebKit. The pages run in a distant time zone and in German, so
examples that depend on either must still hydrate cleanly. Keyboard flows are
tested too: the skip link and navigation, the search palette, and toasts
(focus stays put and F8 reaches the region).

**Phone and tablet shapes (Playwright, Chromium).** Every docs example is
checked at five shapes: a 320px small phone, a phone upright and on its side,
and a tablet upright and on its side. Nothing may scroll the page sideways
(WCAG 1.4.10), and every control needs a tap area of at least 24 by 24 CSS
pixels (WCAG 2.5.8).

**Built in.** Every component stylesheet has a forced-colours block for
Windows High Contrast and similar modes. Durations become 0ms under
`prefers-reduced-motion`. Focus rings use `:focus-visible`.

## Known limits

- **Automated checks find only part of the problems.** axe-core and our own
  tests cannot judge every success criterion, such as whether a label makes
  sense or a reading order is logical.
- **Screen readers are not tested automatically.** Accessible names, roles
  and live regions are checked in code, not by running a screen reader.
- **Forced-colours mode is not tested automatically.** The styles exist, but
  no test renders pages in forced colours.
- **Visual and device checks run in Chromium only.** The axe and keyboard
  suite runs in all three engines.
- **Charts.** The SVG is hidden from assistive technology. Each chart is
  named by a summary, and `ChartFrame` renders the data as a table for screen
  readers and shows it with "View as table". Keyboard inspection is
  announced; hovering with a pointer is not.
- **Rich-text editor.** `@rookcom/monarch-react/editor` is built on Tiptap
  and ProseMirror, which edit through `contenteditable`. How well rich-text
  editing works varies between screen readers and browsers.
- **AI streaming.** Streaming answers are announced at sentence or paragraph
  boundaries, not word by word ([AI accessibility](../guides/ai-accessibility.md)).
- **Conformance checks are lexical.** `monarch check` reads source code with
  regular expressions and a small JSX reader. It is tuned to miss an edge
  case rather than report a false one, and it does not see the rendered page.
- **Translations are the app's job.** Default text is English; see
  [i18n](i18n.md).

## Report a problem

Open an issue on [GitHub](https://github.com/RookHQ/monarch/issues/new/choose)
with the **Bug report** template. Say which component and package version,
what you did (keyboard, screen reader and browser, with versions), what
happened and what you expected. A short code example helps most.

If the problem is a security issue, report it privately as described in
[SECURITY.md](../../SECURITY.md).
