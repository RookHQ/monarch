import { describe, expect, it } from 'vitest';
import { colorDistance, contrast, mixSrgb } from '../src/color.js';
import { buildProductCss, compatPrimary, productSelectors } from '../src/css.js';
import { productIds } from '../src/generated/products.js';
import { NEUTRAL_ACCENT_ID, neutralAccent } from '../src/neutral.js';
import { brand, defaultAccent } from '../src/primitives.js';
import { INVERSE_TINT, THEME_IDS, resolveTheme, themes } from '../src/themes.js';

describe('neutral accent file', () => {
  const css = buildProductCss(NEUTRAL_ACCENT_ID, neutralAccent);

  it('is scoped like a product accent, by data-mn-product="neutral"', () => {
    for (const id of THEME_IDS) {
      for (const selector of productSelectors(NEUTRAL_ACCENT_ID, id))
        expect(css).toContain(selector);
    }
    expect(css).not.toMatch(/:root|:not\(/);
  });

  it('only carries accent variables (and the legacy --primary bridge)', () => {
    expect(css).not.toMatch(/--mn-color-(bg|text|border|danger|success|warning|info|ai)-/);
    expect(css).toMatch(/--mn-color-accent-solid: /);
    expect(css).toMatch(/--primary: /);
  });

  it('is not a registry product, so the registry cannot claim the name', () => {
    expect(productIds as readonly string[]).not.toContain(NEUTRAL_ACCENT_ID);
  });

  it('carries no Rook brand colour', () => {
    for (const c of Object.values(brand)) {
      expect(colorDistance(neutralAccent, c)).toBeGreaterThanOrEqual(10);
    }
    expect(colorDistance(neutralAccent, defaultAccent)).toBeGreaterThanOrEqual(20);
  });
});

describe.each(THEME_IDS)('neutral accent in %s', (themeId) => {
  const theme = themes[themeId];
  const n = theme.neutrals;
  const t = theme.targets;
  const r = resolveTheme(theme, neutralAccent);
  const a = r.intents.accent;

  it('keeps readable text on every fill', () => {
    for (const role of ['solid', 'solid-hover', 'solid-active'] as const) {
      expect(contrast(a['on-solid'], a[role]), role).toBeGreaterThanOrEqual(t.text);
      expect(contrast(a[role], n['bg-canvas']), `${role} vs canvas`).toBeGreaterThanOrEqual(
        theme.contrast === 'more' ? t.ui : 1.5,
      );
    }
    expect(contrast(a['on-strong'], a.strong)).toBeGreaterThanOrEqual(t.text);
  });

  it('keeps strong text, links and focus rings readable on every surface', () => {
    for (const bg of [
      n['bg-canvas'],
      n['bg-surface'],
      n['bg-raised'],
      n['bg-subtle'],
      n['bg-muted'],
      a.subtle,
      a['subtle-hover'],
    ]) {
      expect(contrast(a.strong, bg)).toBeGreaterThanOrEqual(t.text);
    }
    expect(contrast(a.strong, n['bg-surface'])).toBeGreaterThanOrEqual(3);
  });

  it('keeps accent text readable on inverse sections and the inverse gradient', () => {
    const corner = mixSrgb(a.solid, n['bg-inverse'], INVERSE_TINT);
    expect(contrast(a['strong-inverse'], n['bg-inverse'])).toBeGreaterThanOrEqual(t.text);
    expect(contrast(a['strong-inverse'], corner)).toBeGreaterThanOrEqual(t.text);
    for (const role of ['text-default', 'text-muted'] as const) {
      expect(contrast(n[role], a.subtle), `${role} on wash`).toBeGreaterThanOrEqual(t.text);
    }
  });

  it('gives the legacy --primary a valid fill', () => {
    const p = compatPrimary(r);
    expect(contrast(p.on, p.fill)).toBeGreaterThanOrEqual(t.text);
  });

  it('stays distinguishable from the AI purple and the danger, success and warning colours', () => {
    for (const other of ['ai', 'danger', 'success', 'warning'] as const) {
      expect(colorDistance(a.solid, r.intents[other].solid), other).toBeGreaterThanOrEqual(10);
    }
  });
});
