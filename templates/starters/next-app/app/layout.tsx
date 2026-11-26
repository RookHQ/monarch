import type { Metadata } from 'next';
import { headers } from 'next/headers';
import type { ReactNode } from 'react';
import { DirectionProvider } from '@rookcom/monarch-react';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';
import './globals.css'; // Tailwind, Monarch tokens (all themes) and the Tailwind theme
import '@rookcom/monarch-tokens/css/base';
import '@rookcom/monarch-tokens/css/products/neutral';
import '@rookcom/monarch-react/styles/core.css';
import '@rookcom/monarch-react/styles/ai.css'; // for components from @rookcom/monarch-react/ai
import './brand.generated.css'; // written by scripts/brand.mjs; must come after the tokens
import brand from '../brand.json';

// A colour in brand.json sets the accent on :root. Without one, the neutral accent applies.
const product = brand.accent ? undefined : 'neutral';
const appearance = appearanceScript(product ? { product } : {});

export const metadata: Metadata = {
  title: '__PROJECT_NAME__',
  description: 'An app built with the Monarch design system.',
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // With a Content Security Policy, middleware passes the nonce on this header.
  const nonce = (await headers()).get('x-nonce') ?? undefined;
  return (
    <html lang="en" dir="ltr" data-mn-product={product} suppressHydrationWarning>
      <head>
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: appearance }} />
      </head>
      <body>
        <DirectionProvider dir="ltr">{children}</DirectionProvider>
      </body>
    </html>
  );
}
