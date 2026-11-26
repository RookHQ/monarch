'use client';

import { forwardRef, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

const RASTER_DATA =
  /^data:image\/(png|jpe?g|gif|webp|avif|x-icon|vnd\.microsoft\.icon);base64,[a-z0-9+/=\s]+$/i;
const SCHEME = /^([a-z][a-z0-9+.-]*):/i;

/**
 * True when `src` is safe to load as a favicon: an `http:`/`https:` URL, a
 * relative path, or a base64 raster `data:image/…` URL. SVG data URLs,
 * `javascript:`, `blob:` and every other scheme are rejected.
 */
export function isSafeFaviconSrc(src: string | null | undefined): src is string {
  if (!src) return false;
  // eslint-disable-next-line no-control-regex -- control characters are exactly what is removed.
  const compact = src.trim().replace(/[\u0000- \u007f-\u009f]/g, '');
  if (!compact) return false;
  if (/^data:/i.test(compact)) return RASTER_DATA.test(src.trim());
  const scheme = SCHEME.exec(compact);
  if (!scheme) return true;
  const protocol = (scheme[1] ?? '').toLowerCase();
  return protocol === 'http' || protocol === 'https';
}

/**
 * The host name of a URL or bare domain, lower-cased and without `www.`:
 * `https://www.Example.com/a` → `example.com`. Returns `''` when there is none.
 */
export function faviconHostname(urlOrHost: string | null | undefined): string {
  const value = (urlOrHost ?? '').trim();
  if (!value) return '';
  try {
    const url = new URL(/^[a-z][a-z0-9+.-]*:\/\//i.test(value) ? value : `https://${value}`);
    return url.hostname.toLowerCase().replace(/^www\./, '');
  } catch {
    return '';
  }
}

/** The fallback letter: the first letter or digit of the name, upper-cased. */
export function faviconLetter(name: string): string {
  const match = /[\p{L}\p{N}]/u.exec(name);
  return match ? match[0].toLocaleUpperCase() : '?';
}

export interface FaviconProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * The site: a URL (`https://example.com/page`) or a bare domain
   * (`example.com`). Used for the fallback letter and passed to `resolveSrc`.
   */
  domain?: string | undefined;
  /** An explicit icon URL. Unsafe values (see `isSafeFaviconSrc`) are ignored and the letter shows. */
  src?: string | undefined;
  /**
   * Builds an icon URL from the host name when `src` is not given, e.g. your
   * own icon proxy. Monarch never calls a third-party favicon service by
   * itself. The result goes through the same safety check as `src`.
   */
  resolveSrc?: ((hostname: string, pixelSize: number) => string | null | undefined) | undefined;
  /** Rendered size in px. The image is requested at twice this size for sharp screens. Default 16. */
  size?: number;
  /**
   * Text used for the fallback letter instead of the domain, e.g. the site's
   * name. Default: the host name.
   */
  name?: string | undefined;
  /**
   * Accessible name. Without it the favicon is decorative (`alt=""`,
   * `aria-hidden`) because the site name is normally written next to it.
   */
  label?: string | undefined;
  /** Corner shape. Default `rounded`. */
  shape?: 'rounded' | 'circle' | 'square';
}

/**
 * A site's icon at a fixed size, with a letter tile when there is no safe
 * icon URL or the image fails to load. Decorative unless given a `label`.
 */
export const Favicon = forwardRef<HTMLSpanElement, FaviconProps>(function Favicon(
  { domain, src, resolveSrc, size = 16, name, label, shape = 'rounded', className, style, ...rest },
  ref,
) {
  const host = faviconHostname(domain);
  const candidate = src ?? (host && resolveSrc ? resolveSrc(host, size * 2) : undefined);
  const safeSrc = isSafeFaviconSrc(candidate) ? candidate : undefined;
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const imgRef = useRef<HTMLImageElement | null>(null);
  const failed = safeSrc !== undefined && failedSrc === safeSrc;

  // An image that failed before hydration never fires onError; check once mounted.
  useEffect(() => {
    const img = imgRef.current;
    if (img && safeSrc && img.complete && img.naturalWidth === 0 && img.currentSrc) {
      setFailedSrc(safeSrc);
    }
  }, [safeSrc]);

  const showImage = safeSrc !== undefined && !failed;
  const letter = faviconLetter(name ?? host ?? domain ?? '');

  return (
    <span
      ref={ref}
      {...rest}
      {...(label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true })}
      data-state={showImage ? 'image' : 'fallback'}
      data-shape={shape}
      className={cx('mn-favicon', className)}
      style={{ ...style, ['--mn-favicon-size' as string]: `${size}px` }}
    >
      {showImage ? (
        <img
          ref={imgRef}
          className="mn-favicon__image"
          src={safeSrc}
          alt=""
          width={size}
          height={size}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          onError={() => setFailedSrc(safeSrc)}
        />
      ) : (
        <span className="mn-favicon__letter">{letter}</span>
      )}
    </span>
  );
});
