/**
 * One docs page per component folder, read at build time from the
 * `<id>.docs.tsx` files in packages/react/src (docs/architecture/SOURCE-LAYOUT.md).
 * Which exports a page documents comes from the generated registry, so the
 * API tables always match the folder's real exports.
 */
import type { ComponentDocs } from '../../../../packages/react/src/internal/component-docs.js';

export type { ComponentDocs };
export {
  DOC_SECTIONS,
  DOC_SECTION_TITLES,
  isIncomplete,
  type DocSectionId,
  type Example,
} from '../../../../packages/react/src/internal/component-docs.js';

interface RegistryEntry {
  id: string;
  name: string;
  category: string;
  layer: string;
  status: string;
  exports: string[];
  source?: string;
}

const docsModules = import.meta.glob<ComponentDocs>(
  '../../../../packages/react/src/**/*.docs.tsx',
  {
    eager: true,
    import: 'default',
  },
);
const registryFiles = import.meta.glob<{ components: RegistryEntry[] }>(
  '../../../../registry/components.json',
  { eager: true, import: 'default' },
);
const registry = Object.values(registryFiles)[0]?.components ?? [];
export const registryById: Record<string, RegistryEntry> = Object.fromEntries(
  registry.map((e) => [e.id, e]),
);

export interface ComponentPageInfo {
  docs: ComponentDocs;
  /** `components/actions/button` */
  folder: string;
  family: string;
  layer: 'components' | 'patterns';
  /** Every value export of the folder, primary first. */
  exports: string[];
}

function pageFor(path: string, docs: ComponentDocs): ComponentPageInfo {
  const folder = /packages\/react\/src\/((components|patterns)\/([^/]+)\/[^/]+)\//.exec(path);
  if (!folder) throw new Error(`docs file outside a component folder: ${path}`);
  const source = `packages/react/src/${folder[1]}`;
  const own = registry.filter((e) => e.source === source);
  // The folder's own registry entry first, then any other entry it also implements.
  own.sort((a, b) => Number(b.id === docs.id) - Number(a.id === docs.id));
  return {
    docs,
    folder: folder[1] as string,
    family: folder[3] as string,
    layer: folder[2] as 'components' | 'patterns',
    exports: own.flatMap((e) => e.exports),
  };
}

export const componentPages: ComponentPageInfo[] = Object.entries(docsModules)
  .map(([path, docs]) => pageFor(path, docs))
  .sort((a, b) => a.docs.title.localeCompare(b.docs.title));

export const pageById: Record<string, ComponentPageInfo> = Object.fromEntries(
  componentPages.map((p) => [p.docs.id, p]),
);

/** Families in taxonomy order, for the index page. */
export const FAMILY_TITLES: Record<string, string> = {
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
export const ROUTE_ALIASES: Record<string, string> = {
  '/components/field': '/components/form-field',
  '/components/input': '/components/text-field',
};
