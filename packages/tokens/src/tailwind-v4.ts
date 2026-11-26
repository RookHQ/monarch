/**
 * Tailwind CSS v4 theme (CSS-first configuration).
 *
 * Generated from the Tailwind 3 preset in `tailwind.ts`, so both versions
 * offer the same utility names (`bg-mn-surface`, `text-mn-fg-muted`,
 * `shadow-mn-md`, `h-control-md`, `duration-fast` …) and an app can move from
 * v3 to v4 without renaming classes. Nothing here is typed by hand: colours,
 * radius, spacing, fonts, shadows, motion, z-index, gradients, keyframes,
 * breakpoints, the dark variant and the RookRail variant all come from the
 * preset, plus every other `--mn-color-*` variable Monarch declares (the AI
 * roles, confidence, spectrum and scrim) and every duration and easing.
 *
 * Every theme variable points at a Monarch CSS variable and the block uses
 * `@theme inline`, so utilities read `var(--mn-…)` directly. That keeps theme,
 * contrast and product accent switching at runtime, including on nested
 * regions; a plain `@theme` would resolve the variable once on `:root`.
 */
import { staticDecls, themeDecls } from './css.js';
import { defaultAccent, duration, easing } from './primitives.js';
import { monarchPreset } from './tailwind.js';
import { THEME_IDS, resolveTheme, themes } from './themes.js';

type ColorTree = { readonly [key: string]: string | ColorTree };
type Decl = readonly [name: string, value: string];
type Extend = typeof monarchPreset.theme.extend;
type SimpleKey =
  | 'borderRadius'
  | 'boxShadow'
  | 'height'
  | 'spacing'
  | 'maxWidth'
  | 'transitionDuration'
  | 'transitionTimingFunction'
  | 'zIndex'
  | 'backgroundImage'
  | 'animation';

/** Tailwind 3 theme key → Tailwind 4 theme variable namespace. */
const NAMESPACES: ReadonlyArray<readonly [SimpleKey, string]> = [
  ['borderRadius', 'radius'],
  ['boxShadow', 'shadow'],
  ['height', 'height'],
  ['spacing', 'spacing'],
  ['maxWidth', 'max-width'],
  ['transitionDuration', 'transition-duration'],
  ['transitionTimingFunction', 'ease'],
  ['zIndex', 'z-index'],
  ['backgroundImage', 'background-image'],
  ['animation', 'animate'],
];

/** Every custom property Monarch declares in `monarch.css`, in any theme. */
export function monarchVariables(): ReadonlySet<string> {
  const names = new Set<string>();
  for (const [name] of staticDecls()) names.add(name);
  for (const id of THEME_IDS) {
    for (const [name] of themeDecls(resolveTheme(themes[id], defaultAccent))) names.add(name);
  }
  return names;
}

/** Flattens a nested Tailwind 3 colour object; `DEFAULT` adds no segment. */
function flattenColors(tree: ColorTree, prefix: readonly string[] = []): [string, string][] {
  return Object.entries(tree).flatMap(([key, value]): [string, string][] => {
    const path = key === 'DEFAULT' ? prefix : [...prefix, key];
    return typeof value === 'string' ? [[path.join('-'), value]] : flattenColors(value, path);
  });
}

const VAR = /var\((--[\w-]+)\)/;

/** The variable a v3 colour reads, e.g. `color-mix(… var(--mn-color-bg-canvas) …)`. */
function referencedVariable(v3Value: string): string {
  const name = VAR.exec(v3Value)?.[1];
  if (!name) throw new Error(`Tailwind preset colour without a variable: ${v3Value}`);
  return name;
}

function presetColors(): [string, string][] {
  return flattenColors(monarchPreset.theme.extend.colors as ColorTree);
}

/** `--color-mn-*` declarations: the preset's `mn` colours plus every other Monarch colour. */
export function colorDecls(): Decl[] {
  const out: Decl[] = [];
  const used = new Set<string>();
  for (const [name, value] of presetColors()) {
    if (!name.startsWith('mn-')) continue;
    const variable = referencedVariable(value);
    used.add(variable);
    out.push([`--color-${name}`, `var(${variable})`]);
  }
  const taken = new Set(out.map(([n]) => n));
  for (const variable of monarchVariables()) {
    if (!variable.startsWith('--mn-color-') || used.has(variable)) continue;
    const name = `--color-mn-${variable.slice('--mn-color-'.length)}`;
    if (taken.has(name)) throw new Error(`Tailwind v4 colour name clash: ${name}`);
    taken.add(name);
    out.push([name, `var(${variable})`]);
  }
  return out;
}

/** Legacy shadcn colour names (`bg-primary` …), backed by `css/compat.css`. */
export function legacyColorDecls(): Decl[] {
  return presetColors()
    .filter(([name]) => !name.startsWith('mn-'))
    .map(([name, value]) => [`--color-${name}`, `hsl(var(${referencedVariable(value)}))`]);
}

function simpleDecls(extend: Extend): Decl[] {
  const out: Decl[] = [];
  for (const [key, namespace] of NAMESPACES) {
    const group: Readonly<Record<string, string>> = extend[key];
    for (const [name, value] of Object.entries(group)) out.push([`--${namespace}-${name}`, value]);
  }
  // Every Monarch duration and easing, not only the ones the v3 preset names.
  const durations = new Set(Object.keys(extend.transitionDuration));
  for (const name of Object.keys(duration)) {
    if (!durations.has(name))
      out.push([`--transition-duration-${name}`, `var(--mn-duration-${name})`]);
  }
  const easings = new Set(Object.keys(extend.transitionTimingFunction));
  for (const name of Object.keys(easing)) {
    if (!easings.has(name)) out.push([`--ease-${name}`, `var(--mn-ease-${name})`]);
  }
  return out;
}

function typeDecls(extend: Extend): Decl[] {
  const out: Decl[] = Object.entries(extend.fontFamily).map(
    ([name, stack]) => [`--font-${name}`, stack.join(', ')] as const,
  );
  for (const [name, [size, { lineHeight }]] of Object.entries(extend.fontSize)) {
    out.push([`--text-${name}`, size], [`--text-${name}--line-height`, lineHeight]);
  }
  return out;
}

/** Every declaration inside the `@theme inline` block, in output order. */
export function themeV4Decls(): Decl[] {
  const { screens, extend } = monarchPreset.theme;
  return [
    ...Object.entries(screens).map(([name, value]) => [`--breakpoint-${name}`, value] as const),
    ...colorDecls(),
    ...typeDecls(extend),
    ...simpleDecls(extend),
  ];
}

const kebab = (prop: string) => prop.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`);

function keyframesCss(): string {
  return Object.entries(monarchPreset.theme.extend.keyframes)
    .map(([name, frames]) => {
      const steps = Object.entries(
        frames as Readonly<Record<string, Readonly<Record<string, string>>>>,
      )
        .map(([at, props]) => {
          const body = Object.entries(props)
            .map(([p, v]) => `      ${kebab(p)}: ${v};`)
            .join('\n');
          return `    ${at} {\n${body}\n    }`;
        })
        .join('\n');
      return `  @keyframes ${name} {\n${steps}\n  }`;
    })
    .join('\n');
}

interface Variant {
  readonly name: string;
  readonly selectors: readonly string[];
}

/** The dark variant and every variant the preset's plugins add, as v4 custom variants. */
export function customVariants(): Variant[] {
  const variants: Variant[] = [{ name: 'dark', selectors: monarchPreset.darkMode[1] }];
  for (const plugin of monarchPreset.plugins) {
    plugin({
      addVariant: (name, selector) =>
        void variants.push({
          name,
          selectors: typeof selector === 'string' ? [selector] : selector,
        }),
    });
  }
  return variants;
}

function variantCss({ name, selectors }: Variant): string {
  const rules = selectors.map((s) => `  ${s} {\n    @slot;\n  }`).join('\n');
  return `@custom-variant ${name} {\n${rules}\n}`;
}

function containerCss(): string {
  const { container, screens } = monarchPreset.theme;
  const lines = [
    `  margin-inline: ${container.center ? 'auto' : '0'};`,
    `  padding-inline: ${container.padding};`,
  ];
  for (const [screen, max] of Object.entries(container.screens)) {
    const min = (screens as Readonly<Record<string, string>>)[screen];
    if (min) lines.push(`  @media (width >= ${min}) {\n    max-width: ${max};\n  }`);
  }
  return `@utility container {\n${lines.join('\n')}\n}`;
}

const HEADER = `/*! Monarch theme for Tailwind CSS v4. Generated by @rookcom/monarch-tokens. Do not edit.
 *
 * Usage, in the CSS file that imports Tailwind:
 *
 *   @layer theme, base, monarch, components, utilities;
 *   @import "tailwindcss";
 *   @import "@rookcom/monarch-tokens/css";
 *   @import "@rookcom/monarch-tokens/css/tailwind-v4";
 *
 * Utilities have the same names as with the Tailwind 3 preset
 * (bg-mn-surface, text-mn-fg-muted, border-mn-line, bg-mn-accent-solid,
 * shadow-mn-md, h-control-md, duration-fast …). Each one reads a Monarch
 * variable directly (@theme inline), so light, dark, high contrast and the
 * product accent keep switching at runtime.
 *
 * Cascade layers: Tailwind v4 puts its output in the layers theme, base,
 * components and utilities; Monarch puts its CSS in monarch.reset,
 * monarch.tokens, monarch.base and monarch.components. Layer order is fixed
 * by the first stylesheet that names a layer, so declare the order once, as
 * above, before any import: Tailwind's preflight (base) then sits below
 * Monarch, and utilities stay above Monarch components, as in Tailwind 3.
 *
 * Legacy shadcn names (bg-primary, text-muted-foreground …) are in
 * @rookcom/monarch-tokens/css/tailwind-v4-compat, with css/compat.
 */
`;

export function buildTailwindV4Css(): string {
  const decls = themeV4Decls()
    .map(([k, v]) => `  ${k}: ${v};`)
    .join('\n');
  return [
    HEADER,
    `@theme inline {\n${decls}\n\n${keyframesCss()}\n}\n`,
    `${customVariants().map(variantCss).join('\n\n')}\n`,
    `${containerCss()}\n`,
  ].join('\n');
}

export function buildTailwindV4CompatCss(): string {
  const decls = legacyColorDecls()
    .map(([k, v]) => `  ${k}: ${v};`)
    .join('\n');
  return `/*! Monarch legacy colour names for Tailwind CSS v4 (shadcn names). Deprecated: remove after migration.
 * Generated by @rookcom/monarch-tokens. Do not edit.
 *
 * Needs @rookcom/monarch-tokens/css/compat, which defines the variables:
 *
 *   @import "@rookcom/monarch-tokens/css/compat";
 *   @import "@rookcom/monarch-tokens/css/tailwind-v4-compat";
 */

@theme inline {
${decls}
}
`;
}
