'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { SearchInput } from '../../search/search-input/index.js';
import type { DataTableColumnChooserColumn } from '../data-table/index.js';

export interface ColumnChooserMessages {
  title: string;
  /** Count under the title: "5 of 8 shown". */
  shownCount: (shown: number, total: number) => string;
  /** Accessible name of the search box. */
  search: string;
  /** Shown when the search matches nothing. */
  noMatches: string;
  moveUp: (column: string) => string;
  moveDown: (column: string) => string;
  /** Announced after a move: "Status moved to position 3 of 8". */
  moved: (column: string, position: number, total: number) => string;
  /** Hint on columns that cannot be hidden. */
  required: string;
  showAll: string;
  reset: string;
}

export const defaultColumnChooserPanelMessages: ColumnChooserMessages = {
  title: 'Columns',
  shownCount: (shown, total) => `${shown} of ${total} shown`,
  search: 'Find a column',
  noMatches: 'No columns match.',
  moveUp: (c) => `Move ${c} up`,
  moveDown: (c) => `Move ${c} down`,
  moved: (c, position, total) => `${c} moved to position ${position} of ${total}.`,
  required: 'Always shown',
  showAll: 'Show all',
  reset: 'Reset',
};

export interface ColumnChooserProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /**
   * The columns (`{ id, label, hideable }`), as DataTable's chooser takes them:
   * build them with `columnChooserColumns(columns)` or spread
   * `useDataTableColumns(columns).chooserProps`.
   */
  columns: readonly DataTableColumnChooserColumn[];
  /** Visible columns by id; missing ids count as visible. */
  visibility: Readonly<Record<string, boolean>>;
  /** Called with the whole new visibility record. */
  onVisibilityChange: (visibility: Record<string, boolean>) => void;
  /** Column ids in display order. */
  order?: readonly string[] | undefined;
  /** With it, every row gets Move up and Move down buttons. */
  onOrderChange?: ((order: string[]) => void) | undefined;
  /** Adds a Reset button. */
  onReset?: (() => void) | undefined;
  /** Show a search box when there are at least this many columns. Default 8; `Infinity` never. */
  searchThreshold?: number;
  /** The fewest columns that must stay visible. Default 1. */
  minVisible?: number;
  /** Heading element for the title. Default `h3`; `p` where a heading is wrong. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'p';
  messages?: Partial<ColumnChooserMessages>;
}

function orderOf(
  columns: readonly DataTableColumnChooserColumn[],
  order: readonly string[] | undefined,
) {
  if (!order?.length) return columns.slice();
  const pos = new Map(order.map((id, i) => [id, i]));
  return columns
    .map((c, i) => ({ c, i }))
    .sort(
      (a, b) => (pos.get(a.c.id) ?? order.length + a.i) - (pos.get(b.c.id) ?? order.length + b.i),
    )
    .map((x) => x.c);
}

/**
 * An inline panel to show, hide, find and reorder columns, for a Sheet,
 * a settings page or a saved-view editor. Takes the same state as
 * DataTableColumnChooser, so it works with DataTable. Reordering uses
 * buttons, so it needs no drag and drop.
 */
export const ColumnChooser = forwardRef<HTMLDivElement, ColumnChooserProps>(function ColumnChooser(
  {
    columns,
    visibility,
    onVisibilityChange,
    order,
    onOrderChange,
    onReset,
    searchThreshold = 8,
    minVisible = 1,
    headingLevel = 'h3',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultColumnChooserPanelMessages, ...messages };
  const id = useId();
  const [query, setQuery] = useState('');
  const [announcement, setAnnouncement] = useState('');
  const ordered = useMemo(() => orderOf(columns, order), [columns, order]);
  const isShown = (c: DataTableColumnChooserColumn) =>
    c.hideable === false ? true : (visibility[c.id] ?? true);
  const shown = columns.filter(isShown).length;
  const searchable = columns.length >= searchThreshold;
  const q = query.trim().toLocaleLowerCase();
  const filtering = searchable && q !== '';
  const visible = filtering
    ? ordered.filter((c) => c.label.toLocaleLowerCase().includes(q))
    : ordered;
  const canMove = Boolean(onOrderChange) && !filtering;

  const buttons = useRef(new Map<string, HTMLButtonElement | null>());
  const refocus = useRef<string | null>(null);
  useEffect(() => {
    if (!refocus.current) return;
    const el = buttons.current.get(refocus.current);
    refocus.current = null;
    if (el && !el.disabled) el.focus();
  });

  const move = (column: DataTableColumnChooserColumn, delta: -1 | 1) => {
    const ids = ordered.map((c) => c.id);
    const i = ids.indexOf(column.id);
    const j = i + delta;
    if (i < 0 || j < 0 || j >= ids.length) return;
    [ids[i], ids[j]] = [ids[j] as string, ids[i] as string];
    const atEdge = j === 0 || j === ids.length - 1;
    // At the first or last position the pressed button disables; keep focus on its partner.
    refocus.current = `${column.id}:${atEdge ? -delta : delta}`;
    setAnnouncement(m.moved(column.label, j + 1, ids.length));
    onOrderChange?.(ids);
  };

  const Heading = headingLevel;
  return (
    <div
      ref={ref}
      role="group"
      aria-labelledby={`${id}-title`}
      aria-describedby={`${id}-count`}
      {...rest}
      className={cx('mn-column-chooser', className)}
    >
      <div className="mn-column-chooser__header">
        <Heading className="mn-column-chooser__title" id={`${id}-title`}>
          {m.title}
        </Heading>
        <span className="mn-column-chooser__count" id={`${id}-count`}>
          {m.shownCount(shown, columns.length)}
        </span>
      </div>
      {searchable ? (
        <SearchInput
          size="sm"
          aria-label={m.search}
          placeholder={m.search}
          value={query}
          onValueChange={setQuery}
          className="mn-column-chooser__search"
        />
      ) : null}
      {visible.length ? (
        <ul className="mn-column-chooser__list">
          {visible.map((c) => {
            const checked = isShown(c);
            const required = c.hideable === false;
            const index = ordered.indexOf(c);
            return (
              <li
                key={c.id}
                className="mn-column-chooser__item"
                data-hidden={checked ? undefined : ''}
              >
                <Checkbox
                  label={c.label}
                  {...(required ? { description: m.required } : {})}
                  checked={checked}
                  disabled={required || (checked && shown <= minVisible)}
                  onCheckedChange={(on) =>
                    onVisibilityChange({ ...visibility, [c.id]: on === true })
                  }
                />
                {canMove ? (
                  <span className="mn-column-chooser__move">
                    <IconButton
                      ref={(el) => {
                        buttons.current.set(`${c.id}:-1`, el);
                      }}
                      size="xs"
                      variant="ghost"
                      label={m.moveUp(c.label)}
                      disabled={index === 0}
                      icon={<ChevronDownGlyph className="mn-column-chooser__up" />}
                      onClick={() => move(c, -1)}
                    />
                    <IconButton
                      ref={(el) => {
                        buttons.current.set(`${c.id}:1`, el);
                      }}
                      size="xs"
                      variant="ghost"
                      label={m.moveDown(c.label)}
                      disabled={index === ordered.length - 1}
                      icon={<ChevronDownGlyph />}
                      onClick={() => move(c, 1)}
                    />
                  </span>
                ) : null}
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="mn-column-chooser__empty">{m.noMatches}</p>
      )}
      {shown < columns.length || onReset ? (
        <div className="mn-column-chooser__footer">
          {shown < columns.length ? (
            <Button
              size="xs"
              variant="ghost"
              onClick={() => {
                const next: Record<string, boolean> = { ...visibility };
                for (const c of columns) next[c.id] = true;
                onVisibilityChange(next);
              }}
            >
              {m.showAll}
            </Button>
          ) : null}
          {onReset ? (
            <Button size="xs" variant="ghost" onClick={onReset}>
              {m.reset}
            </Button>
          ) : null}
        </div>
      ) : null}
      <span role="status" aria-live="polite" aria-atomic="true" className="mn-visually-hidden">
        {announcement}
      </span>
    </div>
  );
});
