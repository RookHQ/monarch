import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';

/** A small link under the form: terms, privacy, help. */
export interface AuthPageLink {
  /** Stable key. Default: the href. */
  id?: string | undefined;
  label: ReactNode;
  /** Unsafe URLs (such as `javascript:`) are dropped. */
  href: string;
  /** Open in a new tab (with `rel="noopener noreferrer"`). */
  external?: boolean | undefined;
}

/** Assistive-technology text. English defaults. */
export interface AuthPageMessages {
  /** Accessible name of the footer link list. */
  linksLabel: string;
  /** Hidden text after links that open a new tab. */
  newTab: string;
}

export const defaultAuthPageMessages: AuthPageMessages = {
  linksLabel: 'Legal and help',
  newTab: '(opens in a new tab)',
};

export interface AuthPageProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Product logo or wordmark above the card. */
  brand?: ReactNode;
  /** Makes `brand` a link (usually to the public home page). Unsafe URLs are ignored. */
  brandHref?: string | undefined;
  /** Accessible name of the brand link, such as "Product home". Needed when `brand` is an image without alt text. */
  brandLabel?: string | undefined;
  /** The page heading: "Sign in", "Create your account", "Reset your password". */
  title: ReactNode;
  /** One line under the title. */
  description?: ReactNode;
  /** Heading element. Default `h1`. */
  headingLevel?: 'h1' | 'h2' | undefined;
  /** A notice above the form: a session-expired Alert, an error from the last attempt. */
  notice?: ReactNode;
  /** The form. AuthPage holds no form logic. */
  children?: ReactNode;
  /** A line under the form, such as "No account yet? Create one". */
  footer?: ReactNode;
  /** Small links at the bottom: terms, privacy, help. */
  links?: readonly AuthPageLink[] | undefined;
  /**
   * A side panel on wide screens (product message, illustration). Hidden
   * below 1024px, so it must not hold anything needed to sign in.
   */
  aside?: ReactNode;
  /** Side of the panel. Default `start`. */
  asidePosition?: 'start' | 'end' | undefined;
  /** `card` puts the form on a raised surface; `plain` sits on the page. Default `card`. */
  variant?: 'card' | 'plain' | undefined;
  messages?: Partial<AuthPageMessages> | undefined;
}

/**
 * The centred layout for sign-in, sign-up, password reset and verification
 * screens: brand, heading, an optional notice, the product's form, a footer
 * line and small legal links, with an optional side panel on wide screens.
 * Server-component safe.
 */
export const AuthPage = forwardRef<HTMLDivElement, AuthPageProps>(function AuthPage(
  {
    brand,
    brandHref,
    brandLabel,
    title,
    description,
    headingLevel: Heading = 'h1',
    notice,
    children,
    footer,
    links,
    aside,
    asidePosition = 'start',
    variant = 'card',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultAuthPageMessages, ...messages };
  const titleId = useId();
  const safeBrandHref = brandHref && isSafeUrl(brandHref) ? brandHref : undefined;
  const safeLinks = (links ?? []).filter((link) => isSafeUrl(link.href));

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-auth-page', className)}
      data-variant={variant}
      data-has-aside={aside ? '' : undefined}
      data-aside-position={aside ? asidePosition : undefined}
    >
      {aside ? <div className="mn-auth-page__aside">{aside}</div> : null}
      <div className="mn-auth-page__main">
        <div className="mn-auth-page__column">
          {brand ? (
            <div className="mn-auth-page__brand">
              {safeBrandHref ? (
                <a
                  className="mn-auth-page__brand-link"
                  href={safeBrandHref}
                  {...(brandLabel !== undefined && { 'aria-label': brandLabel })}
                >
                  {brand}
                </a>
              ) : (
                brand
              )}
            </div>
          ) : null}
          <section className="mn-auth-page__card" aria-labelledby={titleId}>
            <div className="mn-auth-page__heading">
              <Heading id={titleId} className="mn-auth-page__title">
                {title}
              </Heading>
              {description ? <p className="mn-auth-page__description">{description}</p> : null}
            </div>
            {notice ? <div className="mn-auth-page__notice">{notice}</div> : null}
            {children ? <div className="mn-auth-page__form">{children}</div> : null}
            {footer ? <div className="mn-auth-page__footer">{footer}</div> : null}
          </section>
          {safeLinks.length > 0 ? (
            <nav className="mn-auth-page__links" aria-label={m.linksLabel}>
              <ul className="mn-auth-page__link-list">
                {safeLinks.map((link) => (
                  <li key={link.id ?? link.href}>
                    <a
                      className="mn-auth-page__link"
                      href={link.href}
                      {...(link.external && { target: '_blank', rel: 'noopener noreferrer' })}
                    >
                      {link.label}
                      {link.external ? (
                        <span className="mn-auth-page__hidden"> {m.newTab}</span>
                      ) : null}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          ) : null}
        </div>
      </div>
    </div>
  );
});
