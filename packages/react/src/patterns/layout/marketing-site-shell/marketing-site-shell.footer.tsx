import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { SiteLinkView } from './marketing-site-shell.link.js';
import type { LinkComponent, SiteLink } from './marketing-site-shell.types.js';

type HeadingLevel = 'h2' | 'h3' | 'h4';

export interface SiteFooterColumn {
  title: string;
  links: SiteLink[];
}

export interface SiteFooterProps extends HTMLAttributes<HTMLElement> {
  /** Wordmark (link home). */
  brand?: ReactNode;
  /** One sentence about the product or company. */
  description?: ReactNode;
  columns?: SiteFooterColumn[];
  /** Copyright and company line. */
  legal?: ReactNode;
  /** Policy links beside the legal line. */
  legalLinks?: SiteLink[];
  /** Extra content on the trailing edge of the bottom row (language switcher, status). */
  meta?: ReactNode;
  /** Heading element for column titles. */
  headingAs?: HeadingLevel;
  /** Router link component. Default `a`. */
  linkAs?: LinkComponent;
  /** Accessible name of the footer navigation. */
  navLabel?: string;
}

/**
 * Site footer: brand, link columns with real headings, and a legal row. Every
 * link has at least a 24px target (WCAG 2.5.8). Server-component safe.
 */
export function SiteFooter({
  brand,
  description,
  columns = [],
  legal,
  legalLinks = [],
  meta,
  headingAs: Heading = 'h2',
  linkAs,
  navLabel = 'Footer',
  className,
  ...rest
}: SiteFooterProps) {
  return (
    <footer {...rest} className={cx('mn-site-footer', className)}>
      <div className="mn-site-footer__inner">
        <div className="mn-site-footer__top">
          {brand || description ? (
            <div className="mn-site-footer__about">
              {brand ? <div className="mn-site-footer__brand">{brand}</div> : null}
              {description ? <p className="mn-site-footer__description">{description}</p> : null}
            </div>
          ) : null}
          {columns.length ? (
            <nav aria-label={navLabel} className="mn-site-footer__nav">
              {columns.map((col) => (
                <div key={col.title} className="mn-site-footer__column">
                  <Heading className="mn-site-footer__heading">{col.title}</Heading>
                  <ul>
                    {col.links.map((link) => (
                      <li key={link.href}>
                        <SiteLinkView link={link} as={linkAs} className="mn-site-footer__link" />
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </nav>
          ) : null}
        </div>
        {legal || legalLinks.length || meta ? (
          <div className="mn-site-footer__bottom">
            {legal ? <p className="mn-site-footer__legal">{legal}</p> : null}
            {legalLinks.length ? (
              <ul className="mn-site-footer__legal-links">
                {legalLinks.map((link) => (
                  <li key={link.href}>
                    <SiteLinkView link={link} as={linkAs} className="mn-site-footer__link" />
                  </li>
                ))}
              </ul>
            ) : null}
            {meta ? <div className="mn-site-footer__meta">{meta}</div> : null}
          </div>
        ) : null}
      </div>
    </footer>
  );
}
