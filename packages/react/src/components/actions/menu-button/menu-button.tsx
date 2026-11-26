'use client';

import { forwardRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button, type ButtonProps } from '../button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** One entry of the `items` shorthand. */
export interface MenuButtonItem {
  /** Stable key. Default: the label when it is a string, else the index. */
  id?: string;
  /** Visible text, and the accessible name. */
  label: ReactNode;
  /** Runs when the item is chosen with a click, Enter or Space. */
  onSelect?: ((event: Event) => void) | undefined;
  /** Renders the item as a link. Unsafe URLs (for example `javascript:`) are dropped. */
  href?: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** `danger` for destructive actions; pair them with a confirmation. */
  tone?: 'default' | 'danger';
  /** Keyboard shortcut hint, e.g. "⌘D". Display only. */
  shortcut?: string;
  disabled?: boolean;
}

/** An `items` entry: an item, or `'separator'` for a divider. */
export type MenuButtonEntry = MenuButtonItem | 'separator';

export interface MenuButtonItemsProps {
  /** The entries to render as DropdownMenu items. */
  items: readonly MenuButtonEntry[];
}

/**
 * Renders a `MenuButtonEntry[]` as DropdownMenu items and separators. Used
 * by MenuButton and SplitButton; use it inside your own `DropdownMenuContent`
 * to share one list of actions between a menu and other places.
 */
export function MenuButtonItems({ items }: MenuButtonItemsProps) {
  return (
    <>
      {items.map((entry, index) => {
        if (entry === 'separator') return <DropdownMenuSeparator key={`separator-${index}`} />;
        const key = entry.id ?? (typeof entry.label === 'string' ? entry.label : String(index));
        // A disabled entry is not a link: an anchor would still navigate when activated.
        const safeHref =
          !entry.disabled && entry.href && isSafeUrl(entry.href) ? entry.href : undefined;
        const common = {
          ...(entry.disabled !== undefined ? { disabled: entry.disabled } : {}),
          ...(entry.onSelect ? { onSelect: entry.onSelect } : {}),
        };
        if (safeHref) {
          return (
            <DropdownMenuItem key={key} asChild tone={entry.tone ?? 'default'} {...common}>
              <a href={safeHref}>
                {entry.icon ? (
                  <span className="mn-menu__icon" aria-hidden>
                    {entry.icon}
                  </span>
                ) : null}
                <span className="mn-menu__text">{entry.label}</span>
              </a>
            </DropdownMenuItem>
          );
        }
        return (
          <DropdownMenuItem
            key={key}
            tone={entry.tone ?? 'default'}
            icon={entry.icon}
            {...(entry.shortcut ? { shortcut: entry.shortcut } : {})}
            {...common}
          >
            {entry.label}
          </DropdownMenuItem>
        );
      })}
    </>
  );
}

export interface MenuButtonProps extends Omit<ButtonProps, 'children' | 'asChild'> {
  /** Visible label of the button, and its accessible name. */
  label: ReactNode;
  /**
   * Hide the label and show only `icon`. The label becomes the accessible
   * name (`aria-label`), so it must be a string here.
   */
  iconOnly?: boolean;
  /** Icon for the icon-only form, or a leading icon otherwise. Decorative. */
  icon?: ReactNode;
  /** Hide the chevron after the label. Default `false`. */
  hideChevron?: boolean;
  /** The menu's actions as data. Rendered before `children`. */
  items?: readonly MenuButtonEntry[];
  /** Menu content: DropdownMenuItem, DropdownMenuCheckboxItem, DropdownMenuSeparator, … */
  children?: ReactNode;
  /** Controlled open state. */
  open?: boolean;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Horizontal alignment of the menu to the button. Default `start`. */
  align?: 'start' | 'center' | 'end';
  /** Side of the button the menu opens on. Default `bottom`. */
  side?: 'top' | 'right' | 'bottom' | 'left';
  /** Class name for the menu panel. */
  menuClassName?: string;
  /** Lets the page behind stay interactive while the menu is open. Default `true` (Radix default). */
  modal?: boolean;
}

/**
 * A button that opens a menu of actions, following the WAI-ARIA menu button
 * pattern: `aria-haspopup="menu"` and `aria-expanded` on the button; Enter,
 * Space or Arrow Down opens the menu with focus on its first item; Escape
 * closes it and returns focus to the button. Pass actions as `items`
 * or as DropdownMenu children.
 */
export const MenuButton = forwardRef<HTMLButtonElement, MenuButtonProps>(function MenuButton(
  {
    label,
    iconOnly = false,
    icon,
    hideChevron = false,
    items,
    children,
    open,
    defaultOpen,
    onOpenChange,
    align = 'start',
    side = 'bottom',
    menuClassName,
    modal,
    variant = iconOnly ? 'ghost' : 'secondary',
    leadingIcon,
    className,
    ...rest
  },
  ref,
) {
  const root = {
    ...(open !== undefined ? { open } : {}),
    ...(defaultOpen !== undefined ? { defaultOpen } : {}),
    ...(onOpenChange ? { onOpenChange } : {}),
    ...(modal !== undefined ? { modal } : {}),
  };
  // Radix opens on pointer down and key down; a disabled or loading button
  // must not open the menu (loading stays focusable, so it is not `disabled`).
  const blocked = Boolean(rest.disabled || rest.loading);
  const block = (event: { preventDefault: () => void }) => {
    if (blocked) event.preventDefault();
  };
  return (
    <DropdownMenu {...root}>
      <DropdownMenuTrigger asChild onPointerDown={block} onKeyDown={block}>
        <Button
          ref={ref}
          {...rest}
          variant={variant}
          leadingIcon={iconOnly ? undefined : (leadingIcon ?? icon)}
          trailingIcon={iconOnly || hideChevron ? undefined : <ChevronDownGlyph />}
          aria-label={iconOnly && typeof label === 'string' ? label : rest['aria-label']}
          title={rest.title ?? (iconOnly && typeof label === 'string' ? label : undefined)}
          data-icon-only={iconOnly || undefined}
          className={cx('mn-menu-button', iconOnly && 'mn-icon-button', className)}
        >
          {iconOnly ? (
            <span className="mn-button__icon" aria-hidden>
              {icon}
            </span>
          ) : (
            label
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align={align} side={side} className={menuClassName}>
        {items ? <MenuButtonItems items={items} /> : null}
        {children}
      </DropdownMenuContent>
    </DropdownMenu>
  );
});
