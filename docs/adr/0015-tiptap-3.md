# ADR 0015: The editor entry moves to Tiptap 3

Status: accepted (2026-11-10), `@rookcom/monarch-react` 0.14.0

## Context

`@rookcom/monarch-react/editor` is built on Tiptap, declared as optional peer
dependencies at `^2.27.3`. Every Tiptap 2.x release, and Tiptap 3 before
3.30.4, has a moderate advisory in `@tiptap/core`: `mergeAttributes` copies a
`__proto__` key from node or mark attributes, so crafted content can pollute
object prototypes. Tiptap 2 will not get a fix. An app that installs the
editor's peers at `^2.27.3` keeps the vulnerable code whatever Monarch does
inside its own tests.

[PACKAGE-ARCHITECTURE](../architecture/PACKAGE-ARCHITECTURE.md) recorded the
plan to move the editor and the products to Tiptap 3 together on or after
2026-11-13. This ADR brings that forward as a security fix.

Raising a peer-dependency floor is breaking under
[versioning](../governance/versioning.md). The usual deprecation period (one
minor with a warning) is not possible here: the components are written
against the Tiptap 3 API, and a range that still accepted Tiptap 2 would keep
the advisory open.

## Decision

- The optional `@tiptap/*` peers become `^3.30.4`. Monarch is built and
  tested with 3.33.0.
- The peer list follows Tiptap 3's packages: `@tiptap/extensions` replaces
  `@tiptap/extension-placeholder` and `@tiptap/extension-character-count`,
  and `@tiptap/extension-list` replaces `@tiptap/extension-task-list` and
  `@tiptap/extension-task-item`. The others (`core`, `pm`, `react`,
  `starter-kit`, `suggestion`, `extension-link`, `extension-image`,
  `extension-mention`) keep their names.
- `RichTextEditor` keeps its schema, HTML output and keyboard behaviour: the
  extensions Tiptap 3's StarterKit adds by default (`underline`,
  `trailingNode`, `listKeymap`, and its own `link`) are turned off, and
  Monarch's own Link configuration is used as before.
- No Monarch prop, export, class, `data-*` attribute or variable changes.
  The types that are Tiptap's own (`RichTextEditorInstance`, the `extensions`
  prop, the editor passed to `EditorToolbar`, `BubbleMenu`, `PageOutline` and
  the block extensions) are now Tiptap 3 types.
- The change ships in a minor (0.14.0), which on 0.x is the breaking line the
  compatibility matrix tracks, with a step in
  [migration](../guides/migration.md).

## Consequences

- Apps that import `@rookcom/monarch-react/editor` upgrade their `@tiptap/*`
  packages to 3.30.4 or later when they take 0.14.0, and move any Tiptap
  code of their own to the Tiptap 3 API.
- Apps that install their own Tiptap 2 packages but never import the editor
  entry still get a peer conflict from npm when they install 0.14.0, because
  optional peers are checked when they are present. They upgrade Tiptap too,
  or stay on 0.13.x until they do.
- Apps that never install Tiptap are not affected.
- Tiptap 3 is about 24 KB gzip larger than Tiptap 2 (core, StarterKit and
  `@tiptap/react`, esbuild minified). `RichTextEditor` is now about 155 KB
  min+gzip. The docs site's budgets for the editor pages and the Tiptap
  chunk were raised by that much, and no other budget changed.
- Products must still sanitise stored HTML on the server.
