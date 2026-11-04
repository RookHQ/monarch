/**
 * Component and pattern docs: one page per `<id>.docs.tsx` in packages/react/src
 * (docs/architecture/SOURCE-LAYOUT.md).
 *
 * The shell needs only the small index from `virtual:monarch-docs-index`
 * (titles, summaries, folders). The docs files themselves, with their live
 * examples and extracted example source, load on demand: one chunk per page.
 */
import { use } from 'react';
import { pages, registry, reactVersion, type DocsPageMeta } from 'virtual:monarch-docs-index';
import type {
  ComponentDocs,
  Example,
} from '../../../../packages/react/src/internal/component-docs.js';
import type { ExtractedExample } from '../../plugins/extract-examples.js';

export type { ComponentDocs, DocsPageMeta, Example, ExtractedExample };
export {
  DOC_SECTIONS,
  DOC_SECTION_TITLES,
  isIncomplete,
  type DocSectionId,
} from '../../../../packages/react/src/internal/component-docs.js';

export { reactVersion };

/** Every docs page, by title. */
export const docsPages: readonly DocsPageMeta[] = [...pages].sort((a, b) =>
  a.title.localeCompare(b.title),
);
export const pageById: Readonly<Record<string, DocsPageMeta>> = Object.fromEntries(
  docsPages.map((p) => [p.id, p]),
);
export const registryById = Object.fromEntries(registry.map((e) => [e.id, e]));

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
export function importLine(page: DocsPageMeta): string {
  const from =
    page.entry === '.'
      ? '@rookcom/monarch-react'
      : `@rookcom/monarch-react/${page.entry.replace(/^\.\//, '')}`;
  return `import { ${page.exports.join(', ')} } from '${from}';`;
}

// ---------------------------------------------------------------- lazy docs modules

export interface LoadedDocs {
  docs: ComponentDocs;
  /** One entry per example, in order: the extracted `render` source. */
  source: readonly ExtractedExample[];
}

interface DocsModule {
  default: ComponentDocs;
  exampleSource: ExtractedExample[];
}

const loaders = import.meta.glob<DocsModule>('../../../../packages/react/src/**/*.docs.tsx');
const loaderById: Record<string, () => Promise<DocsModule>> = {};
for (const [file, load] of Object.entries(loaders)) {
  const id = /([^/]+)\.docs\.tsx$/.exec(file)?.[1];
  if (id) loaderById[id] = load;
}

const cache = new Map<string, Promise<LoadedDocs>>();

/** Loads one docs file. The promise is cached, so `use()` can read it without re-suspending. */
export function loadDocs(id: string): Promise<LoadedDocs> {
  let p = cache.get(id);
  if (!p) {
    const load = loaderById[id];
    if (!load) return Promise.reject(new Error(`No docs file for "${id}"`));
    p = load().then((m) => {
      const count = examplesOf(m.default).length;
      if (m.exampleSource.length !== count)
        throw new Error(
          `${id}: ${count} examples but ${m.exampleSource.length} extracted sources (plugins/extract-examples.ts)`,
        );
      return { docs: m.default, source: m.exampleSource };
    });
    cache.set(id, p);
  }
  return p;
}

let all: Promise<Record<string, LoadedDocs>> | null = null;
/** Every docs file, keyed by id. */
export function loadAllDocs(): Promise<Record<string, LoadedDocs>> {
  all ??= Promise.all(docsPages.map((p) => loadDocs(p.id).then((d) => [p.id, d] as const))).then(
    (entries) => Object.fromEntries(entries),
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
