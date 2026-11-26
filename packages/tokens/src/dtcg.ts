/**
 * Design Tokens Community Group (DTCG) format, stable version 2025.10.
 *
 * Emits the same tokens as the CSS, in the W3C format design tools read:
 * colours as `{ colorSpace, components, alpha?, hex }`, dimensions and
 * durations as `{ value, unit }`, easings as cubic Bézier arrays, font
 * families as arrays of names, shadows as objects. Semantic tokens alias a
 * primitive (`"{color.neutral.900}"`) whenever the theme uses that primitive
 * exactly, and another semantic token where the CSS does (`text.link` →
 * `accent.strong`).
 *
 * The four themes are modes: one semantic file per theme, tied together by a
 * resolver document (DTCG Resolver Module 2025.10). `tokens.json` is not
 * touched by this module.
 *
 * Out of scope, because DTCG 2025.10 has no type for them: fluid `clamp()`
 * sizes (display, section, widths), `em` letter spacing and gradients. They
 * stay in the CSS.
 */
import { hsl, toChannels, toHex, type Hsl } from './color.js';
import { themeDecls } from './css.js';
import * as p from './primitives.js';
import { THEME_IDS, resolveTheme, themes, type ThemeId } from './themes.js';

export const DTCG_VERSION = '2025.10';

export interface DtcgColor {
  readonly colorSpace: 'srgb';
  readonly components: readonly [number, number, number];
  readonly alpha?: number;
  readonly hex: string;
}
export interface DtcgDimension {
  readonly value: number;
  readonly unit: 'px' | 'rem';
}
export interface DtcgDuration {
  readonly value: number;
  readonly unit: 'ms';
}
export interface DtcgShadowLayer {
  readonly color: DtcgColor;
  readonly offsetX: DtcgDimension;
  readonly offsetY: DtcgDimension;
  readonly blur: DtcgDimension;
  readonly spread: DtcgDimension;
}
export type DtcgType =
  | 'color'
  | 'dimension'
  | 'duration'
  | 'cubicBezier'
  | 'fontFamily'
  | 'fontWeight'
  | 'number'
  | 'shadow';
/** An alias such as `"{color.neutral.900}"`. */
export type DtcgAlias = `{${string}}`;
export type DtcgValue =
  | DtcgColor
  | DtcgDimension
  | DtcgDuration
  | readonly DtcgShadowLayer[]
  | readonly [number, number, number, number]
  | readonly string[]
  | number
  | DtcgAlias;

export interface DtcgToken {
  readonly $type: DtcgType;
  readonly $value: DtcgValue;
  readonly $description?: string;
  readonly $extensions?: { readonly 'com.rookhq.monarch': { readonly cssVar: string } };
}
export interface DtcgGroup {
  [key: string]: DtcgToken | DtcgGroup | string;
}

export function isToken(node: unknown): node is DtcgToken {
  return typeof node === 'object' && node !== null && '$value' in node;
}

export const isAlias = (v: DtcgValue): v is DtcgAlias =>
  typeof v === 'string' && v.startsWith('{') && v.endsWith('}');

export const isColorValue = (v: DtcgValue): v is DtcgColor =>
  typeof v === 'object' && 'colorSpace' in v;

export const isDimensionValue = (v: DtcgValue): v is DtcgDimension =>
  typeof v === 'object' && 'unit' in v && (v.unit === 'px' || v.unit === 'rem');

/* ------------------------------------------------------------------ */
/* Values                                                              */
/* ------------------------------------------------------------------ */

const round = (n: number, places = 4) => Math.round(n * 10 ** places) / 10 ** places;

/** A colour from its 8-bit hex, so `components` always round-trip to `hex`. */
export function dtcgColor(c: Hsl, alpha = 1): DtcgColor {
  const hex = toHex(c);
  const byte = (i: number) => parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255;
  const components = [round(byte(0)), round(byte(1)), round(byte(2))] as const;
  return alpha < 1
    ? { colorSpace: 'srgb', components, alpha: round(alpha), hex }
    : { colorSpace: 'srgb', components, hex };
}

const CSS_HSL = /^hsl\((\d+(?:\.\d+)?) (\d+(?:\.\d+)?)% (\d+(?:\.\d+)?)%(?: \/ (\d*\.?\d+))?\)$/;

/** Parses the `hsl(h s% l%)` / `hsl(h s% l% / a)` strings the CSS emitter writes. */
export function parseCssHsl(value: string): { color: Hsl; alpha: number } | null {
  const m = CSS_HSL.exec(value.trim());
  if (!m) return null;
  return {
    color: hsl(Number(m[1]), Number(m[2]), Number(m[3])),
    alpha: m[4] === undefined ? 1 : Number(m[4]),
  };
}

/** `0.25rem`, `1px` or `0`. Throws on anything else (clamp(), em, %). */
export function parseDimension(value: string): DtcgDimension {
  if (value.trim() === '0') return { value: 0, unit: 'px' };
  const m = /^(-?\d*\.?\d+)(px|rem)$/.exec(value.trim());
  if (!m) throw new Error(`Not a DTCG dimension: ${value}`);
  return { value: Number(m[1]), unit: m[2] === 'rem' ? 'rem' : 'px' };
}

export function parseDuration(value: string): DtcgDuration {
  const m = /^(\d*\.?\d+)ms$/.exec(value.trim());
  if (!m) throw new Error(`Not a duration in ms: ${value}`);
  return { value: Number(m[1]), unit: 'ms' };
}

export function parseCubicBezier(value: string): readonly [number, number, number, number] {
  const m = /^cubic-bezier\(([^)]+)\)$/.exec(value.trim());
  const n = m?.[1]?.split(',').map((s) => Number(s.trim())) ?? [];
  const [a, b, c, d] = n;
  if (n.length !== 4 || [a, b, c, d].some((x) => x === undefined || Number.isNaN(x))) {
    throw new Error(`Not a cubic-bezier(): ${value}`);
  }
  return [a as number, b as number, c as number, d as number];
}

/** Splits on commas outside parentheses. */
function topLevelList(value: string): string[] {
  const out: string[] = [];
  let depth = 0;
  let current = '';
  for (const ch of value) {
    if (ch === '(') depth++;
    if (ch === ')') depth--;
    if (ch === ',' && depth === 0) {
      out.push(current.trim());
      current = '';
    } else current += ch;
  }
  if (current.trim()) out.push(current.trim());
  return out;
}

/**
 * A CSS font stack as family names. `var(--font-poppins, 'Poppins')` becomes
 * its fallback (`Poppins`), so the result holds no `var()`.
 */
export function parseFontFamily(stack: string): string[] {
  return topLevelList(stack).map((entry) => {
    const v = /^var\(\s*--[\w-]+\s*,\s*(.+)\)$/.exec(entry);
    return (v?.[1] ?? entry).trim().replace(/^['"]|['"]$/g, '');
  });
}

/** `0 4px 12px -2px hsl(… / 0.08), …` or `none` as DTCG shadow layers. */
export function parseShadow(value: string): DtcgShadowLayer[] {
  const zero = parseDimension('0');
  if (value.trim() === 'none') {
    return [
      { color: dtcgColor(hsl(0, 0, 0), 0), offsetX: zero, offsetY: zero, blur: zero, spread: zero },
    ];
  }
  return topLevelList(value).map((layer) => {
    const colorAt = layer.indexOf('hsl(');
    const parsed = parseCssHsl(layer.slice(colorAt));
    const lengths = layer.slice(0, colorAt).trim().split(/\s+/).map(parseDimension);
    const [offsetX, offsetY, blur = zero, spread = zero] = lengths;
    if (colorAt < 0 || !parsed || !offsetX || !offsetY) throw new Error(`Bad shadow: ${layer}`);
    return { color: dtcgColor(parsed.color, parsed.alpha), offsetX, offsetY, blur, spread };
  });
}

/* ------------------------------------------------------------------ */
/* Tree helpers                                                        */
/* ------------------------------------------------------------------ */

/** Token keys may not contain `.`, `{`, `}` or start with `$`. */
export const tokenKey = (key: string | number) => String(key).replace('.', '-');

function setPath(root: DtcgGroup, path: readonly string[], token: DtcgToken): void {
  let node = root;
  for (const [i, key] of path.entries()) {
    if (i === path.length - 1) {
      if (key in node) throw new Error(`Duplicate token ${path.join('.')}`);
      node[key] = token;
      return;
    }
    const next = node[key];
    if (next === undefined) {
      const group: DtcgGroup = {};
      node[key] = group;
      node = group;
    } else if (typeof next === 'object' && !isToken(next)) {
      node = next;
    } else {
      throw new Error(`Token and group clash at ${path.slice(0, i + 1).join('.')}`);
    }
  }
}

/** Every token in a group with its dotted path. */
export function walkTokens(
  group: DtcgGroup,
  prefix: readonly string[] = [],
): [string, DtcgToken][] {
  return Object.entries(group).flatMap(([key, node]): [string, DtcgToken][] => {
    if (key.startsWith('$') || typeof node === 'string') return [];
    const path = [...prefix, key];
    return isToken(node) ? [[path.join('.'), node]] : walkTokens(node, path);
  });
}

function group<T>(
  root: DtcgGroup,
  prefix: readonly string[],
  values: Readonly<Record<string | number, T>>,
  token: (v: T) => DtcgToken,
): void {
  for (const [k, v] of Object.entries(values) as [string, T][]) {
    setPath(root, [...prefix, tokenKey(k)], token(v));
  }
}

/* ------------------------------------------------------------------ */
/* Primitives                                                          */
/* ------------------------------------------------------------------ */

/** Every primitive colour with its token path, in alias-preference order. */
export function primitiveColors(): [string[], Hsl][] {
  const out: [string[], Hsl][] = [];
  const ramp = (path: string[], r: Readonly<Record<string | number, Hsl>>) => {
    for (const [k, c] of Object.entries(r)) out.push([[...path, tokenKey(k)], c]);
  };
  ramp(['color', 'neutral'], p.neutral);
  ramp(['color', 'disabled'], p.textDisabled);
  ramp(['color', 'brand'], p.brand);
  out.push([['color', 'intelligence'], p.intelligenceBase]);
  for (const [name, r] of Object.entries(p.status)) ramp(['color', 'status', name], r);
  for (const [name, r] of Object.entries(p.spectrum)) ramp(['color', 'spectrum', name], r);
  return out;
}

const colorToken = (c: Hsl): DtcgToken => ({ $type: 'color', $value: dtcgColor(c) });
const dimToken = (v: string): DtcgToken => ({ $type: 'dimension', $value: parseDimension(v) });

export function buildDtcgPrimitives(): DtcgGroup {
  const root: DtcgGroup = {
    $description: 'Monarch primitive tokens: raw values. Use the semantic tokens in product work.',
  };
  for (const [path, c] of primitiveColors()) setPath(root, path, colorToken(c));
  group(root, ['font', 'family'], p.fontFamily, (v) => ({
    $type: 'fontFamily',
    $value: parseFontFamily(v),
  }));
  group(root, ['font', 'size'], p.fontSize, dimToken);
  group(root, ['font', 'weight'], p.fontWeight, (v) => ({
    $type: 'fontWeight',
    $value: Number(v),
  }));
  group(root, ['font', 'line-height'], p.lineHeight, (v) => ({
    $type: 'number',
    $value: Number(v),
  }));
  group(root, ['space'], p.space, dimToken);
  group(root, ['radius'], p.radius, dimToken);
  group(root, ['control'], p.controlHeight, dimToken);
  group(root, ['breakpoint'], p.breakpoint, dimToken);
  group(root, ['duration'], p.duration, (v) => ({ $type: 'duration', $value: parseDuration(v) }));
  group(root, ['easing'], p.easing, (v) => ({ $type: 'cubicBezier', $value: parseCubicBezier(v) }));
  group(root, ['z-index'], p.zIndex, (v) => ({ $type: 'number', $value: Number(v) }));
  return root;
}

/* ------------------------------------------------------------------ */
/* Semantic (one theme)                                                */
/* ------------------------------------------------------------------ */

/**
 * Token path for a Monarch variable: `--mn-color-bg-canvas` → color.bg.canvas,
 * `--mn-color-accent-solid-hover` → color.accent.solid-hover,
 * `--mn-shadow-md` → shadow.md. The inverse of `cssVarFor`.
 */
export function pathFor(cssVar: string): string[] {
  const rest = cssVar.replace(/^--mn-/, '');
  const [head = '', ...tail] = rest.split('-');
  if (head === 'color') {
    const [groupName = '', ...name] = tail;
    return ['color', groupName, name.join('-')];
  }
  return [head, tail.join('-')];
}

export const cssVarFor = (path: string): string => `--mn-${path.replace(/\./g, '-')}`;

/** Exact primitive matches, keyed by the channels the CSS emits. */
function primitiveLookup(): Map<string, string> {
  const map = new Map<string, string>();
  for (const [path, c] of primitiveColors()) {
    const key = toChannels(c);
    if (!map.has(key)) map.set(key, path.join('.'));
  }
  return map;
}

/** The semantic tokens for one theme (default accent), as a DTCG group. */
export function buildDtcgSemantic(id: ThemeId): DtcgGroup {
  const theme = themes[id];
  const root: DtcgGroup = {
    $description: `Monarch semantic tokens, ${id} theme. Aliases point at the primitive set.`,
  };
  const lookup = primitiveLookup();
  const ext = (cssVar: string) => ({ 'com.rookhq.monarch': { cssVar } }) as const;
  for (const [name, value] of themeDecls(resolveTheme(theme, p.defaultAccent))) {
    if (!name.startsWith('--mn-')) continue;
    const path = pathFor(name);
    if (name.startsWith('--mn-color-')) {
      const ref = /^var\((--mn-color-[\w-]+)\)$/.exec(value)?.[1];
      if (ref) {
        setPath(root, path, {
          $type: 'color',
          $value: `{${pathFor(ref).join('.')}}`,
          $extensions: ext(name),
        });
        continue;
      }
      const parsed = parseCssHsl(value);
      if (!parsed) continue; // gradients: CSS only
      const primitive = parsed.alpha === 1 ? lookup.get(toChannels(parsed.color)) : undefined;
      setPath(root, path, {
        $type: 'color',
        $value: primitive ? `{${primitive}}` : dtcgColor(parsed.color, parsed.alpha),
        $extensions: ext(name),
      });
    } else if (name.startsWith('--mn-shadow-')) {
      setPath(root, path, { $type: 'shadow', $value: parseShadow(value), $extensions: ext(name) });
    } else if (name === '--mn-focus-ring-width') {
      setPath(root, path, {
        $type: 'dimension',
        $value: parseDimension(value),
        $extensions: ext(name),
      });
    }
  }
  return root;
}

/* ------------------------------------------------------------------ */
/* Documents                                                           */
/* ------------------------------------------------------------------ */

function merge(target: DtcgGroup, source: DtcgGroup): DtcgGroup {
  for (const [path, token] of walkTokens(source)) setPath(target, path.split('.'), token);
  return target;
}

/** One self-contained file: primitives plus the light theme's semantic tokens. */
export function buildDtcgDocument(): DtcgGroup {
  const doc: DtcgGroup = {
    $description:
      'Rook Monarch design tokens (DTCG 2025.10): primitives and the light theme. Other themes: tokens.resolver.json.',
  };
  merge(doc, buildDtcgPrimitives());
  return merge(doc, buildDtcgSemantic('light'));
}

/** File names of the per-theme sets, relative to the resolver. */
export const DTCG_FILES = {
  primitives: 'dtcg/primitives.json',
  semantic: (id: ThemeId) => `dtcg/semantic.${id}.json`,
} as const;

interface ResolverRef {
  readonly $ref: string;
}
export interface DtcgResolver {
  readonly name: string;
  readonly version: typeof DTCG_VERSION;
  readonly description: string;
  readonly sets: Readonly<
    Record<string, { readonly description: string; readonly sources: readonly ResolverRef[] }>
  >;
  readonly modifiers: Readonly<
    Record<
      string,
      {
        readonly description: string;
        readonly contexts: Readonly<Record<string, readonly ResolverRef[]>>;
        readonly default: string;
      }
    >
  >;
  readonly resolutionOrder: readonly ResolverRef[];
}

/** DTCG Resolver Module document: primitives set, then a `theme` modifier with four contexts. */
export function buildDtcgResolver(): DtcgResolver {
  return {
    name: 'Rook Monarch',
    version: DTCG_VERSION,
    description:
      'Primitives, then the semantic tokens of one theme. Pick the theme with the "theme" modifier.',
    sets: {
      primitives: {
        description: 'Raw values shared by every theme.',
        sources: [{ $ref: DTCG_FILES.primitives }],
      },
    },
    modifiers: {
      theme: {
        description: 'Light, dark, high-contrast light and high-contrast dark.',
        contexts: Object.fromEntries(
          THEME_IDS.map((id) => [id, [{ $ref: DTCG_FILES.semantic(id) }]]),
        ),
        default: 'light',
      },
    },
    resolutionOrder: [{ $ref: '#/sets/primitives' }, { $ref: '#/modifiers/theme' }],
  };
}

/** Resolves an alias chain to its final colour, or null if a link is missing. */
export function resolveAlias(
  value: DtcgValue,
  tokens: ReadonlyMap<string, DtcgToken>,
  depth = 0,
): DtcgValue | null {
  if (!isAlias(value)) return value;
  const target = tokens.get(value.slice(1, -1));
  if (!target || depth > 10) return null;
  return resolveAlias(target.$value, tokens, depth + 1);
}

/**
 * Every DTCG file the build writes, keyed by its path in `dist/`. The build
 * writes exactly these, and the tests check the resolver against them.
 */
export function buildDtcgFiles(): ReadonlyMap<string, DtcgGroup | DtcgResolver> {
  const files = new Map<string, DtcgGroup | DtcgResolver>([
    ['tokens.dtcg.json', buildDtcgDocument()],
    ['tokens.resolver.json', buildDtcgResolver()],
    [DTCG_FILES.primitives, buildDtcgPrimitives()],
  ]);
  for (const id of THEME_IDS) files.set(DTCG_FILES.semantic(id), buildDtcgSemantic(id));
  return files;
}
