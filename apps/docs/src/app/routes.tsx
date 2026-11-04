/**
 * Route table. Every page is its own chunk (React.lazy), so a visitor loads the
 * shell, the docs index and the page they asked for. Hidden test routes
 * (`/__gallery`, `/__gallery/<id>`, `/__marketing`) render without the shell.
 */
import { lazy, type ComponentType, type ReactElement } from 'react';
import { pageById } from '../lib/docs.js';
import { navItem } from './navigation.js';

const named = <K extends string>(load: () => Promise<Record<K, ComponentType<never>>>, name: K) =>
  lazy(() => load().then((m) => ({ default: m[name] as ComponentType<Record<string, unknown>> })));

const HomePage = named(() => import('../pages/home/home-page.js'), 'HomePage');
const IntroductionPage = named(
  () => import('../pages/foundations/introduction-page.js'),
  'IntroductionPage',
);
const ColoursPage = named(() => import('../pages/foundations/colours-page.js'), 'ColoursPage');
const TypographyPage = named(
  () => import('../pages/foundations/typography-page.js'),
  'TypographyPage',
);
const TokensPage = named(() => import('../pages/foundations/tokens-page.js'), 'TokensPage');
const GuidePage = named(() => import('../pages/guides/guide-page.js'), 'GuidePage');
const ProductsPage = named(() => import('../pages/guides/products-page.js'), 'ProductsPage');
const GovernancePage = named(() => import('../pages/guides/governance-page.js'), 'GovernancePage');
const OverviewPage = named(() => import('../pages/components/overview-page.js'), 'OverviewPage');
const ComponentPage = named(() => import('../pages/components/component-page.js'), 'ComponentPage');
const NotFoundPage = named(() => import('../pages/not-found-page.js'), 'NotFoundPage');
export const GalleryPage = named(() => import('../pages/gallery.js'), 'GalleryPage');
export const MarketingExample = named(
  () => import('../marketing/example-page.js'),
  'MarketingExample',
);

export interface ResolvedRoute {
  title: string;
  /** Full-width layout without the sidebar (the home page). */
  fullWidth: boolean;
  element: ReactElement;
}

const guide = (guideId: string, eyebrow: string) => <GuidePage guide={guideId} eyebrow={eyebrow} />;

const STATIC: Record<string, () => ReactElement> = {
  '/': () => <HomePage />,
  '/introduction': () => <IntroductionPage />,
  '/colours': () => <ColoursPage />,
  '/typography': () => <TypographyPage />,
  '/tokens': () => <TokensPage />,
  '/theming': () => guide('theming', 'Foundations'),
  '/components': () => <OverviewPage layer="all" />,
  '/patterns': () => <OverviewPage layer="patterns" />,
  '/patterns/guide': () => guide('patterns', 'Patterns'),
  '/marketing': () => guide('marketing', 'Marketing'),
  '/getting-started': () => guide('getting-started', 'Guides'),
  '/migration': () => guide('migration', 'Guides'),
  '/conformance': () => guide('conformance', 'Guides'),
  '/products': () => <ProductsPage />,
  '/governance': () => <GovernancePage />,
};

export function resolveRoute(path: string): ResolvedRoute {
  if (path === '/')
    return {
      title: 'Monarch · Open-source design system',
      fullWidth: true,
      element: <HomePage />,
    };
  const render = STATIC[path];
  if (render) {
    const item = navItem(path);
    const label = item ? (item.label === 'Overview' ? item.group.heading : item.label) : 'Monarch';
    return { title: `${label} · Monarch`, fullWidth: false, element: render() };
  }
  if (path.startsWith('/components/')) {
    const id = path.slice('/components/'.length);
    const page = pageById[id];
    return {
      title: page ? `${page.title} · Monarch` : 'Unknown component · Monarch',
      fullWidth: false,
      element: <ComponentPage key={id} id={id} />,
    };
  }
  return { title: 'Not found · Monarch', fullWidth: false, element: <NotFoundPage /> };
}
