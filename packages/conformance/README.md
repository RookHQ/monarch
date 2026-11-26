# @rookcom/monarch-conformance

Keeps a product consistent with Monarch, the open-source design system for
everyone who builds, as it grows. The `monarch` CLI flags raw colours, off-grid
spacing and local copies of components in your code, and reports them in a
form code review can use.

Works in any React app, inside Rook or not. No runtime dependencies.

```sh
npm i -D -E @rookcom/monarch-conformance
npx monarch check                   # report; --fix applies safe fixes
npx monarch search date picker      # find a component in the manifest
npx monarch add Select --write      # import line, stylesheets, example; adds missing CSS imports
npx monarch init-agents             # AGENTS.md, CLAUDE.md, Cursor and Copilot instructions
npx monarch mcp                     # MCP server for coding agents (stdio)
npx monarch figma-connect --map figma/map.json   # Figma Code Connect files from the manifest
```

Start a new app from a starter (Next.js App Router or Vite, both with React 19
and Tailwind 4). It copies files and never runs an install:

```sh
npx @rookcom/monarch-conformance create my-app --template next-app
npx @rookcom/monarch-conformance create my-app --template vite-react --accent '#3355aa'
```

## ESLint plugin

Feedback in the editor, as an ESLint 9 flat config. `eslint` is an optional
peer.

```js
// eslint.config.js
import monarch from '@rookcom/monarch-conformance/eslint';

export default [monarch.configs.recommended]; // or monarch.configs.general outside Rook
```

Rules: `monarch/no-deep-imports` (fix), `monarch/no-deprecated-api` (fix),
`monarch/no-raw-colors` and `monarch/no-arbitrary-spacing` (suggestions),
`monarch/prefer-monarch-component`, `monarch/require-accessible-name`,
`monarch/require-entry-styles` and, in `general`, `monarch/no-rook-specific`.

## More

`--product <id>` is optional (a Rook product); `--accent <hex>` is for teams
outside Rook. Compatibility and deprecation data are bundled. Text, JSON (with
`help`, `docs` and `fix` per finding) and SARIF output (with `fixes`); baseline ratchet;
registry exceptions; `registry validate | verify | report` for the Monarch
registry.

Add the MCP server to Claude Code with `claude mcp add monarch -- npx monarch mcp`;
Cursor and VS Code setup, the rules and the fixes:
https://github.com/RookHQ/monarch/blob/main/docs/guides/conformance.md

## License

[Apache-2.0](https://github.com/RookHQ/monarch/blob/main/LICENSE) © 2026 Rook Platforms. See [NOTICE](https://github.com/RookHQ/monarch/blob/main/NOTICE). The Rook and Monarch names and logos are trademarks of Rook Platforms and are not covered by this license; see [TRADEMARK.md](https://github.com/RookHQ/monarch/blob/main/TRADEMARK.md).
