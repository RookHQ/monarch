import { Suspense, lazy, useEffect, useState, type ReactNode } from 'react';
import { groupOf, type NavGroup } from '../app/navigation.js';
import { TopBar, loadDrawer } from './top-bar.js';
import { Sidebar } from './sidebar.js';
import { SiteFooter } from './site-footer.js';

const NavDrawer = lazy(() => loadDrawer().then((m) => ({ default: m.NavDrawer })));

/**
 * The documentation chrome: skip link, sticky top bar, sidebar (wide screens)
 * or navigation drawer (narrow screens), the page, and the footer. The home
 * page is full width and has no sidebar.
 */
export function DocsShell({
  path,
  fullWidth,
  children,
}: {
  path: string;
  fullWidth: boolean;
  children: ReactNode;
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  // Opened at least once: stays mounted so closing animates and focus returns.
  const [drawer, setDrawer] = useState(false);
  useEffect(() => setMenuOpen(false), [path]);
  const group: NavGroup | undefined = groupOf(path);

  return (
    <div className="docs-app" data-layout={fullWidth ? 'full' : 'docs'}>
      <a
        className="docs-skip"
        href="#main"
        onClick={(e) => {
          // Move focus as well as scrolling, and keep the address clean.
          e.preventDefault();
          const main = document.getElementById('main');
          main?.focus();
          main?.scrollIntoView();
        }}
      >
        Skip to content
      </a>
      <TopBar
        currentGroup={group}
        menuOpen={menuOpen}
        onOpenMenu={() => {
          setDrawer(true);
          setMenuOpen(true);
        }}
      />
      <div className="docs-layout">
        {fullWidth ? null : (
          <div className="docs-layout__sidebar" data-scroll-box>
            <Sidebar path={path} />
          </div>
        )}
        <main id="main" className="docs-layout__main" tabIndex={-1}>
          <Suspense fallback={<div className="docs-loading" aria-hidden />}>{children}</Suspense>
        </main>
      </div>
      <SiteFooter />
      {drawer ? (
        <Suspense fallback={null}>
          <NavDrawer path={path} open={menuOpen} onOpenChange={setMenuOpen} />
        </Suspense>
      ) : null}
    </div>
  );
}
