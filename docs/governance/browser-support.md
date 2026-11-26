# Browser support

Monarch supports current versions of the three browser engines it tests in:
Chromium (Chrome, Edge), Firefox and WebKit (Safari). This page lists the
minimum versions and the modern CSS that sets them.

## Minimum versions

| Browser | Styles load at all | Every feature works |
| ------- | ------------------ | ------------------- |
| Chrome  | 99                 | 120                 |
| Edge    | 99                 | 120                 |
| Firefox | 97                 | 121                 |
| Safari  | 15.4               | 16.4                |

**Styles load at all** is the hard floor. Every Monarch stylesheet puts its
rules in cascade layers (`@layer monarch.reset, monarch.tokens, monarch.base,
monarch.components`). A browser without cascade layers ignores layered rules,
so components render unstyled.

**Every feature works** is the version where nothing below degrades.
Between the two, components still work but some details are dropped, as
listed next.

## Modern CSS in use

Counts are of the 514 component stylesheets in `@rookcom/monarch-react`.

| Feature                          | Used in                                                           | Chrome / Edge | Firefox | Safari | Below the minimum                                      |
| -------------------------------- | ----------------------------------------------------------------- | ------------- | ------- | ------ | ------------------------------------------------------ |
| Cascade layers (`@layer`)        | every stylesheet, and the token CSS                               | 99            | 97      | 15.4   | No Monarch styles                                      |
| `:has()`                         | 36 stylesheets (forms, data tables, dialogs, cards)               | 105           | 121     | 15.4   | The rules that use it are dropped (some state styling) |
| Container queries (`@container`) | 51 stylesheets                                                    | 105           | 110     | 16     | Layouts do not adapt to the width of their container   |
| `color-mix()`                    | token CSS and 14 stylesheets (tints, translucent bars)            | 111           | 113     | 16.2   | Those backgrounds fall back or are dropped             |
| `:dir()`                         | 40 stylesheets (mirrored icons and motion in right-to-left pages) | 120           | 49      | 16.4   | Icons are not mirrored in right-to-left pages          |
| Dynamic viewport units (`dvh`)   | 12 stylesheets (dialogs, sheets, app shell)                       | 108           | 101     | 15.4   | Some have a `vh` fallback; the rest drop that height   |

Progressive enhancements, which do nothing in a browser without them:
`text-wrap: balance` and `pretty` (headings), `field-sizing: content`
(`InlineEditText`), and `backdrop-filter` (with an `@supports` fallback in
`PageHeader`).

Every stylesheet also has a `@media (forced-colors: active)` block for
Windows High Contrast and other forced-colour modes, and motion follows
`prefers-reduced-motion`.

## What is tested

- Accessibility (axe-core on every docs route, light and dark, plus keyboard
  flows) runs in Chromium, Firefox and WebKit on every change, with the
  engines bundled in Playwright 1.56.1.
- Visual baselines and the phone and tablet checks run in Chromium only,
  because pixel output differs between engines by design.
- Older browser versions are not tested. The minimum versions above come from
  the CSS features in use, not from test runs.

See also [compatibility](../architecture/COMPATIBILITY.md) and the
[accessibility statement](accessibility-statement.md).
