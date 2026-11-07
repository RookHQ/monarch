# Getting started

In a few steps you can have Monarch running in your app: light and dark
themes, your brand colour, and accessible components that already feel at home
together. Monarch works the same in a weekend project and a large product team.
These steps assume React 18 or 19; the tokens alone work with any framework.

## 1. Install

```bash
npm i @rookcom/monarch-tokens @rookcom/monarch-react
```

Packages are public on npm, so `npm ci` works in every Docker build and CI job
with no `.npmrc` or token. Pin exact versions (`npm i -E`) and upgrade by PR.

## 2. Load styles once, in the root layout

```tsx
import '@rookcom/monarch-tokens/css'; // tokens, all themes
import '@rookcom/monarch-tokens/css/base'; // optional: body, focus ring, reduced motion
import '@rookcom/monarch-tokens/css/products/<id>'; // your accent (ids: registry/products)
import '@rookcom/monarch-react/styles.css'; // component styles
```

Order does not matter: every file declares the cascade-layer order.

`styles.css` holds every component. To ship less CSS, load
`@rookcom/monarch-react/styles/core.css` (the root entry and shared styles)
plus one sheet per subpath entry you import: `styles/ai.css`,
`styles/patterns.css`, `styles/charts.css`, `styles/data.css`,
`styles/scheduling.css`, `styles/collaboration.css`, `styles/editor.css`,
`styles/marketing.css` or `styles/shell.css`. Core is about half the size of
the full sheet.

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

## 4. Tailwind (3.4)

```ts
import monarch from '@rookcom/monarch-tokens/tailwind';
export default { presets: [monarch], content: ['./src/**/*.{ts,tsx}'] };
```

You get `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
`bg-mn-accent-solid`, `text-mn-success-strong`, `shadow-mn-md`,
`h-control-md`, `duration-fast`, `rail-expanded:` and the legacy
`bg-primary` / `text-muted-foreground` names.

The preset is typed to be assignable to Tailwind 3's `Config`, so no cast is
needed (fixed in tokens 0.2.2). Tailwind 4 or no Tailwind: skip this step; the
CSS files are enough.

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

## 6. Check conformance

Install the CLI in the app (`npm i -D -E @rookcom/monarch-conformance`), then
use `npx monarch …` from the app root. Without the package installed, `npx
monarch` would fetch an unrelated public package of the same name.

```bash
npx monarch check --product <id>            # text report
npx monarch check --product <id> --write-baseline monarch.baseline.json
```

Then add the reusable workflow (see `docs/guides/conformance.md`).
