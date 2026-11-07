'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SyntheticEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CheckGlyph,
  ChevronRightGlyph,
  CloseGlyph,
  DashGlyph,
  ErrorGlyph,
  WarningGlyph,
  WrenchGlyph,
} from '../../../internal/icons.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { AiCodeBlock } from '../ai-code-block/index.js';
import type { AiToolCallPart, AiToolCallState } from '../ai-conversation/index.js';

/**
 * Turns a tool name as the model called it into words: `search_documents`,
 * `searchDocuments` and `search-documents` all become "Search documents".
 */
export function humanizeToolName(name: string): string {
  const words = name
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_\-.:/]+/g, ' ')
    .trim()
    .toLowerCase();
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : name;
}

/**
 * The display name of a tool: its entry in `toolLabels`, else the humanized
 * name ("Search documents").
 */
export function toolLabel(name: string, toolLabels?: Readonly<Record<string, string>>): string {
  return toolLabels?.[name] ?? humanizeToolName(name);
}

/**
 * Pretty-prints a tool's input or output for display. Objects and arrays are
 * JSON with two-space indentation; a string that holds JSON is re-indented;
 * any other string is returned as is. `json` says whether the result is JSON.
 */
export function formatToolValue(value: unknown): { text: string; json: boolean } {
  if (value === undefined) return { text: '', json: false };
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^[[{]/.test(trimmed)) {
      try {
        return { text: JSON.stringify(JSON.parse(trimmed), null, 2), json: true };
      } catch {
        // Not JSON: show the text.
      }
    }
    return { text: value, json: false };
  }
  try {
    const text = JSON.stringify(value, null, 2);
    if (text !== undefined) return { text, json: true };
  } catch {
    // Circular or a BigInt: fall back to String().
  }
  return { text: String(value), json: false };
}

/** Milliseconds between `startedAt` and `endedAt`, or `null` while either is missing or invalid. */
export function toolCallDuration(
  part: Pick<AiToolCallPart, 'startedAt' | 'endedAt'>,
): number | null {
  if (!part.startedAt || !part.endedAt) return null;
  const ms = Date.parse(part.endedAt) - Date.parse(part.startedAt);
  return Number.isFinite(ms) && ms >= 0 ? ms : null;
}

/** Every user-visible string in AiToolCall. */
export interface AiToolCallMessages {
  /** The state badge text per state. */
  states: Record<AiToolCallState, string>;
  /**
   * How long the call took. Default "0.4s" under a second, "12s" under a
   * minute, then "2m 05s".
   */
  duration: (ms: number) => string;
  /** Heading above the arguments. Default "Input". */
  input: string;
  /** Heading above the result. Default "Output". */
  output: string;
  /** Heading of the error text. Default "Error". */
  error: string;
  /** Names the tool in the details. Default "Tool". */
  tool: string;
  /** Shown in the details when the call has no input and no output yet. Default "No details yet." */
  empty: string;
  /** Announced politely when the state changes, e.g. "Search documents: Done". */
  stateChanged: (label: string, state: string) => string;
}

export const AI_TOOL_CALL_MESSAGES: AiToolCallMessages = {
  states: {
    'pending-approval': 'Waiting for approval',
    running: 'Running',
    succeeded: 'Done',
    failed: 'Failed',
    denied: 'Denied',
    cancelled: 'Cancelled',
  },
  duration: (ms) => {
    if (ms < 1000) return `${(Math.max(ms, 100) / 1000).toFixed(1)}s`;
    const s = Math.round(ms / 1000);
    if (s < 60) return `${s}s`;
    return `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, '0')}s`;
  },
  input: 'Input',
  output: 'Output',
  error: 'Error',
  tool: 'Tool',
  empty: 'No details yet.',
  stateChanged: (label, state) => `${label}: ${state}`,
};

const STATE_ICON: Record<AiToolCallState, ReactNode> = {
  'pending-approval': <WarningGlyph />,
  running: <Spinner size="sm" />,
  succeeded: <CheckGlyph />,
  failed: <ErrorGlyph />,
  denied: <CloseGlyph />,
  cancelled: <DashGlyph />,
};

export interface AiToolCallProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children' | 'part'> {
  /** The tool call to show, straight from the message model. */
  part: AiToolCallPart;
  /**
   * The humanized one-line summary, e.g. “Searched documents for “Q3 plan””.
   * Wins over `describeTool`.
   */
  summary?: ReactNode;
  /**
   * Builds the summary from the call, e.g. from its input. Return `undefined`
   * to fall back to the tool's label.
   */
  describeTool?: ((part: AiToolCallPart) => ReactNode | undefined) | undefined;
  /** Replaces the default wrench icon before the summary. Decorative. */
  icon?: ReactNode;
  /**
   * Display names per tool name, e.g. `{ search_documents: 'Search documents' }`.
   * Unlisted tools get a humanized name.
   */
  toolLabels?: Readonly<Record<string, string>> | undefined;
  /** Opens the input/output details on first render. Default false. */
  defaultOpen?: boolean;
  /** Announces state changes ("Search documents: Done") in a polite live region. Default true. */
  announce?: boolean;
  /** Replaces any of the English strings. */
  messages?: Partial<Omit<AiToolCallMessages, 'states'>> & {
    states?: Partial<Record<AiToolCallState, string>>;
  };
}

function ValueBlock({ label, value }: { label: string; value: unknown }) {
  const { text, json } = formatToolValue(value);
  return (
    <div className="mn-ai-tool-call__section">
      <p className="mn-ai-tool-call__section-label">{label}</p>
      <AiCodeBlock
        className="mn-ai-tool-call__code"
        code={text}
        language={json ? 'json' : undefined}
        filename={label}
        defaultWrap
        showWrapToggle={false}
      />
    </div>
  );
}

/**
 * One tool call of an assistant turn as a collapsible card: a humanized
 * summary, the state as an icon and text (waiting for approval, running,
 * done, failed, denied, cancelled), how long it took, and, on expand, the
 * pretty-printed input and output. A failed call shows its error without
 * expanding. Plugs into `AiMessageView`'s `renderPart`.
 */
export const AiToolCall = forwardRef<HTMLDivElement, AiToolCallProps>(function AiToolCall(
  {
    part,
    summary,
    describeTool,
    icon,
    toolLabels,
    defaultOpen = false,
    announce = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages: AiToolCallMessages = {
    ...AI_TOOL_CALL_MESSAGES,
    ...messagesProp,
    states: { ...AI_TOOL_CALL_MESSAGES.states, ...messagesProp?.states },
  };
  const [open, setOpen] = useState(defaultOpen);
  const label = toolLabel(part.name, toolLabels);
  const title = summary ?? describeTool?.(part) ?? label;
  const ms = toolCallDuration(part);
  const stateText = messages.states[part.state];
  const hasInput = part.input !== undefined && part.input !== null;
  const hasOutput = part.output !== undefined;

  // Announce state changes after mount, never the initial state.
  const previous = useRef(part.state);
  const [announcement, setAnnouncement] = useState('');
  useEffect(() => {
    if (previous.current === part.state) return;
    previous.current = part.state;
    if (announce) setAnnouncement(messages.stateChanged(label, stateText));
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only a state change announces.
  }, [part.state]);

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-tool-call', className)}
      data-state={part.state}
      data-open={open || undefined}
    >
      <details
        className="mn-ai-tool-call__details"
        open={open}
        onToggle={(e: SyntheticEvent<HTMLDetailsElement>) => setOpen(e.currentTarget.open)}
      >
        <summary className="mn-ai-tool-call__summary">
          <ChevronRightGlyph className="mn-ai-tool-call__chevron" />
          <span className="mn-ai-tool-call__icon" aria-hidden>
            {icon ?? <WrenchGlyph />}
          </span>
          <span className="mn-ai-tool-call__title">{title}</span>
          <span className="mn-ai-tool-call__meta">
            <span className="mn-ai-tool-call__state" data-state={part.state}>
              <span className="mn-ai-tool-call__state-icon" aria-hidden>
                {STATE_ICON[part.state]}
              </span>
              {stateText}
            </span>
            {ms !== null ? (
              <span className="mn-ai-tool-call__duration">{messages.duration(ms)}</span>
            ) : null}
          </span>
        </summary>
        <div className="mn-ai-tool-call__body">
          <p className="mn-ai-tool-call__tool">
            {messages.tool}: <code className="mn-ai-tool-call__name">{part.name}</code>
          </p>
          {open && hasInput ? <ValueBlock label={messages.input} value={part.input} /> : null}
          {open && hasOutput ? <ValueBlock label={messages.output} value={part.output} /> : null}
          {!hasInput && !hasOutput ? (
            <p className="mn-ai-tool-call__empty">{messages.empty}</p>
          ) : null}
        </div>
      </details>
      {part.state === 'failed' && part.error ? (
        <p className="mn-ai-tool-call__error">
          <span className="mn-visually-hidden">{messages.error}: </span>
          {part.error}
        </p>
      ) : null}
      {announce ? (
        <span role="status" className="mn-visually-hidden">
          {announcement}
        </span>
      ) : null}
    </div>
  );
});
