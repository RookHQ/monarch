import { useCallback, useEffect, useState, useSyncExternalStore } from 'react';
import {
  ONE_YEAR,
  defaultCookieDomain,
  type ShellCookieStore,
} from '../../../internal/shell/cookies.js';

/** The cookie every Rook app shares the rail state in: `expanded` or `collapsed`. */
export const ROOK_RAIL_COOKIE = 'rook_rail';
/** Fired on `window` after a rail is expanded or collapsed (detail: `expanded` | `collapsed`). */
export const ROOK_RAIL_EVENT = 'rook-rail';

const COOKIE_NAME = /^[A-Za-z0-9_-]{1,64}$/;

export interface RookRailScriptOptions {
  /** Cookie to read. Default `rook_rail`. */
  cookieName?: string;
}

/**
 * Inline `<head>` script that marks `<html data-rook-rail="expanded">` before
 * first paint when the saved rail state is expanded, so the rail (and the
 * content beside it) never flashes the wrong width. Self-contained and safe to
 * inject with `dangerouslySetInnerHTML`.
 */
export function rookRailScript(options: RookRailScriptOptions = {}): string {
  const name = options.cookieName ?? ROOK_RAIL_COOKIE;
  if (!COOKIE_NAME.test(name)) throw new Error('Invalid cookie name');
  return `(function(){try{var m=document.cookie.match(/(?:^|;\\s*)${name}=(expanded|collapsed)/);if(m&&m[1]==='expanded')document.documentElement.setAttribute('data-rook-rail','expanded');}catch(_){}})();`;
}

function subscribe(onChange: () => void) {
  window.addEventListener(ROOK_RAIL_EVENT, onChange);
  // Another Rook app may have changed the shared cookie while this tab was hidden.
  window.addEventListener('focus', onChange);
  document.addEventListener('visibilitychange', onChange);
  return () => {
    window.removeEventListener(ROOK_RAIL_EVENT, onChange);
    window.removeEventListener('focus', onChange);
    document.removeEventListener('visibilitychange', onChange);
  };
}

export interface RailStateOptions {
  expanded: boolean | undefined;
  defaultExpanded: boolean;
  onExpandedChange: ((expanded: boolean) => void) | undefined;
  persist: boolean;
  cookieName: string;
  cookieDomain: string | undefined;
  cookies: ShellCookieStore;
}

/**
 * Rail state: controlled by `expanded` (nothing is persisted then), or read
 * from and written to the `rook_rail` cookie. The server render (and hydration) use `defaultExpanded`;
 * the saved state applies on the first client render after hydration.
 */
export function useRailState(o: RailStateOptions): {
  expanded: boolean;
  setExpanded: (next: boolean) => void;
  ready: boolean;
} {
  const { cookies, cookieName, persist, defaultExpanded } = o;
  const [local, setLocal] = useState(defaultExpanded);
  const read = useCallback(() => {
    const v = cookies.get(cookieName);
    return v === 'expanded' ? true : v === 'collapsed' ? false : defaultExpanded;
  }, [cookies, cookieName, defaultExpanded]);
  const saved = useSyncExternalStore(
    persist ? subscribe : noopSubscribe,
    persist ? read : () => defaultExpanded,
    () => defaultExpanded,
  );
  const expanded = o.expanded ?? (persist ? saved : local);

  const [ready, setReady] = useState(false);
  useEffect(() => setReady(true), []);

  // Mirror the state on <html>, like the pre-paint script, for layouts that read it.
  useEffect(() => {
    if (!persist || o.expanded !== undefined) return;
    const root = document.documentElement;
    if (expanded) root.setAttribute('data-rook-rail', 'expanded');
    else root.removeAttribute('data-rook-rail');
  }, [expanded, persist, o.expanded]);

  const { onExpandedChange, cookieDomain } = o;
  const controlled = o.expanded !== undefined;
  const setExpanded = useCallback(
    (next: boolean) => {
      // A controlled rail belongs to the app: it is told, and nothing is written.
      if (controlled) {
        onExpandedChange?.(next);
        return;
      }
      if (persist) {
        const state = next ? 'expanded' : 'collapsed';
        cookies.set(cookieName, state, {
          maxAge: ONE_YEAR,
          domain: cookieDomain ?? defaultCookieDomain(),
        });
        window.dispatchEvent(new CustomEvent(ROOK_RAIL_EVENT, { detail: state }));
      } else setLocal(next);
      onExpandedChange?.(next);
    },
    [controlled, persist, cookies, cookieName, cookieDomain, onExpandedChange],
  );
  return { expanded, setExpanded, ready };
}

const noopSubscribe = () => () => {};
