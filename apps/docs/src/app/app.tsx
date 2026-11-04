import { Suspense, useEffect } from 'react';
import { TooltipProvider } from '@rookcom/monarch-react';
import { RouterProvider, useLocation } from '../lib/router.js';
import { ROUTE_ALIASES } from '../lib/docs.js';
import { products } from '../lib/registry.js';
import { AppearanceProvider, useAppearance } from '../features/appearance/appearance-provider.js';
import { DocsShell } from '../layout/docs-shell.js';
import { GalleryPage, MarketingExample, resolveRoute } from './routes.js';

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

const isTestRoute = (path: string) =>
  path === '/__gallery' || path.startsWith('/__gallery/') || path === '/__marketing';

function Routes() {
  const { path: raw, anchor } = useLocation();
  const path = ROUTE_ALIASES[raw] ?? raw;
  const test = isTestRoute(path);
  const route = test ? null : resolveRoute(path);

  useEffect(() => {
    if (path === '/__marketing') document.title = 'Marketing example · Monarch';
    else if (route) document.title = route.title;
  }, [path, route?.title]);
  // A new page starts at the top; an anchor is handled by the page itself.
  useEffect(() => {
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

export function App() {
  return (
    <RouterProvider>
      <AppearanceProvider>
        <TooltipProvider>
          <Routes />
        </TooltipProvider>
      </AppearanceProvider>
    </RouterProvider>
  );
}
