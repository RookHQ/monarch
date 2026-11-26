// Writes packages/conformance/src/entry-styles.ts from registry/components.json: for every
// subpath entry of @rookcom/monarch-react, which component sheet
// (styles/components/<id>.css) styles each exported component. `monarch check` uses it for
// the `styles/subpath-css` rule; apps do not have the registry.
//
//   npm run registry:entry-styles
//
// packages/conformance/test/entry-styles.test.ts fails when the committed file is stale.
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { format, resolveConfig } from 'prettier';

const ROOT = fileURLToPath(new URL('../..', import.meta.url));
const OUT = join(ROOT, 'packages/conformance/src/entry-styles.ts');

/** The conformance test repeats this derivation and fails when the committed file differs. */
export function entryStyles(registry) {
  const map = {};
  const components = [...registry.components].sort((a, b) => a.id.localeCompare(b.id));
  for (const c of components) {
    if (c.package !== '@rookcom/monarch-react' || !c.source || !c.entry || c.entry === '.')
      continue;
    const entry = c.entry.replace(/^\.\//, '');
    const sheet = c.source.split('/').pop();
    for (const name of c.exports ?? []) {
      // Components only: hooks, helpers and MESSAGES constants render nothing.
      if (!/^[A-Z][A-Za-z0-9]*$/.test(name) || !/[a-z]/.test(name)) continue;
      (map[entry] ??= {})[name] = sheet;
    }
  }
  return Object.fromEntries(Object.entries(map).sort(([a], [b]) => a.localeCompare(b)));
}

/** Exports that encode a Rook platform contract: the same test as the manifest's `scope`. */
const isRookName = (name) => /Rook|^rook|^ROOK_/.test(name);

/**
 * Alias entries gather exports that another entry owns (`rook`: every Rook-specific export).
 * For each component: its sheet and the entry whose `styles/<owner>.css` also styles it, so an
 * app that already loads the owner's sheet is not asked for a second one.
 */
export function aliasEntryComponents(registry) {
  const rook = {};
  const components = [...registry.components].sort((a, b) => a.id.localeCompare(b.id));
  for (const c of components) {
    if (c.package !== '@rookcom/monarch-react' || !c.source || !c.entry) continue;
    const owner = c.entry === '.' ? 'core' : c.entry.replace(/^\.\//, '');
    const sheet = c.source.split('/').pop();
    for (const name of c.exports ?? []) {
      if (!/^[A-Z][A-Za-z0-9]*$/.test(name) || !/[a-z]/.test(name) || !isRookName(name)) continue;
      rook[name] = { sheet, owner };
    }
  }
  return { rook };
}

const registry = JSON.parse(readFileSync(join(ROOT, 'registry/components.json'), 'utf8'));
const body = `/**
 * Generated from registry/components.json by \`npm run registry:entry-styles\`. Do not edit.
 *
 * Subpath entry of @rookcom/monarch-react → component export → the per-component sheet
 * (\`@rookcom/monarch-react/styles/components/<sheet>.css\`) that styles it.
 */
export const ENTRY_COMPONENT_SHEETS: Readonly<Record<string, Readonly<Record<string, string>>>> = ${JSON.stringify(entryStyles(registry))};

/**
 * Alias entry (\`rook\`) → component export → its sheet and the entry that owns it. The
 * component is styled by \`styles/<alias>.css\`, \`styles/<owner>.css\` or its own sheet.
 */
export const ALIAS_ENTRY_COMPONENTS: Readonly<
  Record<string, Readonly<Record<string, { readonly sheet: string; readonly owner: string }>>>
> = ${JSON.stringify(aliasEntryComponents(registry))};
`;
const formatted = await format(body, {
  ...(await resolveConfig(OUT)),
  filepath: OUT,
});
writeFileSync(OUT, formatted);
console.log(`wrote ${OUT.slice(ROOT.length)}`);
