/**
 * Resolving the viewer's appearance into the attributes Monarch CSS reads.
 *
 * Themes switch on attributes only (see css.ts). This module turns a stored
 * preference plus the OS settings into those attributes. `appearanceScript`
 * returns an inline script for <head> that runs before first paint, so there is
 * no flash of the wrong theme.
 */

export type ThemePreference = 'light' | 'dark' | 'system';
export type ContrastPreference = 'standard' | 'more' | 'system';

export interface Appearance {
  readonly theme: 'light' | 'dark';
  readonly contrast: 'standard' | 'more';
}

export interface Environment {
  readonly prefersDark: boolean;
  readonly prefersMoreContrast: boolean;
}

export function resolveAppearance(
  theme: ThemePreference,
  contrast: ContrastPreference,
  env: Environment,
): Appearance {
  return {
    theme: theme === 'system' ? (env.prefersDark ? 'dark' : 'light') : theme,
    contrast: contrast === 'system' ? (env.prefersMoreContrast ? 'more' : 'standard') : contrast,
  };
}

/** Applies an appearance to an element (normally <html>). */
export function applyAppearance(el: HTMLElement, a: Appearance): void {
  el.setAttribute('data-mn-theme', a.theme);
  el.setAttribute('data-mn-contrast', a.contrast);
  // `.dark` is kept because every Rook app's Tailwind `dark:` variant uses it.
  el.classList.toggle('dark', a.theme === 'dark');
}

export interface AppearanceScriptOptions {
  /** Product ID from the Monarch registry, set as data-mn-product. */
  readonly product?: string;
  /** Cookie holding light|dark|system. Rook apps use `rook_theme`. */
  readonly themeCookie?: string;
  /** Cookie holding standard|more|system. */
  readonly contrastCookie?: string;
  readonly defaultTheme?: ThemePreference;
}

const ID = /^[a-z][a-z0-9-]{1,31}$/;
const COOKIE = /^[A-Za-z0-9_-]{1,64}$/;

/**
 * Inline script source for <head>. Options are validated and JSON-encoded, so
 * the output is safe to inject with dangerouslySetInnerHTML.
 *
 * Honours the Rook convention `<html data-rook-theme-routes="regex">`: dark
 * applies only on matching (signed-in) routes; public pages stay light. It can replace
 * an app's own theme boot script.
 */
export function appearanceScript(options: AppearanceScriptOptions = {}): string {
  const {
    product,
    themeCookie = 'rook_theme',
    contrastCookie = 'mn_contrast',
    defaultTheme = 'light',
  } = options;
  if (product !== undefined && !ID.test(product)) throw new Error(`Invalid product id: ${product}`);
  if (!COOKIE.test(themeCookie) || !COOKIE.test(contrastCookie))
    throw new Error('Invalid cookie name');
  const cfg = JSON.stringify({
    p: product ?? null,
    t: themeCookie,
    c: contrastCookie,
    d: defaultTheme,
  });
  return `(function(){try{var o=${cfg};var e=document.documentElement;function k(n){var m=document.cookie.match(new RegExp('(?:^|; )'+n+'=([^;]*)'));return m?decodeURIComponent(m[1]):null}var t=k(o.t)||o.d,c=k(o.c)||'system',q=function(s){return window.matchMedia&&window.matchMedia(s).matches};if(t==='system')t=q('(prefers-color-scheme: dark)')?'dark':'light';if(t!=='dark')t='light';var r=e.getAttribute('data-rook-theme-routes');if(r&&!new RegExp(r).test(location.pathname))t='light';if(c==='system')c=q('(prefers-contrast: more)')?'more':'standard';if(c!=='more')c='standard';e.setAttribute('data-mn-theme',t);e.setAttribute('data-mn-contrast',c);e.classList.toggle('dark',t==='dark');if(o.p)e.setAttribute('data-mn-product',o.p)}catch(_){}})();`;
}
