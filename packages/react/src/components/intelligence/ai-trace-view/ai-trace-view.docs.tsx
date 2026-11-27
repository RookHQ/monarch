import {
  AiTraceView,
  otelSpansToAiTrace,
  type AiTraceSpan,
  type OtlpTraceData,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-trace-view.fixture.json';

// The spans of the primary example come from the fixture; the rest (the hidden tool call, previews and
// attributes shown only in the details) is web-only and stays here.
const SPAN_DETAILS: Record<string, Partial<AiTraceSpan>> = {
  plan: {
    model: 'large-model',
    input: 'Which renewals are due this quarter?',
    output: '1. Search records\n2. Hand over to the analysis agent\n3. Summarise',
  },
  search: { attributes: { index: 'renewals', hits: 14, cached: false } },
  'analysis-model': { model: 'large-model' },
};

const RUN: AiTraceSpan[] = (fixture.data.spans as AiTraceSpan[]).map((span) => ({
  ...span,
  ...SPAN_DETAILS[span.id],
}));
RUN.splice(5, 0, {
  id: 'analysis-tool',
  parentId: 'analysis',
  kind: 'tool',
  name: 'sum_amounts',
  status: 'ok',
  startMs: 2700,
  endMs: 2900,
  input: '{ "column": "amount", "rows": 14 }',
  output: '{ "total": 48200 }',
});

const FAILED: AiTraceSpan[] = [
  { id: 'run', kind: 'agent', name: 'Answer question', status: 'error', startMs: 0, endMs: 7600 },
  { ...(RUN[2] as AiTraceSpan) },
  { ...(RUN[3] as AiTraceSpan) },
  {
    id: 'report',
    parentId: 'run',
    kind: 'agent',
    name: 'Report agent',
    status: 'error',
    startMs: 2600,
    endMs: 7600,
  },
  {
    id: 'fetch',
    parentId: 'report',
    kind: 'tool',
    name: 'fetch_report',
    status: 'error',
    startMs: 2600,
    endMs: 7600,
    error: 'Timed out after 5 seconds.',
    attributes: { attempt: 2, retryable: true },
    input: '{ "report": "renewals", "quarter": 4 }',
  },
];

const RUNNING: AiTraceSpan[] = [
  { id: 'run', kind: 'agent', name: 'Answer question', status: 'running', startMs: 0 },
  { ...(RUN[2] as AiTraceSpan) },
  { ...(RUN[3] as AiTraceSpan) },
  {
    id: 'write',
    parentId: 'run',
    kind: 'model',
    name: 'Write the summary',
    status: 'running',
    startMs: 2600,
    inputTokens: 3900,
  },
];

const START = 1_760_000_000_000;
const nanos = (ms: number) => `${START + ms}000000`;

/** An OTLP/JSON export with GenAI semantic-convention attributes. */
const OTLP: OtlpTraceData = {
  resourceSpans: [
    {
      scopeSpans: [
        {
          spans: [
            {
              spanId: 'a1',
              name: 'invoke_agent Renewals agent',
              startTimeUnixNano: nanos(0),
              endTimeUnixNano: nanos(4200),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'invoke_agent' } },
                { key: 'gen_ai.agent.name', value: { stringValue: 'Renewals agent' } },
              ],
            },
            {
              spanId: 'b1',
              parentSpanId: 'a1',
              name: 'chat large-model',
              startTimeUnixNano: nanos(40),
              endTimeUnixNano: nanos(1900),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'chat' } },
                { key: 'gen_ai.request.model', value: { stringValue: 'large-model' } },
                { key: 'gen_ai.usage.input_tokens', value: { intValue: '2100' } },
                { key: 'gen_ai.usage.output_tokens', value: { intValue: '310' } },
              ],
            },
            {
              spanId: 'c1',
              parentSpanId: 'a1',
              name: 'execute_tool list_renewals',
              startTimeUnixNano: nanos(1900),
              endTimeUnixNano: nanos(4200),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'execute_tool' } },
                { key: 'gen_ai.tool.name', value: { stringValue: 'list_renewals' } },
                { key: 'gen_ai.tool.call.id', value: { stringValue: 'call-1' } },
              ],
              status: { code: 2, message: 'The records service did not answer.' },
            },
          ],
        },
      ],
    },
  ],
};
const FROM_OTEL = otelSpansToAiTrace(OTLP);

export default {
  id: 'ai-trace-view',
  title: 'AiTraceView',
  summary: 'The technical trace of one agent run: a tree grid with a waterfall and span details.',
  sections: {
    purpose: {
      summary:
        'Shows every step one agent run took (model calls, tool calls, retrieval, sub-agents, guardrails) as a nested waterfall with duration, tokens and cost per step, so builders and support staff can see why a run was slow, expensive or wrong.',
      useWhen: [
        'Debugging one run in a builder, an evaluation view or a support console.',
        'The run has nested steps: an agent that calls tools and hands work to sub-agents.',
        'People need the inputs, outputs, attributes and error of one step.',
      ],
      avoidWhen: [
        'Showing a person what an agent did for them in plain terms: use AiAgentRunTimeline or AiChainOfSteps.',
        'Live progress of a task for an end user: use AiTaskProgress.',
        'Comparing many runs: use a data table of runs and open this view for one of them.',
      ],
    },
    designPrinciples: {
      items: [
        'One shared time scale: every bar is placed by its start and duration, so gaps, overlaps and the slow step are visible at a glance.',
        'The bar is never the only carrier: each row has its duration as text and a text alternative with its start.',
        'Errors are never hidden: failed spans and their ancestors start expanded, and "Errors only" strips the rest away.',
        'Large traces stay light: only rows of expanded parents are built, rows are memoised, and above `windowThreshold` visible rows (default 200) only the rows in view are in the document, so a trace of thousands of spans scrolls smoothly.',
        'It reads what tracing already records: `otelSpansToAiTrace` converts OpenTelemetry spans that follow the GenAI semantic conventions, with no vendor SDK.',
        'Details sit beside the table when there is room and below it when there is not; the table keeps its place.',
        'Inputs and outputs are plain text. Markup in a preview is shown, never interpreted.',
        'It only renders what it is given: it never fetches, and nothing is sent or spent by using it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div role="group">`; native attributes pass through.',
        'Times are milliseconds from the run start. A span without `endMs` is still running: its bar ends at `nowMs`, or at the latest known time without it, so server rendering is deterministic.',
        '`inputTokens`, `outputTokens` and `costMicros` are the span’s own. Totals add every span, so do not roll children up into parents.',
        '`costMicros` is in millionths of the span’s `currency` (default: the `currency` prop). A trace with several currencies shows one total per currency.',
        '`aiTraceTotals(spans, { nowMs, currency })` returns the header totals; `formatAiTraceDuration(ms, { locale, units })` and `formatAiTraceCost(micros, currency, locale)` format like the view does; `aiTraceDefaultExpandedIds(spans)` is the default expansion.',
        'Default expansion is read once on mount. For a trace that grows while it runs, control `expandedIds` to open spans as they arrive.',
        'Keep `spans` and `messages` stable between renders (state, a constant or `useMemo`): rows are memoised on them.',
        '`otelSpansToAiTrace(input, options)` converts OpenTelemetry spans to `AiTraceSpan[]`. It reads OTLP/JSON (`resourceSpans` → `scopeSpans` → `spans`, several exports at once too) and the flat shape SDK exporters write (`id`, `parentId`, `timestamp` and `duration` in microseconds, or `startTime` and `endTime` pairs). Nanosecond timestamps may be strings; times become milliseconds from the earliest span, or from `originUnixNano`.',
        'The mapping follows the GenAI semantic conventions: `gen_ai.operation.name` picks the kind (`chat`, `text_completion`, `generate_content` and `embeddings` are model calls, `execute_tool` a tool, `invoke_agent` and `create_agent` an agent), `gen_ai.tool.name` and `gen_ai.agent.name` name the span, `gen_ai.response.model` or `gen_ai.request.model` is the model, and `gen_ai.usage.input_tokens` and `gen_ai.usage.output_tokens` are the tokens. An error status makes the span `error`, with the status message, the `exception` event or `error.type` as its text. A span without an end is `running`.',
        'A parent that is not in the input is dropped, so orphan spans show at the top level; spans without an id and repeated ids are skipped; attributes the mapping does not know are kept in `attributes`. Options: `traceId` keeps one trace, `content: false` leaves prompts and answers out, `attributes` is `rest`, `all` or `none`, and `kind`, `cost` and `link` override the kind, price a span (OpenTelemetry carries no cost) and set `partId` and `messageId`.',
        '`partId` and `messageId` link a span to a conversation part. Rows carry them as `data-part-id` and `data-message-id`; with `onOpenPart` the details of a linked span show "Show in conversation". From OpenTelemetry, `partId` defaults to `gen_ai.tool.call.id` (the `toolCallId` of a tool-call part) and `messageId` to `gen_ai.response.id`.',
        '`aiTraceToRunEvents(spans, { runStartedAt })` returns the events `AiAgentRunTimeline` shows, and `aiTraceToSubagentTree(spans, { runStartedAt })` the agents `AiSubagentTree` shows, so one trace feeds all three views.',
        'Above `windowThreshold` visible rows the grid is windowed: rows outside the view are replaced by a spacer, the grid has `aria-rowcount` and each row `aria-rowindex`, and the focused row always stays in the document. Pass `Infinity` to render every row.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'filter',
          values: ['all', 'errors', 'slowest'],
          description:
            '`all` shows the tree in start order. `errors` keeps failed spans and their ancestors. `slowest` keeps the tree and sorts siblings by duration, longest first, so the slow path is the first row at every level. Controlled with `filter` / `onFilterChange`, or uncontrolled with `defaultFilter`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Filter', attribute: 'data-filter', description: '`all`, `errors` or `slowest`.' },
        {
          name: 'Details open',
          attribute: 'data-details',
          description: 'A span is selected and its details region is shown.',
        },
        {
          name: 'Kind',
          attribute: 'data-kind (on each row)',
          description:
            '`model`, `tool`, `retrieval`, `agent`, `guardrail` or `other`; shown as text in the Kind column and as the bar colour.',
        },
        {
          name: 'Status',
          attribute: 'data-status (on each row and the details)',
          description:
            '`ok`, `error`, `cancelled` or `running`. Anything but `ok` is written beside the name; errors also have an icon.',
        },
        {
          name: 'Expanded',
          attribute: 'aria-expanded (on rows with children)',
          description: 'Children are rendered only while their parent is expanded.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected / aria-selected',
          description: 'The row whose details are open.',
        },
        {
          name: 'Windowed',
          attribute: 'data-windowed (on the scroll box)',
          description:
            'More than `windowThreshold` rows are visible, so only those in view are rendered.',
        },
        {
          name: 'Linked',
          attribute: 'data-part-id / data-message-id (on each row)',
          description: 'The conversation part and message the span belongs to, when given.',
        },
        {
          name: 'Empty',
          description:
            'No spans: "No spans recorded". "Errors only" with no failures: "No errors in this run".',
        },
      ],
    },
    interaction: {
      items: [
        'Click a row to select it and open its details; "Close details" clears the selection.',
        'Click the chevron to expand or collapse a span without selecting it.',
        'The filter switches between every span, errors only, and slowest first.',
        'Copy puts an input or output preview on the clipboard and confirms it.',
        'With `onOpenPart`, "Show in conversation" in the details hands the span’s `partId` and `messageId` to the app, which scrolls to or opens that part.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the filter, then to the grid (one stop), then into the details.',
        },
        { keys: 'Up / Down arrows', action: 'Move to the previous or next row.' },
        {
          keys: 'Right arrow',
          action: 'Expands a collapsed span; on an expanded span moves to its first child.',
        },
        {
          keys: 'Left arrow',
          action:
            'Collapses an expanded span; otherwise moves to the parent. Left and Right swap in right-to-left.',
        },
        { keys: 'Home / End', action: 'Move to the first or last row.' },
        { keys: 'Enter / Space', action: 'Selects the focused row and opens its details.' },
      ],
    },
    accessibility: {
      role: 'group containing a treegrid',
      name: '`messages.label`, default "Run trace"',
      items: [
        'The table is a `treegrid` in the WAI-ARIA row-focus pattern: rows carry `aria-level`, `aria-posinset`, `aria-setsize`, `aria-expanded` and `aria-selected`, with one roving Tab stop.',
        'Each bar is decorative (`aria-hidden`); its cell has the text "Starts at +1.4 s, takes 1.2 s", and the Duration column repeats the length.',
        'Status is text beside the name plus an icon for errors; colour only reinforces it.',
        'Details are a labelled region with description lists; opening them is announced once through a polite status.',
        'Preview blocks are focusable groups named "Input" and "Output", so they can be scrolled with the keyboard.',
        'Forced colours draw bars in CanvasText with bordered tracks, and outline the selected row.',
        'Windowed grids keep the full count for assistive technology (`aria-rowcount`, `aria-rowindex`, `aria-posinset`, `aria-setsize`); Home, End and the arrows reach rows that are not rendered yet, and the spacer rows are hidden from the accessibility tree.',
        'Span names are isolated with `<bdi>` and errors and previews use `dir="auto"`, so right-to-left text from a model or a tool reads correctly in a left-to-right page.',
      ],
    },
    responsive: {
      items: [
        'Container query: from 60rem the details sit beside the table; below that they sit under it.',
        'The table scrolls horizontally and vertically inside its own box with a sticky header; the page never scrolls sideways.',
        'Rows are taller on coarse pointers.',
        'RTL: bars and indents use logical properties, so time runs from the start side.',
      ],
    },
    theme: {
      items: [
        'Bars: model `--mn-color-ai-solid`, tool `--mn-color-ai-tool`, retrieval `--mn-color-info-solid`, agent `--mn-color-text-default` (neutral, so it never reads as an error in a red-accent theme), guardrail `--mn-color-warning-strong`, other `--mn-color-border-strong`; errors `--mn-color-danger-solid`, cancelled `--mn-color-text-disabled`.',
        'Running bars sweep `--mn-color-ai-thinking` over `--mn-duration-shimmer`; static under reduced motion.',
        'The selected row uses `--mn-color-ai-subtle` with an `--mn-color-ai-solid` edge.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A finished run',
          stack: true,
          render: () => (
            <AiTraceView
              spans={RUN}
              currency={fixture.data.currency}
              locale={fixture.data.locale}
            />
          ),
        },
        {
          title: 'A failed tool call, with its details open',
          stack: true,
          render: () => <AiTraceView spans={FAILED} defaultSelectedId="fetch" />,
        },
        {
          title: 'Errors only',
          stack: true,
          render: () => <AiTraceView spans={FAILED} defaultFilter="errors" />,
        },
        {
          title: 'Slowest first, everything expanded',
          stack: true,
          render: () => (
            <AiTraceView
              spans={RUN}
              defaultFilter="slowest"
              defaultExpandedIds={['run', 'analysis']}
            />
          ),
        },
        {
          title: 'From OpenTelemetry spans',
          stack: true,
          render: () => <AiTraceView spans={FROM_OTEL} defaultSelectedId="c1" />,
        },
        {
          title: 'Still running',
          stack: true,
          render: () => <AiTraceView spans={RUNNING} nowMs={5000} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the currency your billing uses and the viewer’s locale.',
        'Give each span only its own tokens and cost, so totals are right.',
        'Trim previews before passing them: a few kilobytes is enough to debug with.',
      ],
      dont: [
        'Do not put secrets or personal data in `input`, `output` or `attributes`; redact them first. With OpenTelemetry input, pass `content: false` where prompts and answers must not reach the screen.',
        'Do not show this to end users as an explanation of an answer; it is a builder and support tool.',
        'Do not pass a new `spans` array on every render.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-run-timeline', reason: 'A flat, person-facing timeline of one run.' },
        { id: 'ai-subagent-tree', reason: 'Which agents took part, without timing.' },
        { id: 'ai-chain-of-steps', reason: 'Steps as a list in a conversation.' },
        { id: 'ai-usage-meter', reason: 'Usage against quotas, not one run.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'segmented-control'],
    },
    testing: {
      unit: [
        '`aiTraceTotals` (duration, tokens, cost per currency, errors, running spans, empty trace), `formatAiTraceDuration` (ms, s, min, locale, units), `formatAiTraceCost` and `aiTraceDefaultExpandedIds`.',
        'Renders a named group and tree grid with the six columns; ref and props forwarding; totals in the header.',
        'Renders only rows of expanded parents with level, position, set size and cell values; 500 spans with collapsed parents render eleven rows.',
        'Places bars on the shared scale with a text alternative; running spans end at `nowMs`.',
        'Keyboard: Up, Down, Home, End, Right to expand and enter, Left to leave and collapse, Enter to select; one Tab stop.',
        'Details show status, timings, tokens, cost, model, error, attributes and previews; previews are plain text and Copy writes them to the clipboard; Close clears the selection.',
        'Chevron toggles without selecting; controlled and uncontrolled expansion, selection and filter.',
        'Errors-only and slowest-first filters, empty states, unknown parents, parent loops shown at the top level, translated strings with locale numbers and money.',
        'Windowing: 5 000 visible rows render one window with `aria-rowcount` and `aria-rowindex`, the window follows scrolling, End and Home reach rows outside it, the focused row survives scrolling, and the threshold can be changed or turned off.',
        'Conversation links: `data-part-id` and `data-message-id` on rows, and "Show in conversation" calling `onOpenPart`.',
        '`otelSpansToAiTrace`: OTLP/JSON and the flat exported shape, nanosecond strings and time pairs, kinds, names, model, tokens, errors from the status and the exception event, running and cancelled spans, orphans, repeated ids, unknown attributes, and every option.',
        '`aiTraceToRunEvents` and `aiTraceToSubagentTree`: kinds, clock times, links, nesting, tool-call counts, activity and parent loops.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
