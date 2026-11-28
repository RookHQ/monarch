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
- **Web, iOS and Android.** Every component has a SwiftUI and a Jetpack Compose
  version that shares the tokens, data and copy, and behaves like the platform.
- **Stays consistent.** A conformance CLI and an ESLint plugin flag raw
  colours, off-grid spacing and local copies of components, with safe fixes
  and SARIF output for code review.

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Made by [Rook](https://rookhq.com), open to everyone under the
[Apache License 2.0](LICENSE). Every component, token and theme can be used
without restriction beyond the license; the Rook and Monarch names and logos are trademarks (see
[License and trademarks](#license-and-trademarks)).

| Package                                                       | What it is                                                                                              | Needs                |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | -------------------- |
| [`@rookcom/monarch-tokens`](packages/tokens)                  | CSS variables, 4 themes, per-product accents with guaranteed contrast, Tailwind 3 preset, `tokens.json` | nothing              |
| [`@rookcom/monarch-react`](packages/react)                    | Accessible components and patterns (React 18/19, SSR-safe) and `styles.css`                             | tokens, React        |
| [`@rookcom/monarch-conformance`](packages/conformance)        | `monarch` CLI: conformance checks with fixes and SARIF, an ESLint plugin, starter apps, an MCP server   | Node only            |
| [`MonarchUI`, `MonarchTokens`](platforms/ios) (Swift package) | SwiftUI components and tokens for iPhone and iPad                                                       | iOS 17               |
| [`dev.monarchui:monarch-compose`](platforms/android) (Maven)  | Jetpack Compose components and tokens for phones and tablets                                            | Android 7.0 (API 24) |

## Start a new app

```bash
npx @rookcom/monarch-conformance create my-app --template next-app   # or vite-react
```

A small, complete starter with the neutral accent (add `--accent '#3355aa'`
for your own colour). It copies files and never runs an install.

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

## Android and iOS

Monarch also has native implementations that share the tokens, colour roles,
type roles and component vocabulary. They look like Monarch and behave like
the platform:

| Platform | Where                                              | Library                                                             |
| -------- | -------------------------------------------------- | ------------------------------------------------------------------- |
| Android  | [`platforms/android`](platforms/android/README.md) | Jetpack Compose: `dev.monarchui:monarch-compose`                    |
| iOS      | [`platforms/ios`](platforms/ios/README.md)         | SwiftUI: the Swift package products `MonarchUI` and `MonarchTokens` |

Both cover all 515 web components (built, styled, adopted from the platform or
composed) and are not released yet. Their version
is the `@rookcom/monarch-react` version: Maven `0.14.N`, and a plain `0.14.N`
git tag for Swift Package Manager, which reads the generated root
`Package.swift`. The architecture is in
[docs/architecture/NATIVE-PLATFORMS.md](docs/architecture/NATIVE-PLATFORMS.md).

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
platforms/
  parity.json     how each web component is covered on native
  android/        Gradle build: monarch-tokens, monarch-compose, sample, inventory/
  ios/            Swift package: MonarchTokens, MonarchUI, Sample, inventory/
Package.swift     generated from platforms/ios/Package.swift, so SwiftPM can resolve the repository
docs/
  architecture/   adr/   guides/   governance/   roadmap/   ROADMAP.md
.github/workflows/   ci.yml (verify, SARIF, secret scan, reproducible build)  release.yml (canary + changesets, npm provenance)  conformance.yml (reusable, for apps)
```

Dependency direction is enforced: `react → tokens`; `conformance` imports
neither. See [ADR 0001](docs/adr/0001-modular-monorepo.md).

## Develop

```bash
nvm use && npm ci
npm run verify     # format, lint, build, size budgets, typecheck, boundaries, tests, registry
npm run changeset  # before opening a PR that changes a package
npm run new:component -- --id <registry-id> --family <family>   # scaffold a component folder
```

Each component is a folder with its code, CSS, tests and docs page; the tree,
required files, CSS order and docs-file contract are in
[docs/architecture/SOURCE-LAYOUT.md](docs/architecture/SOURCE-LAYOUT.md).

## Governance

- Brand: [docs/guides/brand.md](docs/guides/brand.md)
- Versioning and API stability: [docs/governance/versioning.md](docs/governance/versioning.md); native: [native API stability](docs/governance/native-api-stability.md), [native release and maintenance](docs/governance/native-release-and-maintenance.md)
- Component governance, contribution and deprecation: [docs/governance/COMPONENT-GOVERNANCE.md](docs/governance/COMPONENT-GOVERNANCE.md), [CONTRIBUTION-GUIDELINES.md](docs/governance/CONTRIBUTION-GUIDELINES.md), [DEPRECATION-POLICY.md](docs/governance/DEPRECATION-POLICY.md)
- Component registry (generated): [registry/components.json](registry/components.json); architecture index: [docs/README.md](docs/README.md)
- Exceptions: [docs/governance/exceptions.md](docs/governance/exceptions.md)
- Conformance, the ESLint plugin and starter apps: [docs/guides/conformance.md](docs/guides/conformance.md); starters in [templates/starters](templates/starters)
- Bundle-size budgets: [docs/governance/performance-budgets.md](docs/governance/performance-budgets.md)
- Figma Code Connect: [figma/README.md](figma/README.md)
- Frameworks (Next.js, Vite, React Router, CSP, Tailwind 3 and 4): [docs/guides/frameworks.md](docs/guides/frameworks.md)
- Accessibility: [docs/governance/accessibility-statement.md](docs/governance/accessibility-statement.md), with a self-assessed [conformance report](docs/governance/vpat.md) (VPAT 2.5, WCAG edition); browsers: [docs/governance/browser-support.md](docs/governance/browser-support.md); languages and right-to-left: [docs/governance/i18n.md](docs/governance/i18n.md)
- Dependencies and licences: [docs/governance/dependencies.md](docs/governance/dependencies.md)
- Teams outside Rook: [docs/guides/rook-specific-exports.md](docs/guides/rook-specific-exports.md)
- Decisions: [docs/adr](docs/adr)
- For AI coding agents: [AGENTS.md](AGENTS.md) and `@rookcom/monarch-react/manifest.json`

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md) first. To report a vulnerability, see
[SECURITY.md](SECURITY.md).

## License and trademarks

The software code, components and design tokens are licensed under the
[Apache License, Version 2.0](LICENSE). Copyright 2026 Rook Platforms. Keep
the [NOTICE](NOTICE) file when you redistribute Monarch. Third-party software
and fonts are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Releases before the license change (`@rookcom/monarch-react` 0.14.5,
`@rookcom/monarch-tokens` 0.5.4, `@rookcom/monarch-conformance` 0.5.3 and
earlier) remain available under the MIT License.

> [!IMPORTANT]
> **Trademarks, brand names and official logos are reserved by Rook
> Platforms** and governed by [TRADEMARK.md](TRADEMARK.md). You are free to
> build with every component, token and theme, and to say "Built
> with Monarch". You may not use the Rook or Monarch names or logos as your
> own brand, imply that Rook endorses your product, or impersonate Rook.
