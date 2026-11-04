/**
 * Colour math for Monarch tokens.
 *
 * All colours are authored in HSL because every Rook app already stores its
 * tokens as HSL channels (`354 71% 52%`). Contrast is computed with the WCAG 2.x
 * relative-luminance formula, which is what WCAG 2.2 AA is measured against.
 */

export interface Hsl {
  /** Hue, 0–360. */
  readonly h: number;
  /** Saturation, 0–100. */
  readonly s: number;
  /** Lightness, 0–100. */
  readonly l: number;
}

export function hsl(h: number, s: number, l: number): Hsl {
  return { h: ((h % 360) + 360) % 360, s: clamp(s, 0, 100), l: clamp(l, 0, 100) };
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

/** Rounds to 1 decimal so generated CSS is stable and readable. */
function round1(n: number): number {
  return Math.round(n * 10) / 10;
}

/** `354 71% 52%`, the channel format shadcn-style `hsl(var(--x) / a)` expects. */
export function toChannels(c: Hsl): string {
  return `${round1(c.h)} ${round1(c.s)}% ${round1(c.l)}%`;
}

/** `hsl(354 71% 52%)`. */
export function toCss(c: Hsl): string {
  return `hsl(${toChannels(c)})`;
}

/** sRGB in 0–1. */
export function toRgb(c: Hsl): [number, number, number] {
  const s = c.s / 100;
  const l = c.l / 100;
  const k = (n: number) => (n + c.h / 30) % 12;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
  return [f(0), f(8), f(4)];
}

export function toHex(c: Hsl): string {
  return (
    '#' +
    toRgb(c)
      .map((v) =>
        Math.round(v * 255)
          .toString(16)
          .padStart(2, '0'),
      )
      .join('')
  );
}

/** Parses `#rrggbb` into HSL. Used for registry entries authored as hex. */
export function fromHex(hex: string): Hsl {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m?.[1]) throw new Error(`Invalid hex colour: ${hex}`);
  const n = parseInt(m[1], 16);
  const r = ((n >> 16) & 255) / 255;
  const g = ((n >> 8) & 255) / 255;
  const b = (n & 255) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  const d = max - min;
  let h = 0;
  let s = 0;
  if (d !== 0) {
    s = d / (1 - Math.abs(2 * l - 1));
    if (max === r) h = 60 * (((g - b) / d) % 6);
    else if (max === g) h = 60 * ((b - r) / d + 2);
    else h = 60 * ((r - g) / d + 4);
  }
  return hsl(h, s * 100, l * 100);
}

/**
 * Browsers paint 8-bit channels, so contrast is measured on the quantised
 * colour. Otherwise a pair can pass at 4.504:1 in floating point and fail at
 * 4.48:1 on screen, which is what axe measures.
 */
function quantise(v: number): number {
  return Math.round(v * 255) / 255;
}

export function relativeLuminance(c: Hsl): number {
  const [r, g, b] = toRgb(c)
    .map(quantise)
    .map((v) => (v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4))) as [
    number,
    number,
    number,
  ];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** WCAG contrast ratio, 1–21. */
export function contrast(a: Hsl, b: Hsl): number {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  const [hi, lo] = la > lb ? [la, lb] : [lb, la];
  return (hi + 0.05) / (lo + 0.05);
}

/**
 * Moves lightness in `direction` until `color` reaches `minRatio` against every
 * colour in `against`. Hue and saturation are preserved so the brand stays
 * recognisable. Throws if no lightness satisfies the target, so an impossible
 * theme fails the build instead of shipping silently.
 */
export function ensureContrast(
  color: Hsl,
  against: readonly Hsl[],
  minRatio: number,
  direction: 'darken' | 'lighten',
): Hsl {
  const step = direction === 'darken' ? -0.5 : 0.5;
  let current = color;
  for (let i = 0; i < 220; i++) {
    if (against.every((bg) => contrast(current, bg) >= minRatio)) return current;
    const next = hsl(current.h, current.s, current.l + step);
    if (next.l === current.l) break;
    current = next;
  }
  throw new Error(
    `Cannot reach ${minRatio}:1 for ${toChannels(color)} against ${against.map(toChannels).join(', ')}`,
  );
}

/** Picks whichever candidate foreground has the higher contrast on `bg`. */
export function bestForeground(bg: Hsl, candidates: readonly Hsl[]): { color: Hsl; ratio: number } {
  let best: { color: Hsl; ratio: number } | undefined;
  for (const c of candidates) {
    const ratio = contrast(c, bg);
    if (!best || ratio > best.ratio) best = { color: c, ratio };
  }
  if (!best) throw new Error('bestForeground needs at least one candidate');
  return best;
}

/** sRGB mix of `a` into `b` by `p` (0–1); same as CSS `color-mix(in srgb, a p%, b)`. */
export function mixSrgb(a: Hsl, b: Hsl, p: number): Hsl {
  const [ar, ag, ab] = toRgb(a);
  const [br, bg, bb] = toRgb(b);
  const m = (x: number, y: number) =>
    Math.round((x * p + y * (1 - p)) * 255)
      .toString(16)
      .padStart(2, '0');
  return fromHex(`#${m(ar, br)}${m(ag, bg)}${m(ab, bb)}`);
}
