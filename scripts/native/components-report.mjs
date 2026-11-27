// Counts the native component plan and inventory, and writes the tables into
// docs/architecture/NATIVE-COMPONENTS.md between the two marker comments.
//
//   node scripts/native/components-report.mjs           rewrite the tables
//   node scripts/native/components-report.mjs --check   fail if the tables are stale
//   node scripts/native/components-report.mjs --print   print the tables only
//
// Sources: platforms/parity.json (the plan) and platforms/<platform>/inventory/<id>.json
// (one file per documented entry; schema.json is the schema, not an entry).
// Plain Node, no dependencies. The output is already in Prettier's table format, so
// `prettier --check` and `--check` here agree.
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';

const root = new URL('../../', import.meta.url);
const docFile = new URL('docs/architecture/NATIVE-COMPONENTS.md', root);
const START = '<!-- components-report:start -->';
const END = '<!-- components-report:end -->';

const PLATFORMS = [
  { key: 'android', title: 'Android' },
  { key: 'ios', title: 'iOS' },
];
const COVERAGES = ['build', 'style', 'native', 'compose', 'web-only'];
const CATEGORIES = ['A', 'B', 'C'];
const STATUSES = ['available', 'experimental', 'planned', 'deprecated'];

const readJson = (url) => JSON.parse(readFileSync(url, 'utf8'));

const parity = readJson(new URL('platforms/parity.json', root)).components;

function inventory(platform) {
  const dir = new URL(`platforms/${platform}/inventory/`, root);
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((name) => name.endsWith('.json') && name !== 'schema.json')
    .sort()
    .map((name) => readJson(new URL(name, dir)));
}

const count = (items, predicate) => items.filter(predicate).length;

/** A Markdown table in the layout Prettier writes: padded cells, `---:` for numbers. */
function table(header, rows, numeric) {
  const widths = header.map((cell, i) =>
    Math.max(3, cell.length, ...rows.map((row) => String(row[i]).length)),
  );
  const pad = (cell, i) =>
    numeric[i] ? String(cell).padStart(widths[i]) : String(cell).padEnd(widths[i]);
  const line = (cells) => `| ${cells.map(pad).join(' | ')} |`;
  const rule = `| ${widths.map((w, i) => (numeric[i] ? '-'.repeat(w - 1) + ':' : '-'.repeat(w))).join(' | ')} |`;
  return [line(header), rule, ...rows.map(line)].join('\n');
}

function report() {
  const entries = Object.fromEntries(PLATFORMS.map(({ key }) => [key, inventory(key)]));
  const parityIds = new Set(parity.map((c) => c.id));
  const out = [];

  // Summary: plan against inventory, per platform.
  const summaryRows = [];
  const summaryLabels = [
    ['Entries in the plan (`parity.json`)', () => parity.length],
    ['Inventory entries today', (k) => entries[k].length],
    ['… for a web component id in the plan', (k) => count(entries[k], (e) => parityIds.has(e.id))],
    ['… with no web id (foundations)', (k) => count(entries[k], (e) => !parityIds.has(e.id))],
    [
      'Plan entries with no inventory entry yet',
      (k) => count(parity, (c) => !entries[k].some((e) => e.id === c.id)),
    ],
    ...CATEGORIES.map((cat) => [
      `Inventory entries in class ${cat}`,
      (k) => count(entries[k], (e) => e.category === cat),
    ]),
    ...STATUSES.map((status) => [
      `Inventory entries with status \`${status}\``,
      (k) => count(entries[k], (e) => e.status === status),
    ]),
  ];
  for (const [label, fn] of summaryLabels) {
    summaryRows.push([label, ...PLATFORMS.map(({ key }) => fn(key))]);
  }
  out.push(
    table(['Count', ...PLATFORMS.map((p) => p.title)], summaryRows, [
      false,
      ...PLATFORMS.map(() => true),
    ]),
  );

  // Per platform: planned coverage per family, and how many are in the inventory.
  const families = [...new Set(parity.map((c) => c.family))].sort();
  for (const { key, title } of PLATFORMS) {
    const rows = families.map((family) => {
      const planned = parity.filter((c) => c.family === family);
      return [
        family,
        ...COVERAGES.map((cov) => count(planned, (c) => c[key].coverage === cov)),
        planned.length,
        count(entries[key], (e) => e.family === family && parityIds.has(e.id)),
      ];
    });
    const foundations = count(entries[key], (e) => !parityIds.has(e.id));
    rows.push(['foundations (no web id)', ...COVERAGES.map(() => 0), 0, foundations]);
    rows.push([
      'Total',
      ...COVERAGES.map((cov) => count(parity, (c) => c[key].coverage === cov)),
      parity.length,
      entries[key].length,
    ]);
    out.push(
      `${title}: planned coverage per family (from \`parity.json\`) and inventory entries today.`,
    );
    out.push(
      table(['Family', ...COVERAGES.map((c) => `\`${c}\``), 'Planned', 'In inventory'], rows, [
        false,
        ...COVERAGES.map(() => true),
        true,
        true,
      ]),
    );
  }
  return out.join('\n\n');
}

const args = new Set(process.argv.slice(2));
const generated = report();

if (args.has('--print')) {
  console.log(generated);
  process.exit(0);
}

const doc = readFileSync(docFile, 'utf8');
const start = doc.indexOf(START);
const end = doc.indexOf(END);
if (start === -1 || end === -1 || end < start) {
  console.error(`components-report: markers not found in ${docFile.pathname}`);
  process.exit(1);
}
const next = `${doc.slice(0, start + START.length)}\n\n${generated}\n\n${doc.slice(end)}`;

if (args.has('--check')) {
  if (next !== doc) {
    console.error(
      'components-report: the tables in docs/architecture/NATIVE-COMPONENTS.md are stale. ' +
        'Run: node scripts/native/components-report.mjs',
    );
    process.exit(1);
  }
  console.log('components-report: the tables are current');
} else {
  writeFileSync(docFile, next);
  console.log('components-report: wrote docs/architecture/NATIVE-COMPONENTS.md');
}
