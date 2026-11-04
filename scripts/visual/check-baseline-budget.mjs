/**
 * Visual baseline budget (docs/guides/visual-testing.md).
 *
 * Fails when the committed (git-tracked) screenshots break the rules that keep them small and
 * meaningful:
 *   - only Linux baselines are committed (the CI container is the one source of truth);
 *   - at most MAX_PER_COMPONENT images per gallery component (4 standard shots
 *     plus 3 product accents for the accent-heavy sample);
 *   - no single image above MAX_FILE_BYTES;
 *   - the whole folder stays under MAX_TOTAL_BYTES.
 * Raise a limit only in a reviewed PR that explains why the new coverage needs it.
 */
import { execFileSync } from 'node:child_process';
import { statSync } from 'node:fs';
import { relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(
  new URL('../../apps/docs/tests/__screenshots__/visual', import.meta.url),
);
const MAX_PER_COMPONENT = 7;
const MAX_FILE_BYTES = 1024 * 1024;
const MAX_TOTAL_BYTES = 30 * 1024 * 1024;

// Only files git tracks count: local, git-ignored baselines (MONARCH_VISUAL_LOCAL=1)
// are the developer's own business and never reach the repository.
const files = execFileSync('git', ['ls-files', '-z', '--', ROOT], { encoding: 'utf8' })
  .split('\0')
  .filter(Boolean)
  .map((f) => resolve(f));

const errors = [];
let total = 0;
const perComponent = new Map();
for (const file of files) {
  const rel = relative(ROOT, file).split(sep).join('/');
  const size = statSync(file).size;
  total += size;
  if (!rel.endsWith('-linux.png')) errors.push(`${rel}: only -linux.png baselines are committed`);
  if (size > MAX_FILE_BYTES) {
    errors.push(
      `${rel}: ${(size / 1024).toFixed(0)} KB exceeds the ${MAX_FILE_BYTES / 1024} KB per-image limit`,
    );
  }
  const m = /^gallery\/([^/]+)\//.exec(rel);
  if (m) perComponent.set(m[1], (perComponent.get(m[1]) ?? 0) + 1);
}
for (const [id, n] of perComponent) {
  if (n > MAX_PER_COMPONENT)
    errors.push(`gallery/${id}: ${n} images exceeds ${MAX_PER_COMPONENT} per component`);
}
if (total > MAX_TOTAL_BYTES) {
  errors.push(
    `total ${(total / 1048576).toFixed(1)} MB exceeds the ${MAX_TOTAL_BYTES / 1048576} MB budget`,
  );
}

const summary = `${files.length} images, ${(total / 1048576).toFixed(1)} MB, ${perComponent.size} components`;
if (errors.length) {
  console.error(`Visual baseline budget exceeded (${summary}):\n- ${errors.join('\n- ')}`);
  process.exit(1);
}
console.log(`Visual baseline budget ok: ${summary}`);
