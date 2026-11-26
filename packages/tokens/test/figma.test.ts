import { describe, expect, it } from 'vitest';
import { buildDtcgSemantic, walkTokens } from '../src/dtcg.js';
import { FIGMA_MODES, buildFigmaVariables } from '../src/figma.js';
import { THEME_IDS } from '../src/themes.js';

describe('Figma variables (REST POST /v1/files/:file_key/variables body)', () => {
  const body = buildFigmaVariables();
  const byName = new Map(body.variableCollections.map((c) => [c.name, c]));
  const primitives = byName.get('Primitives');
  const semantic = byName.get('Semantic');
  const variables = new Map(body.variables.map((v) => [v.id, v]));

  it('has exactly the Primitives and Semantic collections', () => {
    expect([...byName.keys()]).toEqual(['Primitives', 'Semantic']);
    expect(primitives?.hiddenFromPublishing).toBe(true);
  });

  it('gives Semantic the four theme modes and Primitives one mode', () => {
    const modesOf = (id: string | undefined) =>
      body.variableModes.filter((m) => m.variableCollectionId === id);
    expect(modesOf(semantic?.id).map((m) => m.name)).toEqual([
      'Light',
      'Dark',
      'HC Light',
      'HC Dark',
    ]);
    expect(modesOf(primitives?.id)).toHaveLength(1);
    // The initial mode of each collection is renamed (UPDATE), the rest created.
    for (const c of body.variableCollections) {
      const initial = body.variableModes.find((m) => m.id === c.initialModeId);
      expect(initial?.action, c.name).toBe('UPDATE');
      expect(initial?.variableCollectionId).toBe(c.id);
    }
    expect(Object.values(FIGMA_MODES)).toEqual(['Light', 'Dark', 'HC Light', 'HC Dark']);
  });

  it('has unique IDs and names, without "." in names', () => {
    expect(variables.size).toBe(body.variables.length);
    const names = body.variables.map((v) => `${v.variableCollectionId} ${v.name}`);
    expect(new Set(names).size).toBe(names.length);
    for (const v of body.variables) {
      expect(v.name).not.toMatch(/[.{}]/);
      expect(v.id).toMatch(/^[\w-]+$/);
    }
  });

  it('gives every semantic variable one value in each of the four modes', () => {
    const semanticVars = body.variables.filter((v) => v.variableCollectionId === semantic?.id);
    const tokens = walkTokens(buildDtcgSemantic('light')).filter(([, t]) => t.$type === 'color');
    expect(semanticVars).toHaveLength(tokens.length);
    for (const v of semanticVars) {
      const modes = body.variableModeValues
        .filter((mv) => mv.variableId === v.id)
        .map((mv) => mv.modeId);
      expect(modes.sort(), v.name).toEqual(THEME_IDS.map((id) => `mode_${id}`).sort());
      expect(v.codeSyntax?.WEB, v.name).toMatch(/^var\(--mn-color-[\w-]+\)$/);
    }
  });

  it('gives every primitive variable one value', () => {
    for (const v of body.variables.filter((x) => x.variableCollectionId === primitives?.id)) {
      const values = body.variableModeValues.filter((mv) => mv.variableId === v.id);
      expect(
        values.map((mv) => mv.modeId),
        v.name,
      ).toEqual([primitives?.initialModeId]);
    }
  });

  it('aliases point at existing variables, and semantic values alias primitives where possible', () => {
    let aliases = 0;
    for (const mv of body.variableModeValues) {
      expect(variables.has(mv.variableId)).toBe(true);
      const value = mv.value;
      if (typeof value === 'object' && 'type' in value) {
        aliases++;
        expect(variables.has(value.id), value.id).toBe(true);
      } else if (typeof value === 'object') {
        for (const c of [value.r, value.g, value.b, value.a]) expect(c >= 0 && c <= 1).toBe(true);
        expect(variables.get(mv.variableId)?.resolvedType).toBe('COLOR');
      } else {
        expect(variables.get(mv.variableId)?.resolvedType).toBe('FLOAT');
      }
    }
    expect(aliases).toBeGreaterThan(50);
    const canvas = body.variableModeValues.find(
      (mv) => mv.variableId === 'variable_s_color_bg_canvas' && mv.modeId === 'mode_light',
    );
    expect(canvas?.value).toEqual({ type: 'VARIABLE_ALIAS', id: 'variable_p_color_neutral_25' });
  });

  it('converts rem to px for number variables', () => {
    const space4 = body.variableModeValues.find((mv) => mv.variableId === 'variable_p_space_4');
    expect(space4?.value).toBe(16);
  });
});
