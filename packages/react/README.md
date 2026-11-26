# @rookcom/monarch-react

Accessible React components from Monarch, an open-source design system for
everyone who builds. Forms, overlays, navigation, charts, data tables,
scheduling, collaboration, a rich-text editor and marketing blocks, all on one
set of tokens, server-render safe on React 18 and 19, and themed in light, dark
and high contrast.

Import `@rookcom/monarch-react/styles.css` once and `@rookcom/monarch-tokens/css`
(version 0.5.3 or later) for variables. Docs: https://monarchui.dev

Every export, with its props, defaults, allowed values, root class, exact import
line, stylesheets and docs page, is in `@rookcom/monarch-react/manifest.json`
([JSON Schema](https://monarchui.dev/schema/manifest.json)). Exports marked
`scope: "rook"` are built for Rook platform contracts; prefer `general` ones
outside Rook. `usesClient` says the module starts with `'use client'`;
`clientOnly` is its deprecated alias and never meant "cannot server-render".

`npx monarch search`, `npx monarch add` and the `monarch mcp` server in
`@rookcom/monarch-conformance` read the same manifest.

## License

[Apache-2.0](https://github.com/RookHQ/monarch/blob/main/LICENSE) © 2026 Rook Platforms. See [NOTICE](https://github.com/RookHQ/monarch/blob/main/NOTICE). The Rook and Monarch names and logos are trademarks of Rook Platforms and are not covered by this license; see [TRADEMARK.md](https://github.com/RookHQ/monarch/blob/main/TRADEMARK.md).
