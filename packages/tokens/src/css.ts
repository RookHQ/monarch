/**
 * CSS generation. Pure functions, so output is reproducible and testable.
 *
 * Selector contract (documented in docs/guides/theming.md):
 *   theme     <html class="dark"> or <html data-mn-theme="dark|light">
 *   contrast  <html data-mn-contrast="more">
 *   product   <html data-mn-product="tickets">   (from products/<id>.css)
 *
 * Theme blocks are emitted in the order light, dark, hc-light, hc-dark. Light
 * uses :where() (0,0,0); dark and hc-light are (0,1,0) and rely on source
 * order between themselves; hc-dark combines both attributes (0,2,0). Product
 * blocks add [data-mn-product] to each theme selector, so a product + theme
 * combination always beats the theme alone whichever file loads first
 * (tested in test/css.test.ts).
 */
import { ensureContrast, toChannels, toCss, type Hsl } from './color.js';
import {
  breakpoint,
  display,
  lead,
  section,
  width,
  controlHeight,
  duration,
  easing,
  fontFamily,
  fontSize,
  fontWeight,
  layout,
  letterSpacing,
  lineHeight,
  radius,
  space,
  spectrum,
  zIndex,
  type Step,
} from './primitives.js';
import {
  INTENT_IDS,
  THEME_IDS,
  resolveTheme,
  themes,
  type IntentRole,
  type IntentScale,
  type NeutralRoles,
  type ResolvedTheme,
  type ThemeDefinition,
  type ThemeId,
} from './themes.js';

export const LAYER = 'monarch.tokens';
/** Declared in every Monarch stylesheet so the order holds whichever loads first. */
export const LAYER_ORDER = '@layer monarch.tokens, monarch.base, monarch.components;';

const DARK = ['.dark', '[data-mn-theme="dark"]'] as const;
const HC = ['[data-mn-contrast="more"]'] as const;

/** Root-level selectors for each theme. */
export function themeSelectors(id: ThemeId): string[] {
  switch (id) {
    case 'light':
      // Zero specificity, so any product accent wins whatever the CSS load
      // order (bundlers may place product files before monarch.css).
      return [':where(:root, [data-mn-theme="light"])'];
    case 'dark':
      return [...DARK];
    case 'hc-light':
      return [...HC];
    case 'hc-dark':
      return DARK.flatMap((d) => HC.map((h) => `${d}${h}`));
  }
}

/** Selectors for a product accent in each theme (same element or descendant). */
export function productSelectors(productId: string, id: ThemeId): string[] {
  const p = `[data-mn-product="${productId}"]`;
  if (id === 'light') return [p];
  return themeSelectors(id).flatMap((q) => [`${q}${p}`, `${q} ${p}`]);
}

/** CSS custom property name for a key, e.g. space `0.5` → `--mn-space-0-5`. */
export function varName(group: string, key: string | number): string {
  return `--mn-${group}-${String(key).replace('.', '-')}`;
}

type Decls = ReadonlyArray<readonly [string, string]>;

function block(selectors: readonly string[], decls: Decls, indent = '  '): string {
  const body = decls.map(([k, v]) => `${indent}  ${k}: ${v};`).join('\n');
  return `${indent}${selectors.join(',\n' + indent)} {\n${body}\n${indent}}`;
}

function entries<T extends Record<string, string>>(group: string, obj: T): Decls {
  return Object.entries(obj).map(([k, v]) => [varName(group, k), v] as const);
}

/** Theme-independent tokens. */
export function staticDecls(): Decls {
  const spectrumDecls: [string, string][] = [];
  for (const [name, ramp] of Object.entries(spectrum)) {
    spectrumDecls.push([`--mn-color-spectrum-${name}`, toCss(ramp[500])]);
  }
  return [
    ['--mn-font-sans', fontFamily.sans],
    ['--mn-font-mono', fontFamily.mono],
    ...entries('text', fontSize),
    ...entries('leading', lineHeight),
    ...entries('weight', fontWeight),
    ...entries('tracking', letterSpacing),
    ...Object.entries(space).map(([k, v]) => [varName('space', k), v] as const),
    ...entries('radius', radius),
    ...entries('control', controlHeight),
    ...entries('duration', duration),
    ...entries('ease', easing),
    ...entries('breakpoint', breakpoint),
    ...entries('z', zIndex),
    ['--mn-layout-rail-collapsed', layout.railCollapsed],
    ['--mn-layout-rail-expanded', layout.railExpanded],
    ['--mn-layout-content-max', layout.contentMax],
    ['--mn-layout-reading-max', layout.readingMax],
    ['--mn-layout-gutter', layout.gutter],
    ['--mn-layout-gutter-lg', layout.gutterLg],
    ...Object.entries(display).flatMap(([k, d]) => [
      [`--mn-display-${k}`, d.size] as const,
      [`--mn-display-${k}-leading`, d.leading] as const,
      [`--mn-display-${k}-tracking`, d.tracking] as const,
      [`--mn-display-${k}-weight`, d.weight] as const,
    ]),
    ['--mn-lead', lead.size],
    ['--mn-lead-leading', lead.leading],
    ...entries('section', section),
    ...entries('width', width),
    ...spectrumDecls,
    // Derived gradients: they reference semantic colours, so every theme and
    // product accent gets its own version with no hand-picked values.
    [
      '--mn-gradient-wash',
      'radial-gradient(120% 70% at 50% 0%, var(--mn-color-accent-subtle) 0%, var(--mn-color-bg-canvas) 72%)',
    ],
    [
      '--mn-gradient-accent',
      'linear-gradient(135deg, var(--mn-color-accent-solid) 0%, var(--mn-color-accent-solid-active) 100%)',
    ],
    [
      '--mn-gradient-inverse',
      'radial-gradient(55% 80% at 100% 0%, color-mix(in srgb, var(--mn-color-accent-solid) 12%, var(--mn-color-bg-inverse)) 0%, var(--mn-color-bg-inverse) 100%)',
    ],
    [
      '--mn-gradient-spectrum',
      `linear-gradient(120deg, ${toCss(spectrum.blue[500])}, ${toCss(spectrum.violet[500])} 30%, ${toCss(spectrum.purple[500])} 55%, ${toCss(spectrum.pink[500])} 78%, ${toCss(spectrum.orange[500])})`,
    ],
  ];
}

export const NEUTRAL_ROLES = [
  'bg-canvas',
  'bg-surface',
  'bg-raised',
  'bg-subtle',
  'bg-muted',
  'bg-emphasis',
  'bg-inverse',
  'text-default',
  'text-muted',
  'text-subtle',
  'text-disabled',
  'text-inverse',
  'text-inverse-muted',
  'border-subtle',
  'border-default',
  'border-strong',
  'border-input',
  'border-inverse',
] as const satisfies readonly (keyof NeutralRoles)[];

export const INTENT_ROLES: readonly IntentRole[] = [
  'solid',
  'solid-hover',
  'solid-active',
  'on-solid',
  'subtle',
  'subtle-hover',
  'strong',
  'on-strong',
  'border',
  'strong-inverse',
];

function intentDecls(intent: string, scale: IntentScale): Decls {
  return INTENT_ROLES.map((role) => [`--mn-color-${intent}-${role}`, toCss(scale[role])] as const);
}

/**
 * Categorical data-visualisation palette (`--mn-color-data-1` … `-5`): the
 * series colours of every Monarch chart, assigned in this fixed order.
 *
 * Each slot starts at a hand-picked spectrum step per scheme (chosen so
 * neighbours stay apart for protanopia, deuteranopia and normal vision, see
 * docs/adr/0010-charts-rendering-and-colour.md), then moves just far enough to
 * meet the theme's non-text target (3:1, 4.5:1 in high contrast) on every
 * chart background. Status hues (red, green, amber) are deliberately absent:
 * they mean danger, success and warning, never "series 4".
 */
export const DATA_PALETTE_ORDER = ['blue', 'orange', 'violet', 'pink', 'purple'] as const;
const DATA_STEPS: Readonly<Record<'light' | 'dark', readonly Step[]>> = {
  light: [600, 600, 600, 600, 600],
  dark: [500, 700, 400, 500, 500],
};

/** The resolved data palette for one theme, in slot order. Tested in test/contrast.test.ts. */
export function dataPalette(theme: ThemeDefinition): Hsl[] {
  const n = theme.neutrals;
  const against = [n['bg-canvas'], n['bg-surface'], n['bg-raised'], n['bg-subtle']];
  const steps = DATA_STEPS[theme.scheme];
  return DATA_PALETTE_ORDER.map((hue, i) =>
    ensureContrast(
      spectrum[hue][steps[i] ?? 600],
      against,
      theme.targets.ui,
      theme.scheme === 'dark' ? 'lighten' : 'darken',
    ),
  );
}

function dataDecls(theme: ThemeDefinition): Decls {
  return dataPalette(theme).map((c, i) => [`--mn-color-data-${i + 1}`, toCss(c)] as const);
}

export function themeDecls(resolved: ResolvedTheme): Decls {
  const { theme } = resolved;
  const n = theme.neutrals;
  return [
    ['color-scheme', theme.scheme],
    ...NEUTRAL_ROLES.map((r) => [`--mn-color-${r}`, toCss(n[r])] as const),
    ...INTENT_IDS.flatMap((id) => intentDecls(id, resolved.intents[id])),
    ['--mn-color-text-link', 'var(--mn-color-accent-strong)'],
    ...aiDecls(theme),
    [
      '--mn-color-focus-ring',
      theme.contrast === 'more' ? 'var(--mn-color-text-default)' : 'var(--mn-color-accent-strong)',
    ],
    ['--mn-focus-ring-width', theme.contrast === 'more' ? '3px' : '2px'],
    ['--mn-color-overlay-scrim', theme.scrim],
    ...(['xs', 'sm', 'md', 'lg', 'xl'] as const).map(
      (k) => [`--mn-shadow-${k}`, theme.shadow[k]] as const,
    ),
    ...dataDecls(theme),
  ];
}

/**
 * AI roles built on the `ai` intent and the status intents, so they inherit
 * their contrast guarantees (ADR 0013). The gradient is decorative only and is
 * a solid colour in the high-contrast themes.
 */
function aiDecls(theme: ThemeDefinition): Decls {
  const hc = theme.contrast === 'more';
  return [
    ['--mn-color-ai-thinking', 'var(--mn-color-ai-subtle)'],
    ['--mn-color-ai-tool', 'var(--mn-color-info-strong)'],
    ['--mn-color-ai-citation', 'var(--mn-color-ai-strong)'],
    ['--mn-color-ai-diff-add', 'var(--mn-color-success-subtle)'],
    ['--mn-color-ai-diff-add-strong', 'var(--mn-color-success-strong)'],
    ['--mn-color-ai-diff-remove', 'var(--mn-color-danger-subtle)'],
    ['--mn-color-ai-diff-remove-strong', 'var(--mn-color-danger-strong)'],
    ['--mn-color-confidence-low', 'var(--mn-color-danger-strong)'],
    ['--mn-color-confidence-medium', 'var(--mn-color-warning-strong)'],
    ['--mn-color-confidence-high', 'var(--mn-color-success-strong)'],
    [
      '--mn-gradient-ai',
      hc
        ? 'var(--mn-color-ai-solid)'
        : 'linear-gradient(135deg, var(--mn-color-ai-solid) 0%, color-mix(in srgb, var(--mn-color-ai-solid) 70%, var(--mn-color-info-solid)) 100%)',
    ],
  ];
}

/** Reduced motion: every duration collapses, so components need no special casing. */
const REDUCED_MOTION = `  @media (prefers-reduced-motion: reduce) {
    :root {
${Object.keys(duration)
  .map((k) => `      ${varName('duration', k)}: 0ms;`)
  .join('\n')}
    }
  }`;

export function buildMonarchCss(defaultAccent: Hsl): string {
  const parts = [`@layer ${LAYER} {`, block([':root'], staticDecls())];
  for (const id of THEME_IDS) {
    parts.push(block(themeSelectors(id), themeDecls(resolveTheme(themes[id], defaultAccent))));
  }
  parts.push(REDUCED_MOTION, '}');
  return header('Monarch tokens') + parts.join('\n\n') + '\n';
}

export function buildProductCss(productId: string, accent: Hsl): string {
  const parts = [`@layer ${LAYER} {`];
  for (const id of THEME_IDS) {
    const resolved = resolveTheme(themes[id], accent);
    parts.push(
      block(productSelectors(productId, id), [
        ...intentDecls('accent', resolved.intents.accent),
        ...compatAccentDecls(resolved),
      ]),
    );
  }
  parts.push('}');
  return header(`Monarch product accent: ${productId}`) + parts.join('\n\n') + '\n';
}

/**
 * Legacy bridge for apps still on shadcn-style variables (`hsl(var(--primary))`).
 * Values are generated from Monarch tokens, never authored separately. Meaning
 * of existing names is preserved: Rook's `secondary` and `accent` have always
 * been the blue informational colour, so they map to `info`, not to a neutral.
 */
function compatNeutralDecls(r: ResolvedTheme): Decls {
  const n = r.theme.neutrals;
  const i = r.intents;
  const c = (h: Hsl) => toChannels(h);
  return [
    ['--background', c(n['bg-canvas'])],
    ['--foreground', c(n['text-default'])],
    ['--card', c(n['bg-surface'])],
    ['--card-foreground', c(n['text-default'])],
    ['--popover', c(n['bg-raised'])],
    ['--popover-foreground', c(n['text-default'])],
    ['--secondary', c(i.info.solid)],
    ['--secondary-foreground', c(i.info['on-solid'])],
    ['--accent', c(i.info.solid)],
    ['--accent-foreground', c(i.info['on-solid'])],
    ['--success', c(i.success.solid)],
    ['--success-foreground', c(i.success['on-solid'])],
    ['--warning', c(i.warning.solid)],
    ['--warning-foreground', c(i.warning['on-solid'])],
    ['--destructive', c(i.danger.solid)],
    ['--destructive-foreground', c(i.danger['on-solid'])],
    ['--muted', c(n['bg-muted'])],
    ['--muted-foreground', c(n['text-muted'])],
    ['--border', c(n['border-default'])],
    ['--input', c(n['border-input'])],
    ['--radius', radius.lg],
    ['--sidebar-background', c(n['bg-surface'])],
    ['--sidebar-foreground', c(n['text-default'])],
    ['--sidebar-accent', c(n['bg-muted'])],
    ['--sidebar-accent-foreground', c(n['text-default'])],
    ['--sidebar-border', c(n['border-default'])],
  ];
}

/**
 * Which accent colour backs the legacy `--primary`. Apps use it both as a fill
 * (`bg-primary` + `text-primary-foreground`) and as text (`text-primary`,
 * `border-primary`). `strong` is readable as text on every surface, so it is
 * used whenever it also works as a fill with the same text colour as `solid`
 * (true for most brands). Brands that need dark text on their fill (Rook Yellow,
 * gold, orange) keep `solid`, and should use `text-mn-accent-strong` for text.
 */
export function compatPrimary(r: ResolvedTheme): { fill: Hsl; on: Hsl; usesStrong: boolean } {
  const a = r.intents.accent;
  const usesStrong = toChannels(a['on-strong']) === toChannels(a['on-solid']);
  return usesStrong
    ? { fill: a.strong, on: a['on-strong'], usesStrong }
    : { fill: a.solid, on: a['on-solid'], usesStrong };
}

function compatAccentDecls(r: ResolvedTheme): Decls {
  const a = r.intents.accent;
  const p = compatPrimary(r);
  return [
    ['--primary', toChannels(p.fill)],
    ['--primary-foreground', toChannels(p.on)],
    ['--ring', toChannels(a.strong)],
    ['--sidebar-primary', toChannels(p.fill)],
    ['--sidebar-primary-foreground', toChannels(p.on)],
    ['--sidebar-ring', toChannels(a.strong)],
  ];
}

export function buildCompatCss(defaultAccent: Hsl): string {
  const parts = [`@layer ${LAYER} {`];
  for (const id of THEME_IDS) {
    const resolved = resolveTheme(themes[id], defaultAccent);
    parts.push(
      block(themeSelectors(id), [...compatNeutralDecls(resolved), ...compatAccentDecls(resolved)]),
    );
  }
  parts.push('}');
  return (
    header('Monarch legacy bridge (shadcn variable names). Deprecated: remove after migration.') +
    parts.join('\n\n') +
    '\n'
  );
}

function header(title: string): string {
  return `/*! ${title}. Generated by @rookcom/monarch-tokens. Do not edit. */\n${LAYER_ORDER}\n\n`;
}
