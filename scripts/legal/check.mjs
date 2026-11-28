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

// Platform terms (Apple and Google). SF Symbols and the San Francisco fonts are
// licensed for Apple platforms only: they may be referenced by name from the
// iOS library, never bundled, and never used on the web or Android. Symbols
// that depict Apple products or marks carry extra restrictions and are not used.
{
  const { execFileSync } = await import('node:child_process');
  const git = (args) => {
    try {
      return execFileSync('git', args, { cwd: root, encoding: 'utf8' });
    } catch (error) {
      return error.stdout ?? '';
    }
  };
  const fontFiles = git(['ls-files', '--cached', '--others', '--exclude-standard'])
    .split('\n')
    .filter((p) =>
      /(^|\/)(SF-?(Pro|Compact|Mono|Arabic)|SanFrancisco|NewYork)[^/]*\.(otf|ttf|woff2?)$/i.test(p),
    );
  if (fontFiles.length > 0)
    problems.push(`Apple system font files must not be in the repository: ${fontFiles.join(', ')}`);
  const outsideIos = git([
    'grep',
    '-lE',
    'systemName:|systemImage:',
    '--',
    'packages',
    'apps',
    'platforms/android',
  ])
    .split('\n')
    .filter(Boolean);
  if (outsideIos.length > 0)
    problems.push(`SF Symbols are used outside the iOS library: ${outsideIos.join(', ')}`);
  // Names of SF Symbols that depict Apple products or marks. `swift` is left out of the
  // bare-literal rule below because it is also the file extension of Swift sources.
  const restrictedNames =
    'applelogo|apple\\.[a-z.]+|iphone[a-z.]*|ipad[a-z.]*|ipod[a-z.]*|macbook[a-z.]*|macmini[a-z.]*|macpro[a-z.]*|imac[a-z.]*|airpods[a-z.]*|airtag[a-z.]*|homepod[a-z.]*|applewatch[a-z.]*|appletv[a-z.]*|visionpro[a-z.]*|vision\\.pro[a-z.]*|faceid|touchid|xcode[a-z.]*|safari[a-z.]*|carplay|airplay[a-z.]*|icloud[a-z.]*|appclip|homekit|arkit|facetime|siri';
  const restricted = [
    // A symbol named in a `systemName:` or `systemImage:` argument (including `swift`).
    ...git([
      'grep',
      '-hoE',
      `(systemName|systemImage): *"(${restrictedNames}|swift)"`,
      '--',
      'platforms/ios',
    ])
      .split('\n')
      .filter(Boolean),
    // The same names as plain string literals in the library sources, for example a
    // symbol returned from a helper (`case .mobile: "iphone"`) and later passed to Image.
    ...git(['grep', '-hoE', `"(${restrictedNames})"`, '--', 'platforms/ios/Sources'])
      .split('\n')
      .filter(Boolean),
  ];
  if (restricted.length > 0)
    problems.push(
      `SF Symbols that depict Apple products or marks are not allowed: ${[...new Set(restricted)].join(', ')}`,
    );
}

// Material Symbols bundled in Monarch for Android (Apache-2.0): every vendored SVG is one that
// platforms/icons.json marks bundled, and THIRD_PARTY_NOTICES.md credits the set and its license.
{
  const { readdirSync } = await import('node:fs');
  const dir = 'platforms/android/glyphs/material-symbols-outlined';
  const icons = JSON.parse(read('platforms/icons.json'));
  const bundled = new Set(
    [...icons.icons, ...(icons.extra ?? [])].filter((i) => i.bundled).map((i) => i.android),
  );
  const vendored = existsSync(join(root, dir))
    ? readdirSync(join(root, dir)).filter((f) => f.endsWith('.svg'))
    : [];
  for (const file of vendored) {
    if (!bundled.has(file.replace(/\.svg$/, '')))
      problems.push(`${dir}/${file} is not marked bundled in platforms/icons.json.`);
  }
  for (const name of bundled) {
    if (!vendored.includes(`${name}.svg`))
      problems.push(`${dir}/${name}.svg is missing (bundled in platforms/icons.json).`);
  }
  const notices = read('THIRD_PARTY_NOTICES.md');
  if (vendored.length && !(notices.includes('Material Symbols') && notices.includes(dir)))
    problems.push(
      'THIRD_PARTY_NOTICES.md must credit the bundled Material Symbols (Apache-2.0) and name ' +
        dir +
        '.',
    );
  const glyphs = read(
    'platforms/android/monarch-compose/src/main/kotlin/com/rookhq/monarch/internal/MonarchGlyphs.kt',
  );
  if (!glyphs.includes('Material Symbols: Copyright Google LLC, Apache License 2.0'))
    problems.push('MonarchGlyphs.kt must carry the Material Symbols copyright and license line.');
}

if (problems.length > 0) {
  console.error(`legal: ${problems.length} problem(s)\n- ${problems.join('\n- ')}`);
  process.exit(1);
}
console.log('legal: LICENSE, NOTICE, TRADEMARK.md and package licenses are in order');
