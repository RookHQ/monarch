import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { inertProps } from '../../../internal/inert.js';

export interface ProductFrameProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /**
   * Accessible description of the illustration, e.g. "Dossier editor with an
   * outline and the Assistant panel". Required: the frame is announced as one image.
   */
  label: string;
  /** Text in the chrome bar: a document title, or a URL with `chrome="browser"`. */
  title?: string;
  /** `window` (app window dots), `browser` (address bar) or `none`. */
  chrome?: 'window' | 'browser' | 'none';
  /** Visible caption under the frame. */
  caption?: ReactNode;
  /** A soft accent panel behind the frame, for hero moments. */
  backdrop?: boolean;
  /** The illustrated UI. Build it from Monarch components so it stays on-brand. */
  children: ReactNode;
}

/**
 * Neutral frame for product illustrations. The contents are presentational:
 * one `role="img"` with `label`, and `inert`, so sample headings and inputs are
 * neither read out nor focusable. Server-component safe.
 */
export function ProductFrame({
  label,
  title,
  chrome = 'window',
  caption,
  backdrop = false,
  children,
  className,
  ...rest
}: ProductFrameProps) {
  return (
    <figure
      {...rest}
      className={cx('mn-product-frame', className)}
      data-backdrop={backdrop || undefined}
    >
      <div className="mn-product-frame__window" role="img" aria-label={label} data-chrome={chrome}>
        {chrome !== 'none' ? (
          <div className="mn-product-frame__chrome" aria-hidden>
            <span className="mn-product-frame__dots">
              <i />
              <i />
              <i />
            </span>
            {title ? (
              <span
                className={
                  chrome === 'browser' ? 'mn-product-frame__url' : 'mn-product-frame__title'
                }
              >
                {title}
              </span>
            ) : null}
            <span className="mn-product-frame__spacer" />
          </div>
        ) : null}
        <div className="mn-product-frame__body" {...inertProps()}>
          {children}
        </div>
      </div>
      {caption ? <figcaption className="mn-product-frame__caption">{caption}</figcaption> : null}
    </figure>
  );
}
