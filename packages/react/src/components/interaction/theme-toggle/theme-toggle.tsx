'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useSyncExternalStore,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { Button } from '../../actions/button/index.js';
import { cx } from '../../../internal/cx.js';
import { applyAppearance, type ThemePreference } from '@rookcom/monarch-tokens';
import {
  ONE_YEAR,
  defaultCookieDomain,
  documentCookieStore,
  type ShellCookieStore,
} from '../../../internal/shell/cookies.js';
import { MoonGlyph, SunGlyph, SystemGlyph } from '../../../internal/shell/icons.js';

export type { ThemePreference };

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-theme-toggle';

/** The cookie every Rook app shares the theme choice in. */
export const ROOK_THEME_COOKIE = 'rook_theme';
/** Fired on `window` after a ThemeToggle writes a new choice (detail: the preference). */
export const ROOK_THEME_EVENT = 'rook-theme';

const PREFERENCES: readonly ThemePreference[] = ['light', 'dark', 'system'];
const isPreference = (v: unknown): v is ThemePreference =>
  typeof v === 'string' && (PREFERENCES as readonly string[]).includes(v);

export interface ThemeToggleMessages {
  /** Name of the control. Default "Theme". */
  label: string;
  light: string;
  dark: string;
  system: string;
}

const DEFAULT_MESSAGES: ThemeToggleMessages = {
  label: 'Theme',
  light: 'Light',
  dark: 'Dark',
  system: 'System',
};

/**
 * Applies a preference to `<html>` exactly as the tokens pre-paint script
 * (`appearanceScript` in `@rookcom/monarch-tokens`) does: `system` follows the
 * OS, routes outside `<html data-rook-theme-routes="regex">` stay light, and
 * the contrast attribute is left as it is.
 */
export function applyThemePreference(
  preference: ThemePreference,
  root: HTMLElement = document.documentElement,
): 'light' | 'dark' {
  const prefersDark =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches;
  let theme: 'light' | 'dark' =
    preference === 'system' ? (prefersDark ? 'dark' : 'light') : preference;
  const routes = root.getAttribute('data-rook-theme-routes');
  if (routes) {
    try {
      if (!new RegExp(routes).test(window.location.pathname)) theme = 'light';
    } catch {
      theme = 'light';
    }
  }
  const contrast = root.getAttribute('data-mn-contrast') === 'more' ? 'more' : 'standard';
  applyAppearance(root, { theme, contrast });
  root.style.colorScheme = theme;
  return theme;
}

export interface ThemeToggleProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children'
> {
  /** The preference (controlled). Without it the toggle reads and writes the cookie itself. */
  value?: ThemePreference | undefined;
  /** Used when there is no cookie yet, and for the server render. Default `light`, the Rook default. */
  defaultValue?: ThemePreference | undefined;
  /** Called with the new preference after it is saved and applied. */
  onValueChange?: ((value: ThemePreference) => void) | undefined;
  /**
   * `segmented`: three visible choices (settings pages). `menu`: one button that
   * opens a Light / Dark / System menu (rails, toolbars, account menus). Default `segmented`.
   */
  variant?: 'segmented' | 'menu' | undefined;
  /** `menu` only: show the current choice as text next to the icon. Default true. */
  showLabel?: boolean | undefined;
  /** `menu` only: where the menu opens. Default `bottom`; use `right` in a rail. */
  side?: 'top' | 'right' | 'bottom' | 'left' | undefined;
  /** `menu` only: alignment against the trigger. Default `start`. */
  align?: 'start' | 'center' | 'end' | undefined;
  size?: 'sm' | 'md' | undefined;
  /** Cookie name. Default `rook_theme`, shared by every Rook app. */
  cookieName?: string | undefined;
  /**
   * Cookie domain. Default: `.rookhq.com` on Rook hosts (shared across apps),
   * otherwise the current host only.
   */
  cookieDomain?: string | undefined;
  /** Where the choice is stored. Default `document.cookie`; `null` keeps it in memory only. */
  cookies?: ShellCookieStore | null | undefined;
  /** Apply the choice to `<html>` (data-mn-theme, `.dark`, color-scheme). Default true. */
  applyToDocument?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ThemeToggleMessages> | undefined;
}

function subscribe(onChange: () => void) {
  window.addEventListener(ROOK_THEME_EVENT, onChange);
  // Another app may have changed the shared cookie while this tab was in the background.
  window.addEventListener('focus', onChange);
  document.addEventListener('visibilitychange', onChange);
  return () => {
    window.removeEventListener(ROOK_THEME_EVENT, onChange);
    window.removeEventListener('focus', onChange);
    document.removeEventListener('visibilitychange', onChange);
  };
}

/** In-memory fallback when `cookies` is null: one value per page, shared by every toggle. */
let memoryPreference: ThemePreference | null = null;

const ICONS: Record<ThemePreference, ReactNode> = {
  light: <SunGlyph />,
  dark: <MoonGlyph />,
  system: <SystemGlyph />,
};

/**
 * Light, dark or system theme for Rook apps. The choice is saved in the shared
 * `rook_theme` cookie (so every Rook app follows it) and applied to `<html>`
 * the same way the tokens pre-paint script does, so render that script in
 * `<head>` and this toggle anywhere. It renders the default on the server and
 * picks up the saved choice on hydration, so there is no mismatch.
 */
export const ThemeToggle = forwardRef<HTMLDivElement, ThemeToggleProps>(function ThemeToggle(
  {
    value: valueProp,
    defaultValue = 'light',
    onValueChange,
    variant = 'segmented',
    showLabel = true,
    side = 'bottom',
    align = 'start',
    size = 'md',
    cookieName = ROOK_THEME_COOKIE,
    cookieDomain,
    cookies = documentCookieStore,
    applyToDocument = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const read = useCallback((): ThemePreference => {
    const saved = cookies ? cookies.get(cookieName) : memoryPreference;
    return isPreference(saved) ? saved : defaultValue;
  }, [cookies, cookieName, defaultValue]);
  const stored = useSyncExternalStore(subscribe, read, () => defaultValue);
  const value = valueProp ?? stored;

  // `system` follows the OS while the page is open.
  useEffect(() => {
    if (!applyToDocument || value !== 'system' || typeof window.matchMedia !== 'function') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => applyThemePreference('system');
    mq.addEventListener?.('change', onChange);
    return () => mq.removeEventListener?.('change', onChange);
  }, [applyToDocument, value]);

  const choose = (next: string) => {
    if (!isPreference(next)) return;
    if (cookies)
      cookies.set(cookieName, next, {
        maxAge: ONE_YEAR,
        domain: cookieDomain ?? defaultCookieDomain(),
      });
    else memoryPreference = next;
    if (applyToDocument) applyThemePreference(next);
    window.dispatchEvent(new CustomEvent(ROOK_THEME_EVENT, { detail: next }));
    onValueChange?.(next);
  };

  const options = PREFERENCES.map((p) => ({ value: p, label: messages[p], icon: ICONS[p] }));

  if (variant === 'menu') {
    const current = messages[value];
    return (
      <div
        ref={ref}
        {...rest}
        className={cx(ROOT, className)}
        data-variant="menu"
        data-value={value}
      >
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size={size}
              className="mn-theme-toggle__trigger"
              aria-label={`${messages.label}: ${current}`}
              leadingIcon={ICONS[value]}
            >
              {showLabel ? <span className="mn-theme-toggle__label">{current}</span> : null}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side={side} align={align} className="mn-theme-toggle__menu">
            <DropdownMenuLabel>{messages.label}</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={value} onValueChange={choose}>
              {options.map((o) => (
                <DropdownMenuRadioItem key={o.value} value={o.value}>
                  <span className="mn-theme-toggle__option">
                    {o.icon}
                    {o.label}
                  </span>
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx(ROOT, className)}
      data-variant="segmented"
      data-value={value}
    >
      <SegmentedControl
        aria-label={messages.label}
        options={options}
        value={value}
        onValueChange={choose}
        size={size}
      />
    </div>
  );
});
