'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph, PencilGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { IconButton } from '../../actions/button/index.js';
import type { AiAgentRef } from '../ai-conversation/index.js';

export interface AiConversationHeaderMessages {
  /** Shown when the title is empty. */
  untitled: string;
  /** Accessible name of the rename button. */
  rename: string;
  /** Accessible name of the title field. */
  titleLabel: string;
  /** Keyboard hint read with the title field. */
  editHint: string;
  /** Shown while the title is being generated. */
  naming: string;
  /** Visually hidden prefix before the agent's name. */
  agentPrefix: string;
  /** Visually hidden prefix before the model. */
  modelPrefix: string;
  /** Shown when a rename fails. */
  renameFailed: string;
  /** Accessible name of the actions group. */
  actions: string;
}

export const AI_CONVERSATION_HEADER_MESSAGES: AiConversationHeaderMessages = {
  untitled: 'New conversation',
  rename: 'Rename conversation',
  titleLabel: 'Conversation title',
  editHint: 'Enter to save, Escape to cancel.',
  naming: 'Naming conversation…',
  agentPrefix: 'AI assistant:',
  modelPrefix: 'Model:',
  renameFailed: 'Couldn’t rename. Try again.',
  actions: 'Conversation actions',
};

export interface AiConversationHeaderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The conversation's title, often generated from the first prompt. Empty shows "New conversation". */
  title: string;
  /**
   * Makes the title editable with a Rename button. Called with the trimmed new
   * title when it changed. Return a promise to show a pending state; reject it
   * to keep the editor open with an error.
   */
  onRename?: ((title: string) => void | Promise<void>) | undefined;
  /** The assistant answering, shown with the Bot mark. */
  agent?: AiAgentRef | undefined;
  /** The model in use, e.g. a name or an AiModelPicker. */
  model?: ReactNode | undefined;
  /** Trailing actions, e.g. Share and a more menu. */
  actions?: ReactNode | undefined;
  /** The title is being generated: shows a placeholder and marks the heading busy. */
  naming?: boolean | undefined;
  /** Heading level of the title. Default 2. */
  headingLevel?: 1 | 2 | 3 | 4 | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiConversationHeaderMessages> | undefined;
}

function isThenable(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { then?: unknown }).then === 'function'
  );
}

/**
 * The top bar of a conversation: its title (often named automatically from
 * the first prompt) with inline rename, the assistant and model in use, and a
 * slot for actions such as Share. Enter saves a rename, Escape cancels, and
 * focus returns to the Rename button.
 */
export const AiConversationHeader = forwardRef<HTMLDivElement, AiConversationHeaderProps>(
  function AiConversationHeader(
    {
      title,
      onRename,
      agent,
      model,
      actions,
      naming = false,
      headingLevel = 2,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_CONVERSATION_HEADER_MESSAGES, ...messagesProp };
    const baseId = useId();
    const hintId = `${baseId}-hint`;
    const errorId = `${baseId}-error`;
    const Heading = `h${headingLevel}` as const;
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(title);
    const [pending, setPending] = useState(false);
    const [failed, setFailed] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const renameRef = useRef<HTMLButtonElement>(null);
    const restoreFocus = useRef(false);
    const mounted = useRef(true);

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    useEffect(() => {
      if (editing) {
        inputRef.current?.focus();
        inputRef.current?.select();
      } else if (restoreFocus.current) {
        restoreFocus.current = false;
        renameRef.current?.focus();
      }
    }, [editing]);

    const startEditing = () => {
      setDraft(title);
      setFailed(false);
      setEditing(true);
    };

    const finish = (refocus: boolean) => {
      restoreFocus.current = refocus;
      setEditing(false);
      setFailed(false);
    };

    const save = async (refocus: boolean) => {
      if (pending) return;
      const next = draft.trim();
      if (!next || next === title.trim() || !onRename) {
        finish(refocus);
        return;
      }
      const result = onRename(next);
      if (!isThenable(result)) {
        finish(refocus);
        return;
      }
      setPending(true);
      try {
        await result;
        if (mounted.current) finish(refocus);
      } catch {
        if (!mounted.current) return;
        setFailed(true);
        inputRef.current?.focus();
      } finally {
        if (mounted.current) setPending(false);
      }
    };

    const onKeyDown = (event: ReactKeyboardEvent<HTMLInputElement>) => {
      if (event.nativeEvent.isComposing) return;
      if (event.key === 'Enter') {
        event.preventDefault();
        void save(true);
      } else if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        if (!pending) finish(true);
      }
    };

    const avatar = agent?.avatar && isSafeImageSrc(agent.avatar) ? agent.avatar : undefined;
    const shownTitle = title.trim() || m.untitled;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-conversation-header', className)}
        data-editing={editing || undefined}
        data-naming={naming || undefined}
      >
        <div className="mn-ai-conversation-header__main">
          {editing ? (
            <div className="mn-ai-conversation-header__editor">
              <input
                ref={inputRef}
                className="mn-ai-conversation-header__input"
                aria-label={m.titleLabel}
                aria-describedby={failed ? `${errorId} ${hintId}` : hintId}
                aria-invalid={failed || undefined}
                aria-busy={pending || undefined}
                readOnly={pending}
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={onKeyDown}
                onBlur={() => {
                  if (!failed) void save(false);
                }}
              />
              <span id={hintId} className="mn-visually-hidden">
                {m.editHint}
              </span>
              {failed ? (
                <p id={errorId} role="alert" className="mn-ai-conversation-header__error">
                  {m.renameFailed}
                </p>
              ) : null}
            </div>
          ) : (
            <div className="mn-ai-conversation-header__title-row">
              <Heading
                className="mn-ai-conversation-header__title"
                aria-busy={naming || undefined}
                data-untitled={!title.trim() || undefined}
              >
                {naming ? (
                  <span className="mn-ai-conversation-header__naming">{m.naming}</span>
                ) : (
                  shownTitle
                )}
              </Heading>
              {onRename && !naming ? (
                <IconButton
                  ref={renameRef}
                  size="sm"
                  label={m.rename}
                  icon={<PencilGlyph />}
                  className="mn-ai-conversation-header__rename"
                  onClick={startEditing}
                />
              ) : null}
            </div>
          )}
          {agent || model ? (
            <div className="mn-ai-conversation-header__meta">
              {agent ? (
                <span className="mn-ai-conversation-header__agent">
                  <span className="mn-ai-conversation-header__mark" aria-hidden>
                    {avatar ? (
                      <img className="mn-ai-conversation-header__avatar" src={avatar} alt="" />
                    ) : (
                      <BotGlyph />
                    )}
                  </span>
                  <span className="mn-visually-hidden">{m.agentPrefix} </span>
                  {agent.name}
                </span>
              ) : null}
              {model ? (
                <span className="mn-ai-conversation-header__model">
                  <span className="mn-visually-hidden">{m.modelPrefix} </span>
                  {model}
                </span>
              ) : null}
            </div>
          ) : null}
        </div>
        {actions ? (
          <div role="group" aria-label={m.actions} className="mn-ai-conversation-header__actions">
            {actions}
          </div>
        ) : null}
      </div>
    );
  },
);
