# Migrating an existing Rook app

Measured starting point for every app: run `monarch check` and `monarch registry verify` on a checkout.
Each step is shippable on its own; nothing requires a rewrite.

## Step 0: baseline (10 minutes)

`npx monarch check --product <id> --write-baseline monarch.baseline.json`,
commit it, add the reusable workflow. From now on new violations fail CI;
old ones are tracked.

## Step 1: tokens under the existing UI (one PR, low risk)

1. Install the packages and import the four CSS files
   (`getting-started.md` §2), including **`css/compat`**.
2. Delete the token blocks (`:root { --background … }` and `.dark { … }`)
   from `globals.css`. Keep your utilities and app CSS, and keep any
   app-specific variables Monarch doesn't define (grep `var(--` and check each
   one still resolves). Apps that use their brand colour for `--accent` need
   `--accent: var(--primary)`: the bridge maps `accent` to Rook Blue, as most
   Rook apps do.
3. Replace `tailwind.config.ts` colour/radius/keyframe sections with the
   preset. Keep `content` and any app-specific plugins.
4. Add `appearanceScript({ product })` to `<head>`, unless the app toggles the
   theme at runtime (see getting-started §3); then just set
   `<html data-mn-product="<id>">`.
5. Visual check in light and dark. Expected differences, all intentional:
   stronger input borders, accessible primary fills (see the product's row in
   `contrast-report.json`), consistent shadows. Note that the legacy
   `--input` now maps to Monarch's `border-input` (about 56% grey instead
   of 88%); the few places using it as a _background_ (for example unchecked
   Switch tracks)
   get a darker track; switch them to `bg-muted` or the Monarch Switch.
   Also expected: `rounded-md`/`rounded-sm` are 8px/6px (were 10px/8px), and
   `destructive` is Rook Red rather than shadcn's generic red.
6. Gold and orange brands keep their fill as
   `--primary` because they need dark text on it; use `text-mn-accent-strong`
   for accent-coloured _text_ in those apps. Every other brand's `--primary`
   is readable as text on every surface.

Conformance drops `tokens/local-token-definitions` to the few deliberate
overrides and `deps/monarch-version` to "current". `registry verify` reports
**partial** after this step (duplicate local components remain) and
**adopted** after Step 2. In the first migration, 63 local token variables
became 5, with 0 axe violations and at most 0.31% of pixels changed.

## Step 2: components (incremental)

Swap `components/ui/*` for Monarch imports, one primitive at a time,
starting with Button, Field/Input, Dialog, DropdownMenu. Map:

| shadcn variant                      | Monarch                                             |
| ----------------------------------- | --------------------------------------------------- |
| `default`                           | `variant="primary"`                                 |
| `outline`                           | `variant="secondary"` (default)                     |
| `ghost`                             | `variant="ghost"`                                   |
| `destructive`                       | `variant="danger"`                                  |
| `size="icon"`                       | `<IconButton label=… icon=… />`                     |
| `<Label>` + `<Input>` + error `<p>` | `<Field><FieldLabel/><Input/><FieldError/></Field>` |

Delete the local file once nothing imports it;
`components/duplicated-primitive` counts what is left.

## Step 3: colours and type (ratchet)

Replace palette classes with `mn-*` classes or semantic legacy names,
guided by the report. Target: `color/no-raw-palette` = 0,
`typography/no-arbitrary-type` = 0. Legitimate literals (OG images, brand
logos, third-party colours) get a registry exception.

## Step 4: remove the bridge

When `legacy/bridge-variable` is 0, drop `css/compat`. Done.

## Rook platform notes

- Keep the shared Rook shell pieces (`RookRail`, the theme toggle, the Space
  switcher and the chat dock) as they are; they already use the shared cookie
  and `rail-expanded:` variant.
- Vite + Tailwind 3 apps follow the same steps.
- Tailwind 4 marketing sites: CSS files only, no preset.
