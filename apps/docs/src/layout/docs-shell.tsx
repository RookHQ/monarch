import { Suspense, useEffect, useState, type ReactNode } from 'react';
import { Sheet, SheetBody, SheetContent, SheetHeader, SheetTitle } from '@rookcom/monarch-react';
import { groupOf, type NavGroup } from '../app/navigation.js';
import { TopBar } from './top-bar.js';
import { Sidebar } from './sidebar.js';
import { SiteFooter } from './site-footer.js';

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
  useEffect(() => setMenuOpen(false), [path]);
  const group: NavGroup | undefined = groupOf(path);

  return (
    <div className="docs-app" data-layout={fullWidth ? 'full' : 'docs'}>
      <a
        className="docs-skip"
        href="#main"
        onClick={(e) => {
          // The hash is the route, so move focus instead of navigating.
          e.preventDefault();
          const main = document.getElementById('main');
          main?.focus();
          main?.scrollIntoView();
        }}
      >
        Skip to content
      </a>
      <TopBar currentGroup={group} menuOpen={menuOpen} onOpenMenu={() => setMenuOpen(true)} />
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
      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" size="sm" className="docs-drawer">
          <SheetHeader>
            <SheetTitle>Monarch</SheetTitle>
          </SheetHeader>
          <SheetBody data-scroll-box>
            <Sidebar path={path} collapsed />
          </SheetBody>
        </SheetContent>
      </Sheet>
    </div>
  );
}
