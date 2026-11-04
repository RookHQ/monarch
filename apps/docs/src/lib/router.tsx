import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

/**
 * Hash router: works on GitHub Pages without server config and keeps the
 * site dependency-free. Routes look like `#/components/button`.
 */
const RouteContext = createContext<string>('/');

function read(): string {
  const h = window.location.hash.replace(/^#/, '');
  return h === '' ? '/' : h;
}

export function RouterProvider({ children }: { children: ReactNode }) {
  const [path, setPath] = useState<string>(() => (typeof window === 'undefined' ? '/' : read()));
  useEffect(() => {
    const onChange = () => setPath(read());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return <RouteContext.Provider value={path}>{children}</RouteContext.Provider>;
}

export function usePath(): string {
  return useContext(RouteContext);
}

export function href(path: string): string {
  return `#${path}`;
}

export function navigate(path: string): void {
  window.location.hash = path;
}
