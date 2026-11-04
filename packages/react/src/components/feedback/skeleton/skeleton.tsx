import type { CSSProperties, HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {
  shape?: 'text' | 'rect' | 'circle';
  width?: CSSProperties['width'];
  height?: CSSProperties['height'];
}

/**
 * Loading placeholder. Hidden from assistive technology: mark the loading
 * region with aria-busy="true" and announce completion separately.
 */
export function Skeleton({
  shape = 'rect',
  width,
  height,
  className,
  style,
  ...rest
}: SkeletonProps) {
  return (
    <div
      {...rest}
      aria-hidden
      className={cx('mn-skeleton', className)}
      data-shape={shape}
      style={{ width, height, ...style }}
    />
  );
}
