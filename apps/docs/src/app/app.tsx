import { Suspense, useEffect, useRef } from 'react';
import { TooltipProvider } from '@rookcom/monarch-react';
import {
  RouterProvider,
  isTestRoute,
  pathFromPathname,
  useLocation,
  type Location,
} from '../lib/router.js';
import { ROUTE_ALIASES } from '../lib/docs.js';
import { products } from '../lib/registry.js';
import { AppearanceProvider, useAppearance } from '../features/appearance/appearance-provider.js';
import { DocsShell } from '../layout/docs-shell.js';
import { GalleryPage, MarketingExample, preloadRoute, resolveRoute } from './routes.js';

/** Hidden routes for the visual baselines: no chrome, exactly as before the redesign. */
function TestRoute({ path }: { path: string }) {
  const { prefs } = useAppearance();
  if (path === '/__gallery') return <GalleryPage />;
  if (path.startsWith('/__gallery/'))
    return <GalleryPage only={path.slice('/__gallery/'.length)} />;
  const name =
    products.find((p) => p.id === prefs.product)?.name.replace(/^Rook /, '') ?? 'Product';
  return <MarketingExample productName={name} />;
}

/**
 * Starts loading a page as soon as a link to it is hovered or focused, so the
 * click usually finds the chunk (and its docs file) already there.
 */
function usePrefetchOnIntent() {
  useEffect(() => {
    const onIntent = (event: Event) => {
      const a = (event.target as Element | null)?.closest?.('a[href]');
      if (!(a instanceof HTMLAnchorElement) || a.origin !== window.location.origin) return;
      const path = pathFromPathname(a.pathname);
      if (path !== null) void preloadRoute(path);
    };
    document.addEventListener('pointerover', onIntent, { passive: true });
    document.addEventListener('focusin', onIntent);
    return () => {
      document.removeEventListener('pointerover', onIntent);
      document.removeEventListener('focusin', onIntent);
    };
  }, []);
}

function Routes() {
  const { path: raw, anchor } = useLocation();
  const path = ROUTE_ALIASES[raw] ?? raw;
  const test = isTestRoute(path);
  const route = test ? null : resolveRoute(path);
  usePrefetchOnIntent();

  useEffect(() => {
    if (path === '/__marketing') document.title = 'Marketing example · Monarch';
    else if (route) document.title = route.title;
  }, [path, route?.title]);
  // A new page starts at the top; an anchor is handled by the page itself. The
  // first page is left where the browser put it (a reload keeps its position).
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    if (!anchor) window.scrollTo(0, 0);
  }, [path]);

  if (test || !route)
    return (
      <Suspense fallback={null}>
        <TestRoute path={path} />
      </Suspense>
    );
  return (
    <DocsShell path={path} fullWidth={route.fullWidth}>
      {route.element}
    </DocsShell>
  );
}

export function App({ initial }: { initial?: Location }) {
  return (
    <RouterProvider {...(initial ? { initial } : {})}>
      <AppearanceProvider>
        <TooltipProvider>
          <Routes />
        </TooltipProvider>
      </AppearanceProvider>
    </RouterProvider>
  );
}
