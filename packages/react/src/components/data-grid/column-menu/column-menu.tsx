'use client';

import { forwardRef, useMemo } from 'react';
import { ColumnsGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import type { DataTableColumnChooserColumn } from '../data-table/index.js';

export interface ColumnMenuMessages {
  /** Trigger button text. */
  trigger: string;
  /** Heading inside the menu. */
  title: string;
  /** Hidden part of the trigger's name: "5 of 8 shown". */
  shownCount: (shown: number, total: number) => string;
  showAll: string;
  reset: string;
}

export const defaultColumnMenuMessages: ColumnMenuMessages = {
  trigger: 'Columns',
  title: 'Visible columns',
  shownCount: (shown, total) => `${shown} of ${total} shown`,
  showAll: 'Show all columns',
  reset: 'Reset columns',
};

export interface ColumnMenuProps {
  /**
   * The columns, in the shape DataTable's chooser uses (`{ id, label,
   * hideable }`). Build them with `columnChooserColumns(columns)` or spread
   * `useDataTableColumns(columns).chooserProps`.
   */
  columns: readonly DataTableColumnChooserColumn[];
  /** Visible columns by id; missing ids count as visible. */
  visibility: Readonly<Record<string, boolean>>;
  /** Called with the whole new visibility record. */
  onVisibilityChange: (visibility: Record<string, boolean>) => void;
  /** Column ids in display order, so the menu lists them as the table shows them. */
  order?: readonly string[] | undefined;
  /** Adds a "Reset columns" item. */
  onReset?: (() => void) | undefined;
  /** Adds a "Show all columns" item. Default false. */
  showAll?: boolean;
  /**
   * The fewest columns that must stay visible: the last ones cannot be
   * unchecked. Default 1, so the table is never empty.
   */
  minVisible?: number;
  /** Trigger size. Default `sm`. */
  size?: 'xs' | 'sm' | 'md';
  /** Menu alignment against the trigger. Default `end`. */
  align?: 'start' | 'center' | 'end';
  messages?: Partial<ColumnMenuMessages>;
  className?: string;
}

/**
 * A "Columns" dropdown with one checkable item per column, to show and hide
 * table columns quickly. Takes the same state as DataTableColumnChooser, so
 * it plugs straight into DataTable; use ColumnChooser when people also need
 * to reorder or search columns.
 */
export const ColumnMenu = forwardRef<HTMLButtonElement, ColumnMenuProps>(function ColumnMenu(
  {
    columns,
    visibility,
    onVisibilityChange,
    order,
    onReset,
    showAll = false,
    minVisible = 1,
    size = 'sm',
    align = 'end',
    messages,
    className,
  },
  ref,
) {
  const m = { ...defaultColumnMenuMessages, ...messages };
  const ordered = useMemo(() => orderColumns(columns, order), [columns, order]);
  const isShown = (c: DataTableColumnChooserColumn) =>
    c.hideable === false ? true : (visibility[c.id] ?? true);
  const shown = columns.filter(isShown).length;
  const allShown = shown === columns.length;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          ref={ref}
          size={size}
          variant="secondary"
          className={className}
          leadingIcon={<ColumnsGlyph />}
        >
          {m.trigger}
          <span className="mn-visually-hidden">, {m.shownCount(shown, columns.length)}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align={align} className="mn-column-menu">
        <DropdownMenuLabel>{m.title}</DropdownMenuLabel>
        {ordered.map((c) => {
          const checked = isShown(c);
          const locked = c.hideable === false || (checked && shown <= minVisible);
          return (
            <DropdownMenuCheckboxItem
              key={c.id}
              checked={checked}
              disabled={locked}
              className="mn-column-menu__item"
              onSelect={(e) => e.preventDefault()}
              onCheckedChange={(on) => onVisibilityChange({ ...visibility, [c.id]: on === true })}
            >
              {c.label}
            </DropdownMenuCheckboxItem>
          );
        })}
        {(showAll && !allShown) || onReset ? <DropdownMenuSeparator /> : null}
        {showAll && !allShown ? (
          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault();
              const next: Record<string, boolean> = { ...visibility };
              for (const c of columns) next[c.id] = true;
              onVisibilityChange(next);
            }}
          >
            {m.showAll}
          </DropdownMenuItem>
        ) : null}
        {onReset ? <DropdownMenuItem onSelect={() => onReset()}>{m.reset}</DropdownMenuItem> : null}
      </DropdownMenuContent>
    </DropdownMenu>
  );
});

/** Columns sorted by `order`; unknown ids are ignored and missing ones keep their place at the end. */
export function orderColumns<C extends { id: string }>(
  columns: readonly C[],
  order: readonly string[] | undefined,
): C[] {
  if (!order?.length) return columns.slice();
  const pos = new Map(order.map((id, i) => [id, i]));
  return columns
    .map((c, i) => ({ c, i }))
    .sort(
      (a, b) => (pos.get(a.c.id) ?? order.length + a.i) - (pos.get(b.c.id) ?? order.length + b.i),
    )
    .map((x) => x.c);
}
