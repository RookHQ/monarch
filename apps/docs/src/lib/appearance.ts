import {
  applyAppearance,
  resolveAppearance,
  type ContrastPreference,
  type ThemePreference,
} from '@rookcom/monarch-tokens/appearance';

/**
 * Appearance preferences, stored the way Rook apps store them: the shared
 * `rook_theme` cookie, the `mn_contrast` cookie, and (docs only) the previewed
 * product accent in localStorage. The pre-paint scripts in index.html
 * (vite.config.ts) apply the same values before first paint, so there is no
 * flash; the defaults here must match theirs (`system` for both).
 */
export const THEME_COOKIE = 'rook_theme';
export const CONTRAST_COOKIE = 'mn_contrast';
export const PRODUCT_KEY = 'mn_docs_product';
export const DEFAULT_PRODUCT = 'monarch';

function cookie(name: string): string | null {
  const m = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return m?.[1] ? decodeURIComponent(m[1]) : null;
}

function setCookie(name: string, value: string): void {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=31536000; samesite=lax`;
}

export interface Prefs {
  theme: ThemePreference;
  contrast: ContrastPreference;
  product: string;
}

/** What a page shows before the stored preferences are read (and what the server renders). */
export const DEFAULT_PREFS: Prefs = {
  theme: 'system',
  contrast: 'system',
  product: DEFAULT_PRODUCT,
};

export function readPrefs(): Prefs {
  const theme = cookie(THEME_COOKIE);
  const contrast = cookie(CONTRAST_COOKIE);
  let product = DEFAULT_PRODUCT;
  try {
    product = localStorage.getItem(PRODUCT_KEY) ?? DEFAULT_PRODUCT;
  } catch {
    // private mode or blocked storage: keep the default
  }
  return {
    theme: theme === 'dark' || theme === 'light' ? theme : 'system',
    contrast: contrast === 'more' || contrast === 'standard' ? contrast : 'system',
    product,
  };
}

/** Applies preferences to <html>, exactly as the pre-paint script does. */
export function applyPrefs(p: Prefs): void {
  const env = {
    prefersDark: window.matchMedia('(prefers-color-scheme: dark)').matches,
    prefersMoreContrast: window.matchMedia('(prefers-contrast: more)').matches,
  };
  const el = document.documentElement;
  const resolved = resolveAppearance(p.theme, p.contrast, env);
  applyAppearance(el, resolved);
  el.setAttribute('data-mn-product', p.product);
}

/** Persists preferences (only called when the visitor changes one). */
export function writePrefs(p: Prefs): void {
  setCookie(THEME_COOKIE, p.theme);
  setCookie(CONTRAST_COOKIE, p.contrast);
  try {
    localStorage.setItem(PRODUCT_KEY, p.product);
  } catch {
    // ignore
  }
}

/** Inline script for <head>: applies the stored product accent before first paint. */
export const productScript = `(function(){try{var p=localStorage.getItem(${JSON.stringify(PRODUCT_KEY)});document.documentElement.setAttribute('data-mn-product',/^[a-z][a-z0-9-]{1,31}$/.test(p||'')?p:${JSON.stringify(DEFAULT_PRODUCT)})}catch(_){document.documentElement.setAttribute('data-mn-product',${JSON.stringify(DEFAULT_PRODUCT)})}})();`;
