/**
 * Numbers the site states, all counted from real data: the docs index, the
 * product registry and tokens.json. Nothing here is typed in by hand.
 */
import tokens from '@rookcom/monarch-tokens/tokens.json';
import { docsPages, reactVersion } from './docs.js';
import { products } from './registry.js';

export const facts = {
  reactVersion,
  components: docsPages.filter((p) => p.layer === 'components').length,
  patterns: docsPages.filter((p) => p.layer === 'patterns').length,
  documented: docsPages.length,
  themes: Object.keys((tokens as { themes: Record<string, unknown> }).themes).length,
  products: products.length,
  examples: docsPages.reduce((n, p) => n + p.exampleCount, 0),
};

export const INSTALL_COMMAND = 'npm i @rookcom/monarch-tokens @rookcom/monarch-react';
