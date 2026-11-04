import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

/**
 * Path router. Every page has a real address under the site's base path
 * (`/components/button/`, or `/monarch/components/button/` when the site is
 * served from a sub-path), and every page is prerendered to its own
 * `index.html` at build time (scripts/prerender.mjs). In-page anchors are
 * ordinary fragments: `/components/button/#keyboard`.
 *
 * Two kinds of address still live in the fragment:
 * - the screenshot routes, `#/__gallery`, `#/__gallery/<id>` and
 *   `#/__marketing`, which the visual baselines open exactly as before;
 * - links from the old hash router (`#/components/button#keyboard`), which
 *   `legacyRedirect()` turns into the page's real address before the app starts.
 */
export interface Location {
  /** `/components/button`: no base path, no trailing slash. */
  path: string;
  /** Element id to scroll to, from the fragment. */
  anchor: string | null;
}

/** The site's base path, always with a trailing slash: `/` or `/monarch/`. */
export const BASE: string = import.meta.env.BASE_URL.endsWith('/')
  ? import.meta.env.BASE_URL
  : `${import.meta.env.BASE_URL}/`;

const TEST_ROUTE = /^\/__(gallery(\/|$)|marketing$)/;

/** The hidden, chrome-less routes the visual baselines use. */
export const isTestRoute = (path: string): boolean => TEST_ROUTE.test(path);

const RouteContext = createContext<Location>({ path: '/', anchor: null });

/** `/components/button/` → `/components/button`; a path outside the base → `null`. */
export function pathFromPathname(pathname: string): string | null {
  const base = BASE.slice(0, -1);
  if (pathname !== base && !pathname.startsWith(BASE)) return null;
  const rest = pathname.slice(base.length).replace(/\/index\.html$/, '/');
  const trimmed = rest.replace(/\/+$/, '');
  return trimmed === '' ? '/' : trimmed;
}

/** `#/path#anchor` (old router and screenshot routes) → its parts. */
function parseHashRoute(hash: string): Location {
  const raw = hash.replace(/^#/, '');
  const at = raw.indexOf('#');
  const path = (at === -1 ? raw : raw.slice(0, at)).replace(/\/+$/, '') || '/';
  const anchor = at === -1 ? null : decodeURIComponent(raw.slice(at + 1)) || null;
  return { path, anchor };
}

/** The location of the current document. */
export function readLocation(): Location {
  const { hash, pathname } = window.location;
  if (hash.startsWith('#/')) return parseHashRoute(hash);
  return {
    path: pathFromPathname(pathname) ?? '/',
    anchor: hash.length > 1 ? decodeURIComponent(hash.slice(1)) : null,
  };
}

/** The address of a page (and optionally a section of it). */
export function href(path: string, anchor?: string): string {
  if (isTestRoute(path)) return `${BASE}#${path}`;
  const url = path === '/' ? BASE : `${BASE}${path.replace(/^\/+|\/+$/g, '')}/`;
  return anchor ? `${url}#${encodeURIComponent(anchor)}` : url;
}

/**
 * Rewrites an old address to the page's real one, without a reload: a
 * hash-router link (`#/components/button#keyboard`) or a renamed page
 * (`aliases`). Screenshot routes are left alone. Returns the location to render.
 */
export function legacyRedirect(aliases: Readonly<Record<string, string>> = {}): Location {
  const location = readLocation();
  if (isTestRoute(location.path)) return location;
  const hashRoute = window.location.hash.startsWith('#/');
  const path = aliases[location.path] ?? location.path;
  if (!hashRoute && path === location.path) return location;
  window.history.replaceState(null, '', href(path, location.anchor ?? undefined));
  return { path, anchor: location.anchor };
}

const NAVIGATE = 'monarch-docs:navigate';

/** Client-side navigation to a page; the browser's back button returns. */
export function navigate(path: string, anchor?: string): void {
  window.history.pushState(null, '', href(path, anchor));
  window.dispatchEvent(new Event(NAVIGATE));
}

/** Files and other non-page addresses under the base path open normally. */
const FILE = /\.[a-z0-9]+$/i;

/**
 * The same-site page a click on `a` should open in place, or `null` when the
 * browser should handle it (new tab, download, another site, a file, or an
 * anchor on the current page).
 */
function internalTarget(event: MouseEvent): URL | null {
  if (event.defaultPrevented || event.button !== 0) return null;
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return null;
  const a = (event.target as Element | null)?.closest?.('a[href]');
  if (!(a instanceof HTMLAnchorElement)) return null;
  if ((a.target && a.target !== '_self') || a.hasAttribute('download')) return null;
  const url = new URL(a.href, window.location.href);
  if (url.origin !== window.location.origin) return null;
  if (pathFromPathname(url.pathname) === null || FILE.test(url.pathname)) return null;
  // Same document, different fragment: the browser scrolls (and fires hashchange).
  if (url.pathname === window.location.pathname && url.search === window.location.search)
    return null;
  return url;
}

export function RouterProvider({
  children,
  initial,
}: {
  children: ReactNode;
  /** The location to start from: the prerendered page's, or the redirected one. */
  initial?: Location;
}) {
  const [location, setLocation] = useState<Location>(() => initial ?? readLocation());
  useEffect(() => {
    const onChange = () => setLocation(readLocation());
    const onClick = (event: MouseEvent) => {
      const url = internalTarget(event);
      if (!url) return;
      event.preventDefault();
      window.history.pushState(null, '', url.pathname + url.search + url.hash);
      onChange();
    };
    window.addEventListener('popstate', onChange);
    window.addEventListener('hashchange', onChange);
    window.addEventListener(NAVIGATE, onChange);
    document.addEventListener('click', onClick);
    return () => {
      window.removeEventListener('popstate', onChange);
      window.removeEventListener('hashchange', onChange);
      window.removeEventListener(NAVIGATE, onChange);
      document.removeEventListener('click', onClick);
    };
  }, []);
  return <RouteContext.Provider value={location}>{children}</RouteContext.Provider>;
}

export function useLocation(): Location {
  return useContext(RouteContext);
}

export function usePath(): string {
  return useContext(RouteContext).path;
}
