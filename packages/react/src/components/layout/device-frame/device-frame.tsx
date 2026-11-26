import { forwardRef, type CSSProperties, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { inertProps } from '../../../internal/inert.js';

export interface DeviceFrameProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /**
   * What the frame shows, e.g. "Preview of your link page on a phone".
   * Required: it names the preview as an image, or as a group when `interactive`.
   */
  label: string;
  /** `phone` (default, rounded with a camera island) or `tablet` (wider bezel, no island). */
  device?: 'phone' | 'tablet' | undefined;
  /** Screen width in CSS pixels; the frame never grows past it. Default 390 (phone) or 820 (tablet). */
  width?: number | undefined;
  /** Screen height in CSS pixels, used for the aspect ratio. Default 844 (phone) or 1180 (tablet). */
  height?: number | undefined;
  /**
   * Let people scroll and use the contents (a live preview). Default false: the
   * contents are presentational, read as one image and not focusable.
   */
  interactive?: boolean | undefined;
  /** Show a status bar with this time text (decorative), e.g. "9:41". */
  statusBarTime?: string | undefined;
  /** Hide the side buttons and shadow, for dense layouts. Default false. */
  flat?: boolean | undefined;
  /** The screen contents. */
  children?: ReactNode;
}

const SIZES = {
  phone: { width: 390, height: 844 },
  tablet: { width: 820, height: 1180 },
} as const;

/**
 * A phone or tablet outline around a preview: a link page, an email, a
 * mobile layout. Decorative chrome with an accessible name; the contents are
 * either one presentational image (default) or, with `interactive`, a
 * scrollable, focusable region. It scales down to its container and keeps the
 * device's aspect ratio. Server-component safe.
 */
export const DeviceFrame = forwardRef<HTMLDivElement, DeviceFrameProps>(function DeviceFrame(
  {
    label,
    device = 'phone',
    width,
    height,
    interactive = false,
    statusBarTime,
    flat = false,
    children,
    className,
    style,
    ...rest
  },
  ref,
) {
  const w = width ?? SIZES[device].width;
  const h = height ?? SIZES[device].height;
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-device-frame', className)}
      data-device={device}
      data-interactive={interactive ? '' : undefined}
      data-flat={flat ? '' : undefined}
      style={{ maxWidth: `${w}px`, aspectRatio: `${w} / ${h}`, ...style } as CSSProperties}
      {...(interactive
        ? { role: 'group', 'aria-label': label }
        : { role: 'img', 'aria-label': label })}
    >
      {flat ? null : (
        <span className="mn-device-frame__buttons" aria-hidden>
          <i />
          <i />
          <i />
          <i />
        </span>
      )}
      <div className="mn-device-frame__bezel">
        <div className="mn-device-frame__screen">
          {device === 'phone' ? <span className="mn-device-frame__island" aria-hidden /> : null}
          {statusBarTime ? (
            <div className="mn-device-frame__status" aria-hidden>
              <span>{statusBarTime}</span>
              <span className="mn-device-frame__signal">
                <i />
                <i />
                <i />
              </span>
            </div>
          ) : null}
          <div
            className="mn-device-frame__content"
            {...(interactive ? { tabIndex: 0 } : inertProps())}
          >
            {children}
          </div>
        </div>
      </div>
    </div>
  );
});
