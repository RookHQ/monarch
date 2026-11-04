import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, it } from 'vitest';
import { ENTRY_FILES } from '../scripts/component-folders.js';

/**
 * Public API guard, one snapshot per package entry (`.`, `./marketing`,
 * `./scheduling`, …). Adding an export is a minor change; removing or renaming
 * one is breaking. Update the snapshot deliberately and add a changeset.
 * An entry is guarded as soon as its file exists under src/.
 */
// A plain path: the test environment's URL class is not Node's, so fs would reject it.
const src = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');
const entries = Object.entries(ENTRY_FILES).filter(([, file]) => existsSync(join(src, file)));

for (const [entry, file] of entries) {
  const name =
    entry === '.'
      ? 'public exports are stable'
      : `@rookcom/monarch-react/${entry.slice(2)} exports are stable`;
  it(name, async () => {
    const mod = (await import(`../src/${file.replace(/\.ts$/, '.js')}`)) as Record<string, unknown>;
    expect(Object.keys(mod).sort()).toMatchSnapshot();
  });
}
