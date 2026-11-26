'use client';

import {
  forwardRef,
  Fragment,
  memo,
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
  ClockGlyph,
  CloseGlyph,
  DashGlyph,
  ErrorGlyph,
  WarningGlyph,
  BotGlyph,
} from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { writeToClipboard } from '../../actions/copy-button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import {
  isStreaming,
  messageText,
  type AiAgentRef,
  type AiArtifactPart,
  type AiEditProposalPart,
  type AiErrorPart,
  type AiFilePart,
  type AiFinishReason,
  type AiMemoryAction,
  type AiMemoryPart,
  type AiMessage,
  type AiPart,
  type AiPlanStepState,
  type AiReasoningPart,
  type AiSourcePart,
  type AiToolCallPart,
  type AiToolCallState,
} from '../ai-conversation/index.js';
import { AiArtifactCard } from '../ai-artifact-card/index.js';
import { AiChainOfSteps, type AiStepState } from '../ai-chain-of-steps/index.js';
import { AiEditProposal, type AiEditHunk } from '../ai-edit-proposal/index.js';
import { AiErrorMessage } from '../ai-error-message/index.js';
import { AiGenerativeUi, type AiUiAction, type AiUiRegistry } from '../ai-generative-ui/index.js';
import {
  AiMessageActions,
  type AiFeedback,
  type AiMessageActionsMessages,
} from '../ai-message-actions/index.js';
import { AiMemoryChip } from '../ai-memory-chip/index.js';
import { AiMessageBranch } from '../ai-message-branch/index.js';
import { AiToolApproval, type AiToolApprovalProps } from '../ai-tool-approval/index.js';

import { AiStreamingText } from '../ai-streaming-text/index.js';

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
  /** Accessible name of a plan's step list. */
  plan: string;
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
  /**
   * Shown under a reply that did not finish normally: cut short by the
   * output limit (`length`), stopped by a safety filter (`content-filter`)
   * or declined (`refusal`).
   */
  finishNotices: Record<AiFinishNoticeReason, string>;
  /** Prefix of a memory chip, by what happened to the memory. */
  memoryActions: Record<AiMemoryAction, string>;
  /** Stands in for the text of a forgotten memory that came without any. */
  memoryForgotten: string;
  /** Accessible name of a memory chip. */
  memoryLabel: string;
  /** Shown on a message written while offline that has not been sent yet. */
  queued: string;
  /** Removes a queued message before it is sent. */
  cancelQueued: string;
  /** Picks up a reply whose connection dropped, on an error that can be resumed. */
  resume: string;
}

/** Finish reasons that get a notice under the reply. */
export type AiFinishNoticeReason = Extract<AiFinishReason, 'length' | 'content-filter' | 'refusal'>;

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
    'awaiting-result': 'Waiting for result',
    running: 'Running',
    succeeded: 'Done',
    failed: 'Failed',
    denied: 'Denied',
    cancelled: 'Cancelled',
  },
  duration: (s) => `${s}s`,
  sources: 'Sources',
  plan: 'Plan',
  citation: (n) => `Source ${n}`,
  versions: 'Versions',
  previousVersion: 'Previous version',
  nextVersion: 'Next version',
  versionOf: (i, n) => `${i} / ${n}`,
  editLabel: 'Edit message',
  editHint: 'Enter to save, Shift+Enter for a new line, Escape to cancel.',
  save: 'Save',
  cancel: 'Cancel',
  finishNotices: {
    length: 'This answer was cut short because it reached the length limit.',
    'content-filter': 'This answer was stopped by a safety filter.',
    refusal: 'The assistant declined to answer this.',
  },
  memoryActions: {
    read: 'Used memory:',
    write: 'Remembered:',
    forget: 'Forgot:',
  },
  memoryForgotten: 'a saved memory',
  memoryLabel: 'Memory',
  queued: 'Queued. It will send when you are back online.',
  cancelQueued: 'Cancel',
  resume: 'Resume',
};

const NOTICE_REASONS: readonly string[] = ['length', 'content-filter', 'refusal'];
const isAiFinishNoticeReason = (r: AiFinishReason | undefined): r is AiFinishNoticeReason =>
  r !== undefined && NOTICE_REASONS.includes(r);

const PLAN_STEP_STATE: Record<AiPlanStepState, AiStepState> = {
  todo: 'pending',
  active: 'active',
  done: 'done',
  skipped: 'skipped',
  failed: 'failed',
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
   * and `null` to render nothing. Every part type has a default: a plan
   * renders as a read-only step list (use this for an AiPlan to approve); artifact, ui and edit-proposal parts render
   * once `onOpenArtifact`, `uiRegistry` or `getEditHunks` gives them what
   * they need. Keep the function stable (module level or `useCallback`), so
   * finished messages are not re-rendered while a reply streams.
   */
  renderPart?:
    ((part: AiPart, index: number, message: AiMessage) => ReactNode | undefined) | undefined;
  /** Renders `artifact` parts as an AiArtifactCard that calls this when opened. */
  onOpenArtifact?: ((part: AiArtifactPart, message: AiMessage) => void) | undefined;
  /**
   * Renders `ui` parts with AiGenerativeUi from this allow-list of components.
   * Without it a `ui` part renders nothing: model output never picks a
   * component the product did not list.
   */
  uiRegistry?: AiUiRegistry | undefined;
  /**
   * Receives actions from generated components (their `emit` prop), e.g. to
   * send one back as a tool result: `conversation.submitToolResult(action.toolCallId, { output: action })`.
   */
  onUiAction?: ((action: AiUiAction, message: AiMessage) => void) | undefined;
  /**
   * Renders `edit-proposal` parts as a read-only AiEditProposal. The part's
   * `diff` has no fixed shape, so this turns it into hunks; return `undefined`
   * to render nothing. Use `renderPart` for accept and reject controls.
   */
  getEditHunks?:
    ((part: AiEditProposalPart, message: AiMessage) => AiEditHunk[] | undefined) | undefined;
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
  /**
   * Shows Resume on an error part marked `resumable` (the connection dropped
   * mid-reply and the service still has the run), e.g. `conversation.resumeRun`.
   */
  onResume?: ((message: AiMessage) => void) | undefined;
  /**
   * Shows Cancel on a message that is still queued (`message.queued`, written
   * while offline), e.g. `conversation.cancelQueued`.
   */
  onCancelQueued?: ((message: AiMessage) => void) | undefined;
  /** Shows Forget on the chip of a memory the reply saved; called with the memory's id. */
  onForgetMemory?: ((memoryId: string, message: AiMessage) => void) | undefined;
  /** Shows Edit on the chip of a memory the reply saved; called with the id and the new text. */
  onEditMemory?: ((memoryId: string, text: string, message: AiMessage) => void) | undefined;
  /**
   * With this or `onDenyTool`, a tool call waiting for approval renders as
   * AiToolApproval with Approve and Deny (e.g. `conversation.approveTool`).
   * `input` is set when the person edited the arguments.
   */
  onApproveTool?: ((toolCallId: string, input: unknown, message: AiMessage) => void) | undefined;
  /** Called by Deny on a tool call waiting for approval, with the reason typed, if any. */
  onDenyTool?:
    ((toolCallId: string, reason: string | undefined, message: AiMessage) => void) | undefined;
  /**
   * Offers "Always allow this tool" on the approval card; called with the
   * tool name. Remember it and approve later calls through
   * `useAiConversation({ toolPolicy })`.
   */
  onAlwaysAllowTool?: ((toolName: string) => void) | undefined;
  /** Extra AiToolApproval props: `describe`, `toolLabels`, `argumentLabels`, `messages`… */
  toolApprovalProps?:
    | Partial<Omit<AiToolApprovalProps, 'part' | 'onApprove' | 'onDeny' | 'onAlwaysAllow'>>
    | undefined;
  /** Shows an inline editor in place of the person's message. */
  editing?: boolean | undefined;
  /** Saves an edit (Enter or Save). */
  onEditSubmit?: ((text: string, message: AiMessage) => void) | undefined;
  /** Cancels an edit (Escape or Cancel). */
  onEditCancel?: ((message: AiMessage) => void) | undefined;
  /** Strings, for localisation. `toolStates`, `finishNotices` and `memoryActions` merge per key. */
  messages?:
    | (Partial<Omit<AiMessageViewMessages, 'toolStates' | 'finishNotices' | 'memoryActions'>> & {
        toolStates?: Partial<Record<AiToolCallState, string>> | undefined;
        finishNotices?: Partial<Record<AiFinishNoticeReason, string>> | undefined;
        memoryActions?: Partial<Record<AiMemoryAction, string>> | undefined;
      })
    | undefined;
}

const TOOL_ICON: Record<AiToolCallState, ReactNode> = {
  'pending-approval': <WarningGlyph />,
  'awaiting-result': <ClockGlyph />,
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
      <div className="mn-ai-message__reasoning-text" dir="auto">
        {part.text}
      </div>
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

/** Error parts already announced, so a remount (virtualised thread) does not announce again. */
const announcedErrors = new WeakSet<AiErrorPart>();

function ErrorPart({
  part,
  onRetry,
  onResume,
  resumeLabel,
}: {
  part: AiErrorPart;
  onRetry?: () => void;
  onResume?: () => void;
  resumeLabel: string;
}) {
  const [first] = useState(() => !announcedErrors.has(part));
  useEffect(() => {
    announcedErrors.add(part);
  }, [part]);
  return (
    <AiErrorMessage
      className="mn-ai-message__error"
      code={part.code}
      details={part.message}
      announce={first}
      {...(part.retryAfterMs ? { retryAfter: Math.ceil(part.retryAfterMs / 1000) } : {})}
      {...(onRetry ? { onRetry } : {})}
      {...(onResume
        ? {
            actions: (
              <Button size="sm" className="mn-ai-message__resume" onClick={onResume}>
                {resumeLabel}
              </Button>
            ),
          }
        : {})}
    />
  );
}

function MemoryPart({
  part,
  m,
  onForget,
  onEdit,
}: {
  part: AiMemoryPart;
  m: Omit<AiMessageViewMessages, 'actions'>;
  onForget?: ((id: string) => void) | undefined;
  onEdit?: ((id: string, text: string) => void) | undefined;
}) {
  // Only a memory that was just saved can still be changed from here.
  const saved = part.action === 'write';
  return (
    <AiMemoryChip
      className="mn-ai-message__memory"
      data-action={part.action}
      memory={{ id: part.id, text: part.text || m.memoryForgotten }}
      messages={{ prefix: m.memoryActions[part.action], label: m.memoryLabel }}
      {...(part.scope ? { meta: part.scope } : {})}
      {...(saved && onForget ? { onForget } : {})}
      {...(saved && onEdit ? { onEdit } : {})}
    />
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
 * reasoning, tool calls, sources, files, plans, artifacts, generative UI,
 * edit proposals and errors) with the layout for its
 * role: the person's messages as an end-aligned bubble, the assistant's at
 * full width under its avatar and name. Adds the action toolbar, a branch
 * switcher, a timestamp and an inline editor for the person's messages.
 *
 * Memoised: a message whose props are the same objects as last time is not
 * rendered again, so a reply streaming at the end of a thread does not
 * re-render the finished messages above it. Pass stable callbacks.
 */
export const AiMessageView = memo(
  forwardRef<HTMLElement, AiMessageViewProps>(function AiMessageView(
    {
      message,
      agent: agentProp,
      renderPart,
      onOpenArtifact,
      uiRegistry,
      onUiAction,
      getEditHunks,
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
      onResume,
      onCancelQueued,
      onForgetMemory,
      onEditMemory,
      onApproveTool,
      onDenyTool,
      onAlwaysAllowTool,
      toolApprovalProps,
      editing = false,
      onEditSubmit,
      onEditCancel,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m: Omit<AiMessageViewMessages, 'actions'> & {
      actions?: Partial<AiMessageActionsMessages> | undefined;
    } = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      toolStates: { ...DEFAULT_MESSAGES.toolStates, ...messagesProp?.toolStates },
      finishNotices: { ...DEFAULT_MESSAGES.finishNotices, ...messagesProp?.finishNotices },
      memoryActions: { ...DEFAULT_MESSAGES.memoryActions, ...messagesProp?.memoryActions },
    };
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

    // The reply as a whole is finished: no text streams and no tool call is still going.
    const replyDone =
      !streaming &&
      !message.parts.some(
        (p) =>
          p.type === 'tool-call' &&
          (p.state === 'running' ||
            p.state === 'pending-approval' ||
            p.state === 'awaiting-result'),
      );
    let lastText = -1;
    message.parts.forEach((p, i) => {
      if (p.type === 'text') lastText = i;
    });
    const approvals = Boolean(onApproveTool || onDenyTool);

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
              dir="auto"
              text={part.text}
              streaming={part.state === 'streaming'}
              announce={announce}
              announceComplete={index === lastText && replyDone}
            />
          );
        case 'reasoning':
          return <Reasoning key={index} part={part} m={m} />;
        case 'tool-call':
          if (part.state === 'pending-approval' && approvals)
            return (
              <AiToolApproval
                key={part.id}
                {...toolApprovalProps}
                className={cx('mn-ai-message__approval', toolApprovalProps?.className)}
                part={part}
                {...(part.risk ? { risk: part.risk } : {})}
                onApprove={(input) => onApproveTool?.(part.id, input, message)}
                onDeny={(reason) => onDenyTool?.(part.id, reason, message)}
                onAlwaysAllow={onAlwaysAllowTool}
              />
            );
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
            <ErrorPart
              key={index}
              part={part}
              resumeLabel={m.resume}
              {...(part.retryable && onRetry ? { onRetry: () => onRetry(message) } : {})}
              {...(part.resumable && onResume ? { onResume: () => onResume(message) } : {})}
            />
          );
        case 'memory':
          return (
            <MemoryPart
              key={`memory-${part.action}-${part.id}`}
              part={part}
              m={m}
              onForget={onForgetMemory ? (id) => onForgetMemory(id, message) : undefined}
              onEdit={onEditMemory ? (id, text) => onEditMemory(id, text, message) : undefined}
            />
          );
        case 'plan':
          // Progress, not a request: AiPlan (with Approve and Reject) is for `renderPart`.
          return (
            <AiChainOfSteps
              key={index}
              className="mn-ai-message__plan"
              steps={part.steps.map((step) => ({
                id: step.id,
                label: step.label,
                state: PLAN_STEP_STATE[step.state],
              }))}
              collapsible={false}
              announce={false}
              messages={{ label: m.plan }}
            />
          );
        case 'artifact':
          return onOpenArtifact ? (
            <AiArtifactCard
              key={index}
              className="mn-ai-message__artifact"
              part={part}
              onOpen={(opened) => onOpenArtifact(opened, message)}
            />
          ) : null;
        case 'ui':
          return uiRegistry ? (
            <AiGenerativeUi
              key={index}
              className="mn-ai-message__ui"
              part={part}
              registry={uiRegistry}
              {...(onUiAction
                ? { onAction: (action: AiUiAction) => onUiAction(action, message) }
                : {})}
            />
          ) : null;
        case 'edit-proposal': {
          const hunks = getEditHunks?.(part, message);
          return hunks ? (
            <AiEditProposal
              key={index}
              className="mn-ai-message__edit-proposal"
              part={part}
              hunks={hunks}
            />
          ) : null;
        }
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

    const queued = isUser && message.queued === true;
    const toolbar =
      showActions && !editing && !streaming && !queued ? (
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
      <AiMessageBranch
        index={branch.index}
        count={branch.count}
        onChange={(index) => onBranch?.(index, message)}
        messages={{
          label: m.versions,
          previous: m.previousVersion,
          next: m.nextVersion,
          position: m.versionOf,
        }}
      />
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
        data-queued={queued || undefined}
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
            {isAssistant && replyDone && isAiFinishNoticeReason(message.finishReason) ? (
              <p className="mn-ai-message__finish" data-reason={message.finishReason}>
                <WarningGlyph className="mn-ai-message__finish-icon" aria-hidden />
                {m.finishNotices[message.finishReason]}
              </p>
            ) : null}
          </div>
        )}
        {queued ? (
          <p className="mn-ai-message__queued">
            <ClockGlyph className="mn-ai-message__queued-icon" aria-hidden />
            <span className="mn-ai-message__queued-text">{m.queued}</span>
            {onCancelQueued ? (
              <Button
                size="sm"
                variant="ghost"
                className="mn-ai-message__queued-cancel"
                onClick={() => onCancelQueued(message)}
              >
                {m.cancelQueued}
              </Button>
            ) : null}
          </p>
        ) : null}
        {(!isAssistant && timestamp) || switcher || toolbar ? (
          <div className="mn-ai-message__footer">
            {!isAssistant ? timestamp : null}
            {switcher}
            {toolbar}
          </div>
        ) : null}
      </article>
    );
  }),
);

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
