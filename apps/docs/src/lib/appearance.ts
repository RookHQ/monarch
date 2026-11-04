import {
  applyAppearance,
  resolveAppearance,
  type ContrastPreference,
  type ThemePreference,
} from '@rookcom/monarch-tokens/appearance';

const THEME_COOKIE = 'rook_theme';
const CONTRAST_COOKIE = 'mn_contrast';
const PRODUCT_KEY = 'mn_docs_product';

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

export function readPrefs(): Prefs {
  const theme = cookie(THEME_COOKIE);
  const contrast = cookie(CONTRAST_COOKIE);
  let product = 'monarch';
  try {
    product = localStorage.getItem(PRODUCT_KEY) ?? 'monarch';
  } catch {
    // private mode or blocked storage: keep the default
  }
  return {
    theme: theme === 'dark' || theme === 'system' ? theme : 'light',
    contrast: contrast === 'more' || contrast === 'system' ? contrast : 'standard',
    product,
  };
}

/** Applies preferences to <html> and persists them the way Rook apps do. */
export function writePrefs(p: Prefs): void {
  setCookie(THEME_COOKIE, p.theme);
  setCookie(CONTRAST_COOKIE, p.contrast);
  try {
    localStorage.setItem(PRODUCT_KEY, p.product);
  } catch {
    // ignore
  }
  const env = {
    prefersDark: window.matchMedia('(prefers-color-scheme: dark)').matches,
    prefersMoreContrast: window.matchMedia('(prefers-contrast: more)').matches,
  };
  const el = document.documentElement;
  applyAppearance(el, resolveAppearance(p.theme, p.contrast, env));
  el.setAttribute('data-mn-product', p.product);
}
