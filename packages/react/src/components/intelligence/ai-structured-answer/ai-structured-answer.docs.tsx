import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiStructuredAnswer, type AiStructuredField } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const briefSchema: AiStructuredField[] = [
  { key: 'candidate', label: 'Candidate', type: 'text' },
  { key: 'role', label: 'Role', type: 'text' },
  { key: 'stage', label: 'Stage', type: 'badge', tone: 'info' },
  { key: 'score', label: 'Overall score', type: 'number', format: { maximumFractionDigits: 1 } },
  { key: 'strengths', label: 'Strengths', type: 'list' },
  { key: 'risks', label: 'Open questions', type: 'list' },
  { key: 'summary', label: 'Summary', type: 'markdown' },
];

const brief: Record<string, unknown> = {
  candidate: 'Alex Morgan',
  role: 'Senior engineer',
  stage: 'Final interview',
  score: 4.2,
  strengths: ['Clear system design reasoning', 'Strong written communication'],
  risks: ['Limited on-call experience'],
  summary: 'Interviewers agree on **technical depth**. Confirm availability for the start date.',
};

function StreamingDemo() {
  const [count, setCount] = useState(0);
  const keys = briefSchema.map((f) => f.key);
  const streaming = count < keys.length;
  useEffect(() => {
    if (!streaming) return undefined;
    const t = setTimeout(() => setCount((c) => c + 1), 450);
    return () => clearTimeout(t);
  }, [count, streaming]);
  const data = Object.fromEntries(keys.slice(0, count).map((k) => [k, brief[k]]));
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <AiStructuredAnswer
        title="Interview brief"
        schema={briefSchema}
        data={data}
        streaming={streaming}
      />
      <div>
        <Button size="sm" onClick={() => setCount(0)} disabled={streaming}>
          Replay
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'ai-structured-answer',
  title: 'AiStructuredAnswer',
  summary: 'Structured model output as labelled fields: text, lists, numbers, badges and Markdown.',
  sections: {
    purpose: {
      summary:
        'Shows a model’s structured output (JSON matching a schema) as labelled fields instead of a wall of prose, so briefs, score summaries and extracted records are quick to scan and compare. Fields fill in as they stream.',
      useWhen: [
        'The model returns structured data: an interview brief, a lead summary, fields extracted from a document.',
        'The same kind of answer is generated often and people compare them.',
      ],
      avoidWhen: [
        'Free-form answers: use AiMarkdown inside AiMessageView.',
        'Many rows of the same shape: use AiTableAnswer.',
        'Facts the product already knows (not generated): use DescriptionList.',
      ],
    },
    designPrinciples: {
      items: [
        'The schema decides the layout; the model only fills values, so answers look the same every time.',
        'Values are untrusted data: text is rendered as text, Markdown without raw HTML.',
        'Missing fields show a placeholder while streaming and “—” when done, so the layout never jumps.',
        'Always labelled “AI answer” with the Bot glyph.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through.',
        'Built on DescriptionList and DescriptionItem; badges use Badge; Markdown uses AiMarkdown.',
        'Each field value is memoised, so completed fields do not re-render while later ones stream (keep `schema` stable).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['list', 'grid'],
          description:
            '`list` (default): label beside value, one per row, stacking on narrow screens. `grid`: each field as a small card in responsive columns. Exposed as `data-layout`.',
        },
        {
          prop: 'schema[].type',
          values: ['text', 'list', 'number', 'badge', 'markdown'],
          description:
            'How each value renders. `list` and `markdown` fields span the full width unless `wide` says otherwise.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          attribute: 'data-streaming',
          description:
            'Also `aria-busy="true"`. Missing fields show a shimmer bar read as “Still generating”.',
        },
        {
          name: 'Empty field',
          attribute: 'data-empty',
          description: 'On the field once complete: “—” read as “Not provided”.',
        },
      ],
    },
    interaction: {
      items: [
        'Links inside Markdown fields are the only interactive content, besides any `actions`.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab', action: 'Moves through links in Markdown fields and the actions.' }],
      notes: ['The fields themselves are not focusable.'],
    },
    accessibility: {
      role: 'region',
      name: '“AI answer” plus the title (via `aria-labelledby`)',
      items: [
        'AI provenance is the visible “AI answer” label, which is also part of the region’s name.',
        'Fields are a description list (`<dl>`), so each value is read with its label.',
        'While streaming the region is busy; “Response complete.” is announced politely once at the end, never per field.',
      ],
    },
    responsive: {
      items: [
        '`list` stacks label above value below 420px; `grid` drops to one column at 390px.',
        'Long values wrap; nothing scrolls sideways.',
        'Logical properties throughout for RTL.',
      ],
    },
    theme: {
      items: [
        'Card: `--mn-color-bg-surface` with `--mn-color-border-default`.',
        'Label: `--mn-color-ai-subtle`, `--mn-color-ai-border`, `--mn-color-ai-strong`.',
        'Placeholders shimmer with `--mn-color-ai-subtle` over `--mn-duration-shimmer`; static under reduced motion.',
        'Forced colours: CanvasText borders and GrayText placeholders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Interview brief',
          stack: true,
          render: () => (
            <AiStructuredAnswer title="Interview brief" schema={briefSchema} data={brief} />
          ),
        },
        { title: 'Streaming in', stack: true, render: () => <StreamingDemo /> },
        {
          title: 'Grid layout',
          stack: true,
          render: () => (
            <AiStructuredAnswer
              layout="grid"
              title="Account health"
              schema={[
                {
                  key: 'health',
                  label: 'Health',
                  type: 'badge',
                  tone: (v) => (v === 'At risk' ? 'warning' : 'success'),
                },
                { key: 'usage', label: 'Seat usage', type: 'number', format: { style: 'percent' } },
                { key: 'renewal', label: 'Renewal', type: 'text' },
              ]}
              data={{ health: 'At risk', usage: 0.62, renewal: 'In 45 days' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the schema short and ordered by importance.',
        'Use `badge` for values from a small fixed set and give each a tone.',
      ],
      dont: [
        'Do not put long prose in `text` fields; use `markdown`.',
        'Do not rebuild the schema on every render; it defeats memoisation.',
      ],
    },
    related: {
      items: [
        { id: 'ai-table-answer', reason: 'Many rows of the same shape.' },
        { id: 'ai-summary-card', reason: 'A prose summary of a record.' },
        { id: 'description-list', reason: 'The underlying key-value layout.' },
        { id: 'ai-response-skeleton', reason: 'Placeholder before the first field arrives.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['description-list', 'badge', 'ai-markdown'],
    },
    testing: {
      unit: [
        'Named region with the AI label and title; each field type renders correctly; ref and props forwarding.',
        'Streaming placeholders, busy state and the completion announcement.',
        'Model HTML is rendered as text, never as markup.',
        'Grid layout and localised messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
