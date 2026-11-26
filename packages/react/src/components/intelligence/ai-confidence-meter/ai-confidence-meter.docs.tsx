import { AiConfidenceMeter } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-confidence-meter',
  title: 'AiConfidenceMeter',
  summary: 'How sure the AI is about a value: low, medium or high, as a badge or a bar.',
  sections: {
    purpose: {
      summary:
        'Turns a model’s 0–1 confidence score into a band people can act on. Low confidence reads "Needs check", so people know which AI values to verify before they rely on them.',
      useWhen: [
        'An AI-extracted or AI-suggested value has a confidence score (form autofill, classification, matching).',
        'A list of AI results should show which ones need review (`variant="badge"`, `size="sm"`).',
        'A detail view should show the score with an explanation (`variant="bar"`).',
      ],
      avoidWhen: [
        'There is no real score from the model: do not invent one.',
        'The value is a business score (lead score, risk score): use AiScore.',
        'You only need to mark content as AI-made: use AiGeneratedBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'Three bands, not a raw number: people decide faster with "Needs check" than with "0.58".',
        'Every band has text and an icon as well as a colour.',
        'Low confidence is phrased as an action ("Needs check"), not a judgement.',
        'Thresholds belong to the product: tune `thresholds` per model and task rather than changing the labels.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiConfidenceLevel(value, thresholds?)` is exported for apps that branch on the band; `DEFAULT_CONFIDENCE_THRESHOLDS` is `{ low: 0.6, high: 0.85 }`.',
        'Below `low` is low, at or above `high` is high, anything between is medium. Values outside 0–1 are clamped and `NaN` counts as 0.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['badge', 'bar'],
          description:
            '`badge` is a compact pill; `bar` is a labelled meter with the percentage and an explanation underneath. Exposed as `data-variant`.',
        },
        {
          prop: 'size',
          values: ['md', 'sm'],
          description: '`sm` for table cells and dense lists. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Low',
          attribute: 'data-level="low"',
          description: 'Warning icon, "Needs check", danger colours.',
        },
        {
          name: 'Medium',
          attribute: 'data-level="medium"',
          description: 'Partial icon, "Medium confidence", warning colours.',
        },
        {
          name: 'High',
          attribute: 'data-level="high"',
          description: 'Check icon, "High confidence", success colours.',
        },
        {
          name: 'With explanation',
          attribute: 'tabindex="0" (badge)',
          description:
            'The badge becomes a tab stop and shows the explanation in a tooltip; the bar shows it as text below.',
        },
      ],
    },
    interaction: {
      items: [
        'Display only. A badge with an explanation opens a tooltip on hover and focus.',
        'The bar fill animates when the value changes, except under reduced motion.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses a badge that has an explanation, which opens it.' },
        { keys: 'Escape', action: 'Closes the explanation tooltip.' },
      ],
      notes: ['Without an explanation, and in the bar variant, nothing is focusable.'],
    },
    accessibility: {
      role: 'meter (bar); inline text (badge)',
      name: '"Confidence" for the bar; the badge reads "Needs check, 42% confidence"',
      items: [
        'The bar is `role="meter"` with `aria-valuenow` 0–100 and `aria-valuetext` carrying the band label and percentage; the explanation is its description.',
        'The badge reads one sentence from visually hidden text (band, percentage, explanation); the visible pieces are hidden from assistive technology so nothing is read twice.',
        'Icons are decorative; the band is always in text.',
      ],
    },
    responsive: {
      items: [
        'The badge never wraps and truncates a long custom label; the bar fills its container and its header wraps, so there is no horizontal scroll at 390px.',
      ],
    },
    theme: {
      items: [
        'Text and fill use `--mn-color-confidence-low`, `-medium` and `-high`; badge backgrounds and borders use the danger, warning and success subtle and border tokens.',
        'The bar track is `--mn-color-bg-muted`.',
        'In forced colours the badge is a CanvasText outline and the bar fill is CanvasText inside an outlined track.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Badges',
          render: () => (
            <>
              <AiConfidenceMeter value={0.42} />
              <AiConfidenceMeter value={0.72} />
              <AiConfidenceMeter value={0.93} showValue />
            </>
          ),
        },
        {
          title: 'Badge with an explanation',
          render: () => (
            <AiConfidenceMeter value={0.5} explanation="Two sources give different dates." />
          ),
        },
        {
          title: 'Bar',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 'var(--mn-space-4)', maxWidth: '20rem' }}>
              <AiConfidenceMeter
                variant="bar"
                value={0.88}
                explanation="Matched the invoice number and total."
              />
              <AiConfidenceMeter
                variant="bar"
                value={0.55}
                explanation="The due date was read from a low-quality scan."
              />
            </div>
          ),
        },
        {
          title: 'Custom thresholds',
          render: () => <AiConfidenceMeter value={0.8} thresholds={{ high: 0.95 }} showValue />,
        },
      ],
    },
    doDont: {
      do: [
        'Show the badge next to every AI value that has a score, and draw attention to "Needs check".',
        'Explain low confidence when you can.',
        'Tune `thresholds` per model and task.',
      ],
      dont: [
        'Do not show a score the model did not produce.',
        'Do not rely on colour alone; keep the labels.',
        'Do not use it for business scores.',
      ],
    },
    related: {
      items: [
        { id: 'ai-generated-badge', reason: 'Marks content as AI-made.' },
        { id: 'ai-autofill-field', reason: 'Shows this badge on AI-filled form fields.' },
        { id: 'ai-score', reason: 'Business scores with bands and factors.' },
        { id: 'progress-bar', reason: 'Task progress, not confidence.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['tooltip'] },
    testing: {
      unit: [
        'Maps values, out-of-range values and NaN to bands; partial thresholds.',
        'Label, icon and hidden value text per band; optional visible value; ref and prop forwarding.',
        'Badge explanation is in the accessible text and opens a tooltip on keyboard focus.',
        'Bar is a named meter with value, value text and description; clamping and localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
