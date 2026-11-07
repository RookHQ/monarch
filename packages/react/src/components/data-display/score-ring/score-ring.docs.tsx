import { ScoreRing } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'score-ring',
  title: 'ScoreRing',
  summary: 'A circular score with the number written in the centre.',
  sections: {
    purpose: {
      summary:
        'Shows one score on a fixed scale at a glance: an arc filled to the value, coloured by band, with the number as real text in the centre. Needed by: Ranker (visibility, audit and page scores), Recruit (candidate match score).',
      useWhen: [
        'A headline score out of a known maximum (0–100, 0–5) on a dashboard or record header.',
        'A few scores side by side that the reader compares by size and colour.',
      ],
      avoidWhen: [
        'Progress towards finishing a task: use ProgressBar.',
        'A value with no maximum or a trend over time: use StatCard or Sparkline.',
        'A model’s confidence in an answer: use AiConfidenceMeter.',
      ],
    },
    designPrinciples: {
      items: [
        'The number is text, not drawn into the SVG, so it scales with the font, can be read in forced colours and is never colour-only.',
        'Bands carry status meaning (success, info, warning, danger); use `tone="accent"` when the score is not good or bad.',
        'An empty score is a dash and “No score yet”, never a misleading 0.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with `role="meter"`, or `role="img"` when `value` is null. Other div attributes pass through.',
        '`defaultScoreRingThresholds` and `defaultScoreRingMessages` are exported to extend.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Ranker `ScoreRing`): `value`, `size`, `thickness` and `label` are unchanged; `sublabel` → `description`; `variant="compact"` → `showMax={false}`. The Ranker colour bands are the default `thresholds`.',
        'Migrating from Recruit’s marketing mock-up: `score` → `value`, `tone="accent"`; the verdict and signal list stay in the product next to the ring.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['auto', 'neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            '`auto` (default) picks a tone from `thresholds`; any other value is fixed. Exposed as `data-tone`.',
        },
        {
          prop: 'size / thickness',
          values: ['number (px)'],
          description:
            'Diameter (default 120) and stroke width (default a tenth of the size). The number scales with the size.',
        },
        {
          prop: 'showMax',
          values: ['true', 'false'],
          description: 'The small “/ 100” under the number. Default on from 72 px.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Scored',
          attribute: 'data-tone',
          description: 'Arc filled to `value / max` in the band colour.',
        },
        {
          name: 'No score',
          attribute: 'data-empty',
          description: 'Track only, a dash in the centre and “No score yet” for screen readers.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. When the value changes the arc animates to the new length, unless reduced motion is requested.',
      ],
    },
    keyboard: { items: [], notes: ['Not focusable; it is read in the normal reading order.'] },
    accessibility: {
      role: 'meter (img when there is no value)',
      name: 'The visible `label`, else `aria-label`, else “Score”.',
      items: [
        '`aria-valuenow`, `aria-valuemin` and `aria-valuemax` describe the scale; `aria-valuetext` reads “78 out of 100”.',
        'The SVG is hidden from assistive technology; the colour is never the only signal because the number is always written.',
      ],
    },
    responsive: {
      items: ['Fixed size; pick a smaller `size` for dense rows. The label wraps under the ring.'],
    },
    theme: {
      items: [
        'Track `--mn-color-bg-muted`; arc `--mn-color-<tone>-solid`; number `--mn-color-<tone>-strong`, in every theme.',
        'Forced colours: the track is GrayText and the arc and number CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Bands',
          render: () => (
            <>
              <ScoreRing value={92} label="Visibility" />
              <ScoreRing value={67} label="Coverage" />
              <ScoreRing value={44} label="Speed" />
              <ScoreRing value={18} label="Links" />
            </>
          ),
        },
        {
          title: 'Sizes, accent and empty',
          render: () => (
            <>
              <ScoreRing value={78} size={48} aria-label="Page score" />
              <ScoreRing
                value={78}
                size={88}
                tone="accent"
                label="Match"
                description="Updated today"
              />
              <ScoreRing value={null} label="Not scored" />
            </>
          ),
        },
        {
          title: 'Other scales',
          render: () => (
            <ScoreRing
              value={4.2}
              max={5}
              thresholds={[
                { min: 4, tone: 'success' },
                { min: 3, tone: 'warning' },
                { min: 0, tone: 'danger' },
              ]}
              formatValue={(v) => v.toFixed(1)}
              label="Rating"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give each ring a short label.',
        'Use the same scale and thresholds for rings shown together.',
      ],
      dont: [
        'Do not use the accent tone for good/bad scores, or bands for scores that have no meaning.',
        'Do not show 0 for a score that has not been computed; pass `null`.',
      ],
    },
    related: {
      items: [
        { id: 'progress-bar', reason: 'Linear progress towards completion.' },
        { id: 'ai-score', reason: 'AI quality and confidence scores.' },
        { id: 'stat-card', reason: 'A number with a trend and no fixed maximum.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Meter role, value, value text and the number in the centre.',
        'Name from the visible label, `aria-label` or the default.',
        'Clamping, default bands, custom thresholds, fixed tones and value formatting.',
        'Empty state for `null`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
