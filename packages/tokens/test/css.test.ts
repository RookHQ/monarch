import { describe, expect, it } from 'vitest';
import {
  buildCompatCss,
  buildMonarchCss,
  buildProductCss,
  productSelectors,
  themeSelectors,
  varName,
} from '../src/css.js';
import { appearanceScript, resolveAppearance } from '../src/appearance.js';
import { hsl } from '../src/color.js';
import { defaultAccent } from '../src/primitives.js';
import { monarchPreset } from '../src/tailwind.js';

/** Specificity (a,b,c) for the simple selectors Monarch generates. */
function specificity(sel: string): number {
  if (sel.startsWith(':where(')) return 0;
  if (sel === ':root') return 10;
  const attrs = (sel.match(/\[[^\]]+\]/g) ?? []).length;
  const classes = (sel.match(/\.[a-z][\w-]*/g) ?? []).length;
  return (attrs + classes) * 10;
}

describe('selectors', () => {
  it('orders specificity light < dark <= hc-light < hc-dark for products', () => {
    const s = (t: Parameters<typeof productSelectors>[1]) =>
      Math.min(...productSelectors('x', t).map(specificity));
    expect(s('light')).toBeLessThan(s('dark'));
    expect(s('dark')).toBeLessThanOrEqual(s('hc-light'));
    expect(s('hc-light')).toBeLessThan(s('hc-dark'));
  });

  it('product accents beat their theme block in every theme, whatever the load order', () => {
    for (const t of ['light', 'dark', 'hc-light', 'hc-dark'] as const) {
      const theme = Math.max(...themeSelectors(t).map(specificity));
      const product = Math.min(...productSelectors('x', t).map(specificity));
      expect(product, t).toBeGreaterThan(theme);
    }
  });

  it('supports the existing .dark class and the data attribute', () => {
    expect(themeSelectors('dark')).toEqual(['.dark', '[data-mn-theme="dark"]']);
  });

  it('escapes fractional keys in variable names', () => {
    expect(varName('space', 0.5)).toBe('--mn-space-0-5');
  });
});

describe('generated css', () => {
  const css = buildMonarchCss(defaultAccent);

  it('is deterministic', () => {
    expect(buildMonarchCss(defaultAccent)).toBe(css);
  });

  it('wraps everything in the monarch.tokens layer so unlayered app CSS can still win', () => {
    expect(css).toMatch(
      /^\/\*![^\n]*\*\/\n@layer monarch\.tokens, monarch\.base, monarch\.components;\n\n@layer monarch\.tokens \{/,
    );
  });

  it('collapses motion for reduced-motion users', () => {
    expect(css).toContain('@media (prefers-reduced-motion: reduce)');
    expect(css).toMatch(/--mn-duration-base: 0ms/);
  });

  it('emits every theme block', () => {
    for (const sel of [
      ':root',
      '.dark',
      '[data-mn-contrast="more"]',
      '.dark[data-mn-contrast="more"]',
    ]) {
      expect(css).toContain(sel);
    }
  });

  it('product files only carry accent variables', () => {
    const p = buildProductCss('tickets', hsl(354, 71, 52));
    expect(p).toContain('[data-mn-product="tickets"]');
    expect(p).not.toMatch(/--mn-color-bg-/);
    expect(p).toMatch(/--primary: /);
  });

  it('legacy bridge keeps shadcn channel format', () => {
    expect(buildCompatCss(defaultAccent)).toMatch(
      /--background: \d+(\.\d)? \d+(\.\d)?% \d+(\.\d)?%;/,
    );
  });
});

describe('appearance', () => {
  it('resolves system preferences', () => {
    expect(
      resolveAppearance('system', 'system', { prefersDark: true, prefersMoreContrast: false }),
    ).toEqual({ theme: 'dark', contrast: 'standard' });
    expect(
      resolveAppearance('light', 'more', { prefersDark: true, prefersMoreContrast: false }),
    ).toEqual({ theme: 'light', contrast: 'more' });
  });

  it('rejects unsafe input to the inline script', () => {
    expect(() => appearanceScript({ product: '"</script>' })).toThrow();
    expect(() => appearanceScript({ themeCookie: 'a;b' })).toThrow();
    expect(appearanceScript({ product: 'tickets' })).toContain('"p":"tickets"');
  });
});

describe('tailwind preset', () => {
  it('keeps the RookRail variant and both colour namespaces', () => {
    const colors = monarchPreset.theme.extend.colors;
    expect(colors.mn.accent.solid).toContain('--mn-color-accent-solid');
    expect(colors.primary.DEFAULT).toBe('hsl(var(--primary) / <alpha-value>)');
    const variants: string[] = [];
    monarchPreset.plugins[0]!({ addVariant: (name) => void variants.push(name) });
    expect(variants).toEqual(['rail-expanded']);
  });
});
