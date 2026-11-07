'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import {
  AiContextKindGlyph,
  formatAiContextTokens,
  type AiContextChip,
  type AiContextChipKind,
} from '../ai-context-chips/index.js';
import type { AiPromptMenuExtension } from '../ai-slash-commands/index.js';
import type {
  AiPromptTrigger,
  AiPromptTriggerApi,
  AiPromptTriggerState,
} from '../ai-prompt-input/index.js';

// ------------------------------------------------------------------ data

/** What a mention points at. */
export type AiMentionKind = 'doc' | 'person' | 'record' | 'app';

/** One search result in the `@` menu. */
export interface AiMentionItem {
  /** Stable id: becomes the context chip's id. */
  id: string;
  kind: AiMentionKind;
  /** Display name. */
  label: string;
  /** Secondary line: a path, a role, a record type. */
  description?: string | undefined;
  /** Estimated tokens it would add to the context. */
  tokens?: number | undefined;
  /** Avatar image for people. Initials are shown without it. */
  avatarSrc?: string | null | undefined;
  /** Shown but cannot be picked, e.g. a document the assistant may not read. */
  disabled?: boolean | undefined;
}

/**
 * Finds mentionable things for a query. Return an array or a promise of one;
 * abort the request when `signal` fires (the person kept typing or closed the menu).
 */
export type AiMentionSearch = (
  query: string,
  context: { signal: AbortSignal },
) => readonly AiMentionItem[] | Promise<readonly AiMentionItem[]>;

/** Where the menu's search is. */
export type AiMentionStatus = 'idle' | 'loading' | 'ready' | 'error';

const KIND_ORDER: readonly AiMentionKind[] = ['doc', 'person', 'record', 'app'];
const CHIP_KIND: Record<AiMentionKind, AiContextChipKind> = {
  doc: 'file',
  person: 'person',
  record: 'record',
  app: 'app',
};

/** Turns a picked mention into a context chip for AiContextChips. Documents become `file` chips. */
export function aiMentionToContextChip(item: AiMentionItem): AiContextChip {
  return {
    id: item.id,
    kind: CHIP_KIND[item.kind],
    label: item.label,
    ...(item.tokens !== undefined ? { tokens: item.tokens } : {}),
    ...(item.description !== undefined ? { description: item.description } : {}),
  };
}

function optionId(listId: string, itemId: string): string {
  return `${listId}-opt-${itemId.replace(/[^\w-]/g, '_')}`;
}

// ------------------------------------------------------------------ menu

/** Every string the mention menu shows or announces. */
export interface AiMentionContextMessages {
  /** Name of the listbox. */
  label: string;
  /** Group headings, shown when results have more than one kind. */
  groups: Record<AiMentionKind, string>;
  loading: string;
  /** Shown when nothing matches. */
  empty: (query: string) => string;
  error: string;
  retry: string;
  /** A result's token estimate; `formatted` is the compact number. */
  tokens: (formatted: string, count: number) => string;
  /** Announced politely when results arrive. */
  count: (count: number) => string;
}

export const AI_MENTION_CONTEXT_MESSAGES: AiMentionContextMessages = {
  label: 'Add context',
  groups: { doc: 'Documents', person: 'People', record: 'Records', app: 'Apps' },
  loading: 'Searching…',
  empty: (query) => (query ? `Nothing matches “${query}”` : 'Type to search'),
  error: 'Couldn’t load results.',
  retry: 'Retry',
  tokens: (formatted, n) => (n === 1 ? '1 token' : `${formatted} tokens`),
  count: (n) => (n === 1 ? '1 result' : `${n} results`),
};

type MessagesProp =
  | (Partial<Omit<AiMentionContextMessages, 'groups'>> & {
      groups?: Partial<Record<AiMentionKind, string>>;
    })
  | undefined;

const mergeMessages = (m: MessagesProp): AiMentionContextMessages => ({
  ...AI_MENTION_CONTEXT_MESSAGES,
  ...m,
  groups: { ...AI_MENTION_CONTEXT_MESSAGES.groups, ...m?.groups },
});

export interface AiMentionContextProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSelect' | 'id'
> {
  /** The results to show. They are grouped by kind (documents, people, records, apps). */
  items: readonly AiMentionItem[];
  /** Where the search is. Default `ready`. */
  status?: AiMentionStatus | undefined;
  /** Picks a result (click, or Enter from the input). */
  onSelect: (item: AiMentionItem) => void;
  /** The highlighted result. Focus stays in the input, which points here with `aria-activedescendant`. */
  activeId?: string | null | undefined;
  /** Called when the pointer moves over a result. */
  onActiveChange?: ((id: string) => void) | undefined;
  /** Repeats a failed search. Shows a Retry button in the error state. */
  onRetry?: (() => void) | undefined;
  /** The text typed after `@`, for the empty message. */
  query?: string | undefined;
  /** Id of the listbox, for `aria-controls`. Generated when omitted. */
  listId?: string | undefined;
  /** Locale for token numbers. Default `en`. */
  locale?: string | undefined;
  /** Override any string. `groups` merges per key. */
  messages?: MessagesProp;
}

/**
 * The `@` context menu: search results for documents, people, records and
 * apps, grouped by kind, with loading, empty and error states. Picking one
 * adds it to what the AI will see (as an AiContextChips chip). Focus stays in
 * the prompt; use `useAiMentionContext` to wire it into AiPromptInput.
 */
export const AiMentionContext = forwardRef<HTMLDivElement, AiMentionContextProps>(
  function AiMentionContext(
    {
      items,
      status = 'ready',
      onSelect,
      activeId,
      onActiveChange,
      onRetry,
      query = '',
      listId: listIdProp,
      locale = 'en',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = mergeMessages(messagesProp);
    const auto = `mn-ai-mention-${useId().replace(/:/g, '')}`;
    const listId = listIdProp ?? `${auto}-listbox`;
    const groups = KIND_ORDER.map((kind) => ({
      kind,
      items: items.filter((i) => i.kind === kind),
    })).filter((g) => g.items.length);
    const grouped = groups.length > 1;

    const option = (item: AiMentionItem) => {
      const active = item.id === activeId;
      const descId = item.description ? `${optionId(listId, item.id)}-desc` : undefined;
      return (
        // Options are never focused: focus stays in the textarea (aria-activedescendant), which handles keys.
        // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
        <div
          key={item.id}
          id={optionId(listId, item.id)}
          role="option"
          aria-selected={active}
          aria-disabled={item.disabled || undefined}
          aria-labelledby={`${optionId(listId, item.id)}-label`}
          aria-describedby={descId}
          data-highlighted={active || undefined}
          data-disabled={item.disabled || undefined}
          data-kind={item.kind}
          className="mn-ai-mention-context__option"
          onMouseDown={(e) => e.preventDefault()}
          onMouseMove={() => {
            if (!active && !item.disabled) onActiveChange?.(item.id);
          }}
          onClick={() => {
            if (!item.disabled) onSelect(item);
          }}
        >
          <span className="mn-ai-mention-context__icon" aria-hidden>
            {item.kind === 'person' ? (
              <Avatar name={item.label} src={item.avatarSrc ?? null} size="xs" decorative />
            ) : (
              <AiContextKindGlyph kind={CHIP_KIND[item.kind]} />
            )}
          </span>
          <span className="mn-ai-mention-context__text">
            <span
              id={`${optionId(listId, item.id)}-label`}
              className="mn-ai-mention-context__label"
            >
              {item.label}
            </span>
            {item.description ? (
              <span id={descId} className="mn-ai-mention-context__description">
                {item.description}
              </span>
            ) : null}
          </span>
          {typeof item.tokens === 'number' ? (
            <span className="mn-ai-mention-context__tokens" aria-hidden>
              {m.tokens(formatAiContextTokens(item.tokens, locale), item.tokens)}
            </span>
          ) : null}
        </div>
      );
    };

    const ready = status === 'ready' && items.length > 0;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-mention-context', className)}
        data-status={status}
      >
        {ready ? (
          <div
            role="listbox"
            id={listId}
            aria-label={m.label}
            className="mn-ai-mention-context__list"
          >
            {grouped
              ? groups.map((g) => (
                  <div
                    key={g.kind}
                    role="group"
                    aria-labelledby={`${listId}-group-${g.kind}`}
                    className="mn-ai-mention-context__group"
                  >
                    <div
                      role="presentation"
                      id={`${listId}-group-${g.kind}`}
                      className="mn-ai-mention-context__group-label"
                    >
                      {m.groups[g.kind]}
                    </div>
                    {g.items.map(option)}
                  </div>
                ))
              : items.map(option)}
          </div>
        ) : (
          <div className="mn-ai-mention-context__message" data-status={status}>
            {status === 'loading' ? (
              <>
                <Spinner size="sm" />
                <span>{m.loading}</span>
              </>
            ) : status === 'error' ? (
              <>
                <span>{m.error}</span>
                {onRetry ? (
                  <button
                    type="button"
                    className="mn-ai-mention-context__retry"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={onRetry}
                  >
                    {m.retry}
                  </button>
                ) : null}
              </>
            ) : (
              <span>{m.empty(query)}</span>
            )}
          </div>
        )}
        <span role="status" className="mn-visually-hidden">
          {status === 'loading'
            ? m.loading
            : status === 'error'
              ? m.error
              : status === 'ready'
                ? items.length
                  ? m.count(items.length)
                  : m.empty(query)
                : ''}
        </span>
      </div>
    );
  },
);

// ------------------------------------------------------------------ hook

export interface UseAiMentionContextOptions {
  /** Finds results for the text typed after `@`. Called after `debounceMs`, also for an empty query. */
  search: AiMentionSearch;
  /**
   * Called with the picked result. Add it to your context list (see
   * `aiMentionToContextChip`). Return text to put in place of the typed
   * `@query`; return nothing to remove it.
   */
  onSelect: (item: AiMentionItem) => unknown;
  /** Ids already in the context; they are left out of the results. */
  exclude?: readonly string[] | undefined;
  /** The trigger. Default `@`. */
  char?: string | undefined;
  /** Wait this long after typing before searching. Default 150 ms. */
  debounceMs?: number | undefined;
  /** Strings for the menu. */
  messages?: MessagesProp;
  /** Locale for token numbers. Default `en`. */
  locale?: string | undefined;
  /** Class for the menu panel. */
  className?: string | undefined;
}

/**
 * Wires the `@` context menu into AiPromptInput. Returns `triggers`,
 * `renderTriggerMenu` and `textareaProps` to spread onto the input (combine
 * with slash commands through `combineAiPromptMenus`). Typing `@` searches
 * with debounce and abort; Arrow keys move, Enter or Tab pick, Escape closes.
 */
export function useAiMentionContext({
  search,
  onSelect,
  exclude,
  char = '@',
  debounceMs = 150,
  messages,
  locale,
  className,
}: UseAiMentionContextOptions): AiPromptMenuExtension {
  const listId = `mn-ai-mention-${useId().replace(/:/g, '')}-listbox`;
  const [session, setSession] = useState<{ query: string; api: AiPromptTriggerApi } | null>(null);
  const [results, setResults] = useState<readonly AiMentionItem[]>([]);
  const [status, setStatus] = useState<AiMentionStatus>('idle');
  const [activeId, setActiveId] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const searchRef = useRef(search);
  searchRef.current = search;
  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;

  const trigger = useMemo<AiPromptTrigger>(
    () => ({
      char,
      onTrigger: (query, api) => {
        setSession({ query, api });
        setActiveId(null);
      },
      onDismiss: () => setSession(null),
    }),
    [char],
  );

  const query = session?.query ?? null;
  useEffect(() => {
    if (query === null) {
      setResults([]);
      setStatus('idle');
      return;
    }
    const controller = new AbortController();
    setStatus('loading');
    const timer = setTimeout(() => {
      let result: readonly AiMentionItem[] | Promise<readonly AiMentionItem[]>;
      try {
        result = searchRef.current(query, { signal: controller.signal });
      } catch {
        setStatus('error');
        return;
      }
      Promise.resolve(result).then(
        (found) => {
          if (controller.signal.aborted) return;
          setResults(found);
          setStatus('ready');
        },
        () => {
          if (!controller.signal.aborted) setStatus('error');
        },
      );
    }, debounceMs);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query, debounceMs, attempt]);

  const items = useMemo(() => {
    const skip = new Set(exclude ?? []);
    return results.filter((r) => !skip.has(r.id));
  }, [results, exclude]);
  // Keyboard order follows the grouped display order.
  const ordered = useMemo(
    () => KIND_ORDER.flatMap((kind) => items.filter((i) => i.kind === kind)),
    [items],
  );
  const enabled = ordered.filter((i) => !i.disabled);
  const active = enabled.find((i) => i.id === activeId) ?? enabled[0];

  const pick = useCallback(
    (item: AiMentionItem) => {
      if (!session || item.disabled) return;
      const text = onSelectRef.current(item);
      session.api.insert(typeof text === 'string' ? text : '');
    },
    [session],
  );

  const listed = Boolean(session && status === 'ready' && items.length);

  const onKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if (!listed || !active || event.nativeEvent.isComposing) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const i = enabled.indexOf(active);
      const n = enabled.length;
      const next = enabled[(i + (event.key === 'ArrowDown' ? 1 : -1) + n) % n];
      if (next) setActiveId(next.id);
    } else if ((event.key === 'Enter' || event.key === 'Tab') && !event.shiftKey) {
      event.preventDefault();
      pick(active);
    }
  };

  return {
    triggers: [trigger],
    renderTriggerMenu: (state: AiPromptTriggerState): ReactNode =>
      state.trigger === trigger ? (
        <AiMentionContext
          className={className}
          items={ordered}
          status={status === 'idle' ? 'loading' : status}
          activeId={active?.id ?? null}
          onActiveChange={setActiveId}
          onSelect={pick}
          onRetry={() => setAttempt((n) => n + 1)}
          query={state.query}
          listId={listId}
          locale={locale}
          messages={messages}
        />
      ) : null,
    textareaProps: {
      'aria-autocomplete': 'list',
      'aria-controls': listed ? listId : undefined,
      'aria-activedescendant': listed && active ? optionId(listId, active.id) : undefined,
      onKeyDown,
    },
  };
}
