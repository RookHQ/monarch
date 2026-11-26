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
 * The optional `ai` colour replaces the Monarch intelligence purple for the
 * same region. It runs through the same engine, so it gets the same contrast
 * guarantees, and it is checked for distance from the accent and the status
 * colours (ADR 0013): AI content must not look like a link or an error. Rook
 * products never pass it; they keep the shared purple so AI looks the same in
 * every Rook app.
 *
 * Everything here is a pure function of its arguments: no DOM, no globals, no
 * caching. It runs the same on the server, in a worker or in the browser.
 */
import { colorDistance, fromHex, toChannels, toCss, toHex, type Hsl } from './color.js';
import { compatPrimary, INTENT_ROLES, themeSelectors, LAYER } from './css.js';
import { defaultAccent, statusBase } from './primitives.js';
import {
  THEME_IDS,
  deriveIntent,
  resolveTheme,
  themes,
  type IntentId,
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
  /** The derived AI roles for this theme, when an `ai` colour was given and valid. */
  readonly ai?: IntentScale;
  /** What `deriveIntent` changed in the AI colour to meet contrast, when there is one. */
  readonly aiReport?: IntentReport;
  /**
   * `--mn-color-accent-*`, `--mn-color-text-link` and `--mn-color-focus-ring`
   * for this theme, plus the `--mn-color-ai-*` roles, `--mn-color-ai-thinking`,
   * `--mn-color-ai-citation` and `--mn-gradient-ai` when an `ai` colour is set.
   */
  readonly vars: BrandThemeVars;
}

/** The colour the AI colour is measured against. */
export type BrandAiNeighbour = Exclude<IntentId, 'ai'>;

export interface BrandAiCheck {
  /** The value passed as `options.ai`, unchanged. */
  readonly input: string;
  /** The AI colour used, as lowercase `#rrggbb`, or null when `input` was invalid. */
  readonly color: string | null;
  /** False when `input` was not a hex colour; the Monarch purple is kept. */
  readonly valid: boolean;
  /**
   * The smallest perceptual distance (OKLab ΔE ×100) between the AI fill and
   * the accent or a status fill, in any theme, and which colour and theme it
   * was. Null when `valid` is false.
   */
  readonly closest: {
    readonly distance: number;
    readonly to: BrandAiNeighbour;
    readonly theme: ThemeId;
  } | null;
  /** True when `closest.distance` is at least `AI_MIN_DISTANCE`. */
  readonly distinct: boolean;
  /** Plain-language notes for whoever picked the colour. Empty when all is well. */
  readonly warnings: readonly string[];
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
  /** The AI colour check, when `options.ai` was passed (even if invalid). */
  readonly ai: BrandAiCheck | null;
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
  /**
   * A custom AI colour (`#rgb` or `#rrggbb`) for libraries and products
   * outside Rook. Derived for all four themes with the accent's contrast
   * guarantees, and checked for distance from the accent and status colours:
   * `theme.ai.warnings` says when it is too close. An invalid value is
   * ignored and the Monarch purple is kept. Default: none (Monarch purple).
   */
  readonly ai?: string;
}

/**
 * The smallest OKLab distance (×100) the AI fill should keep from the accent
 * and status fills. The Monarch purple is held to the same floor against
 * every Rook product accent (ADR 0013).
 */
export const AI_MIN_DISTANCE = 10;

const AI_NEIGHBOURS: readonly BrandAiNeighbour[] = [
  'accent',
  'danger',
  'success',
  'warning',
  'info',
];

const NEIGHBOUR_NAMES: Record<BrandAiNeighbour, string> = {
  accent: 'the brand colour',
  danger: 'the error red',
  success: 'the success green',
  warning: 'the warning amber',
  info: 'the info blue',
};

function aiVars(theme: ThemeId, ai: IntentScale): BrandThemeVars {
  const vars: Record<`--${string}`, string> = {};
  for (const role of INTENT_ROLES) vars[`--mn-color-ai-${role}`] = toCss(ai[role]);
  // Declared on :root as var(--mn-color-ai-*), which resolves where it is
  // declared, so a re-themed region must restate them (as for links).
  vars['--mn-color-ai-thinking'] = toCss(ai.subtle);
  vars['--mn-color-ai-citation'] = toCss(ai.strong);
  vars['--mn-gradient-ai'] =
    themes[theme].contrast === 'more'
      ? toCss(ai.solid)
      : `linear-gradient(135deg, ${toCss(ai.solid)} 0%, color-mix(in srgb, ${toCss(ai.solid)} 70%, var(--mn-color-info-solid)) 100%)`;
  return vars;
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
  const aiBase = options.ai === undefined ? null : parseHexColor(options.ai);
  const modes = {} as Record<ThemeId, BrandThemeMode>;
  let closest: BrandAiCheck['closest'] = null;
  for (const id of THEME_IDS) {
    const { scale, report } = deriveIntent(base, themes[id]);
    const vars = accentVars(id, base, scale, compat);
    if (!aiBase) {
      modes[id] = { accent: scale, report, vars };
      continue;
    }
    const ai = deriveIntent(aiBase, themes[id]);
    for (const to of AI_NEIGHBOURS) {
      const fill =
        to === 'accent' ? scale.solid : deriveIntent(statusBase[to], themes[id]).scale.solid;
      const distance = colorDistance(ai.scale.solid, fill);
      if (!closest || distance < closest.distance) closest = { distance, to, theme: id };
    }
    modes[id] = {
      accent: scale,
      report,
      ai: ai.scale,
      aiReport: ai.report,
      vars: { ...vars, ...aiVars(id, ai.scale) },
    };
  }
  return {
    input: brand,
    brand: toHex(base),
    valid: parsed !== null,
    modes,
    ai: options.ai === undefined ? null : aiCheck(options.ai, aiBase, closest),
  };
}

function aiCheck(input: string, base: Hsl | null, closest: BrandAiCheck['closest']): BrandAiCheck {
  if (!base) {
    return {
      input,
      color: null,
      valid: false,
      closest: null,
      distinct: false,
      warnings: [`"${input}" is not a hex colour, so the Monarch AI purple is used.`],
    };
  }
  const distinct = closest !== null && closest.distance >= AI_MIN_DISTANCE;
  const warnings =
    distinct || !closest
      ? []
      : [
          `The AI colour is too close to ${NEIGHBOUR_NAMES[closest.to]} in the ${closest.theme} theme ` +
            `(distance ${closest.distance.toFixed(1)}, needs ${AI_MIN_DISTANCE}). People may not tell ` +
            `AI content apart from it. Pick a hue further away; the AI label and icon still show.`,
        ];
  return { input, color: toHex(base), valid: true, closest, distinct, warnings };
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
  const ai = theme.ai?.color ? `, AI ${theme.ai.color}` : '';
  const comment = `/* Monarch brand theme ${theme.brand}${theme.valid ? '' : ' (fallback)'}${ai} */`;
  return options.layer === false
    ? `${comment}\n${blocks.join('\n\n')}\n`
    : `${comment}\n@layer ${LAYER} {\n${blocks.join('\n\n')}\n}\n`;
}
