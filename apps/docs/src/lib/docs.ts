/**
 * Component and pattern docs: one page per `<id>.docs.tsx` in packages/react/src
 * (docs/architecture/SOURCE-LAYOUT.md).
 *
 * The shell needs only the small index from `virtual:monarch-docs-index`
 * (ids, titles, families). Per-page details (summaries, exports, registry
 * entries) are a second module, `virtual:monarch-docs-details`, loaded on
 * demand by search, the component cards and component pages, so the entry
 * chunk does not grow with every summary. The docs files themselves, with
 * their live examples and extracted example source, load on demand too: one
 * chunk per page.
 */
import { use } from 'react';
import { exampleCount, pages, reactVersion } from 'virtual:monarch-docs-index';
import type { DocsPageDetails, RegistryRef } from 'virtual:monarch-docs-details';
import type {
  ComponentDocs,
  Example,
} from '../../../../packages/react/src/internal/component-docs.js';
import type { ExtractedExample } from '../../plugins/extract-examples.js';
import type { ExportDoc } from './manifest.js';

export type { ComponentDocs, DocsPageDetails, Example, ExtractedExample, RegistryRef };
export {
  DOC_SECTIONS,
  DOC_SECTION_TITLES,
  isIncomplete,
  type DocSectionId,
} from '../../../../packages/react/src/internal/component-docs.js';

export { reactVersion };
/** Examples across every docs file. */
export const totalExamples: number = exampleCount;

/** What the shell knows about a docs page without loading anything. */
export interface DocsPageMeta {
  id: string;
  title: string;
  /** `components/actions/button` */
  folder: string;
  layer: 'components' | 'patterns';
  family: string;
  /** Package entry the exports come from: `.`, `./data`, `./marketing` … */
  entry: string;
}

/** Every docs page, by title. */
export const docsPages: readonly DocsPageMeta[] = pages
  .map((p) => ({
    ...p,
    folder: `${p.layer}/${p.family}/${p.id}`,
    entry: p.entry ?? '.',
  }))
  .sort((a, b) => a.title.localeCompare(b.title));
export const pageById: Readonly<Record<string, DocsPageMeta>> = Object.fromEntries(
  docsPages.map((p) => [p.id, p]),
);

/** Pages for public marketing sites, kept in their own navigation group. */
export const isMarketing = (p: DocsPageMeta) => p.id.startsWith('marketing-');

/** Families in taxonomy order (docs/architecture/COMPONENT-TAXONOMY.md). */
export const FAMILY_TITLES: Readonly<Record<string, string>> = {
  layout: 'Layout',
  typography: 'Typography',
  actions: 'Actions',
  forms: 'Forms',
  navigation: 'Navigation',
  'data-display': 'Data display',
  'data-grid': 'Tables and data grid',
  feedback: 'Status and feedback',
  overlays: 'Overlays',
  scheduling: 'Date, time and scheduling',
  charts: 'Charts',
  files: 'Files',
  search: 'Search and discovery',
  identity: 'Identity',
  collaboration: 'Collaboration',
  workflow: 'Workflow',
  intelligence: 'Intelligence',
  crm: 'CRM',
  technical: 'Technical infrastructure',
  billing: 'Billing',
  geo: 'Location',
  interaction: 'Advanced interaction',
};

/**
 * Routes from before pages were keyed by registry id. Old links keep working
 * and show the canonical page.
 */
export const ROUTE_ALIASES: Readonly<Record<string, string>> = {
  '/components/field': '/components/form-field',
  '/components/input': '/components/text-field',
};

/** The import line for a page's exports. */
export function importLine(page: DocsPageMeta, exports: readonly string[]): string {
  const from =
    page.entry === '.'
      ? '@rookcom/monarch-react'
      : `@rookcom/monarch-react/${page.entry.replace(/^\.\//, '')}`;
  return `import { ${exports.join(', ')} } from '${from}';`;
}

// ---------------------------------------------------------------- lazy docs modules

export interface LoadedDocs {
  docs: ComponentDocs;
  /** One entry per example, in order: the extracted `render` source. */
  source: readonly ExtractedExample[];
  /** Manifest entries for the page's exports, in export order. */
  api: readonly ExportDoc[];
}

interface DocsModule {
  default: ComponentDocs;
  exampleSource: ExtractedExample[];
  apiEntries: ExportDoc[];
}

/**
 * Marks a promise as settled the way React's `use()` reads it, so a page whose
 * docs file is already loaded (prefetched, or loaded before hydration) renders
 * in one pass instead of suspending once.
 */
function track<T>(p: Promise<T>): Promise<T> {
  const t = p as Promise<T> & { status?: string; value?: T; reason?: unknown };
  t.status = 'pending';
  p.then(
    (value) => {
      t.status = 'fulfilled';
      t.value = value;
    },
    (reason: unknown) => {
      t.status = 'rejected';
      t.reason = reason;
    },
  );
  return p;
}

// ---------------------------------------------------------------- lazy page details

export interface LoadedDetails {
  /** Summary, exports and example count, by page id. */
  details: Readonly<Record<string, DocsPageDetails>>;
  registryById: Readonly<Record<string, RegistryRef>>;
}

let detailsPromise: Promise<LoadedDetails> | null = null;
/** Loads the per-page details once (a chunk of its own). */
export function loadDetails(): Promise<LoadedDetails> {
  detailsPromise ??= track(
    import('virtual:monarch-docs-details').then((m) => ({
      details: m.details,
      registryById: Object.fromEntries(m.registry.map((e) => [e.id, e])),
    })),
  );
  return detailsPromise;
}

/** Suspends until the per-page details are loaded. */
export function useDetails(): LoadedDetails {
  return use(loadDetails());
}

const loaders = import.meta.glob<DocsModule>('../../../../packages/react/src/**/*.docs.tsx');
const loaderById: Record<string, () => Promise<DocsModule>> = {};
for (const [file, load] of Object.entries(loaders)) {
  const id = /([^/]+)\.docs\.tsx$/.exec(file)?.[1];
  if (id) loaderById[id] = load;
}

const cache = new Map<string, Promise<LoadedDocs>>();

/** True when the page has a docs file. */
export const hasDocs = (id: string): boolean => id in loaderById;

/** Loads one docs file. The promise is cached, so `use()` can read it without re-suspending. */
export function loadDocs(id: string): Promise<LoadedDocs> {
  let p = cache.get(id);
  if (!p) {
    const load = loaderById[id];
    if (!load) return Promise.reject(new Error(`No docs file for "${id}"`));
    p = track(
      load().then((m) => {
        const count = examplesOf(m.default).length;
        if (m.exampleSource.length !== count)
          throw new Error(
            `${id}: ${count} examples but ${m.exampleSource.length} extracted sources (plugins/extract-examples.ts)`,
          );
        return { docs: m.default, source: m.exampleSource, api: m.apiEntries };
      }),
    );
    cache.set(id, p);
  }
  return p;
}

let all: Promise<Record<string, LoadedDocs>> | null = null;
/** Every docs file, keyed by id. */
export function loadAllDocs(): Promise<Record<string, LoadedDocs>> {
  all ??= track(
    Promise.all(docsPages.map((p) => loadDocs(p.id).then((d) => [p.id, d] as const))).then(
      (entries) => Object.fromEntries(entries),
    ),
  );
  return all;
}

/** Suspends until the docs file is loaded. */
export function useDocs(id: string): LoadedDocs {
  return use(loadDocs(id));
}

export function useAllDocs(): Record<string, LoadedDocs> {
  return use(loadAllDocs());
}

/** The examples of a docs file, whether its section is complete or partial. */
export function examplesOf(docs: ComponentDocs): Example[] {
  const s = docs.sections.examples;
  if ('items' in s) return s.items;
  return (s.partial?.items as Example[] | undefined) ?? [];
}
