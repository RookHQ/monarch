import type { ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import type { LinkComponent, SiteLink as SiteLinkData } from './marketing-site-shell.types.js';

/** Renders a SiteLink with the app's router link, `aria-current` and external handling. */
export function SiteLinkView({
  link,
  as: Link = 'a',
  className,
  children,
}: {
  link: SiteLinkData;
  as?: LinkComponent | undefined;
  className?: string | undefined;
  children?: ReactNode;
}) {
  const external = link.external ? { target: '_blank', rel: 'noopener noreferrer' } : {};
  return (
    <Link
      href={link.href}
      className={cx(className)}
      aria-current={link.current ? 'page' : undefined}
      {...external}
    >
      {children ?? link.label}
      {link.external ? <span className="mn-visually-hidden"> (opens in a new tab)</span> : null}
    </Link>
  );
}
