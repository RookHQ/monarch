'use client';

import {
  forwardRef,
  useState,
  type AnchorHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';
import { Favicon, faviconHostname } from '../../data-display/favicon/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** A link preview's picture. */
export interface LinkCardImage {
  /** Image URL. Only http(s), relative and data:image sources are rendered. */
  src: string;
  /** Natural width, used with `height` to reserve the right space before it loads. */
  width?: number | undefined;
  /** Natural height. */
  height?: number | undefined;
  /** Alternative text. Default empty: the title already describes the link. */
  alt?: string | undefined;
}

/** The data of one link preview, as a server unfurled it. */
export interface LinkCardPreview {
  /** The link. Rendered only when it is http(s) or mailto; otherwise the card is plain text. */
  url: string;
  /** Page title. */
  title: string;
  /** Page description, clamped to two lines. */
  description?: string | undefined;
  /** Site name shown above the title. Default: the URL's host name. */
  siteName?: string | undefined;
  /** The page's picture. */
  image?: LinkCardImage | null | undefined;
  /** The link is a video: a play mark is drawn on the picture. */
  video?: boolean | undefined;
  /** The site's icon. Without it the Favicon component draws a letter. */
  faviconSrc?: string | undefined;
}

/** Every string a link card announces. */
export interface LinkCardMessages {
  /** Name of the dismiss button. */
  dismiss: (title: string) => string;
  /** Appended to the link name when it opens a new tab. */
  newTab: string;
  /** Appended to the link name for a video. */
  video: string;
}

export const LINK_CARD_MESSAGES: LinkCardMessages = {
  dismiss: (title) => `Remove the preview of ${title}`,
  newTab: '(opens in a new tab)',
  video: 'Video',
};

const DATA_IMAGE = /^data:image\/(?:png|jpe?g|gif|webp|avif|svg\+xml)[;,]/i;

/** http(s), relative and blob: sources, plus inline data:image pictures (never run as script inside an img). */
function isSafeCardImage(src: string): boolean {
  return isSafeImageSrc(src) || DATA_IMAGE.test(src.trim());
}

/** Pictures are shown between a near-square and a wide banner, so one odd picture never dominates. */
export const LINK_CARD_MIN_RATIO = 1.2;
export const LINK_CARD_MAX_RATIO = 2.2;

/** The aspect ratio a card picture is shown at: its own, kept between 1.2 and 2.2 (2.2 when unknown). */
export function linkCardImageRatio(width?: number, height?: number): number {
  if (!width || !height) return LINK_CARD_MAX_RATIO;
  return Math.min(LINK_CARD_MAX_RATIO, Math.max(LINK_CARD_MIN_RATIO, width / height));
}

export interface LinkCardProps
  extends LinkCardPreview, Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** `sm` for narrow places such as threads; `md` (default) in the main timeline. */
  size?: 'sm' | 'md' | undefined;
  /** Open the link in a new tab. Default true. */
  newTab?: boolean | undefined;
  /** Extra `rel` values. `noopener noreferrer` is always added for new tabs. Default `nofollow`. */
  rel?: string | undefined;
  /** Props for the link element, such as an `onClick` for analytics. */
  linkProps?: Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href' | 'children'> | undefined;
  /** Shows a remove button (for the author of the message). Called when it is pressed. */
  onDismiss?: (() => void) | undefined;
  /** Extra content under the text, such as a product-specific action row. Not part of the link. */
  footer?: ReactNode | undefined;
  /** Override any string. */
  messages?: Partial<LinkCardMessages> | undefined;
}

/**
 * A preview of a shared link: picture, site, title and description, as one
 * link. Data comes in already unfurled; the card never fetches. The picture
 * keeps its space while it loads and is left out if it fails, and an optional
 * button removes the preview.
 */
export const LinkCard = forwardRef<HTMLDivElement, LinkCardProps>(function LinkCard(
  {
    url,
    title,
    description,
    siteName,
    image,
    video = false,
    faviconSrc,
    size = 'md',
    newTab = true,
    rel = 'nofollow',
    linkProps,
    onDismiss,
    footer,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...LINK_CARD_MESSAGES, ...messagesProp };
  const [imageState, setImageState] = useState<'loading' | 'loaded' | 'failed'>('loading');
  const safe = isSafeUrl(url);
  const host = faviconHostname(url);
  const site = siteName ?? host;
  const showImage = image && isSafeCardImage(image.src) && imageState !== 'failed';

  const body = (
    <>
      {showImage ? (
        <span
          className="mn-link-card__media"
          style={{ aspectRatio: String(linkCardImageRatio(image.width, image.height)) }}
          data-state={imageState}
        >
          <img
            src={image.src}
            alt={image.alt ?? ''}
            width={image.width}
            height={image.height}
            loading="lazy"
            decoding="async"
            onLoad={() => setImageState('loaded')}
            onError={() => setImageState('failed')}
            className="mn-link-card__image"
          />
          {video ? (
            <span className="mn-link-card__play" aria-hidden>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M8 5.5v13l11-6.5z" />
              </svg>
            </span>
          ) : null}
        </span>
      ) : null}
      <span className="mn-link-card__text">
        {site ? (
          <span className="mn-link-card__site">
            <Favicon
              domain={host || site}
              name={site}
              size={14}
              {...(faviconSrc ? { src: faviconSrc } : {})}
            />
            <span className="mn-link-card__site-name">{site}</span>
          </span>
        ) : null}
        <span className="mn-link-card__title">{title}</span>
        {description ? <span className="mn-link-card__description">{description}</span> : null}
        {video ? <span className="mn-visually-hidden">, {m.video}</span> : null}
        {safe && newTab ? <span className="mn-visually-hidden"> {m.newTab}</span> : null}
      </span>
    </>
  );

  const relValue = newTab
    ? [...new Set(['noopener', 'noreferrer', ...rel.split(/\s+/).filter(Boolean)])].join(' ')
    : rel || undefined;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-link-card', className)}
      data-size={size}
      data-video={video || undefined}
    >
      {safe ? (
        <a
          {...linkProps}
          href={url}
          className={cx('mn-link-card__link', linkProps?.className)}
          {...(newTab ? { target: '_blank' } : {})}
          rel={relValue}
        >
          {body}
        </a>
      ) : (
        <div className="mn-link-card__link" data-inert="">
          {body}
        </div>
      )}
      {footer ? <div className="mn-link-card__footer">{footer}</div> : null}
      {onDismiss ? (
        <button
          type="button"
          className="mn-link-card__dismiss"
          aria-label={m.dismiss(title)}
          onClick={onDismiss}
        >
          <CloseGlyph aria-hidden />
        </button>
      ) : null}
    </div>
  );
});

export interface LinkCardSkeletonProps extends HTMLAttributes<HTMLDivElement> {
  /** Match the card size it stands in for. Default `md`. */
  size?: 'sm' | 'md' | undefined;
}

/**
 * Holds the space of a link card while its preview is being fetched, so the
 * message does not jump when the card arrives. Hidden from assistive technology.
 */
export const LinkCardSkeleton = forwardRef<HTMLDivElement, LinkCardSkeletonProps>(
  function LinkCardSkeleton({ size = 'md', className, ...rest }, ref) {
    return (
      <div
        ref={ref}
        aria-hidden
        {...rest}
        className={cx('mn-link-card', 'mn-link-card--skeleton', className)}
        data-size={size}
      >
        <Skeleton shape="rect" className="mn-link-card__media" />
        <div className="mn-link-card__text">
          <Skeleton shape="text" width="5rem" />
          <Skeleton shape="text" width="75%" />
          <Skeleton shape="text" width="100%" />
        </div>
      </div>
    );
  },
);

export interface LinkCardListProps extends HTMLAttributes<HTMLDivElement> {
  /** The previews of one message, in the order the links appear. */
  previews: readonly LinkCardPreview[];
  /** Card size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Shows a remove button on each card; receives the preview's URL. */
  onDismiss?: ((url: string) => void) | undefined;
  /** Show a skeleton after the cards while another preview is still being fetched. */
  pending?: boolean | undefined;
  /** Strings passed to every card. */
  messages?: Partial<LinkCardMessages> | undefined;
}

/** The link cards of one message, stacked. Renders nothing when there are none and nothing is pending. */
export const LinkCardList = forwardRef<HTMLDivElement, LinkCardListProps>(function LinkCardList(
  { previews, size = 'md', onDismiss, pending = false, messages, className, ...rest },
  ref,
) {
  if (!previews.length && !pending) return null;
  return (
    <div ref={ref} {...rest} className={cx('mn-link-card-list', className)}>
      {previews.map((p) => (
        <LinkCard
          key={p.url}
          {...p}
          size={size}
          {...(onDismiss ? { onDismiss: () => onDismiss(p.url) } : {})}
          {...(messages ? { messages } : {})}
        />
      ))}
      {pending ? <LinkCardSkeleton size={size} /> : null}
    </div>
  );
});
