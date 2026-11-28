// Keeps the native version equal to the monarch-react version (decision D7):
// Maven publishes dev.monarchui:*:<version>, SwiftPM is tagged <version>,
// and MonarchUI.version reports it. packages/react/package.json is the only
// place the version is written by hand (Changesets bumps it).
//
//   node scripts/native/sync-version.mjs           write the version into the native files
//   node scripts/native/sync-version.mjs --check   fail if a native file has another version
//   node scripts/native/sync-version.mjs --print   print the version only
//
// Files it writes:
//   platforms/android/gradle/libs.versions.toml   monarch = "<version>"
//   platforms/ios/Sources/MonarchUI/MonarchUI.swift   public static let version = "<version>"
//   platforms/ios/Sample/project.yml   MARKETING_VERSION: <version> (the catalogue app)
// The Android catalogue app reads its versionName from the catalog entry.
//
// The native libraries stay in the 0.14.x patch series, like monarch-react; a
// version outside it fails, so a minor bump is a deliberate change to this script.
// Plain Node, no dependencies.
import { readFileSync, writeFileSync } from 'node:fs';

const root = new URL('../../', import.meta.url);
const SERIES = /^0\.14\.\d+$/;

const TARGETS = [
  {
    file: 'platforms/android/gradle/libs.versions.toml',
    pattern: /^(monarch = ")([^"]*)(")$/m,
  },
  {
    file: 'platforms/ios/Sources/MonarchUI/MonarchUI.swift',
    pattern: /^(\s*public static let version = ")([^"]*)(")$/m,
  },
  {
    file: 'platforms/ios/Sample/project.yml',
    pattern: /^(\s*MARKETING_VERSION: )(\S+)()$/m,
  },
];

export function reactVersion() {
  const { version } = JSON.parse(
    readFileSync(new URL('packages/react/package.json', root), 'utf8'),
  );
  if (!SERIES.test(version)) {
    throw new Error(
      `packages/react/package.json version ${version} is outside the 0.14.x patch series the native libraries follow.`,
    );
  }
  return version;
}

const mode = process.argv.includes('--check')
  ? 'check'
  : process.argv.includes('--print')
    ? 'print'
    : 'write';

const version = reactVersion();
if (mode === 'print') {
  console.log(version);
  process.exit(0);
}

const stale = [];
for (const { file, pattern } of TARGETS) {
  const url = new URL(file, root);
  const text = readFileSync(url, 'utf8');
  const match = text.match(pattern);
  if (!match) throw new Error(`${file}: no version line matching ${pattern}`);
  if (match[2] === version) continue;
  stale.push(`${file}: ${match[2]}`);
  if (mode === 'write') writeFileSync(url, text.replace(pattern, `$1${version}$3`));
}

if (mode === 'check' && stale.length > 0) {
  console.error(
    `sync-version: the native version must be ${version} (packages/react/package.json):`,
  );
  for (const line of stale) console.error(`  ${line}`);
  console.error('Run `node scripts/native/sync-version.mjs`.');
  process.exit(1);
}
console.log(
  mode === 'write' && stale.length > 0
    ? `sync-version: native version set to ${version} (${stale.length} files)`
    : `sync-version: native version is ${version}`,
);
