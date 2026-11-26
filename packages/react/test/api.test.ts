import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { ENTRY_FILES } from '../scripts/component-folders.js';

/**
 * Public API guard, one snapshot per package entry (`.`, `./marketing`,
 * `./scheduling`, …). Adding an export is additive (a patch release before
 * 1.0); removing or renaming one is breaking (a minor, with a migration note).
 * Update the snapshot deliberately and add a changeset.
 */
// A plain path: the test environment's URL class is not Node's, so fs would reject it.
const pkgDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const src = join(pkgDir, 'src');
const pkg = JSON.parse(readFileSync(join(pkgDir, 'package.json'), 'utf8')) as {
  exports: Record<string, unknown>;
};

/** Importing a whole entry barrel transforms hundreds of modules: far beyond the 5 s default. */
const IMPORT_TIMEOUT_MS = 60_000;

/** JavaScript entries of package.json `exports` (not stylesheets, the manifest or package.json). */
const jsEntries = Object.entries(pkg.exports)
  .filter(([, target]) => typeof target === 'object' && target !== null && 'types' in target)
  .map(([entry]) => entry)
  .sort();

describe('package entries', () => {
  it('every JavaScript entry in package.json has a source file', () => {
    expect(jsEntries.filter((e) => !(e in ENTRY_FILES))).toEqual([]);
    expect(Object.keys(ENTRY_FILES).sort()).toEqual(jsEntries);
    const missing = Object.entries(ENTRY_FILES)
      .filter(([, file]) => !existsSync(join(src, file)))
      .map(([entry, file]) => `${entry} → src/${file}`);
    expect(missing).toEqual([]);
  });
});

for (const [entry, file] of Object.entries(ENTRY_FILES)) {
  const name =
    entry === '.'
      ? 'public exports are stable'
      : `@rookcom/monarch-react/${entry.slice(2)} exports are stable`;
  it(
    name,
    async () => {
      // A missing entry fails here (and above) instead of being skipped.
      const mod = (await import(`../src/${file.replace(/\.ts$/, '.js')}`)) as Record<
        string,
        unknown
      >;
      expect(Object.keys(mod).sort()).toMatchSnapshot();
    },
    IMPORT_TIMEOUT_MS,
  );
}
