'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';
import { SearchInput } from '../../search/search-input/index.js';
import {
  AiMemoryChip,
  shortMemoryText,
  type AiMemory,
  type AiMemoryChipMessages,
  type AiMemoryEntity,
} from '../ai-memory-chip/index.js';

/** A remembered fact as AiMemoryPanel lists it: always about an entity, with a last-updated time. */
export interface AiMemoryItem extends AiMemory {
  /** Who or what the memory is about; memories are grouped by it. */
  entity: AiMemoryEntity;
  /** When the memory was saved or last changed: ISO 8601 string, epoch milliseconds or a Date. */
  updatedAt: string | number | Date;
}

/** Every string AiMemoryPanel shows or announces. English defaults. */
export interface AiMemoryPanelMessages {
  /** Accessible name of the panel. */
  label: string;
  /** Accessible name of the search field. */
  search: string;
  searchPlaceholder: string;
  /** Accessible name of the checkbox that selects every memory shown. */
  selectAll: string;
  /** Visible text next to the select-all checkbox. */
  selectAllText: (shown: number) => string;
  /** Accessible name of a group's checkbox. */
  selectGroup: (entityName: string) => string;
  /** Accessible name of a memory's checkbox. */
  select: (text: string) => string;
  /** The bulk action button. */
  forgetSelected: (count: number) => string;
  /** Group label per entity kind. */
  kinds: Record<AiMemoryEntity['kind'], string>;
  /** Count after a group's name. */
  count: (count: number) => string;
  /** Last-updated line under each memory. */
  updated: (formatted: string) => string;
  /** Formats `updatedAt`. */
  formatDate: (date: Date) => string;
  /** Shown when there are no memories at all. */
  empty: string;
  /** Shown when the search matches nothing. */
  noMatches: (query: string) => string;
  /** Announced politely after a search. */
  matches: (count: number) => string;
  /** Confirmation dialog for bulk forget. */
  confirmTitle: (count: number) => string;
  confirmDescription: string;
  confirmLabel: string;
  /** Announced politely after forgetting. */
  forgotten: (count: number) => string;
  /** Announced politely after an edit. */
  edited: string;
  /** Strings for each row (edit, forget, field labels). */
  chip?: Partial<AiMemoryChipMessages> | undefined;
}

const plural = (n: number, one: string, many: string) => (n === 1 ? one : many);

export const AI_MEMORY_PANEL_MESSAGES: AiMemoryPanelMessages = {
  label: 'Memories',
  search: 'Search memories',
  searchPlaceholder: 'Search memories',
  selectAll: 'Select all shown memories',
  selectAllText: (n) => `Select all (${n})`,
  selectGroup: (name) => `Select all memories about ${name}`,
  select: (text) => `Select memory: ${shortMemoryText(text)}`,
  forgetSelected: (n) => (n > 0 ? `Forget selected (${n})` : 'Forget selected'),
  kinds: { person: 'Person', org: 'Organization', project: 'Project', other: 'Other' },
  count: (n) => `${n} ${plural(n, 'memory', 'memories')}`,
  updated: (formatted) => `Updated ${formatted}`,
  formatDate: (date) => new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date),
  empty: 'Nothing is remembered yet.',
  noMatches: (query) => `No memories match “${query}”.`,
  matches: (n) => `${n} ${plural(n, 'memory matches', 'memories match')}`,
  confirmTitle: (n) => `Forget ${n} ${plural(n, 'memory', 'memories')}?`,
  confirmDescription:
    'The assistant will stop using them in future conversations. This can’t be undone.',
  confirmLabel: 'Forget',
  forgotten: (n) => (n === 1 ? 'Memory forgotten' : `${n} memories forgotten`),
  edited: 'Memory updated',
};

const KIND_ORDER: AiMemoryEntity['kind'][] = ['person', 'org', 'project', 'other'];

interface Group {
  key: string;
  entity: AiMemoryEntity;
  items: AiMemoryItem[];
}

function time(value: AiMemoryItem['updatedAt']): number {
  const t = value instanceof Date ? value.getTime() : new Date(value).getTime();
  return Number.isFinite(t) ? t : 0;
}

/**
 * Groups memories by entity (people, then organizations, projects and other,
 * each by name) with the most recently updated memory first in each group.
 */
export function groupAiMemories(memories: readonly AiMemoryItem[]): Group[] {
  const groups = new Map<string, Group>();
  for (const memory of memories) {
    const key = `${memory.entity.kind}:${memory.entity.name}`;
    let group = groups.get(key);
    if (!group) {
      group = { key, entity: memory.entity, items: [] };
      groups.set(key, group);
    }
    group.items.push(memory);
  }
  const list = [...groups.values()];
  for (const g of list) g.items.sort((a, b) => time(b.updatedAt) - time(a.updatedAt));
  return list.sort(
    (a, b) =>
      KIND_ORDER.indexOf(a.entity.kind) - KIND_ORDER.indexOf(b.entity.kind) ||
      a.entity.name.localeCompare(b.entity.name),
  );
}

function matches(memory: AiMemoryItem, query: string) {
  const q = query.trim().toLocaleLowerCase();
  if (!q) return true;
  return (
    memory.text.toLocaleLowerCase().includes(q) ||
    memory.entity.name.toLocaleLowerCase().includes(q)
  );
}

function checkState(selected: ReadonlySet<string>, ids: readonly string[]) {
  const n = ids.filter((id) => selected.has(id)).length;
  if (n === 0) return false;
  return n === ids.length ? true : ('indeterminate' as const);
}

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

export interface AiMemoryPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Everything the AI remembers. Controlled: remove or change items after the callbacks. */
  memories: readonly AiMemoryItem[];
  /** Saves an edited memory, with its id and the new trimmed text. */
  onEdit: (id: string, text: string) => void;
  /** Forgets one memory. Called straight away; the row's Forget button does not confirm. */
  onForget: (id: string) => void;
  /**
   * Forgets the selected memories, after the person confirms. May return a
   * promise: the dialog stays open and pending until it settles, and shows the
   * error if it rejects.
   */
  onForgetMany: (ids: string[]) => unknown;
  /** Heading level of each entity group. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Strings, for localisation. `kinds` and `chip` merge per key. */
  messages?:
    | (Partial<Omit<AiMemoryPanelMessages, 'kinds'>> & {
        kinds?: Partial<AiMemoryPanelMessages['kinds']> | undefined;
      })
    | undefined;
}

/**
 * Everything the AI remembers, grouped by the person, organization or project
 * it is about. People can search, edit a memory in place, forget one, or
 * select several and forget them together after a confirmation. Changes are
 * announced politely.
 */
export const AiMemoryPanel = forwardRef<HTMLElement, AiMemoryPanelProps>(function AiMemoryPanel(
  {
    memories,
    onEdit,
    onForget,
    onForgetMany,
    headingLevel = 3,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m: AiMemoryPanelMessages = {
    ...AI_MEMORY_PANEL_MESSAGES,
    ...messagesProp,
    kinds: { ...AI_MEMORY_PANEL_MESSAGES.kinds, ...messagesProp?.kinds },
  };
  const id = useId();
  const rootRef = useRef<HTMLElement | null>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<ReadonlySet<string>>(() => new Set());
  const [announcement, setAnnouncement] = useState('');
  const { confirm, element: confirmElement } = useConfirm();
  const pendingFocus = useRef<{ id: string; index: number } | null>(null);
  const focusSearchOnClose = useRef(false);

  const visible = useMemo(() => memories.filter((x) => matches(x, query)), [memories, query]);
  const groups = useMemo(() => groupAiMemories(visible), [visible]);
  const visibleIds = useMemo(() => groups.flatMap((g) => g.items.map((x) => x.id)), [groups]);

  // Drop selections for memories that no longer exist.
  const existing = useMemo(() => new Set(memories.map((x) => x.id)), [memories]);
  const selectedIds = memories.filter((x) => selected.has(x.id)).map((x) => x.id);

  // After a single forget, move focus to the row that took its place.
  useEffect(() => {
    const pending = pendingFocus.current;
    if (!pending || existing.has(pending.id)) return;
    pendingFocus.current = null;
    const boxes = rootRef.current?.querySelectorAll<HTMLElement>(
      '.mn-ai-memory-panel__item [role="checkbox"]',
    );
    const next = boxes?.[Math.min(pending.index, boxes.length - 1)];
    (next ?? searchRef.current)?.focus();
  }, [existing]);

  const toggle = (ids: readonly string[], on: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev);
      for (const x of ids) {
        if (on) next.add(x);
        else next.delete(x);
      }
      return next;
    });
  };

  const forgetOne = (memoryId: string) => {
    pendingFocus.current = { id: memoryId, index: visibleIds.indexOf(memoryId) };
    toggle([memoryId], false);
    onForget(memoryId);
    setAnnouncement(m.forgotten(1));
  };

  const forgetSelected = async () => {
    const ids = selectedIds;
    if (ids.length === 0) return;
    const result = await confirm({
      title: m.confirmTitle(ids.length),
      description: m.confirmDescription,
      confirmLabel: m.confirmLabel,
      tone: 'danger',
      onConfirm: async () => {
        await onForgetMany(ids);
        focusSearchOnClose.current = true;
      },
      onCloseAutoFocus: (event) => {
        if (!focusSearchOnClose.current) return;
        focusSearchOnClose.current = false;
        event.preventDefault();
        searchRef.current?.focus();
      },
    });
    if (result) {
      toggle(ids, false);
      setAnnouncement(m.forgotten(ids.length));
    }
  };

  const allState = checkState(selected, visibleIds);
  const Heading = `h${headingLevel}` as const;
  const trimmedQuery = query.trim();

  return (
    <section
      ref={(node) => {
        rootRef.current = node;
        setRef(ref, node);
      }}
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-memory-panel', className)}
      data-empty={memories.length === 0 || undefined}
    >
      {memories.length > 0 ? (
        <div className="mn-ai-memory-panel__toolbar">
          <SearchInput
            ref={searchRef}
            className="mn-ai-memory-panel__search"
            aria-label={m.search}
            placeholder={m.searchPlaceholder}
            value={query}
            onValueChange={setQuery}
            debounce={300}
            onSearch={(q) => setAnnouncement(q.trim() ? m.matches(visibleCount(memories, q)) : '')}
            size="sm"
          />
          <div className="mn-ai-memory-panel__bulk">
            <Checkbox
              aria-label={m.selectAll}
              checked={allState}
              disabled={visibleIds.length === 0}
              onCheckedChange={(v) => toggle(visibleIds, v === true)}
            />
            <span className="mn-ai-memory-panel__bulk-text" aria-hidden>
              {m.selectAllText(visibleIds.length)}
            </span>
            <Button
              size="sm"
              variant="danger-subtle"
              className="mn-ai-memory-panel__forget-selected"
              disabled={selectedIds.length === 0}
              onClick={() => void forgetSelected()}
            >
              {m.forgetSelected(selectedIds.length)}
            </Button>
          </div>
        </div>
      ) : null}

      {memories.length === 0 ? (
        <p className="mn-ai-memory-panel__empty">{m.empty}</p>
      ) : groups.length === 0 ? (
        <p className="mn-ai-memory-panel__empty">{m.noMatches(trimmedQuery)}</p>
      ) : (
        groups.map((group, groupIndex) => {
          const headingId = `${id}-group-${groupIndex}`;
          const ids = group.items.map((x) => x.id);
          return (
            <div
              key={group.key}
              className="mn-ai-memory-panel__group"
              data-kind={group.entity.kind}
            >
              <div className="mn-ai-memory-panel__group-header">
                <Checkbox
                  aria-label={m.selectGroup(group.entity.name)}
                  checked={checkState(selected, ids)}
                  onCheckedChange={(v) => toggle(ids, v === true)}
                />
                <Heading id={headingId} className="mn-ai-memory-panel__heading">
                  <span className="mn-ai-memory-panel__entity">{group.entity.name}</span>
                  <span className="mn-ai-memory-panel__kind">
                    {m.kinds[group.entity.kind]} · {m.count(group.items.length)}
                  </span>
                </Heading>
              </div>
              <ul className="mn-ai-memory-panel__list" aria-labelledby={headingId}>
                {group.items.map((memory) => (
                  <li key={memory.id} className="mn-ai-memory-panel__item">
                    <AiMemoryChip
                      variant="row"
                      memory={memory}
                      aria-label={undefined}
                      role={undefined}
                      leading={
                        <Checkbox
                          aria-label={m.select(memory.text)}
                          checked={selected.has(memory.id)}
                          onCheckedChange={(v) => toggle([memory.id], v === true)}
                        />
                      }
                      meta={m.updated(m.formatDate(new Date(time(memory.updatedAt))))}
                      onEdit={(memoryId, text) => {
                        onEdit(memoryId, text);
                        setAnnouncement(m.edited);
                      }}
                      onForget={forgetOne}
                      messages={m.chip}
                    />
                  </li>
                ))}
              </ul>
            </div>
          );
        })
      )}

      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {announcement}
      </span>
      {confirmElement}
    </section>
  );
});

function visibleCount(memories: readonly AiMemoryItem[], query: string) {
  return memories.filter((x) => matches(x, query)).length;
}
