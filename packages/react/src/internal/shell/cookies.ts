/**
 * Cookie I/O for the Rook cross-app contracts (`rook_rail`, `rook_theme`).
 * Every shell component that persists a choice takes a `ShellCookieStore`, so
 * tests, docs examples, embedded frames and server-driven apps can replace
 * `document.cookie` with their own storage.
 */

export interface ShellCookieOptions {
  /** Lifetime in seconds. */
  maxAge: number;
  /**
   * Cookie domain, e.g. `.rookhq.com` so every Rook app shares the choice.
   * Omitted: the cookie belongs to the current host only.
   */
  domain?: string | undefined;
}

/** Where the shell reads and writes its cookies. */
export interface ShellCookieStore {
  get(name: string): string | null;
  set(name: string, value: string, options: ShellCookieOptions): void;
}

/** The domain Rook apps share their shell cookies on. */
export const ROOK_COOKIE_DOMAIN = '.rookhq.com';
export const ONE_YEAR = 60 * 60 * 24 * 365;

const NAME = /^[A-Za-z0-9_-]{1,64}$/;
const DOMAIN = /^\.?[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*$/;

/**
 * `.rookhq.com` on rookhq.com and its subdomains, otherwise undefined (a
 * host-only cookie, which is what localhost and preview hosts need).
 */
export function defaultCookieDomain(hostname?: string): string | undefined {
  const host = hostname ?? (typeof window === 'undefined' ? '' : window.location.hostname);
  return host === 'rookhq.com' || host.endsWith('.rookhq.com') ? ROOK_COOKIE_DOMAIN : undefined;
}

/** `document.cookie`. Safe to call on the server (reads null, writes nothing). */
export const documentCookieStore: ShellCookieStore = {
  get(name) {
    if (typeof document === 'undefined' || !NAME.test(name)) return null;
    try {
      const m = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
      return m?.[1] ? decodeURIComponent(m[1]) : null;
    } catch {
      return null;
    }
  },
  set(name, value, { maxAge, domain }) {
    if (typeof document === 'undefined' || !NAME.test(name)) return;
    const d = domain && DOMAIN.test(domain) ? `; domain=${domain}` : '';
    const secure = window.location.protocol === 'https:' ? '; secure' : '';
    try {
      document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${maxAge}; samesite=lax${d}${secure}`;
    } catch {
      // Cookies blocked: the choice still applies to this page view.
    }
  },
};
