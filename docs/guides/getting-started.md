# Getting started

In a few steps you can have Monarch running in your app: light and dark
themes, your brand colour, and accessible components that already feel at home
together. Monarch works the same in a weekend project and a large product team.
These steps assume React 18 or 19; the tokens alone work with any framework.

## Start from a working app

To skip the steps below, copy a starter. It has the styles, the appearance
script, Tailwind 4, a page of components, an assistant page and the lint and
conformance setup already in place:

```bash
npx @rookcom/monarch-conformance create my-app --template next-app    # Next.js App Router
npx @rookcom/monarch-conformance create my-app --template vite-react  # Vite + React
cd my-app && npm install && npm run dev
```

Add `--accent '#3355aa'` for your own brand colour; without it the starter
uses the neutral accent. The command only copies files (it never runs an
install) and refuses a directory that is not empty. The sources are in
[`templates/starters`](../../templates/starters).

## 1. Install

```bash
npm i @rookcom/monarch-tokens @rookcom/monarch-react
```

Packages are public on npm, so `npm ci` works in every Docker build and CI job
with no `.npmrc` or token. Pin exact versions (`npm i -E`) and upgrade by PR.

`@rookcom/monarch-react` 0.14.x needs `@rookcom/monarch-tokens`
`>=0.5.3 <1.0.0` (its peer range). See
[compatibility](../architecture/COMPATIBILITY.md) and, for Next.js, Vite and
React Router, [frameworks](frameworks.md).

## 2. Load styles once, in the root layout

```tsx
import '@rookcom/monarch-tokens/css'; // tokens, all themes
import '@rookcom/monarch-tokens/css/base'; // optional: body, focus ring, reduced motion
import '@rookcom/monarch-tokens/css/products/<id>'; // your accent (ids: registry/products)
import '@rookcom/monarch-react/styles.css'; // component styles
```

**Not a Rook product?** Use the neutral accent instead of a product id:
import `@rookcom/monarch-tokens/css/products/neutral` and set
`<html data-mn-product="neutral">` (or pass `product: 'neutral'` to
`appearanceScript` in step 3). With a brand colour of your own, build its
stylesheet with `deriveTheme(hex)` and `brandThemeCss` instead
([theming](theming.md#teams-outside-rook-the-neutral-accent)).

Order does not matter: every file declares the cascade-layer order.

`styles.css` holds every component. If you load it, you are done.

**`styles/core.css` does not style subpath components.** Components you import
from `@rookcom/monarch-react/<entry>` (patterns, data, charts, …) render
unstyled until their sheet is loaded too. If you load `core.css` instead of
`styles.css`, add one line per subpath entry you import:

| If you import from                     | also load                                         |
| -------------------------------------- | ------------------------------------------------- |
| `@rookcom/monarch-react`               | `@rookcom/monarch-react/styles/core.css`          |
| `@rookcom/monarch-react/patterns`      | `@rookcom/monarch-react/styles/patterns.css`      |
| `@rookcom/monarch-react/data`          | `@rookcom/monarch-react/styles/data.css`          |
| `@rookcom/monarch-react/charts`        | `@rookcom/monarch-react/styles/charts.css`        |
| `@rookcom/monarch-react/ai`            | `@rookcom/monarch-react/styles/ai.css`            |
| `@rookcom/monarch-react/shell`         | `@rookcom/monarch-react/styles/shell.css`         |
| `@rookcom/monarch-react/scheduling`    | `@rookcom/monarch-react/styles/scheduling.css`    |
| `@rookcom/monarch-react/collaboration` | `@rookcom/monarch-react/styles/collaboration.css` |
| `@rookcom/monarch-react/editor`        | `@rookcom/monarch-react/styles/editor.css`        |
| `@rookcom/monarch-react/marketing`     | `@rookcom/monarch-react/styles/marketing.css`     |
| `@rookcom/monarch-react/rook`          | `@rookcom/monarch-react/styles/rook.css`          |

```tsx
import '@rookcom/monarch-react/styles/core.css';
import '@rookcom/monarch-react/styles/patterns.css'; // for anything from /patterns
```

Core is about half the size of the full sheet. For the smallest CSS, load
core.css plus one sheet per subpath component you use instead of the entry
sheet: `@rookcom/monarch-react/styles/components/<id>.css` (for example
`styles/components/ai-thread.css`). Each sheet includes the styles of the
subpath components it composes.

`npx monarch check` catches a missing sheet: the `styles/subpath-css` rule
reports a subpath import with no matching stylesheet anywhere in the project
and names the import to add (see [conformance](conformance.md)).

## 3. Apply the appearance before paint

```tsx
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

<html suppressHydrationWarning>
  <head>
    <script dangerouslySetInnerHTML={{ __html: appearanceScript({ product: '<id>' }) }} />
  </head>
```

It reads the shared `rook_theme` cookie (light | dark | system) and the
`mn_contrast` cookie, and sets `data-mn-theme`, `data-mn-contrast`,
`data-mn-product` and the `.dark` class on `<html>`. Existing
theme-toggle code keeps working as long as it writes the same cookie.

Rook apps scope dark mode to signed-in routes with
`<html data-rook-theme-routes="^/(w|home)(/|$)">`; the script honours it, so
public pages stay light. It replaces an app's own theme boot script; keep
your theme toggle (it must write the same cookie).

**If the app also changes the theme at runtime** (for example a store or
effect that toggles `.dark` after hydration, as a custom `ThemeEffect` might),
don't add this script yet: it sets `data-mn-theme`, which that code won't
clear. Keep the app's own boot script and only set
`<html data-mn-product="<id>">`; Monarch's CSS follows `.dark` either way.

## 4. Tailwind

### Tailwind 3.4

```ts
import monarch from '@rookcom/monarch-tokens/tailwind';
export default { presets: [monarch], content: ['./src/**/*.{ts,tsx}'] };
```

You get `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
`bg-mn-accent-solid`, `text-mn-success-strong`, `shadow-mn-md`,
`h-control-md`, `duration-fast`, `rail-expanded:` and the legacy
`bg-primary` / `text-muted-foreground` names.

The preset is typed to be assignable to Tailwind 3's `Config`, so no cast is
needed (fixed in tokens 0.2.2).

**Tailwind 3 preflight.** `@tailwind base` is not in a cascade layer, so it
beats Monarch's layered styles: without a fix, primary buttons lose their fill
and inputs their borders. Put it in Monarch's reset layer, `monarch.reset`:

```css
/* globals.css */
@layer monarch.reset {
  @tailwind base;
}
@tailwind components;
@tailwind utilities;
```

Every Monarch stylesheet opens with
`@layer monarch.reset, monarch.tokens, monarch.base, monarch.components;`, so
`monarch.reset` is always the lowest Monarch layer, whichever file loads
first. Preflight then loses to every Monarch rule and only fills in what
Monarch leaves alone, while your utilities stay unlayered and still win, also
on Monarch elements (`<Button className="mt-4">`). It works in any import
order and needs no `postcss-import`. Your own `@layer base { … }` rules go
into the same place, below Monarch. Requires `@rookcom/monarch-tokens` 0.5.2
and `@rookcom/monarch-react` 0.10.0 or later.

`styles/tailwind-preflight.css` (0.9.2) is superseded: loaded after the
utilities it also cancels utility classes on Monarch elements. Remove it when
you add the layer.

### Tailwind 4

There is no preset. In the CSS file that imports Tailwind:

```css
@layer theme, base, monarch, components, utilities;
@import 'tailwindcss';
@import '@rookcom/monarch-tokens/css';
@import '@rookcom/monarch-tokens/css/tailwind-v4';
/* only for legacy shadcn class names: */
@import '@rookcom/monarch-tokens/css/compat';
@import '@rookcom/monarch-tokens/css/tailwind-v4-compat';
```

The `@layer` line goes first, so Tailwind's preflight sits below Monarch and
your utilities stay above it. Class names are the same as with the preset
([theming](theming.md#tailwind-css)).

### No Tailwind

Skip this step; the CSS files are enough.

## 5. Use components

```tsx
import { Button, Field, FieldLabel, Input, FieldError } from '@rookcom/monarch-react';

<Field invalid={!!error}>
  <FieldLabel>Email</FieldLabel>
  <Input type="email" value={v} onChange={…} />
  <FieldError>{error}</FieldError>
</Field>
<Button variant="primary" type="submit" loading={saving}>Save</Button>
```

Wrap the app in `<TooltipProvider>` once if you use tooltips.

Navigation components take your router's link through `linkAs` or
`linkComponent`: pass Next.js `Link` (or any component typed
`MonarchLinkComponent`) as it is. Monarch always gives it a string `href` and
renders a button or text where there is no page to link to.

## 6. Check conformance

Install the CLI in the app (`npm i -D -E @rookcom/monarch-conformance`), then
use `npx monarch …` from the app root. Without the package installed, `npx
monarch` would fetch an unrelated public package of the same name.

```bash
npx monarch check                           # text report
npx monarch check --product <id>            # in a Rook product
npx monarch check --write-baseline monarch.baseline.json
```

`--product` is optional. Without it the check still runs every rule, using
the compatibility data bundled in the package; an unknown product id is a
warning, not an error. `npx monarch search <query>` and
`npx monarch add <Component>` help you find a component and its imports.

For the same feedback in the editor, add the ESLint plugin that ships in the
package (ESLint 9, flat config):

```js
// eslint.config.js
import monarch from '@rookcom/monarch-conformance/eslint';

export default [monarch.configs.recommended]; // monarch.configs.general outside Rook
```

Then add the reusable workflow. The rules, the fixes and the plugin's options
are in [conformance](conformance.md).

## Entry points

Most components come from `@rookcom/monarch-react`. The rest are grouped in
subpath entries so an app loads only what it uses: `/patterns`, `/data`,
`/charts`, `/scheduling`, `/collaboration`, `/editor`, `/marketing`, `/ai`,
`/shell` and `/rook`. `/rook` holds the exports built for the Rook platform
([rook-specific exports](rook-specific-exports.md)); an app outside Rook does
not need it. The manifest (`@rookcom/monarch-react/manifest.json`) gives the
exact import line, stylesheets and gzip size of every export.
