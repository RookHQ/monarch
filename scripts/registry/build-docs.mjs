// Fills the generated tables in the architecture documents from the registry, so
// no table in them is hand-typed.
//
//   npm run docs:generate         rewrite the generated blocks
//   npm run docs:generate:check   fail if any block is stale (CI)
//
// A block is everything between `<!-- generated:NAME -->` and
// `<!-- /generated:NAME -->`. Text outside blocks is hand-written and kept.
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, buildRegistry, formatFor, inScope, loadInputs } from './build-components.mjs';

const read = (p) => JSON.parse(readFileSync(join(ROOT, p), 'utf8'));

const DOCS = [
  'docs/architecture/COMPONENT-SELECTION.md',
  'docs/architecture/COMPONENT-TAXONOMY.md',
  'docs/architecture/COMPONENT-GAP-ANALYSIS.md',
  'docs/roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md',
];

const GAPS = [
  'implemented-complete',
  'implemented-incomplete',
  'missing',
  'product-extension',
  'not-evidenced',
];
const LAYERS = ['primitive', 'component', 'pattern', 'template', 'product-extension'];

// ------------------------------------------------------------------ helpers

const esc = (s) =>
  String(s)
    .replace(/\|/g, '\\|')
    .replace(/\s*\n\s*/g, ' ');
function table(headers, rows) {
  if (rows.length === 0) return '_None._';
  return [
    `| ${headers.join(' | ')} |`,
    `| ${headers.map(() => '---').join(' | ')} |`,
    ...rows.map((r) => `| ${r.map(esc).join(' | ')} |`),
  ].join('\n');
}
function short(list, n = 5) {
  if (!list.length) return '';
  const head = list.slice(0, n).join('; ');
  return list.length > n ? `${head} (+${list.length - n} more)` : head;
}
const by = (f) => (a, b) => (f(a) < f(b) ? -1 : f(a) > f(b) ? 1 : 0);

// ------------------------------------------------------------------ context

async function context() {
  const inputs = await loadInputs();
  const { registry, errors } = buildRegistry(inputs);
  if (errors.length) throw new Error(errors.join('\n'));
  return {
    registry,
    entries: registry.components,
    scoped: registry.components.filter((e) => e.scope === 'in-scope'),
    demand: inputs.consolidated.components,
    categories: read('registry/component-categories.json').categories,
    waves: read('registry/roadmap-waves.json').waves,
  };
}

// ------------------------------------------------------------------ blocks

function blocks(ctx) {
  const out = {};
  const { entries, scoped, demand } = ctx;
  const catNum = (cat) => cat.slice(0, 2);
  const catTitle = Object.fromEntries(ctx.categories.map((c) => [c.number, c.title]));

  // COMPONENT-SELECTION
  const dist = {};
  for (const d of demand) {
    const k = d.productCount >= 6 ? '6+' : String(d.productCount);
    dist[k] = (dist[k] ?? 0) + 1;
  }
  out['selection-distribution'] = table(
    ['Products needing it', 'Components in the registry'],
    ['1', '2', '3', '4', '5', '6+'].map((k) => [k, dist[k] ?? 0]),
  );
  out['selection-totals'] = table(
    ['Measure', 'Value'],
    [
      ['Registry entries', entries.length],
      ['Needed by 3+ products', demand.filter((d) => d.productCount >= 3).length],
      [
        'Needed by exactly 2 products at P0',
        demand.filter((d) => d.productCount === 2 && d.priority === 'P0').length,
      ],
      ['In scope (3+ products, or 2+ at P0)', demand.filter(inScope).length],
    ],
  );

  // COMPONENT-TAXONOMY
  out['taxonomy-categories'] = table(
    ['#', 'Category', 'Id', 'In scope', 'Registry entries'],
    ctx.categories.map((c) => {
      const id = [...new Set(entries.map((e) => e.category))].find((x) => catNum(x) === c.number);
      return [
        c.number,
        c.title,
        id ? `\`${id}\`` : '',
        scoped.filter((e) => catNum(e.category) === c.number).length,
        entries.filter((e) => catNum(e.category) === c.number).length,
      ];
    }),
  );
  out['taxonomy-layers'] = table(
    ['Layer', 'In scope', 'Registry entries'],
    LAYERS.map((l) => [
      l,
      scoped.filter((e) => e.layer === l).length,
      entries.filter((e) => e.layer === l).length,
    ]),
  );
  out['taxonomy-placement'] = table(
    ['Category', 'Component', 'Id', 'Layer', 'Package', 'Entry', 'Wave', 'Status'],
    [...scoped]
      .sort(by((e) => `${e.category}${LAYERS.indexOf(e.layer)}${e.id}`))
      .map((e) => [
        `${catNum(e.category)} ${catTitle[catNum(e.category)]}`,
        e.name,
        `\`${e.id}\``,
        e.layer,
        `\`${e.package}\``,
        `\`${e.entry}\``,
        e.wave || '–',
        e.status,
      ]),
  );

  // COMPONENT-GAP-ANALYSIS
  out['gap-summary'] = table(
    ['Class', 'In scope', 'Roadmap (below the rule)', 'Shipped (below the rule)'],
    [...GAPS, 'not-assessed'].map((g) => [
      g,
      ...['in-scope', 'roadmap', 'shipped'].map(
        (s) => entries.filter((e) => e.scope === s && e.gap === g).length,
      ),
    ]),
  );
  const gapRows = (list) =>
    table(
      ['Component', 'Demand', 'Priority', 'Wave', 'Missing capabilities', 'Builds on'],
      list
        .sort(by((e) => `${e.priority}${String(99 - e.productCount).padStart(2, '0')}${e.id}`))
        .map((e) => [
          `${e.name} (\`${e.id}\`)`,
          `${e.productCount} products`,
          e.priority,
          e.wave || '–',
          short(e.missingCapabilities, 6) || '–',
          e.buildsOn.join(', ') || '–',
        ]),
    );
  out['gap-in-scope'] = GAPS.map((g) => {
    const list = scoped.filter((e) => e.gap === g);
    return `### ${g} (${list.length})\n\n${gapRows(list)}`;
  }).join('\n\n');
  out['gap-roadmap'] = gapRows(entries.filter((e) => e.scope === 'roadmap'));
  out['gap-shipped'] = table(
    ['Component', 'Exports', 'Gap', 'Docs page'],
    entries
      .filter((e) => e.scope === 'shipped')
      .map((e) => [
        `${e.name} (\`${e.id}\`)`,
        e.exports.join(', '),
        e.gap,
        e.docs.status === 'complete'
          ? e.docs.route
          : e.docs.status === 'legacy-incomplete'
            ? `${e.docs.route} (incomplete)`
            : 'none',
      ]),
  );

  // ROADMAP
  out['roadmap-waves'] = table(
    ['Wave', 'Family', 'In scope', 'Below the rule', 'Components (package, entry)'],
    ctx.waves.map((w) => {
      const list = entries.filter((e) => e.wave === w.wave);
      return [
        w.wave,
        w.family,
        list.filter((e) => e.scope === 'in-scope').length,
        list.filter((e) => e.scope !== 'in-scope').length,
        w.groups
          .map((g) => `${g.package} \`${g.entry}\`: ${g.ids.map((i) => `\`${i}\``).join(', ')}`)
          .join(' · '),
      ];
    }),
  );
  return out;
}

// ------------------------------------------------------------------ inject

function inject(text, values, file) {
  const re = /<!-- generated:([a-z0-9:-]+) -->[\s\S]*?<!-- \/generated:\1 -->/g;
  const used = new Set();
  const result = text.replace(re, (_m, name) => {
    if (!(name in values)) throw new Error(`${file}: unknown generated block "${name}"`);
    used.add(name);
    return `<!-- generated:${name} -->\n\n${values[name]}\n\n<!-- /generated:${name} -->`;
  });
  return { result, used };
}

async function main() {
  const check = process.argv.includes('--check');
  const values = blocks(await context());
  const used = new Set();
  let stale = 0;
  for (const file of DOCS) {
    const current = readFileSync(join(ROOT, file), 'utf8');
    const { result, used: u } = inject(current, values, file);
    for (const n of u) used.add(n);
    const formatted = await formatFor(file, result);
    if (formatted === current) continue;
    if (check) {
      console.error(`${file} is stale: run \`npm run docs:generate\``);
      stale++;
    } else {
      writeFileSync(join(ROOT, file), formatted);
      console.log(`updated ${file}`);
    }
  }
  const unused = Object.keys(values).filter((n) => !used.has(n));
  if (unused.length) {
    console.error(`generated blocks not placed in any document: ${unused.join(', ')}`);
    process.exit(1);
  }
  if (stale) process.exit(1);
  if (check) console.log('generated documentation blocks are current');
}

await main();
