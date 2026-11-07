'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ownsOpenPopup } from '../../../internal/dismiss.js';
import { ChevronDownGlyph, CloseGlyph, BotGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import type { AiAgentRef, AiMessage, UseAiConversationResult } from '../ai-conversation/index.js';
import { AiThread, type AiThreadProps } from '../ai-thread/index.js';
import type { AiMessageViewProps } from '../ai-message/index.js';
import {
  AiPromptInput,
  type AiPromptInputProps,
  type AiPromptSubmitDetails,
} from '../ai-prompt-input/index.js';

/** Every string AiAssistantPanel shows or announces. English defaults. */
export interface AiAssistantPanelMessages {
  close: string;
  minimize: string;
  expand: string;
  /** Accessible name of the prompt input. */
  inputLabel: string;
  /** Accessible name of the context chip list. */
  context: string;
  /** Shown above the starters when the conversation is empty. */
  welcome: string;
  /** Accessible name of the starter list. */
  starters: string;
}

const DEFAULT_MESSAGES: AiAssistantPanelMessages = {
  close: 'Close assistant',
  minimize: 'Minimize assistant',
  expand: 'Expand assistant',
  inputLabel: 'Message the assistant',
  context: 'Context',
  welcome: 'How can I help?',
  starters: 'Suggested prompts',
};

/** One context chip: what the assistant can see on the current page. */
export interface AiAssistantContextItem {
  /** Short kind, e.g. "Page" or "Selected". */
  label: string;
  /** The value, e.g. the record's name or "3 rows". */
  items: ReactNode;
}

/** A suggested first prompt. A string is both the label and the prompt. */
export type AiAssistantStarter = string | { label: string; prompt?: string | undefined };

export interface AiAssistantPanelProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'title' | 'children'
> {
  /** The `useAiConversation` result the panel shows and sends to. */
  conversation: UseAiConversationResult;
  /** Heading of the panel, e.g. "Assistant". Names the panel for assistive technology. */
  title: ReactNode;
  /** The agent shown in the header and on replies. Default: `conversation.agent`. */
  agent?: AiAgentRef | undefined;
  /** Chips that show what the assistant can see on the current page. */
  context?: readonly AiAssistantContextItem[] | undefined;
  /** Whether the panel is shown. */
  open: boolean;
  /** Called with `false` by the close button and by Escape (when nothing is running). */
  onOpenChange: (open: boolean) => void;
  /** Collapse the panel to its header. Uncontrolled when left out. */
  minimized?: boolean | undefined;
  /** Called by the minimize / expand button. */
  onMinimizedChange?: ((minimized: boolean) => void) | undefined;
  /** Suggested prompts shown while the conversation is empty. Choosing one sends it. */
  starters?: readonly AiAssistantStarter[] | undefined;
  /** Replaces the whole empty state (welcome and starters). */
  empty?: ReactNode | undefined;
  /** Controls in the prompt bar, e.g. a model or mode picker. */
  toolbar?: ReactNode | undefined;
  /** Under the prompt input, e.g. an `AiDisclaimer`. */
  footer?: ReactNode | undefined;
  /** Extra header buttons before minimize and close, e.g. "New conversation". */
  headerActions?: ReactNode | undefined;
  /** Which edge the floating panel docks to. Default `right`. */
  side?: 'right' | 'left' | undefined;
  /**
   * `fixed` (default): docks to the viewport edge and becomes a full-screen
   * modal sheet below 640px. `inline`: fills its container, for a layout that
   * reserves a rail column.
   */
  placement?: 'fixed' | 'inline' | undefined;
  /** Escape closes the panel when no reply is running. Default true. */
  closeOnEscape?: boolean | undefined;
  /** Keep the panel mounted (hidden) while closed, so the draft and scroll position survive. Default true. */
  keepMounted?: boolean | undefined;
  /** Replaces sending: called with the prompt instead of `conversation.send`. */
  onSend?: ((text: string, details: AiPromptSubmitDetails) => unknown) | undefined;
  /** Props for each `AiMessageView`, merged over the panel's wiring (regenerate, feedback, branches, retry). */
  messageProps?:
    | Partial<Omit<AiMessageViewProps, 'message'>>
    | ((message: AiMessage) => Partial<Omit<AiMessageViewProps, 'message'>>)
    | undefined;
  /** Extra props for the `AiThread`. */
  threadProps?:
    Partial<Omit<AiThreadProps, 'messages' | 'status' | 'error' | 'onStop'>> | undefined;
  /** Extra props for the `AiPromptInput`, e.g. `onAttach`, `maxLength`, `triggers`. */
  promptProps?:
    | Partial<
        Omit<AiPromptInputProps, 'onSubmit' | 'status' | 'onStop' | 'label' | 'toolbar' | 'footer'>
      >
    | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiAssistantPanelMessages> | undefined;
}

const COMPACT_QUERY = '(max-width: 639.98px)';

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/** True below 640px, read after mount (false on the server). */
function useCompact(enabled: boolean): boolean {
  const [compact, setCompact] = useState(false);
  useEffect(() => {
    if (!enabled || typeof window === 'undefined' || !window.matchMedia) {
      setCompact(false);
      return;
    }
    const mq = window.matchMedia(COMPACT_QUERY);
    const update = () => setCompact(mq.matches);
    update();
    mq.addEventListener?.('change', update);
    return () => mq.removeEventListener?.('change', update);
  }, [enabled]);
  return compact;
}

/**
 * The assistant rail any app mounts: a header with the agent and minimize /
 * close buttons, chips for the context of the current page, and an `AiThread`
 * over an `AiPromptInput`, both wired to a `useAiConversation` result. Escape
 * stops a running reply (and otherwise closes), opening focuses the input and
 * closing returns focus to where it was. Below 640px it becomes a full-screen
 * modal sheet. Pair it with `AiLauncher`, which owns the keyboard shortcut.
 */
export const AiAssistantPanel = forwardRef<HTMLElement, AiAssistantPanelProps>(
  function AiAssistantPanel(
    {
      conversation,
      title,
      agent: agentProp,
      context,
      open,
      onOpenChange,
      minimized: minimizedProp,
      onMinimizedChange,
      starters,
      empty,
      toolbar,
      footer,
      headerActions,
      side = 'right',
      placement = 'fixed',
      closeOnEscape = true,
      keepMounted = true,
      onSend,
      messageProps,
      threadProps,
      promptProps,
      messages: messagesProp,
      className,
      onKeyDown,
      id: idProp,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const autoId = useId();
    const id = idProp ?? `mn-ai-assistant-panel-${autoId.replace(/:/g, '')}`;
    const titleId = `${id}-title`;
    const bodyId = `${id}-body`;
    const rootRef = useRef<HTMLElement | null>(null);
    const inputRef = useRef<HTMLTextAreaElement | null>(null);
    const returnTo = useRef<HTMLElement | null>(null);

    const [minInner, setMinInner] = useState(false);
    const minimized = minimizedProp ?? minInner;
    const setMinimized = (next: boolean) => {
      if (minimizedProp === undefined) setMinInner(next);
      onMinimizedChange?.(next);
    };

    const compact = useCompact(placement === 'fixed');
    const modal = compact && open && !minimized;
    const agent = agentProp ?? conversation.agent;

    // ------------------------------------------------------------ focus
    const prevOpen = useRef(open);
    useEffect(() => {
      const was = prevOpen.current;
      prevOpen.current = open;
      if (open && !was) {
        const active = document.activeElement;
        returnTo.current =
          active instanceof HTMLElement && active !== document.body ? active : null;
        if (!minimized) inputRef.current?.focus();
      } else if (!open && was) {
        const active = document.activeElement;
        const root = rootRef.current;
        const lost = !active || active === document.body || (root && root.contains(active));
        const target = returnTo.current;
        returnTo.current = null;
        if (lost && target && target.isConnected) target.focus();
      }
      // `minimized` is read for the moment of opening only.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const prevMinimized = useRef(minimized);
    useEffect(() => {
      const was = prevMinimized.current;
      prevMinimized.current = minimized;
      if (open && was && !minimized) inputRef.current?.focus();
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [minimized]);

    // ------------------------------------------------------------ keys
    const handleKeyDown = (e: ReactKeyboardEvent<HTMLElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented) return;
      const root = rootRef.current;
      // Events from portalled content (menus, popovers) bubble through React
      // but belong to their own layer.
      if (!root || !(e.target instanceof Node) || !root.contains(e.target)) return;
      if (e.key === 'Escape') {
        if (ownsOpenPopup(e.target)) return;
        if (conversation.busy) {
          e.preventDefault();
          conversation.stop();
        } else if (closeOnEscape) {
          e.preventDefault();
          onOpenChange(false);
        }
        return;
      }
      // Below 640px the panel is modal: keep Tab inside it.
      if (e.key === 'Tab' && modal) {
        const items = Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
          (el) => !el.closest('[hidden]'),
        );
        const first = items[0];
        const last = items[items.length - 1];
        if (!first || !last) return;
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };

    if (!open && !keepMounted) return null;

    // ------------------------------------------------------------ parts
    const send = (text: string, details: AiPromptSubmitDetails) =>
      onSend ? onSend(text, details) : conversation.send(text);

    const starterList = starters?.map((s) =>
      typeof s === 'string'
        ? { label: s, prompt: s }
        : { label: s.label, prompt: s.prompt ?? s.label },
    );
    const emptyState =
      empty ??
      (starterList?.length ? (
        <div className="mn-ai-assistant-panel__welcome">
          <p className="mn-ai-assistant-panel__welcome-title">{m.welcome}</p>
          <ul className="mn-ai-assistant-panel__starters" aria-label={m.starters}>
            {starterList.map((s) => (
              <li key={s.label}>
                <Button
                  size="sm"
                  className="mn-ai-assistant-panel__starter"
                  disabled={conversation.busy}
                  onClick={() => {
                    void send(s.prompt, { files: [] });
                    inputRef.current?.focus();
                  }}
                >
                  {s.label}
                </Button>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <p className="mn-ai-assistant-panel__welcome-title">{m.welcome}</p>
      ));

    const wiredMessageProps = (
      message: AiMessage,
    ): Partial<Omit<AiMessageViewProps, 'message'>> => {
      const extra = typeof messageProps === 'function' ? messageProps(message) : messageProps;
      return {
        agent,
        onRegenerate: (msg) => conversation.regenerate(msg.id),
        onFeedback: (f, msg) => conversation.setFeedback(msg.id, f),
        onBranch: (i, msg) => conversation.branchTo(msg.id, i),
        onRetry: (msg) => conversation.regenerate(msg.id),
        ...extra,
      };
    };

    const name = agent?.name;

    return (
      // A non-modal dialog beside the page; modal (aria-modal) below 640px.
      // Escape (stop / close) and the compact focus wrap are handled here.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
      <section
        ref={(node) => {
          rootRef.current = node;
          setRef(ref, node);
        }}
        id={id}
        aria-labelledby={titleId}
        role="dialog"
        aria-modal={modal || undefined}
        {...rest}
        hidden={!open}
        className={cx('mn-ai-assistant-panel', className)}
        data-side={side}
        data-placement={placement}
        data-minimized={minimized || undefined}
        data-compact={compact || undefined}
        data-status={conversation.status}
        onKeyDown={handleKeyDown}
      >
        <header className="mn-ai-assistant-panel__header">
          <span className="mn-ai-assistant-panel__avatar">
            <Avatar
              name={name ?? 'Assistant'}
              src={agent?.avatar ?? null}
              size="sm"
              decorative
              fallback="icon"
              icon={<BotGlyph />}
            />
          </span>
          <div className="mn-ai-assistant-panel__heading">
            <h2 id={titleId} className="mn-ai-assistant-panel__title">
              {title}
            </h2>
            {name || agent?.role ? (
              <p className="mn-ai-assistant-panel__agent">
                {[name, agent?.role].filter(Boolean).join(' · ')}
              </p>
            ) : null}
          </div>
          <div className="mn-ai-assistant-panel__actions">
            {headerActions}
            <IconButton
              size="sm"
              label={minimized ? m.expand : m.minimize}
              icon={<ChevronDownGlyph />}
              className="mn-ai-assistant-panel__minimize"
              aria-expanded={!minimized}
              aria-controls={bodyId}
              onClick={() => setMinimized(!minimized)}
            />
            <IconButton
              size="sm"
              label={m.close}
              icon={<CloseGlyph />}
              className="mn-ai-assistant-panel__close"
              onClick={() => onOpenChange(false)}
            />
          </div>
        </header>
        <div id={bodyId} className="mn-ai-assistant-panel__body" hidden={minimized}>
          {context?.length ? (
            <ul className="mn-ai-assistant-panel__context" aria-label={m.context}>
              {context.map((c, i) => (
                <li key={`${c.label}-${i}`} className="mn-ai-assistant-panel__chip">
                  <span className="mn-ai-assistant-panel__chip-label">{c.label}</span>{' '}
                  <span className="mn-ai-assistant-panel__chip-value">{c.items}</span>
                </li>
              ))}
            </ul>
          ) : null}
          <AiThread
            {...threadProps}
            className={cx('mn-ai-assistant-panel__thread', threadProps?.className)}
            messages={conversation.messages}
            status={conversation.status}
            error={conversation.error}
            onStop={conversation.stop}
            empty={emptyState}
            messageProps={wiredMessageProps}
          />
          <div className="mn-ai-assistant-panel__composer">
            <AiPromptInput
              {...promptProps}
              label={m.inputLabel}
              status={conversation.status}
              onStop={conversation.stop}
              onSubmit={send}
              textareaRef={inputRef}
              {...(toolbar !== undefined ? { toolbar } : {})}
              {...(footer !== undefined ? { footer } : {})}
            />
          </div>
        </div>
      </section>
    );
  },
);
