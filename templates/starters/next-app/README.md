# `__PROJECT_NAME__`

A Next.js (App Router) app built with the Monarch design system: React 19,
TypeScript, Tailwind CSS 4 and a small AI chat on a mock transport.

## Run

```bash
npm install
npm run dev      # http://localhost:3000
npm run build
npm run start
```

## Check

```bash
npm run lint     # ESLint with the Monarch rules
npm run check    # monarch check: colours, spacing, accessibility, versions
```

`npx monarch search <query>` finds a component and `npx monarch add <Component>`
prints its import, stylesheets and a small example.

## Where things are

| File                               | What it does                                                  |
| ---------------------------------- | ------------------------------------------------------------- |
| `app/layout.tsx`                   | Loads the stylesheets once and applies the theme before paint |
| `app/globals.css`                  | Tailwind CSS 4 with the Monarch tokens and theme              |
| `app/page.tsx`                     | Home page, a server component                                 |
| `app/greeting.tsx`                 | A client component: a field with state                        |
| `app/assistant/assistant-chat.tsx` | The AI chat. Replace `mockTransport` with your own service    |
| `brand.json`, `scripts/brand.mjs`  | The accent colour (see below)                                 |
| `monarch.config.json`              | Settings for `monarch check`                                  |
| `AGENTS.md`                        | UI rules for AI coding agents                                 |

## Accent colour

With `"accent": ""` in `brand.json` the app uses Monarch's neutral accent.
To use your own colour, set a hex value:

```json
{ "accent": "#0f766e" }
```

`scripts/brand.mjs` runs before `dev` and `build` (or run `npm run brand`). It
calls `deriveTheme` and `brandThemeCss` from `@rookcom/monarch-tokens` and
writes `app/brand.generated.css` with an accent that meets the contrast targets
in the light, dark and high-contrast themes. The colour may be adjusted to get
there.

## Content Security Policy

`app/layout.tsx` reads a nonce from the `x-nonce` request header and puts it on
the inline appearance script. If you add a CSP, create the nonce in middleware
and pass it on that header.

## Docs

- Components: https://monarchui.dev
- Guides (getting started, frameworks, theming, conformance):
  https://github.com/RookHQ/monarch/tree/main/docs/guides
