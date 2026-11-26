'use client';

import { forwardRef, Fragment, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { MoreGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { useConfirm, type ConfirmOptions } from '../../overlays/confirm-dialog/index.js';

/** One item of a RowActionsMenu. The same shape as DataTable's `DataTableRowAction`. */
export interface RowAction {
  /** Stable id: the React key. */
  id: string;
  /** Visible text of the item. */
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode | undefined;
  /**
   * Runs the action. With `confirm`, it runs only after the person confirms,
   * inside the dialog: return a promise to show the pending state there, and
   * reject it to show the error.
   */
  onSelect: () => unknown;
  /** `danger` for destructive actions: red text, and grouped last after a separator. */
  tone?: 'default' | 'danger' | undefined;
  disabled?: boolean | undefined;
  /** Keyboard shortcut hint, e.g. "⌘D". Display only. */
  shortcut?: string | undefined;
  /**
   * Ask before running: a ConfirmDialog opens with these options and
   * `onSelect` runs only when the person confirms. Use it for destructive
   * actions that cannot be undone (see the destructive-actions guide).
   */
  confirm?: ConfirmOptions | undefined;
  /** Ignored by the menu (DataTable shows inline actions as buttons). */
  inline?: boolean | undefined;
}

export interface RowActionsMenuMessages {
  /** Trigger name with a row label: "Actions for Acme". */
  trigger: (rowLabel: string) => string;
  /** Trigger name without a row label. */
  triggerGeneric: string;
}

export const defaultRowActionsMenuMessages: RowActionsMenuMessages = {
  trigger: (row) => `Actions for ${row}`,
  triggerGeneric: 'Row actions',
};

export interface RowActionsMenuProps {
  /** The items. Danger items are moved to the end, after a separator. */
  actions: readonly RowAction[];
  /** Plain-text name of the row, for the trigger's accessible name ("Actions for Acme"). */
  rowLabel?: string | undefined;
  /** Trigger size. Default `sm`. */
  size?: 'xs' | 'sm' | 'md' | undefined;
  /** Menu alignment against the trigger. Default `end`. */
  align?: 'start' | 'center' | 'end' | undefined;
  /** Open state (controlled). */
  open?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Disables the trigger. */
  disabled?: boolean | undefined;
  messages?: Partial<RowActionsMenuMessages> | undefined;
  className?: string | undefined;
}

/**
 * The per-row "⋯" menu of a table, list or card: a labelled icon button that
 * opens a DropdownMenu of actions. Destructive items use the danger tone,
 * sit last after a separator, and can ask for confirmation before they run.
 */
export const RowActionsMenu = forwardRef<HTMLButtonElement, RowActionsMenuProps>(
  function RowActionsMenu(
    {
      actions,
      rowLabel,
      size = 'sm',
      align = 'end',
      open: openProp,
      onOpenChange,
      disabled = false,
      messages,
      className,
    },
    ref,
  ) {
    const m = { ...defaultRowActionsMenuMessages, ...messages };
    const { confirm, element } = useConfirm();
    const [innerOpen, setInnerOpen] = useState(false);
    const open = openProp ?? innerOpen;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setInnerOpen(next);
      onOpenChange?.(next);
    };
    const items = actions.filter((a) => !a.inline);
    if (!items.length) return null;
    const regular = items.filter((a) => a.tone !== 'danger');
    const danger = items.filter((a) => a.tone === 'danger');

    const run = (a: RowAction) => {
      if (!a.confirm) {
        a.onSelect();
        return;
      }
      // The action runs inside the dialog, so async work shows its pending and error states there.
      void confirm({
        ...(a.tone === 'danger' ? { tone: 'danger' as const } : {}),
        ...a.confirm,
        onConfirm: () => a.onSelect(),
      });
    };

    const item = (a: RowAction) => (
      <DropdownMenuItem
        key={a.id}
        icon={a.icon}
        tone={a.tone ?? 'default'}
        disabled={a.disabled}
        {...(a.shortcut ? { shortcut: a.shortcut } : {})}
        onSelect={() => run(a)}
      >
        {a.label}
      </DropdownMenuItem>
    );

    return (
      <>
        <DropdownMenu open={open} onOpenChange={setOpen}>
          <DropdownMenuTrigger asChild disabled={disabled}>
            <IconButton
              ref={ref}
              size={size}
              label={rowLabel ? m.trigger(rowLabel) : m.triggerGeneric}
              icon={<MoreGlyph />}
              className={cx('mn-row-actions-menu', className)}
              data-state={open ? 'open' : 'closed'}
            />
          </DropdownMenuTrigger>
          <DropdownMenuContent align={align} className="mn-row-actions-menu__content">
            {regular.map(item)}
            {regular.length && danger.length ? <DropdownMenuSeparator /> : null}
            {danger.map((a) => (
              <Fragment key={a.id}>{item(a)}</Fragment>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
        {element}
      </>
    );
  },
);
