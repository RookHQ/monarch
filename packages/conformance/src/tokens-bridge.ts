/**
 * Reaches @rookcom/monarch-tokens from the app (or a Monarch checkout) at run
 * time, so the conformance package keeps no runtime dependencies. Used by the
 * MCP server's list_tokens and get_theme tools.
 */
import { existsSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const TOKENS = '@rookcom/monarch-tokens';

/** The tokens package directory: the app's install first, then a Monarch checkout. */
export function tokensDir(root: string): string | null {
  try {
    const require = createRequire(join(root, 'package.json'));
    const pkgJson = require.resolve(`${TOKENS}/package.json`);
    return join(pkgJson, '..');
  } catch {
    const checkout = fileURLToPath(new URL('../../tokens', import.meta.url));
    return existsSync(join(checkout, 'dist', 'index.js')) ? checkout : null;
  }
}

const missing = () => new Error(`${TOKENS} is not installed in this project: npm i ${TOKENS}`);

/** dist/tokens.json of the tokens package. */
export function readTokensJson(root: string): unknown {
  const dir = tokensDir(root);
  const path = dir ? join(dir, 'dist', 'tokens.json') : null;
  if (!path || !existsSync(path)) throw missing();
  return JSON.parse(readFileSync(path, 'utf8')) as unknown;
}

export interface DeriveThemeOptions {
  readonly ai?: string;
  readonly fallback?: string;
  readonly compat?: boolean;
}

export interface TokensModule {
  deriveTheme(brand: string, options?: DeriveThemeOptions): unknown;
  brandThemeCss(theme: unknown, options?: { selector?: string; layer?: boolean }): string;
}

const isTokensModule = (m: unknown): m is TokensModule =>
  typeof m === 'object' &&
  m !== null &&
  typeof (m as Record<string, unknown>).deriveTheme === 'function' &&
  typeof (m as Record<string, unknown>).brandThemeCss === 'function';

export async function loadTokensModule(root: string): Promise<TokensModule> {
  const dir = tokensDir(root);
  const entry = dir ? join(dir, 'dist', 'index.js') : null;
  if (!entry || !existsSync(entry)) throw missing();
  const mod: unknown = await import(pathToFileURL(entry).href);
  if (!isTokensModule(mod)) throw new Error(`${TOKENS} at ${dir} has no deriveTheme`);
  return mod;
}

/** Flattens a DTCG-style tree into `path → $value` rows, optionally filtered by a path prefix. */
export function flattenTokens(
  tree: unknown,
  prefix = '',
): { path: string; type?: string; value: unknown }[] {
  const out: { path: string; type?: string; value: unknown }[] = [];
  const visit = (node: unknown, path: string) => {
    if (typeof node !== 'object' || node === null) return;
    const rec = node as Record<string, unknown>;
    if ('$value' in rec) {
      out.push({
        path,
        ...(typeof rec.$type === 'string' ? { type: rec.$type } : {}),
        value: rec.$value,
      });
      return;
    }
    for (const [k, v] of Object.entries(rec))
      if (!k.startsWith('$')) visit(v, path ? `${path}.${k}` : k);
  };
  visit(tree, '');
  return prefix ? out.filter((r) => r.path.startsWith(prefix)) : out;
}
