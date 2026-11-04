import { Suspense, lazy, useCallback, useEffect, useLayoutEffect, useState } from 'react';
import { Kbd } from '@rookcom/monarch-react';
import { SearchIcon } from '../../ui/icons.js';

/** The palette and its index load on first use (or when the trigger is hovered or focused). */
const loadDialog = () => import('./search-dialog.js');
const SearchDialog = lazy(() => loadDialog().then((m) => ({ default: m.SearchDialog })));

/** Same test as CommandPalette's own hotkey: ⌘ where the platform has a Command key, Ctrl elsewhere. */
function hasCommandKey(): boolean {
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
  return /mac|iphone|ipad|ipod/i.test(nav.userAgentData?.platform ?? nav.platform ?? '');
}

/**
 * Site search: Monarch's own CommandPalette over every docs page, component
 * and pattern (search-index.ts). Opens with ⌘K / Ctrl K or the top-bar button.
 *
 * Only the trigger is part of the page. The palette, its data entry and the
 * index are a separate chunk, loaded the first time search is wanted, so the
 * first paint of every page does not pay for them.
 */
export function SearchPalette() {
  const [open, setOpen] = useState(false);
  // Opened at least once: keep the dialog mounted so closing animates and focus returns.
  const [mounted, setMounted] = useState(false);
  // Prerendered as "Ctrl K"; corrected before paint where the shortcut is ⌘K.
  const [command, setCommand] = useState(false);
  useLayoutEffect(() => setCommand(hasCommandKey()), []);

  const show = useCallback((next: boolean) => {
    if (next) setMounted(true);
    setOpen(next);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() !== 'k' || e.altKey || e.shiftKey) return;
      if (!(hasCommandKey() ? e.metaKey && !e.ctrlKey : e.ctrlKey && !e.metaKey)) return;
      e.preventDefault();
      setMounted(true);
      setOpen((o) => !o);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  return (
    <>
      <button
        type="button"
        className="docs-search-trigger"
        aria-keyshortcuts="Meta+K Control+K"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => show(true)}
        onPointerEnter={() => void loadDialog()}
        onFocus={() => void loadDialog()}
      >
        <SearchIcon className="docs-search-trigger__icon" />
        <span className="docs-search-trigger__label">Search</span>
        <span className="docs-search-trigger__keys" aria-hidden>
          <Kbd>{command ? '⌘K' : 'Ctrl K'}</Kbd>
        </span>
      </button>
      {mounted ? (
        <Suspense fallback={null}>
          <SearchDialog open={open} onOpenChange={show} />
        </Suspense>
      ) : null}
    </>
  );
}
