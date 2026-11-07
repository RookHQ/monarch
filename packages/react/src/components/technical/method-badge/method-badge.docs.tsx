import { MethodBadge } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const endpoints = [
  { method: 'GET', path: '/v1/projects' },
  { method: 'POST', path: '/v1/projects' },
  { method: 'PATCH', path: '/v1/projects/{id}' },
  { method: 'DELETE', path: '/v1/projects/{id}' },
] as const;

export default {
  id: 'method-badge',
  title: 'MethodBadge',
  summary: 'An HTTP method label (GET, POST, PATCH, DELETE) with a tone per method.',
  sections: {
    purpose: {
      summary:
        'Labels an API operation or a logged request with its HTTP method, so people can scan an endpoint list or a request log by what each call does. Needed by: Developers (API reference, endpoint lists, request logs and the log drawer).',
      useWhen: [
        'An API reference or endpoint table lists operations.',
        'A request log or log drawer shows the method of each call.',
        'Documentation pages describe an operation next to its path.',
      ],
      avoidWhen: [
        'The label is a status (succeeded, failed): use StatusBadge.',
        'The label is not an HTTP method: use Badge.',
      ],
    },
    designPrinciples: {
      items: [
        'The method name is the label. Colour only reinforces it: reads are green, creates blue, updates yellow and deletes red.',
        'Monospace and a fixed minimum width, so paths line up in a column of endpoints.',
        'Quiet enough to repeat on every row of a log.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` and forwards its ref and HTML attributes to it.',
        '`methodTone(method)` returns the tone a method gets, for products that colour other elements to match.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`method` accepts any string; the common methods are typed as `HttpMethod` for autocompletion.',
        'Migrating from the app copy (Developers `MethodBadge`): `method` and `className` are unchanged. The app copy coloured POST with the brand accent; Monarch uses the info tone, because the accent never carries meaning. Add `size="sm"` in dense log rows.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`md` (default) for tables and lists, `sm` for dense log rows.',
        },
        {
          prop: 'tone',
          values: ['neutral', 'info', 'success', 'warning', 'danger'],
          description: 'Overrides the tone chosen from the method. Rarely needed.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Method',
          attribute: 'data-method',
          description: 'The lower-case method, for styling or tests.',
        },
        {
          name: 'Tone',
          attribute: 'data-tone',
          description:
            'GET is success, POST info, PUT and PATCH warning, DELETE danger; HEAD, OPTIONS and unknown methods neutral.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Put it inside a link or row when the operation can be opened.'],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable. The surrounding row or link carries keyboard support.'],
    },
    accessibility: {
      role: 'none (inline text)',
      name: 'Its text: the upper-case method.',
      items: [
        'The method is plain text, so screen readers read "DELETE" and colour is never the only cue.',
        'Each tone uses the strong text colour on the subtle background, which meets text contrast in every theme.',
        'In forced-colours mode it keeps a visible border and system text colour.',
      ],
    },
    responsive: {
      items: [
        'Never wraps and never shrinks below its minimum width, so it stays aligned in narrow tables.',
        'Use `size="sm"` in rows that must fit at 390px alongside a long path.',
      ],
    },
    theme: {
      items: [
        'Uses the success, info, warning and danger intent tokens, so it follows every theme.',
        'Text uses `--mn-font-mono`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every method',
          render: () => (
            <>
              {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map((m) => (
                <MethodBadge key={m} method={m} />
              ))}
            </>
          ),
        },
        {
          title: 'Endpoint list',
          stack: true,
          render: () => (
            <ul
              style={{
                listStyle: 'none',
                margin: 0,
                padding: 0,
                display: 'grid',
                gap: 'var(--mn-space-2)',
              }}
            >
              {endpoints.map((e) => (
                <li
                  key={`${e.method} ${e.path}`}
                  style={{ display: 'flex', alignItems: 'center', gap: 'var(--mn-space-3)' }}
                >
                  <MethodBadge method={e.method} />
                  <code
                    style={{ fontFamily: 'var(--mn-font-mono)', fontSize: 'var(--mn-text-sm)' }}
                  >
                    {e.path}
                  </code>
                </li>
              ))}
            </ul>
          ),
        },
        {
          title: 'Small, for log rows',
          render: () => (
            <>
              <MethodBadge method="get" size="sm" />
              <MethodBadge method="post" size="sm" />
              <MethodBadge method="delete" size="sm" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Put the path next to the badge in a monospace font.',
        'Use `size="sm"` in dense request logs.',
      ],
      dont: [
        'Do not use it for statuses or response codes.',
        'Do not override tones per product: the same method should look the same in every app.',
      ],
    },
    related: {
      items: [
        { id: 'badge', reason: 'General labels that are not HTTP methods.' },
        { id: 'status-badge', reason: 'Request outcomes and other statuses.' },
        { id: 'lifecycle-badge', reason: 'The release stage of an API.' },
        { id: 'log-drawer', reason: 'Shows a request in detail, including its method.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Shows the method as upper-case text with the right tone for each method.',
        'Unknown methods render neutral; lower-case input is upper-cased.',
        'Tone override, size, className, HTML attributes and ref.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
