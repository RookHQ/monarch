// Guards Monarch's license and brand files. Fails if the Apache License text
// changes, if NOTICE or TRADEMARK.md go missing, or if a published package
// stops declaring Apache-2.0 or stops shipping LICENSE and NOTICE.
// Part of `npm run verify` (see ADR 0016).
import { createHash } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (p) => readFileSync(join(root, p), 'utf8');
const problems = [];

// SHA-256 of https://www.apache.org/licenses/LICENSE-2.0.txt, unmodified.
const APACHE_SHA256 = 'cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30';
const licenseHash = createHash('sha256')
  .update(readFileSync(join(root, 'LICENSE')))
  .digest('hex');
if (licenseHash !== APACHE_SHA256)
  problems.push('LICENSE is not the unmodified Apache License 2.0 text.');

for (const file of ['NOTICE', 'TRADEMARK.md', 'THIRD_PARTY_NOTICES.md']) {
  if (!existsSync(join(root, file))) problems.push(`${file} is missing.`);
}
if (existsSync(join(root, 'NOTICE')) && !read('NOTICE').includes('Apache License, Version 2.0')) {
  problems.push('NOTICE does not name the Apache License, Version 2.0.');
}
if (existsSync(join(root, 'TRADEMARK.md'))) {
  const trademark = read('TRADEMARK.md');
  for (const phrase of ['No brand hijacking', 'No impersonation', 'logos']) {
    if (!trademark.includes(phrase)) problems.push(`TRADEMARK.md no longer mentions "${phrase}".`);
  }
}

const rootPkg = JSON.parse(read('package.json'));
if (rootPkg.license !== 'Apache-2.0') problems.push('package.json: license must be "Apache-2.0".');
for (const dir of ['packages/tokens', 'packages/react', 'packages/conformance']) {
  const pkg = JSON.parse(read(`${dir}/package.json`));
  if (pkg.license !== 'Apache-2.0')
    problems.push(`${dir}/package.json: license must be "Apache-2.0".`);
  for (const file of ['LICENSE', 'NOTICE']) {
    if (!pkg.files?.includes(file))
      problems.push(`${dir}/package.json: "files" must include ${file}.`);
  }
}

if (problems.length > 0) {
  console.error(`legal: ${problems.length} problem(s)\n- ${problems.join('\n- ')}`);
  process.exit(1);
}
console.log('legal: LICENSE, NOTICE, TRADEMARK.md and package licenses are in order');
