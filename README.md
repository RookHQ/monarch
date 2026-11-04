# Monarch

**Meet Monarch.**

Monarch is an open-source design system for everyone who builds: startups and
enterprise teams, independent designers and developers. It gives you a calm,
considered foundation, so your product feels finished from the very first
screen and stays consistent as it grows.

- **Accessible by default.** Keyboard support, focus management and accessible
  names in every component, tested with axe and checked with axe in Chromium,
  Firefox and WebKit on the docs site.
- **Your colour, everywhere.** Light, dark and high-contrast themes. Bring one
  brand colour and `deriveTheme` builds a palette that meets WCAG 2.2 AA
  contrast in every theme.
- **Everything a product needs.** Forms, overlays, navigation, charts, data
  tables, scheduling, collaboration, a rich-text editor and marketing blocks.
- **Safe to render anywhere.** Server-render safe components on React 18 and 19.
- **Stays consistent.** A conformance CLI flags raw colours, off-grid spacing
  and local copies of components, with SARIF output for code review.

Made by [Rook](https://rookhq.com), open to everyone under the [MIT License](LICENSE).

| Package                                                | What it is                                                                                              | Needs         |
| ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------- | ------------- |
| [`@rookcom/monarch-tokens`](packages/tokens)           | CSS variables, 4 themes, per-product accents with guaranteed contrast, Tailwind 3 preset, `tokens.json` | nothing       |
| [`@rookcom/monarch-react`](packages/react)             | Accessible components and patterns (React 18/19, SSR-safe) and `styles.css`                             | tokens, React |
| [`@rookcom/monarch-conformance`](packages/conformance) | `monarch` CLI: design-conformance checks, SARIF, registry verification                                  | Node only     |

## Install

```bash
npm i @rookcom/monarch-tokens @rookcom/monarch-react
```

```tsx
// app/layout.tsx
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/products/app'; // your product id
import '@rookcom/monarch-react/styles.css';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

<html suppressHydrationWarning>
  <head>
    <script dangerouslySetInnerHTML={{ __html: appearanceScript({ product: 'app' }) }} />
  </head>
```

```ts
// tailwind.config.ts (Tailwind 3.4)
import monarch from '@rookcom/monarch-tokens/tailwind';
export default { presets: [monarch], content: ['./src/**/*.{ts,tsx}'] };
```

Full steps, including the legacy bridge for existing shadcn variables, in
[docs/guides/getting-started.md](docs/guides/getting-started.md) and
[docs/guides/migration.md](docs/guides/migration.md).

## Repository layout

```
packages/
  tokens/        src/{color,primitives,themes,css,tailwind,appearance}.ts  scripts/{generate,emit}.ts  test/
  react/         src/{components,patterns}/<family>/<id>/  src/styles/order.json  scripts/  test/   (docs/architecture/SOURCE-LAYOUT.md)
  conformance/   src/{rules,check,report,registry,semver,cli}.ts  test/
registry/
  products/*.json       declared brand data: id, domain, accent, source paths
  product.schema.json   compatibility.json   deprecations.json   roadmap-waves.json
  component-demand.json how many products need each component (counts only)
  components.json       generated component registry (scripts/registry)
docs/
  architecture/   adr/   guides/   governance/   roadmap/   ROADMAP.md
.github/workflows/   ci.yml (verify, SARIF, secret scan, reproducible build)  release.yml (canary + changesets, npm provenance)  conformance.yml (reusable, for apps)
```

Dependency direction is enforced: `react → tokens`; `conformance` imports
neither. See [ADR 0001](docs/adr/0001-modular-monorepo.md).

## Develop

```bash
nvm use && npm ci
npm run verify     # format, lint, build, typecheck, boundaries, tests, registry
npm run changeset  # before opening a PR that changes a package
npm run new:component -- --id <registry-id> --family <family>   # scaffold a component folder
```

Each component is a folder with its code, CSS, tests and docs page; the tree,
required files, CSS order and docs-file contract are in
[docs/architecture/SOURCE-LAYOUT.md](docs/architecture/SOURCE-LAYOUT.md).

## Governance

- Brand: [docs/guides/brand.md](docs/guides/brand.md)
- Versioning and API stability: [docs/governance/versioning.md](docs/governance/versioning.md)
- Component governance, contribution and deprecation: [docs/governance/COMPONENT-GOVERNANCE.md](docs/governance/COMPONENT-GOVERNANCE.md), [CONTRIBUTION-GUIDELINES.md](docs/governance/CONTRIBUTION-GUIDELINES.md), [DEPRECATION-POLICY.md](docs/governance/DEPRECATION-POLICY.md)
- Component registry (generated): [registry/components.json](registry/components.json); architecture index: [docs/README.md](docs/README.md)
- Exceptions: [docs/governance/exceptions.md](docs/governance/exceptions.md)
- Conformance: [docs/guides/conformance.md](docs/guides/conformance.md)
- Decisions: [docs/adr](docs/adr)
- For AI coding agents: [AGENTS.md](AGENTS.md) and `@rookcom/monarch-react/manifest.json`

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md) first. To report a vulnerability, see
[SECURITY.md](SECURITY.md).

## License

Monarch is licensed under the [MIT License](LICENSE). Copyright (c) 2026 Rook
Platforms. Third-party components and fonts used by the documentation site are
listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Trademarks

The Rook name and logos are trademarks of Rook Platforms and are not licensed
under the MIT License. You may use the code freely, but you may not use the Rook
name or logos to suggest that your project or product is made or endorsed by
Rook Platforms. The brand colours exposed as tokens are part of the code; the
logos in the Rook brand kit are not.
