import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  DEFAULT_PREFS,
  applyPrefs,
  readPrefs,
  writePrefs,
  type Prefs,
} from '../../lib/appearance.js';

interface AppearanceValue {
  prefs: Prefs;
  /** Changes one or more preferences, applies them to <html> and saves them. */
  update: (next: Partial<Prefs>) => void;
}

const AppearanceContext = createContext<AppearanceValue | null>(null);

export function AppearanceProvider({ children }: { children: ReactNode }) {
  // `null` until the stored preferences are read. Pages are prerendered, and the
  // first client render must match that HTML, so it uses the defaults; the
  // stored values replace them before the browser paints (layout effect). The
  // pre-paint script has already applied them to <html>.
  const [stored, setStored] = useState<Prefs | null>(null);
  useLayoutEffect(() => setStored(readPrefs()), []);
  const prefs = stored ?? DEFAULT_PREFS;

  // Same values the pre-paint script applied; this also covers storage it could not read.
  useEffect(() => {
    if (stored) applyPrefs(stored);
  }, [stored]);

  // `system` keeps following the OS while the page is open.
  useEffect(() => {
    if (!stored) return;
    if (prefs.theme !== 'system' && prefs.contrast !== 'system') return;
    const queries = ['(prefers-color-scheme: dark)', '(prefers-contrast: more)'].map((q) =>
      window.matchMedia(q),
    );
    const onChange = () => applyPrefs(prefs);
    for (const mq of queries) mq.addEventListener('change', onChange);
    return () => {
      for (const mq of queries) mq.removeEventListener('change', onChange);
    };
  }, [stored, prefs]);

  const update = useCallback((next: Partial<Prefs>) => {
    setStored((current) => {
      const merged = { ...(current ?? readPrefs()), ...next };
      writePrefs(merged);
      return merged;
    });
  }, []);

  const value = useMemo(() => ({ prefs, update }), [prefs, update]);
  return <AppearanceContext.Provider value={value}>{children}</AppearanceContext.Provider>;
}

export function useAppearance(): AppearanceValue {
  const v = useContext(AppearanceContext);
  if (!v) throw new Error('useAppearance needs <AppearanceProvider>');
  return v;
}
