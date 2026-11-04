import { describe, expect, it } from 'vitest';
import { contrast, fromHex, hsl, toChannels, toHex, toRgb } from '../src/color.js';
import { defaultAccent } from '../src/primitives.js';
import {
  INTENT_IDS,
  INVERSE_RAISED,
  INVERSE_TINT,
  THEME_IDS,
  resolveTheme,
  themes,
} from '../src/themes.js';
import { DATA_PALETTE_ORDER, compatPrimary, dataPalette } from '../src/css.js';
import { products, productIds } from '../src/generated/products.js';

const accents = [
  ['default', defaultAccent] as const,
  ...productIds.map((id) => [id, products[id].accent] as const),
];

describe('colour math', () => {
  it('matches known WCAG ratios', () => {
    expect(contrast(hsl(0, 0, 0), hsl(0, 0, 100))).toBeCloseTo(21, 5);
    expect(contrast(hsl(0, 0, 100), hsl(0, 0, 100))).toBeCloseTo(1, 5);
    // #767676 on white is the well-known 4.54:1 threshold grey.
    expect(contrast(fromHex('#767676'), fromHex('#ffffff'))).toBeCloseTo(4.54, 2);
  });

  it('round-trips hex', () => {
    for (const hex of ['#dc2e3e', '#0096d4', '#28af4c', '#f4b400', '#6366f1']) {
      expect(toHex(fromHex(hex))).toBe(hex);
    }
  });
});

describe.each(THEME_IDS)('theme %s', (themeId) => {
  const theme = themes[themeId];
  const n = theme.neutrals;
  const t = theme.targets;
  const backgrounds = ['bg-canvas', 'bg-surface', 'bg-raised', 'bg-subtle'] as const;

  it.each(['text-default', 'text-muted', 'text-subtle'] as const)(
    '%s meets text target on every background',
    (role) => {
      for (const bg of [...backgrounds, 'bg-muted'] as const) {
        expect(contrast(n[role], n[bg]), `${role} on ${bg}`).toBeGreaterThanOrEqual(t.text);
      }
    },
  );

  it('text-inverse meets text target on bg-inverse', () => {
    expect(contrast(n['text-inverse'], n['bg-inverse'])).toBeGreaterThanOrEqual(t.text);
  });

  it('input borders meet the non-text contrast target (WCAG 1.4.11)', () => {
    for (const bg of backgrounds) {
      expect(contrast(n['border-input'], n[bg]), `border-input on ${bg}`).toBeGreaterThanOrEqual(
        t.ui,
      );
    }
  });

  it.each(accents)('accent for %s meets every guarantee', (_id, accent) => {
    const r = resolveTheme(theme, accent);
    for (const intent of INTENT_IDS) {
      const s = r.intents[intent];
      expect(contrast(s['on-solid'], s.solid), `${intent} on-solid`).toBeGreaterThanOrEqual(t.text);
      expect(
        contrast(s['on-solid'], s['solid-hover']),
        `${intent} on-solid hover`,
      ).toBeGreaterThanOrEqual(t.text);
      expect(
        contrast(s['on-solid'], s['solid-active']),
        `${intent} on-solid active`,
      ).toBeGreaterThanOrEqual(t.text);
      expect(contrast(s['on-strong'], s.strong), `${intent} on-strong`).toBeGreaterThanOrEqual(
        t.text,
      );
      for (const bg of [
        ...backgrounds.map((b) => n[b]),
        n['bg-muted'],
        s.subtle,
        s['subtle-hover'],
      ]) {
        expect(contrast(s.strong, bg), `${intent} strong`).toBeGreaterThanOrEqual(t.text);
      }
      // Focus ring (accent strong) must stand out from surfaces (WCAG 2.4.11 / 1.4.11).
      expect(contrast(s.strong, n['bg-surface'])).toBeGreaterThanOrEqual(3);
    }
  });
});

describe('deriveIntent edge accents', () => {
  const edge = [
    ['white', hsl(0, 0, 100)],
    ['near-white', hsl(0, 0, 98)],
    ['black', hsl(0, 0, 0)],
    ['mid-grey', hsl(0, 0, 50)],
    ['pure-yellow', hsl(60, 100, 50)],
    ['pale-yellow', hsl(60, 100, 95)],
    ['navy', hsl(240, 100, 5)],
    ['pure-red', hsl(0, 100, 50)],
    ['desaturated', hsl(180, 5, 50)],
  ] as const;

  it.each(THEME_IDS)(
    '%s: solid, hover and active stay visible on the canvas with readable text',
    (themeId) => {
      const theme = themes[themeId];
      for (const [name, accent] of [...edge, ...accents]) {
        const s = resolveTheme(theme, accent).intents.accent;
        for (const role of ['solid', 'solid-hover', 'solid-active'] as const) {
          // Standard themes: a filled control only needs to be distinguishable
          // from the page (its text identifies it). High contrast: the UI target.
          expect(
            contrast(s[role], theme.neutrals['bg-canvas']),
            `${name} ${role} vs canvas`,
          ).toBeGreaterThanOrEqual(theme.contrast === 'more' ? theme.targets.ui : 1.5);
          expect(
            contrast(s['on-solid'], s[role]),
            `${name} text on ${role}`,
          ).toBeGreaterThanOrEqual(theme.targets.text);
        }
      }
    },
  );
});

describe('legacy bridge --primary', () => {
  it.each(THEME_IDS)('%s: text-primary is readable wherever it backs the fill', (themeId) => {
    const theme = themes[themeId];
    const n = theme.neutrals;
    for (const [name, accent] of accents) {
      const r = resolveTheme(theme, accent);
      const p = compatPrimary(r);
      // Always a valid fill.
      expect(contrast(p.on, p.fill), `${name} primary-foreground`).toBeGreaterThanOrEqual(
        theme.targets.text,
      );
      if (p.usesStrong) {
        for (const bg of ['bg-canvas', 'bg-surface', 'bg-subtle', 'bg-muted'] as const) {
          expect(contrast(p.fill, n[bg]), `${name} text-primary on ${bg}`).toBeGreaterThanOrEqual(
            theme.targets.text,
          );
        }
      }
    }
  });
});

/** sRGB mix, matching `color-mix(in srgb, a p%, b)`. */
function mix(a: Parameters<typeof toRgb>[0], b: Parameters<typeof toRgb>[0], p: number) {
  const [ar, ag, ab] = toRgb(a);
  const [br, bg, bb] = toRgb(b);
  const m = (x: number, y: number) => Math.round((x * p + y * (1 - p)) * 255);
  const hex =
    '#' + [m(ar, br), m(ag, bg), m(ab, bb)].map((v) => v.toString(16).padStart(2, '0')).join('');
  return fromHex(hex);
}

describe('marketing surfaces', () => {
  it.each(THEME_IDS)('%s: inverse sections keep every text role readable', (themeId) => {
    const theme = themes[themeId];
    const n = theme.neutrals;
    const t = theme.targets.text;
    expect(contrast(n['text-inverse'], n['bg-inverse'])).toBeGreaterThanOrEqual(t);
    expect(contrast(n['text-inverse-muted'], n['bg-inverse'])).toBeGreaterThanOrEqual(t);
    for (const [name, accent] of accents) {
      const r = resolveTheme(theme, accent);
      for (const intent of INTENT_IDS) {
        const si = r.intents[intent]['strong-inverse'];
        expect(
          contrast(si, n['bg-inverse']),
          `${name} ${intent} strong-inverse`,
        ).toBeGreaterThanOrEqual(t);
      }
      // --mn-gradient-inverse tints the corner with INVERSE_TINT of the accent fill.
      const corner = mix(r.intents.accent.solid, n['bg-inverse'], INVERSE_TINT);
      for (const role of ['text-inverse', 'text-inverse-muted'] as const) {
        expect(
          contrast(n[role], corner),
          `${name} ${role} on inverse gradient`,
        ).toBeGreaterThanOrEqual(t);
      }
      expect(
        contrast(r.intents.accent['strong-inverse'], corner),
        `${name} accent on inverse gradient`,
      ).toBeGreaterThanOrEqual(t);
      // Surfaces inside an inverse section mix up to 9% of text-inverse into bg-inverse.
      const raised = mix(n['text-inverse'], n['bg-inverse'], INVERSE_RAISED);
      for (const fg of [
        n['text-inverse'],
        n['text-inverse-muted'],
        r.intents.accent['strong-inverse'],
      ]) {
        expect(contrast(fg, raised), `${name} text on inverse card`).toBeGreaterThanOrEqual(t);
      }
    }
  });

  it.each(THEME_IDS)(
    '%s: text stays readable on the accent wash and accent gradient',
    (themeId) => {
      const theme = themes[themeId];
      const n = theme.neutrals;
      const t = theme.targets.text;
      for (const [name, accent] of accents) {
        const a = resolveTheme(theme, accent).intents.accent;
        // --mn-gradient-wash runs from accent subtle to canvas.
        for (const role of ['text-default', 'text-muted'] as const) {
          expect(contrast(n[role], a.subtle), `${name} ${role} on wash`).toBeGreaterThanOrEqual(t);
        }
        // --mn-gradient-accent runs from solid to solid-active; on-solid sits on both.
        expect(contrast(a['on-solid'], a.solid)).toBeGreaterThanOrEqual(t);
        expect(
          contrast(a['on-solid'], a['solid-active']),
          `${name} on accent gradient`,
        ).toBeGreaterThanOrEqual(t);
        // Buttons inverted on the accent band: solid-coloured text on an on-solid fill.
        expect(contrast(a.solid, a['on-solid']), `${name} inverted button`).toBeGreaterThanOrEqual(
          t,
        );
        void toChannels;
      }
    },
  );
});

/* ------------------------------------------------------------------ */
/* Categorical data palette (--mn-color-data-1 … -5)                   */
/* ------------------------------------------------------------------ */

type Rgb = readonly [number, number, number];
const linear = (v: number) => (v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4));
const mul = (m: readonly Rgb[], [r, g, b]: Rgb): Rgb =>
  m.map(([x, y, z]) => Math.min(1, Math.max(0, x * r + y * g + z * b))) as unknown as Rgb;

/** Linear sRGB → OKLab (Björn Ottosson), ×100 so ΔE thresholds read as in the dataviz method. */
function oklab([r, g, b]: Rgb): Rgb {
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return [
    100 * (0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s),
    100 * (1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s),
    100 * (0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s),
  ];
}
const deltaE = (a: Rgb, b: Rgb) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]);

/** Machado, Oliveira and Fernandes (2009), severity 1.0, applied in linear RGB. */
const CVD: Record<string, readonly Rgb[]> = {
  normal: [
    [1, 0, 0],
    [0, 1, 0],
    [0, 0, 1],
  ],
  protanopia: [
    [0.152286, 1.052583, -0.204868],
    [0.114503, 0.786281, 0.099216],
    [-0.003882, -0.048116, 1.051998],
  ],
  deuteranopia: [
    [0.367322, 0.860646, -0.227968],
    [0.280085, 0.672501, 0.047413],
    [-0.01182, 0.04294, 0.968881],
  ],
};

describe.each(THEME_IDS)('data palette in %s', (themeId) => {
  const theme = themes[themeId];
  const palette = dataPalette(theme);
  const n = theme.neutrals;

  it('has five slots and no status hue', () => {
    expect(palette).toHaveLength(5);
    expect(DATA_PALETTE_ORDER).not.toContain('red');
    expect(DATA_PALETTE_ORDER).not.toContain('green');
  });

  it("meets the theme's non-text target on every chart background (WCAG 1.4.11)", () => {
    for (const [i, c] of palette.entries()) {
      for (const bg of ['bg-canvas', 'bg-surface', 'bg-raised', 'bg-subtle'] as const) {
        expect(contrast(c, n[bg]), `data-${i + 1} on ${bg}`).toBeGreaterThanOrEqual(
          theme.targets.ui,
        );
      }
    }
  });

  it.each(Object.keys(CVD))('keeps neighbouring slots apart under %s vision', (vision) => {
    // Normal vision: ΔE ≥ 15 (hard floor). Simulated CVD: ΔE ≥ 8 (target).
    const floor = vision === 'normal' ? 15 : 8;
    const lab = palette.map((c) =>
      oklab(mul(CVD[vision]!, toRgb(c).map(linear) as unknown as Rgb)),
    );
    for (let i = 1; i < lab.length; i++) {
      expect(deltaE(lab[i - 1]!, lab[i]!), `data-${i} vs data-${i + 1}`).toBeGreaterThanOrEqual(
        floor,
      );
    }
  });
});
