import { describe, expect, it } from 'vitest';
import { buildCompatCss } from '../src/css.js';
import { defaultAccent, duration, easing } from '../src/primitives.js';
import { monarchPreset } from '../src/tailwind.js';
import {
  buildTailwindV4CompatCss,
  buildTailwindV4Css,
  customVariants,
  monarchVariables,
  themeV4Decls,
} from '../src/tailwind-v4.js';

type Tree = { readonly [key: string]: string | Tree };
function flatten(tree: Tree, prefix: readonly string[] = []): string[] {
  return Object.entries(tree).flatMap(([k, v]) => {
    const path = k === 'DEFAULT' ? prefix : [...prefix, k];
    return typeof v === 'string' ? [path.join('-')] : flatten(v, path);
  });
}

function balanced(css: string): boolean {
  let depth = 0;
  for (const ch of css.replace(/\/\*[\s\S]*?\*\//g, '')) {
    if (ch === '{') depth++;
    if (ch === '}') depth--;
    if (depth < 0) return false;
  }
  return depth === 0;
}

const themeBlock = (css: string) => /@theme inline \{([\s\S]*?)\n\}/.exec(css)?.[1] ?? '';

describe('Tailwind v4 theme', () => {
  const css = buildTailwindV4Css();
  const block = themeBlock(css);
  const v3Colors = flatten(monarchPreset.theme.extend.colors as Tree);

  it('is deterministic and looks like valid CSS', () => {
    expect(buildTailwindV4Css()).toBe(css);
    expect(balanced(css)).toBe(true);
    expect(css.startsWith('/*!')).toBe(true);
    for (const line of block.split('\n').filter((l) => /^ {2}--/.test(l))) {
      expect(line, line).toMatch(/^ {2}--[\w-]+: [^;]+;$/);
    }
  });

  it('uses @theme inline, so utilities read the Monarch variable directly', () => {
    expect(css).toContain('@theme inline {');
    expect(css).not.toMatch(/@theme \{/);
  });

  it('explains usage and the cascade layer order in its header', () => {
    expect(css).toContain('@import "tailwindcss";');
    expect(css).toContain('@import "@rookcom/monarch-tokens/css";');
    expect(css).toContain('@import "@rookcom/monarch-tokens/css/tailwind-v4";');
    expect(css).toContain('@layer theme, base, monarch, components, utilities;');
  });

  it('has a variable for every mn colour the Tailwind 3 preset exposes', () => {
    const mn = v3Colors.filter((n) => n.startsWith('mn-'));
    expect(mn.length).toBeGreaterThan(80);
    for (const name of mn) expect(block, name).toContain(`  --color-${name}: var(--mn-color-`);
  });

  it('includes the AI intent, AI roles and the data palette', () => {
    for (const name of [
      'mn-ai',
      'mn-ai-solid',
      'mn-ai-strong',
      'mn-ai-thinking',
      'mn-ai-citation',
      'mn-ai-diff-add',
      'mn-confidence-high',
      'mn-data-1',
      'mn-data-5',
    ]) {
      expect(block, name).toContain(`--color-${name}: `);
    }
  });

  it('references only variables that monarch.css declares', () => {
    const declared = monarchVariables();
    const used = [...css.matchAll(/var\((--mn-[\w-]+)\)/g)].map((m) => m[1] ?? '');
    expect(used.length).toBeGreaterThan(100);
    for (const name of used) expect(declared.has(name), name).toBe(true);
  });

  it('declares each theme variable once', () => {
    const names = themeV4Decls().map(([n]) => n);
    expect(new Set(names).size).toBe(names.length);
  });

  it('keeps the Tailwind 3 utility names for every other theme key', () => {
    for (const decl of [
      '--shadow-mn-md: var(--mn-shadow-md);',
      '--height-control-md: var(--mn-control-md);',
      '--transition-duration-fast: var(--mn-duration-fast);',
      '--ease-standard: var(--mn-ease-standard);',
      '--radius-md: var(--mn-radius-md);',
      '--font-sans: var(--mn-font-sans);',
      '--text-2xs: var(--mn-text-2xs);',
      '--text-2xs--line-height: 1rem;',
      '--spacing-section-md: var(--mn-section-md);',
      '--max-width-reading: var(--mn-layout-reading-max);',
      '--z-index-modal: var(--mn-z-modal);',
      '--background-image-mn-spectrum: var(--mn-gradient-spectrum);',
      '--animate-fade-in: fadeIn var(--mn-duration-slow) var(--mn-ease-enter);',
      '--breakpoint-md: 768px;',
    ]) {
      expect(block, decl).toContain(`  ${decl}`);
    }
    for (const name of Object.keys(monarchPreset.theme.extend.keyframes)) {
      expect(block).toContain(`@keyframes ${name} {`);
    }
  });

  it('offers every Monarch duration and easing', () => {
    for (const k of Object.keys(duration)) expect(block).toContain(`--transition-duration-${k}: `);
    for (const k of Object.keys(easing)) expect(block).toContain(`--ease-${k}: `);
  });

  it('ports the dark and rail-expanded variants and the container', () => {
    expect(customVariants().map((v) => v.name)).toEqual(['dark', 'rail-expanded']);
    expect(css).toContain('@custom-variant dark {\n  &:is(.dark *) {\n    @slot;\n  }');
    expect(css).toContain('html[data-rook-rail="expanded"] & {');
    expect(css).toMatch(
      /@utility container \{\n {2}margin-inline: auto;\n {2}padding-inline: 2rem;/,
    );
  });
});

describe('Tailwind v4 legacy colour names', () => {
  const css = buildTailwindV4CompatCss();
  const legacy = flatten(monarchPreset.theme.extend.colors as Tree).filter(
    (n) => !n.startsWith('mn-'),
  );

  it('covers every legacy colour of the Tailwind 3 preset', () => {
    expect(balanced(css)).toBe(true);
    for (const name of legacy)
      expect(css, name).toMatch(new RegExp(`--color-${name}: hsl\\(var\\(--`));
  });

  it('reads only variables that css/compat.css declares', () => {
    const compat = buildCompatCss(defaultAccent);
    for (const m of css.matchAll(/var\((--[\w-]+)\)/g)) {
      expect(compat, m[1]).toContain(`${m[1]}: `);
    }
  });
});
