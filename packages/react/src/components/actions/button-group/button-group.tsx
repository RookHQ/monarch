import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface ButtonGroupProps extends Omit<HTMLAttributes<HTMLDivElement>, 'role'> {
  /** Lay the buttons out in a row or a column. Default `horizontal`. */
  orientation?: 'horizontal' | 'vertical';
  /**
   * Join the buttons into one control with shared borders and squared inner
   * corners. With `false` they keep a small gap. Default `true`.
   */
  attached?: boolean;
  /** Stretch to the container width and share it equally between the buttons. */
  block?: boolean;
}

/**
 * Visually joins related buttons, such as Previous / Next or a set of zoom
 * controls, and exposes them as a `role="group"`. Give it an `aria-label`
 * when the buttons need context ("Zoom"). It does not add keyboard handling:
 * every button stays its own Tab stop. Use ToggleGroup for pressed states
 * and Toolbar for arrow-key navigation.
 */
export const ButtonGroup = forwardRef<HTMLDivElement, ButtonGroupProps>(function ButtonGroup(
  { orientation = 'horizontal', attached = true, block = false, className, ...rest },
  ref,
) {
  return (
    <div
      ref={ref}
      role="group"
      {...rest}
      data-orientation={orientation}
      data-attached={attached || undefined}
      data-block={block || undefined}
      className={cx('mn-button-group', className)}
    />
  );
});
