/**
 * Vite plugin that turns the component docs files into docs-site data.
 *
 * - `virtual:monarch-docs-index`: a small index the site shell loads up front
 *   (navigation, search, the components overview): id, title, summary and
 *   folder of every `<id>.docs.tsx`, the registry entries the pages link to,
 *   and the published version of `@rookcom/monarch-react`. The docs files
 *   themselves are loaded lazily, one chunk per page.
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
      exampleTitles: docs.examples.map((e) => e.title),
    };
  });
  return {
    reactVersion,
    pages,
    registry: registry.map(({ id, name, status }) => ({ id, name, status })),
    files,
  };
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
      return id === INDEX_ID ? RESOLVED_INDEX_ID : undefined;
    },
    load(id) {
      if (id !== RESOLVED_INDEX_ID) return undefined;
      let index: ReturnType<typeof buildIndex>;
      try {
        index = buildIndex(repoRoot);
      } catch (e) {
        this.error(e instanceof Error ? e.message : String(e));
      }
      for (const f of index.files) this.addWatchFile(f);
      this.addWatchFile(path.join(repoRoot, 'registry/components.json'));
      return [
        `export const reactVersion = ${JSON.stringify(index.reactVersion)};`,
        `export const pages = ${JSON.stringify(index.pages)};`,
        `export const registry = ${JSON.stringify(index.registry)};`,
      ].join('\n');
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
      // A docs file's title or summary feeds the index; rebuild it.
      if (DOCS_FILE.test(file) || file === manifestFile) {
        exportsById = null;
        manifest = null;
      }
      if (DOCS_FILE.test(file)) {
        const mod = server.moduleGraph.getModuleById(RESOLVED_INDEX_ID);
        if (mod) server.moduleGraph.invalidateModule(mod);
      }
    },
  };
}
