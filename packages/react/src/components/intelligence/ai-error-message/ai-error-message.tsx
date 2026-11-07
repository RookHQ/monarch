'use client';

import { forwardRef, useEffect, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ClockGlyph,
  OfflineGlyph,
  PencilGlyph,
  PlusGlyph,
  RetryGlyph,
  WrenchGlyph,
} from '../../../internal/icons.js';
import { Alert } from '../../feedback/alert/index.js';
import { Button } from '../../actions/button/index.js';

/** Why an assistant turn failed. */
export type AiErrorKind =
  'rate-limit' | 'context-overflow' | 'safety-block' | 'network' | 'tool-failure' | 'unknown';

const KIND_RULES: [AiErrorKind, RegExp][] = [
  ['rate-limit', /(^|-)(429|529)$|rate-?limit|too-many-requests|quota|overloaded|throttl/],
  [
    'context-overflow',
    /context-(length|overflow|window|limit)|(^|-)413$|token-limit|too-many-tokens|prompt-too-long|max-context/,
  ],
  ['safety-block', /safety|content-(filter|policy)|moderation|policy|refusal|blocked/],
  ['tool-failure', /tool/],
  [
    'network',
    /network|offline|timeout|timedout|timed-out|connection|econn|fetch-failed|unreachable|(^|-)(0|408|502|503|504)$/,
  ],
];

/**
 * Maps a raw error code to an {@link AiErrorKind}: `429` and `rate_limit_exceeded`
 * → `rate-limit`, `context_length_exceeded` and `413` → `context-overflow`,
 * `content_filter` → `safety-block`, `ETIMEDOUT` and `503` → `network`,
 * `tool_error` → `tool-failure`, anything else → `unknown`. Case, `_` and
 * spaces are ignored, and `http-429`-style codes from `rookIntelligenceTransport`
 * are understood.
 */
export function aiErrorKind(code: string | number | null | undefined): AiErrorKind {
  if (code === undefined || code === null || code === '') return 'unknown';
  const c = String(code)
    .trim()
    .toLowerCase()
    .replace(/[\s_.:/]+/g, '-');
  for (const [kind, rule] of KIND_RULES) if (rule.test(c)) return kind;
  return 'unknown';
}

export interface AiErrorMessageMessages {
  /** Default title per kind. */
  titles: Record<AiErrorKind, string>;
  /** Default description per kind. */
  descriptions: Record<AiErrorKind, string>;
  retry: string;
  /** The Retry label while `retryAfter` counts down. */
  retryIn: (seconds: number) => string;
  /** Announced politely when the countdown ends. */
  retryReady: string;
  editPrompt: string;
  switchModel: string;
  newConversation: string;
  summarize: string;
  /** Summary of the technical details disclosure. */
  details: string;
}

const DEFAULT_MESSAGES: AiErrorMessageMessages = {
  titles: {
    'rate-limit': 'Too many requests',
    'context-overflow': 'This conversation is too long',
    'safety-block': 'The assistant can’t respond to this',
    network: 'Connection lost',
    'tool-failure': 'A tool didn’t work',
    unknown: 'Something went wrong',
  },
  descriptions: {
    'rate-limit': 'The assistant is handling a lot of requests. Wait a moment, then try again.',
    'context-overflow':
      'The assistant can’t take in any more of this conversation. Start a new conversation, or summarize this one and continue from the summary.',
    'safety-block':
      'This request was blocked by a safety filter. Rephrase your message and send it again.',
    network: 'The assistant couldn’t be reached. Check your connection and try again.',
    'tool-failure':
      'The assistant couldn’t finish because a tool it used failed. Try again, or ask in a different way.',
    unknown: 'The assistant couldn’t finish this response. Try again.',
  },
  retry: 'Try again',
  retryIn: (seconds) => `Try again in ${seconds}s`,
  retryReady: 'You can try again now.',
  editPrompt: 'Edit message',
  switchModel: 'Switch model',
  newConversation: 'New conversation',
  summarize: 'Summarize and continue',
  details: 'Technical details',
};

const TONE: Record<AiErrorKind, 'warning' | 'danger'> = {
  'rate-limit': 'warning',
  'context-overflow': 'warning',
  'safety-block': 'warning',
  network: 'danger',
  'tool-failure': 'danger',
  unknown: 'danger',
};

const ICON: Partial<Record<AiErrorKind, ReactNode>> = {
  'rate-limit': <ClockGlyph />,
  network: <OfflineGlyph />,
  'tool-failure': <WrenchGlyph />,
};

type ActionId = 'retry' | 'editPrompt' | 'switchModel' | 'newConversation' | 'summarize';

/** Action order per kind; the first available one is the primary button. */
const ORDER: Record<AiErrorKind, ActionId[]> = {
  'rate-limit': ['retry', 'switchModel', 'editPrompt', 'newConversation'],
  'context-overflow': ['newConversation', 'summarize', 'editPrompt', 'switchModel', 'retry'],
  'safety-block': ['editPrompt', 'retry', 'newConversation', 'switchModel'],
  network: ['retry', 'editPrompt', 'switchModel', 'newConversation'],
  'tool-failure': ['retry', 'editPrompt', 'switchModel', 'newConversation'],
  unknown: ['retry', 'editPrompt', 'switchModel', 'newConversation'],
};

export interface AiErrorMessageProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Why the turn failed. Wins over `code`. Default: derived from `code`, else `unknown`. */
  kind?: AiErrorKind;
  /**
   * The raw error code (an HTTP status, `rate_limit_exceeded`,
   * `AiErrorPart.code`, …), mapped with `aiErrorKind`. Never shown.
   */
  code?: string | number;
  /** Replaces the kind's default title. */
  title?: ReactNode;
  /** Replaces the kind's default description. */
  description?: ReactNode;
  /**
   * Technical information (a request id, the provider's message) in a
   * collapsed "Technical details" disclosure. A string is shown as
   * preformatted text.
   */
  details?: ReactNode | undefined;
  /**
   * Seconds until a retry is allowed (a `Retry-After` header, say). Retry is
   * disabled and counts down ("Try again in 12s") until then.
   */
  retryAfter?: number;
  /** Shows Try again. */
  onRetry?: (() => void) | undefined;
  /** Shows Edit message: put the failed prompt back in the input. */
  onEditPrompt?: (() => void) | undefined;
  /** Shows Switch model. */
  onSwitchModel?: (() => void) | undefined;
  /** Shows New conversation; the primary action for `context-overflow`. */
  onNewConversation?: (() => void) | undefined;
  /** Shows Summarize and continue, offered for `context-overflow`. */
  onSummarize?: (() => void) | undefined;
  /** Shows a spinner on Try again while the retry is starting. */
  retrying?: boolean;
  /** Extra actions after the built-in ones. */
  actions?: ReactNode;
  /** Strings, for localisation. `titles` and `descriptions` merge per kind. */
  messages?: Partial<Omit<AiErrorMessageMessages, 'titles' | 'descriptions'>> & {
    titles?: Partial<Record<AiErrorKind, string>>;
    descriptions?: Partial<Record<AiErrorKind, string>>;
  };
}

/** Seconds left until `retryAfter` elapses; ticks every half second. */
function useCountdown(retryAfter: number | undefined) {
  const initial = retryAfter && retryAfter > 0 ? Math.ceil(retryAfter) : 0;
  const [left, setLeft] = useState(initial);
  useEffect(() => {
    if (!retryAfter || retryAfter <= 0) {
      setLeft(0);
      return;
    }
    const deadline = Date.now() + retryAfter * 1000;
    const tick = () => {
      const next = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setLeft(next);
      if (next === 0) clearInterval(id);
    };
    const id = setInterval(tick, 500);
    tick();
    return () => clearInterval(id);
  }, [retryAfter]);
  return left;
}

/**
 * The failure card for an assistant turn: what went wrong in plain words
 * (rate limit, conversation too long, safety block, connection, tool failure),
 * the actions that can fix it, an optional retry countdown and a technical
 * details disclosure. Announced once, assertively, when it appears.
 */
export const AiErrorMessage = forwardRef<HTMLDivElement, AiErrorMessageProps>(
  function AiErrorMessage(
    {
      kind: kindProp,
      code,
      title,
      description,
      details,
      retryAfter,
      onRetry,
      onEditPrompt,
      onSwitchModel,
      onNewConversation,
      onSummarize,
      retrying = false,
      actions,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      titles: { ...DEFAULT_MESSAGES.titles, ...messagesProp?.titles },
      descriptions: { ...DEFAULT_MESSAGES.descriptions, ...messagesProp?.descriptions },
    };
    const kind = kindProp ?? aiErrorKind(code);
    const tone = TONE[kind];
    const left = useCountdown(retryAfter);
    const waiting = left > 0;
    const [ready, setReady] = useState(false);
    const titleId = useId();

    // Announce "You can try again now" once, when a countdown finishes.
    useEffect(() => {
      if (waiting) setReady(false);
      else if (retryAfter && retryAfter > 0) setReady(true);
    }, [waiting, retryAfter]);

    const handlers: Record<ActionId, (() => void) | undefined> = {
      retry: onRetry,
      editPrompt: onEditPrompt,
      switchModel: onSwitchModel,
      newConversation: onNewConversation,
      summarize: kind === 'context-overflow' ? onSummarize : undefined,
    };
    const icons: Partial<Record<ActionId, ReactNode>> = {
      retry: <RetryGlyph />,
      editPrompt: <PencilGlyph />,
      newConversation: <PlusGlyph />,
    };
    const available = ORDER[kind].filter((id) => handlers[id]);

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-error-message', className)}
        data-kind={kind}
        data-waiting={waiting || undefined}
      >
        <Alert
          tone={tone}
          icon={ICON[kind]}
          className="mn-ai-error-message__card"
          aria-labelledby={titleId}
          role="group"
        >
          <div role="alert" className="mn-ai-error-message__text">
            <p id={titleId} className="mn-ai-error-message__title">
              {title ?? messages.titles[kind]}
            </p>
            <div className="mn-ai-error-message__description">
              {description ?? messages.descriptions[kind]}
            </div>
          </div>
          {available.length || actions ? (
            <div className="mn-ai-error-message__actions">
              {available.map((id, i) => {
                const isRetry = id === 'retry';
                return (
                  <Button
                    key={id}
                    size="sm"
                    variant={i === 0 ? 'primary' : 'secondary'}
                    leadingIcon={icons[id]}
                    disabled={isRetry && waiting}
                    loading={isRetry && retrying}
                    data-action={id}
                    className="mn-ai-error-message__action"
                    onClick={handlers[id]}
                  >
                    {isRetry && waiting ? messages.retryIn(left) : messages[id]}
                  </Button>
                );
              })}
              {actions}
            </div>
          ) : null}
          {!onRetry && waiting ? (
            <p className="mn-ai-error-message__countdown">{messages.retryIn(left)}</p>
          ) : null}
          {details ? (
            <details className="mn-ai-error-message__details">
              <summary className="mn-ai-error-message__summary">{messages.details}</summary>
              {typeof details === 'string' ? (
                <pre className="mn-ai-error-message__details-body">{details}</pre>
              ) : (
                <div className="mn-ai-error-message__details-body">{details}</div>
              )}
            </details>
          ) : null}
        </Alert>
        <span role="status" className="mn-visually-hidden">
          {ready ? messages.retryReady : ''}
        </span>
      </div>
    );
  },
);
