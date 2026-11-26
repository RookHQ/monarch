'use client';

import {
  createContext,
  forwardRef,
  useContext,
  useMemo,
  type CSSProperties,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import {
  bestForeground,
  contrast,
  ensureContrast,
  fromHex,
  themes,
  toHex,
  type Hsl,
} from '@rookcom/monarch-tokens';
import { cx } from '../../../internal/cx.js';
import {
  DotGlyph,
  ErrorGlyph,
  HelpGlyph,
  InfoGlyph,
  PartialGlyph,
  SuccessGlyph,
  WarningGlyph,
  WrenchGlyph,
} from '../../../internal/icons.js';

/** The semantic tones a status maps to. They are Monarch intent tokens, so every theme works. */
export type StatusTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export interface StatusDefinition {
  /** Human label shown in the badge ("Partial outage"), never the raw enum. */
  label: string;
  tone: StatusTone;
  /**
   * Decorative icon. A distinct shape per status means colour is never the
   * only signal. `null` for none.
   */
  icon?: ReactNode;
  /**
   * Custom colour as `#rrggbb` (for example a workspace-defined ticket
   * status). Used for the dot, icon and border only; the text stays a theme
   * token. Adjusted per theme until it reaches 3:1 against the surface
   * (WCAG 1.4.11); see `checkStatusColor`.
   */
  color?: string;
  /** One line explaining the status, for a tooltip or legend the product renders. */
  description?: string;
}

export type StatusRegistry<K extends string = string> = Readonly<Record<K, StatusDefinition>>;

/** The statuses Monarch ships. Service health first, then the generic set. */
export const builtInStatuses = {
  operational: { label: 'Operational', tone: 'success', icon: <SuccessGlyph /> },
  degraded: { label: 'Degraded', tone: 'warning', icon: <WarningGlyph /> },
  partial: { label: 'Partial outage', tone: 'warning', icon: <PartialGlyph /> },
  down: { label: 'Down', tone: 'danger', icon: <ErrorGlyph /> },
  maintenance: { label: 'Maintenance', tone: 'info', icon: <WrenchGlyph /> },
  unknown: { label: 'Unknown', tone: 'neutral', icon: <HelpGlyph /> },
  success: { label: 'Success', tone: 'success', icon: <SuccessGlyph /> },
  warning: { label: 'Warning', tone: 'warning', icon: <WarningGlyph /> },
  danger: { label: 'Error', tone: 'danger', icon: <ErrorGlyph /> },
  info: { label: 'Info', tone: 'info', icon: <InfoGlyph /> },
  neutral: { label: 'Neutral', tone: 'neutral', icon: <DotGlyph /> },
} as const satisfies StatusRegistry;

export type BuiltInStatus = keyof typeof builtInStatuses;

/** Result of the contrast guard for a custom colour. */
export interface StatusColorCheck {
  /** The colour as given. */
  input: string;
  /** Colour for light and more-contrast light themes. */
  light: string;
  /** Colour for dark and more-contrast dark themes. */
  dark: string;
  /** Text colour for a solid fill of `light` (4.5:1 or more). */
  onLight: string;
  /** Text colour for a solid fill of `dark` (4.5:1 or more). */
  onDark: string;
  /** True when the input had to be darkened or lightened for at least one theme. */
  adjusted: boolean;
  /** Contrast of the input against each theme's surface. */
  ratios: { light: number; dark: number; 'hc-light': number; 'hc-dark': number };
}

const HEX = /^#[0-9a-f]{6}$/i;
const TEXT_CANDIDATES = [
  themes.light.neutrals['text-default'],
  themes.light.neutrals['text-inverse'],
] as const;
const BLACK_WHITE = [fromHex('#000000'), fromHex('#ffffff')] as const;

/** Dark or light text, whichever reads better on `hex`; always 4.5:1 or more. */
function readableOn(hex: string): string {
  const bg = fromHex(hex);
  const best = bestForeground(bg, TEXT_CANDIDATES);
  return toHex(best.ratio >= 4.5 ? best.color : bestForeground(bg, BLACK_WHITE).color);
}
const guardCache = new Map<string, StatusColorCheck | null>();

/**
 * The contrast guard. Measures `#rrggbb` against the surface of every Monarch
 * theme and, where it is below the theme's non-text target (3:1, or more in
 * the more-contrast themes), moves its lightness until it passes. Hue and
 * saturation are kept, so a workspace's colour stays recognisable. Returns
 * `null` for anything that is not `#rrggbb`.
 */
export function checkStatusColor(color: string): StatusColorCheck | null {
  const cached = guardCache.get(color);
  if (cached !== undefined) return cached;
  let result: StatusColorCheck | null = null;
  if (HEX.test(color)) {
    const c = fromHex(color);
    const surface = (id: keyof typeof themes): Hsl => themes[id].neutrals['bg-surface'];
    const ratios = {
      light: contrast(c, surface('light')),
      dark: contrast(c, surface('dark')),
      'hc-light': contrast(c, surface('hc-light')),
      'hc-dark': contrast(c, surface('hc-dark')),
    };
    const fit = (
      ids: readonly ('light' | 'dark' | 'hc-light' | 'hc-dark')[],
      dir: 'darken' | 'lighten',
    ) => {
      const target = Math.max(...ids.map((id) => themes[id].targets.ui));
      const against = ids.map(surface);
      if (against.every((bg) => contrast(c, bg) >= target))
        return { hex: color.toLowerCase(), moved: false };
      return { hex: toHex(ensureContrast(c, against, target, dir)), moved: true };
    };
    const light = fit(['light', 'hc-light'], 'darken');
    const dark = fit(['dark', 'hc-dark'], 'lighten');
    result = {
      input: color,
      light: light.hex,
      dark: dark.hex,
      onLight: readableOn(light.hex),
      onDark: readableOn(dark.hex),
      adjusted: light.moved || dark.moved,
      ratios,
    };
  }
  guardCache.set(color, result);
  return result;
}

/**
 * Defines a product's own statuses, typed by key. Custom colours are checked
 * up front: an invalid one throws here, in development, instead of rendering
 * an unreadable badge.
 *
 *   const ticketStatuses = defineStatuses({
 *     open: { label: 'Open', tone: 'info' },
 *     waiting: { label: 'Waiting on customer', tone: 'warning', icon: <Clock /> },
 *   });
 */
export function defineStatuses<K extends string>(
  definitions: Record<K, StatusDefinition>,
): StatusRegistry<K>;
/** With `{ extend: true }` the result also contains the built-in statuses. */
export function defineStatuses<K extends string>(
  definitions: Record<K, StatusDefinition>,
  options: { extend: true },
): StatusRegistry<K | BuiltInStatus>;
export function defineStatuses<K extends string>(
  definitions: Record<K, StatusDefinition>,
  options: { extend?: boolean } = {},
): StatusRegistry {
  for (const [key, def] of Object.entries(definitions) as [K, StatusDefinition][]) {
    if (def.color !== undefined && checkStatusColor(def.color) === null) {
      throw new Error(`defineStatuses: "${key}" has colour "${def.color}"; use #rrggbb.`);
    }
  }
  return options.extend ? { ...builtInStatuses, ...definitions } : { ...definitions };
}

const StatusRegistryContext = createContext<StatusRegistry | null>(null);

export interface StatusRegistryProviderProps {
  /** Statuses added to (and overriding) the built-in set for everything inside. */
  statuses: StatusRegistry;
  children: ReactNode;
}

/** Makes a product's statuses available to every StatusBadge below it. Nested providers add up. */
export function StatusRegistryProvider({ statuses, children }: StatusRegistryProviderProps) {
  const parent = useContext(StatusRegistryContext);
  const value = useMemo(() => ({ ...(parent ?? {}), ...statuses }), [parent, statuses]);
  return <StatusRegistryContext.Provider value={value}>{children}</StatusRegistryContext.Provider>;
}

/** "pending_delete" → "Pending delete". Used for statuses that are not in the registry. */
export function formatStatusLabel(status: string): string {
  const words = status.replace(/[_-]+/g, ' ').trim().toLowerCase();
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : 'Unknown';
}

/**
 * Looks up a status: the `statuses` argument first, then the nearest
 * StatusRegistryProvider, then the built-in set. Unknown values resolve to
 * the neutral "unknown" look with a readable label, never a raw enum.
 */
export function resolveStatus(
  status: string | null | undefined,
  ...registries: (StatusRegistry | null | undefined)[]
): StatusDefinition & { known: boolean } {
  const key = status ?? 'unknown';
  for (const registry of [...registries, builtInStatuses as StatusRegistry]) {
    const def =
      registry && Object.prototype.hasOwnProperty.call(registry, key) ? registry[key] : undefined;
    if (def) return { ...def, known: true };
  }
  return {
    ...builtInStatuses.unknown,
    label: status ? formatStatusLabel(status) : 'Unknown',
    known: false,
  };
}

export interface StatusBadgeProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'color'> {
  /** The status key: a built-in one or one from `statuses` / a StatusRegistryProvider. */
  status: BuiltInStatus | (string & {}) | null | undefined;
  /** Statuses for this badge only, checked before the provider and the built-in set. */
  statuses?: StatusRegistry;
  /** Overrides the registry label, for example with a translated string. */
  label?: ReactNode;
  /**
   * `subtle` (default) tinted pill; `outline` for dense tables; `solid` for a
   * single prominent status; `dot` for a quiet coloured dot plus text.
   */
  variant?: 'subtle' | 'outline' | 'solid' | 'dot';
  size?: 'sm' | 'md';
  /** Replaces the registry icon. `false` hides it (the text still carries the meaning). */
  icon?: ReactNode | false;
  /** Overrides the registry colour (`#rrggbb`), passed through the contrast guard. */
  color?: string;
}

/**
 * A status shown as text plus a tone and a distinct icon or dot, so colour is
 * never the only signal. Statuses come from a typed registry that products
 * extend with `defineStatuses` and `StatusRegistryProvider`.
 */
export const StatusBadge = forwardRef<HTMLSpanElement, StatusBadgeProps>(function StatusBadge(
  {
    status,
    statuses,
    label,
    variant = 'subtle',
    size = 'md',
    icon,
    color,
    className,
    style,
    children,
    ...rest
  },
  ref,
) {
  const provided = useContext(StatusRegistryContext);
  const def = resolveStatus(status, statuses, provided);
  const custom = checkStatusColor(color ?? def.color ?? '');
  // The dot variant shows a dot instead of the icon, unless an icon is passed explicitly.
  const explicitIcon = icon !== undefined && icon !== false;
  const shownIcon =
    icon === false ? null : explicitIcon ? icon : variant === 'dot' ? null : def.icon;
  // A custom colour must be visible somewhere: without an icon it gets a dot.
  const showDot = !explicitIcon && (variant === 'dot' || (custom !== null && !shownIcon));
  const customStyle = custom
    ? ({
        '--mn-status-light': custom.light,
        '--mn-status-dark': custom.dark,
        '--mn-status-on-light': custom.onLight,
        '--mn-status-on-dark': custom.onDark,
      } as CSSProperties)
    : undefined;

  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-status-badge', className)}
      style={customStyle ? { ...customStyle, ...style } : style}
      data-status={status ?? 'unknown'}
      data-tone={def.tone}
      data-variant={variant}
      data-size={size}
      data-custom-color={custom ? '' : undefined}
      data-known={def.known ? undefined : 'false'}
    >
      {showDot ? <span className="mn-status-badge__dot" aria-hidden /> : null}
      {shownIcon ? (
        <span className="mn-status-badge__icon" aria-hidden>
          {shownIcon}
        </span>
      ) : null}
      <span className="mn-status-badge__label">{label ?? children ?? def.label}</span>
    </span>
  );
});
