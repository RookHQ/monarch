# UI rules: Monarch design system

This app builds its UI with Monarch (`@rookcom/monarch-react` and
`@rookcom/monarch-tokens`). Follow these rules when you add or change UI.

## Find the right component

- Manifest: `node_modules/@rookcom/monarch-react/dist/manifest.json`
  (`@rookcom/monarch-react/manifest.json`). Each export lists `import` (the
  exact import line), `css` (the stylesheets it needs), `props`,
  `className`, `docs` and `scope`.
- Docs: `https://monarchui.dev/components/<id>/`, Markdown at
  `https://monarchui.dev/components/<id>/index.md`, and
  `https://monarchui.dev/llms.txt` or `https://monarchui.dev/llms/<entry>.txt`.
- `npx monarch search <query>` finds a component.
  `npx monarch add <Component>` prints its import, CSS and a minimal example.
- MCP clients: run `npx monarch mcp` (tools `search_components`,
  `get_component`, `list_tokens`, `get_theme`, `check_code`).

## Rules

1. Use a Monarch component before writing a new UI primitive. Do not copy
   Monarch source into the app.
2. Avoid exports marked `scope: "rook"` in the manifest unless this is a
   Rook product. They encode Rook platform contracts (Rook bar and rail,
   `SignInWithRookButton`, `rookIntelligenceTransport`, `ROOK_*` constants)
   and are imported from `@rookcom/monarch-react/rook`; in a Rook product,
   import them from there, not from `/shell`, `/patterns` or `/ai`.
3. Colour only through tokens: `--mn-*` variables or classes such as
   `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
   `bg-mn-accent-solid`, `text-mn-danger-strong`. Never `bg-red-500`,
   `#hex`, `rgb()` or arbitrary `[...]` colours.
4. Type and spacing: the named scale (`text-sm`, `p-4`, `gap-2`), never
   `text-[13px]` or `p-[7px]`.
5. `danger`, `success`, `warning` and `info` mean status. `accent` is the
   brand. Do not use the accent to mean a status.
6. Accessibility: icon-only controls have a label (`IconButton label=`),
   inputs sit in a `Field` with a `FieldLabel`, every `Dialog` has a
   `DialogTitle`, destructive actions use `variant="danger"` and a
   confirmation.
7. Never call `window.confirm`, `alert` or `prompt`. Use `useConfirm()`,
   `usePrompt()`, `useChoice()` or `useUndoToast()`.
8. If the app loads `@rookcom/monarch-react/styles/core.css` instead of
   `styles.css`, load every stylesheet in the component's manifest `css`
   field once in the root layout. `npx monarch add <Component> --write`
   adds a missing one.
9. Style components through props, tokens and the root and part `mn-*`
   classes listed in the manifest (`className`). Do not target modifier
   classes or `--_*` variables, and do not redefine `--mn-*` variables.
10. Monarch does not use sparkle icons for AI; use the lucide `Bot` icon.

## Accent

- Not a Rook product: load `@rookcom/monarch-tokens/css/products/neutral`
  and set `<html data-mn-product="neutral">`, or derive your own brand with
  `deriveTheme(hex)` and `brandThemeCss` at build time.
- A Rook product: load `@rookcom/monarch-tokens/css/products/<id>`.

## Before you finish

Run `npx monarch check` (`--product <id>` is optional) and fix new
findings. Each finding has `help` and `docs`; `--fix` applies safe fixes.
Do not add `monarch-ignore` without a written reason.

If the app lints with the Monarch ESLint plugin
(`@rookcom/monarch-conformance/eslint`, rules named `monarch/*`), fix what it
reports as you go: `eslint --fix` rewrites deep `dist/` imports and
deprecated import paths, and the other rules say what to use instead.
