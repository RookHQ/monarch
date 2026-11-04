/**
 * Semantic tokens and themes.
 *
 * A theme maps meaning ("the page background", "muted text") to concrete
 * colours. Intent colours (accent and the four statuses) are derived from one
 * base colour per theme by `deriveIntent`, which guarantees WCAG contrast. That
 * is what lets 16+ products each bring their own brand accent safely.
 */
import {
  bestForeground,
  contrast,
  ensureContrast,
  hsl,
  mixSrgb,
  toChannels,
  type Hsl,
} from './color.js';
import { neutral, statusBase } from './primitives.js';

export type ThemeId = 'light' | 'dark' | 'hc-light' | 'hc-dark';
export const THEME_IDS: readonly ThemeId[] = ['light', 'dark', 'hc-light', 'hc-dark'];

export interface NeutralRoles {
  readonly 'bg-canvas': Hsl;
  readonly 'bg-surface': Hsl;
  readonly 'bg-raised': Hsl;
  readonly 'bg-subtle': Hsl;
  readonly 'bg-muted': Hsl;
  readonly 'bg-emphasis': Hsl;
  readonly 'bg-inverse': Hsl;
  readonly 'text-default': Hsl;
  readonly 'text-muted': Hsl;
  readonly 'text-subtle': Hsl;
  readonly 'text-disabled': Hsl;
  readonly 'text-inverse': Hsl;
  /** Secondary text on `bg-inverse` sections. */
  readonly 'text-inverse-muted': Hsl;
  readonly 'border-subtle': Hsl;
  readonly 'border-default': Hsl;
  readonly 'border-strong': Hsl;
  readonly 'border-input': Hsl;
  /** Hairlines on `bg-inverse` sections. */
  readonly 'border-inverse': Hsl;
}

export interface ThemeDefinition {
  readonly id: ThemeId;
  readonly scheme: 'light' | 'dark';
  readonly contrast: 'standard' | 'more';
  /** Minimum ratios this theme guarantees. */
  readonly targets: { readonly text: number; readonly ui: number };
  readonly neutrals: NeutralRoles;
  readonly shadow: Readonly<Record<'xs' | 'sm' | 'md' | 'lg' | 'xl', string>>;
  readonly scrim: string;
}

const lightShadow = (a: number) => `hsl(222 30% 10% / ${a})`;
const darkShadow = (a: number) => `hsl(0 0% 0% / ${a})`;

export const themes: Readonly<Record<ThemeId, ThemeDefinition>> = {
  light: {
    id: 'light',
    scheme: 'light',
    contrast: 'standard',
    targets: { text: 4.5, ui: 3 },
    neutrals: {
      'bg-canvas': neutral[25],
      'bg-surface': neutral[0],
      'bg-raised': neutral[0],
      'bg-subtle': neutral[50],
      'bg-muted': neutral[100],
      'bg-emphasis': neutral[150],
      'bg-inverse': neutral[900],
      'text-default': neutral[850],
      'text-muted': neutral[600],
      'text-subtle': neutral[500],
      'text-disabled': neutral[400],
      'text-inverse': neutral[0],
      'text-inverse-muted': neutral[300],
      'border-subtle': neutral[150],
      'border-default': neutral[200],
      'border-strong': neutral[300],
      'border-input': hsl(220, 9, 56),
      'border-inverse': neutral[700],
    },
    shadow: {
      xs: `0 1px 2px ${lightShadow(0.06)}`,
      sm: `0 1px 3px ${lightShadow(0.08)}, 0 1px 2px ${lightShadow(0.04)}`,
      md: `0 4px 12px -2px ${lightShadow(0.08)}, 0 2px 4px -2px ${lightShadow(0.05)}`,
      lg: `0 12px 32px -8px ${lightShadow(0.14)}, 0 4px 8px -4px ${lightShadow(0.06)}`,
      xl: `0 24px 64px -16px ${lightShadow(0.22)}, 0 8px 16px -8px ${lightShadow(0.08)}`,
    },
    scrim: 'hsl(222 25% 8% / 0.42)',
  },
  dark: {
    id: 'dark',
    scheme: 'dark',
    contrast: 'standard',
    targets: { text: 4.5, ui: 3 },
    neutrals: {
      'bg-canvas': neutral[950],
      'bg-surface': neutral[900],
      'bg-raised': neutral[850],
      'bg-subtle': neutral[900],
      'bg-muted': neutral[800],
      'bg-emphasis': neutral[700],
      'bg-inverse': neutral[50],
      'text-default': neutral[50],
      'text-muted': neutral[300],
      'text-subtle': neutral[400],
      'text-disabled': neutral[600],
      'text-inverse': neutral[900],
      'text-inverse-muted': neutral[600],
      'border-subtle': neutral[850],
      'border-default': neutral[800],
      'border-strong': neutral[700],
      'border-input': hsl(220, 9, 45),
      'border-inverse': neutral[200],
    },
    shadow: {
      xs: `0 1px 2px ${darkShadow(0.4)}`,
      sm: `0 1px 3px ${darkShadow(0.45)}, 0 1px 2px ${darkShadow(0.3)}`,
      md: `0 4px 12px -2px ${darkShadow(0.5)}, 0 2px 4px -2px ${darkShadow(0.35)}`,
      lg: `0 12px 32px -8px ${darkShadow(0.6)}, 0 4px 8px -4px ${darkShadow(0.4)}`,
      xl: `0 24px 64px -16px ${darkShadow(0.7)}, 0 8px 16px -8px ${darkShadow(0.45)}`,
    },
    scrim: 'hsl(0 0% 0% / 0.62)',
  },
  'hc-light': {
    id: 'hc-light',
    scheme: 'light',
    contrast: 'more',
    targets: { text: 7, ui: 4.5 },
    neutrals: {
      'bg-canvas': neutral[0],
      'bg-surface': neutral[0],
      'bg-raised': neutral[0],
      'bg-subtle': neutral[50],
      'bg-muted': neutral[100],
      'bg-emphasis': neutral[150],
      'bg-inverse': neutral[1000],
      'text-default': neutral[1000],
      'text-muted': neutral[800],
      'text-subtle': neutral[700],
      'text-disabled': neutral[500],
      'text-inverse': neutral[0],
      'text-inverse-muted': neutral[150],
      'border-subtle': neutral[500],
      'border-default': neutral[600],
      'border-strong': neutral[800],
      'border-input': neutral[700],
      'border-inverse': neutral[500],
    },
    shadow: {
      xs: 'none',
      sm: 'none',
      md: '0 0 0 1px hsl(0 0% 0%)',
      lg: '0 0 0 2px hsl(0 0% 0%)',
      xl: '0 0 0 2px hsl(0 0% 0%)',
    },
    scrim: 'hsl(0 0% 0% / 0.7)',
  },
  'hc-dark': {
    id: 'hc-dark',
    scheme: 'dark',
    contrast: 'more',
    targets: { text: 7, ui: 4.5 },
    neutrals: {
      'bg-canvas': neutral[1000],
      'bg-surface': neutral[1000],
      'bg-raised': neutral[950],
      'bg-subtle': neutral[950],
      'bg-muted': neutral[900],
      'bg-emphasis': neutral[800],
      'bg-inverse': neutral[0],
      'text-default': neutral[0],
      'text-muted': neutral[150],
      'text-subtle': neutral[200],
      'text-disabled': neutral[400],
      'text-inverse': neutral[1000],
      'text-inverse-muted': neutral[800],
      'border-subtle': neutral[500],
      'border-default': neutral[400],
      'border-strong': neutral[200],
      'border-input': neutral[300],
      'border-inverse': neutral[500],
    },
    shadow: {
      xs: 'none',
      sm: 'none',
      md: '0 0 0 1px hsl(0 0% 100%)',
      lg: '0 0 0 2px hsl(0 0% 100%)',
      xl: '0 0 0 2px hsl(0 0% 100%)',
    },
    scrim: 'hsl(0 0% 0% / 0.8)',
  },
};

export type IntentId = 'accent' | 'danger' | 'success' | 'warning' | 'info';
export const INTENT_IDS: readonly IntentId[] = ['accent', 'danger', 'success', 'warning', 'info'];

export type IntentRole =
  | 'solid'
  | 'solid-hover'
  | 'solid-active'
  | 'on-solid'
  | 'subtle'
  | 'subtle-hover'
  | 'strong'
  | 'on-strong'
  | 'border'
  /** Readable as text and icons on `bg-inverse` sections (same targets as `strong`). */
  | 'strong-inverse';

export type IntentScale = Readonly<Record<IntentRole, Hsl>>;

export interface IntentReport {
  readonly base: string;
  /** Human-readable notes about every adjustment made to meet contrast. */
  readonly adjustments: readonly string[];
  readonly ratios: Readonly<{
    onSolid: number;
    strongOnCanvas: number;
    strongOnSurface: number;
    strongOnSubtle: number;
  }>;
}

const WHITE = neutral[0];
const INK = neutral[950];

/** How far a brand fill may be darkened to keep white text before we switch to dark text. */
const SOLID_DARKEN_BUDGET = 10;
/** Share of the accent fill mixed into the corner of `--mn-gradient-inverse`. */
export const INVERSE_TINT = 0.12;
/** Most text-inverse mixed into bg-inverse for cards inside an inverse section. */
export const INVERSE_RAISED = 0.09;
/** A filled control must at least be distinguishable from the page behind it. */
const SOLID_CANVAS_FLOOR = 1.5;

/**
 * Derives the nine colours an intent needs in one theme, each meeting the
 * theme's contrast targets.
 */
export function deriveIntent(
  base: Hsl,
  theme: ThemeDefinition,
): { scale: IntentScale; report: IntentReport } {
  const n = theme.neutrals;
  const dark = theme.scheme === 'dark';
  const hc = theme.contrast === 'more';
  const target = theme.targets.text;
  const adjustments: string[] = [];
  const achromatic = base.s < 8;

  // Solid fill (primary buttons, selected states). Preference order keeps
  // the brand colour whenever it can carry readable text:
  //   1. brand fill + white text
  //   2. brand fill + dark text          (Rook Yellow, orange, light accents)
  //   3. fill darkened within budget + white text
  //   4. fill moved away from the canvas with whichever text works
  // The fill must also stay visible against the canvas: WCAG 1.4.11 does not
  // require 3:1 for a filled button with readable text, so the standard floor
  // is a distinguishability threshold; high contrast raises it to the UI target.
  const away = dark ? 'lighten' : 'darken';
  const ui = theme.targets.ui;
  const canvasFloor = hc ? ui : SOLID_CANVAS_FLOOR;
  let solid = base;
  if (dark && achromatic) {
    solid = hsl(base.h, base.s, 94);
    adjustments.push('achromatic brand inverted to a light fill for dark theme');
  }
  if (contrast(solid, n['bg-canvas']) < canvasFloor) {
    solid = ensureContrast(solid, [n['bg-canvas']], canvasFloor, away);
    adjustments.push(
      `solid moved to ${toChannels(solid)} to stay visible (${canvasFloor}:1) against the canvas`,
    );
  }

  let onSolid: Hsl;
  let darkened: Hsl | undefined;
  if (contrast(WHITE, solid) < target) {
    try {
      const candidate = ensureContrast(solid, [WHITE], target, 'darken');
      if (solid.l - candidate.l <= SOLID_DARKEN_BUDGET) darkened = candidate;
    } catch {
      darkened = undefined;
    }
  }
  if (contrast(WHITE, solid) >= target) {
    onSolid = WHITE;
  } else if (darkened && contrast(darkened, n['bg-canvas']) >= canvasFloor) {
    adjustments.push(
      `solid darkened ${toChannels(solid)} → ${toChannels(darkened)} so white text meets ${target}:1`,
    );
    solid = darkened;
    onSolid = WHITE;
  } else if (contrast(INK, solid) >= target) {
    onSolid = INK;
    adjustments.push('dark text used on solid (white would fail contrast)');
  } else {
    {
      const fg = dark ? INK : WHITE;
      solid = ensureContrast(solid, [fg, n['bg-canvas']], target, away);
      onSolid = fg;
      adjustments.push(
        `solid moved to ${toChannels(solid)} so ${fg === WHITE ? 'white' : 'dark'} text meets ${target}:1`,
      );
    }
  }

  // Hover/active shift lightness while keeping both guarantees. Prefer the
  // direction that increases text contrast; fall back to the other, then to
  // smaller steps.
  const shade = (steps: number): Hsl => {
    const ok = (c: Hsl) =>
      contrast(onSolid, c) >= target && contrast(c, n['bg-canvas']) >= canvasFloor;
    const prefer = onSolid === WHITE ? -1 : 1;
    for (const magnitude of [steps, steps / 2]) {
      for (const dir of [prefer, -prefer]) {
        const c = hsl(solid.h, solid.s, solid.l + dir * magnitude);
        if (ok(c)) return c;
      }
    }
    return solid;
  };
  const solidHover = shade(5);
  const solidActive = shade(9);

  const subtle = dark
    ? hsl(base.h, base.s * 0.4, hc ? 10 : 14)
    : hsl(base.h, Math.min(base.s, 90) * 0.9, hc ? 97 : 96.5);
  const subtleHover = dark
    ? hsl(base.h, base.s * 0.4, hc ? 14 : 18)
    : hsl(base.h, Math.min(base.s, 90) * 0.9, hc ? 94 : 93.5);

  // Strong: the accessible version of the brand for text, icons, focus rings
  // and state indicators (checkbox fill, switch track).
  // bg-muted is where tables, selected rows and toolbars sit; accent text and
  // focus rings must stay readable there too.
  const strongAgainst = [
    n['bg-canvas'],
    n['bg-surface'],
    n['bg-subtle'],
    n['bg-muted'],
    subtle,
    subtleHover,
  ];
  const strongSeed = dark && achromatic ? hsl(base.h, base.s, 80) : base;
  const strong = ensureContrast(strongSeed, strongAgainst, target, dark ? 'lighten' : 'darken');
  if (Math.abs(strong.l - strongSeed.l) >= 0.5) {
    adjustments.push(`strong set to ${toChannels(strong)} for ${target}:1 text contrast`);
  }
  const onStrong = bestForeground(strong, [WHITE, INK]).color;

  const border = hc
    ? strong
    : dark
      ? hsl(base.h, base.s * 0.45, 32)
      : hsl(base.h, base.s * 0.6, 84);

  // Accent text on inverse (dark-in-light, light-in-dark) sections, including
  // the corner of --mn-gradient-inverse, which is tinted with INVERSE_TINT of
  // the solid fill.
  const inverseCorner = mixSrgb(solid, n['bg-inverse'], INVERSE_TINT);
  const inverseCard = mixSrgb(n['text-inverse'], n['bg-inverse'], INVERSE_RAISED);
  const strongInverse = ensureContrast(
    dark && achromatic ? hsl(base.h, base.s, 30) : base,
    [n['bg-inverse'], inverseCorner, inverseCard],
    target,
    dark ? 'darken' : 'lighten',
  );

  const scale: IntentScale = {
    solid,
    'solid-hover': solidHover,
    'solid-active': solidActive,
    'on-solid': onSolid,
    subtle,
    'subtle-hover': subtleHover,
    strong,
    'on-strong': onStrong,
    border,
    'strong-inverse': strongInverse,
  };

  return {
    scale,
    report: {
      base: toChannels(base),
      adjustments,
      ratios: {
        onSolid: round2(contrast(onSolid, solid)),
        strongOnCanvas: round2(contrast(strong, n['bg-canvas'])),
        strongOnSurface: round2(contrast(strong, n['bg-surface'])),
        strongOnSubtle: round2(contrast(strong, subtle)),
      },
    },
  };
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

export interface ResolvedTheme {
  readonly theme: ThemeDefinition;
  readonly intents: Readonly<Record<IntentId, IntentScale>>;
  readonly reports: Readonly<Record<IntentId, IntentReport>>;
}

/** Resolves every semantic colour for one theme and accent. */
export function resolveTheme(theme: ThemeDefinition, accent: Hsl): ResolvedTheme {
  const bases: Record<IntentId, Hsl> = { accent, ...statusBase };
  const intents = {} as Record<IntentId, IntentScale>;
  const reports = {} as Record<IntentId, IntentReport>;
  for (const id of INTENT_IDS) {
    const { scale, report } = deriveIntent(bases[id], theme);
    intents[id] = scale;
    reports[id] = report;
  }
  return { theme, intents, reports };
}
