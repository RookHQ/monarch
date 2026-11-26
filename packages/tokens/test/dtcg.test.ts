import { describe, expect, it } from 'vitest';
import { toHex } from '../src/color.js';
import { themeDecls } from '../src/css.js';
import {
  DTCG_FILES,
  DTCG_VERSION,
  buildDtcgDocument,
  buildDtcgFiles,
  buildDtcgPrimitives,
  buildDtcgResolver,
  buildDtcgSemantic,
  isAlias,
  isColorValue,
  parseCssHsl,
  parseFontFamily,
  parseShadow,
  pathFor,
  resolveAlias,
  walkTokens,
  type DtcgColor,
  type DtcgGroup,
  type DtcgToken,
  type DtcgValue,
} from '../src/dtcg.js';
import { defaultAccent } from '../src/primitives.js';
import { THEME_IDS, resolveTheme, themes } from '../src/themes.js';

const primitives = buildDtcgPrimitives();
const semantic = Object.fromEntries(THEME_IDS.map((id) => [id, buildDtcgSemantic(id)]));
const doc = buildDtcgDocument();
const tokenMap = (...groups: DtcgGroup[]) =>
  new Map<string, DtcgToken>(groups.flatMap((g) => walkTokens(g)));

/** Every colour value in a document, including colours inside shadows. */
function colours(group: DtcgGroup): [string, DtcgColor][] {
  return walkTokens(group).flatMap(([path, t]): [string, DtcgColor][] => {
    const v: DtcgValue = t.$value;
    if (isColorValue(v)) return [[path, v]];
    if (t.$type === 'shadow' && Array.isArray(v)) {
      return (v as readonly { color: DtcgColor }[]).map((l, i) => [`${path}[${i}]`, l.color]);
    }
    return [];
  });
}

const allGroups: [string, DtcgGroup][] = [
  ['tokens.dtcg.json', doc],
  ['primitives', primitives],
  ...THEME_IDS.map((id) => [id, semantic[id] as DtcgGroup] as [string, DtcgGroup]),
];

describe('DTCG colour values', () => {
  it.each(allGroups)('%s: every colour has colorSpace, components and a matching hex', (_n, g) => {
    const list = colours(g);
    expect(list.length).toBeGreaterThan(10);
    for (const [path, c] of list) {
      expect(c.colorSpace, path).toBe('srgb');
      expect(c.components, path).toHaveLength(3);
      expect(c.hex, path).toMatch(/^#[0-9a-f]{6}$/);
      const bytes = c.components.map((v) => Math.round(v * 255));
      const fromHex = [1, 3, 5].map((i) => parseInt(c.hex.slice(i, i + 2), 16));
      expect(bytes, path).toEqual(fromHex);
      for (const v of c.components) expect(v >= 0 && v <= 1, path).toBe(true);
      if (c.alpha !== undefined) expect(c.alpha >= 0 && c.alpha < 1, path).toBe(true);
    }
  });
});

describe('DTCG aliases', () => {
  it('every alias in tokens.dtcg.json resolves inside the file', () => {
    const map = tokenMap(doc);
    const aliases = [...map].filter(([, t]) => isAlias(t.$value));
    expect(aliases.length).toBeGreaterThan(20);
    for (const [path, t] of aliases) expect(resolveAlias(t.$value, map), path).not.toBeNull();
  });

  it.each(THEME_IDS)('every alias in the %s set resolves against primitives + that set', (id) => {
    const map = tokenMap(primitives, semantic[id] as DtcgGroup);
    for (const [path, t] of walkTokens(semantic[id] as DtcgGroup)) {
      if (isAlias(t.$value)) expect(resolveAlias(t.$value, map), path).not.toBeNull();
    }
  });

  it('aliases neutral surfaces to the neutral primitives', () => {
    const light = tokenMap(semantic.light as DtcgGroup);
    expect(light.get('color.bg.inverse')?.$value).toBe('{color.neutral.900}');
    expect(light.get('color.text.link')?.$value).toBe('{color.accent.strong}');
  });

  it.each(THEME_IDS)('%s: every semantic colour resolves to the colour monarch.css uses', (id) => {
    const map = tokenMap(primitives, semantic[id] as DtcgGroup);
    let checked = 0;
    for (const [name, value] of themeDecls(resolveTheme(themes[id], defaultAccent))) {
      const parsed = name.startsWith('--mn-color-') ? parseCssHsl(value) : null;
      if (!parsed) continue;
      const token = map.get(pathFor(name).join('.'));
      expect(token, name).toBeDefined();
      const resolved = resolveAlias(token?.$value ?? '{missing}', map);
      expect(resolved !== null && isColorValue(resolved) ? resolved.hex : null, name).toBe(
        toHex(parsed.color),
      );
      checked++;
    }
    expect(checked).toBeGreaterThan(80);
  });

  it('every theme defines the same semantic tokens', () => {
    const paths = (id: (typeof THEME_IDS)[number]) =>
      walkTokens(semantic[id] as DtcgGroup).map(([p]) => p);
    for (const id of THEME_IDS) expect(paths(id)).toEqual(paths('light'));
  });
});

describe('DTCG other types', () => {
  const tokens = walkTokens(doc);

  it('font families are arrays of names without var()', () => {
    const families = tokens.filter(([, t]) => t.$type === 'fontFamily');
    expect(families.length).toBe(2);
    for (const [path, t] of families) {
      expect(Array.isArray(t.$value), path).toBe(true);
      expect(JSON.stringify(t.$value), path).not.toContain('var(');
    }
    expect(parseFontFamily("var(--font-poppins, 'Poppins'), ui-sans-serif, sans-serif")).toEqual([
      'Poppins',
      'ui-sans-serif',
      'sans-serif',
    ]);
  });

  it('dimensions, durations, easings and numbers have the spec shapes', () => {
    for (const [path, t] of tokens) {
      const v = t.$value;
      if (isAlias(v)) continue;
      if (t.$type === 'dimension')
        expect(v, path).toMatchObject({ unit: expect.stringMatching(/^(px|rem)$/) });
      if (t.$type === 'duration')
        expect(v, path).toMatchObject({ unit: 'ms', value: expect.any(Number) });
      if (t.$type === 'cubicBezier') {
        expect(Array.isArray(v) && v.length === 4, path).toBe(true);
        const [x1, , x2] = v as readonly number[];
        expect((x1 ?? -1) >= 0 && (x1 ?? 2) <= 1 && (x2 ?? -1) >= 0 && (x2 ?? 2) <= 1, path).toBe(
          true,
        );
      }
      if (t.$type === 'number' || t.$type === 'fontWeight') expect(typeof v, path).toBe('number');
    }
    const map = tokenMap(doc);
    expect(map.get('space.1')?.$value).toEqual({ value: 0.25, unit: 'rem' });
    expect(map.get('space.0-5')?.$value).toEqual({ value: 0.125, unit: 'rem' });
    expect(map.get('duration.fast')?.$value).toEqual({ value: 120, unit: 'ms' });
    expect(map.get('easing.standard')?.$value).toEqual([0.2, 0, 0, 1]);
  });

  it('shadows are layer objects, and "none" is a transparent layer', () => {
    const md = parseShadow(
      '0 4px 12px -2px hsl(222 30% 10% / 0.08), 0 2px 4px -2px hsl(222 30% 10% / 0.05)',
    );
    expect(md).toHaveLength(2);
    expect(md[0]).toMatchObject({
      offsetX: { value: 0, unit: 'px' },
      offsetY: { value: 4, unit: 'px' },
      blur: { value: 12, unit: 'px' },
      spread: { value: -2, unit: 'px' },
      color: { alpha: 0.08 },
    });
    expect(parseShadow('none')[0]?.color.alpha).toBe(0);
  });

  it('token names never contain ".", "{" or "}"', () => {
    for (const [path] of tokens) {
      for (const key of path.split('.')) expect(key, path).toMatch(/^[^.{}$][^.{}]*$/);
    }
  });
});

describe('DTCG resolver', () => {
  const resolver = buildDtcgResolver();
  const files = buildDtcgFiles();

  it('is a 2025.10 resolver with a theme modifier for the four themes', () => {
    expect(resolver.version).toBe(DTCG_VERSION);
    expect(Object.keys(resolver.modifiers.theme?.contexts ?? {})).toEqual([...THEME_IDS]);
    expect(resolver.modifiers.theme?.default).toBe('light');
  });

  it('references only files the build emits', () => {
    const refs = [
      ...Object.values(resolver.sets).flatMap((s) => s.sources),
      ...Object.values(resolver.modifiers).flatMap((m) => Object.values(m.contexts).flat()),
    ].map((r) => r.$ref);
    expect(refs).toContain(DTCG_FILES.primitives);
    for (const ref of refs) expect(files.has(ref), ref).toBe(true);
  });

  it('resolution order points at sets and modifiers it defines', () => {
    for (const { $ref } of resolver.resolutionOrder) {
      const [, kind, name] = /^#\/(sets|modifiers)\/(.+)$/.exec($ref) ?? [];
      const target = kind === 'sets' ? resolver.sets : resolver.modifiers;
      expect(name !== undefined && name in target, $ref).toBe(true);
    }
  });

  it('emits the single-file document and every theme set', () => {
    expect(files.has('tokens.dtcg.json')).toBe(true);
    expect(files.has('tokens.resolver.json')).toBe(true);
    for (const id of THEME_IDS) expect(files.has(DTCG_FILES.semantic(id))).toBe(true);
  });
});
