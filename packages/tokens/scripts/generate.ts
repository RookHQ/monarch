/**
 * Step 1 of the build: turn registry/products/*.json into a typed module.
 * The registry is the single source of product accents; nothing is copied by hand.
 */
import { readdirSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const registryDir = join(here, '../../../registry/products');
const outFile = join(here, '../src/generated/products.ts');

const CHANNELS = /^(\d{1,3}(?:\.\d+)?) (\d{1,3}(?:\.\d+)?)% (\d{1,3}(?:\.\d+)?)%$/;

const rows = readdirSync(registryDir)
  .filter((f) => f.endsWith('.json'))
  .sort()
  .map((f) => {
    const p = JSON.parse(readFileSync(join(registryDir, f), 'utf8')) as {
      id: string;
      name: string;
      theme: { accent: string };
    };
    if (`${p.id}.json` !== f) throw new Error(`${f}: id "${p.id}" must match file name`);
    const m = CHANNELS.exec(p.theme.accent);
    if (!m) throw new Error(`${f}: theme.accent must be HSL channels like "354 71% 52%"`);
    return { id: p.id, name: p.name, h: Number(m[1]), s: Number(m[2]), l: Number(m[3]) };
  });

const body = rows
  .map(
    (r) =>
      `  ${JSON.stringify(r.id)}: { id: ${JSON.stringify(r.id)}, name: ${JSON.stringify(r.name)}, accent: hsl(${r.h}, ${r.s}, ${r.l}) },`,
  )
  .join('\n');

mkdirSync(dirname(outFile), { recursive: true });
writeFileSync(
  outFile,
  `// Generated from registry/products by scripts/generate.ts. Do not edit.
import { hsl, type Hsl } from '../color.js';

export interface ProductAccent {
  readonly id: string;
  readonly name: string;
  readonly accent: Hsl;
}

export const products = {
${body}
} as const satisfies Record<string, ProductAccent>;

export type ProductId = keyof typeof products;
export const productIds = Object.keys(products) as ProductId[];
`,
);
console.log(`generated ${rows.length} products`);
