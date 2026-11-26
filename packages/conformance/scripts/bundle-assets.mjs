// Copies what the CLI reads at run time into dist/ (step 2 of `npm run build`):
//   dist/templates/agents/   templates/agents (monarch init-agents)
//   dist/templates/starters/ templates/starters (monarch create), without anything a local
//                            trial run left behind (node_modules, .next, dist)
//   dist/data/manifest.json  packages/react/dist/manifest.json (monarch search, add, mcp),
//                            used when the app has no @rookcom/monarch-react installed
import { cpSync, existsSync, mkdirSync } from 'node:fs';
import { basename, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const pkg = fileURLToPath(new URL('..', import.meta.url));
const repo = join(pkg, '..', '..');
const dist = join(pkg, 'dist');

const templates = join(repo, 'templates', 'agents');
if (!existsSync(templates)) throw new Error('templates/agents is missing');
cpSync(templates, join(dist, 'templates', 'agents'), { recursive: true });

const starters = join(repo, 'templates', 'starters');
if (!existsSync(starters)) throw new Error('templates/starters is missing');
const LEFTOVERS = new Set(['node_modules', '.next', 'dist', '.DS_Store']);
cpSync(starters, join(dist, 'templates', 'starters'), {
  recursive: true,
  filter: (src) => !LEFTOVERS.has(basename(src)),
});

const manifest = join(repo, 'packages', 'react', 'dist', 'manifest.json');
if (existsSync(manifest)) {
  mkdirSync(join(dist, 'data'), { recursive: true });
  cpSync(manifest, join(dist, 'data', 'manifest.json'));
  console.log('bundled templates/agents, templates/starters and the component manifest');
} else {
  // `npm run build` builds react first; a standalone build still works from the app's install.
  console.warn("packages/react/dist/manifest.json not built: the CLI will read the app's copy");
}
