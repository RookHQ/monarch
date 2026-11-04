import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

/**
 * Hash router: works on GitHub Pages without server config and keeps the
 * site dependency-free. Routes look like `#/components/button`; an in-page
 * anchor is a second hash, `#/components/button#keyboard`, so table-of-contents
 * links never leave the route.
 */
export interface Location {
  path: string;
  /** Element id to scroll to, from `#/path#anchor`. */
  anchor: string | null;
}

const RouteContext = createContext<Location>({ path: '/', anchor: null });

function read(): Location {
  const raw = window.location.hash.replace(/^#/, '');
  const at = raw.indexOf('#');
  const path = (at === -1 ? raw : raw.slice(0, at)) || '/';
  const anchor = at === -1 ? null : decodeURIComponent(raw.slice(at + 1)) || null;
  return { path, anchor };
}

export function RouterProvider({ children }: { children: ReactNode }) {
  const [location, setLocation] = useState<Location>(() =>
    typeof window === 'undefined' ? { path: '/', anchor: null } : read(),
  );
  useEffect(() => {
    const onChange = () => setLocation(read());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return <RouteContext.Provider value={location}>{children}</RouteContext.Provider>;
}

export function useLocation(): Location {
  return useContext(RouteContext);
}

export function usePath(): string {
  return useContext(RouteContext).path;
}

export function href(path: string, anchor?: string): string {
  return anchor ? `#${path}#${encodeURIComponent(anchor)}` : `#${path}`;
}

export function navigate(path: string): void {
  window.location.hash = path;
}
