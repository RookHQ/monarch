// Keeps registry/compatibility.json in step with package versions. Runs after
// `changeset version`, so the matrix can never fall behind a release.
import { readFileSync, writeFileSync } from 'node:fs';

const file = new URL('../registry/compatibility.json', import.meta.url);
const compat = JSON.parse(readFileSync(file, 'utf8'));
for (const name of ['tokens', 'react', 'conformance']) {
  const pkg = JSON.parse(
    readFileSync(new URL(`../packages/${name}/package.json`, import.meta.url), 'utf8'),
  );
  const [major, minor] = pkg.version.split('.').map(Number);
  // 0.x: a minor is breaking, so support the current minor line only.
  const supported =
    major === 0 ? `>=0.${minor}.0 <0.${minor + 1}.0` : `>=${major}.0.0 <${major + 1}.0.0`;
  const entry = compat.packages[pkg.name] ?? { deprecated: [] };
  compat.packages[pkg.name] = { ...entry, current: pkg.version, supported };
}
writeFileSync(file, JSON.stringify(compat, null, 2) + '\n');
console.log('compatibility.json synced');
