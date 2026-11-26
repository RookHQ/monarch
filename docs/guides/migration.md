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
- Tailwind 4: import `@rookcom/monarch-tokens/css/tailwind-v4`; class names are
  the same as with the preset.

## Rook-specific exports move to `/rook`

`@rookcom/monarch-react` 0.14.5 adds the entry `@rookcom/monarch-react/rook`
with every export that encodes a Rook platform contract: `RookRail`,
`rookRailScript`, `RookBar` and its panels, `useRookBarPanel`, the
`ROOK_RAIL_*` and `ROOK_THEME_*` constants, `SignInWithRookButton`,
`rookIntelligenceTransport`, `mapRookIntelligenceEvent`, and their types.

Nothing breaks in 0.14.x: `/shell`, `/patterns` and `/ai` still export them,
marked `@deprecated`. They stop in 0.15.0. To move now:

1. Run `npx monarch check --fix` (or `eslint --fix` with the Monarch plugin).
   It changes the import path and nothing else:

   ```diff
   - import { RookRail, type RookRailProps } from '@rookcom/monarch-react/shell';
   + import { RookRail, type RookRailProps } from '@rookcom/monarch-react/rook';
   ```

2. Stylesheets need no change. `styles/shell.css` and `styles/patterns.css`
   still style these components; `styles/rook.css` holds the same styles for
   an app that loads per-entry sheets and imports only `/rook`.

The full list is in [rook-specific exports](rook-specific-exports.md).

## Upgrading to 0.14: Tiptap 3 for the editor entry

`@rookcom/monarch-react` 0.14.0 moves `@rookcom/monarch-react/editor` from
Tiptap 2 to Tiptap 3, because every Tiptap 2 release has a prototype
pollution issue in `@tiptap/core` (`mergeAttributes`) that is fixed only in
3.30.4 and later ([ADR 0015](../adr/0015-tiptap-3.md)). Monarch's props,
classes and output do not change. Apps that never install `@tiptap/*` have
nothing to do.

If the app installs any `@tiptap/*` package (even without importing
`./editor`, because npm checks optional peers that are present):

1. Upgrade every `@tiptap/*` package to the same 3.x version, 3.30.4 or
   later. Install the editor's peers:

   ```sh
   npm install @tiptap/core@^3.30.4 @tiptap/pm@^3.30.4 @tiptap/react@^3.30.4 \
     @tiptap/starter-kit@^3.30.4 @tiptap/extensions@^3.30.4 \
     @tiptap/extension-list@^3.30.4 @tiptap/extension-link@^3.30.4 \
     @tiptap/extension-image@^3.30.4 @tiptap/extension-mention@^3.30.4 \
     @tiptap/suggestion@^3.30.4
   ```

2. Remove `@tiptap/extension-placeholder`, `@tiptap/extension-character-count`,
   `@tiptap/extension-task-list` and `@tiptap/extension-task-item` unless the
   app imports them itself; Monarch now uses `@tiptap/extensions` and
   `@tiptap/extension-list`.
3. Move the app's own Tiptap code to the Tiptap 3 API (Tiptap's upgrade guide
   from v2 to v3 lists every change). The ones Rook code is most likely to
   hit, before and after:

   ```ts
   // Tiptap 2
   import Placeholder from '@tiptap/extension-placeholder';
   extensions: [StarterKit, Link.configure({ openOnClick: false })];
   editor.commands.setContent(html, false);

   // Tiptap 3
   import { Placeholder } from '@tiptap/extensions';
   // StarterKit now includes Link (and Underline, TrailingNode, ListKeymap):
   // configure it there, or turn it off with `link: false` before adding your own.
   extensions: [StarterKit.configure({ link: { openOnClick: false } })];
   editor.commands.setContent(html, { emitUpdate: false });
   ```

   `history` is now `undoRedo` in StarterKit's options, `BubbleMenu` and
   `FloatingMenu` moved to `@tiptap/react/menus` and use Floating UI instead
   of tippy.js, and `useEditor` no longer re-renders on every transaction
   unless `shouldRerenderOnTransaction: true` is set.

4. Custom extensions passed to `RichTextEditor`'s `extensions` prop, the
   block extensions and the editor given to `EditorToolbar`, `BubbleMenu` and
   `PageOutline` must all be Tiptap 3.
5. Run the app's editor tests and check stored documents still load.
