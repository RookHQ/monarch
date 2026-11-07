# Instructions for AI coding agents

This file is read by AI coding agents and similar tools working in
this repository or in an application that consumes Monarch (such as the Rook
applications).

## If you are building UI in an app that uses Monarch

1. Use Monarch components before writing any new UI primitive. The full list
   with props is in `node_modules/@rookcom/monarch-react/dist/manifest.json`
   (machine-readable) or `docs/guides/component-contract.md`.
2. Colour only through tokens: `--mn-*` variables or Tailwind classes
   `bg-mn-surface`, `text-mn-fg-muted`, `border-mn-line`,
   `bg-mn-accent-solid`, `text-mn-danger-strong`, and so on. Never
   `bg-red-500`, `#hex`, `rgb()`, or arbitrary `[...]` colours.
3. Type and spacing: Tailwind's named scale (`text-sm`, `p-4`, `gap-2`),
   never `text-[13px]` or `p-[7px]`.
4. Status meaning: `danger` (Rook Red), `success` (Rook Green), `warning`
   (Rook Yellow), `info` (Rook Blue), `accent` (the product's brand). Do
   not use the accent to mean a status.
5. Accessibility is not optional: every icon-only control has a label
   (`IconButton label=`), every input sits in a `Field` with a `FieldLabel`,
   every `Dialog` has a `DialogTitle`, and destructive actions use
   `variant="danger"` plus a confirmation.
6. Never call `window.confirm`, `alert` or `prompt`. Use `useConfirm()`,
   `usePrompt()`, `useChoice()` or `useUndoToast()`, and pick the level from
   `docs/guides/destructive-actions.md`. `dialogs/no-native-dialog` enforces it.
7. Before finishing, run `npx monarch check --product <id>` (or
   `--root src`) and fix new findings. Do not add `monarch-ignore` without a
   reason, and never hand-edit generated registry files.
8. Do not copy Monarch source into the app. Do not redefine `--mn-*` or
   `--primary`-style variables locally.

## If you are changing Monarch itself

- Run `npm run verify` before claiming anything works. It runs format, lint,
  build, typecheck, dependency boundaries, 230+ tests and registry validation.
- Tokens: raw values go in `packages/tokens/src/primitives.ts` only;
  meaning in `themes.ts`. The contrast tests in `test/contrast.test.ts` must
  pass for every product in `registry/products` and every theme.
- Components: one folder per component, `src/{components,patterns}/<family>/<id>/`,
  created with `npm run new:component -- --id <id> --family <family>` and laid
  out as in [SOURCE-LAYOUT](docs/architecture/SOURCE-LAYOUT.md): `<id>.tsx`,
  plain CSS in `<id>.css` using only `--mn-*` variables (the build rejects raw
  colours and any stylesheet not in `src/styles/order.json`), stable `mn-*`
  class names and `data-*` state attributes, `<id>.test.tsx` covering
  keyboard, focus, accessible names (axe) and SSR, `<id>.docs.tsx` with all 16
  sections, and `index.ts`. Import other components only through their
  `index.ts`. Update the API snapshot deliberately.
- Public API changes need a changeset; breaking changes need an ADR in
  `docs/adr` and a note in `docs/guides/migration.md`.
- Never write a "compliance score". Report counts.
- Do not claim a product has adopted Monarch. Only `monarch registry verify`
  decides that, from a real checkout.

## Icons

Never use sparkle icons (`Sparkles`, `Sparkle`, `WandSparkles`, `Wand2`). For AI
features use the lucide `Bot` icon. Enforced by `icons/no-sparkles`.
