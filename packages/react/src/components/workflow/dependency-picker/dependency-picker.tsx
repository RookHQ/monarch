'use client';

import { forwardRef, useEffect, useId, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { useFocusAfterRemove } from '../../../internal/focus-after-remove.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { IconButton } from '../../actions/button/index.js';
import {
  Combobox,
  type ComboboxOption,
  type ComboboxSearchContext,
} from '../../forms/combobox/index.js';
import { StatusGlyph, type StatusOption } from '../status-picker/index.js';

/**
 * How another item relates to this one. `blocks`: this item must finish
 * first. `blocked-by`: the other item must finish first. `relates`: linked
 * without order. `duplicates` / `duplicated-by`: the same work.
 */
export type DependencyRelation =
  'blocks' | 'blocked-by' | 'relates' | 'duplicates' | 'duplicated-by';

/** Relations in display order. */
export const DEPENDENCY_RELATIONS: readonly DependencyRelation[] = [
  'blocked-by',
  'blocks',
  'relates',
  'duplicates',
  'duplicated-by',
];

/** An item that can be linked, such as an issue. */
export interface DependencyItem {
  /** Stable id. */
  id: string;
  /** Short key shown before the title, e.g. `STR-12`. */
  key?: string | undefined;
  /** Title. */
  title: string;
  /** Current status, shown with its glyph and spoken name. */
  status?: StatusOption | undefined;
  /** Where the item opens. Only http(s) and relative URLs are linked. */
  href?: string | undefined;
}

/** One existing link from this item to another. */
export interface DependencyLink {
  /** Stable id of the link itself. */
  id: string;
  relation: DependencyRelation;
  item: DependencyItem;
}

/** Every string a dependency picker shows or announces. */
export interface DependencyPickerMessages {
  /** Group headings and relation select options. */
  relations: Record<DependencyRelation, string>;
  /** Label of the relation select. */
  relationLabel: string;
  /** Label of the search input. */
  searchLabel: string;
  /** Placeholder of the search input. */
  searchPlaceholder: string;
  /** Shown when the search finds nothing. */
  noResults: string;
  /** Shown when there are no links. */
  empty: string;
  /** Name of a remove button. */
  remove: (relation: string, item: string) => string;
  /** Announced after adding. */
  added: (relation: string, item: string) => string;
  /** Announced after removing. */
  removed: (relation: string, item: string) => string;
  /** Spoken status of a linked item. */
  status: (name: string) => string;
}

export const DEPENDENCY_PICKER_MESSAGES: DependencyPickerMessages = {
  relations: {
    blocks: 'Blocks',
    'blocked-by': 'Blocked by',
    relates: 'Related to',
    duplicates: 'Duplicates',
    'duplicated-by': 'Duplicated by',
  },
  relationLabel: 'Relation',
  searchLabel: 'Find an item to link',
  searchPlaceholder: 'Search by key or title',
  noResults: 'No matching items.',
  empty: 'No linked items.',
  remove: (relation, item) => `Remove “${relation}” link to ${item}`,
  added: (relation, item) => `${relation} ${item} added.`,
  removed: (relation, item) => `${relation} ${item} removed.`,
  status: (name) => `Status: ${name}`,
};

const itemLabel = (item: DependencyItem) => (item.key ? `${item.key} ${item.title}` : item.title);

export interface DependencyPickerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The current links, grouped by relation in the list. */
  value: readonly DependencyLink[];
  /** Adds a link. The product saves it and updates `value`. */
  onAdd?: ((relation: DependencyRelation, item: DependencyItem) => void) | undefined;
  /** Removes a link. */
  onRemove?: ((link: DependencyLink) => void) | undefined;
  /** Items to search locally. Ignored when `onSearch` is set. */
  items?: readonly DependencyItem[] | undefined;
  /** Searches items on a server; a newer query aborts the previous one through `signal`. */
  onSearch?:
    ((query: string, context: ComboboxSearchContext) => Promise<DependencyItem[]>) | undefined;
  /** Ids that cannot be linked, such as the item itself. Already linked items are excluded too. */
  excludeIds?: readonly string[] | undefined;
  /** Relations offered, in order. Default all five. */
  relations?: readonly DependencyRelation[] | undefined;
  /** The relation selected when the picker opens. Default the first of `relations`. */
  defaultRelation?: DependencyRelation | undefined;
  /** Heading level of the relation group titles. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Hide the add controls (read-only list). */
  readOnly?: boolean | undefined;
  /** Disables adding and removing. */
  disabled?: boolean | undefined;
  /** Translated strings. */
  messages?:
    | (Partial<Omit<DependencyPickerMessages, 'relations'>> & {
        relations?: Partial<Record<DependencyRelation, string>> | undefined;
      })
    | undefined;
}

/**
 * Lists an item’s dependencies (blocked by, blocks, related, duplicates) and
 * adds new ones: choose the relation, then search for the item with a
 * combobox. Every relation is named in text and links are removed with
 * labelled buttons.
 */
export const DependencyPicker = forwardRef<HTMLDivElement, DependencyPickerProps>(
  function DependencyPicker(
    {
      value,
      onAdd,
      onRemove,
      items,
      onSearch,
      excludeIds,
      relations = DEPENDENCY_RELATIONS,
      defaultRelation,
      headingLevel = 3,
      readOnly = false,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEPENDENCY_PICKER_MESSAGES,
      ...messagesProp,
      relations: { ...DEPENDENCY_PICKER_MESSAGES.relations, ...messagesProp?.relations },
    };
    const id = useId();
    const focusAfterRemove = useFocusAfterRemove();
    const Heading = `h${headingLevel}` as const;
    const [relation, setRelation] = useState<DependencyRelation>(
      defaultRelation ?? relations[0] ?? 'blocked-by',
    );
    // Remounting the search after each add clears its text and keeps focus handling simple.
    const [round, setRound] = useState(0);
    const searchRef = useRef<HTMLInputElement | null>(null);
    useEffect(() => {
      if (round > 0) searchRef.current?.focus();
    }, [round]);
    const [announcement, setAnnouncement] = useState('');
    // Items seen in search results, so a picked value can be turned back into an item.
    const seen = useRef(new Map<string, DependencyItem>());

    const excluded = new Set([...(excludeIds ?? []), ...value.map((l) => l.item.id)]);
    const toOption = (item: DependencyItem): ComboboxOption => {
      seen.current.set(item.id, item);
      return {
        value: item.id,
        label: item.title,
        ...(item.key ? { hint: item.key } : {}),
        ...(item.status ? { description: item.status.name } : {}),
      };
    };
    const localOptions = (items ?? []).filter((i) => !excluded.has(i.id)).map(toOption);

    const groups = relations
      .map((r) => ({ relation: r, links: value.filter((l) => l.relation === r) }))
      .filter((g) => g.links.length > 0);

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-dependency-picker', className)}
        data-disabled={disabled || undefined}
      >
        {groups.length ? (
          groups.map((g) => {
            const headingId = `${id}-${g.relation}`;
            return (
              <section
                key={g.relation}
                className="mn-dependency-picker__group"
                aria-labelledby={headingId}
              >
                <Heading id={headingId} className="mn-dependency-picker__heading">
                  {m.relations[g.relation]}
                </Heading>
                <ul className="mn-dependency-picker__list">
                  {g.links.map((link) => {
                    const href =
                      link.item.href && isSafeUrl(link.item.href, ['http', 'https'])
                        ? link.item.href
                        : undefined;
                    const name = itemLabel(link.item);
                    return (
                      <li key={link.id} className="mn-dependency-picker__row">
                        {link.item.status ? (
                          <span className="mn-dependency-picker__status">
                            <StatusGlyph category={link.item.status.category} />
                            <span className="mn-visually-hidden">
                              {m.status(link.item.status.name)}
                            </span>
                          </span>
                        ) : null}
                        {link.item.key ? (
                          <span className="mn-dependency-picker__key">{link.item.key}</span>
                        ) : null}
                        {href ? (
                          <a className="mn-dependency-picker__title" href={href}>
                            {link.item.title}
                          </a>
                        ) : (
                          <span className="mn-dependency-picker__title">{link.item.title}</span>
                        )}
                        {onRemove && !readOnly ? (
                          <IconButton
                            size="xs"
                            className="mn-dependency-picker__remove"
                            label={m.remove(m.relations[link.relation], name)}
                            icon={<CloseGlyph />}
                            disabled={disabled}
                            onClick={(e) => {
                              // Focus stays in the picker: the next link, or the search.
                              focusAfterRemove(e.currentTarget, {
                                root: '.mn-dependency-picker',
                                row: '.mn-dependency-picker__row',
                                target: '.mn-dependency-picker__remove',
                                fallback: '.mn-dependency-picker__search input',
                              });
                              onRemove(link);
                              setAnnouncement(m.removed(m.relations[link.relation], name));
                            }}
                          />
                        ) : null}
                      </li>
                    );
                  })}
                </ul>
              </section>
            );
          })
        ) : (
          <p className="mn-dependency-picker__empty">{m.empty}</p>
        )}
        {!readOnly && onAdd ? (
          <div className="mn-dependency-picker__add">
            <label className="mn-dependency-picker__relation">
              <span className="mn-dependency-picker__relation-label">{m.relationLabel}</span>
              <select
                className="mn-dependency-picker__select"
                value={relation}
                disabled={disabled}
                onChange={(e) => setRelation(e.currentTarget.value as DependencyRelation)}
              >
                {relations.map((r) => (
                  <option key={r} value={r}>
                    {m.relations[r]}
                  </option>
                ))}
              </select>
            </label>
            <div className="mn-dependency-picker__search">
              <Combobox
                key={round}
                aria-label={m.searchLabel}
                listLabel={m.searchLabel}
                placeholder={m.searchPlaceholder}
                disabled={disabled}
                value={null}
                ref={searchRef}
                options={onSearch ? [] : localOptions}
                {...(onSearch
                  ? {
                      onSearch: async (q: string, ctx: ComboboxSearchContext) =>
                        (await onSearch(q, ctx)).filter((i) => !excluded.has(i.id)).map(toOption),
                    }
                  : {})}
                emptyMessage={m.noResults}
                onValueChange={(picked) => {
                  const item = picked ? seen.current.get(picked) : undefined;
                  if (!item) return;
                  onAdd(relation, item);
                  setRound((n) => n + 1);
                  setAnnouncement(m.added(m.relations[relation], itemLabel(item)));
                }}
              />
            </div>
          </div>
        ) : null}
        <span className="mn-visually-hidden" role="status">
          {announcement}
        </span>
      </div>
    );
  },
);
