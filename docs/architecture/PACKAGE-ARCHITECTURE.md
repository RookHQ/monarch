# Package architecture (Monarch 0.4)

Decided 2026-11-03 from the component demand data across the Rook
applications ([COMPONENT-SELECTION](COMPONENT-SELECTION.md)). Supersedes
nothing; extends [ADR 0001](../adr/0001-modular-monorepo.md).

## Principles

1. **A package exists only for a different dependency set or release cadence.**
   Otherwise a component lives in an existing package under a subpath entry.
2. **Dependency direction is one-way** and enforced by dependency-cruiser:
   `tokens ← react`; `conformance` imports none.
3. **No product business logic in Monarch.** Components take data and
   callbacks; products own fetching, permissions and workflow rules.
4. **No runtime dependency on any Monarch service.** Everything ships in the
   consuming app's bundle.
5. **Tree-shakable by construction:** per-file ESM, `sideEffects` limited to CSS,
   subpath exports so an app that never imports charts never parses them.

## Packages

| Package                        | Contains                                                                                                                    | Runtime deps            | Why separate                                       |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------- | ----------------------- | -------------------------------------------------- |
| `@rookcom/monarch-tokens`      | Tokens, themes, contrast engine, product accents, Tailwind preset, appearance script, runtime brand theming (`deriveTheme`) | none                    | Framework-independent; used by non-React surfaces  |
| `@rookcom/monarch-react`       | Primitives, components, patterns, templates (subpath entries below)                                                         | React, Radix primitives | One release line for the whole component catalogue |
| `@rookcom/monarch-conformance` | `monarch` CLI                                                                                                               | none                    | Governance must never import design runtime        |

Monarch publishes exactly these three packages. The Rook app shell and the
rich-text editor are subpath entries of `@rookcom/monarch-react` (`./shell`,
`./editor`), not packages of their own (decided 2026-11-04):

- **`./shell`** encodes Rook cross-app contracts: the `rook_rail` and
  `rook_theme` cookies, `rook-chat-embed` v1 and App Settings embed v1. Those
  contracts change on the Rook-platform cadence, not the component cadence, so
  a change to one is a reviewed change to `src/entries/shell.ts` and its
  folders, called out in the changeset. Its runtime deps are react's own, so a
  separate package would only add a second release line, a second stylesheet
  and a second manifest.
- **`./editor`** carries the heavy Tiptap/ProseMirror dependency (~200 KB) that
  only a few products need. As its own entry it is never parsed by
  an app that does not import it.

## `@rookcom/monarch-react` entry points

| Import                                 | Layer                   | Families                                                                                                                                                                                                                                        |
| -------------------------------------- | ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `@rookcom/monarch-react`               | primitives + components | layout, typography & formatters, actions, forms, navigation, data display, feedback, overlays, identity                                                                                                                                         |
| `@rookcom/monarch-react/data`          | components              | DataGrid, bulk action bar, filter bar, filter chips, saved views, command palette, load-more                                                                                                                                                    |
| `@rookcom/monarch-react/charts`        | components              | line, bar, bar list, sparkline, donut, stacked bar, meter/gauge, delta, chart frame (legend, accessible summary, table fallback)                                                                                                                |
| `@rookcom/monarch-react/scheduling`    | components              | date / date-time / date-range pickers, timezone select, month / time grid, agenda, event chip, weekly hours editor, duration input                                                                                                              |
| `@rookcom/monarch-react/collaboration` | components              | activity timeline, audit log with diff, message row & timeline, mention / caret suggestion menu, notification center, kanban board, file upload & queue                                                                                         |
| `@rookcom/monarch-react/patterns`      | patterns                | page header, toolbar, settings section/row, split layout, record header, custom-domain connect, onboarding checklist, stepper, …                                                                                                                |
| `@rookcom/monarch-react/marketing`     | patterns + templates    | site header/footer, hero, sections, product frame, CTA band, FAQ                                                                                                                                                                                |
| `@rookcom/monarch-react/shell`         | components + patterns   | Rook app shell: AppShell, RookRail, identity bar, Space/Workspace switchers, theme toggle, Help & support panel, chat dock, embedded app frame, no-app-access state                                                                             |
| `@rookcom/monarch-react/ai`            | components + patterns   | AI interfaces: the vendor-neutral message model and `useAiConversation` transport, thread, message, prompt input, streaming text, Markdown, code block, reasoning, tool calls and approval, citations, feedback (ADR 0013). No model-vendor SDK |
| `@rookcom/monarch-react/editor`        | components              | rich-text editor and message composer built on Tiptap; the only entry with the optional `@tiptap/*` peer dependencies                                                                                                                           |

All entries share one stylesheet (`@rookcom/monarch-react/styles.css`) in the
`monarch.components` cascade layer; each component's CSS is a separate source
file in its folder, concatenated in the declared order of
`src/styles/order.json` ([SOURCE-LAYOUT](SOURCE-LAYOUT.md#css-order)), so
unused families add bytes but no specificity conflicts. Subpath entries will
live in `src/entries/<name>.ts`.

## The `./editor` entry

`RichTextEditor` and `MessageComposer` live in
`src/components/collaboration/{rich-text-editor,message-composer}/` and are
exported only from `src/entries/editor.ts`.

- **Tiptap is an optional peer dependency** of `@rookcom/monarch-react`
  (`^2.27.3` in `peerDependencies`, `optional` in `peerDependenciesMeta`;
  tested with 2.27.3). Apps that import
  `./editor` install the twelve `@tiptap/*` packages listed there; apps that
  never import it need none of them.
- **Enforced:** `tiptap-only-in-the-editor-entry` (nothing else in `src`
  imports `@tiptap/*`) and `editor-only-through-its-entry` (no other entry or
  component reaches the editor folders).
- **Bundle cost** (esbuild, minified, react, react-dom and Radix external):
  `RichTextEditor` about 124 KB min+gzip, about 117 KB of it ProseMirror and
  Tiptap core; `MessageComposer` adds about 3 KB. The root entry contains no
  Tiptap. Unused Tiptap code (bubble/floating menus, tippy.js) is tree-shaken;
  the mention, checklist, image and character-count extensions (~4 KB) are
  always bundled with the editor but only enter the schema when enabled.
- **Security follow-up:** advisory GHSA-cp6q-959q-f8rh (moderate,
  `mergeAttributes` and `__proto__`) affects every Tiptap 2.x and is fixed in
  Tiptap 3.32.2. Decision: stay on 2.27.3 now and move the editor and the
  products to Tiptap 3 together on or after 2026-11-13. Products must still
  sanitise stored HTML on the server.

## Layers

- **Primitives** — Box/Stack/Inline/Grid, Text/Heading, VisuallyHidden, formatters (RelativeTime, FormattedDate, Money, NumberDisplay).
- **Components** — functional, self-contained (DataGrid, DatePicker, CommandPalette, FileUpload).
- **Patterns** — compositions solving a recurring problem (bulk edit with preview, record header, custom-domain connect).
- **Templates** — page-level structures (list & manage page, detail page, settings page, three-pane inbox, dashboard).
- **Product extensions** — stay in products unless 2+ products need them; when promoted they live in `@rookcom/monarch-react/shell` (platform) or a product-named subpath and use only Monarch foundations.

## Boundaries enforced

`.dependency-cruiser.cjs`: `react` imports `tokens` through its public entry
only, and `tokens` imports no other package. Inside `react`, no entry imports
another (`nothing-imports-an-entry`), so `./shell` and `./editor` never pull the
rest of the catalogue into their chunk; shared code goes in `src/internal`
([SOURCE-LAYOUT](SOURCE-LAYOUT.md#import-rules)). Only the editor folders and
`./editor` may import `@tiptap/*`, and nothing else reaches the editor folders
(see [The `./editor` entry](#the-editor-entry)).
