'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { dayKey, type DayInput } from '../../../internal/day-groups.js';
import {
  MoreGlyph,
  PencilGlyph,
  PlusGlyph,
  StarGlyph,
  CloseGlyph,
} from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { SearchInput } from '../../search/search-input/index.js';

/** One conversation in the history. */
export interface AiConversationSummary {
  id: string;
  /** The conversation's title. Empty shows "New conversation". */
  title: string;
  /** When it last changed; decides its group. */
  updatedAt: DayInput;
  /** Pinned conversations are listed first, in their own group. */
  pinned?: boolean | undefined;
  /** A line of the latest message, shown under the title. */
  preview?: string | undefined;
}

/** The groups the history is split into, in display order. */
export type AiConversationGroup = 'pinned' | 'today' | 'previous7Days' | 'older';

export interface AiConversationListMessages {
  /** Accessible name of the navigation landmark. */
  label: string;
  /** Group headings. */
  groups: Record<AiConversationGroup, string>;
  untitled: string;
  newConversation: string;
  /** Accessible name of the search field. */
  search: string;
  /** Placeholder of the search field. */
  searchPlaceholder: string;
  /** Shown when there are no conversations. */
  empty: string;
  /** Shown when the search matches nothing. */
  noResults: (query: string) => string;
  /** Announced politely after the search changes. */
  results: (count: number) => string;
  /** Accessible name of a row's menu button. */
  more: (title: string) => string;
  rename: string;
  pin: string;
  unpin: string;
  delete: string;
  /** Accessible name of the rename field. */
  renameLabel: string;
  /** Keyboard hint read with the rename field. */
  renameHint: string;
  /** Delete confirmation. */
  deleteTitle: (title: string) => string;
  deleteDescription: string;
  deleteConfirm: string;
  cancel: string;
}

export const AI_CONVERSATION_LIST_MESSAGES: AiConversationListMessages = {
  label: 'Conversation history',
  groups: {
    pinned: 'Pinned',
    today: 'Today',
    previous7Days: 'Previous 7 days',
    older: 'Older',
  },
  untitled: 'New conversation',
  newConversation: 'New conversation',
  search: 'Search conversations',
  searchPlaceholder: 'Search',
  empty: 'No conversations yet.',
  noResults: (query) => `No conversations match “${query}”.`,
  results: (count) => (count === 1 ? '1 conversation' : `${count} conversations`),
  more: (title) => `More actions for “${title}”`,
  rename: 'Rename',
  pin: 'Pin',
  unpin: 'Unpin',
  delete: 'Delete',
  renameLabel: 'Conversation title',
  renameHint: 'Enter to save, Escape to cancel.',
  deleteTitle: (title) => `Delete “${title}”?`,
  deleteDescription: 'The conversation and its messages will be deleted. This can’t be undone.',
  deleteConfirm: 'Delete conversation',
  cancel: 'Cancel',
};

export interface AiConversationListProps extends Omit<HTMLAttributes<HTMLElement>, 'onSelect'> {
  /** The history, in any order; each group is sorted newest first. */
  conversations: readonly AiConversationSummary[];
  /** The open conversation, marked `aria-current="page"`. */
  activeId?: string | undefined;
  /** Opens a conversation. */
  onSelect: (id: string) => void;
  /** Shows a "New conversation" button at the top. */
  onNew?: (() => void) | undefined;
  /** Adds Rename to each row's menu (and F2). Called with the trimmed title when it changed. */
  onRename?: ((id: string, title: string) => void) | undefined;
  /**
   * Adds Delete to each row's menu (and the Delete key). Asks for confirmation
   * first. Return a promise to keep the dialog pending until it settles; a
   * rejection is shown in the dialog.
   */
  onDelete?: ((id: string) => unknown) | undefined;
  /** Adds Pin / Unpin to each row's menu. */
  onPin?: ((id: string, pinned: boolean) => void) | undefined;
  /**
   * Search on the server: called with the query on every change, and the list
   * shows `conversations` as given. Without it the list filters titles and
   * previews itself.
   */
  onSearch?: ((query: string) => void) | undefined;
  /** Show the search field. Default true. */
  searchable?: boolean | undefined;
  /** Heading level of the group headings. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Reference time for grouping (tests and stories). Default: now. */
  now?: DayInput | undefined;
  /** IANA time zone for grouping. Default: the viewer's. */
  timeZone?: string | undefined;
  /** Strings, for localisation. `groups` merges per key. */
  messages?:
    | (Partial<Omit<AiConversationListMessages, 'groups'>> & {
        groups?: Partial<Record<AiConversationGroup, string>>;
      })
    | undefined;
}

const GROUP_ORDER: AiConversationGroup[] = ['pinned', 'today', 'previous7Days', 'older'];

function time(input: DayInput): number {
  const t = (input instanceof Date ? input : new Date(input)).getTime();
  return Number.isNaN(t) ? 0 : t;
}

function daysBetween(fromKey: string, toKey: string): number {
  const toUtc = (key: string) => {
    const [y, m, d] = key.split('-').map(Number) as [number, number, number];
    return Date.UTC(y, m - 1, d);
  };
  return Math.round((toUtc(toKey) - toUtc(fromKey)) / 86_400_000);
}

/**
 * Splits conversations into Pinned, Today, Previous 7 days and Older (in the
 * viewer's time zone, or `timeZone`), newest first within each group. Empty
 * groups are left out.
 */
export function groupAiConversations<T extends AiConversationSummary>(
  conversations: readonly T[],
  options: { now?: DayInput | undefined; timeZone?: string | undefined } = {},
): { group: AiConversationGroup; items: T[] }[] {
  const today = dayKey(options.now ?? Date.now(), options.timeZone);
  const buckets: Record<AiConversationGroup, T[]> = {
    pinned: [],
    today: [],
    previous7Days: [],
    older: [],
  };
  for (const c of conversations) {
    if (c.pinned) {
      buckets.pinned.push(c);
      continue;
    }
    const key = dayKey(c.updatedAt, options.timeZone);
    const age = key === 'invalid' ? Infinity : daysBetween(key, today);
    if (age <= 0) buckets.today.push(c);
    else if (age <= 7) buckets.previous7Days.push(c);
    else buckets.older.push(c);
  }
  return GROUP_ORDER.filter((g) => buckets[g].length > 0).map((group) => ({
    group,
    items: [...buckets[group]].sort((a, b) => time(b.updatedAt) - time(a.updatedAt)),
  }));
}

function matches(c: AiConversationSummary, query: string): boolean {
  const q = query.trim().toLocaleLowerCase();
  if (!q) return true;
  return (
    c.title.toLocaleLowerCase().includes(q) || (c.preview ?? '').toLocaleLowerCase().includes(q)
  );
}

/**
 * The history sidebar of an AI chat: conversations grouped into Pinned,
 * Today, Previous 7 days and Older, with search, the open conversation
 * marked, and per-row Rename (inline), Pin and Delete (confirmed). Arrow keys
 * move between conversations; F2 renames and Delete deletes the focused one.
 */
export const AiConversationList = forwardRef<HTMLElement, AiConversationListProps>(
  function AiConversationList(
    {
      conversations,
      activeId,
      onSelect,
      onNew,
      onRename,
      onDelete,
      onPin,
      onSearch,
      searchable = true,
      headingLevel = 3,
      now,
      timeZone,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...AI_CONVERSATION_LIST_MESSAGES,
      ...messagesProp,
      groups: { ...AI_CONVERSATION_LIST_MESSAGES.groups, ...messagesProp?.groups },
    };
    const baseId = useId();
    const Heading = `h${headingLevel}` as const;
    const [query, setQuery] = useState('');
    const [announcement, setAnnouncement] = useState('');
    const [renaming, setRenaming] = useState<string | null>(null);
    const [draft, setDraft] = useState('');
    const rows = useRef(new Map<string, HTMLButtonElement>());
    const searchRef = useRef<HTMLInputElement>(null);
    const renameInput = useRef<HTMLInputElement>(null);
    const focusAfterRename = useRef<string | null>(null);
    /** What the menu should do once it has closed (and returned focus). */
    const afterMenu = useRef<{ kind: 'rename' | 'delete'; id: string } | null>(null);
    const { confirm, element: confirmElement } = useConfirm();

    const visible = useMemo(
      () => (onSearch ? conversations : conversations.filter((c) => matches(c, query))),
      [conversations, onSearch, query],
    );
    const groups = useMemo(
      () => groupAiConversations(visible, { now, timeZone }),
      [visible, now, timeZone],
    );
    const order = groups.flatMap((g) => g.items.map((c) => c.id));
    const orderRef = useRef(order);
    orderRef.current = order;

    const titleOf = (c: AiConversationSummary) => c.title.trim() || m.untitled;

    useEffect(() => {
      if (renaming) {
        renameInput.current?.focus();
        renameInput.current?.select();
        return;
      }
      const id = focusAfterRename.current;
      if (!id) return;
      focusAfterRename.current = null;
      rows.current.get(id)?.focus();
    }, [renaming]);

    const changeQuery = (value: string) => {
      setQuery(value);
      onSearch?.(value);
      if (!onSearch) {
        const count = value.trim() ? conversations.filter((c) => matches(c, value)).length : 0;
        setAnnouncement(value.trim() ? m.results(count) : '');
      }
    };

    const startRename = (c: AiConversationSummary) => {
      setDraft(c.title);
      setRenaming(c.id);
    };

    const endRename = (save: boolean, refocus: boolean) => {
      const id = renaming;
      if (!id) return;
      const current = conversations.find((c) => c.id === id);
      const next = draft.trim();
      if (save && current && next && next !== current.title.trim()) onRename?.(id, next);
      if (refocus) focusAfterRename.current = id;
      setRenaming(null);
    };

    const startDelete = async (c: AiConversationSummary) => {
      const index = orderRef.current.indexOf(c.id);
      let deleted = false;
      const result = await confirm({
        title: m.deleteTitle(titleOf(c)),
        description: m.deleteDescription,
        tone: 'danger',
        confirmLabel: m.deleteConfirm,
        cancelLabel: m.cancel,
        onConfirm: async () => {
          await onDelete?.(c.id);
          deleted = true;
        },
        onCloseAutoFocus: (event) => {
          // On delete, focus the conversation that took its place.
          if (!deleted) return;
          const ids = orderRef.current.filter((id) => id !== c.id);
          const nextId = ids[Math.min(index, ids.length - 1)];
          const target = (nextId ? rows.current.get(nextId) : undefined) ?? searchRef.current;
          if (!target) return;
          event.preventDefault();
          target.focus();
        },
      });
      return result;
    };

    const onRowKeyDown = (
      event: ReactKeyboardEvent<HTMLButtonElement>,
      c: AiConversationSummary,
    ) => {
      const ids = orderRef.current;
      const index = ids.indexOf(c.id);
      const focusAt = (i: number) => {
        const id = ids[Math.max(0, Math.min(ids.length - 1, i))];
        if (id) rows.current.get(id)?.focus();
      };
      switch (event.key) {
        case 'ArrowDown':
          focusAt(index + 1);
          break;
        case 'ArrowUp':
          focusAt(index - 1);
          break;
        case 'Home':
          focusAt(0);
          break;
        case 'End':
          focusAt(ids.length - 1);
          break;
        case 'F2':
          if (!onRename) return;
          startRename(c);
          break;
        case 'Delete':
          if (!onDelete) return;
          void startDelete(c);
          break;
        default:
          return;
      }
      event.preventDefault();
    };

    const onRenameKeyDown = (event: ReactKeyboardEvent<HTMLInputElement>) => {
      if (event.nativeEvent.isComposing) return;
      if (event.key === 'Enter') {
        event.preventDefault();
        endRename(true, true);
      } else if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        endRename(false, true);
      }
    };

    const hasMenu = Boolean(onRename || onDelete || onPin);
    const searching = !onSearch && query.trim().length > 0;

    return (
      <nav
        ref={ref}
        aria-label={m.label}
        {...rest}
        className={cx('mn-ai-conversation-list', className)}
      >
        {onNew || searchable ? (
          <div className="mn-ai-conversation-list__top">
            {onNew ? (
              <Button
                size="sm"
                leadingIcon={<PlusGlyph />}
                className="mn-ai-conversation-list__new"
                onClick={onNew}
              >
                {m.newConversation}
              </Button>
            ) : null}
            {searchable ? (
              <SearchInput
                ref={searchRef}
                size="sm"
                aria-label={m.search}
                placeholder={m.searchPlaceholder}
                value={query}
                onValueChange={changeQuery}
                className="mn-ai-conversation-list__search"
              />
            ) : null}
          </div>
        ) : null}

        <div className="mn-ai-conversation-list__groups">
          {conversations.length === 0 ? (
            <p className="mn-ai-conversation-list__empty">{m.empty}</p>
          ) : groups.length === 0 ? (
            <p className="mn-ai-conversation-list__empty">{m.noResults(query.trim())}</p>
          ) : (
            groups.map(({ group, items }) => {
              const headingId = `${baseId}-${group}`;
              return (
                <section
                  key={group}
                  aria-labelledby={headingId}
                  className="mn-ai-conversation-list__group"
                  data-group={group}
                >
                  <Heading id={headingId} className="mn-ai-conversation-list__heading">
                    {m.groups[group]}
                  </Heading>
                  <ul className="mn-ai-conversation-list__list">
                    {items.map((c) => {
                      const active = c.id === activeId;
                      const title = titleOf(c);
                      const previewId = `${baseId}-${c.id}-preview`;
                      const titleId = `${baseId}-${c.id}-title`;
                      return (
                        <li
                          key={c.id}
                          className="mn-ai-conversation-list__item"
                          data-active={active || undefined}
                          data-pinned={c.pinned || undefined}
                        >
                          {renaming === c.id ? (
                            <div className="mn-ai-conversation-list__rename">
                              <input
                                ref={renameInput}
                                className="mn-ai-conversation-list__rename-input"
                                aria-label={m.renameLabel}
                                aria-describedby={`${baseId}-rename-hint`}
                                value={draft}
                                onChange={(event) => setDraft(event.target.value)}
                                onKeyDown={onRenameKeyDown}
                                onBlur={() => endRename(true, false)}
                              />
                              <span id={`${baseId}-rename-hint`} className="mn-visually-hidden">
                                {m.renameHint}
                              </span>
                            </div>
                          ) : (
                            <button
                              ref={(el) => {
                                if (el) rows.current.set(c.id, el);
                                else rows.current.delete(c.id);
                              }}
                              type="button"
                              className="mn-ai-conversation-list__row"
                              aria-current={active ? 'page' : undefined}
                              aria-labelledby={titleId}
                              aria-describedby={c.preview ? previewId : undefined}
                              aria-keyshortcuts={
                                [onRename ? 'F2' : '', onDelete ? 'Delete' : '']
                                  .filter(Boolean)
                                  .join(' ') || undefined
                              }
                              onClick={() => onSelect(c.id)}
                              onKeyDown={(event) => onRowKeyDown(event, c)}
                            >
                              <span className="mn-ai-conversation-list__title">
                                {c.pinned ? (
                                  <StarGlyph aria-hidden className="mn-ai-conversation-list__pin" />
                                ) : null}
                                <span id={titleId} className="mn-ai-conversation-list__title-text">
                                  {title}
                                </span>
                              </span>
                              {c.preview ? (
                                <span id={previewId} className="mn-ai-conversation-list__preview">
                                  {c.preview}
                                </span>
                              ) : null}
                            </button>
                          )}
                          {hasMenu && renaming !== c.id ? (
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <IconButton
                                  size="sm"
                                  label={m.more(title)}
                                  icon={<MoreGlyph />}
                                  className="mn-ai-conversation-list__more"
                                />
                              </DropdownMenuTrigger>
                              <DropdownMenuContent
                                align="end"
                                onCloseAutoFocus={(event) => {
                                  const next = afterMenu.current;
                                  afterMenu.current = null;
                                  if (!next) return;
                                  if (next.kind === 'rename') {
                                    event.preventDefault();
                                    const target = conversations.find((x) => x.id === next.id);
                                    if (target) startRename(target);
                                  } else {
                                    const target = conversations.find((x) => x.id === next.id);
                                    // Let focus return to the trigger first, so the
                                    // confirmation returns focus there on cancel.
                                    if (target)
                                      window.setTimeout(() => void startDelete(target), 0);
                                  }
                                }}
                              >
                                {onRename ? (
                                  <DropdownMenuItem
                                    icon={<PencilGlyph />}
                                    shortcut="F2"
                                    onSelect={() => {
                                      afterMenu.current = { kind: 'rename', id: c.id };
                                    }}
                                  >
                                    {m.rename}
                                  </DropdownMenuItem>
                                ) : null}
                                {onPin ? (
                                  <DropdownMenuItem
                                    icon={<StarGlyph />}
                                    onSelect={() => onPin(c.id, !c.pinned)}
                                  >
                                    {c.pinned ? m.unpin : m.pin}
                                  </DropdownMenuItem>
                                ) : null}
                                {onDelete ? (
                                  <>
                                    {onRename || onPin ? <DropdownMenuSeparator /> : null}
                                    <DropdownMenuItem
                                      tone="danger"
                                      icon={<CloseGlyph />}
                                      shortcut="Del"
                                      onSelect={() => {
                                        afterMenu.current = { kind: 'delete', id: c.id };
                                      }}
                                    >
                                      {m.delete}
                                    </DropdownMenuItem>
                                  </>
                                ) : null}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          ) : null}
                        </li>
                      );
                    })}
                  </ul>
                </section>
              );
            })
          )}
        </div>

        <span role="status" className="mn-visually-hidden">
          {searching ? announcement : ''}
        </span>
        {confirmElement}
      </nav>
    );
  },
);
