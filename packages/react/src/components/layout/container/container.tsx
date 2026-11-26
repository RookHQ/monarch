import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import type { LayoutElement } from '../stack/index.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-container';

export interface ContainerProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `div`; use `main` or `section` for landmarks. */
  as?: LayoutElement | undefined;
  /**
   * Maximum content width: `narrow` 44rem (reading, forms), `content` 72rem
   * (default, app pages), `wide` 80rem (dashboards, tables), `full` none.
   */
  size?: 'narrow' | 'content' | 'wide' | 'full' | undefined;
  /**
   * Inline padding: 16px on phones, 24px from 768px (`--mn-layout-gutter`,
   * `--mn-layout-gutter-lg`). Default true; turn it off when a parent already pads.
   */
  gutter?: boolean | undefined;
}

/**
 * Centres page content at a maximum width with responsive side gutters, 16px
 * on phones. Use it once per page section instead of ad-hoc
 * `container mx-auto px-4` wrappers.
 */
export const Container = forwardRef<HTMLElement, ContainerProps>(function Container(
  { as: Tag = 'div', size = 'content', gutter = true, className, ...rest },
  ref,
) {
  return (
    <Tag
      ref={ref as never}
      {...rest}
      className={cx(ROOT, className)}
      data-size={size}
      data-gutter={gutter ? '' : undefined}
    />
  );
});
