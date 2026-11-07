/**
 * Primitive tokens: raw values with no meaning attached.
 *
 * Product code must never reference primitives directly. Components and apps
 * use semantic tokens (see `themes.ts`), which map to primitives per theme.
 */
import { fromHex, hsl, type Hsl } from './color.js';

export type Step = 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 950;
export type Ramp = Readonly<Record<Step, Hsl>>;

const STEPS: readonly Step[] = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950];
const LIGHTNESS: Readonly<Record<Step, number>> = {
  50: 97,
  100: 94,
  200: 88,
  300: 79,
  400: 68,
  500: 58,
  600: 49,
  700: 41,
  800: 33,
  900: 25,
  950: 16,
};

/** Saturation eases off at the light and dark ends so tints stay soft. */
function ramp(h: number, s: number): Ramp {
  const out = {} as Record<Step, Hsl>;
  for (const step of STEPS) {
    const edge = step <= 100 || step >= 900 ? 0.82 : 1;
    out[step] = hsl(h, s * edge, LIGHTNESS[step]);
  }
  return out;
}

/**
 * Neutrals are hand-tuned rather than generated: they carry almost all of the
 * interface, so each step is placed for a specific job (canvas, border, text).
 * A faint cool hue (220) keeps whites from looking yellow on most displays.
 */
export const neutral = {
  0: hsl(0, 0, 100),
  25: hsl(220, 20, 99),
  50: hsl(220, 17, 97.5),
  100: hsl(220, 14, 95.5),
  150: hsl(220, 13, 92.5),
  200: hsl(220, 13, 89),
  300: hsl(220, 11, 80),
  400: hsl(220, 9, 64),
  500: hsl(220, 9, 43.5),
  600: hsl(220, 10, 37),
  700: hsl(220, 13, 27),
  800: hsl(221, 15, 18),
  850: hsl(222, 16, 13.5),
  900: hsl(222, 18, 10),
  950: hsl(224, 22, 6.5),
  1000: hsl(0, 0, 0),
} as const;

export type NeutralStep = keyof typeof neutral;

/**
 * Disabled text, between two neutral steps: the lightest (light theme) or
 * darkest (dark theme) grey that still reaches 2.5:1 on every surface up to
 * `bg-muted`, so a disabled label stays distinguishable without competing with
 * real text (docs/architecture/CONTRAST-MATRIX.md).
 */
export const textDisabled = {
  light: hsl(220, 9, 62),
  dark: hsl(220, 9, 42),
} as const;

/** The Monarch identity spectrum: used sparingly for brand moments and data. */
export const spectrum = {
  blue: ramp(214, 90),
  violet: ramp(256, 82),
  purple: ramp(280, 68),
  pink: ramp(330, 78),
  orange: ramp(24, 94),
} as const;

/**
 * The official Rook brand palette, as published at rookhq.com/brand-kit.
 * These are the only hard-coded brand values in Monarch; everything else is
 * derived. Usage text is the brand kit's own.
 */
export const brand = {
  /** Rook Red: primary brand colour, call-to-action elements. */
  red: fromHex('#dc2e3e'),
  /** Rook Blue: secondary brand colour, links, information. */
  blue: fromHex('#0096d4'),
  /** Rook Green: success states, positive indicators. */
  green: fromHex('#28af4c'),
  /** Rook Yellow: warning states, highlights. */
  yellow: fromHex('#f4b400'),
  /** Rook Violet: accent colour, premium features. */
  violet: fromHex('#6366f1'),
} as const;

/** Ramps built from the brand hues, for data display and tints. */
export const status = {
  red: ramp(brand.red.h, brand.red.s),
  green: ramp(brand.green.h, brand.green.s),
  amber: ramp(brand.yellow.h, brand.yellow.s),
  sky: ramp(brand.blue.h, brand.blue.s),
} as const;

/**
 * The intelligence hue: marks AI-generated content and AI controls in every
 * product. A purple at 294°, between the nearest product accents (indigo at
 * 239°, magenta at 326°); the sweep in ADR 0013 found it gives the largest
 * separation from every accent and status colour in all four themes. Colour
 * is never the only AI cue: AI content also carries a label and the Bot icon.
 */
export const intelligenceBase: Hsl = hsl(294, 90, 54);

/** Base colours each status intent is derived from (see `themes.ts`). */
export const statusBase = {
  danger: brand.red,
  success: brand.green,
  warning: brand.yellow,
  info: brand.blue,
  ai: intelligenceBase,
} as const;

/** Accent used when no product is set: the brand's primary colour. */
export const defaultAccent: Hsl = brand.red;

export const fontFamily = {
  sans: "var(--font-poppins, 'Poppins'), ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
  mono: "var(--font-mono, ui-monospace), 'SFMono-Regular', Menlo, Consolas, 'Liberation Mono', monospace",
} as const;

/** Aligned with Tailwind's names so `text-sm` means the same everywhere. */
export const fontSize = {
  '2xs': '0.6875rem',
  xs: '0.75rem',
  sm: '0.875rem',
  base: '1rem',
  lg: '1.125rem',
  xl: '1.25rem',
  '2xl': '1.5rem',
  '3xl': '1.875rem',
  '4xl': '2.25rem',
  '5xl': '3rem',
} as const;

export const lineHeight = {
  tight: '1.2',
  snug: '1.35',
  normal: '1.5',
  relaxed: '1.65',
} as const;

export const fontWeight = {
  regular: '400',
  medium: '500',
  semibold: '600',
  bold: '700',
} as const;

export const letterSpacing = {
  tight: '-0.02em',
  snug: '-0.01em',
  normal: '0',
  wide: '0.02em',
  caps: '0.06em',
} as const;

/** 4px grid. Keys match Tailwind's spacing scale. */
export const space = {
  0: '0',
  px: '1px',
  0.5: '0.125rem',
  1: '0.25rem',
  1.5: '0.375rem',
  2: '0.5rem',
  2.5: '0.625rem',
  3: '0.75rem',
  4: '1rem',
  5: '1.25rem',
  6: '1.5rem',
  8: '2rem',
  10: '2.5rem',
  12: '3rem',
  16: '4rem',
  20: '5rem',
  24: '6rem',
} as const;

export const radius = {
  none: '0',
  xs: '0.25rem',
  sm: '0.375rem',
  md: '0.5rem',
  lg: '0.75rem',
  xl: '1rem',
  '2xl': '1.25rem',
  full: '9999px',
} as const;

/** Control heights keep buttons, inputs and selects aligned in a row. */
export const controlHeight = {
  xs: '1.75rem',
  sm: '2rem',
  md: '2.25rem',
  lg: '2.5rem',
  xl: '3rem',
} as const;

export const duration = {
  instant: '0ms',
  fast: '120ms',
  base: '180ms',
  slow: '260ms',
  slower: '400ms',
  /** One blink of the streaming caret. */
  'stream-caret': '1000ms',
  /** One pass of the "thinking" shimmer. */
  shimmer: '1600ms',
} as const;

export const easing = {
  standard: 'cubic-bezier(0.2, 0, 0, 1)',
  enter: 'cubic-bezier(0, 0, 0.2, 1)',
  exit: 'cubic-bezier(0.4, 0, 1, 1)',
  emphasized: 'cubic-bezier(0.3, 0, 0, 1.15)',
  /** Streamed text settling in: quick and even, so it never feels bouncy. */
  stream: 'cubic-bezier(0.2, 0, 0.2, 1)',
} as const;

export const breakpoint = {
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px',
} as const;

export const zIndex = {
  base: '0',
  raised: '10',
  sticky: '100',
  rail: '200',
  dropdown: '1000',
  overlay: '1100',
  modal: '1200',
  popover: '1300',
  toast: '1400',
  tooltip: '1500',
} as const;

/**
 * Display type for marketing and editorial surfaces. Fluid between a 390px and
 * a 1440px viewport. Weights are deliberate: Poppins at 600 with tight tracking
 * reads as confident and precise; light weights look thin on large type.
 */
export const display = {
  xl: {
    size: 'clamp(2.625rem, 1.85rem + 3.2vw, 4.5rem)',
    leading: '1.04',
    tracking: '-0.035em',
    weight: '600',
  },
  lg: {
    size: 'clamp(2.125rem, 1.6rem + 2.2vw, 3.5rem)',
    leading: '1.08',
    tracking: '-0.03em',
    weight: '600',
  },
  md: {
    size: 'clamp(1.75rem, 1.45rem + 1.25vw, 2.5rem)',
    leading: '1.14',
    tracking: '-0.025em',
    weight: '600',
  },
  sm: {
    size: 'clamp(1.375rem, 1.25rem + 0.55vw, 1.75rem)',
    leading: '1.25',
    tracking: '-0.02em',
    weight: '600',
  },
} as const;

/** Lead paragraph under display headings. */
export const lead = { size: 'clamp(1.0625rem, 1rem + 0.3vw, 1.25rem)', leading: '1.6' } as const;

/** Vertical rhythm between marketing sections (fluid). */
export const section = {
  sm: 'clamp(3rem, 2.25rem + 3vw, 4.5rem)',
  md: 'clamp(4rem, 2.75rem + 5vw, 7rem)',
  lg: 'clamp(5rem, 3.25rem + 7vw, 9rem)',
} as const;

/** Marketing content widths. */
export const width = {
  narrow: '44rem',
  content: '72rem',
  wide: '80rem',
  gutter: 'clamp(1.25rem, 0.75rem + 2vw, 2rem)',
} as const;

/** App-shell measurements shared with the existing RookRail. */
export const layout = {
  railCollapsed: '3rem',
  railExpanded: '14rem',
  contentMax: '80rem',
  readingMax: '42rem',
  gutter: '1rem',
  gutterLg: '1.5rem',
} as const;
