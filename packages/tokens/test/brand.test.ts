import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { contrast, fromHex, hsl, toHex, type Hsl } from '../src/color.js';
import { INTENT_ROLES } from '../src/css.js';
import { defaultAccent } from '../src/primitives.js';
import { THEME_IDS, deriveIntent, themes } from '../src/themes.js';
import { brandThemeCss, deriveTheme, parseHexColor } from '../src/brand.js';
import { products, productIds } from '../src/generated/products.js';

/** Every hue at every saturation and lightness band, plus the classic edge cases. */
function extremeInputs(): string[] {
  const out = new Set<string>([
    '#ffffff',
    '#000000',
    '#fefefe',
    '#010101',
    '#808080',
    '#ffff00',
    '#ffffe0',
    '#00ff00',
    '#00ffff',
    '#0000ff',
    '#000080',
    '#ff0000',
    '#ff00ff',
    '#f4b400',
    '#dc2e3e',
    '#0096d4',
    '#777777',
    '#767676',
    '#1a1a1a',
    '#e5e5e5',
  ]);
  for (let h = 0; h < 360; h += 15)
    for (const s of [0, 6, 25, 60, 100])
      for (const l of [0, 3, 10, 25, 40, 50, 60, 75, 90, 97, 100]) out.add(toHex(hsl(h, s, l)));
  return [...out];
}

const inputs = extremeInputs();

describe('parseHexColor', () => {
  it('accepts #rgb, #rrggbb, bare hex, any case and surrounding space', () => {
    expect(toHex(parseHexColor('#0af')!)).toBe('#00aaff');
    expect(toHex(parseHexColor('0AF')!)).toBe('#00aaff');
    expect(toHex(parseHexColor('  #DC2E3E ')!)).toBe('#dc2e3e');
    expect(toHex(parseHexColor('dc2e3e')!)).toBe('#dc2e3e');
  });

  it.each([
    '',
    '#',
    'red',
    '#12',
    '#1234',
    '#12345',
    '#1234567',
    '#dc2e3e80',
    'rgb(0,0,0)',
    '#ggg',
    null,
    undefined,
  ])('rejects %j', (value) => {
    expect(parseHexColor(value as string)).toBeNull();
  });
});

describe('deriveTheme', () => {
  it('is pure: same input, same output, and the input is reported back', () => {
    const a = deriveTheme('#6366F1');
    const b = deriveTheme('#6366F1');
    expect(a).toEqual(b);
    expect(a.input).toBe('#6366F1');
    expect(a.brand).toBe('#6366f1');
    expect(a.valid).toBe(true);
  });

  it('produces one accent set per Monarch theme with every intent role', () => {
    const t = deriveTheme('#0096d4');
    expect(Object.keys(t.modes).sort()).toEqual([...THEME_IDS].sort());
    for (const id of THEME_IDS) {
      for (const role of INTENT_ROLES) {
        expect(t.modes[id].accent[role], `${id} ${role}`).toBeDefined();
        expect(t.modes[id].vars[`--mn-color-accent-${role}`]).toMatch(/^hsl\(/);
      }
      expect(t.modes[id].vars['--mn-color-text-link']).toBe(
        t.modes[id].vars['--mn-color-accent-strong'],
      );
    }
    // High contrast keeps the ink focus ring the Monarch themes use.
    expect(t.modes['hc-light'].vars['--mn-color-focus-ring']).toBe('var(--mn-color-text-default)');
    expect(t.modes.light.vars['--mn-color-focus-ring']).toBe(
      t.modes.light.vars['--mn-color-accent-strong'],
    );
  });

  it('matches deriveIntent exactly, so tenant colours get the product-accent guarantees', () => {
    const base = fromHex('#28af4c');
    const t = deriveTheme('#28af4c');
    for (const id of THEME_IDS) {
      expect(t.modes[id].accent).toEqual(deriveIntent(base, themes[id]).scale);
    }
  });

  it('falls back instead of throwing on an invalid colour', () => {
    const t = deriveTheme('not-a-colour');
    expect(t.valid).toBe(false);
    expect(t.brand).toBe(toHex(defaultAccent));
    const custom = deriveTheme('', { fallback: '#0096d4' });
    expect(custom.valid).toBe(false);
    expect(custom.brand).toBe('#0096d4');
    // An invalid fallback falls back to the Monarch default.
    expect(deriveTheme('x', { fallback: 'y' }).brand).toBe(toHex(defaultAccent));
  });

  it('adds the legacy bridge variables only when asked', () => {
    expect(deriveTheme('#0096d4').modes.light.vars['--primary']).toBeUndefined();
    const v = deriveTheme('#0096d4', { compat: true }).modes.dark.vars;
    for (const k of ['--primary', '--primary-foreground', '--ring', '--sidebar-primary']) {
      expect(v[k as `--${string}`], k).toMatch(/^\d+(\.\d)? \d+(\.\d)?% \d+(\.\d)?%$/);
    }
  });

  it('matches every registered product accent (the build uses the same engine)', () => {
    for (const id of productIds) {
      const accent: Hsl = products[id].accent;
      const t = deriveTheme(toHex(accent));
      for (const themeId of THEME_IDS) {
        const direct = deriveIntent(fromHex(toHex(accent)), themes[themeId]).scale;
        expect(t.modes[themeId].accent, `${id} ${themeId}`).toEqual(direct);
      }
    }
  });
});

describe.each(THEME_IDS)('deriveTheme contrast guarantees in %s', (themeId) => {
  const theme = themes[themeId];
  const n = theme.neutrals;
  const text = theme.targets.text;
  const hc = theme.contrast === 'more';

  it(`holds for ${inputs.length} extreme inputs (${hc ? '7' : '4.5'}:1 text)`, () => {
    expect(text).toBe(hc ? 7 : 4.5);
    const failures: string[] = [];
    const check = (ok: boolean, msg: string) => {
      if (!ok) failures.push(msg);
    };
    for (const hex of inputs) {
      const a = deriveTheme(hex).modes[themeId].accent;
      for (const fill of ['solid', 'solid-hover', 'solid-active'] as const) {
        check(contrast(a['on-solid'], a[fill]) >= text, `${hex} on-solid/${fill}`);
        check(
          contrast(a[fill], n['bg-canvas']) >= (hc ? theme.targets.ui : 1.5),
          `${hex} ${fill} vs canvas`,
        );
      }
      check(contrast(a['on-strong'], a.strong) >= text, `${hex} on-strong`);
      for (const bg of [
        n['bg-canvas'],
        n['bg-surface'],
        n['bg-raised'],
        n['bg-subtle'],
        n['bg-muted'],
        a.subtle,
        a['subtle-hover'],
      ]) {
        check(contrast(a.strong, bg) >= text, `${hex} strong on ${toHex(bg)}`);
      }
      // Body text stays readable on the accent tint (callouts, selected rows).
      check(contrast(n['text-default'], a.subtle) >= text, `${hex} text on subtle`);
      // Focus ring (accent strong) against surfaces, WCAG 2.4.11 / 1.4.11.
      check(contrast(a.strong, n['bg-surface']) >= 3, `${hex} focus ring`);
    }
    expect(failures).toEqual([]);
  });
});

describe('brandThemeCss', () => {
  const t = deriveTheme('#6366f1');

  it('emits one block per theme on the Monarch selectors, in a layer by default', () => {
    const css = brandThemeCss(t, { selector: '.booking' });
    expect(css).toContain('@layer monarch.tokens {');
    expect(css).toMatch(/^ {2}\.booking \{$/m);
    expect(css).toContain('.dark .booking');
    expect(css).toContain('[data-mn-theme="dark"].booking');
    expect(css).toContain('[data-mn-contrast="more"] .booking');
    expect(css).toContain('.dark[data-mn-contrast="more"] .booking');
    for (const id of THEME_IDS) {
      expect(css).toContain(
        `--mn-color-accent-solid: ${t.modes[id].vars['--mn-color-accent-solid']};`,
      );
    }
    // Light, dark, hc-light, hc-dark: the cascade order the tokens use.
    const order = [
      '  .booking {',
      '.dark .booking',
      '[data-mn-contrast="more"] .booking',
      '.dark[data-mn-contrast="more"] .booking',
    ].map((s) => css.indexOf(s));
    expect(order).toEqual([...order].sort((x, y) => x - y));
  });

  it('defaults to :root and can skip the layer', () => {
    const css = brandThemeCss(t, { layer: false });
    expect(css).not.toContain('@layer');
    expect(css).toMatch(/^:root \{$/m);
    expect(css).toContain('.dark:root');
  });

  it('marks a fallback theme in its comment', () => {
    expect(brandThemeCss(deriveTheme('nope'))).toContain('(fallback)');
  });
});

describe('no runtime DOM dependency', () => {
  it('brand.ts never touches window, document or other browser globals', () => {
    const src = readFileSync(new URL('../src/brand.ts', import.meta.url), 'utf8')
      // Ignore the doc comment examples.
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .replace(/\/\/.*$/gm, '');
    expect(src).not.toMatch(
      /\b(window|document|globalThis|navigator|localStorage|getComputedStyle|matchMedia)\b/,
    );
  });

  it('runs in a plain Node environment', () => {
    expect(typeof (globalThis as { document?: unknown }).document).toBe('undefined');
    expect(() => deriveTheme('#123456')).not.toThrow();
  });
});
