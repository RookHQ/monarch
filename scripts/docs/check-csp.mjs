/**
 * CSP check for the built docs site (deploy/docs-site/Caddyfile.snippet).
 *
 * The production Content-Security-Policy allows inline scripts only by hash.
 * This computes the hash of every executable inline script in the built pages
 * and fails if the set differs from the one in the Caddy snippet, so a deploy
 * can never ship pages the policy would block. `--print` shows the hashes.
 */
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const dist = path.join(root, 'apps/docs/dist');
const snippet = path.join(root, 'deploy/docs-site/Caddyfile.snippet');
if (!existsSync(path.join(dist, 'index.html'))) {
  console.error('Docs CSP check: apps/docs/dist is missing; run `npm run build:docs` first.');
  process.exit(1);
}

function htmlFiles(dir) {
  const out = [];
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...htmlFiles(full));
    else if (e.name.endsWith('.html')) out.push(full);
  }
  return out;
}

// Docs-only demo frames (EmbeddedAppFrame stand-in) build a blob: page with this inline script;
// the blob inherits the site policy, so its hash is allowed too. It changes only when
// packages/react/src/patterns/technical/embedded-app-frame/embedded-app-frame.docs.tsx does.
const DEMO_FRAME_HASHES = ["'sha256-YmBFy6fCAkcT3f61J8F5cdJlexU6nKhCYk1qi4KQqbw='"];

const built = new Set(DEMO_FRAME_HASHES);
for (const file of htmlFiles(dist)) {
  const html = readFileSync(file, 'utf8');
  for (const m of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/g)) {
    const attrs = m[1];
    if (/\bsrc=/.test(attrs)) continue;
    // Data blocks (JSON-LD and the like) are not executed, so the policy does not govern them.
    const type = /\btype="([^"]*)"/.exec(attrs)?.[1];
    if (type && !/^(module|text\/javascript)$/.test(type)) continue;
    built.add(`'sha256-${createHash('sha256').update(m[2]).digest('base64')}'`);
  }
}

if (process.argv.includes('--print')) {
  console.log([...built].sort().join(' '));
  process.exit(0);
}

const configured = new Set(readFileSync(snippet, 'utf8').match(/'sha256-[A-Za-z0-9+/=]+'/g) ?? []);
const missing = [...built].filter((h) => !configured.has(h));
const stale = [...configured].filter((h) => !built.has(h));
if (missing.length || stale.length) {
  console.error(
    'Docs CSP check failed: script hashes in deploy/docs-site/Caddyfile.snippet are out of date.',
  );
  for (const h of missing) console.error(`  missing: ${h}`);
  for (const h of stale) console.error(`  stale:   ${h}`);
  console.error(
    'Update the snippet (`node scripts/docs/check-csp.mjs --print`), then re-run server-setup.sh on the server.',
  );
  process.exit(1);
}
console.log(`Docs CSP check ok: ${built.size} inline scripts, all allowed by hash.`);
