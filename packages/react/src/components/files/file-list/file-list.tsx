'use client';

import {
  forwardRef,
  useCallback,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { FileCard, type FileCardFile, type FileCardMessages } from '../file-card/index.js';

/** One file in a FileList. */
export interface FileListItem extends FileCardFile {
  /** Stable id, used for selection and React keys. */
  id: string;
  /** Badges or status under the name. */
  badges?: ReactNode;
  /** Cannot be opened or selected. */
  disabled?: boolean | undefined;
}

/** Every string a file list shows or announces. */
export interface FileListMessages {
  /** Name of the select-all checkbox. */
  selectAll: string;
  /** Visible count when some files are selected. */
  selectedCount: (count: number) => string;
  /** Name of the bulk actions toolbar. */
  bulkActions: string;
  /** Shown when there are no files and no `empty` is given. */
  empty: string;
  /** Strings passed to each FileCard. */
  card?: Partial<FileCardMessages> | undefined;
}

export const FILE_LIST_MESSAGES: FileListMessages = {
  selectAll: 'Select all files',
  selectedCount: (count) => (count === 1 ? '1 file selected' : `${count} files selected`),
  bulkActions: 'Actions for selected files',
  empty: 'No files yet.',
};

export interface FileListProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The files, in display order. */
  items: readonly FileListItem[];
  /** `list` (default): one row per file. `grid`: cards with previews. */
  layout?: 'list' | 'grid' | undefined;
  /** Accessible name of the list, e.g. "Project files". */
  label?: string | undefined;
  /** Adds checkboxes, select all and the bulk actions bar. */
  selectable?: boolean | undefined;
  /** Selected ids (controlled). */
  selectedIds?: readonly string[] | undefined;
  /** Initially selected ids (uncontrolled). */
  defaultSelectedIds?: readonly string[] | undefined;
  /** Called with the new selection, in item order. */
  onSelectionChange?: ((ids: string[]) => void) | undefined;
  /** Opens a file (click or Enter on its name). Items with `href` navigate as well. */
  onOpen?: ((item: FileListItem, event: MouseEvent<HTMLElement>) => void) | undefined;
  /** Per-file actions, such as a DropdownMenu. */
  renderActions?: ((item: FileListItem) => ReactNode) | undefined;
  /** Actions for the selection (Download, Move, Delete), shown in a toolbar while any file is selected. */
  bulkActions?: ((selectedIds: string[]) => ReactNode) | undefined;
  /** Content when `items` is empty, e.g. an EmptyState. Default: a short sentence. */
  empty?: ReactNode;
  /** Translated strings. */
  messages?: Partial<FileListMessages> | undefined;
}

/**
 * A list or grid of files with optional selection (checkboxes, Shift-click
 * ranges, select all), per-file actions and a bulk actions bar.
 */
export const FileList = forwardRef<HTMLDivElement, FileListProps>(function FileList(
  {
    items,
    layout = 'list',
    label,
    selectable = false,
    selectedIds,
    defaultSelectedIds,
    onSelectionChange,
    onOpen,
    renderActions,
    bulkActions,
    empty,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const messages = { ...FILE_LIST_MESSAGES, ...messagesProp };
  const [inner, setInner] = useState<readonly string[]>(defaultSelectedIds ?? []);
  const selected = selectedIds ?? inner;
  const selectedSet = new Set(selected);
  const anchor = useRef<string | null>(null);
  const selectableItems = items.filter((i) => !i.disabled);
  const count = items.filter((i) => selectedSet.has(i.id)).length;
  const allChecked =
    selectableItems.length > 0 && selectableItems.every((i) => selectedSet.has(i.id));

  const commit = useCallback(
    (next: Set<string>) => {
      const ids = items.filter((i) => next.has(i.id)).map((i) => i.id);
      if (selectedIds === undefined) setInner(ids);
      onSelectionChange?.(ids);
    },
    [items, onSelectionChange, selectedIds],
  );

  const toggle = (item: FileListItem, on: boolean, shiftKey: boolean) => {
    const next = new Set(selected);
    const from = anchor.current ? items.findIndex((i) => i.id === anchor.current) : -1;
    const to = items.findIndex((i) => i.id === item.id);
    if (shiftKey && from >= 0 && to >= 0) {
      const [a, b] = from < to ? [from, to] : [to, from];
      for (const i of items.slice(a, b + 1)) {
        if (i.disabled) continue;
        if (on) next.add(i.id);
        else next.delete(i.id);
      }
    } else if (on) next.add(item.id);
    else next.delete(item.id);
    anchor.current = item.id;
    commit(next);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented || !selectable) return;
    if (event.key === 'Escape' && count > 0) {
      event.preventDefault();
      commit(new Set());
    }
  };

  if (!items.length) {
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-file-list', className)}
        data-layout={layout}
        data-empty=""
      >
        {empty ?? <p className="mn-file-list__empty">{messages.empty}</p>}
      </div>
    );
  }

  const ids = items.filter((i) => selectedSet.has(i.id)).map((i) => i.id);

  return (
    // Escape clears the selection from any control inside; the controls are the interactive elements.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={ref}
      {...rest}
      className={cx('mn-file-list', className)}
      data-layout={layout}
      data-selecting={(selectable && count > 0) || undefined}
      onKeyDown={handleKeyDown}
    >
      {selectable ? (
        <div className="mn-file-list__toolbar">
          <Checkbox
            label={messages.selectAll}
            checked={allChecked ? true : count > 0 ? 'indeterminate' : false}
            disabled={!selectableItems.length}
            onCheckedChange={() =>
              commit(allChecked ? new Set() : new Set(selectableItems.map((i) => i.id)))
            }
          />
          {count > 0 && bulkActions ? (
            <div className="mn-file-list__bulk" role="toolbar" aria-label={messages.bulkActions}>
              {bulkActions(ids)}
            </div>
          ) : null}
          <span className="mn-file-list__count" role="status">
            {count > 0 ? messages.selectedCount(count) : ''}
          </span>
        </div>
      ) : null}
      <ul className="mn-file-list__items" aria-label={label}>
        {items.map((item) => (
          <li key={item.id} className="mn-file-list__item">
            <FileCard
              layout={layout === 'grid' ? 'card' : 'row'}
              name={item.name}
              size={item.size}
              contentType={item.contentType}
              href={item.href}
              thumbnailSrc={item.thumbnailSrc}
              meta={item.meta}
              badges={item.badges}
              disabled={item.disabled}
              onOpen={onOpen ? (e) => onOpen(item, e) : undefined}
              selectable={selectable}
              selected={selectedSet.has(item.id)}
              onSelectedChange={(on, { shiftKey }) => toggle(item, on, shiftKey)}
              actions={renderActions?.(item)}
              messages={messages.card}
            />
          </li>
        ))}
      </ul>
    </div>
  );
});
