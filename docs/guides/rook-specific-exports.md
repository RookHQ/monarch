# Rook-specific exports

Monarch is made by Rook and open to everyone. A few exports encode contracts
of the Rook platform: Rook's shared cookies, Rook ID sign-in, the Rook
Intelligence API and Rook's product colours. They work outside Rook, but they
are built for it. This page lists them and what a team outside Rook should
use instead.

From `@rookcom/monarch-react` 0.14.5 every one of them is exported from one
entry, `@rookcom/monarch-react/rook`. Nothing is renamed, and the old import
paths (`/shell`, `/patterns`, `/ai`) keep working until they are
removed no earlier than 2027-02-11 (90-day window); see
[Moving to `/rook`](#moving-to-rook) below. The
[versioning policy](../governance/versioning.md) applies to these exports as
to any other.

## How to tell

- The component manifest (`@rookcom/monarch-react/manifest.json`) gives every
  export a `scope`: `"general"` or `"rook"`.
- `npx monarch search <query>` lists general components first.
- They are imported from `@rookcom/monarch-react/rook`. An app outside Rook
  that never imports that entry uses none of them.
- The ESLint config for teams outside Rook
  (`monarch.configs.general` from `@rookcom/monarch-conformance/eslint`)
  turns on `monarch/no-rook-specific`, which reports an import of any of them
  and names the general component to use.

## `@rookcom/monarch-react`

| Export                                                                                                                                     | Import from | Old path (deprecated) | Rook contract                                                                                 | Outside Rook, use                                                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------ | ----------- | --------------------- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `RookRail`, `rookRailScript`, `ROOK_RAIL_COOKIE`, `ROOK_RAIL_EVENT`                                                                        | `/rook`     | `/shell`              | The `rook_rail` cookie and `<html data-rook-rail="expanded">`, shared by every Rook app       | `SidebarNav` (root entry) in `AppShell`'s `rail` slot                                                                   |
| `RookBar`, `RookBarPanelProvider`, `useRookBarPanel`, `RookBarAppCatalog`, `RookBarLinkList`, `RookBarFeaturePanel`, `RookBarStoriesPanel` | `/rook`     | `/shell`              | The Rook company bar on public pages: wordmark, app catalogue, stories, Rook account controls | `SiteHeader` (`/marketing`) or `TopNav` (root entry), with `AccountMenu` (`/patterns`)                                  |
| `ROOK_THEME_COOKIE`, `ROOK_THEME_EVENT`                                                                                                    | `/rook`     | `/shell`              | The `rook_theme` cookie and the `rook-theme` event, shared by every Rook app                  | `ThemeToggle` with your own `cookieName` (and `cookieDomain`), and the same name in `appearanceScript({ themeCookie })` |
| `SignInWithRookButton`                                                                                                                     | `/rook`     | `/patterns`           | Sign-in with Rook ID, with the Rook mark                                                      | `SignInForm` (`/patterns`) with your own providers in its `providers` slot, or a `Button`                               |
| `rookIntelligenceTransport`, `mapRookIntelligenceEvent`                                                                                    | `/rook`     | `/ai`                 | The Rook Intelligence API (`POST /v1/agents/{id}/chat`, server-sent events)                   | `createSseTransport` with your own endpoint and event mapper, or your own `AiTransport`                                 |

Each export above comes with its types (`RookRailProps`, `RookBarProps`,
`SignInWithRookButtonProps`, `RookIntelligenceTransportOptions` and so on).

The stylesheet for the entry is `@rookcom/monarch-react/styles/rook.css`. An
app that already loads `styles/shell.css` and `styles/patterns.css` (or the
full `styles.css`) needs nothing more: those sheets still style the same
components.

## Moving to `/rook`

Change the import path; nothing else changes.

```diff
- import { AppShell, RookRail } from '@rookcom/monarch-react/shell';
+ import { AppShell } from '@rookcom/monarch-react/shell';
+ import { RookRail } from '@rookcom/monarch-react/rook';
```

Both tools do it for you:

- `npx monarch check --fix` rewrites every import it reports under
  `api/deprecated`. A statement that mixes moved and other names over several
  lines is reported without a fix; split it by hand.
- ESLint: `monarch/no-deprecated-api` with `eslint --fix`.

The old paths are deprecated from 0.14.5 (2026-11-13) and stop exporting
these names in a later 0.14.x patch, no earlier than 2027-02-11 (90-day
window) and not before 0.14.7 ([deprecation policy](../governance/DEPRECATION-POLICY.md),
[migration](migration.md#rook-specific-exports-move-to-rook)). The
manifest lists each export with `entry: "./rook"` and the old entry in
`alsoEntries`.

Some general components carry Rook defaults that you can change:

- `ThemeToggle` writes the `rook_theme` cookie by default, on `.rookhq.com`
  when the page is on a Rook host. Set `cookieName` and `cookieDomain`.
- `AppShell`, `IdentityBar`, `SpaceSwitcher` and `WorkspaceSwitcher` live in
  `/shell`, and their docs use Rook examples. They take
  any content: pass your own navigation, product name and links.

## `@rookcom/monarch-tokens`

| What                                                           | Rook contract                                                             | Outside Rook, use                                                                                  |
| -------------------------------------------------------------- | ------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Product accents: `css/products/<id>`, `products`, `productIds` | One accent per Rook product, from `registry/products`                     | `css/products/neutral` with `data-mn-product="neutral"`, or `deriveTheme(hex)` and `brandThemeCss` |
| The default accent in `css` (Rook Red), `brand`                | The Rook brand palette                                                    | The neutral accent or your own brand colour, as above                                              |
| `appearanceScript` default `themeCookie: 'rook_theme'`         | The theme cookie shared by every Rook app                                 | `appearanceScript({ themeCookie: '<your cookie>' })`                                               |
| `<html data-rook-theme-routes="regex">`                        | Rook apps keep public pages light and allow dark only on signed-in routes | Leave it out; the script then applies the chosen theme on every page                               |
| The `rail-expanded:` Tailwind variant                          | Matches `<html data-rook-rail="expanded">`, set by `RookRail`             | Not needed without `RookRail`                                                                      |

The status colours (danger, success, warning, info) and the AI purple come
from the Rook palette too. They are general: every theme meets the same
contrast rules with any accent, and `deriveTheme(hex, { ai: '#hex' })` sets
your own AI colour.

## See also

- [Theming](theming.md#teams-outside-rook-the-neutral-accent): the neutral
  accent and your own brand colour.
- [Getting started](getting-started.md).
