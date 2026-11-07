/**
 * Vite plugin that turns the component docs files into docs-site data.
 *
 * - `virtual:monarch-docs-index`: the small index the site shell loads up front
 *   (sidebar, routes, page titles): id, title, layer, family and package entry
 *   of every `<id>.docs.tsx`, the total example count and the published
 *   version of `@rookcom/monarch-react`. It is part of the entry chunk, so it
 *   carries nothing per page that the shell does not render.
 * - `virtual:monarch-docs-details`: everything else per page (summary, exports,
 *   example count) and the registry entries the pages link to. Search, the
 *   overview cards and component pages load it lazily (`loadDetails()` in
 *   src/lib/docs.ts); the server entry imports it directly.
 *   `virtual:monarch-docs-details?pages=a,b` is the same module for just those
 *   pages, in that order (the home page's showcase), so a page that shows a
 *   few cards does not load every summary. An unknown id fails the build.
 * - The docs files themselves are loaded lazily, one chunk per page.
 * - Every `<id>.docs.tsx` gets an extra export, `exampleSource`: the source of
 *   each example's `render`, extracted with the TypeScript compiler API
 *   (`extract-examples.ts`). "Show code" displays it, so example code is
 *   never written twice by hand.
 *
 * - Every `<id>.docs.tsx` also gets `apiEntries`: the entries of
 *   `@rookcom/monarch-react/manifest.json` for the folder's exports, so a
 *   component page loads the API of its own component, not the whole manifest.
 *
 * An example that cannot be extracted fails the build with its file and line.
 */
import { readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import type { Plugin } from 'vite';
import { extractDocs, extractExamples } from './extract-examples.js';

const INDEX_ID = 'virtual:monarch-docs-index';
const RESOLVED_INDEX_ID = `\0${INDEX_ID}`;
const DETAILS_ID = 'virtual:monarch-docs-details';
const RESOLVED_DETAILS_ID = `\0${DETAILS_ID}`;
const DOCS_FILE = /[\\/]packages[\\/]react[\\/]src[\\/](.+)\.docs\.tsx$/;

interface RegistryEntry {
  id: string;
  name: string;
  category: string;
  layer: string;
  status: string;
  entry?: string;
  exports: string[];
  source?: string;
}

export interface MonarchDocsOptions {
  /** Repository root (contains packages/ and registry/). */
  repoRoot: string;
}

/** All docs files under packages/react/src, sorted, as absolute paths. */
export function docsFiles(repoRoot: string): string[] {
  const src = path.join(repoRoot, 'packages/react/src');
  return readdirSync(src, { recursive: true, encoding: 'utf8' })
    .filter((f) => f.endsWith('.docs.tsx'))
    .map((f) => path.join(src, f))
    .sort();
}

/** The index module's data. Exported for tests. */
export function buildIndex(repoRoot: string) {
  const registry = (
    JSON.parse(readFileSync(path.join(repoRoot, 'registry/components.json'), 'utf8')) as {
      components: RegistryEntry[];
    }
  ).components;
  const reactVersion = (
    JSON.parse(readFileSync(path.join(repoRoot, 'packages/react/package.json'), 'utf8')) as {
      version: string;
    }
  ).version;
  const files = docsFiles(repoRoot);
  const pages = files.map((file) => {
    const rel = path
      .relative(path.join(repoRoot, 'packages/react/src'), file)
      .replaceAll('\\', '/');
    const m = /^((components|patterns)\/([^/]+)\/([^/]+))\/\4\.docs\.tsx$/.exec(rel);
    if (!m) throw new Error(`${rel}: docs file outside a component folder`);
    const docs = extractDocs(file, readFileSync(file, 'utf8'));
    if (docs.id !== m[4]) throw new Error(`${rel}: id "${docs.id}" does not match its folder`);
    const source = `packages/react/src/${m[1]}`;
    const own = registry.filter((e) => e.source === source);
    // The folder's own registry entry first, then any other entry it also implements.
    own.sort((a, b) => Number(b.id === docs.id) - Number(a.id === docs.id));
    return {
      id: docs.id,
      title: docs.title,
      summary: docs.summary,
      folder: m[1] as string,
      layer: m[2] as 'components' | 'patterns',
      family: m[3] as string,
      entry: own[0]?.entry ?? '.',
      exports: own.flatMap((e) => e.exports),
      exampleCount: docs.examples.length,
    };
  });
  return {
    reactVersion,
    pages,
    registry: registry.map(({ id, name, status }) => ({ id, name, status })),
    files,
  };
}

type DocsIndex = ReturnType<typeof buildIndex>;

/** Source of `virtual:monarch-docs-index`: only what the always-visible shell needs. */
export function indexModule(index: DocsIndex): string {
  const pages = index.pages.map(({ id, title, layer, family, entry }) =>
    entry === '.' ? { id, title, layer, family } : { id, title, layer, family, entry },
  );
  const exampleCount = index.pages.reduce((n, p) => n + p.exampleCount, 0);
  return [
    `export const reactVersion = ${JSON.stringify(index.reactVersion)};`,
    `export const exampleCount = ${exampleCount};`,
    `export const pages = ${JSON.stringify(pages)};`,
  ].join('\n');
}

/**
 * Source of `virtual:monarch-docs-details`: per-page details, loaded lazily.
 * With `only`, just those pages, in that order, and no registry.
 */
export function detailsModule(index: DocsIndex, only?: readonly string[]): string {
  const byId = new Map(index.pages.map((p) => [p.id, p]));
  const ids = only ?? index.pages.map((p) => p.id);
  const details = Object.fromEntries(
    ids.map((id) => {
      const page = byId.get(id);
      if (!page) throw new Error(`virtual:monarch-docs-details: no docs page "${id}"`);
      const { summary, exports, exampleCount } = page;
      return [id, { summary, exports, exampleCount }];
    }),
  );
  return [
    `export const details = ${JSON.stringify(details)};`,
    `export const registry = ${JSON.stringify(only ? [] : index.registry)};`,
  ].join('\n');
}

/** The page ids of a `virtual:monarch-docs-details?pages=a,b` request, or undefined for all. */
function detailsSubset(id: string): string[] | undefined {
  const query = id.slice(RESOLVED_DETAILS_ID.length);
  if (!query) return undefined;
  const pages = new URLSearchParams(query.slice(1)).get('pages');
  return (pages ?? '').split(',').filter(Boolean);
}

interface ManifestEntry {
  name: string;
}

export function monarchDocs({ repoRoot }: MonarchDocsOptions): Plugin {
  const manifestFile = path.join(repoRoot, 'packages/react/dist/manifest.json');
  let exportsById: Map<string, string[]> | null = null;
  let manifest: Map<string, ManifestEntry> | null = null;
  /** The manifest entries for one docs file's exports, in export order. */
  const apiEntries = (id: string): ManifestEntry[] => {
    exportsById ??= new Map(buildIndex(repoRoot).pages.map((p) => [p.id, p.exports]));
    manifest ??= new Map(
      (
        JSON.parse(readFileSync(manifestFile, 'utf8')) as { components: ManifestEntry[] }
      ).components.map((c) => [c.name, c]),
    );
    return (exportsById.get(id) ?? [])
      .map((name) => manifest?.get(name))
      .filter((e): e is ManifestEntry => e !== undefined);
  };
  return {
    name: 'monarch-docs',
    enforce: 'pre',
    resolveId(id) {
      if (id === INDEX_ID) return RESOLVED_INDEX_ID;
      if (id === DETAILS_ID || id.startsWith(`${DETAILS_ID}?`)) return `\0${id}`;
      return undefined;
    },
    load(id) {
      const isDetails = id === RESOLVED_DETAILS_ID || id.startsWith(`${RESOLVED_DETAILS_ID}?`);
      if (id !== RESOLVED_INDEX_ID && !isDetails) return undefined;
      try {
        const index = buildIndex(repoRoot);
        for (const f of index.files) this.addWatchFile(f);
        this.addWatchFile(path.join(repoRoot, 'registry/components.json'));
        return isDetails ? detailsModule(index, detailsSubset(id)) : indexModule(index);
      } catch (e) {
        this.error(e instanceof Error ? e.message : String(e));
      }
    },
    transform(code, id) {
      const file = id.split('?')[0] as string;
      if (!DOCS_FILE.test(file)) return undefined;
      let examples;
      try {
        examples = extractExamples(file, code);
      } catch (e) {
        this.error(e instanceof Error ? e.message : String(e));
      }
      const docsId = /([^\\/]+)\.docs\.tsx$/.exec(file)?.[1] ?? '';
      this.addWatchFile(manifestFile);
      return {
        code:
          `${code}\nexport const exampleSource = ${JSON.stringify(examples)};\n` +
          `export const apiEntries = ${JSON.stringify(apiEntries(docsId))};\n`,
        map: null,
      };
    },
    handleHotUpdate({ file, server }) {
      // A docs file's title or summary feeds the index and the details; rebuild them.
      if (DOCS_FILE.test(file) || file === manifestFile) {
        exportsById = null;
        manifest = null;
      }
      if (DOCS_FILE.test(file)) {
        for (const mod of server.moduleGraph.idToModuleMap.values())
          if (mod.id === RESOLVED_INDEX_ID || mod.id?.startsWith(RESOLVED_DETAILS_ID))
            server.moduleGraph.invalidateModule(mod);
      }
    },
  };
}
