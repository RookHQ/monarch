/**
 * Figma Variables, as the request body of Figma's REST endpoint
 * `POST /v1/files/:file_key/variables`.
 *
 * Two collections:
 * - `Primitives` (one mode, `Value`): every primitive colour, plus space,
 *   radius and control heights as numbers in px. Hidden from publishing, so
 *   designers pick semantic variables.
 * - `Semantic` (modes `Light`, `Dark`, `HC Light`, `HC Dark`): every semantic
 *   colour. A value aliases a primitive (or another semantic variable)
 *   wherever the DTCG files do, so a change to a primitive flows through.
 *
 * Built from the DTCG builders, so Figma, DTCG and CSS cannot drift. IDs are
 * temporary IDs, which Figma replaces on import; the body creates variables,
 * so post it once into a file that does not have them yet.
 */
import {
  buildDtcgPrimitives,
  buildDtcgSemantic,
  cssVarFor,
  isAlias,
  isColorValue,
  isDimensionValue,
  walkTokens,
  type DtcgColor,
  type DtcgDimension,
  type DtcgToken,
} from './dtcg.js';
import { THEME_IDS, type ThemeId } from './themes.js';

export interface FigmaRgba {
  readonly r: number;
  readonly g: number;
  readonly b: number;
  readonly a: number;
}
export interface FigmaAlias {
  readonly type: 'VARIABLE_ALIAS';
  readonly id: string;
}
export type FigmaScope =
  | 'ALL_SCOPES'
  | 'FRAME_FILL'
  | 'SHAPE_FILL'
  | 'TEXT_FILL'
  | 'STROKE_COLOR'
  | 'EFFECT_COLOR'
  | 'CORNER_RADIUS'
  | 'WIDTH_HEIGHT'
  | 'GAP';

export interface FigmaCollectionChange {
  readonly action: 'CREATE';
  readonly id: string;
  readonly name: string;
  readonly initialModeId: string;
  readonly hiddenFromPublishing: boolean;
}
export interface FigmaModeChange {
  readonly action: 'CREATE' | 'UPDATE';
  readonly id: string;
  readonly name: string;
  readonly variableCollectionId: string;
}
export interface FigmaVariableChange {
  readonly action: 'CREATE';
  readonly id: string;
  readonly name: string;
  readonly variableCollectionId: string;
  readonly resolvedType: 'COLOR' | 'FLOAT';
  readonly scopes: readonly FigmaScope[];
  readonly codeSyntax?: { readonly WEB: string };
}
export interface FigmaModeValue {
  readonly variableId: string;
  readonly modeId: string;
  readonly value: FigmaRgba | FigmaAlias | number;
}
/** The JSON body for `POST /v1/files/:file_key/variables`. */
export interface FigmaVariablesBody {
  readonly variableCollections: readonly FigmaCollectionChange[];
  readonly variableModes: readonly FigmaModeChange[];
  readonly variables: readonly FigmaVariableChange[];
  readonly variableModeValues: readonly FigmaModeValue[];
}

export const FIGMA_COLLECTIONS = { primitives: 'Primitives', semantic: 'Semantic' } as const;
export const FIGMA_MODES: Readonly<Record<ThemeId, string>> = {
  light: 'Light',
  dark: 'Dark',
  'hc-light': 'HC Light',
  'hc-dark': 'HC Dark',
};

const ROOT_PX = 16;
// Temporary IDs: letters, digits, `_` and `-` only, so they never look like
// Figma's own `VariableID:1:2` IDs. Token keys never contain `_`, so mapping
// `.` to `_` cannot make two paths collide.
const COLLECTION = { primitives: 'collection_primitives', semantic: 'collection_semantic' };
const PRIMITIVE_MODE = 'mode_primitives';
const modeId = (id: ThemeId) => `mode_${id}`;
const variableId = (set: 'p' | 's', path: string) => `variable_${set}_${path.replace(/\./g, '_')}`;
/** Figma groups by `/`; names cannot contain `.`. */
const figmaName = (path: string) => path.replace(/\./g, '/');

const rgba = (c: DtcgColor): FigmaRgba => ({
  r: c.components[0],
  g: c.components[1],
  b: c.components[2],
  a: c.alpha ?? 1,
});
const px = (d: DtcgDimension) => (d.unit === 'rem' ? d.value * ROOT_PX : d.value);

function colorScopes(path: string): FigmaScope[] {
  const groupName = path.split('.')[1];
  if (groupName === 'bg') return ['FRAME_FILL', 'SHAPE_FILL'];
  if (groupName === 'text') return ['TEXT_FILL'];
  if (groupName === 'border') return ['STROKE_COLOR'];
  return ['ALL_SCOPES'];
}

const NUMBER_SCOPES: Readonly<Record<string, readonly FigmaScope[]>> = {
  space: ['GAP', 'WIDTH_HEIGHT'],
  radius: ['CORNER_RADIUS'],
  control: ['WIDTH_HEIGHT'],
};

export function buildFigmaVariables(): FigmaVariablesBody {
  const variables: FigmaVariableChange[] = [];
  const values: FigmaModeValue[] = [];

  const primitives = new Map<string, DtcgToken>(walkTokens(buildDtcgPrimitives()));
  for (const [path, token] of primitives) {
    const root = path.split('.')[0] ?? '';
    const id = variableId('p', path);
    const base = {
      action: 'CREATE',
      id,
      name: figmaName(path),
      variableCollectionId: COLLECTION.primitives,
    } as const;
    const v = token.$value;
    const scopes = NUMBER_SCOPES[root];
    if (isColorValue(v)) {
      variables.push({ ...base, resolvedType: 'COLOR', scopes: ['ALL_SCOPES'] });
      values.push({ variableId: id, modeId: PRIMITIVE_MODE, value: rgba(v) });
    } else if (isDimensionValue(v) && scopes) {
      variables.push({ ...base, resolvedType: 'FLOAT', scopes: [...scopes] });
      values.push({ variableId: id, modeId: PRIMITIVE_MODE, value: px(v) });
    }
  }
  const primitiveIds = new Set(variables.map((v) => v.id));

  const themesTokens = THEME_IDS.map(
    (id) => [id, new Map<string, DtcgToken>(walkTokens(buildDtcgSemantic(id)))] as const,
  );
  const [, first] = themesTokens[0] ?? [];
  const colorPaths = [...(first?.keys() ?? [])].filter(
    (path) => first?.get(path)?.$type === 'color',
  );
  for (const path of colorPaths) {
    variables.push({
      action: 'CREATE',
      id: variableId('s', path),
      name: figmaName(path),
      variableCollectionId: COLLECTION.semantic,
      resolvedType: 'COLOR',
      scopes: colorScopes(path),
      codeSyntax: { WEB: `var(${cssVarFor(path)})` },
    });
  }
  for (const [theme, tokens] of themesTokens) {
    for (const path of colorPaths) {
      const token = tokens.get(path);
      if (!token) throw new Error(`${path} is missing in the ${theme} theme`);
      const v = token.$value;
      let value: FigmaModeValue['value'];
      if (isAlias(v)) {
        const target = v.slice(1, -1);
        const id = primitiveIds.has(variableId('p', target))
          ? variableId('p', target)
          : variableId('s', target);
        value = { type: 'VARIABLE_ALIAS', id };
      } else if (isColorValue(v)) {
        value = rgba(v);
      } else {
        throw new Error(`${path} in the ${theme} theme is not a colour`);
      }
      values.push({ variableId: variableId('s', path), modeId: modeId(theme), value });
    }
  }

  return {
    variableCollections: [
      {
        action: 'CREATE',
        id: COLLECTION.primitives,
        name: FIGMA_COLLECTIONS.primitives,
        initialModeId: PRIMITIVE_MODE,
        hiddenFromPublishing: true,
      },
      {
        action: 'CREATE',
        id: COLLECTION.semantic,
        name: FIGMA_COLLECTIONS.semantic,
        initialModeId: modeId('light'),
        hiddenFromPublishing: false,
      },
    ],
    variableModes: [
      {
        action: 'UPDATE',
        id: PRIMITIVE_MODE,
        name: 'Value',
        variableCollectionId: COLLECTION.primitives,
      },
      ...THEME_IDS.map((id): FigmaModeChange => ({
        action: id === 'light' ? 'UPDATE' : 'CREATE',
        id: modeId(id),
        name: FIGMA_MODES[id],
        variableCollectionId: COLLECTION.semantic,
      })),
    ],
    variables,
    variableModeValues: values,
  };
}
