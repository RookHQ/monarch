#!/usr/bin/env node
/**
 * Cross-platform inventory check for the native libraries.
 *
 * Compares platforms/ios/inventory/*.json with platforms/android/inventory/*.json
 * and with platforms/parity.json, which carries the status, layer, coverage and
 * name of every component for both platforms:
 *   1. ids: every id exists on both platforms;
 *   2. status: the same status on both platforms, and equal to parity.json;
 *   3. layer: every entry has "layer" ("components" or "patterns"), the same on
 *      both platforms and in parity.json, and a pattern is experimental
 *      (ADR 0017, D4);
 *   4. names: the shared model names in the "Checked names" table of
 *      docs/architecture/NATIVE-NAMES.md (--names=<file> to use another file)
 *      appear in each platform's entry, and retired names do not;
 *   5. parity: the coverage and name of each platform equal parity.json;
 *   6. fixtures: the cross-platform parity fields (CROSS-PLATFORM.md section 8) are well formed: a bound
 *      example is examples[0], its fixture exists (packages/react/src/**\/<id>.fixture.json) and its
 *      webExample is the title of the web primary example; both platforms bind the same fixture; toneMap
 *      holds tones, icons name glyphs of platforms/icons.json, and each parityExceptions item has a rule,
 *      a reason and a convention.
 *
 * --write-parity regenerates platforms/parity.json from the two inventories
 * (status, layer, coverage and name of each platform; the dependsOn of a
 * composition gains what either platform's recipe uses). Run it after an
 * inventory change, then format the file with prettier.
 *
 * Prints every difference, grouped, with the platform it is on, and exits 1
 * when there is any. With --platform=android or --platform=ios it still prints
 * everything but fails only on differences that need a change on that
 * platform (one-sided rules, and names on that side).
 *
 * Usage: node scripts/native/status-check.mjs [--names=<file>] [--platform=android|ios] [--json] [--write-parity]
 */
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const PLATFORMS = ['ios', 'android'];
const STATUSES = new Set(['available', 'experimental', 'planned', 'deprecated']);
const LAYERS = new Set(['components', 'patterns']);

const args = process.argv.slice(2);
const option = (name) => args.find((a) => a.startsWith(`--${name}=`))?.slice(name.length + 3);
const only = option('platform');
if (only && !PLATFORMS.includes(only)) {
  console.error(`--platform must be ios or android, not "${only}"`);
  process.exit(2);
}

/** Every inventory entry of a platform, by id. */
function inventory(platform) {
  const folder = join(root, 'platforms', platform, 'inventory');
  const entries = new Map();
  for (const file of readdirSync(folder).sort()) {
    if (!file.endsWith('.json') || file === 'schema.json') continue;
    const entry = JSON.parse(readFileSync(join(folder, file), 'utf8'));
    entries.set(entry.id ?? file.slice(0, -5), entry);
  }
  return entries;
}

/** The checked rows of the names table: | id | concept | web | Android | iOS | retired | reason |. */
function names(file) {
  if (!file || !existsSync(file)) return { file, rows: [] };
  const text = readFileSync(file, 'utf8');
  const section = text.split(/^## /m).find((s) => s.startsWith('Checked names')) ?? '';
  const code = (cell) => [...cell.matchAll(/`([^`]+)`/g)].map((m) => m[1]);
  const rows = section
    .split('\n')
    .filter((line) => line.startsWith('|') && !/^\|\s*(id|-{3})/.test(line))
    .map((line) =>
      line
        .split('|')
        .slice(1, -1)
        .map((c) => c.trim()),
    )
    .filter((cells) => cells.length >= 6 && /^[a-z0-9-]+$/.test(cells[0]))
    .map(([id, concept, , android, ios, retired]) => ({
      id,
      concept,
      android: code(android)[0],
      ios: code(ios)[0],
      retired: retired
        .replace(/`/g, '')
        .split(',')
        .map((n) => n.trim())
        .filter(Boolean),
    }));
  return { file, rows };
}

const inventories = Object.fromEntries(PLATFORMS.map((p) => [p, inventory(p)]));
/** platforms/parity.json: the plan, with the status, layer, coverage and name of each platform. */
const parityFile = join(root, 'platforms', 'parity.json');
const parity = JSON.parse(readFileSync(parityFile, 'utf8'));
const parityById = new Map(parity.components.map((c) => [c.id, c]));

if (args.includes('--write-parity')) {
  const ids = new Set(parityById.keys());
  parity.components = parity.components.map((component) => {
    const ios = inventories.ios.get(component.id);
    const android = inventories.android.get(component.id);
    if (!ios || !android) return component;
    const next = {};
    for (const [key, value] of Object.entries(component)) {
      if (key === 'status') continue;
      next[key] = value;
      if (key === 'layer') next.status = ios.status;
    }
    next.layer = ios.layer;
    next.ios = { ...component.ios, coverage: ios.coverage, name: ios.name };
    next.android = { ...component.android, coverage: android.coverage, name: android.name };
    if (ios.coverage === 'compose' || android.coverage === 'compose') {
      const used = [...ios.dependsOn, ...android.dependsOn].filter(
        (id) => ids.has(id) && id !== component.id,
      );
      next.dependsOn = [...new Set([...component.dependsOn, ...used])];
    }
    return next;
  });
  writeFileSync(parityFile, JSON.stringify(parity, null, 2) + '\n');
  console.log(`wrote ${parityFile}; run prettier on it`);
  process.exit(0);
}

/** The names table: --names, else the one in the repository. */
const defaultNames = join(root, 'docs', 'architecture', 'NATIVE-NAMES.md');
const nameTable = names(option('names') ?? defaultNames);

/** problems[group] = [{ fix: platform | 'both', text }] */
const problems = {};
const report = (group, fix, text) => (problems[group] ??= []).push({ fix, text });
const other = (p) => (p === 'ios' ? 'android' : 'ios');

// 1. ids on one platform only.
for (const p of PLATFORMS) {
  for (const id of inventories[p].keys()) {
    if (!inventories[other(p)].has(id))
      report('ids', other(p), `${id}: only on ${p}; missing on ${other(p)}`);
  }
}

// 2 and 3: per entry and between platforms.
const shared = [...inventories.ios.keys()].filter((id) => inventories.android.has(id)).sort();
for (const p of PLATFORMS) {
  for (const [id, entry] of inventories[p]) {
    if (!STATUSES.has(entry.status))
      report('status', p, `${id}: ${p} status "${entry.status}" is not a status`);
    if (entry.layer === undefined) report('layer', p, `${id}: ${p} has no "layer"`);
    else if (!LAYERS.has(entry.layer))
      report('layer', p, `${id}: ${p} layer "${entry.layer}" is not components or patterns`);
    else if (entry.layer === 'patterns' && entry.status !== 'experimental') {
      report(
        'layer',
        p,
        `${id}: ${p} is a pattern but its status is ${entry.status}; a pattern is experimental`,
      );
    }
  }
}
for (const id of shared) {
  const ios = inventories.ios.get(id);
  const android = inventories.android.get(id);
  if (ios.status !== android.status) {
    report('status', 'both', `${id}: ios ${ios.status}, android ${android.status}`);
  }
  if (ios.layer !== undefined && android.layer !== undefined && ios.layer !== android.layer) {
    report('layer', 'both', `${id}: ios ${ios.layer}, android ${android.layer}`);
  }
}

// 4. names.
const mentions = (entry, name) => {
  const text = [entry.name ?? '', ...(entry.api ?? [])].join('\n');
  return new RegExp(`(^|[^A-Za-z0-9_])${name}([^A-Za-z0-9_]|$)`).test(text);
};
for (const row of nameTable.rows) {
  for (const p of PLATFORMS) {
    const entry = inventories[p].get(row.id);
    const want = p === 'ios' ? row.ios : row.android;
    if (!entry) continue; // reported under ids
    if (want && !mentions(entry, want))
      report('names', p, `${row.id}: ${p} api does not name ${want} (${row.concept})`);
    for (const old of row.retired) {
      if (old !== want && mentions(entry, old)) {
        report('names', p, `${row.id}: ${p} api still names ${old}; use ${want} (${row.concept})`);
      }
    }
  }
}

// 5. parity.json is the plan for both platforms: ids, status, layer, coverage, name.
for (const component of parity.components) {
  for (const p of PLATFORMS) {
    const entry = inventories[p].get(component.id);
    if (!entry) {
      report('ids', p, `${component.id}: in parity.json; missing on ${p}`);
      continue;
    }
    for (const field of ['status', 'layer']) {
      if (component[field] !== entry[field])
        report(
          'parity',
          p,
          `${component.id}: ${p} ${field} is ${entry[field]}, parity.json has ${component[field]}`,
        );
    }
    for (const field of ['coverage', 'name']) {
      if (component[p]?.[field] !== entry[field])
        report(
          'parity',
          p,
          `${component.id}: ${p} ${field} is ${entry[field]}, parity.json has ${component[p]?.[field]}`,
        );
    }
  }
}

// 6. Cross-platform parity fields.
const TONES = new Set(['neutral', 'info', 'success', 'warning', 'danger', 'accent', 'ai']);
const glyphs = new Set(
  JSON.parse(readFileSync(join(root, 'platforms', 'icons.json'), 'utf8')).icons.map((i) => i.glyph),
);
const reactSrc = join(root, 'packages', 'react', 'src');
function findFile(dir, name) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      const found = findFile(path, name);
      if (found) return found;
    } else if (entry.name === name) return path;
  }
  return null;
}
/** The title of the web primary example: the first `title:` after `examples:` in <id>.docs.tsx. */
function webPrimaryTitle(id) {
  const docs = findFile(reactSrc, `${id}.docs.tsx`);
  if (!docs) return null;
  const text = readFileSync(docs, 'utf8');
  const at = text.indexOf('examples:');
  return at < 0 ? null : (/title: '([^']+)'/.exec(text.slice(at))?.[1] ?? null);
}
const isObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);
for (const p of PLATFORMS) {
  for (const [id, entry] of inventories[p]) {
    (entry.examples ?? []).forEach((example, index) => {
      if (example.fixture === undefined) return;
      if (index !== 0)
        report('fixtures', p, `${id}: ${p} example ${index} binds a fixture; only examples[0] may`);
      if (!findFile(reactSrc, `${example.fixture}.fixture.json`))
        report(
          'fixtures',
          p,
          `${id}: ${p} fixture "${example.fixture}" has no ${example.fixture}.fixture.json`,
        );
      const web = webPrimaryTitle(id);
      if (example.webExample !== web)
        report(
          'fixtures',
          p,
          `${id}: ${p} webExample is "${example.webExample}", the web primary example is "${web}"`,
        );
    });
    if (entry.toneMap !== undefined) {
      if (!isObject(entry.toneMap)) report('fixtures', p, `${id}: ${p} toneMap must be an object`);
      else
        for (const [table, map] of Object.entries(entry.toneMap)) {
          if (!isObject(map))
            report('fixtures', p, `${id}: ${p} toneMap.${table} must map states to tones`);
          else
            for (const [state, tone] of Object.entries(map))
              if (!TONES.has(tone))
                report(
                  'fixtures',
                  p,
                  `${id}: ${p} toneMap.${table}.${state} "${tone}" is not a tone`,
                );
        }
    }
    if (entry.icons !== undefined) {
      if (!isObject(entry.icons)) report('fixtures', p, `${id}: ${p} icons must be an object`);
      else
        for (const [slot, glyph] of Object.entries(entry.icons))
          if (!glyphs.has(glyph))
            report(
              'fixtures',
              p,
              `${id}: ${p} icons.${slot} "${glyph}" is not a glyph of platforms/icons.json`,
            );
    }
    for (const [i, x] of (entry.parityExceptions ?? []).entries()) {
      for (const field of ['rule', 'reason', 'convention'])
        if (typeof x?.[field] !== 'string' || !x[field].trim())
          report('fixtures', p, `${id}: ${p} parityExceptions[${i}] needs a ${field}`);
      if (x?.platform !== undefined && x.platform !== p)
        report('fixtures', p, `${id}: ${p} parityExceptions[${i}] names platform ${x.platform}`);
    }
  }
}
for (const id of shared) {
  const ios = inventories.ios.get(id).examples?.[0]?.fixture;
  const android = inventories.android.get(id).examples?.[0]?.fixture;
  if (ios !== android)
    report(
      'fixtures',
      'both',
      `${id}: ios examples[0] binds ${ios ?? 'no fixture'}, android ${android ?? 'no fixture'}`,
    );
}

// Output.
const groups = ['ids', 'status', 'layer', 'names', 'parity', 'fixtures'];
const count = (fix) =>
  groups.flatMap((g) => problems[g] ?? []).filter((x) => fix === undefined || x.fix === fix).length;
if (args.includes('--json')) {
  console.log(JSON.stringify({ problems, names: nameTable.file ?? null }, null, 2));
} else {
  console.log(
    `native status check: ios ${inventories.ios.size} entries, android ${inventories.android.size} entries, ` +
      `${shared.length} shared ids, ${nameTable.rows.length} checked names` +
      (nameTable.file && existsSync(nameTable.file)
        ? ''
        : ' (names file not found; names not checked)'),
  );
  for (const group of groups) {
    const list = problems[group] ?? [];
    console.log(`\n${group}: ${list.length} difference${list.length === 1 ? '' : 's'}`);
    for (const { fix, text } of list)
      console.log(`  [${fix === 'both' ? 'between platforms' : `fix on ${fix}`}] ${text}`);
  }
  console.log(
    `\ntotal: ${count()} (fix on ios: ${count('ios')}, fix on android: ${count('android')}, ` +
      `between platforms: ${count('both')})`,
  );
}
const failing = only ? count(only) : count();
process.exit(failing === 0 ? 0 : 1);
