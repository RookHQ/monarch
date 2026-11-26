# UI rules

This app uses the Monarch design system. The full rules are in
[AGENTS.md](AGENTS.md); read it before you change UI. The essentials:

- Use a Monarch component before writing a new one. Look it up with
  `npx monarch search <query>` or in
  `node_modules/@rookcom/monarch-react/dist/manifest.json`; copy the `import`
  and `css` fields exactly. Docs: `https://monarchui.dev/components/<id>/`.
- Avoid exports marked `scope: "rook"` (the `@rookcom/monarch-react/rook`
  entry) unless this is a Rook product.
- Colour only through `--mn-*` tokens or `bg-mn-*` / `text-mn-*` /
  `border-mn-*` classes. No palette classes, hex, `rgb()` or arbitrary
  values. Spacing and type use the named scale.
- Every icon-only control has a label, every input sits in a `Field` with a
  `FieldLabel`, every `Dialog` has a `DialogTitle`.
- No `window.confirm`, `alert` or `prompt`: use `useConfirm()`,
  `usePrompt()`, `useChoice()` or `useUndoToast()`.
- Monarch does not use sparkle icons for AI; use the lucide `Bot` icon.
- Run `npx monarch check` before you finish and fix new findings. If the app
  uses the Monarch ESLint plugin (`monarch/*` rules), fix those too.
