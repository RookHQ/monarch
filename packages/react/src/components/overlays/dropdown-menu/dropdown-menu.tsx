'use client';

import {
  Children,
  forwardRef,
  isValidElement,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type ReactNode,
} from 'react';
import * as Menu from '@radix-ui/react-dropdown-menu';
import { cx } from '../../../internal/cx.js';
import { warnOnce } from '../../../internal/dev-warn.js';
import { CheckGlyph, ChevronRightGlyph, DotGlyph } from '../../../internal/icons.js';

/** Action menu. Arrow keys move, typeahead jumps, Escape closes and restores focus. */
export const DropdownMenu = Menu.Root;
export const DropdownMenuTrigger = Menu.Trigger;
export const DropdownMenuGroup = Menu.Group;
export const DropdownMenuRadioGroup = Menu.RadioGroup;
export const DropdownMenuSub = Menu.Sub;

export const DropdownMenuContent = forwardRef<
  ElementRef<typeof Menu.Content>,
  ComponentPropsWithoutRef<typeof Menu.Content>
>(function DropdownMenuContent({ className, sideOffset = 4, align = 'start', ...rest }, ref) {
  return (
    <Menu.Portal>
      <Menu.Content
        ref={ref}
        sideOffset={sideOffset}
        align={align}
        {...rest}
        className={cx('mn-menu', className)}
      />
    </Menu.Portal>
  );
});

export interface DropdownMenuItemProps extends ComponentPropsWithoutRef<typeof Menu.Item> {
  /** `danger` for destructive actions. Pair with a confirmation step. */
  tone?: 'default' | 'danger';
  icon?: ReactNode;
  /** Keyboard shortcut hint, e.g. "⌘D". Display only. */
  shortcut?: string;
}

const BLOCK_TAGS = new Set(['div', 'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'table']);

function hasBlockChild(children: ReactNode): boolean {
  return Children.toArray(children).some(
    (c) => isValidElement(c) && typeof c.type === 'string' && BLOCK_TAGS.has(c.type),
  );
}

export const DropdownMenuItem = forwardRef<ElementRef<typeof Menu.Item>, DropdownMenuItemProps>(
  function DropdownMenuItem(
    { tone = 'default', icon, shortcut, className, children, ...rest },
    ref,
  ) {
    if (!rest.asChild && hasBlockChild(children)) {
      warnOnce(
        'DropdownMenuItem children are wrapped in one text span, so <div>, <p> and headings inside stack on separate lines. Put an avatar or icon in the `icon` prop and use <span> for the text, or use `asChild`.',
      );
    }
    if (rest.asChild) {
      // The child (e.g. a link) becomes the menu item; icon and shortcut do not apply.
      return (
        <Menu.Item ref={ref} {...rest} data-tone={tone} className={cx('mn-menu__item', className)}>
          {children}
        </Menu.Item>
      );
    }
    return (
      <Menu.Item ref={ref} {...rest} data-tone={tone} className={cx('mn-menu__item', className)}>
        {icon ? (
          <span className="mn-menu__icon" aria-hidden>
            {icon}
          </span>
        ) : null}
        <span className="mn-menu__text">{children}</span>
        {shortcut ? (
          <span className="mn-menu__shortcut" aria-hidden>
            {shortcut}
          </span>
        ) : null}
      </Menu.Item>
    );
  },
);

export const DropdownMenuCheckboxItem = forwardRef<
  ElementRef<typeof Menu.CheckboxItem>,
  ComponentPropsWithoutRef<typeof Menu.CheckboxItem>
>(function DropdownMenuCheckboxItem({ className, children, ...rest }, ref) {
  return (
    <Menu.CheckboxItem ref={ref} {...rest} className={cx('mn-menu__item', className)}>
      <span className="mn-menu__text">{children}</span>
      <Menu.ItemIndicator className="mn-menu__indicator">
        <CheckGlyph />
      </Menu.ItemIndicator>
    </Menu.CheckboxItem>
  );
});

export const DropdownMenuRadioItem = forwardRef<
  ElementRef<typeof Menu.RadioItem>,
  ComponentPropsWithoutRef<typeof Menu.RadioItem>
>(function DropdownMenuRadioItem({ className, children, ...rest }, ref) {
  return (
    <Menu.RadioItem ref={ref} {...rest} className={cx('mn-menu__item', className)}>
      <span className="mn-menu__text">{children}</span>
      <Menu.ItemIndicator className="mn-menu__indicator">
        <DotGlyph />
      </Menu.ItemIndicator>
    </Menu.RadioItem>
  );
});

export const DropdownMenuLabel = forwardRef<
  ElementRef<typeof Menu.Label>,
  ComponentPropsWithoutRef<typeof Menu.Label>
>(function DropdownMenuLabel({ className, ...rest }, ref) {
  return <Menu.Label ref={ref} {...rest} className={cx('mn-menu__label', className)} />;
});

export const DropdownMenuSeparator = forwardRef<
  ElementRef<typeof Menu.Separator>,
  ComponentPropsWithoutRef<typeof Menu.Separator>
>(function DropdownMenuSeparator({ className, ...rest }, ref) {
  return <Menu.Separator ref={ref} {...rest} className={cx('mn-menu__separator', className)} />;
});

export const DropdownMenuSubTrigger = forwardRef<
  ElementRef<typeof Menu.SubTrigger>,
  ComponentPropsWithoutRef<typeof Menu.SubTrigger>
>(function DropdownMenuSubTrigger({ className, children, ...rest }, ref) {
  return (
    <Menu.SubTrigger ref={ref} {...rest} className={cx('mn-menu__item', className)}>
      <span className="mn-menu__text">{children}</span>
      <ChevronRightGlyph className="mn-menu__indicator" />
    </Menu.SubTrigger>
  );
});

export const DropdownMenuSubContent = forwardRef<
  ElementRef<typeof Menu.SubContent>,
  ComponentPropsWithoutRef<typeof Menu.SubContent>
>(function DropdownMenuSubContent({ className, ...rest }, ref) {
  return (
    <Menu.Portal>
      <Menu.SubContent ref={ref} {...rest} className={cx('mn-menu', className)} />
    </Menu.Portal>
  );
});
