import {
  AiMessageView,
  AiToolCall,
  type AiMessage,
  type AiToolCallPart,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const STARTED: AiToolCallPart = {
  type: 'tool-call',
  id: 'call-1',
  name: 'search_documents',
  input: { query: 'Q3 plan', limit: 5 },
  state: 'running',
  startedAt: '2026-03-02T10:00:00.000Z',
};

const SEARCH: AiToolCallPart = {
  ...STARTED,
  state: 'succeeded',
  output: { results: [{ title: 'Q3 planning notes', id: 'doc-18' }], total: 1 },
  endedAt: '2026-03-02T10:00:01.400Z',
};

const STATES: AiToolCallPart[] = [
  { ...STARTED, id: 's1', state: 'pending-approval' },
  { ...STARTED, id: 's2', state: 'running' },
  SEARCH,
  {
    ...STARTED,
    id: 's4',
    state: 'failed',
    error: 'The document index did not respond.',
    endedAt: '2026-03-02T10:00:05.000Z',
  },
  { ...STARTED, id: 's5', state: 'denied' },
  { ...STARTED, id: 's6', state: 'cancelled' },
];

const describeSearch = (part: AiToolCallPart) => {
  const query = (part.input as { query?: string } | null)?.query;
  if (part.name !== 'search_documents' || !query) return undefined;
  return part.state === 'succeeded'
    ? `Searched documents for “${query}”`
    : `Search documents for “${query}”`;
};

const MESSAGE: AiMessage = {
  id: 'm1',
  role: 'assistant',
  createdAt: '2026-03-02T10:00:02.000Z',
  parts: [SEARCH, { type: 'text', text: 'I found one document about the Q3 plan.', state: 'done' }],
};

export default {
  id: 'ai-tool-call',
  title: 'AiToolCall',
  summary: 'A tool call in an assistant reply as a collapsible card with state and details.',
  sections: {
    purpose: {
      summary:
        'Shows what the assistant did with a tool in plain words (“Searched documents for “Q3 plan””), whether it is waiting, running, done or failed, how long it took, and on demand the exact input and output.',
      useWhen: [
        'An assistant message contains a `tool-call` part and people should see what the assistant did.',
        'Through `AiMessageView renderPart` to replace the compact default tool row with a richer card.',
        'Audit or debugging views where the raw arguments and results must be inspectable.',
      ],
      avoidWhen: [
        'The call waits for a decision: render AiToolApproval instead while `state` is `pending-approval`.',
        'You want to show the result of the action as a thing (a created record, a sent email): use AiToolResultCard.',
      ],
    },
    designPrinciples: {
      items: [
        'Lead with what happened in human words; the raw tool name and JSON are one click away, never the headline.',
        'State is always an icon and a word, never colour alone.',
        'Collapsed by default so a reply with many tool calls stays readable.',
        'A failure is visible without expanding: the error line sits outside the disclosure.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Summary priority: `summary` → `describeTool(part)` → `toolLabels[part.name]` → the humanized name (`search_documents` → “Search documents”).',
        'Helpers: `humanizeToolName(name)`, `toolLabel(name, toolLabels?)`, `formatToolValue(value)` (pretty JSON, or the plain string) and `toolCallDuration(part)` (milliseconds or `null`).',
        'Input and output render through AiCodeBlock with the `json` language, wrapped, with Copy. They mount only while expanded.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants. The look follows `part.state`, exposed as `data-state`.'],
    },
    states: {
      items: [
        {
          name: 'Waiting for approval',
          attribute: 'data-state="pending-approval"',
          description: 'Warning icon and “Waiting for approval”.',
        },
        {
          name: 'Running',
          attribute: 'data-state="running"',
          description: 'A spinner and “Running”. No duration until it ends.',
        },
        {
          name: 'Done',
          attribute: 'data-state="succeeded"',
          description: 'Check icon, “Done” and the duration.',
        },
        {
          name: 'Failed',
          attribute: 'data-state="failed"',
          description: 'Error icon, “Failed”, and `part.error` below the summary.',
        },
        { name: 'Denied', attribute: 'data-state="denied"', description: 'The person said no.' },
        {
          name: 'Cancelled',
          attribute: 'data-state="cancelled"',
          description: 'The reply was stopped before the call ended.',
        },
        {
          name: 'Expanded',
          attribute: 'data-open',
          description: 'The disclosure is open: tool name, input and output.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the summary row opens and closes the details.',
        'When `part.state` changes after mount, “<tool>: <state>” is announced politely. The initial state is never announced, so history does not chatter.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the summary row, then into the open details (Copy buttons).',
        },
        { keys: 'Enter / Space', action: 'Opens or closes the details (native `<summary>`).' },
      ],
    },
    accessibility: {
      role: 'Native `<details>`/`<summary>` disclosure',
      name: 'The summary row: the humanized summary, the state text and the duration',
      items: [
        'State icons are `aria-hidden`; the state word carries the meaning.',
        'A hidden polite `role="status"` announces state changes; turn it off with `announce={false}` when a thread already announces them.',
        'The error line is prefixed with a hidden “Error:”.',
        'Input and output blocks are named “Input” and “Output” through AiCodeBlock.',
      ],
    },
    responsive: {
      items: [
        'The summary wraps: the state badge and duration move under the title at narrow widths; there is no horizontal scroll at 390px.',
        'JSON wraps instead of scrolling sideways.',
        'On coarse pointers the summary row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Card: `--mn-color-bg-subtle` with `--mn-color-border-subtle`; the tool icon uses `--mn-color-ai-tool`.',
        'State icons: success, danger and warning strong tokens; denied and cancelled use muted text.',
        'Forced colours: CanvasText borders and icons; Highlight focus ring.',
        'The chevron flips in RTL; its rotation is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Humanized summary with details',
          stack: true,
          render: () => <AiToolCall part={SEARCH} describeTool={describeSearch} defaultOpen />,
        },
        {
          title: 'Every state',
          stack: true,
          render: () => (
            <>
              {STATES.map((part) => (
                <AiToolCall key={part.id} part={part} describeTool={describeSearch} />
              ))}
            </>
          ),
        },
        {
          title: 'Inside AiMessageView',
          stack: true,
          render: () => (
            <AiMessageView
              message={MESSAGE}
              showActions={false}
              renderPart={(part) =>
                part.type === 'tool-call' ? (
                  <AiToolCall part={part} describeTool={describeSearch} />
                ) : undefined
              }
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write `describeTool` per tool so the summary reads like a sentence in the past tense once done.',
        'Pass `toolLabels` for tools whose names do not humanize well.',
        'Switch to AiToolApproval while the call is `pending-approval`.',
      ],
      dont: [
        'Do not show raw JSON as the headline.',
        'Do not open every card by default in a long thread.',
        'Do not put secrets in tool input or output: it is shown verbatim on expand.',
      ],
    },
    related: {
      items: [
        { id: 'ai-tool-approval', reason: 'The gate shown while a call waits for approval.' },
        { id: 'ai-tool-result-card', reason: 'A preview of what a finished call produced.' },
        { id: 'ai-message', reason: 'Plug it in through `renderPart`.' },
        { id: 'ai-code-block', reason: 'Renders the input and output JSON.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-code-block', 'spinner'] },
    testing: {
      unit: [
        'Humanizes tool names, formats values (objects, JSON strings, plain strings, circular) and computes durations.',
        'Summary priority: summary, describeTool, toolLabels, humanized name.',
        'Expands to show pretty-printed input and output; shows a failed call’s error without expanding.',
        'Every state has a text label; state changes are announced, the initial one is not.',
        'Message overrides, prop and ref forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
