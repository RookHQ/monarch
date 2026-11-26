'use client';

import { forwardRef, type CSSProperties, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { Button, type ButtonProps } from '../button/index.js';
import { MenuButtonItems, type MenuButtonEntry } from '../menu-button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** Every visible or announced string of SplitButton, for translation. */
export interface SplitButtonMessages {
  /**
   * Accessible name of the arrow button. A function receives the main
   * action's label when it is a string. Default: "More options" or
   * "More options for <label>".
   */
  moreOptions: string | ((label: string | undefined) => string);
}

const DEFAULT_MESSAGES: SplitButtonMessages = {
  moreOptions: (label) => (label ? `More options for ${label}` : 'More options'),
};

export interface SplitButtonProps extends Omit<
  ButtonProps,
  'children' | 'asChild' | 'block' | 'trailingIcon'
> {
  /** The main action's label. */
  children: ReactNode;
  /** The alternative actions as data. Rendered before `menuContent`. */
  items?: readonly MenuButtonEntry[];
  /** Menu content as DropdownMenu children (DropdownMenuItem, …). */
  menuContent?: ReactNode;
  /** Controlled open state of the menu. */
  open?: boolean;
  /** Initial open state of the menu when uncontrolled. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Horizontal alignment of the menu. Default `end`, under the arrow. */
  align?: 'start' | 'center' | 'end';
  /** Disable only the menu, keeping the main action available. */
  menuDisabled?: boolean;
  /** Replaces any of the default strings. */
  messages?: Partial<SplitButtonMessages>;
  /** Class name of the wrapper `div` (`className` goes to the wrapper too). */
  className?: string;
  /** Style of the wrapper `div`. */
  style?: CSSProperties;
}

/**
 * A main action joined to an arrow button that opens a menu of related
 * alternatives, such as "Save" with "Save as draft" and "Save and close".
 * The main button acts at once; the arrow is a WAI-ARIA menu button with its
 * own Tab stop and accessible name. `className` and `style` apply to the
 * wrapper; every other Button prop and the ref go to the main button.
 */
export const SplitButton = forwardRef<HTMLButtonElement, SplitButtonProps>(function SplitButton(
  {
    children,
    items,
    menuContent,
    open,
    defaultOpen,
    onOpenChange,
    align = 'end',
    menuDisabled = false,
    messages: messagesProp,
    variant = 'secondary',
    size = 'md',
    disabled,
    loading,
    className,
    style,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const plain = typeof children === 'string' ? children : undefined;
  const moreLabel =
    typeof messages.moreOptions === 'function' ? messages.moreOptions(plain) : messages.moreOptions;
  const arrowBlocked = Boolean(disabled || loading || menuDisabled);
  const block = (event: { preventDefault: () => void }) => {
    if (arrowBlocked) event.preventDefault();
  };
  const root = {
    ...(open !== undefined ? { open } : {}),
    ...(defaultOpen !== undefined ? { defaultOpen } : {}),
    ...(onOpenChange ? { onOpenChange } : {}),
  };
  return (
    <div
      role="group"
      aria-label={plain}
      className={cx('mn-split-button', className)}
      style={style}
      data-variant={variant}
      data-size={size}
    >
      <Button
        ref={ref}
        {...rest}
        variant={variant}
        size={size}
        {...(disabled !== undefined ? { disabled } : {})}
        {...(loading !== undefined ? { loading } : {})}
        className="mn-split-button__main"
      >
        {children}
      </Button>
      <DropdownMenu {...root}>
        <DropdownMenuTrigger asChild onPointerDown={block} onKeyDown={block}>
          <Button
            variant={variant}
            size={size}
            disabled={disabled || menuDisabled}
            aria-label={moreLabel}
            title={moreLabel}
            className="mn-split-button__toggle mn-icon-button"
          >
            <span className="mn-button__icon" aria-hidden>
              <ChevronDownGlyph />
            </span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align={align}>
          {items ? <MenuButtonItems items={items} /> : null}
          {menuContent}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
});
