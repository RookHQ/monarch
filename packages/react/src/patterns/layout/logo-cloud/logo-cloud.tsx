'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';

/** One organisation in the cloud. */
export interface LogoCloudItem {
  /** Stable key. Default: the name. */
  id?: string | undefined;
  /** Organisation name. The logo's alt text, and the wordmark when there is no image. */
  name: string;
  /**
   * Logo image URL (http, https or a same-origin path). Unsafe or failing
   * sources fall back to the name as a wordmark.
   */
  src?: string | undefined;
  /** Separate logo for dark themes. Shown instead of `src` when the page is dark. */
  srcDark?: string | undefined;
  /** Intrinsic width of the image, to reserve space and avoid layout shift. */
  width?: number | undefined;
  /** Intrinsic height of the image. */
  height?: number | undefined;
  /** Optional link, e.g. to a case study. Unsafe URLs render as plain logos. */
  href?: string | undefined;
  /** Open `href` in a new tab (with `rel="noopener noreferrer"`). */
  external?: boolean | undefined;
}

export interface LogoCloudMessages {
  /** Accessible name of the list when there is no visible `title`. Default "Organisations". */
  listLabel: string;
  /** Hidden text appended to links that open in a new tab. Default "(opens in a new tab)". */
  newTab: string;
}

export const defaultLogoCloudMessages: LogoCloudMessages = {
  listLabel: 'Organisations',
  newTab: '(opens in a new tab)',
};

export interface LogoCloudProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The organisations, in display order. Only list ones with permission to be shown. */
  logos: readonly LogoCloudItem[];
  /** Short line above the logos, such as "Teams that use it". Names the section. */
  title?: ReactNode;
  /** Heading level for `title`. Default `h2`; `p` keeps it out of the outline. */
  titleAs?: 'h2' | 'h3' | 'p' | undefined;
  /** `row` wraps logos centred on lines; `grid` puts them in equal cells. Default `row`. */
  layout?: 'row' | 'grid' | undefined;
  /** Logo height. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Shows logos in a single muted tone (full colour on hover or focus). Default `true`. */
  monochrome?: boolean | undefined;
  messages?: Partial<LogoCloudMessages> | undefined;
}

function Logo({ item, newTab }: { item: LogoCloudItem; newTab: string }) {
  const [failed, setFailed] = useState(false);
  const src = item.src && isSafeImageSrc(item.src) ? item.src : undefined;
  const srcDark = item.srcDark && isSafeImageSrc(item.srcDark) ? item.srcDark : undefined;
  const showImage = src !== undefined && !failed;
  const visual = showImage ? (
    <picture className="mn-logo-cloud__picture">
      {srcDark ? <source srcSet={srcDark} media="(prefers-color-scheme: dark)" /> : null}
      <img
        className="mn-logo-cloud__image"
        src={src}
        alt={item.name}
        width={item.width}
        height={item.height}
        loading="lazy"
        decoding="async"
        onError={() => setFailed(true)}
      />
    </picture>
  ) : (
    <span className="mn-logo-cloud__wordmark">{item.name}</span>
  );
  const href = item.href && isSafeUrl(item.href) ? item.href : undefined;
  if (!href) return visual;
  return (
    <a
      className="mn-logo-cloud__link"
      href={href}
      {...(item.external && { target: '_blank', rel: 'noopener noreferrer' })}
    >
      {visual}
      {item.external ? <span className="mn-logo-cloud__hidden"> {newTab}</span> : null}
    </a>
  );
}

/**
 * A row or grid of organisation logos for a public page, with a short
 * title. Image and link URLs are checked; a logo that is missing, unsafe or
 * fails to load is shown as the organisation's name instead.
 */
export const LogoCloud = forwardRef<HTMLElement, LogoCloudProps>(function LogoCloud(
  {
    logos,
    title,
    titleAs: Title = 'h2',
    layout = 'row',
    size = 'md',
    monochrome = true,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultLogoCloudMessages, ...messages };
  const titleId = useId();
  return (
    <section
      ref={ref}
      {...(title ? { 'aria-labelledby': titleId } : { 'aria-label': m.listLabel })}
      {...rest}
      className={cx('mn-logo-cloud', className)}
      data-layout={layout}
      data-size={size}
      data-monochrome={monochrome || undefined}
    >
      {title ? (
        <Title id={titleId} className="mn-logo-cloud__title">
          {title}
        </Title>
      ) : null}
      <ul className="mn-logo-cloud__list">
        {logos.map((item) => (
          <li key={item.id ?? item.name} className="mn-logo-cloud__item">
            <Logo item={item} newTab={m.newTab} />
          </li>
        ))}
      </ul>
    </section>
  );
});
