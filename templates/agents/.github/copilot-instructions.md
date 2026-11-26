# Copilot instructions: Monarch UI

This app builds its UI with the Monarch design system
(`@rookcom/monarch-react`, `@rookcom/monarch-tokens`). The full rules are in
`AGENTS.md` in the repository root.

- Use a Monarch component before writing a new UI primitive. Find it in
  `node_modules/@rookcom/monarch-react/dist/manifest.json` or with
  `npx monarch search <query>`, and copy its `import` and `css` fields
  exactly. Docs: `https://monarchui.dev/components/<id>/`.
- Avoid exports marked `scope: "rook"` (the `@rookcom/monarch-react/rook`
  entry) unless this is a Rook product.
- Colour only through `--mn-*` tokens or `bg-mn-*`, `text-mn-*`,
  `border-mn-*` classes. Never palette classes such as `bg-red-500`, hex,
  `rgb()` or arbitrary `[...]` colours.
- Type and spacing use the named scale (`text-sm`, `p-4`, `gap-2`).
- `danger`, `success`, `warning` and `info` mean status; `accent` is the
  brand.
- Icon-only controls have a label (`IconButton label=`), inputs sit in a
  `Field` with a `FieldLabel`, every `Dialog` has a `DialogTitle`, and
  destructive actions use `variant="danger"` and a confirmation.
- Never `window.confirm`, `alert` or `prompt`: use `useConfirm()`,
  `usePrompt()`, `useChoice()` or `useUndoToast()`.
- With `@rookcom/monarch-react/styles/core.css`, load each stylesheet in the
  export's `css` field once in the root layout.
- Style through props, tokens and the root and part `mn-*` classes in the
  manifest (`className`); never modifier classes or `--_*` variables.
- Monarch does not use sparkle icons for AI; use the lucide `Bot` icon.
- Outside Rook, use the neutral accent
  (`@rookcom/monarch-tokens/css/products/neutral` with
  `<html data-mn-product="neutral">`) or your own via `deriveTheme(hex)`.
- Run `npx monarch check` before finishing and fix new findings, along with
  anything the Monarch ESLint plugin (`monarch/*` rules) reports.
