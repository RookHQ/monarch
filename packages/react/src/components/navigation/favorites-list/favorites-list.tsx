'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  type ElementType,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, StarGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { ReorderableList } from '../../interaction/reorderable-list/index.js';

/** One pinned item. The product resolves names and links before passing them in. */
export interface FavoriteItem {
  /** Stable, unique key, e.g. `project:42`. */
  id: string;
  /** Visible name. */
  label: string;
  /** Where the link goes. */
  href: string;
  /** Small leading icon or glyph. Decorative. */
  icon?: ReactNode;
  /** The current page: rendered with `aria-current="page"`. */
  current?: boolean;
}

/** Visible strings, for translation. */
export interface FavoritesListMessages {
  /** Heading and navigation name. Default "Favorites". */
  title: string;
  /** Label of the button that enters edit mode. Default "Edit favorites". */
  edit: string;
  /** Label of the button that leaves edit mode. Default "Done". */
  done: string;
  /** Accessible name of a remove button. Default "Remove {label} from favorites". */
  remove: (label: string) => string;
  /** Announced after removing. Default "{label} removed from favorites." */
  removed: (label: string) => string;
}

export const FAVORITES_LIST_MESSAGES: FavoritesListMessages = {
  title: 'Favorites',
  edit: 'Edit favorites',
  done: 'Done',
  remove: (label) => `Remove ${label} from favorites`,
  removed: (label) => `${label} removed from favorites.`,
};

export interface FavoritesListProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The pinned items, in order. */
  items: readonly FavoriteItem[];
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. */
  linkAs?: ElementType<AnchorHTMLAttributes<HTMLAnchorElement>> | undefined;
  /** Remove an item. When given, edit mode shows a remove button on each row. */
  onRemove?: ((item: FavoriteItem) => void) | undefined;
  /** Save a new order. When given, edit mode lets people drag or move rows. */
  onReorder?: ((items: FavoriteItem[]) => void) | undefined;
  /** Controlled edit mode. */
  editing?: boolean;
  /** Uncontrolled initial edit mode. */
  defaultEditing?: boolean;
  onEditingChange?: ((editing: boolean) => void) | undefined;
  /**
   * Shown when there are no items. By default the whole list renders nothing
   * when empty, as navigation sections usually disappear.
   */
  empty?: ReactNode;
  /** Show the star before the heading. Default `true`. */
  showIcon?: boolean;
  /** Visible strings, for translation. */
  messages?: Partial<FavoritesListMessages>;
}

/**
 * A navigation section of the person's pinned pages (projects, views,
 * documents), for a sidebar or rail. Items are links; when the product
 * passes `onRemove` or `onReorder`, an Edit button turns the rows into a
 * reorderable list with remove buttons. Storage stays in the product.
 */
export const FavoritesList = forwardRef<HTMLElement, FavoritesListProps>(function FavoritesList(
  {
    items,
    linkAs: LinkComp = 'a',
    onRemove,
    onReorder,
    editing: editingProp,
    defaultEditing = false,
    onEditingChange,
    empty,
    showIcon = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...FAVORITES_LIST_MESSAGES, ...messagesProp };
  const headingId = useId();
  const [editingState, setEditingState] = useState(defaultEditing);
  const editable = !!(onRemove || onReorder);
  const editing = editable && (editingProp ?? editingState);
  const [announcement, setAnnouncement] = useState('');
  const removeButtons = useRef(new Map<string, HTMLButtonElement>());
  const toggleRef = useRef<HTMLButtonElement>(null);
  const focusAfterRemove = useRef<string | null | undefined>(undefined);

  const setEditing = (next: boolean) => {
    if (editingProp === undefined) setEditingState(next);
    onEditingChange?.(next);
  };

  // After a removal, keep focus in the list: the next row's remove button,
  // or the Done button when the list is now empty.
  useEffect(() => {
    const target = focusAfterRemove.current;
    if (target === undefined) return;
    focusAfterRemove.current = undefined;
    const button = target ? removeButtons.current.get(target) : undefined;
    (button ?? toggleRef.current)?.focus();
  }, [items]);

  const remove = (item: FavoriteItem) => {
    const index = items.findIndex((i) => i.id === item.id);
    const next = items[index + 1] ?? items[index - 1];
    focusAfterRemove.current = next ? next.id : null;
    setAnnouncement(messages.removed(item.label));
    onRemove?.(item);
  };

  if (items.length === 0 && empty === undefined) return null;

  const row = (item: FavoriteItem, inEdit: boolean) => (
    <>
      {inEdit ? (
        <span className="mn-favorites-list__entry">
          {item.icon ? (
            <span className="mn-favorites-list__icon" aria-hidden>
              {item.icon}
            </span>
          ) : null}
          <span className="mn-favorites-list__label">{item.label}</span>
        </span>
      ) : (
        <LinkComp
          href={item.href}
          className="mn-favorites-list__link"
          aria-current={item.current ? 'page' : undefined}
          data-current={item.current || undefined}
        >
          {item.icon ? (
            <span className="mn-favorites-list__icon" aria-hidden>
              {item.icon}
            </span>
          ) : null}
          <span className="mn-favorites-list__label">{item.label}</span>
        </LinkComp>
      )}
      {inEdit && onRemove ? (
        <IconButton
          ref={(node) => {
            if (node) removeButtons.current.set(item.id, node);
            else removeButtons.current.delete(item.id);
          }}
          size="xs"
          className="mn-favorites-list__remove"
          label={messages.remove(item.label)}
          icon={<CloseGlyph />}
          onClick={() => remove(item)}
        />
      ) : null}
    </>
  );

  return (
    <nav
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-favorites-list', className)}
      data-editing={editing || undefined}
    >
      <div className="mn-favorites-list__header">
        <p className="mn-favorites-list__title" id={headingId}>
          {showIcon ? <StarGlyph className="mn-favorites-list__star" /> : null}
          {messages.title}
        </p>
        {editable && items.length > 0 ? (
          <button
            ref={toggleRef}
            type="button"
            className="mn-favorites-list__toggle"
            onClick={() => setEditing(!editing)}
          >
            {editing ? messages.done : messages.edit}
          </button>
        ) : null}
      </div>
      {items.length === 0 ? (
        <div className="mn-favorites-list__empty">{empty}</div>
      ) : editing && onReorder ? (
        <ReorderableList
          className="mn-favorites-list__reorder"
          items={items}
          getKey={(item) => item.id}
          getLabel={(item) => item.label}
          onReorder={(next) => onReorder(next)}
          renderItem={(item) => <div className="mn-favorites-list__row">{row(item, true)}</div>}
        />
      ) : (
        <ul className="mn-favorites-list__list">
          {items.map((item) => (
            <li key={item.id} className="mn-favorites-list__row">
              {row(item, editing)}
            </li>
          ))}
        </ul>
      )}
      <span className="mn-visually-hidden" role="status">
        {announcement}
      </span>
    </nav>
  );
});

export interface FavoriteToggleProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'onChange' | 'children'
> {
  /** Whether the item is pinned. */
  pressed: boolean;
  /** Called with the new state. */
  onPressedChange?: ((pressed: boolean) => void) | undefined;
  /**
   * Accessible name. It does not change with the state; `aria-pressed`
   * tells assistive technology whether the item is pinned. Default "Favorite".
   */
  label?: string;
  /** Size of the button. Default `sm`. */
  size?: 'xs' | 'sm' | 'md';
}

/**
 * The star button on a page header or row that pins it to the favorites
 * list. A toggle button: its name stays the same and `aria-pressed` says
 * whether the item is pinned.
 */
export const FavoriteToggle = forwardRef<HTMLButtonElement, FavoriteToggleProps>(
  function FavoriteToggle(
    { pressed, onPressedChange, label = 'Favorite', size = 'sm', className, onClick, ...rest },
    ref,
  ) {
    return (
      <IconButton
        ref={ref}
        size={size}
        {...rest}
        className={cx('mn-favorite-toggle', className)}
        label={label}
        aria-pressed={pressed}
        data-pressed={pressed || undefined}
        icon={<StarGlyph />}
        onClick={(event) => {
          onClick?.(event);
          if (!event.defaultPrevented) onPressedChange?.(!pressed);
        }}
      />
    );
  },
);
