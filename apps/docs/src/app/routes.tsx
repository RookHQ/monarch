/**
 * Route table. Every page is its own chunk (React.lazy), so a visitor loads the
 * shell, the docs index and the page they asked for. Hidden test routes
 * (`/__gallery`, `/__gallery/<id>`, `/__marketing`) render without the shell.
 *
 * Page modules are found with `import.meta.glob`, so the same key names a page
 * here, in the build manifest (`src/pages/…`) and in the prerender step
 * (scripts/prerender.mjs), which uses `routeModules()` to preload each page's
 * JavaScript and CSS.
 */
import { lazy, type ComponentType, type ReactElement } from 'react';
import { ROUTE_ALIASES, docsPages, hasDocs, loadDetails, loadDocs, pageById } from '../lib/docs.js';
import { navItem } from './navigation.js';

type PageModule = Record<string, ComponentType<never>>;
const modules = import.meta.glob<PageModule>([
  '../pages/**/*-page.tsx',
  '../pages/gallery.tsx',
  '../marketing/example-page.tsx',
]);

const loaded = new Map<string, Promise<PageModule>>();
/** Loads a page module once; preloading and rendering share the promise. */
function load(file: string): Promise<PageModule> {
  let p = loaded.get(file);
  if (!p) {
    const importer = modules[file];
    if (!importer) throw new Error(`routes: no page module ${file}`);
    p = importer();
    loaded.set(file, p);
  }
  return p;
}

const named = (file: string, name: string) =>
  Object.assign(
    lazy(() =>
      load(file).then((m) => ({ default: m[name] as ComponentType<Record<string, unknown>> })),
    ),
    { file },
  );

const HOME = '../pages/home/home-page.tsx';
const INTRODUCTION = '../pages/foundations/introduction-page.tsx';
const COLOURS = '../pages/foundations/colours-page.tsx';
const TYPOGRAPHY = '../pages/foundations/typography-page.tsx';
const TOKENS = '../pages/foundations/tokens-page.tsx';
const GUIDE = '../pages/guides/guide-page.tsx';
const PRODUCTS = '../pages/guides/products-page.tsx';
const GOVERNANCE = '../pages/guides/governance-page.tsx';
const OVERVIEW = '../pages/components/overview-page.tsx';
const COMPONENT = '../pages/components/component-page.tsx';
const NOT_FOUND = '../pages/not-found-page.tsx';

/** Pages that read the lazily loaded page details (summaries, exports, registry). */
const USES_DETAILS = new Set([OVERVIEW, COMPONENT]);

/** True when a route renders the lazily loaded page details (`loadDetails()`). */
export const usesDetails = (route: ResolvedRoute): boolean =>
  route.files.some((f) => USES_DETAILS.has(f));

const HomePage = named(HOME, 'HomePage');
const IntroductionPage = named(INTRODUCTION, 'IntroductionPage');
const ColoursPage = named(COLOURS, 'ColoursPage');
const TypographyPage = named(TYPOGRAPHY, 'TypographyPage');
const TokensPage = named(TOKENS, 'TokensPage');
const GuidePage = named(GUIDE, 'GuidePage');
const ProductsPage = named(PRODUCTS, 'ProductsPage');
const GovernancePage = named(GOVERNANCE, 'GovernancePage');
const OverviewPage = named(OVERVIEW, 'OverviewPage');
const ComponentPage = named(COMPONENT, 'ComponentPage');
const NotFoundPage = named(NOT_FOUND, 'NotFoundPage');
export const GalleryPage = named('../pages/gallery.tsx', 'GalleryPage');
export const MarketingExample = named('../marketing/example-page.tsx', 'MarketingExample');

export interface ResolvedRoute {
  title: string;
  /** Full-width layout without the sidebar (the home page). */
  fullWidth: boolean;
  element: ReactElement;
  /** Page modules the route renders (keys of `import.meta.glob` above). */
  files: readonly string[];
  /** Docs file the route renders, if any. */
  docsId?: string;
  /** The not-found page. */
  notFound?: boolean;
}

const guide = (guideId: string, eyebrow: string) => <GuidePage guide={guideId} eyebrow={eyebrow} />;

const STATIC: Record<string, { file: string; render: () => ReactElement }> = {
  '/': { file: HOME, render: () => <HomePage /> },
  '/introduction': { file: INTRODUCTION, render: () => <IntroductionPage /> },
  '/colours': { file: COLOURS, render: () => <ColoursPage /> },
  '/typography': { file: TYPOGRAPHY, render: () => <TypographyPage /> },
  '/tokens': { file: TOKENS, render: () => <TokensPage /> },
  '/theming': { file: GUIDE, render: () => guide('theming', 'Foundations') },
  '/components': { file: OVERVIEW, render: () => <OverviewPage layer="all" /> },
  '/patterns': { file: OVERVIEW, render: () => <OverviewPage layer="patterns" /> },
  '/patterns/guide': { file: GUIDE, render: () => guide('patterns', 'Patterns') },
  '/marketing': { file: GUIDE, render: () => guide('marketing', 'Marketing') },
  '/getting-started': { file: GUIDE, render: () => guide('getting-started', 'Guides') },
  '/migration': { file: GUIDE, render: () => guide('migration', 'Guides') },
  '/conformance': { file: GUIDE, render: () => guide('conformance', 'Guides') },
  '/products': { file: PRODUCTS, render: () => <ProductsPage /> },
  '/governance': { file: GOVERNANCE, render: () => <GovernancePage /> },
};

/** Every page of the site, for the prerender step and the sitemap. */
export const ROUTES: readonly string[] = [
  ...Object.keys(STATIC),
  ...docsPages.map((p) => `/components/${p.id}`),
];

export function resolveRoute(raw: string): ResolvedRoute {
  const path = ROUTE_ALIASES[raw] ?? raw;
  if (path === '/')
    return {
      title: 'Monarch · Open-source design system',
      fullWidth: true,
      element: <HomePage />,
      files: [HOME],
    };
  const page = STATIC[path];
  if (page) {
    const item = navItem(path);
    const label = item ? (item.label === 'Overview' ? item.group.heading : item.label) : 'Monarch';
    return {
      title: `${label} · Monarch`,
      fullWidth: false,
      element: page.render(),
      files: [page.file],
    };
  }
  if (path.startsWith('/components/')) {
    const id = path.slice('/components/'.length);
    const meta = pageById[id];
    return {
      title: meta ? `${meta.title} · Monarch` : 'Unknown component · Monarch',
      fullWidth: false,
      element: <ComponentPage key={id} id={id} />,
      files: [COMPONENT],
      ...(meta && hasDocs(id) ? { docsId: id } : {}),
    };
  }
  return {
    title: 'Not found · Monarch',
    fullWidth: false,
    element: <NotFoundPage />,
    files: [NOT_FOUND],
    notFound: true,
  };
}

/**
 * Starts loading everything a route renders: its page chunk, the page details
 * if it shows summaries or exports and, for a component page, its docs file.
 * Called on hover or focus of a link and before hydration, so the page renders
 * in one pass.
 */
export function preloadRoute(path: string): Promise<unknown> {
  const route = resolveRoute(path);
  return Promise.all([
    ...route.files.map(load),
    ...(usesDetails(route) ? [loadDetails()] : []),
    ...(route.docsId ? [loadDocs(route.docsId)] : []),
  ]).catch(() => undefined);
}
