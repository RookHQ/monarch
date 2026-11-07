import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiErrorMessage,
  mockTransport,
  useAiConversation,
  type AiErrorKind,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const failing = mockTransport({
  delayMs: 300,
  reply: [{ type: 'error', code: 'http-429', message: 'Too many requests.', retryable: true }],
});

function ConversationDemo() {
  const { status, error, send, regenerate, reset } = useAiConversation({ transport: failing });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div>
        <Button
          size="sm"
          loading={status === 'submitted' || status === 'streaming'}
          onClick={() => send('Summarize the quarterly report.')}
        >
          Send a message
        </Button>
      </div>
      {status === 'error' && error ? (
        <AiErrorMessage
          code={error.code}
          retryAfter={5}
          onRetry={error.retryable ? () => regenerate() : undefined}
          onNewConversation={reset}
          details={`code: ${error.code}\nmessage: ${error.message}`}
        />
      ) : null}
    </div>
  );
}

function CountdownDemo() {
  const [key, setKey] = useState(0);
  return (
    <AiErrorMessage
      key={key}
      kind="rate-limit"
      retryAfter={12}
      onRetry={() => setKey((k) => k + 1)}
      onSwitchModel={() => {}}
    />
  );
}

const KINDS: AiErrorKind[] = [
  'rate-limit',
  'context-overflow',
  'safety-block',
  'network',
  'tool-failure',
  'unknown',
];

export default {
  id: 'ai-error-message',
  title: 'AiErrorMessage',
  summary: 'The failure card for an assistant turn, with the actions that fix it.',
  sections: {
    purpose: {
      summary:
        'Explains in plain words why an assistant reply failed (too many requests, a conversation that is too long, a safety block, a lost connection, a failed tool) and offers the actions that can recover from it.',
      useWhen: [
        'An assistant turn ended with an error: render it where the reply would have been.',
        'You have a raw error code from a transport (`AiErrorPart.code`, an HTTP status) and need human copy for it.',
        'The provider asked you to wait (`Retry-After`): pass `retryAfter` to count down before Retry.',
      ],
      avoidWhen: [
        'A non-AI section of a page failed to load: use InlineErrorRetry.',
        'The whole assistant panel is unavailable: use ErrorState.',
        'The person stopped the reply themselves: that is not an error, show the partial reply.',
      ],
    },
    designPrinciples: {
      items: [
        'Say what happened and what to do next; never show the provider’s raw message in the title or description. Put it in `details`.',
        'Offer only actions the app can perform: each one appears only when its handler is passed.',
        'The kind decides the primary action: Retry for transient failures, New conversation for a conversation that is too long, Edit message for a safety block.',
        'Waiting is a warning, not a failure: rate limits, overflow and safety blocks use the warning tone; connection, tool and unknown failures use danger.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiErrorKind(code)` is exported for apps that branch on the kind themselves; `kind` wins over `code`.',
        'Mapped codes: `429`, `529`, `rate_limit*`, `quota`, `overloaded` → rate-limit; `context_length*`, `context-overflow`, `413`, `token-limit` → context-overflow; `safety`, `content_filter`, `moderation`, `policy`, `refusal`, `blocked` → safety-block; `network`, `offline`, `timeout`, `ETIMEDOUT`, `ECONN*`, `0`, `408`, `502`–`504` → network; anything with `tool` → tool-failure; the rest → unknown.',
        '`messages.titles` and `messages.descriptions` merge per kind, so you can override one kind’s copy.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: KINDS,
          description:
            'Picks the default title, description, icon, tone and action order. Exposed as `data-kind` on the root and `data-tone` (`warning` or `danger`) on the card.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Failed', description: 'Title, description and the available actions.' },
        {
          name: 'Waiting to retry',
          attribute: 'data-waiting',
          description:
            'While `retryAfter` counts down, Retry is disabled and reads "Try again in 12s". When it reaches zero Retry is enabled and "You can try again now." is announced politely.',
        },
        {
          name: 'Retrying',
          attribute: 'aria-busy="true" on Retry',
          description: 'With `retrying`, Retry shows a spinner and ignores clicks but keeps focus.',
        },
        {
          name: 'Details open',
          attribute: 'open on the details element',
          description: 'The technical details disclosure is expanded.',
        },
      ],
    },
    interaction: {
      items: [
        'Each action button calls its handler once per click; the card never removes itself, so the app decides what replaces it.',
        'The first available action in the kind’s order is the primary button; the rest are secondary.',
        'Summarize and continue appears only for `context-overflow`.',
        'The countdown restarts when `retryAfter` changes; a remount (a new `key`) also restarts it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the actions, then the details summary.' },
        { keys: 'Enter / Space', action: 'Runs the focused action, or opens and closes details.' },
      ],
      notes: [
        'The card never moves focus when it appears; the alert announcement tells the person.',
      ],
    },
    accessibility: {
      role: 'group, containing an alert (title and description) and a polite status',
      name: 'The title (aria-labelledby on the card)',
      items: [
        'Only the title and description sit in role="alert", so they are announced assertively once when the card appears. The countdown, buttons and details are outside it and never re-announce.',
        'A separate hidden role="status" says "You can try again now." when a countdown finishes.',
        'Retry is a native disabled button during the countdown and its label carries the remaining time.',
        'Technical details use native `<details>`/`<summary>`; a string is shown in `<pre>` that wraps instead of scrolling.',
        'Icons are decorative (`aria-hidden`).',
      ],
    },
    responsive: {
      items: [
        'Actions wrap onto as many rows as they need; long codes in details wrap, so there is no horizontal scroll at 390px.',
        'On coarse pointers the action buttons and the details summary are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Built on Alert: the warning or danger subtle background, border and strong icon colour from the status tokens.',
        'Details use `--mn-color-bg-surface`, `--mn-color-border-default` and `--mn-font-mono`.',
        'In forced colours the card and details keep a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'From a failed conversation turn',
          stack: true,
          render: () => <ConversationDemo />,
        },
        {
          title: 'Rate limit with a retry countdown',
          stack: true,
          render: () => <CountdownDemo />,
        },
        {
          title: 'Conversation too long',
          stack: true,
          render: () => (
            <AiErrorMessage
              code="context_length_exceeded"
              onNewConversation={() => {}}
              onSummarize={() => {}}
            />
          ),
        },
        {
          title: 'Every kind',
          stack: true,
          render: () => (
            <>
              {KINDS.map((kind) => (
                <AiErrorMessage
                  key={kind}
                  kind={kind}
                  onRetry={kind === 'context-overflow' ? undefined : () => {}}
                  onEditPrompt={() => {}}
                  onNewConversation={kind === 'context-overflow' ? () => {} : undefined}
                  details={
                    kind === 'tool-failure' ? 'tool: search_documents\nstatus: 502' : undefined
                  }
                />
              ))}
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the raw code and let the component pick the copy; override copy per kind through `messages`.',
        'Pass `retryAfter` whenever the provider tells you how long to wait.',
        'Put request ids and provider messages in `details` for support.',
      ],
      dont: [
        'Do not put the provider’s error text in `title` or `description`.',
        'Do not offer Retry for a safety block unless retrying can succeed.',
        'Do not show a toast as well: the card already announces itself.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'The card it is built on.' },
        { id: 'inline-error-retry', reason: 'A non-AI section that failed to load.' },
        { id: 'ai-message', reason: 'Renders this card for a message’s error part.' },
        { id: 'ai-conversation', reason: 'Provides `error` and `regenerate`.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['alert', 'button'] },
    testing: {
      unit: [
        'Maps 25 raw codes (numbers, HTTP-style, snake case, errno) to the right kind.',
        'Announces title and description once in role="alert"; the countdown never changes the alert.',
        'Default copy and tone for every kind; `kind` wins over `code`; messages and copy overrides.',
        'Shows only actions with handlers, in the kind’s order, and runs them by keyboard and click.',
        'Counts down `retryAfter` with fake timers, blocks Retry until zero, then enables it and announces it.',
        'Details disclosure, busy Retry, extra actions and ref forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
