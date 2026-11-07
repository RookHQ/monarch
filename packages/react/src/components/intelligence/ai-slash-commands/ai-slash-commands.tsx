'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import type {
  AiPromptTrigger,
  AiPromptTriggerApi,
  AiPromptTriggerState,
} from '../ai-prompt-input/index.js';

// ------------------------------------------------------------------ shared menu wiring

/**
 * The textarea props a trigger menu needs: autocomplete ARIA and its keys.
 * The textarea keeps its textbox role (a `<textarea>` may not be a combobox),
 * like the mention picker's `useMentionTextarea`.
 */
export interface AiPromptMenuTextareaProps {
  'aria-autocomplete': 'list';
  'aria-controls': string | undefined;
  'aria-activedescendant': string | undefined;
  /** Arrow keys move, Enter and Tab pick; it calls `preventDefault()` only for keys it used. */
  onKeyDown: (event: ReactKeyboardEvent<HTMLTextAreaElement>) => void;
}

/**
 * What a trigger-menu hook returns: exactly the AiPromptInput props it needs,
 * so it can be spread straight onto the input (`<AiPromptInput {...menu} />`).
 * Combine several with `combineAiPromptMenus`.
 */
export interface AiPromptMenuExtension {
  triggers: AiPromptTrigger[];
  renderTriggerMenu: (state: AiPromptTriggerState) => ReactNode;
  textareaProps: AiPromptMenuTextareaProps;
}

/**
 * Merges trigger menus (for example `/` commands and `@` mentions) into one
 * set of AiPromptInput props: all triggers, each menu rendered for its own
 * trigger, the open menu's ARIA on the textarea, and every menu's keys.
 */
export function combineAiPromptMenus(
  first: AiPromptMenuExtension,
  ...rest: AiPromptMenuExtension[]
): AiPromptMenuExtension {
  const menus = [first, ...rest];
  const open = menus.find((m) => m.textareaProps['aria-controls']) ?? first;
  return {
    triggers: menus.flatMap((m) => m.triggers),
    renderTriggerMenu: (state) =>
      menus.find((m) => m.triggers.includes(state.trigger))?.renderTriggerMenu(state) ?? null,
    textareaProps: {
      ...open.textareaProps,
      onKeyDown: (event) => {
        for (const m of menus) {
          if (event.defaultPrevented) return;
          m.textareaProps.onKeyDown(event);
        }
      },
    },
  };
}

/** Id of a menu option, for `aria-activedescendant`. */
function optionId(listId: string, itemId: string): string {
  return `${listId}-opt-${itemId.replace(/[^\w-]/g, '_')}`;
}

/** Index of the next enabled entry, wrapping; -1 when none is enabled. */
function stepEnabled<T extends { disabled?: boolean | undefined }>(
  list: readonly T[],
  from: number,
  step: 1 | -1,
): number {
  const n = list.length;
  for (let k = 1; k <= n; k++) {
    const i = (((from + step * k) % n) + n) % n;
    if (!list[i]?.disabled) return i;
  }
  return -1;
}

// ------------------------------------------------------------------ data

/** One slash command. */
export interface AiSlashCommand {
  /** Stable id, used for option ids. */
  id: string;
  /** What is typed after the slash, e.g. `summarize`. A leading slash is ignored. */
  name: string;
  /** What it does, shown under the name and read with it. */
  description?: string | undefined;
  /** Decorative icon before the name. */
  icon?: ReactNode | undefined;
  /** Extra words the filter matches, e.g. `['tl;dr', 'shorten']`. */
  keywords?: readonly string[] | undefined;
  /** Shown but cannot be picked, e.g. a command that needs a selection. */
  disabled?: boolean | undefined;
}

const bare = (name: string) => name.replace(/^\/+/, '');

/**
 * Filters commands by what was typed after the slash: names that start with
 * the query first, then names that contain it, then matches in the
 * description or keywords. Case-insensitive; an empty query keeps all.
 */
export function filterAiSlashCommands(
  commands: readonly AiSlashCommand[],
  query: string,
): AiSlashCommand[] {
  const q = query.trim().toLowerCase();
  if (!q) return [...commands];
  const ranked: { command: AiSlashCommand; rank: number; index: number }[] = [];
  commands.forEach((command, index) => {
    const name = bare(command.name).toLowerCase();
    const rest = [command.description ?? '', ...(command.keywords ?? [])].join(' ').toLowerCase();
    const rank = name.startsWith(q) ? 0 : name.includes(q) ? 1 : rest.includes(q) ? 2 : -1;
    if (rank >= 0) ranked.push({ command, rank, index });
  });
  return ranked.sort((a, b) => a.rank - b.rank || a.index - b.index).map((r) => r.command);
}

// ------------------------------------------------------------------ listbox

/** Every string the slash command menu shows or announces. */
export interface AiSlashCommandsMessages {
  /** Name of the listbox. */
  label: string;
  /** Shown when nothing matches. */
  empty: (query: string) => string;
  /** Announced politely as the list changes. */
  count: (count: number) => string;
}

export const AI_SLASH_COMMANDS_MESSAGES: AiSlashCommandsMessages = {
  label: 'Commands',
  empty: (query) => `No commands match “${query}”`,
  count: (n) => (n === 1 ? '1 command' : `${n} commands`),
};

export interface AiSlashCommandsProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSelect' | 'id'
> {
  /** The commands to show, already filtered (see `filterAiSlashCommands`). */
  commands: readonly AiSlashCommand[];
  /** Picks a command (click or Enter from the input). */
  onSelect: (command: AiSlashCommand) => void;
  /** The highlighted command. Focus stays in the input, which points here with `aria-activedescendant`. */
  activeId?: string | null | undefined;
  /** Called when the pointer moves over a command. */
  onActiveChange?: ((id: string) => void) | undefined;
  /** The text typed after the trigger, for the empty message. */
  query?: string | undefined;
  /** The trigger character shown before each name. Default `/`. */
  char?: string | undefined;
  /** Id of the listbox, for `aria-controls`. Generated when omitted. */
  listId?: string | undefined;
  /** Override any string. */
  messages?: Partial<AiSlashCommandsMessages> | undefined;
}

/**
 * The slash command menu: a listbox of commands with icons, names and
 * descriptions, opened from AiPromptInput's `/` trigger. Focus stays in the
 * textarea; the highlighted option is announced through
 * `aria-activedescendant`. Use `useAiSlashCommands` to wire it up (filtering,
 * keys, insertion); render this directly only for a custom integration.
 */
export const AiSlashCommands = forwardRef<HTMLDivElement, AiSlashCommandsProps>(
  function AiSlashCommands(
    {
      commands,
      onSelect,
      activeId,
      onActiveChange,
      query = '',
      char = '/',
      listId: listIdProp,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_SLASH_COMMANDS_MESSAGES, ...messagesProp };
    const auto = `mn-ai-slash-${useId().replace(/:/g, '')}`;
    const listId = listIdProp ?? `${auto}-listbox`;
    return (
      <div ref={ref} {...rest} className={cx('mn-ai-slash-commands', className)}>
        {commands.length ? (
          <div
            role="listbox"
            id={listId}
            aria-label={m.label}
            className="mn-ai-slash-commands__list"
          >
            {commands.map((command) => {
              const active = command.id === activeId;
              const descId = command.description
                ? `${optionId(listId, command.id)}-desc`
                : undefined;
              return (
                // Options are never focused: focus stays in the textarea (aria-activedescendant), which handles keys.
                // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
                <div
                  key={command.id}
                  id={optionId(listId, command.id)}
                  role="option"
                  aria-selected={active}
                  aria-disabled={command.disabled || undefined}
                  aria-labelledby={`${optionId(listId, command.id)}-name`}
                  aria-describedby={descId}
                  data-highlighted={active || undefined}
                  data-disabled={command.disabled || undefined}
                  className="mn-ai-slash-commands__option"
                  onMouseDown={(e) => e.preventDefault()}
                  onMouseMove={() => {
                    if (!active && !command.disabled) onActiveChange?.(command.id);
                  }}
                  onClick={() => {
                    if (!command.disabled) onSelect(command);
                  }}
                >
                  {command.icon ? (
                    <span className="mn-ai-slash-commands__icon" aria-hidden>
                      {command.icon}
                    </span>
                  ) : null}
                  <span className="mn-ai-slash-commands__text">
                    <span
                      id={`${optionId(listId, command.id)}-name`}
                      className="mn-ai-slash-commands__name"
                    >
                      {`${char}${bare(command.name)}`}
                    </span>
                    {command.description ? (
                      <span id={descId} className="mn-ai-slash-commands__description">
                        {command.description}
                      </span>
                    ) : null}
                  </span>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="mn-ai-slash-commands__empty">{m.empty(`${char}${query}`)}</p>
        )}
        <span role="status" className="mn-visually-hidden">
          {commands.length ? m.count(commands.length) : ''}
        </span>
      </div>
    );
  },
);

// ------------------------------------------------------------------ hook

export interface UseAiSlashCommandsOptions {
  /** The trigger. Default `/`. */
  char?: string | undefined;
  /** Strings for the menu. */
  messages?: Partial<AiSlashCommandsMessages> | undefined;
  /** Class for the menu panel. */
  className?: string | undefined;
}

/**
 * Wires slash commands into AiPromptInput. Returns `triggers`,
 * `renderTriggerMenu` and `textareaProps` to spread onto the input. Typing
 * `/` opens a filtered AiSlashCommands menu; Arrow keys move, Enter or Tab
 * pick, Escape closes. `onSelect` may return text to put in place of the
 * typed `/query` (e.g. `'/summarize '`); return nothing to remove it.
 */
export function useAiSlashCommands(
  commands: readonly AiSlashCommand[],
  onSelect: (command: AiSlashCommand) => unknown,
  options: UseAiSlashCommandsOptions = {},
): AiPromptMenuExtension {
  const { char = '/', messages, className } = options;
  const listId = `mn-ai-slash-${useId().replace(/:/g, '')}-listbox`;
  const [session, setSession] = useState<{ query: string; api: AiPromptTriggerApi } | null>(null);
  const [activeId, setActiveId] = useState<string | null>(null);
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

  const matches = useMemo(
    () => (session ? filterAiSlashCommands(commands, session.query) : []),
    [commands, session],
  );
  const active =
    matches.find((c) => c.id === activeId && !c.disabled) ?? matches.find((c) => !c.disabled);

  const pick = useCallback(
    (command: AiSlashCommand) => {
      if (!session || command.disabled) return;
      const text = onSelectRef.current(command);
      session.api.insert(typeof text === 'string' ? text : '');
    },
    [session],
  );

  const expanded = Boolean(session && matches.length);

  const onKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if (!session || !active || event.nativeEvent.isComposing) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const next = stepEnabled(
        matches,
        matches.indexOf(active),
        event.key === 'ArrowDown' ? 1 : -1,
      );
      const command = matches[next];
      if (command) setActiveId(command.id);
    } else if (event.key === 'Enter' || event.key === 'Tab') {
      if (event.shiftKey) return;
      event.preventDefault();
      pick(active);
    }
  };

  return {
    triggers: [trigger],
    renderTriggerMenu: (state) =>
      state.trigger === trigger ? (
        <AiSlashCommands
          className={className}
          commands={matches}
          activeId={active?.id ?? null}
          onActiveChange={setActiveId}
          onSelect={pick}
          query={state.query}
          char={char}
          listId={listId}
          messages={messages}
        />
      ) : null,
    textareaProps: {
      'aria-autocomplete': 'list',
      'aria-controls': expanded ? listId : undefined,
      'aria-activedescendant': expanded && active ? optionId(listId, active.id) : undefined,
      onKeyDown,
    },
  };
}
