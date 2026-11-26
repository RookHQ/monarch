/**
 * Emits the golden vectors that keep the native brand theme engines
 * (`MonarchBrandTheme` on Android and iOS) identical to `deriveTheme`.
 *
 * Every input runs through the web `deriveTheme` and `parseHexColor`, and the
 * exact results are written to one JSON file, committed as a test resource on
 * both platforms:
 *
 *   platforms/android/monarch-tokens/src/test/resources/brand-theme-vectors.json
 *   platforms/ios/Tests/Fixtures/brand-theme-vectors.json
 *
 * Colours are written as the CSS prints them (channels rounded to one decimal,
 * then to 8-bit sRGB), the same rounding `emit-native.ts` uses for the
 * generated accents. `--check` fails when either file no longer matches the
 * token source.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deriveTheme, parseHexColor, type BrandTheme } from '../src/brand.js';
import { toCss, toHex, type Hsl } from '../src/color.js';
import { parseCssHsl } from '../src/dtcg.js';
import { neutralAccent } from '../src/neutral.js';
import { defaultAccent } from '../src/primitives.js';
import { products } from '../src/generated/products.js';
import { THEME_IDS, type IntentReport, type IntentScale } from '../src/themes.js';

const root = join(dirname(fileURLToPath(import.meta.url)), '../../..');
const FILES = [
  'platforms/android/monarch-tokens/src/test/resources/brand-theme-vectors.json',
  'platforms/ios/Tests/Fixtures/brand-theme-vectors.json',
];

const ROLES = [
  ['solid', 'solid'],
  ['solidHover', 'solid-hover'],
  ['solidActive', 'solid-active'],
  ['onSolid', 'on-solid'],
  ['subtle', 'subtle'],
  ['subtleHover', 'subtle-hover'],
  ['strong', 'strong'],
  ['onStrong', 'on-strong'],
  ['border', 'border'],
  ['strongInverse', 'strong-inverse'],
] as const;

/** A derived colour as the CSS prints it, as `#RRGGBB` (same as `rgbaOfCss` in emit-native.ts). */
function cssHex(c: Hsl): string {
  const parsed = parseCssHsl(toCss(c));
  if (!parsed) throw new Error(`Cannot read ${toCss(c)}`);
  return toHex(parsed.color).toUpperCase();
}

const scale = (s: IntentScale) => Object.fromEntries(ROLES.map(([n, r]) => [n, cssHex(s[r])]));
const report = (r: IntentReport) => ({
  base: r.base,
  adjustments: r.adjustments,
  ratios: r.ratios,
});

function result(t: BrandTheme) {
  return {
    input: t.input,
    brand: t.brand,
    valid: t.valid,
    modes: Object.fromEntries(
      THEME_IDS.map((id) => {
        const m = t.modes[id];
        return [
          id,
          {
            accent: scale(m.accent),
            report: report(m.report),
            ...(m.ai && m.aiReport ? { ai: scale(m.ai), aiReport: report(m.aiReport) } : {}),
          },
        ];
      }),
    ),
    // The raw distance depends on the platform's pow and cbrt in its last bits, so it is
    // written to 9 decimals and compared to that precision; everything else is exact.
    ai: t.ai && {
      ...t.ai,
      closest: t.ai.closest && {
        ...t.ai.closest,
        distance: Math.round(t.ai.closest.distance * 1e9) / 1e9,
      },
    },
  };
}

const unique = (list: string[]) => [...new Set(list)];

const brands: string[] = unique([
  // Rook-like reds, in every accepted spelling.
  toHex(defaultAccent),
  '#DC2E3E',
  'dc2e3e',
  '  #dc2e3e  ',
  '#e53935',
  '#c8102e',
  '#ff0000',
  '#b71c1c',
  // Every product accent and the neutral accent, as hex.
  ...Object.values(products).map((p) => toHex(p.accent)),
  toHex(neutralAccent),
  // Very light, very dark and greys.
  '#ffffff',
  '#fafafa',
  '#f0f0f0',
  '#000000',
  '#111111',
  '#1a1a2e',
  '#808080',
  '#9e9e9e',
  '#555555',
  // Saturated yellows, cyans and greens that need large adjustment.
  '#ffff00',
  '#ffeb3b',
  '#f4b400',
  '#00ffff',
  '#00e5ff',
  '#7fffd4',
  '#ccff00',
  '#39ff14',
  // Blues, purples and others.
  '#0a7cff',
  '#0000ff',
  '#1e90ff',
  '#8a2be2',
  '#ff69b4',
  '#008080',
  '#ff8c00',
  '#6366f1',
  // Three-digit hex.
  '#fff',
  '#000',
  '#777',
  '#f0a',
  'abc',
  '#ABC',
  // Surrounding white space the web trims.
  '\t#0a7cff\n',
  ' #0a7cff﻿',
  // Invalid: the default accent is used.
  '',
  '   ',
  'red',
  '#12345',
  '#1234567',
  '#12345678',
  '#abcd',
  '#ggg',
  '##abc',
  'rgb(0, 0, 0)',
  '#0a7cff ff',
]);

/** Brand and AI colour pairs: distinct, too close to the accent or a status, and invalid. */
const withAi: [string, string][] = [
  ['#0a7cff', '#9333ea'],
  ['#dc2e3e', '#7c3aed'],
  ['#0a7cff', '#0b7dff'],
  ['#0a7cff', '#e11d48'],
  ['#0a7cff', '#22c55e'],
  ['#6366f1', '#f59e0b'],
  ['#008080', '#a0f'],
  ['#008080', 'purple'],
  ['#008080', ''],
  ['nope', '#9333ea'],
  ['#ffff00', '#ffffff'],
  ['#000', '#000'],
];

const parseInputs: (string | null)[] = [
  ...unique([
    ...brands,
    ...withAi.map(([, a]) => a),
    '#A1B2C3',
    'F0A',
    ' #f0a ',
    '#f0a\n',
    'f0a ',
    '#',
    '0x0a7cff',
    '#0a7cff00',
    '#éée',
  ]),
  null,
];

function parseVector(input: string | null) {
  const c = parseHexColor(input);
  return c
    ? { input, hex: toHex(c), h: c.h, s: c.s, l: c.l }
    : { input, hex: null, h: null, s: null, l: null };
}

/**
 * The WCAG linearised value of every 8-bit channel, as the web computes it, to 12
 * significant digits: `Math.pow` may differ from the JVM's and Apple's in the last bit.
 */
const linear = Array.from({ length: 256 }, (_, i) => {
  const v = i / 255;
  return Number((v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)).toPrecision(12));
});

const vectors = {
  about:
    'Generated by packages/tokens/scripts/emit-brand-vectors.ts from @rookcom/monarch-tokens. Do not edit.',
  linear,
  parse: parseInputs.map(parseVector),
  themes: [
    ...brands.map((brand) => ({ brand, result: result(deriveTheme(brand)) })),
    ...withAi.map(([brand, ai]) => ({ brand, ai, result: result(deriveTheme(brand, { ai })) })),
  ],
};

const content = `${JSON.stringify(vectors, null, 1)}\n`;
const check = process.argv.includes('--check');
let stale = 0;
for (const rel of FILES) {
  const file = join(root, rel);
  if (check) {
    if (!existsSync(file) || readFileSync(file, 'utf8') !== content) {
      console.error(`Stale: ${rel}`);
      stale++;
    }
  } else {
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, content);
    console.log(`Wrote ${rel}`);
  }
}
if (stale > 0) {
  console.error('Brand theme vectors are out of date. Run: npm run native:brand');
  process.exit(1);
}
console.log(
  `Brand theme vectors ${check ? 'ok' : 'written'}: ${vectors.themes.length} themes, ${vectors.parse.length} parse cases.`,
);
