import { useEffect, useState } from 'react';
import { TooltipProvider, Button } from '@rookcom/monarch-react';
import { RouterProvider, href, usePath } from './lib/router.js';
import { readPrefs, writePrefs, type Prefs } from './lib/appearance.js';
import { products } from './lib/registry.js';
import { ROUTE_ALIASES, componentPages } from './lib/components.js';
import { HomePage } from './pages/home.js';
import { GuidePage } from './pages/guide.js';
import { TokensPage } from './pages/tokens.js';
import { ComponentsIndexPage, ComponentPage } from './pages/components.js';
import { ProductsPage } from './pages/products.js';
import { GovernancePage } from './pages/governance.js';
import { GalleryPage } from './pages/gallery.js';
import { MarketingExample } from './marketing/example-page.js';
import './marketing/example.css';

const NAV: { heading: string; items: { path: string; label: string }[] }[] = [
  {
    heading: 'Start',
    items: [
      { path: '/', label: 'Monarch' },
      { path: '/getting-started', label: 'Getting started' },
      { path: '/theming', label: 'Theming' },
      { path: '/migration', label: 'Migration' },
    ],
  },
  { heading: 'Foundation', items: [{ path: '/tokens', label: 'Tokens' }] },
  {
    heading: 'Patterns',
    items: [{ path: '/marketing', label: 'Marketing pages' }],
  },
  {
    heading: 'Components',
    items: [
      { path: '/components', label: 'Overview' },
      ...componentPages.map((p) => ({ path: `/components/${p.docs.id}`, label: p.docs.title })),
    ],
  },
  {
    heading: 'Authority',
    items: [
      { path: '/products', label: 'Products' },
      { path: '/governance', label: 'Governance' },
    ],
  },
];

function titleFor(path: string): string {
  for (const g of NAV)
    for (const i of g.items)
      if (i.path === path) return i.label === 'Monarch' ? 'Monarch' : `${i.label} · Monarch`;
  if (path === '/__gallery' || path.startsWith('/__gallery/')) return 'Gallery · Monarch';
  if (path === '/__marketing') return 'Marketing example · Monarch';
  return 'Not found · Monarch';
}

function Page({ path }: { path: string }) {
  if (path === '/') return <HomePage />;
  if (path === '/getting-started') return <GuidePage guide="getting-started" />;
  if (path === '/theming') return <GuidePage guide="theming" />;
  if (path === '/migration') return <GuidePage guide="migration" />;
  if (path === '/marketing') return <GuidePage guide="marketing" />;
  if (path === '/tokens') return <TokensPage />;
  if (path === '/components') return <ComponentsIndexPage />;
  if (path.startsWith('/components/'))
    return <ComponentPage id={path.slice('/components/'.length)} />;
  if (path === '/products') return <ProductsPage />;
  if (path === '/governance') return <GovernancePage />;
  return (
    <>
      <h1>Page not found</h1>
      <p className="docs-lead">
        <a href={href('/')}>Back to the start</a>
      </p>
    </>
  );
}

function Controls({ prefs, onChange }: { prefs: Prefs; onChange: (p: Prefs) => void }) {
  return (
    <div className="docs-controls" role="group" aria-label="Appearance">
      <label>
        <span className="mn-visually-hidden">Theme</span>
        <select
          value={prefs.theme}
          onChange={(e) => onChange({ ...prefs, theme: e.target.value as Prefs['theme'] })}
          data-testid="theme"
        >
          <option value="light">Light</option>
          <option value="dark">Dark</option>
          <option value="system">System</option>
        </select>
      </label>
      <label>
        <span className="mn-visually-hidden">Contrast</span>
        <select
          value={prefs.contrast}
          onChange={(e) => onChange({ ...prefs, contrast: e.target.value as Prefs['contrast'] })}
          data-testid="contrast"
        >
          <option value="standard">Standard contrast</option>
          <option value="more">More contrast</option>
          <option value="system">System contrast</option>
        </select>
      </label>
      <label>
        <span className="mn-visually-hidden">Product accent</span>
        <select
          value={prefs.product}
          onChange={(e) => onChange({ ...prefs, product: e.target.value })}
          data-testid="product"
        >
          <option value="monarch">Rook (default accent)</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}

function Shell() {
  const raw = usePath();
  const path = ROUTE_ALIASES[raw] ?? raw;
  const [prefs, setPrefs] = useState<Prefs>(() => readPrefs());
  const [navOpen, setNavOpen] = useState(false);
  useEffect(() => writePrefs(prefs), [prefs]);
  useEffect(() => {
    document.title = titleFor(path);
    setNavOpen(false);
    window.scrollTo(0, 0);
  }, [path]);

  if (path === '/__gallery') return <GalleryPage />;
  if (path.startsWith('/__gallery/'))
    return <GalleryPage only={path.slice('/__gallery/'.length)} />;
  if (path === '/__marketing') {
    const name =
      products.find((p) => p.id === prefs.product)?.name.replace(/^Rook /, '') ?? 'Product';
    return <MarketingExample productName={name} />;
  }

  return (
    <div className="docs-shell">
      <a className="docs-skip" href="#main">
        Skip to content
      </a>
      <header className="docs-header">
        <a className="docs-brand" href={href('/')}>
          <span className="docs-brand__mark" aria-hidden />
          Monarch
        </a>
        <Button
          className="docs-nav-toggle"
          size="sm"
          aria-expanded={navOpen}
          aria-controls="docs-nav"
          onClick={() => setNavOpen((o) => !o)}
        >
          Menu
        </Button>
        <Controls prefs={prefs} onChange={setPrefs} />
      </header>
      <nav
        id="docs-nav"
        className="docs-nav"
        aria-label="Documentation"
        hidden={!navOpen && window.innerWidth < 768 ? true : undefined}
      >
        {NAV.map((g) => (
          <div key={g.heading}>
            <h2>{g.heading}</h2>
            <ul>
              {g.items.map((i) => (
                <li key={i.path}>
                  <a href={href(i.path)} aria-current={path === i.path ? 'page' : undefined}>
                    {i.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
      <main id="main" className="docs-main" tabIndex={-1}>
        <Page path={path} />
      </main>
    </div>
  );
}

export function App() {
  return (
    <RouterProvider>
      <TooltipProvider>
        <Shell />
      </TooltipProvider>
    </RouterProvider>
  );
}
