/**
 * Contrast measured on what ships: the CSS declaration strings, parsed back.
 *
 * `toCss` rounds each HSL channel to one decimal. A guarantee that holds only
 * for the unrounded maths is not a guarantee: before the fix, the success fill
 * in hc-light was 24.66% lightness (7.06:1 with white) and shipped as
 * `hsl(136 62.8% 24.7%)` (6.97:1). These tests read the emitted strings, so
 * the rounding can never again drop a pair below its target.
 */
import { describe, expect, it } from 'vitest';
import { deriveTheme } from '../src/brand.js';
import {
  colorDistance,
  contrast,
  emitted,
  fromHex,
  hsl,
  toCss,
  toHex,
  type Hsl,
} from '../src/color.js';
import { themeDecls } from '../src/css.js';
import { parseCssHsl } from '../src/dtcg.js';
import { neutralAccent } from '../src/neutral.js';
import { defaultAccent } from '../src/primitives.js';
import { products, productIds } from '../src/generated/products.js';
import {
  INTENT_IDS,
  THEME_IDS,
  deriveIntent,
  resolveTheme,
  themes,
  type ThemeId,
} from '../src/themes.js';

const accents: (readonly [string, Hsl])[] = [
  ['default', defaultAccent],
  ['neutral', neutralAccent],
  ...productIds.map((id) => [id, products[id].accent] as const),
];

/** Parses an emitted `hsl(...)` string, following `var(--x)` references in `vars`. */
function read(vars: ReadonlyMap<string, string>, name: string): Hsl {
  let value = vars.get(name);
  for (let hops = 0; value?.startsWith('var(') && hops < 5; hops++) {
    value = vars.get(value.slice(4, -1));
  }
  if (value === undefined) throw new Error(`No value for ${name}`);
  const parsed = parseCssHsl(value);
  if (!parsed) throw new Error(`${name} is not an hsl() colour: ${value}`);
  return parsed.color;
}

function emittedVars(themeId: ThemeId, accent: Hsl): Map<string, string> {
  return new Map(
    themeDecls(resolveTheme(themes[themeId], accent)).map(([k, v]) => [k, v] as [string, string]),
  );
}

const TEXT = ['text-default', 'text-muted', 'text-subtle'] as const;
const BG = ['bg-canvas', 'bg-surface', 'bg-raised', 'bg-subtle', 'bg-muted'] as const;
const STRONG_ON = ['bg-canvas', 'bg-surface', 'bg-subtle', 'bg-muted'] as const;

describe('emitted precision', () => {
  it('emitted() is what toCss prints', () => {
    for (const c of [hsl(136.04, 62.84, 24.66), hsl(359.96, 0.04, 99.95), fromHex('#28af4c')]) {
      const parsed = parseCssHsl(toCss(c));
      expect(parsed).not.toBeNull();
      expect(toHex(parsed!.color)).toBe(toHex(emitted(c)));
      // Same string, except that a hue rounding up to 360 is normalised to 0 (the same colour).
      if (c.h < 359.95) expect(toCss(emitted(c))).toBe(toCss(c));
    }
  });

  it('regression: hc-light success on-solid reaches 7:1 as emitted (#28af4c)', () => {
    const { scale } = deriveIntent(fromHex('#28af4c'), themes['hc-light']);
    const fill = parseCssHsl(toCss(scale.solid))!.color;
    const text = parseCssHsl(toCss(scale['on-solid']))!.color;
    expect(contrast(text, fill)).toBeGreaterThanOrEqual(7);
  });
});

describe.each(THEME_IDS)('emitted CSS in %s', (themeId) => {
  const theme = themes[themeId];
  const t = theme.targets;

  it.each(accents)('accent %s: every guaranteed pair meets its target', (_id, accent) => {
    const v = emittedVars(themeId, accent);
    const c = (name: string) => read(v, `--mn-color-${name}`);
    for (const role of TEXT) {
      for (const bg of BG) {
        expect(contrast(c(role), c(bg)), `${role} on ${bg}`).toBeGreaterThanOrEqual(t.text);
      }
    }
    expect(contrast(c('text-inverse'), c('bg-inverse'))).toBeGreaterThanOrEqual(t.text);
    expect(contrast(c('text-inverse-muted'), c('bg-inverse'))).toBeGreaterThanOrEqual(t.text);
    for (const bg of ['bg-canvas', 'bg-surface', 'bg-raised', 'bg-subtle'] as const) {
      expect(contrast(c('border-input'), c(bg)), `border-input on ${bg}`).toBeGreaterThanOrEqual(
        t.ui,
      );
    }
    for (const intent of INTENT_IDS) {
      const i = (role: string) => c(`${intent}-${role}`);
      for (const fill of ['solid', 'solid-hover', 'solid-active']) {
        expect(
          contrast(i('on-solid'), i(fill)),
          `${intent} on-solid on ${fill}`,
        ).toBeGreaterThanOrEqual(t.text);
      }
      expect(contrast(i('on-strong'), i('strong')), `${intent} on-strong`).toBeGreaterThanOrEqual(
        t.text,
      );
      for (const bg of [...STRONG_ON.map(c), i('subtle'), i('subtle-hover')]) {
        expect(contrast(i('strong'), bg), `${intent} strong`).toBeGreaterThanOrEqual(t.text);
      }
      expect(
        contrast(i('strong-inverse'), c('bg-inverse')),
        `${intent} strong-inverse`,
      ).toBeGreaterThanOrEqual(t.text);
    }
    expect(contrast(c('text-link'), c('bg-surface'))).toBeGreaterThanOrEqual(t.text);
    expect(contrast(c('focus-ring'), c('bg-surface'))).toBeGreaterThanOrEqual(3);
    for (let k = 1; k <= 5; k++) {
      for (const bg of ['bg-canvas', 'bg-surface', 'bg-raised', 'bg-subtle'] as const) {
        expect(contrast(c(`data-${k}`), c(bg)), `data-${k} on ${bg}`).toBeGreaterThanOrEqual(t.ui);
      }
    }
  });
});

/** A sweep of brand colours across hue, saturation and lightness, plus the hard cases. */
const sweep: string[] = [
  '#28af4c',
  '#ffff00',
  '#00ffff',
  '#ffffff',
  '#000000',
  '#808080',
  '#0a7cff',
  '#f4b400',
];
for (let h = 0; h < 360; h += 15) {
  for (const s of [15, 60, 100]) {
    for (const l of [20, 45, 70, 90]) sweep.push(toHex(hsl(h, s, l)));
  }
}

describe('deriveTheme vars as emitted', () => {
  it.each(THEME_IDS)('%s: accent and custom AI roles meet every guarantee', (themeId) => {
    const theme = themes[themeId];
    const t = theme.targets.text;
    const page = emittedVars(themeId, defaultAccent);
    const bg = (name: string) => read(page, `--mn-color-${name}`);
    for (const brand of sweep) {
      const vars = new Map(
        Object.entries(deriveTheme(brand, { ai: '#9333ea' }).modes[themeId].vars),
      );
      for (const intent of ['accent', 'ai']) {
        const i = (role: string) => read(vars, `--mn-color-${intent}-${role}`);
        for (const fill of ['solid', 'solid-hover', 'solid-active']) {
          expect(
            contrast(i('on-solid'), i(fill)),
            `${brand} ${intent} on-solid on ${fill}`,
          ).toBeGreaterThanOrEqual(t);
        }
        expect(
          contrast(i('on-strong'), i('strong')),
          `${brand} ${intent} on-strong`,
        ).toBeGreaterThanOrEqual(t);
        for (const b of [...STRONG_ON.map(bg), i('subtle'), i('subtle-hover')]) {
          expect(contrast(i('strong'), b), `${brand} ${intent} strong`).toBeGreaterThanOrEqual(t);
        }
        expect(
          contrast(i('strong-inverse'), bg('bg-inverse')),
          `${brand} ${intent} strong-inverse`,
        ).toBeGreaterThanOrEqual(t);
      }
    }
  });
});

describe('neutral roles stay distinct (C-086)', () => {
  // Border and fill must not be the same colour, or a bordered bg-muted control
  // loses its edge; a subtle panel must not vanish on a surface.
  it.each(THEME_IDS)(
    '%s: bg-subtle differs from bg-surface, border-default from bg-muted',
    (id) => {
      const n = themes[id].neutrals;
      expect(toCss(n['bg-subtle'])).not.toBe(toCss(n['bg-surface']));
      expect(toCss(n['border-default'])).not.toBe(toCss(n['bg-muted']));
      expect(colorDistance(n['bg-subtle'], n['bg-surface'])).toBeGreaterThanOrEqual(1.5);
      expect(colorDistance(n['border-default'], n['bg-muted'])).toBeGreaterThanOrEqual(4);
    },
  );

  it('dark keeps the surface < subtle < muted and muted < border-default < border-strong order', () => {
    const n = themes.dark.neutrals;
    const l = (c: Hsl) => c.l;
    expect(l(n['bg-surface'])).toBeLessThan(l(n['bg-subtle']));
    expect(l(n['bg-subtle'])).toBeLessThan(l(n['bg-muted']));
    expect(l(n['bg-muted'])).toBeLessThan(l(n['border-default']));
    expect(l(n['border-default'])).toBeLessThan(l(n['border-strong']));
  });
});
