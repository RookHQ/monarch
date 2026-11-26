/**
 * Reading the component manifest (`@rookcom/monarch-react/manifest.json`) for
 * `monarch search`, `monarch add` and the MCP server. The app's installed copy
 * wins, so answers match the version the app really uses; otherwise the copy
 * bundled with this package at build time, then the one in a Monarch checkout.
 */
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

export interface ManifestProp {
  readonly name: string;
  readonly type: string;
  readonly required: boolean;
  readonly description: string;
  readonly default?: string;
  readonly values?: readonly (string | number)[];
}

export interface ManifestExport {
  readonly name: string;
  readonly entry: string;
  readonly alsoEntries?: readonly string[];
  readonly kind: 'component' | 'hook' | 'utility';
  /** Older manifests (before 0.14.4) have no scope, import, css or docs. */
  readonly scope?: 'general' | 'rook';
  readonly description: string;
  readonly className?: string;
  readonly usesClient?: boolean;
  readonly clientOnly: boolean;
  readonly import?: string;
  readonly css?: readonly string[];
  readonly componentCss?: string;
  readonly docs?: string;
  /** Minified, gzipped bytes of the component's module (0.14.5 and later). */
  readonly size?: { readonly gzip: number };
  readonly props: readonly ManifestProp[];
}

export interface Manifest {
  readonly package: string;
  readonly version: string;
  readonly styles: string;
  readonly tokens: string;
  readonly components: readonly ManifestExport[];
}

export interface LoadedManifest {
  readonly manifest: Manifest;
  /** Where it was read from. */
  readonly path: string;
}

const PKG = '@rookcom/monarch-react';

/** Candidate manifest paths, most specific first. */
export function manifestCandidates(root: string, explicit?: string): string[] {
  const here = fileURLToPath(new URL('.', import.meta.url));
  return [
    ...(explicit ? [explicit] : []),
    join(root, 'node_modules', PKG, 'dist', 'manifest.json'),
    // Bundled at build time (scripts/bundle-assets.mjs): dist/data/manifest.json.
    join(here, 'data', 'manifest.json'),
    // In a Monarch checkout: packages/conformance/{src,dist} → packages/react/dist.
    join(here, '..', '..', 'react', 'dist', 'manifest.json'),
  ];
}

export function loadManifest(root: string, explicit?: string): LoadedManifest {
  for (const path of manifestCandidates(root, explicit)) {
    if (!existsSync(path)) continue;
    const manifest = JSON.parse(readFileSync(path, 'utf8')) as Manifest;
    if (!Array.isArray(manifest.components)) throw new Error(`${path} is not a Monarch manifest`);
    return { manifest, path };
  }
  throw new Error(
    `No component manifest found. Install ${PKG} in the app (it ships manifest.json), or pass --manifest FILE.`,
  );
}

/** `.` → `@rookcom/monarch-react`; `./ai` → `@rookcom/monarch-react/ai`. */
export const moduleOf = (entry: string): string =>
  entry === '.' ? PKG : `${PKG}/${entry.replace(/^\.\//, '')}`;

/** Fields newer manifests carry, derived for older ones. */
export function importOf(e: ManifestExport): string {
  return e.import ?? `import { ${e.name} } from '${moduleOf(e.entry)}';`;
}

export function cssOf(e: ManifestExport): readonly string[] {
  if (e.css) return e.css;
  const sheets = ['@rookcom/monarch-tokens/css', `${PKG}/styles/core.css`];
  if (e.entry !== '.') sheets.push(`${PKG}/styles/${e.entry.replace(/^\.\//, '')}.css`);
  return sheets;
}

export function scopeOf(e: ManifestExport): 'general' | 'rook' {
  return e.scope ?? (/Rook|^rook|^ROOK_/.test(e.name) ? 'rook' : 'general');
}

export function findExport(manifest: Manifest, name: string): ManifestExport | undefined {
  const exact = manifest.components.find((c) => c.name === name);
  if (exact) return exact;
  const lower = name.toLowerCase().replace(/[-_\s]/g, '');
  return manifest.components.find((c) => c.name.toLowerCase() === lower);
}

/** The first sentence of a description, for one-line listings. */
export function summaryOf(description: string): string {
  const first =
    description
      .split(/\n\s*\n/)[0]
      ?.replace(/\s+/g, ' ')
      .trim() ?? '';
  const sentence = /^(.+?[.!?])(?:\s|$)/.exec(first)?.[1] ?? first;
  return sentence.length > 160 ? `${sentence.slice(0, 157)}…` : sentence;
}
