import {
  forwardRef,
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Spinner } from '../../feedback/spinner/index.js';

/**
 * `light` and `dark` keep the same look on every page whatever its theme;
 * `neutral` is a borderless light grey; `auto` follows the page's theme.
 */
export type SignInWithRookButtonVariant = 'light' | 'dark' | 'neutral' | 'auto';
export type SignInWithRookButtonSize = 'sm' | 'md' | 'lg';

interface SignInWithRookButtonOwnProps {
  /** Default `light`. */
  variant?: SignInWithRookButtonVariant | undefined;
  /** `sm` 32px, `md` 40px (default), `lg` 48px tall. */
  size?: SignInWithRookButtonSize | undefined;
  /** The visible label. Default "Sign in with Rook ID". */
  label?: string | undefined;
  /**
   * URL of the Rook ID mark, served by your site. Rendered decoratively
   * before the label. Use `mark` instead to pass an element (for example an
   * inline SVG).
   */
  markSrc?: string | undefined;
  /** The mark as an element; wins over `markSrc`. Decorative. */
  mark?: ReactNode;
  /** The sign-in is starting: shows a spinner, sets `aria-busy` and blocks clicks. */
  loading?: boolean | undefined;
  /** Stretch to the container width. */
  block?: boolean | undefined;
}

/** Renders a `<button>` (default type "button"). */
export type SignInWithRookButtonAsButtonProps = SignInWithRookButtonOwnProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { href?: undefined };

/** Renders an `<a>`, for a sign-in route that starts with a plain GET (e.g. `/login`). */
export type SignInWithRookButtonAsLinkProps = SignInWithRookButtonOwnProps &
  Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'children'> & { href: string };

export type SignInWithRookButtonProps =
  SignInWithRookButtonAsButtonProps | SignInWithRookButtonAsLinkProps;

const THEME: Record<SignInWithRookButtonVariant, 'light' | 'dark' | undefined> = {
  light: 'light',
  dark: 'dark',
  neutral: 'light',
  auto: undefined,
};

/**
 * The "Sign in with Rook ID" button, for any site or app that offers Rook ID
 * sign-in. A native button by default; pass `href` to render a link. Its
 * `light` and `dark` variants pin the theme so it looks the same on every
 * page.
 */
export const SignInWithRookButton = forwardRef<
  HTMLButtonElement | HTMLAnchorElement,
  SignInWithRookButtonProps
>(function SignInWithRookButton(props, ref) {
  const {
    variant = 'light',
    size = 'md',
    label = 'Sign in with Rook ID',
    markSrc,
    mark,
    loading = false,
    block = false,
    className,
    ...rest
  } = props;
  const shared = {
    className: cx('mn-sign-in-with-rook-button', className),
    'data-variant': variant,
    'data-size': size,
    'data-block': block || undefined,
    'data-loading': loading || undefined,
    'data-mn-theme': THEME[variant],
    'aria-busy': loading || undefined,
  };
  const content = (
    <>
      {loading || mark || markSrc ? (
        <span className="mn-sign-in-with-rook-button__mark" aria-hidden="true">
          {loading ? <Spinner size="sm" /> : (mark ?? <img src={markSrc} alt="" />)}
        </span>
      ) : null}
      <span className="mn-sign-in-with-rook-button__label">{label}</span>
    </>
  );

  if (typeof rest.href === 'string') {
    const { onClick, href, ...anchor } = rest as AnchorHTMLAttributes<HTMLAnchorElement> & {
      href: string;
    };
    return (
      <a
        ref={ref as Ref<HTMLAnchorElement>}
        {...anchor}
        {...shared}
        href={href}
        aria-disabled={loading || undefined}
        onClick={(event) => {
          if (loading) {
            event.preventDefault();
            return;
          }
          onClick?.(event);
        }}
      >
        {content}
      </a>
    );
  }
  const {
    type = 'button',
    onClick,
    href: _href,
    ...button
  } = rest as ButtonHTMLAttributes<HTMLButtonElement> & { href?: undefined };
  return (
    <button
      ref={ref as Ref<HTMLButtonElement>}
      {...button}
      {...shared}
      type={type}
      onClick={(event) => {
        if (loading) {
          event.preventDefault();
          return;
        }
        onClick?.(event);
      }}
    >
      {content}
    </button>
  );
});
