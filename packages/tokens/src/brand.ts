/**
 * Brand theme engine: one brand colour in, a complete accent token set out.
 *
 * Products that let a customer pick a colour (booking pages, support portals,
 * community sites, careers and status pages) need contrast maths of their own.
 * `deriveTheme` provides it once: it runs the same
 * `deriveIntent` that builds every Monarch product accent, for all four
 * themes, so a tenant colour gets exactly the guarantees a Rook product
 * colour gets (ADR 0003):
 *
 * - `on-solid` on `solid`, `solid-hover` and `solid-active`: ≥ 4.5:1 (7:1 in
 *   the high-contrast themes);
 * - `strong` (links, icons, focus rings) on canvas, surface, subtle, muted and
 *   the accent's own subtle tints: ≥ 4.5:1 (7:1 high contrast);
 * - the fill stays distinguishable from the canvas (≥ 1.5:1, 4.5:1 high contrast).
 *
 * Everything here is a pure function of its arguments: no DOM, no globals, no
 * caching. It runs the same on the server, in a worker or in the browser.
 */
import { fromHex, toChannels, toCss, toHex, type Hsl } from './color.js';
import { compatPrimary, INTENT_ROLES, themeSelectors, LAYER } from './css.js';
import { defaultAccent } from './primitives.js';
import {
  THEME_IDS,
  deriveIntent,
  resolveTheme,
  themes,
  type IntentReport,
  type IntentScale,
  type ThemeId,
} from './themes.js';

/** CSS custom properties, ready for a `style` object or a stylesheet block. */
export type BrandThemeVars = Readonly<Record<`--${string}`, string>>;

export interface BrandThemeMode {
  /** The derived accent roles for this theme (HSL). */
  readonly accent: IntentScale;
  /** What `deriveIntent` changed to meet contrast, in plain language, and the measured ratios. */
  readonly report: IntentReport;
  /** `--mn-color-accent-*`, `--mn-color-text-link` and `--mn-color-focus-ring` for this theme. */
  readonly vars: BrandThemeVars;
}

export interface BrandTheme {
  /** The value that was passed in, unchanged. */
  readonly input: string;
  /** The colour the theme was derived from, as lowercase `#rrggbb`. */
  readonly brand: string;
  /** False when `input` was not a hex colour and the fallback was used instead. */
  readonly valid: boolean;
  /** One accent set per Monarch theme. */
  readonly modes: Readonly<Record<ThemeId, BrandThemeMode>>;
}

export interface DeriveThemeOptions {
  /**
   * Used when the brand colour is missing or not a hex colour, so a bad value
   * saved by a customer never breaks the page. Default: the Monarch default
   * accent (Rook Red).
   */
  readonly fallback?: string;
  /**
   * Also emit the legacy shadcn-style variables (`--primary`,
   * `--primary-foreground`, `--ring`, `--sidebar-primary*`) for apps still on
   * the compat bridge. Default false.
   */
  readonly compat?: boolean;
}

/**
 * Parses `#rgb`, `#rrggbb`, `rgb` or `rrggbb` (any case, surrounding spaces
 * allowed). Returns null for anything else, including named colours and
 * `#rrggbbaa`: a brand colour must be opaque for contrast to mean anything.
 */
export function parseHexColor(input: string | null | undefined): Hsl | null {
  if (typeof input !== 'string') return null;
  const v = input.trim().replace(/^#/, '');
  const six = /^[0-9a-f]{3}$/i.test(v) ? v.replace(/./g, (c) => c + c) : v;
  if (!/^[0-9a-f]{6}$/i.test(six)) return null;
  return fromHex(six);
}

function accentVars(
  theme: ThemeId,
  base: Hsl,
  accent: IntentScale,
  compat: boolean,
): BrandThemeVars {
  const vars: Record<`--${string}`, string> = {};
  for (const role of INTENT_ROLES) vars[`--mn-color-accent-${role}`] = toCss(accent[role]);
  // These two are declared on :root as var(--mn-color-accent-strong). A custom
  // property's var() is resolved where it is declared, so re-theming a region
  // must restate them, or links and focus rings keep the page accent.
  vars['--mn-color-text-link'] = toCss(accent.strong);
  vars['--mn-color-focus-ring'] =
    themes[theme].contrast === 'more' ? 'var(--mn-color-text-default)' : toCss(accent.strong);
  if (compat) {
    const p = compatPrimary(resolveTheme(themes[theme], base));
    vars['--primary'] = toChannels(p.fill);
    vars['--primary-foreground'] = toChannels(p.on);
    vars['--ring'] = toChannels(accent.strong);
    vars['--sidebar-primary'] = toChannels(p.fill);
    vars['--sidebar-primary-foreground'] = toChannels(p.on);
    vars['--sidebar-ring'] = toChannels(accent.strong);
  }
  return vars;
}

/**
 * Derives a full accent token set from one brand colour, for the light, dark
 * and both high-contrast themes. Pure: the same input always gives the same
 * output, and nothing touches the DOM.
 *
 * ```ts
 * const theme = deriveTheme(workspace.brandColor);
 * // Inline, for a page that is always light:
 * <div style={theme.modes.light.vars}>…</div>
 * // Or a stylesheet that follows the user's theme:
 * <style>{brandThemeCss(theme, { selector: '.booking-page' })}</style>
 * ```
 */
export function deriveTheme(brand: string, options: DeriveThemeOptions = {}): BrandTheme {
  const parsed = parseHexColor(brand);
  const fallback = parseHexColor(options.fallback) ?? defaultAccent;
  const base = parsed ?? fallback;
  const compat = options.compat ?? false;
  const modes = {} as Record<ThemeId, BrandThemeMode>;
  for (const id of THEME_IDS) {
    const { scale, report } = deriveIntent(base, themes[id]);
    modes[id] = { accent: scale, report, vars: accentVars(id, base, scale, compat) };
  }
  return { input: brand, brand: toHex(base), valid: parsed !== null, modes };
}

export interface BrandThemeCssOptions {
  /**
   * The element the theme applies to. Default `:root` (the whole page). Use a
   * class or attribute to theme one region, such as a booking widget.
   */
  readonly selector?: string;
  /**
   * Wrap the rules in `@layer monarch.tokens` so they sit with the other
   * Monarch tokens and any unlayered app rule still wins. Default true.
   */
  readonly layer?: boolean;
}

/**
 * The theme as CSS text: one block per theme, keyed to the same selectors as
 * the Monarch stylesheets (`.dark` / `data-mn-theme="dark"` and
 * `data-mn-contrast="more"` on an ancestor or on the element itself), so it
 * follows the user's theme with no script. Pure string output.
 */
export function brandThemeCss(theme: BrandTheme, options: BrandThemeCssOptions = {}): string {
  const target = options.selector?.trim() || ':root';
  const selectorsFor = (id: ThemeId): string[] =>
    id === 'light'
      ? [target]
      : themeSelectors(id).flatMap((q) => [`${q}${target}`, `${q} ${target}`]);
  const indent = options.layer === false ? '' : '  ';
  const blocks = THEME_IDS.map((id) => {
    const body = Object.entries(theme.modes[id].vars)
      .map(([k, v]) => `${indent}  ${k}: ${v};`)
      .join('\n');
    return `${indent}${selectorsFor(id).join(`,\n${indent}`)} {\n${body}\n${indent}}`;
  });
  const comment = `/* Monarch brand theme ${theme.brand}${theme.valid ? '' : ' (fallback)'} */`;
  return options.layer === false
    ? `${comment}\n${blocks.join('\n\n')}\n`
    : `${comment}\n@layer ${LAYER} {\n${blocks.join('\n\n')}\n}\n`;
}
