#!/usr/bin/env node
/**
 * Cross-platform scenario fixtures (CROSS-PLATFORM.md section 8, items 1 and 2).
 *
 * One JSON per component, next to its web source: packages/react/src/**\/<id>.fixture.json. It is the
 * scenario of the web primary example (the first item of `sections.examples` in <id>.docs.tsx, which reads
 * it): the demo data and copy in `data`, and what a person sees, in `texts` (reading order), `tones` (the
 * `data-tone` values in DOM order), `icons` (web glyph names, keys of platforms/icons.json), `actions` and
 * `states`. The web test packages/react/test/parity-fixtures.test.tsx checks the web against it.
 *
 * The first example of platforms/{ios,android}/inventory/<id>.json binds the fixture with
 * `"fixture": "<id>"` and `"webExample": "<web title>"`, and is written as a `template` (and an optional
 * `setupTemplate`) in which `{{path}}` stands for a value of `data`. This script writes the example's `code`
 * and `setup` from them, so the docs, the compiled examples and the previews show the fixture's values and
 * nothing is typed twice. Placeholders: `{{a.b.0.c}}` is the value as a Swift or Kotlin literal; filters
 * after a bar: `tone` (.warning / MonarchTone.Warning), `case` (Swift enum case: .noLimit), `pascal`
 * (Kotlin enum entry: NoLimit), `double` (0.8, 5.0), `millis` (an ISO date as epoch milliseconds, Kotlin
 * Long), `date` (an ISO date as a Swift Date), `raw` (a string without quotes).
 *
 * Every value of `data` must be used by each platform's template, and items of a list in their order,
 * unless the entry has a `parityExceptions` item `{ rule: "fixture-data", platform, paths: [...], reason,
 * convention }` that names the path.
 *
 * It also writes the fixtures as Swift and Kotlin constants for the native tests and previews:
 *   platforms/ios/Tests/MonarchUITests/Generated/Fixtures/ParityFixtures.generated.swift
 *   platforms/android/monarch-compose/src/test/kotlin/dev/monarchui/fixtures/ParityFixtures.kt
 *
 * Usage: node scripts/native/fixtures.mjs [--check]
 *   --check  write nothing; fail when an inventory example or a generated file is stale, or on any problem.
 * After a write, run platforms/ios/Scripts/generate.py and ./gradlew generateInventory for components.json.
 */
import { existsSync, readFileSync, readdirSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const check = process.argv.includes('--check');
const PLATFORMS = ['ios', 'android'];
const TONES = new Set(['neutral', 'info', 'success', 'warning', 'danger', 'accent', 'ai']);
const problems = [];
const stale = [];
const problem = (where, text) => problems.push(`${where}: ${text}`);

const read = (path) => readFileSync(path, 'utf8');
const rel = (path) => relative(root, path);

// --- Fixtures ----------------------------------------------------------------------------------

function fixtureFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return fixtureFiles(path);
    return entry.name.endsWith('.fixture.json') ? [path] : [];
  });
}

const icons = JSON.parse(read(join(root, 'platforms', 'icons.json')));
const iconByGlyph = new Map(icons.icons.map((i) => [i.glyph, i]));

const fixtures = new Map();
for (const file of fixtureFiles(join(root, 'packages', 'react', 'src')).sort()) {
  const where = rel(file);
  let fixture;
  try {
    fixture = JSON.parse(read(file));
  } catch (error) {
    problem(where, `not valid JSON: ${error.message}`);
    continue;
  }
  const id = file
    .split('/')
    .pop()
    .replace(/\.fixture\.json$/, '');
  if (fixture.id !== id) problem(where, `id must be "${id}", the file name`);
  if (typeof fixture.example !== 'string' || !fixture.example)
    problem(where, '"example" must be the title of the web primary example');
  if (typeof fixture.data !== 'object' || fixture.data === null || Array.isArray(fixture.data))
    problem(where, '"data" must be an object');
  if (!Array.isArray(fixture.texts) || !fixture.texts.every((t) => typeof t === 'string' && t))
    problem(where, '"texts" must be a list of the visible texts, in reading order');
  for (const tone of fixture.tones ?? [])
    if (!TONES.has(tone)) problem(where, `tone "${tone}" is not a Monarch tone`);
  for (const glyph of fixture.icons ?? [])
    if (!iconByGlyph.has(glyph))
      problem(where, `icon "${glyph}" is not a glyph of platforms/icons.json`);
  for (const action of fixture.actions ?? [])
    if (typeof action?.label !== 'string') problem(where, 'every action needs a "label"');
  const known = new Set([
    '$schema',
    'id',
    'example',
    'data',
    'texts',
    'tones',
    'icons',
    'actions',
    'states',
    'docsHarness',
  ]);
  for (const key of Object.keys(fixture))
    if (!known.has(key)) problem(where, `unknown field "${key}"`);
  // The web docs file must exist and import it.
  const docs = file.replace(/\.fixture\.json$/, '.docs.tsx');
  if (!existsSync(docs)) problem(where, `no ${rel(docs)}`);
  else if (!read(docs).includes(`./${id}.fixture.json`))
    problem(rel(docs), `the web primary example must read ./${id}.fixture.json`);
  fixtures.set(id, { ...fixture, file });
}

/** Every leaf of `data` as a dotted path, in document order. */
function leaves(value, prefix = '') {
  if (value !== null && typeof value === 'object') {
    return Object.entries(value).flatMap(([k, v]) => leaves(v, prefix ? `${prefix}.${k}` : k));
  }
  return [prefix];
}

function at(data, path) {
  let value = data;
  for (const key of path.split('.')) {
    if (value === null || typeof value !== 'object' || !(key in value)) return { missing: true };
    value = value[key];
  }
  return { value };
}

// --- Templates ---------------------------------------------------------------------------------

const PLACEHOLDER = /\{\{\s*([A-Za-z0-9_.]+)\s*(?:\|\s*(\w+)\s*)?\}\}/g;
const pascal = (s) =>
  String(s)
    .split(/[-_ ]+/)
    .filter(Boolean)
    .map((p) => p[0].toUpperCase() + p.slice(1))
    .join('');
const camel = (s) => {
  const p = pascal(s);
  return p[0].toLowerCase() + p.slice(1);
};
const SWIFT_KEYWORDS = new Set([
  'switch',
  'default',
  'case',
  'in',
  'is',
  'as',
  'if',
  'else',
  'for',
  'while',
  'return',
  'class',
  'struct',
  'enum',
  'protocol',
  'extension',
  'func',
  'var',
  'let',
  'import',
  'init',
  'self',
  'super',
  'where',
  'repeat',
  'do',
  'try',
  'throw',
  'true',
  'false',
  'nil',
  'static',
  'public',
  'private',
]);
/** A Swift identifier for a fixture id: keywords such as `switch` are written in backticks. */
const swiftName = (id) => (SWIFT_KEYWORDS.has(camel(id)) ? `\`${camel(id)}\`` : camel(id));
const swiftString = (s) =>
  `"${s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n')}"`;
const kotlinString = (s) =>
  `"${s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$').replace(/\n/g, '\\n')}"`;
const groupDigits = (n) => {
  const [whole, fraction] = String(n).split('.');
  const grouped = whole.length > 4 ? whole.replace(/\B(?=(\d{3})+(?!\d))/g, '_') : whole;
  return fraction === undefined ? grouped : `${grouped}.${fraction}`;
};

function literal(platform, value, filter, where) {
  const swift = platform === 'ios';
  switch (filter) {
    case undefined:
      if (typeof value === 'string') return swift ? swiftString(value) : kotlinString(value);
      if (typeof value === 'number') return groupDigits(value);
      if (typeof value === 'boolean') return String(value);
      if (value === null) return swift ? 'nil' : 'null';
      break;
    case 'raw':
      return String(value);
    case 'tone':
      if (!TONES.has(value)) break;
      return swift ? `.${value}` : `MonarchTone.${pascal(value)}`;
    case 'case':
      return `.${camel(value)}`;
    case 'pascal':
      return pascal(value);
    case 'double':
      if (typeof value !== 'number') break;
      return Number.isInteger(value) ? `${groupDigits(value)}.0` : String(value);
    case 'millis': {
      const ms = Date.parse(value);
      if (Number.isNaN(ms)) break;
      return `${groupDigits(ms)}L`;
    }
    case 'date': {
      const ms = Date.parse(value);
      if (Number.isNaN(ms)) break;
      return `Date(timeIntervalSince1970: ${groupDigits(ms / 1000)})`;
    }
    default:
      problem(where, `unknown filter "${filter}"`);
      return '';
  }
  problem(
    where,
    `cannot write ${JSON.stringify(value)} with ${filter ?? 'no filter'} for ${platform}`,
  );
  return '';
}

/** The template with every placeholder written out, and the paths it used with their first offset. */
function renderTemplate(platform, template, data, where) {
  const used = new Map();
  const text = template.replace(PLACEHOLDER, (match, path, filter, offset) => {
    const found = at(data, path);
    if (found.missing) {
      problem(where, `{{${path}}}: the fixture has no such value`);
      return match;
    }
    if (found.value !== null && typeof found.value === 'object') {
      problem(where, `{{${path}}}: a placeholder must name one value, not an object or a list`);
      return match;
    }
    if (!used.has(path)) used.set(path, offset);
    return literal(platform, found.value, filter, `${where}: {{${path}}}`);
  });
  return { text, used };
}

// --- Inventories -------------------------------------------------------------------------------

/** platform -> id -> { entry, example } of the bound examples. */
const bound = { ios: new Map(), android: new Map() };

for (const platform of PLATFORMS) {
  const folder = join(root, 'platforms', platform, 'inventory');
  for (const name of readdirSync(folder).sort()) {
    if (!name.endsWith('.json') || name === 'schema.json') continue;
    const path = join(folder, name);
    const where = `platforms/${platform}/inventory/${name}`;
    const before = read(path);
    const entry = JSON.parse(before);
    let changed = false;
    (entry.examples ?? []).forEach((example, index) => {
      const hasTemplate = example.template !== undefined || example.setupTemplate !== undefined;
      if (!example.fixture) {
        if (hasTemplate) problem(where, `example ${index}: a template needs "fixture"`);
        return;
      }
      const at0 = `${where} example ${index}`;
      if (index !== 0) problem(at0, 'only the first example (the primary one) binds a fixture');
      const fixture = fixtures.get(example.fixture);
      if (!fixture) {
        problem(
          at0,
          `no fixture "${example.fixture}" (packages/react/src/**/${example.fixture}.fixture.json)`,
        );
        return;
      }
      if (example.fixture !== entry.id)
        problem(at0, `binds fixture "${example.fixture}", not its own id`);
      if (example.webExample !== fixture.example)
        problem(at0, `"webExample" must be "${fixture.example}", the web primary example`);
      if (typeof example.template !== 'string') {
        problem(at0, 'a bound example needs a "template"');
        return;
      }
      bound[platform].set(entry.id, { entry, example });
      const setup = example.setupTemplate
        ? renderTemplate(platform, example.setupTemplate, fixture.data, `${at0} setupTemplate`)
        : null;
      const code = renderTemplate(platform, example.template, fixture.data, `${at0} template`);
      // Coverage and order: every value is shown, list items in their order.
      const offsets = new Map();
      const setupLength = example.setupTemplate?.length ?? 0;
      for (const [p, o] of setup?.used ?? []) offsets.set(p, o);
      for (const [p, o] of code.used) if (!offsets.has(p)) offsets.set(p, setupLength + 1 + o);
      const waived = new Set(
        (entry.parityExceptions ?? [])
          .filter((x) => x.rule === 'fixture-data' && (x.platform ?? platform) === platform)
          .flatMap((x) => x.paths ?? []),
      );
      for (const leaf of leaves(fixture.data)) {
        if (offsets.has(leaf)) continue;
        if ([...waived].some((w) => leaf === w || leaf.startsWith(`${w}.`))) continue;
        problem(
          at0,
          `does not show fixture value "${leaf}"; use {{${leaf}}} or add a parityExceptions item ` +
            '{ "rule": "fixture-data", "paths": [...] } with the reason',
        );
      }
      const lists = new Map();
      for (const [p, o] of offsets) {
        const m = /^(.*?)\.(\d+)(\.|$)/.exec(p);
        if (!m) continue;
        const key = m[1];
        const i = Number(m[2]);
        const list = lists.get(key) ?? new Map();
        list.set(i, Math.min(list.get(i) ?? Infinity, o));
        lists.set(key, list);
      }
      for (const [key, list] of lists) {
        const order = [...list.entries()].sort((a, b) => a[1] - b[1]).map(([i]) => i);
        if (order.some((v, i) => i > 0 && v < order[i - 1]))
          problem(
            at0,
            `shows the items of "${key}" in the order ${order.join(', ')}, not the fixture's`,
          );
      }
      if (setup && setup.text !== example.setup) {
        example.setup = setup.text;
        changed = true;
      }
      if (!setup && example.setup !== undefined) {
        delete example.setup;
        changed = true;
      }
      if (code.text !== example.code) {
        example.code = code.text;
        changed = true;
      }
    });
    if (changed) {
      if (check) stale.push(where);
      else {
        // Keep the field order: title, fixture, webExample, code, setup... are already in place.
        writeFileSync(path, JSON.stringify(entry, null, 2) + '\n');
        console.log(`fixtures: wrote ${where}`);
      }
    }
  }
}

for (const id of fixtures.keys()) {
  for (const platform of PLATFORMS) {
    if (!bound[platform].has(id))
      problem(
        rel(fixtures.get(id).file),
        `platforms/${platform}/inventory/${id}.json must bind it in its first example ("fixture": "${id}")`,
      );
  }
}

// --- Generated constants -----------------------------------------------------------------------

const ids = [...fixtures.keys()].filter((id) => PLATFORMS.every((p) => bound[p].has(id)));

function swiftFile() {
  const items = ids.map((id) => {
    const f = fixtures.get(id);
    const title = bound.ios.get(id).example.title;
    const strings = (list) => `[${list.map(swiftString).join(', ')}]`;
    const actions = (f.actions ?? [])
      .map(
        (a) =>
          `            ParityFixture.Action(label: ${swiftString(a.label)}, variant: ${swiftString(a.variant ?? '')}, size: ${swiftString(a.size ?? '')})`,
      )
      .join(',\n');
    return (
      `    /// ${rel(f.file)}\n` +
      `    static let ${swiftName(id)} = ParityFixture(\n` +
      `        id: ${swiftString(id)},\n` +
      `        webExample: ${swiftString(f.example)},\n` +
      `        exampleTitle: ${swiftString(title)},\n` +
      `        texts: ${strings(f.texts)},\n` +
      `        tones: ${strings(f.tones ?? [])},\n` +
      `        symbols: ${strings((f.icons ?? []).map((g) => iconByGlyph.get(g)?.ios ?? ''))},\n` +
      `        actions: [${actions ? `\n${actions}\n        ` : ''}]\n` +
      `    )\n`
    );
  });
  return (
    '// Generated by scripts/native/fixtures.mjs from packages/react/src/**/<id>.fixture.json. Do not edit.\n\n' +
    '/// The scenario of a web primary example, which the first inventory example of the same id renders\n' +
    '/// (CROSS-PLATFORM.md section 8): its visible texts in reading order, its tones, its SF Symbols and its\n' +
    '/// buttons.\n' +
    'struct ParityFixture: Sendable {\n' +
    '    struct Action: Sendable, Equatable {\n        let label: String\n        let variant: String\n        let size: String\n    }\n\n' +
    '    let id: String\n    let webExample: String\n    let exampleTitle: String\n    let texts: [String]\n' +
    '    let tones: [String]\n    let symbols: [String]\n    let actions: [Action]\n}\n\n' +
    '/// Every fixture.\nenum ParityFixtures {\n' +
    items.join('\n') +
    `\n    static let all: [ParityFixture] = [${ids.map(swiftName).join(', ')}]\n}\n`
  );
}

function kotlinFile() {
  const items = ids.map((id) => {
    const f = fixtures.get(id);
    const title = bound.android.get(id).example.title;
    const strings = (list) =>
      list.length ? `listOf(${list.map(kotlinString).join(', ')})` : 'emptyList()';
    const actions = (f.actions ?? [])
      .map(
        (a) =>
          `            ParityFixture.Action(label = ${kotlinString(a.label)}, variant = ${kotlinString(a.variant ?? '')}, size = ${kotlinString(a.size ?? '')}),`,
      )
      .join('\n');
    return (
      `    /** ${rel(f.file)} */\n` +
      `    val ${pascal(id)}: ParityFixture by lazy {\n    ParityFixture(\n` +
      `        id = ${kotlinString(id)},\n` +
      `        webExample = ${kotlinString(f.example)},\n` +
      `        exampleTitle = ${kotlinString(title)},\n` +
      `        texts = ${strings(f.texts)},\n` +
      `        tones = ${strings(f.tones ?? [])},\n` +
      `        glyphs = ${strings((f.icons ?? []).map((g) => iconByGlyph.get(g)?.android ?? ''))},\n` +
      `        actions = ${actions ? `listOf(\n${actions}\n        )` : 'emptyList()'},\n` +
      `    )\n    }\n`
    );
  });
  return (
    '// Generated by scripts/native/fixtures.mjs from packages/react/src/**/<id>.fixture.json. Do not edit.\n' +
    'package dev.monarchui.fixtures\n\n' +
    '/**\n * The scenario of a web primary example, which the first inventory example of the same id renders\n' +
    ' * (CROSS-PLATFORM.md section 8): its visible texts in reading order, its tones, its Material Symbols\n * and its buttons.\n */\n' +
    'internal data class ParityFixture(\n    val id: String,\n    val webExample: String,\n    val exampleTitle: String,\n' +
    '    val texts: List<String>,\n    val tones: List<String>,\n    val glyphs: List<String>,\n    val actions: List<Action>,\n) {\n' +
    '    data class Action(val label: String, val variant: String, val size: String)\n}\n\n' +
    '/** Every fixture. */\ninternal object ParityFixtures {\n' +
    items.join('\n') +
    `\n    val all: List<ParityFixture> by lazy { listOf(${ids.map(pascal).join(', ')}) }\n}\n`
  );
}

const outputs = {
  [join(
    root,
    'platforms/ios/Tests/MonarchUITests/Generated/Fixtures/ParityFixtures.generated.swift',
  )]: swiftFile(),
  [join(
    root,
    'platforms/android/monarch-compose/src/test/kotlin/dev/monarchui/fixtures/ParityFixtures.kt',
  )]: kotlinFile(),
};
if (!problems.length) {
  for (const [path, content] of Object.entries(outputs)) {
    const current = existsSync(path) ? read(path) : null;
    if (current === content) continue;
    if (check) stale.push(rel(path));
    else {
      mkdirSync(dirname(path), { recursive: true });
      writeFileSync(path, content);
      console.log(`fixtures: wrote ${rel(path)}`);
    }
  }
}

if (problems.length) {
  console.error(`fixtures: ${problems.length} problem(s)\n  ${problems.join('\n  ')}`);
  process.exit(1);
}
if (stale.length) {
  console.error(`fixtures: stale, run npm run native:fixtures:\n  ${stale.join('\n  ')}`);
  process.exit(1);
}
console.log(
  `fixtures: ${fixtures.size} fixture(s), bound on ios ${bound.ios.size} and android ${bound.android.size}; up to date`,
);
