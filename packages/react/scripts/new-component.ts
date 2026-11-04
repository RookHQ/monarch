/**
 * Scaffolds a component folder from its registry entry.
 *
 *   npm run new:component -- --id <registry-id> --family <family> [--dry-run]
 *
 * Reads registry/components.json for the name, layer, category, entry and
 * products, then writes src/<components|patterns>/<family>/<id>/ with a typed
 * forwardRef component, stylesheet, test (render, axe, SSR), docs file and
 * index, and wires it into the package: the export line in the entry index,
 * the stylesheet in src/styles/order.json and the EXPORT_GROUPS entry in
 * scripts/registry/build-components.mjs.
 *
 * Refuses ids that are unknown, already built, or not in @rookcom/monarch-react,
 * a family that does not match the registry category, and entries that do not
 * exist yet. The generated docs file contains TODO markers, which
 * `npm run components:check` rejects until they are replaced with real content.
 *
 * See docs/architecture/SOURCE-LAYOUT.md#adding-a-component.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { parseArgs } from 'node:util';
import * as prettier from 'prettier';
import {
  ENTRY_FILES,
  FAMILIES,
  REPO_ROOT,
  ROOT_OF_LAYER,
  SRC_DIR,
  familyOfCategory,
  listFolders,
} from './component-folders.js';

interface RegistryEntry {
  id: string;
  name: string;
  category: string;
  layer: string;
  package: string;
  entry: string;
  exports: string[];
  status: string;
  sourceProducts: string[];
  wave: number;
}

function die(msg: string): never {
  console.error(`new:component: ${msg}`);
  process.exit(1);
}

const { values } = parseArgs({
  options: {
    id: { type: 'string' },
    family: { type: 'string' },
    'dry-run': { type: 'boolean', default: false },
  },
});
const id = values.id;
const family = values.family;
const dry = values['dry-run'];
if (!id || !family) die('usage: npm run new:component -- --id <registry-id> --family <family>');

const registry = JSON.parse(readFileSync(join(REPO_ROOT, 'registry/components.json'), 'utf8')) as {
  components: RegistryEntry[];
};
const entry = registry.components.find((c) => c.id === id);
if (!entry)
  die(`"${id}" is not in registry/components.json. Propose it first (CONTRIBUTION-GUIDELINES §1).`);
if (entry.exports.length || entry.status !== 'planned')
  die(`"${id}" already ships (${entry.status}: ${entry.exports.join(', ') || 'no exports'}).`);
const existing = listFolders().find((f) => f.id === id);
if (existing) die(`src/${existing.rel} already exists.`);
if (entry.package !== '@rookcom/monarch-react')
  die(`"${id}" ships from ${entry.package}, not @rookcom/monarch-react (PACKAGE-ARCHITECTURE).`);
if (!(FAMILIES as readonly string[]).includes(family))
  die(`unknown family "${family}". Families: ${FAMILIES.join(', ')}`);
const expectedFamily = familyOfCategory(entry.category);
if (family !== expectedFamily)
  die(
    `the registry files "${id}" under category ${entry.category}, so its family is "${expectedFamily}". ` +
      `Change the category in registry/component-demand.json if that is wrong.`,
  );
const root = ROOT_OF_LAYER[entry.layer];
if (!root) die(`layer "${entry.layer}" has no home in @rookcom/monarch-react.`);
const entryFile = ENTRY_FILES[entry.entry];
if (!entryFile) die(`unknown entry "${entry.entry}" in the registry.`);
if (!existsSync(join(SRC_DIR, entryFile)))
  die(
    `entry "${entry.entry}" (src/${entryFile}) does not exist yet. Creating a subpath entry needs a ` +
      `package.json export and a review (PACKAGE-ARCHITECTURE); do that in its own PR first.`,
  );

const Name = entry.name.replace(/[^A-Za-z0-9]/g, '');
const rel = `${root}/${family}/${id}`;
const dir = join(SRC_DIR, rel);
const up = '../../../';
const products = entry.sourceProducts.join(', ') || 'none recorded';

const files: Record<string, string> = {
  'index.ts': `export * from './${id}.js';\n`,
  [`${id}.tsx`]: `import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '${up}internal/cx.js';

// When it gains props, make this an interface extending HTMLAttributes<HTMLDivElement>
// and document every prop: the comment is the docs-site description (dist/manifest.json).
export type ${Name}Props = HTMLAttributes<HTMLDivElement>;

/**
 * ${entry.name}. TODO: one sentence on what it is for; this comment is the
 * docs-site description.
 *
 * Add 'use client' as the first line only if it needs state, effects or Radix.
 */
export const ${Name} = forwardRef<HTMLDivElement, ${Name}Props>(function ${Name}(
  { className, ...rest },
  ref,
) {
  return <div ref={ref} {...rest} className={cx('mn-${id}', className)} />;
});
`,
  [`${id}.css`]: `/* ${entry.name}. Only --mn-* variables; state goes in data-* attributes. */
.mn-${id} {
  color: var(--mn-color-text-default);
}
`,
  [`${id}.test.tsx`]: `import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '${up}../test/a11y.js';
import { ${Name} } from './index.js';

describe('${Name}', () => {
  it('renders with its root class and forwards props', async () => {
    render(<${Name} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-${id}');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    expect(renderToString(<${Name} />)).toContain('mn-${id}');
  });
});
`,
  [`${id}.docs.tsx`]: `import { ${Name} } from '@rookcom/monarch-react';
import type { ComponentDocs } from '${up}internal/component-docs.js';

// Replace every TODO: \`npm run components:check\` fails while any remain.
// Contract: docs/architecture/SOURCE-LAYOUT.md#docs-file-contract.
// Reference examples: button, form-field and tabs docs files.
export default {
  id: '${id}',
  title: '${Name}',
  summary: 'TODO: one line for the components index.',
  sections: {
    purpose: {
      summary: 'TODO: what problem it solves. Needed by: ${products}.',
      useWhen: ['TODO'],
      avoidWhen: ['TODO'],
    },
    designPrinciples: { items: ['TODO'] },
    apiReference: { source: 'typescript' },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['TODO: list variant props, or say there are none.'] },
    states: { items: [{ name: 'TODO', description: 'TODO', attribute: 'data-TODO' }] },
    interaction: { items: ['TODO'] },
    keyboard: { items: [{ keys: 'TODO', action: 'TODO' }] },
    accessibility: { role: 'TODO', name: 'TODO', items: ['TODO'] },
    responsive: { items: ['TODO'] },
    theme: { items: ['TODO'] },
    examples: {
      items: [
        {
          title: 'TODO',
          render: () => <${Name}>TODO</${Name}>,
          code: \`<${Name}>TODO</${Name}>\`,
        },
      ],
    },
    doDont: { do: ['TODO'], dont: ['TODO'] },
    related: { items: [] },
    dependencies: { packages: ['react'], components: [] },
    testing: { unit: ['TODO'], axe: true, ssr: true },
  },
} satisfies ComponentDocs;
`,
};

// ---- wiring
const entryAbs = join(SRC_DIR, entryFile);
const fromEntry = relative(join(SRC_DIR, entryFile, '..'), join(dir, 'index.js'))
  .split('\\')
  .join('/');
const exportLine = `export { ${Name}, type ${Name}Props } from '${fromEntry.startsWith('.') ? fromEntry : './' + fromEntry}';\n`;
let entrySrc = readFileSync(entryAbs, 'utf8');
const patternsMarker = '\n// Patterns:';
entrySrc =
  root === 'components' && entrySrc.includes(patternsMarker)
    ? entrySrc.replace(patternsMarker, `\n${exportLine.trimEnd()}${patternsMarker}`)
    : entrySrc.trimEnd() + '\n' + exportLine;

const orderPath = join(SRC_DIR, 'styles/order.json');
const order = JSON.parse(readFileSync(orderPath, 'utf8')) as { $comment: string; order: string[] };
order.order.push(`${rel}/${id}.css`);

const groupsPath = join(REPO_ROOT, 'scripts/registry/build-components.mjs');
const groupsSrc = readFileSync(groupsPath, 'utf8');
const groupsEnd = groupsSrc.indexOf('\n];', groupsSrc.indexOf('const EXPORT_GROUPS = ['));
if (groupsEnd < 0) die('cannot find EXPORT_GROUPS in scripts/registry/build-components.mjs');
const groupsNext =
  groupsSrc.slice(0, groupsEnd) +
  `\n  { id: '${id}', exports: ['${Name}'] },` +
  groupsSrc.slice(groupsEnd);

if (dry) {
  console.log(`would create src/${rel}/ with ${Object.keys(files).join(', ')}`);
  console.log(`would add to src/${entryFile}: ${exportLine.trim()}`);
  console.log(`would append ${rel}/${id}.css to src/styles/order.json`);
  console.log(`would add { id: '${id}', exports: ['${Name}'] } to EXPORT_GROUPS`);
  process.exit(0);
}

/** Writes a file formatted with the repository's Prettier config, so `format:check` passes. */
async function write(file: string, text: string) {
  const options = (await prettier.resolveConfig(file)) ?? {};
  writeFileSync(file, await prettier.format(text, { ...options, filepath: file }));
}

mkdirSync(dir, { recursive: true });
for (const [name, text] of Object.entries(files)) await write(join(dir, name), text);
await write(entryAbs, entrySrc);
await write(orderPath, JSON.stringify(order, null, 2));
await write(groupsPath, groupsNext);

console.log(`Created packages/react/src/${rel}/ (${entry.layer}, wave ${entry.wave}, entry ${entry.entry})
Wired: src/${entryFile}, src/styles/order.json, EXPORT_GROUPS.

Next:
  1. Build the component; replace every TODO in ${id}.docs.tsx with what the code does.
  2. npm run build && npx vitest run -u packages/react/test/api.test.ts   (new export in the API snapshot)
  3. npm run registry:components && npm run docs:generate   (status planned → stable)
  4. Add it to GALLERY in apps/docs/src/pages/gallery.tsx if it should have visual baselines.
  5. npm run changeset (minor) and npm run verify.`);
