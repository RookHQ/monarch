'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { applyAppearance } from '@rookcom/monarch-tokens';
import { cx } from '../../../internal/cx.js';
import {
  ONE_YEAR,
  defaultCookieDomain,
  documentCookieStore,
  type ShellCookieStore,
} from '../../../internal/shell/cookies.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { ROOK_THEME_COOKIE, ThemeToggle, type ThemePreference } from '../theme-toggle/index.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-appearance-panel';

/** Contrast: the standard palette, the more-contrast palette, or follow the device. */
export type AppearanceContrast = 'standard' | 'more' | 'system';
/** Motion: follow the device setting, or always reduce. */
export type AppearanceMotion = 'system' | 'reduce';
/** Density: default spacing, or tighter spacing. */
export type AppearanceDensity = 'comfortable' | 'compact';
/** The rows the panel can show. */
export type AppearanceSection = 'theme' | 'contrast' | 'motion' | 'density';

/** Every appearance preference the panel edits. */
export interface AppearancePreferences {
  theme: ThemePreference;
  contrast: AppearanceContrast;
  motion: AppearanceMotion;
  density: AppearanceDensity;
}

/** Interface text, for translation. */
export interface AppearancePanelMessages {
  theme: string;
  themeDescription: string;
  light: string;
  dark: string;
  system: string;
  contrast: string;
  contrastDescription: string;
  contrastStandard: string;
  contrastMore: string;
  contrastSystem: string;
  motion: string;
  motionDescription: string;
  motionSystem: string;
  motionReduce: string;
  density: string;
  densityDescription: string;
  densityComfortable: string;
  densityCompact: string;
}

const DEFAULT_MESSAGES: AppearancePanelMessages = {
  theme: 'Theme',
  themeDescription: 'Light, dark, or match your device.',
  light: 'Light',
  dark: 'Dark',
  system: 'System',
  contrast: 'Contrast',
  contrastDescription: 'Stronger text and border colours.',
  contrastStandard: 'Standard',
  contrastMore: 'More',
  contrastSystem: 'System',
  motion: 'Motion',
  motionDescription: 'Reduce animation and movement.',
  motionSystem: 'System',
  motionReduce: 'Reduce',
  density: 'Density',
  densityDescription: 'Tighter spacing fits more on screen.',
  densityComfortable: 'Comfortable',
  densityCompact: 'Compact',
};

export const DEFAULT_APPEARANCE: AppearancePreferences = {
  theme: 'light',
  contrast: 'system',
  motion: 'system',
  density: 'comfortable',
};

/** The cookie the tokens `appearanceScript` reads the contrast choice from. */
export const MONARCH_CONTRAST_COOKIE = 'mn_contrast';

const ALL_SECTIONS: readonly AppearanceSection[] = ['theme', 'contrast', 'motion', 'density'];
const CONTRASTS: readonly string[] = ['standard', 'more', 'system'];
const THEMES: readonly string[] = ['light', 'dark', 'system'];

export interface AppearancePanelProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children'
> {
  /** The preferences (controlled). */
  value?: AppearancePreferences | undefined;
  /** Starting preferences when uncontrolled; saved theme and contrast cookies win after hydration. */
  defaultValue?: Partial<AppearancePreferences> | undefined;
  /** Called with all preferences after any one changes. Save motion and density from here. */
  onValueChange?: ((value: AppearancePreferences) => void) | undefined;
  /** Which rows to show, in order. Default all four. */
  sections?: readonly AppearanceSection[] | undefined;
  /**
   * Where theme and contrast are saved. Default `document.cookie` (the
   * `rook_theme` and `mn_contrast` cookies the pre-paint script reads);
   * `null` keeps them in memory only.
   */
  cookies?: ShellCookieStore | null | undefined;
  /** Theme cookie name. Default `rook_theme`. */
  themeCookie?: string | undefined;
  /** Contrast cookie name. Default `mn_contrast`. */
  contrastCookie?: string | undefined;
  /** Cookie domain. Default: `.rookhq.com` on Rook hosts, otherwise the current host. */
  cookieDomain?: string | undefined;
  /**
   * Apply choices to `<html>`: theme and contrast as `data-mn-theme` /
   * `data-mn-contrast`, motion as `data-mn-motion="reduce"`, density as
   * `data-mn-density="compact"`. Default true.
   */
  applyToDocument?: boolean | undefined;
  /** Optional heading content above the rows. */
  heading?: ReactNode;
  /** Interface text, for translation. */
  messages?: Partial<AppearancePanelMessages> | undefined;
}

function prefersMoreContrast() {
  return (
    typeof window.matchMedia === 'function' && window.matchMedia('(prefers-contrast: more)').matches
  );
}

/** Applies contrast, motion and density to `root` (theme is applied by ThemeToggle). */
function applyRest(root: HTMLElement, value: AppearancePreferences) {
  const contrast =
    value.contrast === 'system' ? (prefersMoreContrast() ? 'more' : 'standard') : value.contrast;
  const theme = root.getAttribute('data-mn-theme') === 'dark' ? 'dark' : 'light';
  applyAppearance(root, { theme, contrast });
  if (value.motion === 'reduce') root.setAttribute('data-mn-motion', 'reduce');
  else root.removeAttribute('data-mn-motion');
  if (value.density === 'compact') root.setAttribute('data-mn-density', 'compact');
  else root.removeAttribute('data-mn-density');
}

/**
 * A settings panel for appearance: theme, contrast, motion and density. Theme
 * uses ThemeToggle (shared `rook_theme` cookie); contrast is saved in the
 * `mn_contrast` cookie the tokens pre-paint script reads; motion and density
 * are applied as `<html>` attributes and reported through `onValueChange`.
 */
export const AppearancePanel = forwardRef<HTMLDivElement, AppearancePanelProps>(
  function AppearancePanel(
    {
      value: valueProp,
      defaultValue,
      onValueChange,
      sections = ALL_SECTIONS,
      cookies = documentCookieStore,
      themeCookie = ROOK_THEME_COOKIE,
      contrastCookie = MONARCH_CONTRAST_COOKIE,
      cookieDomain,
      applyToDocument = true,
      heading,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const id = useId();
    const [internal, setInternal] = useState<AppearancePreferences>({
      ...DEFAULT_APPEARANCE,
      ...defaultValue,
    });
    const value = valueProp ?? internal;
    const [ready, setReady] = useState(false);

    // After hydration, pick up the saved theme and contrast (uncontrolled only).
    useEffect(() => {
      if (valueProp === undefined && cookies) {
        const theme = cookies.get(themeCookie);
        const contrast = cookies.get(contrastCookie);
        setInternal((prev) => ({
          ...prev,
          ...(theme && THEMES.includes(theme) ? { theme: theme as ThemePreference } : null),
          ...(contrast && CONTRASTS.includes(contrast)
            ? { contrast: contrast as AppearanceContrast }
            : null),
        }));
      }
      setReady(true);
      // Read once on mount.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
      if (!ready || !applyToDocument) return;
      applyRest(document.documentElement, value);
    }, [ready, applyToDocument, value]);

    // "System" contrast follows the device while the page is open.
    useEffect(() => {
      if (
        !applyToDocument ||
        value.contrast !== 'system' ||
        typeof window.matchMedia !== 'function'
      )
        return;
      const mq = window.matchMedia('(prefers-contrast: more)');
      const onChange = () => applyRest(document.documentElement, value);
      mq.addEventListener?.('change', onChange);
      return () => mq.removeEventListener?.('change', onChange);
    }, [applyToDocument, value]);

    const update = (patch: Partial<AppearancePreferences>) => {
      const next = { ...value, ...patch };
      if (valueProp === undefined) setInternal(next);
      onValueChange?.(next);
    };

    const chooseContrast = (contrast: AppearanceContrast) => {
      if (cookies)
        cookies.set(contrastCookie, contrast, {
          maxAge: ONE_YEAR,
          domain: cookieDomain ?? defaultCookieDomain(),
        });
      update({ contrast });
    };

    const row = (
      key: AppearanceSection,
      label: string,
      description: string,
      control: ReactNode,
    ) => (
      <div
        key={key}
        className="mn-appearance-panel__row"
        role="group"
        aria-labelledby={`${id}-${key}`}
        aria-describedby={`${id}-${key}-desc`}
        data-section={key}
      >
        <div className="mn-appearance-panel__text">
          <span id={`${id}-${key}`} className="mn-appearance-panel__label">
            {label}
          </span>
          <span id={`${id}-${key}-desc`} className="mn-appearance-panel__description">
            {description}
          </span>
        </div>
        <div className="mn-appearance-panel__control">{control}</div>
      </div>
    );

    const rows: Record<AppearanceSection, () => ReactNode> = {
      theme: () =>
        row(
          'theme',
          messages.theme,
          messages.themeDescription,
          <ThemeToggle
            value={value.theme}
            onValueChange={(theme) => update({ theme })}
            cookies={cookies}
            cookieName={themeCookie}
            cookieDomain={cookieDomain}
            applyToDocument={applyToDocument}
            messages={{
              label: messages.theme,
              light: messages.light,
              dark: messages.dark,
              system: messages.system,
            }}
          />,
        ),
      contrast: () =>
        row(
          'contrast',
          messages.contrast,
          messages.contrastDescription,
          <SegmentedControl<AppearanceContrast>
            aria-labelledby={`${id}-contrast`}
            value={value.contrast}
            onValueChange={chooseContrast}
            options={[
              { value: 'standard', label: messages.contrastStandard },
              { value: 'more', label: messages.contrastMore },
              { value: 'system', label: messages.contrastSystem },
            ]}
          />,
        ),
      motion: () =>
        row(
          'motion',
          messages.motion,
          messages.motionDescription,
          <SegmentedControl<AppearanceMotion>
            aria-labelledby={`${id}-motion`}
            value={value.motion}
            onValueChange={(motion) => update({ motion })}
            options={[
              { value: 'system', label: messages.motionSystem },
              { value: 'reduce', label: messages.motionReduce },
            ]}
          />,
        ),
      density: () =>
        row(
          'density',
          messages.density,
          messages.densityDescription,
          <SegmentedControl<AppearanceDensity>
            aria-labelledby={`${id}-density`}
            value={value.density}
            onValueChange={(density) => update({ density })}
            options={[
              { value: 'comfortable', label: messages.densityComfortable },
              { value: 'compact', label: messages.densityCompact },
            ]}
          />,
        ),
    };

    return (
      <div ref={ref} {...rest} className={cx(ROOT, className)}>
        {heading ? <div className="mn-appearance-panel__title">{heading}</div> : null}
        {sections.filter((s) => ALL_SECTIONS.includes(s)).map((s) => rows[s]())}
      </div>
    );
  },
);
