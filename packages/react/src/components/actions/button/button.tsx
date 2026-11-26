'use client';

import {
  forwardRef,
  useMemo,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type ReactNode,
} from 'react';
import { Slot, Slottable } from '@radix-ui/react-slot';
import {
  deriveIntent,
  fromHex,
  THEME_IDS,
  themes,
  toCss,
  type ThemeId,
} from '@rookcom/monarch-tokens';
import { cx } from '../../../internal/cx.js';
import { Spinner } from '../../feedback/spinner/index.js';

export type ButtonVariant =
  'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-subtle' | 'success' | 'warning' | 'brand';
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Visual weight. Use one `primary` per view region. Default `secondary`. */
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Shows a spinner, sets aria-busy and blocks clicks while keeping focus. */
  loading?: boolean;
  /** Icon before the label. Decorative: hidden from assistive technology. */
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
  /** Stretch to the container width. */
  block?: boolean;
  /** Render the child element (e.g. a Next.js <Link>) with button styling. */
  asChild?: boolean;
  /**
   * Brand or tenant colour for `variant="brand"`, as `#rrggbb` or `#rgb`.
   * The fill and text are derived per theme by the tokens contrast engine, so
   * the label always meets WCAG AA (7:1 in more-contrast themes); the colour
   * may be darkened or lightened to get there. Ignored by other variants. An
   * invalid or missing colour falls back to the product accent.
   */
  brandColor?: string;
}

const THEME_KEY: Record<ThemeId, string> = {
  light: 'l',
  dark: 'd',
  'hc-light': 'hl',
  'hc-dark': 'hd',
};

/** Parses `#rgb` / `#rrggbb`; null when the value is not a hex colour. */
function parseHex(value: string | undefined) {
  if (!value) return null;
  const v = value.trim().replace(/^#/, '');
  const six = /^[0-9a-f]{3}$/i.test(v) ? v.replace(/./g, (c) => c + c) : v;
  if (!/^[0-9a-f]{6}$/i.test(six)) return null;
  return fromHex(six);
}

/**
 * Per-theme custom properties for a brand colour: fill, hover, active and
 * text for each of the four Monarch themes. button.css picks the set that
 * matches the theme on an ancestor (`.dark`, `data-mn-theme`, `data-mn-contrast`).
 */
function brandVars(color: string | undefined): CSSProperties | undefined {
  const base = parseHex(color);
  if (!base) return undefined;
  const vars: Record<string, string> = {};
  for (const id of THEME_IDS) {
    const { scale } = deriveIntent(base, themes[id]);
    const k = THEME_KEY[id];
    vars[`--_brand-${k}-bg`] = toCss(scale.solid);
    vars[`--_brand-${k}-bg-hover`] = toCss(scale['solid-hover']);
    vars[`--_brand-${k}-bg-active`] = toCss(scale['solid-active']);
    vars[`--_brand-${k}-fg`] = toCss(scale['on-solid']);
  }
  return vars as CSSProperties;
}

/**
 * The primary action control.
 *
 * Defaults to `type="button"` so a button inside a form never submits by
 * accident; pass `type="submit"` explicitly.
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = 'secondary',
    size = 'md',
    loading = false,
    leadingIcon,
    trailingIcon,
    block = false,
    asChild = false,
    brandColor,
    disabled,
    type,
    className,
    style,
    children,
    onClick,
    ...rest
  },
  ref,
) {
  const Comp = asChild ? Slot : 'button';
  const brand = variant === 'brand';
  const derived = useMemo(() => (brand ? brandVars(brandColor) : undefined), [brand, brandColor]);
  // A loading button stays focusable (aria-disabled, not disabled) so keyboard
  // users do not lose their place while the action completes.
  const blocked = disabled || loading;
  return (
    <Comp
      ref={ref}
      {...rest}
      {...(asChild ? {} : { type: type ?? 'button', disabled: disabled || undefined })}
      aria-disabled={blocked || undefined}
      aria-busy={loading || undefined}
      data-variant={variant}
      data-size={size}
      data-loading={loading || undefined}
      data-block={block || undefined}
      data-brand={derived ? '' : undefined}
      style={derived ? { ...derived, ...style } : style}
      className={cx('mn-button', className)}
      onClick={(event) => {
        if (blocked) {
          event.preventDefault();
          return;
        }
        onClick?.(event);
      }}
    >
      {loading ? (
        <Spinner size="sm" className="mn-button__icon" />
      ) : leadingIcon ? (
        <span className="mn-button__icon" aria-hidden>
          {leadingIcon}
        </span>
      ) : null}
      <Slottable>{children}</Slottable>
      {trailingIcon ? (
        <span className="mn-button__icon" aria-hidden>
          {trailingIcon}
        </span>
      ) : null}
    </Comp>
  );
});

export interface IconButtonProps extends Omit<
  ButtonProps,
  'leadingIcon' | 'trailingIcon' | 'children' | 'block' | 'aria-label'
> {
  /** Required accessible name, since there is no visible text. */
  label: string;
  icon: ReactNode;
}

/** Square button containing only an icon. `label` is required by the type system. */
export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { label, icon, variant = 'ghost', className, ...rest },
  ref,
) {
  return (
    <Button
      ref={ref}
      {...rest}
      variant={variant}
      aria-label={label}
      className={cx('mn-icon-button', className)}
    >
      {rest.loading ? null : (
        <span className="mn-button__icon" aria-hidden>
          {icon}
        </span>
      )}
    </Button>
  );
});
