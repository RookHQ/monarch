/**
 * `monarch figma-connect`: writes Figma Code Connect files
 * (`<Component>.figma.tsx`) from the component manifest and a map of
 * component name → Figma node URL that the app's team supplies.
 *
 * Monarch does not publish a Figma library, so the URLs must point at the
 * team's own Figma components. The generated files are a starting point: the
 * Figma property names are guessed from the React prop names (Title Case) and
 * should be reviewed before `figma connect publish`.
 */
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import {
  findExport,
  moduleOf,
  type Manifest,
  type ManifestExport,
  type ManifestProp,
} from './manifest.js';

export interface FigmaMapEntry {
  readonly component: string;
  readonly url: string;
  /** Optional overrides: manifest prop name → Figma property name. */
  readonly props?: Readonly<Record<string, string>>;
  /** Optional overrides: manifest prop name → { prop value → Figma option label }. */
  readonly values?: Readonly<Record<string, Readonly<Record<string, string>>>>;
}

export interface FigmaMap {
  readonly components: readonly FigmaMapEntry[];
}

export class FigmaConnectError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FigmaConnectError';
  }
}

export interface FigmaConnectResult {
  /** File names written, relative to the output folder, sorted. */
  readonly written: readonly string[];
  readonly skipped: readonly { component: string; reason: string }[];
}

const FIGMA_HOSTS = new Set(['www.figma.com', 'figma.com']);
const IDENTIFIER = /^[A-Za-z_$][\w$]*$/;
const PLACEHOLDER = /[<>{}]|%3C|%3E|%7B|%7D|FILE_KEY|FILE_NAME|NODE_ID|\bYOUR[_-]/i;
/** Figma node ids: `12-34`, `12:34`, or an instance path such as `I5:6;7:8`. */
const NODE_ID = /^I?\d+[-:]\d+(?:;\d+[-:]\d+)*$/;
/** Props that never map to a Figma property. */
const LEFT_OUT = new Set(['className', 'style', 'ref', 'key', 'id']);

type Json = Readonly<Record<string, unknown>>;

const isRecord = (value: unknown): value is Json =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

/** Why a URL cannot be used as a Figma node URL, or undefined when it can. */
export function figmaUrlProblem(url: string): string | undefined {
  if (PLACEHOLDER.test(url)) {
    return 'is a placeholder. Replace it with the link Figma gives you: select the component, then "Copy link to selection"';
  }
  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return 'is not a URL';
  }
  if (parsed.protocol !== 'https:' || !FIGMA_HOSTS.has(parsed.hostname)) {
    return 'must start with https://www.figma.com/ or https://figma.com/';
  }
  if (!/^\/(design|file)\/[A-Za-z0-9]+(\/|$)/.test(parsed.pathname)) {
    return 'must be a Figma design or file URL (https://www.figma.com/design/…)';
  }
  const nodeId = parsed.searchParams.get('node-id');
  if (!nodeId) return 'has no node-id. Copy the link to the component itself, not to the file';
  if (!NODE_ID.test(nodeId)) return `has a node-id that is not a Figma node id ("${nodeId}")`;
  return undefined;
}

function stringRecord(value: unknown, where: string): Readonly<Record<string, string>> {
  if (!isRecord(value)) throw new FigmaConnectError(`${where} must be an object`);
  for (const [key, v] of Object.entries(value)) {
    if (typeof v !== 'string' || v.trim() === '') {
      throw new FigmaConnectError(`${where}.${key} must be a non-empty string`);
    }
  }
  return value as Readonly<Record<string, string>>;
}

function valuesRecord(value: unknown, where: string): NonNullable<FigmaMapEntry['values']> {
  if (!isRecord(value)) throw new FigmaConnectError(`${where} must be an object`);
  const out: Record<string, Readonly<Record<string, string>>> = {};
  for (const [prop, labels] of Object.entries(value)) {
    out[prop] = stringRecord(labels, `${where}.${prop}`);
  }
  return out;
}

function parseEntry(value: unknown, index: number): FigmaMapEntry {
  const where = `components[${index}]`;
  if (!isRecord(value)) throw new FigmaConnectError(`${where} must be an object`);
  const { component, url, props, values } = value;
  if (typeof component !== 'string' || component.trim() === '') {
    throw new FigmaConnectError(`${where}.component must be a component name`);
  }
  if (typeof url !== 'string') throw new FigmaConnectError(`${where}.url must be a string`);
  const problem = figmaUrlProblem(url);
  if (problem) throw new FigmaConnectError(`${where}.url (${component}) ${problem}.`);
  return {
    component,
    url,
    ...(props === undefined ? {} : { props: stringRecord(props, `${where}.props`) }),
    ...(values === undefined ? {} : { values: valuesRecord(values, `${where}.values`) }),
  };
}

/** Validates the parsed JSON of a map file. Throws FigmaConnectError. */
export function parseFigmaMap(value: unknown): FigmaMap {
  if (!isRecord(value) || !Array.isArray(value.components)) {
    throw new FigmaConnectError('The map must be an object with a "components" array.');
  }
  const components = (value.components as readonly unknown[]).map(parseEntry);
  const seen = new Set<string>();
  for (const entry of components) {
    if (seen.has(entry.component)) {
      throw new FigmaConnectError(`"${entry.component}" is listed more than once.`);
    }
    seen.add(entry.component);
  }
  return { components };
}

/** `leadingIcon` → `Leading Icon`; `danger-subtle` → `Danger Subtle`. */
export function titleCase(name: string): string {
  return name
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .split(/[\s_-]+/)
    .filter((word) => word !== '')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/** A single-quoted JavaScript string literal. */
function quote(text: string): string {
  const body = text
    .replace(/[\\']/g, '\\$&')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
  return `'${body}'`;
}

const objectKey = (key: string): string => (IDENTIFIER.test(key) ? key : quote(key));

const plainType = (type: string): string => type.replace(/\s*\|\s*undefined\b/g, '').trim();

const isNode = (type: string): boolean => /\bReact(Node|Element)\b/.test(type);

/** String option values of a prop, or undefined when it is not a string enum. */
function enumValues(prop: ManifestProp): readonly string[] | undefined {
  const values = prop.values;
  if (!values || values.length === 0) return undefined;
  if (!values.every((v): v is string => typeof v === 'string' && v !== '')) return undefined;
  return [...values].sort();
}

const figmaName = (prop: ManifestProp, entry: FigmaMapEntry): string =>
  entry.props?.[prop.name] ?? titleCase(prop.name);

function enumHelper(prop: ManifestProp, values: readonly string[], entry: FigmaMapEntry): string {
  const overrides: Readonly<Record<string, string>> = entry.values?.[prop.name] ?? {};
  const used = new Set<string>();
  const pairs = values.map((value) => {
    const wanted = overrides[value] ?? titleCase(value);
    // Two values with the same label would be a duplicate key: keep the raw value.
    const label = wanted === '' || used.has(wanted) ? value : wanted;
    used.add(label);
    return `${objectKey(label)}: ${quote(value)}`;
  });
  return `figma.enum(${quote(figmaName(prop, entry))}, { ${pairs.join(', ')} })`;
}

function isLeftOut(name: string): boolean {
  return LEFT_OUT.has(name) || !IDENTIFIER.test(name) || /^on[A-Z]/.test(name);
}

/** The `figma.*` helper call for one prop, or undefined when the prop is left out. */
function helperFor(prop: ManifestProp, entry: FigmaMapEntry): string | undefined {
  if (isLeftOut(prop.name)) return undefined;
  const type = plainType(prop.type);
  if (prop.name === 'children' && isNode(type)) return `figma.children('*')`;
  const values = enumValues(prop);
  if (values) return enumHelper(prop, values, entry);
  if (type === 'boolean') return `figma.boolean(${quote(figmaName(prop, entry))})`;
  if (type === 'string') return `figma.string(${quote(figmaName(prop, entry))})`;
  return undefined;
}

interface MappedProp {
  readonly name: string;
  readonly helper: string;
}

/** Mapped props, sorted by name with `children` last. */
function mappedProps(e: ManifestExport, entry: FigmaMapEntry): MappedProp[] {
  const byName = new Map<string, MappedProp>();
  for (const prop of e.props) {
    const helper = helperFor(prop, entry);
    if (helper && !byName.has(prop.name)) byName.set(prop.name, { name: prop.name, helper });
  }
  const rank = (name: string): number => (name === 'children' ? 1 : 0);
  return [...byName.values()].sort(
    (a, b) => rank(a.name) - rank(b.name) || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0),
  );
}

function exampleOf(name: string, props: readonly MappedProp[]): string {
  const attrs = props
    .filter((p) => p.name !== 'children')
    .map((p) => ` ${p.name}={${p.name}}`)
    .join('');
  const hasChildren = props.some((p) => p.name === 'children');
  const element = hasChildren ? `<${name}${attrs}>{children}</${name}>` : `<${name}${attrs} />`;
  const args = props.length > 0 ? `{ ${props.map((p) => p.name).join(', ')} }` : '';
  return `(${args}) => ${element}`;
}

/** The text of `<Component>.figma.tsx` for one manifest entry. */
export function codeConnectSource(e: ManifestExport, entry: FigmaMapEntry): string {
  if (!IDENTIFIER.test(e.name)) {
    throw new FigmaConnectError(`"${e.name}" is not a name that can be imported.`);
  }
  const props = mappedProps(e, entry);
  const propLines = props.map((p) => `    ${p.name}: ${p.helper},`);
  const propsBlock = props.length > 0 ? ['  props: {', ...propLines, '  },'] : [];
  return [
    '// Generated by `monarch figma-connect` from the Monarch component manifest.',
    '// A starting point: check each Figma property name against your Figma component.',
    `import figma from '@figma/code-connect';`,
    `import { ${e.name} } from ${quote(moduleOf(e.entry))};`,
    '',
    `figma.connect(${e.name}, ${quote(entry.url)}, {`,
    ...propsBlock,
    `  example: ${exampleOf(e.name, props)},`,
    '});',
    '',
  ].join('\n');
}

type Outcome = { readonly file: string; readonly source: string } | { readonly reason: string };

function plan(manifest: Manifest, entry: FigmaMapEntry, seen: ReadonlySet<string>): Outcome {
  const problem = figmaUrlProblem(entry.url);
  if (problem) return { reason: `the URL ${problem}` };
  const e = findExport(manifest, entry.component);
  if (!e) return { reason: 'not in the component manifest' };
  if (e.kind !== 'component') return { reason: `is a ${e.kind}, not a component` };
  if (!IDENTIFIER.test(e.name)) return { reason: 'is not a name that can be imported' };
  if (seen.has(e.name)) return { reason: 'listed more than once' };
  return { file: `${e.name}.figma.tsx`, source: codeConnectSource(e, entry) };
}

/** Writes one Code Connect file per mapped component into `outDir`. */
export function generateFigmaConnect(
  manifest: Manifest,
  map: FigmaMap,
  outDir: string,
  opts: { readonly force?: boolean } = {},
): FigmaConnectResult {
  const written: string[] = [];
  const skipped: { component: string; reason: string }[] = [];
  const seen = new Set<string>();
  const entries = [...map.components].sort((a, b) =>
    a.component < b.component ? -1 : a.component > b.component ? 1 : 0,
  );
  for (const entry of entries) {
    const outcome = plan(manifest, entry, seen);
    if ('reason' in outcome) {
      skipped.push({ component: entry.component, reason: outcome.reason });
      continue;
    }
    seen.add(outcome.file.replace(/\.figma\.tsx$/, ''));
    if (existsSync(join(outDir, outcome.file)) && !opts.force) {
      skipped.push({
        component: entry.component,
        reason: `${outcome.file} exists (pass --force to overwrite)`,
      });
      continue;
    }
    mkdirSync(outDir, { recursive: true });
    writeFileSync(join(outDir, outcome.file), outcome.source);
    written.push(outcome.file);
  }
  return { written: written.sort(), skipped };
}
