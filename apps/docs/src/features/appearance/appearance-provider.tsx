import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { applyPrefs, readPrefs, writePrefs, type Prefs } from '../../lib/appearance.js';

interface AppearanceValue {
  prefs: Prefs;
  /** Changes one or more preferences, applies them to <html> and saves them. */
  update: (next: Partial<Prefs>) => void;
}

const AppearanceContext = createContext<AppearanceValue | null>(null);

export function AppearanceProvider({ children }: { children: ReactNode }) {
  const [prefs, setPrefs] = useState<Prefs>(() => readPrefs());

  // Same values the pre-paint script applied; this also covers storage it could not read.
  useEffect(() => applyPrefs(prefs), [prefs]);

  // `system` keeps following the OS while the page is open.
  useEffect(() => {
    if (prefs.theme !== 'system' && prefs.contrast !== 'system') return;
    const queries = ['(prefers-color-scheme: dark)', '(prefers-contrast: more)'].map((q) =>
      window.matchMedia(q),
    );
    const onChange = () => applyPrefs(prefs);
    for (const mq of queries) mq.addEventListener('change', onChange);
    return () => {
      for (const mq of queries) mq.removeEventListener('change', onChange);
    };
  }, [prefs]);

  const update = useCallback((next: Partial<Prefs>) => {
    setPrefs((current) => {
      const merged = { ...current, ...next };
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
