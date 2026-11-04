'use client';

import {
  Children,
  cloneElement,
  forwardRef,
  isValidElement,
  useState,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
export type AvatarStatus = 'online' | 'away' | 'busy' | 'offline';

export interface AvatarProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** Person or entity name. Used for the image alt text and the initials fallback. */
  name: string;
  src?: string | null;
  /** A preset, or a number in CSS pixels (e.g. `28`) for sizes between the presets. */
  size?: AvatarSize | number;
  /** `circle` for people; `square` for workspaces, organisations and brands. */
  shape?: 'circle' | 'square';
  /**
   * Set when a visible name sits next to the avatar, so screen readers do not
   * hear the name twice.
   */
  decorative?: boolean;
  /**
   * Hashed to pick the fallback colour. Default `name`. Pass a stable id so a
   * renamed person keeps their colour.
   */
  seed?: string;
  /**
   * Fallback colour source. `hue` (default) derives a soft colour from the
   * seed; `tokens` picks one of the five `--mn-color-data-*` tokens, so the
   * colour follows the theme and contrast mode.
   */
  palette?: 'hue' | 'tokens';
  /**
   * What to show when there is no image: `initials` (default), or `icon` for
   * entities such as an organisation without a logo. Initials also fall back to
   * the icon when the name has no letters.
   */
  fallback?: 'initials' | 'icon';
  /** Icon for the `icon` fallback. Default: a neutral person glyph. Decorative. */
  icon?: ReactNode;
  /** Presence dot. Its text alternative is added to the accessible name. */
  status?: AvatarStatus;
  /** Text alternative for `status`, e.g. a translated "Online". Default: the English status name. */
  statusLabel?: string;
}

const STATUS_LABELS: Record<AvatarStatus, string> = {
  online: 'Online',
  away: 'Away',
  busy: 'Busy',
  offline: 'Offline',
};

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0]?.charAt(0) ?? '';
  const last = parts.length > 1 ? (parts[parts.length - 1]?.charAt(0) ?? '') : '';
  return (first + last).toUpperCase();
}

/** Stable hash per seed so the same person gets the same fallback colour everywhere. */
function hashOf(seed: string, modulo: number): number {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) % modulo;
  return h;
}

/** Index (1–5) of the `--mn-color-data-*` token a seed maps to with `palette="tokens"`. */
export function avatarPaletteIndex(seed: string): number {
  return (hashOf(seed, 360) % 5) + 1;
}

function PersonGlyph() {
  return (
    <svg
      aria-hidden
      focusable={false}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="8" cy="5.5" r="2.75" />
      <path d="M2.75 14c.6-2.6 2.7-4.25 5.25-4.25s4.65 1.65 5.25 4.25" />
    </svg>
  );
}

/**
 * A person or entity image with a fallback chain: image, then initials on a
 * colour stable per seed, then an icon.
 */
export const Avatar = forwardRef<HTMLSpanElement, AvatarProps>(function Avatar(
  {
    name,
    src,
    size = 'md',
    shape = 'circle',
    decorative = false,
    seed,
    palette = 'hue',
    fallback = 'initials',
    icon,
    status,
    statusLabel,
    className,
    style,
    ...rest
  },
  ref,
) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const showImage = Boolean(src) && failedSrc !== src;
  const text = initials(name);
  const showIcon = !showImage && (fallback === 'icon' || !/\p{L}|\p{N}/u.test(text));
  const key = seed ?? name;
  const statusText = status ? (statusLabel ?? STATUS_LABELS[status]) : undefined;
  const a11y = decorative
    ? { 'aria-hidden': true as const }
    : {
        role: 'img' as const,
        'aria-label': statusText ? `${name} (${statusText})` : name,
      };
  const numeric = typeof size === 'number';
  const vars: Record<string, string> =
    palette === 'tokens' ? {} : { ['--mn-avatar-hue']: String(hashOf(key, 360)) };
  if (numeric) vars['--mn-avatar-size'] = `${size}px`;
  return (
    <span
      ref={ref}
      {...rest}
      {...a11y}
      className={cx('mn-avatar', className)}
      data-size={numeric ? 'custom' : size}
      data-shape={shape}
      data-palette={palette === 'tokens' ? avatarPaletteIndex(key) : undefined}
      data-status={status}
      style={{ ...style, ...vars }}
    >
      {showImage ? (
        <img
          className="mn-avatar__image"
          src={src ?? undefined}
          alt=""
          onError={() => setFailedSrc(src ?? null)}
        />
      ) : showIcon ? (
        <span className="mn-avatar__icon" aria-hidden>
          {icon ?? <PersonGlyph />}
        </span>
      ) : (
        <span className="mn-avatar__initials" aria-hidden>
          {text}
        </span>
      )}
      {status ? <span className="mn-avatar__status" aria-hidden /> : null}
    </span>
  );
});

export interface AvatarGroupProps extends HTMLAttributes<HTMLDivElement> {
  /** Names the group for assistive technology, e.g. "Assignees". */
  label: string;
  /** Show at most this many avatars, then a "+N" tile. Default: show all. */
  max?: number;
  /** Applied to every avatar that does not set its own size. */
  size?: AvatarSize | number;
  /** Applied to every avatar that does not set its own shape. */
  shape?: 'circle' | 'square';
  /** Text alternative for the "+N" tile. Default `(n) => \`${n} more\``. */
  overflowLabel?: (hidden: number) => string;
  /** `Avatar` elements. */
  children: ReactNode;
}

/**
 * Overlapping stack of avatars with a "+N" overflow tile. Each avatar keeps
 * its own name, so the group reads as a list of people.
 */
export const AvatarGroup = forwardRef<HTMLDivElement, AvatarGroupProps>(function AvatarGroup(
  {
    label,
    max,
    size = 'md',
    shape,
    overflowLabel = (n) => `${n} more`,
    className,
    style,
    children,
    ...rest
  },
  ref,
) {
  const avatars = Children.toArray(children).filter(isValidElement) as ReactElement<AvatarProps>[];
  const limit = max === undefined ? avatars.length : Math.max(0, Math.floor(max));
  // A "+1" tile would take the same space as the avatar it hides, so show that avatar instead.
  const visibleCount = avatars.length - limit === 1 ? avatars.length : limit;
  const visible = avatars.slice(0, visibleCount);
  const hidden = avatars.length - visible.length;
  const numeric = typeof size === 'number';
  return (
    <div
      ref={ref}
      role="group"
      aria-label={label}
      {...rest}
      className={cx('mn-avatar-group', className)}
      data-size={numeric ? 'custom' : size}
      style={numeric ? { ...style, ['--mn-avatar-size' as string]: `${size}px` } : style}
    >
      {visible.map((child) =>
        cloneElement(child, {
          size: child.props.size ?? size,
          ...(shape && !child.props.shape ? { shape } : {}),
        }),
      )}
      {hidden > 0 ? (
        <span
          role="img"
          aria-label={overflowLabel(hidden)}
          className="mn-avatar mn-avatar-group__overflow"
          data-size={numeric ? 'custom' : size}
          data-shape={shape ?? 'circle'}
        >
          <span aria-hidden>{`+${hidden}`}</span>
        </span>
      ) : null}
    </div>
  );
});
