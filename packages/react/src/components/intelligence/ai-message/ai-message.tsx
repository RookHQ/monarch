'use client';

import {
  forwardRef,
  Fragment,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CheckGlyph,
  ChevronRightGlyph,
  CloseGlyph,
  DashGlyph,
  ErrorGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { writeToClipboard } from '../../actions/copy-button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import {
  isStreaming,
  messageText,
  type AiAgentRef,
  type AiFilePart,
  type AiMessage,
  type AiPart,
  type AiReasoningPart,
  type AiSourcePart,
  type AiToolCallPart,
  type AiToolCallState,
} from '../ai-conversation/index.js';
import { AiErrorMessage } from '../ai-error-message/index.js';
import {
  AiMessageActions,
  type AiFeedback,
  type AiMessageActionsMessages,
} from '../ai-message-actions/index.js';
import { AiStreamingText } from '../ai-streaming-text/index.js';

/** The lucide-style Bot glyph; Monarch never uses sparkles for AI. */
function BotGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
      {...props}
    >
      <path d="M8 2.5V5" />
      <rect x="3" y="5" width="10" height="8" rx="2" />
      <path d="M1.5 9h1.5M13 9h1.5M6 8.25v1.25M10 8.25v1.25" />
    </svg>
  );
}

function FileGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
      {...props}
    >
      <path d="M9 1.75H4.5a1.25 1.25 0 0 0-1.25 1.25v10a1.25 1.25 0 0 0 1.25 1.25h7a1.25 1.25 0 0 0 1.25-1.25V5.5L9 1.75z" />
      <path d="M9 1.75V5.5h3.75" />
    </svg>
  );
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiMessageViewMessages {
  /** Visually hidden prefix identifying an assistant message to screen readers. */
  assistantSaid: string;
  /** Visually hidden prefix of the person's own message. */
  youSaid: string;
  /** Prefix of a system or tool message. */
  systemSaid: string;
  /** Name shown when the message has no `agent`. */
  assistant: string;
  /** Reasoning summary while it streams. */
  thinking: string;
  /** Reasoning summary once done, with a duration. */
  thoughtFor: (seconds: number) => string;
  /** Reasoning summary once done, without a duration. */
  thought: string;
  /** Summary of redacted reasoning. */
  reasoningHidden: string;
  /** Tool-call state text. */
  toolStates: Record<AiToolCallState, string>;
  /** Tool-call duration. */
  duration: (seconds: number) => string;
  /** Heading of the sources list. */
  sources: string;
  /** Accessible name of an inline citation marker. */
  citation: (n: number) => string;
  /** Accessible name of the branch switcher group. */
  versions: string;
  previousVersion: string;
  nextVersion: string;
  /** Visible branch position, e.g. "2 / 3". */
  versionOf: (index: number, count: number) => string;
  /** Accessible name of the edit textarea. */
  editLabel: string;
  editHint: string;
  save: string;
  cancel: string;
  /** Labels of the action toolbar. */
  actions?: Partial<AiMessageActionsMessages> | undefined;
}

const DEFAULT_MESSAGES: Omit<AiMessageViewMessages, 'actions'> = {
  assistantSaid: 'Assistant said:',
  youSaid: 'You said:',
  systemSaid: 'System message:',
  assistant: 'Assistant',
  thinking: 'Thinking…',
  thoughtFor: (s) => `Thought for ${s}s`,
  thought: 'Thought',
  reasoningHidden: 'Reasoning hidden',
  toolStates: {
    'pending-approval': 'Waiting for approval',
    running: 'Running',
    succeeded: 'Done',
    failed: 'Failed',
    denied: 'Denied',
    cancelled: 'Cancelled',
  },
  duration: (s) => `${s}s`,
  sources: 'Sources',
  citation: (n) => `Source ${n}`,
  versions: 'Versions',
  previousVersion: 'Previous version',
  nextVersion: 'Next version',
  versionOf: (i, n) => `${i} / ${n}`,
  editLabel: 'Edit message',
  editHint: 'Enter to save, Shift+Enter for a new line, Escape to cancel.',
  save: 'Save',
  cancel: 'Cancel',
};

export interface AiMessageViewProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'children' | 'onCopy'
> {
  /** The message to render. */
  message: AiMessage;
  /** Agent shown when `message.agent` is not set (e.g. `useAiConversation().agent`). */
  agent?: AiAgentRef | undefined;
  /**
   * Renders a part yourself. Return `undefined` to keep the default rendering
   * and `null` to render nothing. Parts without a default (artifact, plan,
   * edit-proposal, ui) render only through this.
   */
  renderPart?:
    ((part: AiPart, index: number, message: AiMessage) => ReactNode | undefined) | undefined;
  /**
   * Whether streamed text announces itself to screen readers. Default true;
   * `AiThread` passes false because it owns one announcer for the thread.
   */
  announce?: boolean | undefined;
  /** Shows the time the message was created. Default true. */
  showTimestamp?: boolean | undefined;
  /** BCP 47 locale for the timestamp and file sizes. */
  locale?: string | undefined;
  /** Shows the action toolbar (hidden while the message streams). Default true. */
  showActions?: boolean | undefined;
  /** Toolbar visibility; see `AiMessageActions`. Default `hover`. */
  actionsVisibility?: 'hover' | 'always' | undefined;
  /**
   * Replaces the built-in Copy (which writes the message's text to the
   * clipboard). Pass `false` to hide Copy.
   */
  onCopy?: ((text: string, message: AiMessage) => void | Promise<void>) | false | undefined;
  /** Shows Regenerate on assistant messages. */
  onRegenerate?: ((message: AiMessage) => void) | undefined;
  /** Shows Edit on the person's messages; usually sets `editing`. */
  onEdit?: ((message: AiMessage) => void) | undefined;
  /** Shows thumbs on assistant messages; pressed state comes from `message.feedback`. */
  onFeedback?: ((feedback: AiFeedback | undefined, message: AiMessage) => void) | undefined;
  /** Shows Share. */
  onShare?: ((message: AiMessage) => void) | undefined;
  /** Shows Read aloud on assistant messages (where speech synthesis exists). */
  onReadAloud?: ((speaking: boolean, message: AiMessage) => void) | undefined;
  /** Extra toolbar controls. */
  moreActions?: ReactNode | undefined;
  /** Switches between alternatives when `message.branch` is set; receives the 0-based index. */
  onBranch?: ((index: number, message: AiMessage) => void) | undefined;
  /** Called by Try again on an error part that is retryable. */
  onRetry?: ((message: AiMessage) => void) | undefined;
  /** Shows an inline editor in place of the person's message. */
  editing?: boolean | undefined;
  /** Saves an edit (Enter or Save). */
  onEditSubmit?: ((text: string, message: AiMessage) => void) | undefined;
  /** Cancels an edit (Escape or Cancel). */
  onEditCancel?: ((message: AiMessage) => void) | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiMessageViewMessages> | undefined;
}

const TOOL_ICON: Record<AiToolCallState, ReactNode> = {
  'pending-approval': <WarningGlyph />,
  running: <Spinner size="sm" />,
  succeeded: <CheckGlyph />,
  failed: <ErrorGlyph />,
  denied: <CloseGlyph />,
  cancelled: <DashGlyph />,
};

function seconds(ms: number) {
  return Math.max(1, Math.round(ms / 1000));
}

function toolDuration(part: AiToolCallPart): number | null {
  if (!part.startedAt || !part.endedAt) return null;
  const ms = Date.parse(part.endedAt) - Date.parse(part.startedAt);
  return Number.isFinite(ms) && ms >= 0 ? ms : null;
}

function formatSize(bytes: number, locale?: string) {
  const units = ['byte', 'kilobyte', 'megabyte', 'gigabyte'] as const;
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return new Intl.NumberFormat(locale, {
    style: 'unit',
    unit: units[unit],
    unitDisplay: 'short',
    maximumFractionDigits: unit === 0 ? 0 : 1,
  }).format(value);
}

function formatTime(iso: string, locale?: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return new Intl.DateTimeFormat(locale, { hour: 'numeric', minute: '2-digit' }).format(d);
}

const LINK_REL = 'noopener noreferrer nofollow';

function Reasoning({
  part,
  m,
}: {
  part: AiReasoningPart;
  m: Omit<AiMessageViewMessages, 'actions'>;
}) {
  const live = part.state === 'streaming';
  const label = part.redacted
    ? m.reasoningHidden
    : live
      ? m.thinking
      : part.durationMs !== undefined
        ? m.thoughtFor(seconds(part.durationMs))
        : m.thought;
  if (part.redacted || !part.text) {
    return (
      <div
        className="mn-ai-message__reasoning"
        data-state={part.state}
        data-redacted={part.redacted || undefined}
      >
        <span className="mn-ai-message__reasoning-summary" data-static="">
          <BotGlyph className="mn-ai-message__reasoning-icon" />
          {label}
        </span>
      </div>
    );
  }
  return (
    <details className="mn-ai-message__reasoning" data-state={part.state}>
      <summary className="mn-ai-message__reasoning-summary">
        <ChevronRightGlyph className="mn-ai-message__reasoning-chevron" />
        {label}
      </summary>
      <div className="mn-ai-message__reasoning-text">{part.text}</div>
    </details>
  );
}

function ToolRow({ part, m }: { part: AiToolCallPart; m: Omit<AiMessageViewMessages, 'actions'> }) {
  const ms = toolDuration(part);
  return (
    <div className="mn-ai-message__tool" data-state={part.state}>
      <span className="mn-ai-message__tool-icon" aria-hidden>
        {TOOL_ICON[part.state]}
      </span>
      <code className="mn-ai-message__tool-name">{part.name}</code>
      <span className="mn-ai-message__tool-state">{m.toolStates[part.state]}</span>
      {ms !== null ? (
        <span className="mn-ai-message__tool-duration">{m.duration(seconds(ms))}</span>
      ) : null}
      {part.state === 'failed' && part.error ? (
        <span className="mn-ai-message__tool-error">{part.error}</span>
      ) : null}
    </div>
  );
}

function FileChip({ part, locale }: { part: AiFilePart; locale?: string | undefined }) {
  const content = (
    <>
      <FileGlyph className="mn-ai-message__file-icon" />
      <span className="mn-ai-message__file-name">{part.name}</span>
      {part.size !== undefined ? (
        <span className="mn-ai-message__file-size">{formatSize(part.size, locale)}</span>
      ) : null}
    </>
  );
  return part.url && isSafeUrl(part.url) ? (
    <a className="mn-ai-message__file" href={part.url} target="_blank" rel={LINK_REL}>
      {content}
    </a>
  ) : (
    <span className="mn-ai-message__file">{content}</span>
  );
}

function Sources({
  sources,
  idBase,
  heading,
}: {
  sources: AiSourcePart[];
  idBase: string;
  heading: string;
}) {
  const headingId = `${idBase}-sources`;
  return (
    <div className="mn-ai-message__sources">
      <p id={headingId} className="mn-ai-message__sources-heading">
        {heading}
      </p>
      <ol className="mn-ai-message__sources-list" aria-labelledby={headingId}>
        {sources.map((s) => (
          <li key={s.id} id={`${idBase}-source-${s.id}`} className="mn-ai-message__source">
            {s.url && isSafeUrl(s.url) ? (
              <a href={s.url} target="_blank" rel={LINK_REL} className="mn-ai-message__source-link">
                {s.title}
              </a>
            ) : (
              <span className="mn-ai-message__source-title">{s.title}</span>
            )}
            {s.product ? <span className="mn-ai-message__source-meta">{s.product}</span> : null}
          </li>
        ))}
      </ol>
    </div>
  );
}

/**
 * One chat message. Renders the message's parts in order (streaming text,
 * reasoning, tool calls, sources, files, errors) with the layout for its
 * role: the person's messages as an end-aligned bubble, the assistant's at
 * full width under its avatar and name. Adds the action toolbar, a branch
 * switcher, a timestamp and an inline editor for the person's messages.
 */
export const AiMessageView = forwardRef<HTMLElement, AiMessageViewProps>(function AiMessageView(
  {
    message,
    agent: agentProp,
    renderPart,
    announce = true,
    showTimestamp = true,
    locale,
    showActions = true,
    actionsVisibility = 'hover',
    onCopy,
    onRegenerate,
    onEdit,
    onFeedback,
    onShare,
    onReadAloud,
    moreActions,
    onBranch,
    onRetry,
    editing = false,
    onEditSubmit,
    onEditCancel,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const idBase = useId();
  const role = message.role;
  const isUser = role === 'user';
  const isAssistant = role === 'assistant';
  const streaming = isStreaming(message);
  const text = messageText(message);
  const agent = message.agent ?? agentProp;
  const name = agent?.name ?? m.assistant;
  const time = showTimestamp ? formatTime(message.createdAt, locale) : null;
  const prefix = isUser ? m.youSaid : isAssistant ? m.assistantSaid : m.systemSaid;

  const sources = message.parts.filter((p): p is AiSourcePart => p.type === 'source');
  const sourceIndex = new Map(sources.map((s, i) => [s.id, i + 1]));
  let sourcesShown = false;

  const renderDefault = (part: AiPart, index: number): ReactNode => {
    switch (part.type) {
      case 'text':
        return (
          <AiStreamingText
            key={index}
            className="mn-ai-message__text"
            text={part.text}
            streaming={part.state === 'streaming'}
            announce={announce}
          />
        );
      case 'reasoning':
        return <Reasoning key={index} part={part} m={m} />;
      case 'tool-call':
        return <ToolRow key={part.id} part={part} m={m} />;
      case 'source':
        if (sourcesShown) return null;
        sourcesShown = true;
        return <Sources key="sources" sources={sources} idBase={idBase} heading={m.sources} />;
      case 'citation': {
        const n = sourceIndex.get(part.sourceId);
        if (n === undefined) return null;
        return (
          <a
            key={part.id}
            className="mn-ai-message__citation"
            href={`#${idBase}-source-${part.sourceId}`}
            aria-label={m.citation(n)}
          >
            {n}
          </a>
        );
      }
      case 'file':
        return <FileChip key={index} part={part} locale={locale} />;
      case 'error':
        return (
          <AiErrorMessage
            key={index}
            className="mn-ai-message__error"
            code={part.code}
            details={part.message}
            {...(part.retryable && onRetry ? { onRetry: () => onRetry(message) } : {})}
          />
        );
      default:
        return null;
    }
  };

  const content = message.parts.map((part, index) => {
    if (renderPart) {
      const custom = renderPart(part, index, message);
      if (custom !== undefined) return <Fragment key={`custom-${index}`}>{custom}</Fragment>;
    }
    return renderDefault(part, index);
  });

  const branch = message.branch && message.branch.count > 1 ? message.branch : undefined;

  const copyHandler =
    onCopy === false
      ? undefined
      : (value: string) => (onCopy ? onCopy(value, message) : writeToClipboard(value));

  const toolbar =
    showActions && !editing && !streaming ? (
      <AiMessageActions
        className="mn-ai-message__actions"
        visibility={actionsVisibility}
        messageText={text}
        onCopy={text ? copyHandler : undefined}
        onRegenerate={isAssistant && onRegenerate ? () => onRegenerate(message) : undefined}
        onEdit={isUser && onEdit ? () => onEdit(message) : undefined}
        feedback={message.feedback}
        onFeedback={isAssistant && onFeedback ? (f) => onFeedback(f, message) : undefined}
        onShare={onShare ? () => onShare(message) : undefined}
        onReadAloud={isAssistant && onReadAloud ? (s) => onReadAloud(s, message) : undefined}
        more={moreActions}
        messages={m.actions}
      />
    ) : null;

  const switcher = branch ? (
    <div className="mn-ai-message__branch" role="group" aria-label={m.versions}>
      <IconButton
        size="sm"
        label={m.previousVersion}
        icon={<ChevronRightGlyph className="mn-ai-message__branch-prev" />}
        disabled={branch.index <= 0}
        onClick={() => onBranch?.(branch.index - 1, message)}
      />
      <span className="mn-ai-message__branch-status">
        {m.versionOf(branch.index + 1, branch.count)}
      </span>
      <IconButton
        size="sm"
        label={m.nextVersion}
        icon={<ChevronRightGlyph className="mn-ai-message__branch-next" />}
        disabled={branch.index >= branch.count - 1}
        onClick={() => onBranch?.(branch.index + 1, message)}
      />
    </div>
  ) : null;

  const timestamp = time ? (
    <time className="mn-ai-message__time" dateTime={message.createdAt}>
      {time}
    </time>
  ) : null;

  return (
    <article
      ref={ref}
      {...rest}
      className={cx('mn-ai-message', className)}
      data-role={role}
      data-streaming={streaming || undefined}
      data-editing={editing || undefined}
      // AiStreamingText keeps its own live region outside its busy content, so
      // the article is only marked busy when that region is off (the thread
      // announces instead); otherwise aria-busy would silence it.
      aria-busy={(streaming && !announce) || undefined}
    >
      {isAssistant ? (
        <header className="mn-ai-message__header">
          <span className="mn-ai-message__avatar">
            <Avatar
              name={name}
              src={agent?.avatar ?? null}
              size="sm"
              decorative
              fallback="icon"
              icon={<BotGlyph />}
            />
            {agent?.avatar ? (
              <span className="mn-ai-message__badge" aria-hidden>
                <BotGlyph />
              </span>
            ) : null}
          </span>
          <span className="mn-ai-message__name">{name}</span>
          {agent?.role ? <span className="mn-ai-message__agent-role">{agent.role}</span> : null}
          {timestamp}
        </header>
      ) : null}
      {isUser && editing ? (
        <EditForm
          initial={text}
          m={m}
          onSubmit={(value) => onEditSubmit?.(value, message)}
          onCancel={() => onEditCancel?.(message)}
        />
      ) : (
        <div className="mn-ai-message__body">
          <span className="mn-visually-hidden">{prefix} </span>
          {content}
        </div>
      )}
      {(!isAssistant && timestamp) || switcher || toolbar ? (
        <div className="mn-ai-message__footer">
          {!isAssistant ? timestamp : null}
          {switcher}
          {toolbar}
        </div>
      ) : null}
    </article>
  );
});

function EditForm({
  initial,
  m,
  onSubmit,
  onCancel,
}: {
  initial: string;
  m: Omit<AiMessageViewMessages, 'actions'>;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}) {
  const [value, setValue] = useState(initial);
  const ref = useRef<HTMLTextAreaElement>(null);
  const hintId = useId();
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.focus();
    el.setSelectionRange(el.value.length, el.value.length);
  }, []);
  const save = () => {
    const next = value.trim();
    if (next) onSubmit(next);
  };
  const onKeyDown = (e: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      // Keep Escape from also stopping a reply in the surrounding thread.
      e.stopPropagation();
      onCancel();
    } else if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      save();
    }
  };
  return (
    <div className="mn-ai-message__edit">
      <textarea
        ref={ref}
        className="mn-ai-message__edit-input"
        aria-label={m.editLabel}
        aria-describedby={hintId}
        value={value}
        rows={Math.min(8, Math.max(2, value.split('\n').length))}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={onKeyDown}
      />
      <p id={hintId} className="mn-ai-message__edit-hint">
        {m.editHint}
      </p>
      <div className="mn-ai-message__edit-actions">
        <Button size="sm" onClick={onCancel}>
          {m.cancel}
        </Button>
        <Button size="sm" variant="primary" disabled={!value.trim()} onClick={save}>
          {m.save}
        </Button>
      </div>
    </div>
  );
}
