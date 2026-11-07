import { AiScore } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const factors = [
  { label: 'Opened the last three emails', impact: 14, detail: 'All within the past week' },
  { label: 'Visited the pricing page', impact: 9 },
  { label: 'No meeting booked yet', impact: -11 },
  { label: 'Company size', impact: 0, detail: 'Within the usual range' },
];

export default {
  id: 'ai-score',
  title: 'AiScore',
  summary: 'A 0–100 model score as a ring or bar, with its band, rationale and factors.',
  sections: {
    purpose: {
      summary:
        'Shows a score a model produced (lead fit, churn risk, health) so people can read it at a glance and check it: the number, its band in words, why the model gave it and which factors raised or lowered it.',
      useWhen: [
        'A model scores a record from 0 to 100 and people act on the score.',
        'You can explain the score with a rationale or a factor breakdown.',
      ],
      avoidWhen: [
        'A deterministic metric (revenue, a count): use StatCard.',
        'Progress toward completion: use ProgressBar.',
        'How sure the model is about an answer, not a score of the record: use a confidence indicator.',
      ],
    },
    designPrinciples: {
      items: [
        'Text first: the number sits in the centre of the ring as real text, the band is a word ("High"), and the factor impacts are signed numbers. The ring, bar and colours only reinforce them.',
        'Explainable: a score without its rationale or factors invites blind trust. Pass at least one.',
        'Always marked as an AI score.',
        'The band legend says what each range means, so colour never has to.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiScoreBand(value, bands)` returns the band a value falls in and `aiScoreBandRanges(bands)` the sorted bands with their computed `max`, for apps that show the band elsewhere.',
        '`DEFAULT_AI_SCORE_BANDS` is Low (0, danger), Medium (40, warning), High (70, success). Pass `bands` to rename or re-cut them, for example for a risk score where high is bad.',
        'A band runs from its `min` to one below the next band’s `min`; the last band ends at 100.',
        'Values are clamped to 0–100 and rounded for display; `null` shows "No score yet".',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['ring', 'bar'],
          description:
            '`ring` is a circular gauge with the number in its centre; `bar` is a horizontal bar with ticks at band boundaries and the number above it. Exposed as `data-variant`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Ring diameter: 4rem or 6rem. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Scored',
          attribute: 'data-tone',
          description:
            'The number, the arc or fill to that value, and the band label. `data-tone` is the band’s tone.',
        },
        {
          name: 'No score',
          attribute: 'data-empty',
          description: '`value={null}`: "No score yet" in place of the number, no arc and no band.',
        },
        {
          name: 'Legend open',
          attribute: 'open on the details element',
          description: 'The band table is expanded and marks the current band in words.',
        },
      ],
    },
    interaction: {
      items: [
        'Not interactive apart from the band legend disclosure.',
        'When `value` changes the arc or fill moves to the new value with a short transition (none under reduced motion).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Opens or closes the band legend (on its summary).' },
      ],
      notes: ['The legend summary is the only tab stop.'],
    },
    accessibility: {
      role: 'group',
      name: 'The label, e.g. "Lead score" (aria-labelledby)',
      items: [
        'The score is read as text: "72 out of 100", then the band ("High"). The "/100" shown visually is hidden and replaced with the localisable `messages.outOf`.',
        'The ring SVG and the bar are `aria-hidden`; they add nothing a screen reader needs.',
        'Factors are a real `<table>` with a caption, row headers and signed impacts ("+14", "-11"), so direction never depends on green or red.',
        'The band legend is a table inside `<details>`; the current band is marked "(this score)" in text.',
        'The "AI score" chip is visible text, so the score is identified as AI-generated to everyone.',
      ],
    },
    responsive: {
      items: [
        'The band label wraps under the ring or bar when space is short; the factor table uses two columns with details under the factor name, so it fits 390px without horizontal scroll.',
        'The legend summary is at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Band tones map to `--mn-color-{danger,warning,success,info}-strong`, `--mn-color-text-muted` (neutral) and `--mn-color-ai-solid` (ai).',
        'The track uses `--mn-color-border-default`; factor bars use the success and danger strong colours on `--mn-color-bg-muted`.',
        'In forced colours the arc, fills and swatches use CanvasText on a GrayText track, so the value stays visible.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Ring with rationale and factors',
          stack: true,
          render: () => (
            <AiScore
              value={72}
              label="Lead score"
              rationale="Engaged with recent emails and pricing, but no meeting is booked yet."
              factors={factors}
            />
          ),
        },
        {
          title: 'Bar',
          stack: true,
          render: () => <AiScore value={48} label="Account health" variant="bar" />,
        },
        {
          title: 'Custom bands for a risk score',
          stack: true,
          render: () => (
            <AiScore
              value={81}
              label="Churn risk"
              variant="bar"
              bands={[
                { min: 0, label: 'Low risk', tone: 'success' },
                { min: 50, label: 'Elevated', tone: 'warning' },
                { min: 75, label: 'High risk', tone: 'danger' },
              ]}
              rationale="Usage fell sharply and two invoices are unpaid."
            />
          ),
        },
        {
          title: 'Small ring and no score yet',
          render: () => (
            <>
              <AiScore value={35} size="sm" label="Fit" showBands={false} />
              <AiScore value={null} size="sm" label="Fit" showBands={false} />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give a rationale or factors with every score.',
        'Name bands for what they mean to the person ("High risk"), not just "Low/Medium/High", when the direction is not obvious.',
        'Show `null` while a score is being computed rather than 0.',
      ],
      dont: [
        'Do not use it for numbers that are not model output.',
        'Do not hide the band legend when the bands are custom and not self-explanatory.',
        'Do not rely on the ring colour to say whether the score is good.',
      ],
    },
    related: {
      items: [
        { id: 'stat-card', reason: 'A deterministic metric, not a model score.' },
        { id: 'progress-bar', reason: 'Progress toward completion.' },
        {
          id: 'ai-insight-callout',
          reason: 'Explain a notable change in the score as an insight.',
        },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        '`aiScoreBand` picks the right band at boundaries; `aiScoreBandRanges` sorts and computes ranges.',
        'Named group, value as text in the ring centre ("72 out of 100"), arc length, hidden SVG; props and ref forwarded.',
        'Band label as text with its tone; the AI chip is present.',
        'Clamping, the null state and the bar variant with band ticks.',
        'Factors render as a captioned table with row headers and signed impacts.',
        'Band legend table marks the current band; it can be hidden; custom bands and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
