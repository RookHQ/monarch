// Keeps registry/compatibility.json and the starter templates
// (templates/starters/*/package.json, which pin exact Monarch versions) in step
// with package versions. Runs after `changeset version`, so neither can fall
// behind a release.
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';

const file = new URL('../registry/compatibility.json', import.meta.url);
const compat = JSON.parse(readFileSync(file, 'utf8'));
const versions = {};
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
  versions[pkg.name] = pkg.version;
}
writeFileSync(file, JSON.stringify(compat, null, 2) + '\n');
console.log('compatibility.json synced');

const starters = new URL('../templates/starters/', import.meta.url);
let synced = 0;
for (const name of existsSync(starters) ? readdirSync(starters) : []) {
  const pkgFile = new URL(`${name}/package.json`, starters);
  if (!existsSync(pkgFile)) continue;
  const pkg = JSON.parse(readFileSync(pkgFile, 'utf8'));
  for (const group of ['dependencies', 'devDependencies'])
    for (const dep of Object.keys(pkg[group] ?? {}))
      if (versions[dep]) pkg[group][dep] = versions[dep];
  writeFileSync(pkgFile, JSON.stringify(pkg, null, 2) + '\n');
  synced += 1;
}
console.log(`starter templates synced: ${synced}`);
