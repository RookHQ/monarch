/**
 * The site's information architecture, built from data: the fixed pages
 * below plus one entry per docs file (virtual:monarch-docs-index). The
 * sidebar, the search palette, page titles and previous/next links all read it.
 * Component entries carry no description or keywords: those come from the
 * lazily loaded page details (search-index.ts, entry-server.tsx), so the
 * entry chunk does not grow with every summary.
 */
import { FAMILY_TITLES, docsPages, isMarketing, type DocsPageMeta } from '../lib/docs.js';

export interface NavItem {
  path: string;
  label: string;
  /** One line for search results and the page's meta description (fixed pages only). */
  description?: string;
  /** Extra words search should match. */
  keywords?: readonly string[];
  /** Short context shown beside the label in search results (the component family). */
  hint?: string;
}

export interface NavGroup {
  id: string;
  heading: string;
  items: NavItem[];
}

const componentItem = (p: DocsPageMeta): NavItem => ({
  path: `/components/${p.id}`,
  label: p.title,
  keywords: [p.family, p.id],
  hint: FAMILY_TITLES[p.family] ?? p.family,
});

export const NAV: readonly NavGroup[] = [
  {
    id: 'foundations',
    heading: 'Foundations',
    items: [
      {
        path: '/introduction',
        label: 'Introduction',
        description: 'What Monarch is, its packages and how they fit together.',
      },
      {
        path: '/colours',
        label: 'Colours',
        description: 'Brand colours and semantic colour tokens, with live contrast ratios.',
        keywords: ['color', 'palette', 'contrast'],
      },
      {
        path: '/typography',
        label: 'Typography',
        description: 'Type scale, display sizes and weights.',
        keywords: ['font', 'type scale'],
      },
      {
        path: '/tokens',
        label: 'Tokens',
        description: 'Spacing, radius, elevation and motion tokens.',
        keywords: ['spacing', 'radius', 'shadow', 'motion', 'css variables'],
      },
      {
        path: '/theming',
        label: 'Theming',
        description: 'Light, dark and high contrast themes, and product accents.',
        keywords: ['dark mode', 'theme', 'accent', 'contrast'],
      },
    ],
  },
  {
    id: 'components',
    heading: 'Components',
    items: [
      {
        path: '/components',
        label: 'Overview',
        description: 'Every component and pattern, with a live preview.',
      },
      ...docsPages.filter((p) => p.layer === 'components').map(componentItem),
    ],
  },
  {
    id: 'patterns',
    heading: 'Patterns',
    items: [
      {
        path: '/patterns',
        label: 'Overview',
        description: 'Compositions of components for screens most applications have.',
      },
      ...docsPages.filter((p) => p.layer === 'patterns' && !isMarketing(p)).map(componentItem),
    ],
  },
  {
    id: 'marketing',
    heading: 'Marketing',
    items: [
      {
        path: '/marketing',
        label: 'Marketing pages',
        description: 'Building public product pages with the marketing entry.',
      },
      ...docsPages.filter(isMarketing).map(componentItem),
    ],
  },
  {
    id: 'guides',
    heading: 'Guides',
    items: [
      {
        path: '/getting-started',
        label: 'Getting started',
        description: 'Install the packages, load the styles and apply the theme.',
        keywords: ['install', 'setup'],
      },
      {
        path: '/migration',
        label: 'Migration',
        description: 'Moving an existing app onto Monarch.',
      },
      {
        path: '/conformance',
        label: 'Conformance',
        description: 'The monarch CLI: design-conformance checks for your codebase.',
        keywords: ['cli', 'lint', 'check'],
      },
      {
        path: '/products',
        label: 'Products',
        description: 'The Rook products registered with Monarch and their accents.',
      },
      {
        path: '/governance',
        label: 'Governance',
        description: 'How Monarch changes: versioning, deprecation, exceptions.',
      },
    ],
  },
];

/** Every page in reading order, for previous/next links. Overview pages included. */
export const NAV_ORDER: readonly (NavItem & { group: NavGroup })[] = NAV.flatMap((g) =>
  g.items.map((i) => ({ ...i, group: g })),
);

const byPath = new Map(NAV_ORDER.map((i) => [i.path, i]));

export function navItem(path: string) {
  return byPath.get(path);
}

/** The group a path belongs to (component pages belong to their layer's group). */
export function groupOf(path: string): NavGroup | undefined {
  return byPath.get(path)?.group;
}

export function neighbours(path: string) {
  const i = NAV_ORDER.findIndex((n) => n.path === path);
  if (i === -1) return { prev: undefined, next: undefined };
  return { prev: NAV_ORDER[i - 1], next: NAV_ORDER[i + 1] };
}

/** The label to show out of context (search, previous/next): "Overview" says which. */
export function displayLabel(item: NavItem & { group: NavGroup }): string {
  return item.label === 'Overview' ? `${item.group.heading} overview` : item.label;
}
