#!/usr/bin/env node
/**
 * Cross-platform parity lints (CROSS-PLATFORM.md section 8, items 5 to 7), per component id:
 *
 *   fixture          the native primary example does not render a scenario fixture yet (section 8.1); a
 *                    parityExceptions item { rule: "no-fixture" } waives it for a component with no scenario;
 *   example-title    a native example after the first matches no web example by title (section 1.3);
 *   icon-coverage    a glyph the web component draws itself (an import from packages/react/src/internal/icons)
 *                    has no row in platforms/icons.json, or the native component's source does not draw the
 *                    mapped SF Symbol (iOS) or bundled MonarchGlyphs vector (Android) (section 3, 8.5);
 *   tone-map         a web state-to-tone table (`const STATE_TONE = { ok: 'neutral', ... }`) is not declared in
 *                    the inventory's toneMap, is declared differently, or the native source maps a state to
 *                    another tone (section 2, 8.6);
 *   forbidden-icon   a sparkle symbol for AI (AGENTS.md; use Bot / cpu / smart_toy);
 *   leaked-default   stock platform visuals in component sources (section 6, 8.7). Android has its own
 *                    failing test (rules/LeakedDefaultRulesTest.kt); this covers both platforms for the backlog.
 *
 * Every finding has a stable key. platforms/parity-baseline.json lists the known ones. The lint fails on a finding that is not in the baseline, and on a baseline entry that is no longer
 * found (remove it: the list only shrinks).
 *
 * Usage: node scripts/native/parity-lint.mjs [--json] [--summary] [--update-baseline]
 */
import { existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const args = process.argv.slice(2);
const read = (p) => readFileSync(p, 'utf8');
const PLATFORMS = ['ios', 'android'];
const TONES = ['neutral', 'info', 'success', 'warning', 'danger', 'accent', 'ai'];

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

function walk(dir, test) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name);
    return e.isDirectory() ? walk(p, test) : test(p) ? [p] : [];
  });
}

/** findings[category] = Map(key -> message) */
const findings = {};
const report = (category, key, message) => {
  (findings[category] ??= new Map()).set(key, message);
};

// --- Sources -----------------------------------------------------------------------------------

const icons = JSON.parse(read(join(root, 'platforms', 'icons.json')));
const iconByGlyph = new Map(icons.icons.map((i) => [i.glyph, i]));

/** Web component folders by id: packages/react/src/{components,patterns}/<family>/<id>/. */
const webFolders = new Map();
for (const layer of ['components', 'patterns']) {
  const base = join(root, 'packages', 'react', 'src', layer);
  if (!existsSync(base)) continue;
  for (const family of readdirSync(base)) {
    const fdir = join(base, family);
    if (!statSync(fdir).isDirectory()) continue;
    for (const id of readdirSync(fdir)) {
      const dir = join(fdir, id);
      if (statSync(dir).isDirectory()) webFolders.set(id, dir);
    }
  }
}
const webSources = (id) =>
  walk(
    webFolders.get(id) ?? '/nonexistent',
    (p) => /\.tsx?$/.test(p) && !/\.(test|docs)\.tsx?$/.test(p),
  )
    .map(read)
    .join('\n');
const docsTitles = (id) => {
  const file = join(webFolders.get(id) ?? '', `${id}.docs.tsx`);
  if (!existsSync(file)) return null;
  const text = read(file);
  const at = text.indexOf('examples:');
  if (at < 0) return [];
  return [...text.slice(at).matchAll(/^\s{8,12}title: '([^']+)'/gm)].map((m) => m[1]);
};

/** Glyphs imported from internal/icons. */
const webGlyphs = (source) => {
  const glyphs = new Set();
  for (const m of source.matchAll(
    /import\s*\{([^}]*)\}\s*from\s*'[./]*internal\/icons(?:\.js)?'/g,
  )) {
    for (const name of m[1].split(',').map((s) => s.trim().split(/\s+as\s+/)[0])) {
      if (/Glyph$/.test(name)) glyphs.add(name);
    }
  }
  return glyphs;
};

/** `const X_TONE = { a: 'warning', ... }` tables with tone values only. */
const webToneTables = (source) => {
  const tables = {};
  for (const m of source.matchAll(/^const ([A-Z][A-Z_]*TONES?[A-Z_]*)\b[^=\n]*=\s*\{([^}]*)\}/gm)) {
    const entries = [...m[2].matchAll(/['"]?([A-Za-z0-9-]+)['"]?\s*:\s*'([a-z-]+)'/g)].map((e) => [
      e[1],
      e[2],
    ]);
    if (!entries.length || !entries.every(([, t]) => TONES.includes(t))) continue;
    tables[m[1]] = Object.fromEntries(entries);
  }
  return tables;
};

const inventory = Object.fromEntries(
  PLATFORMS.map((p) => {
    const dir = join(root, 'platforms', p, 'inventory');
    const map = new Map();
    for (const f of readdirSync(dir).sort()) {
      if (!f.endsWith('.json') || f === 'schema.json') continue;
      const e = JSON.parse(read(join(dir, f)));
      map.set(e.id, e);
    }
    return [p, map];
  }),
);

const iosFamily = (family) => (family === 'crm' ? 'Workflow' : pascal(family));
const androidFamily = (family) => (family === 'crm' ? 'workflow' : family.replace(/-/g, ''));
/** The hand-written source of a native component, or null for a recipe or an entry without code. */
function nativeSource(platform, entry) {
  if (!entry.name || !['build', 'style'].includes(entry.coverage)) return null;
  const dir =
    platform === 'ios'
      ? join(
          root,
          'platforms/ios/Sources/MonarchUI/Components',
          iosFamily(entry.family),
          entry.name,
        )
      : join(
          root,
          'platforms/android/monarch-compose/src/main/kotlin/com/rookhq/monarch/components',
          androidFamily(entry.family),
          entry.name.replace(/^Monarch/, '').toLowerCase(),
        );
  const files = walk(dir, (p) => /\.(swift|kt)$/.test(p));
  return files.length ? files.map(read).join('\n') : null;
}
/**
 * The glyphs behind each member of MonarchDefaultIcons (the default icon slots of the Android components): the
 * vectors it names and, transitively, the other members it uses.
 */
const defaultIconGlyphs = (() => {
  const file = join(
    root,
    'platforms/android/monarch-compose/src/main/kotlin/com/rookhq/monarch/internal/MonarchDefaultIcons.kt',
  );
  const members = new Map();
  if (!existsSync(file)) return members;
  const text = read(file);
  const body = text.slice(text.indexOf('internal object MonarchDefaultIcons'));
  const parts = body.split(/\n {4}(?=val|fun |private fun )/).slice(1);
  for (const part of parts) {
    const name = /^(?:val|fun|private fun)\s+(\w+)/.exec(part)?.[1];
    if (!name) continue;
    members.set(name, {
      glyphs: [...part.matchAll(/MonarchGlyphs\.(\w+)/g)].map((m) => m[1]),
      uses: [...part.matchAll(/\b([A-Z]\w*|tone|callout)\b(?:\(\))?/g)].map((m) => m[1]),
    });
  }
  return members;
})();
function glyphsOfDefaultIcons(source) {
  const out = new Set();
  const seen = new Set();
  const visit = (name) => {
    if (seen.has(name) || !defaultIconGlyphs.has(name)) return;
    seen.add(name);
    const m = defaultIconGlyphs.get(name);
    m.glyphs.forEach((g) => out.add(g));
    m.uses.forEach(visit);
  };
  for (const m of source.matchAll(/MonarchDefaultIcons\.(\w+)/g)) visit(m[1]);
  return out;
}
const iosPalette = read(
  join(root, 'platforms/ios/Sources/MonarchUI/Internal/MonarchTonePalette.swift'),
);

// --- 1. Fixtures and example titles ----------------------------------------------------------

for (const p of PLATFORMS) {
  for (const [id, entry] of inventory[p]) {
    const titles = docsTitles(id);
    if (titles === null || !(entry.examples ?? []).length) continue; // no web page, or nothing to show
    if (
      !entry.examples[0].fixture &&
      !(entry.parityExceptions ?? []).some((x) => x.rule === 'no-fixture')
    )
      report('fixture', `${id}|${p}`, `${id}: ${p} examples[0] does not render a scenario fixture`);
    for (const ex of entry.examples.slice(1)) {
      if (!titles.includes(ex.title))
        report(
          'example-title',
          `${id}|${p}|${ex.title}`,
          `${id}: ${p} example "${ex.title}" matches no web example`,
        );
    }
  }
}

// --- 2. Icon coverage ------------------------------------------------------------------------

for (const id of [...webFolders.keys()].sort()) {
  const glyphs = webGlyphs(webSources(id));
  if (!glyphs.size) continue;
  for (const glyph of glyphs) {
    const row = iconByGlyph.get(glyph);
    if (!row) {
      report(
        'icon-coverage',
        `${id}|mapping|${glyph}`,
        `${id}: ${glyph} has no row in platforms/icons.json`,
      );
      continue;
    }
    for (const p of PLATFORMS) {
      const entry = inventory[p].get(id);
      if (!entry) continue;
      const source = nativeSource(p, entry);
      if (source === null) continue; // a recipe or no native code: its parts are checked on their own
      if (p === 'ios') {
        const direct = source.includes(`"${row.ios}"`);
        const viaPalette = /MonarchTonePalette/.test(source) && iosPalette.includes(`"${row.ios}"`);
        if (!direct && !viaPalette)
          report(
            'icon-coverage',
            `${id}|ios|${glyph}`,
            `${id}: ios does not draw ${row.ios} (web ${glyph})`,
          );
      } else {
        const name = `MonarchGlyphs.${pascal(row.android)}`;
        if (!row.bundled)
          report(
            'icon-coverage',
            `${id}|android|${glyph}`,
            `${id}: android glyph ${row.android} (web ${glyph}) is not bundled`,
          );
        else if (!source.includes(name) && !glyphsOfDefaultIcons(source).has(pascal(row.android)))
          report(
            'icon-coverage',
            `${id}|android|${glyph}`,
            `${id}: android does not draw ${name} (web ${glyph})`,
          );
      }
    }
  }
}

// --- 3. Tone maps ------------------------------------------------------------------------------

const toneToken = {
  ios: (t) => new RegExp(`\\.${t}\\b`),
  android: (t) => new RegExp(`MonarchTone\\.${pascal(t)}\\b`),
};
for (const id of [...webFolders.keys()].sort()) {
  const tables = webToneTables(webSources(id));
  for (const [table, map] of Object.entries(tables)) {
    for (const p of PLATFORMS) {
      const entry = inventory[p].get(id);
      if (!entry || !['build', 'style'].includes(entry.coverage)) continue;
      const declared = entry.toneMap?.[table];
      if (!declared) {
        report(
          'tone-map',
          `${id}|${p}|${table}|undeclared`,
          `${id}: ${p} inventory has no toneMap.${table}`,
        );
        continue;
      }
      for (const [state, tone] of Object.entries(map)) {
        if (declared[state] !== tone)
          report(
            'tone-map',
            `${id}|${p}|${table}|${state}`,
            `${id}: ${p} toneMap.${table}.${state} is ${declared[state]}, the web has ${tone}`,
          );
      }
      const source = nativeSource(p, entry);
      if (!source) continue;
      const lines = source.split('\n');
      for (const [state, tone] of Object.entries(map)) {
        const word = new RegExp(`(\\.|\\b)(${camel(state)}|${pascal(state)})\\b`);
        const found = new Set();
        for (const line of lines) {
          if (!word.test(line)) continue;
          for (const t of TONES) if (toneToken[p](t).test(line)) found.add(t);
        }
        if (found.size && !found.has(tone))
          report(
            'tone-map',
            `${id}|${p}|${table}|${state}|source`,
            `${id}: ${p} source maps ${state} to ${[...found].join('/')}, the web to ${tone}`,
          );
      }
    }
  }
}

// --- 4. Forbidden icons and leaked defaults ----------------------------------------------------

const relKey = (p) => relative(root, p);
const sparkle = /"sparkles?(\.[a-z.]+)?"|Icons\.\w+\.AutoAwesome|\bAutoAwesome\b/;
for (const file of [
  ...walk(join(root, 'platforms/ios/Sources'), (p) => p.endsWith('.swift')),
  ...walk(
    join(root, 'platforms/ios/inventory'),
    (p) => p.endsWith('.json') && !p.endsWith('schema.json'),
  ),
  ...walk(join(root, 'platforms/ios/Sample/Sources'), (p) => p.endsWith('.swift')),
  ...walk(join(root, 'platforms/android/monarch-compose/src/main'), (p) => p.endsWith('.kt')),
  ...walk(
    join(root, 'platforms/android/inventory'),
    (p) => p.endsWith('.json') && !p.endsWith('schema.json'),
  ),
  ...walk(join(root, 'platforms/android/sample/src'), (p) => p.endsWith('.kt')),
]) {
  const text = read(file);
  const count = text.split('\n').filter((l) => sparkle.test(l)).length;
  if (count)
    report(
      'forbidden-icon',
      relKey(file),
      `${relKey(file)}: ${count} sparkle icon use(s) for AI; use Bot (cpu, smart_toy)`,
    );
}

const iosComponents = walk(join(root, 'platforms/ios/Sources/MonarchUI/Components'), (p) =>
  p.endsWith('.swift'),
);
const iosLeaks = [
  [/\.accentColor\b|Color\.accentColor/, 'the system accent colour'],
  [
    /\bColor\.(blue|green|red|orange|yellow|purple|pink|gray|grey|black|white)\b/,
    'a system colour',
  ],
  [/\bUIColor\.(system\w+|label|secondaryLabel)\b/, 'a UIKit colour'],
  [/\.buttonStyle\(\.(borderedProminent|bordered)\)/, 'a stock button style'],
];
for (const file of iosComponents) {
  const text = read(file);
  for (const [pattern, what] of iosLeaks) {
    const n = text.split('\n').filter((l) => !l.trim().startsWith('///') && pattern.test(l)).length;
    if (n)
      report(
        'leaked-default',
        `${relKey(file)}|${what}`,
        `${relKey(file)}: ${n} use(s) of ${what}`,
      );
  }
}
const androidComponents = walk(
  join(root, 'platforms/android/monarch-compose/src/main/kotlin/com/rookhq/monarch/components'),
  (p) => p.endsWith('.kt'),
);
for (const file of androidComponents) {
  const text = read(file);
  const code = text
    .split('\n')
    .filter((l) => !/^\s*(\*|\/\/|\/\*\*)/.test(l))
    .join('\n');
  const leaks = [
    [/android\.R\.drawable/, 'android.R.drawable'],
    [/MaterialTheme\.(colorScheme|typography|shapes)\b/, 'an unthemed MaterialTheme value'],
    [/\bTestIcon\b/, 'TestIcon'],
  ];
  for (const [pattern, what] of leaks) {
    if (pattern.test(code))
      report('leaked-default', `${relKey(file)}|${what}`, `${relKey(file)}: ${what}`);
  }
}
// Material 3 progress indicators: the stop dot and the track gap (the web draws one continuous fill).
for (const file of walk(join(root, 'platforms/android/monarch-compose/src/main/kotlin'), (p) =>
  p.endsWith('.kt'),
)) {
  const text = read(file);
  const lines = [];
  for (const m of text.matchAll(/(?<![\w.])(Linear|Circular)ProgressIndicator\(/g)) {
    let depth = 1;
    let i = m.index + m[0].length;
    while (depth && i < text.length) {
      if (text[i] === '(') depth++;
      else if (text[i] === ')') depth--;
      i++;
    }
    const call = text.slice(m.index, i);
    if (!/\bprogress\s*=/.test(call)) continue; // indeterminate: no gap, no stop dot
    const missing = ['gapSize', ...(m[1] === 'Linear' ? ['drawStopIndicator'] : [])].filter(
      (x) => !call.includes(x),
    );
    if (missing.length)
      lines.push(
        `${text.slice(0, m.index).split('\n').length} (${m[1]}, no ${missing.join(' or ')})`,
      );
  }
  // One finding per file, so moving code does not change the key.
  if (lines.length)
    report(
      'leaked-default',
      `${relKey(file)}|progress track gap`,
      `${relKey(file)}: Material progress track gap or stop dot at line ${lines.join(', ')}`,
    );
}

// --- Baseline ----------------------------------------------------------------------------------

const baselineFile = join(root, 'platforms', 'parity-baseline.json');
const current = Object.fromEntries(
  Object.entries(findings)
    .sort()
    .map(([c, m]) => [c, [...m.keys()].sort()]),
);
if (args.includes('--update-baseline')) {
  writeFileSync(
    baselineFile,
    JSON.stringify(
      {
        $comment:
          'The cross-platform parity backlog: every finding of scripts/native/parity-lint.mjs that is known. Fix one, then remove its key (node scripts/native/parity-lint.mjs --update-baseline). The lint fails on a finding that is not listed and on a listed one that is gone.',
        findings: current,
      },
      null,
      2,
    ) + '\n',
  );
  console.log(`parity-lint: wrote ${relKey(baselineFile)}`);
}
const baseline = existsSync(baselineFile) ? JSON.parse(read(baselineFile)).findings : {};
const added = [];
const gone = [];
for (const [c, keys] of Object.entries(current)) {
  const known = new Set(baseline[c] ?? []);
  for (const k of keys) if (!known.has(k)) added.push(findings[c].get(k));
}
for (const [c, keys] of Object.entries(baseline)) {
  const now = new Set(current[c] ?? []);
  for (const k of keys) if (!now.has(k)) gone.push(`${c}: ${k}`);
}

const counts = Object.fromEntries(Object.entries(current).map(([c, k]) => [c, k.length]));
if (args.includes('--json')) {
  console.log(
    JSON.stringify(
      {
        counts,
        findings: Object.fromEntries(
          Object.entries(findings).map(([c, m]) => [c, [...m.values()]]),
        ),
      },
      null,
      2,
    ),
  );
} else {
  console.log(
    `parity-lint: ${Object.entries(counts)
      .map(([c, n]) => `${c} ${n}`)
      .join(', ')} (all in the baseline unless listed below)`,
  );
  if (args.includes('--summary')) {
    for (const [c, m] of Object.entries(findings)) {
      const byPlatform = {};
      for (const key of m.keys()) {
        const p = key.includes('|ios')
          ? 'ios'
          : key.includes('|android')
            ? 'android'
            : key.includes('platforms/ios')
              ? 'ios'
              : key.includes('platforms/android')
                ? 'android'
                : 'web';
        byPlatform[p] = (byPlatform[p] ?? 0) + 1;
      }
      console.log(`  ${c}: ${JSON.stringify(byPlatform)}`);
    }
  }
}
if (added.length)
  console.error(
    `\nnew findings (fix them, or add a parityExceptions item):\n  ${added.join('\n  ')}`,
  );
if (gone.length)
  console.error(
    `\nfixed but still in platforms/parity-baseline.json (remove them):\n  ${gone.join('\n  ')}`,
  );
process.exitCode = added.length || gone.length ? 1 : 0;
