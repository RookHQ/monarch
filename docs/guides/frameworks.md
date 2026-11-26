# Frameworks

How to set Monarch up in common React frameworks. The steps are the same
everywhere: load the stylesheets once, run `appearanceScript` in `<head>`
before first paint, and render components. The general steps are in
[getting started](getting-started.md); this page covers what differs.

Requirements: React 18.3 or 19, `@rookcom/monarch-react` 0.14.x with
`@rookcom/monarch-tokens` `>=0.5.3 <1.0.0`
([compatibility](../architecture/COMPATIBILITY.md)). Browser minimums are in
[browser support](../governance/browser-support.md).

## How the package is built

- Plain ES modules, one file per source module (compiled with `tsc`, not
  bundled), so bundlers tree-shake per component.
- `'use client'` stays at the top of each file that needs it. Files that hold
  state, effects, context or a Radix primitive have it; purely presentational
  files do not. Of the 514 component and pattern folders, 418 main files are
  client modules and 96 are not (for example `Card`, `Badge`, `Heading`,
  `Text`, `PageHeader`, `Kbd`, `StatCard`).
- The entry points (`@rookcom/monarch-react`, `/patterns`, `/data`, `/ai` and
  the rest) carry no directive themselves. They only re-export, so a server
  component can import from any entry and each client component still
  becomes its own client boundary.
- The [component contract](component-contract.md) forbids browser globals
  at module scope, and every component's test renders it with
  `renderToString`.

## Next.js App Router (React Server Components)

```tsx
// app/layout.tsx (a server component)
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/products/neutral'; // or products/<id> in a Rook product
import '@rookcom/monarch-react/styles.css';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';
import { TooltipProvider } from '@rookcom/monarch-react';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: appearanceScript({ product: 'neutral' }) }} />
      </head>
      <body>
        <TooltipProvider>{children}</TooltipProvider>
      </body>
    </html>
  );
}
```

- **Server components can import Monarch** and render any component. A
  client component (such as `Button` or `Dialog`) renders on the server too
  and hydrates in the browser.
- **The usual RSC rule applies at the boundary.** A server component cannot
  pass a function to a client component, so `onClick`, `onValueChange`,
  render props and hooks (`useConfirm`, `useToast`) belong in a file of your
  own that starts with `'use client'`. Pass data, strings and React elements
  across the boundary.
- `suppressHydrationWarning` on `<html>` is needed because the appearance
  script sets attributes before React hydrates.
- Navigation components take your router's link through `linkAs` or
  `linkComponent`; pass `next/link` as it is.

## Next.js Pages Router

Global CSS must be imported in `pages/_app.tsx`; the script goes in
`pages/_document.tsx`.

```tsx
// pages/_app.tsx
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/products/neutral';
import '@rookcom/monarch-react/styles.css';
import type { AppProps } from 'next/app';

export default function App({ Component, pageProps }: AppProps) {
  return <Component {...pageProps} />;
}
```

```tsx
// pages/_document.tsx
import { Head, Html, Main, NextScript } from 'next/document';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

export default function Document() {
  return (
    <Html lang="en" suppressHydrationWarning>
      <Head>
        <script dangerouslySetInnerHTML={{ __html: appearanceScript({ product: 'neutral' }) }} />
      </Head>
      <body>
        <Main />
        <NextScript />
      </body>
    </Html>
  );
}
```

`'use client'` has no effect in the Pages Router; every component works.

## Vite (single-page app)

Import the stylesheets in your entry file (`src/main.tsx`) and inject the
script into `index.html` at build time. The Monarch docs site does the same
in `apps/docs/vite.config.ts`.

```ts
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'monarch-appearance',
      transformIndexHtml: () => [
        {
          tag: 'script',
          children: appearanceScript({ product: 'neutral' }),
          injectTo: 'head-prepend',
        },
      ],
    },
  ],
});
```

## React Router 7 (framework mode) and Remix

Render the script in the root `Layout`. Stylesheet imports work as in Vite.

```tsx
// app/root.tsx
import { Links, Meta, Outlet, Scripts, ScrollRestoration } from 'react-router';
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/products/neutral';
import '@rookcom/monarch-react/styles.css';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

const APPEARANCE = appearanceScript({ product: 'neutral' });

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <script dangerouslySetInnerHTML={{ __html: APPEARANCE }} />
        <Meta />
        <Links />
      </head>
      <body>
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  );
}

export default function App() {
  return <Outlet />;
}
```

Remix 2 on Vite is the same, with imports from `@remix-run/react`.

## Content Security Policy

`appearanceScript` returns a short inline script. It validates and
JSON-encodes its options, but a strict CSP still blocks inline scripts unless
you allow it. It has no `nonce` option; use one of these.

**Nonce.** Put the nonce on the `<script>` element yourself. In the Next.js
App Router, middleware creates the nonce and the CSP header and passes the
nonce on a request header; the layout reads it:

```tsx
// app/layout.tsx
import { headers } from 'next/headers';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const nonce = (await headers()).get('x-nonce') ?? undefined;
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          nonce={nonce}
          dangerouslySetInnerHTML={{ __html: appearanceScript({ product: 'neutral' }) }}
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
```

Reading headers makes the route render per request, which a nonce needs
anyway.

**Hash.** The script text depends only on its options, so a static site can
allow it by hash and keep pages cacheable:

```ts
import { createHash } from 'node:crypto';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

const src = appearanceScript({ product: 'neutral' });
const hash = `'sha256-${createHash('sha256').update(src).digest('base64')}'`;
// script-src 'self' <hash>
```

Hash the exact string you emit, and recompute it whenever you upgrade
`@rookcom/monarch-tokens` or change the options. The Monarch docs site
allows its inline scripts this way and checks the hashes in CI.

**Styles.** Some components set inline `style` attributes (sizes, positions,
per-item colours), so `style-src` needs `'unsafe-inline'`. The Monarch docs
site uses `style-src 'self' 'unsafe-inline'`.

## Tailwind 3 and Tailwind 4

Tailwind is optional. Both versions get the same class names (`bg-mn-surface`,
`text-mn-fg-muted`, `border-mn-line`, `rail-expanded:` and so on).

|                      | Tailwind 3.4                                                 | Tailwind 4                                                                    |
| -------------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| Set-up               | `presets: [monarch]` from `@rookcom/monarch-tokens/tailwind` | `@import '@rookcom/monarch-tokens/css/tailwind-v4'` in your CSS               |
| Preflight and layers | wrap `@tailwind base` in `@layer monarch.reset { … }`        | start the CSS file with `@layer theme, base, monarch, components, utilities;` |
| Legacy shadcn names  | included in the preset                                       | add `css/compat` and `css/tailwind-v4-compat`                                 |

Full steps: [getting started](getting-started.md#4-tailwind) and
[theming](theming.md#tailwind-css).
