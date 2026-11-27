import { Gauge } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './gauge.fixture.json';

const LOAD_BANDS = fixture.data.bands as readonly {
  from: number;
  to: number;
  tone: 'success' | 'warning' | 'danger';
  label: string;
}[];

export default {
  id: 'gauge',
  title: 'Gauge',
  summary:
    'Semi-circular meter for one reading on a scale, with optional meaning bands, a target marker and the value written as text.',
  sections: {
    purpose: {
      summary:
        'Shows one reading against a fixed scale where position on the scale matters: utilisation, a quota used, a health score against thresholds. Needed by: none recorded yet in the Rook apps (they use lucide `Gauge` as an icon only).',
      useWhen: [
        'A single value with a known minimum and maximum, read at a glance on a dashboard.',
        'The scale has meaning bands (healthy, high, critical) or a target.',
      ],
      avoidWhen: [
        'A score out of 100 with no bands or target: ScoreRing is more compact.',
        'Progress through a task: use ProgressBar or ProgressCircle.',
        'Comparing several values: bars compare lengths far better than arcs.',
        'A value over time: use Sparkline or LineChart.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is text in the middle, so the arc is a reading aid, never the only signal.',
        'Bands carry status meaning, so they use status tones; the value arc takes the tone of the band it falls in, and the band name is shown and spoken.',
        'Out-of-range values are clamped on the arc but shown truthfully in the text ("125%").',
        'Static by design: no sweeping animation, so the reading is instant and motion-safe.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'A `role="meter"` element with `aria-valuemin`, `aria-valuemax`, `aria-valuenow` (clamped) and `aria-valuetext`; with `value={null}` it is an image named by its label with "No data".',
        'Bands are `[from, to)`; the band ending at `max` also includes `max`.',
        'Numbers go through `Intl.NumberFormat` with `locale` and `valueFormat`, or `formatValue`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'indicator',
          values: ['arc', 'needle'],
          description:
            '`arc` fills the track to the value with thin band strips outside it; `needle` draws the bands as the track and points at the value. Exposed as `data-indicator`.',
        },
        {
          prop: 'tone',
          values: ['auto', 'neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            "`auto` takes the band's tone (accent without bands). Exposed as `data-tone`.",
        },
        {
          prop: 'showRange',
          values: ['true', 'false'],
          description: 'Minimum and maximum under the ends of the arc.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'In a band',
          attribute: 'data-band',
          description: 'The band label shows under the value and is added to the spoken value.',
        },
        {
          name: 'No value',
          attribute: 'data-empty',
          description: 'Empty track, a dash, role img and the "No data" text.',
        },
        {
          name: 'Target',
          description: 'A tick across the track; "Target 80" is the accessible description.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive: a gauge displays one reading. Put actions (open details) in the surrounding card.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable; screen readers reach the meter in reading order.'],
    },
    accessibility: {
      role: 'meter (img when there is no value)',
      name: 'The visible `label`, `aria-label`, or `messages.name` ("Gauge").',
      items: [
        '`aria-valuetext` reads "72, Healthy" with bands, or "42 (from 0 to 100)" without, using the formatted numbers.',
        'The description and target are linked with `aria-describedby`.',
        'The SVG is `aria-hidden`; the band name is visible text as well as spoken, so tone is never the only signal.',
      ],
    },
    responsive: {
      items: [
        'Scales with its container up to `size` px (an SVG `viewBox`), so it fits a 390px card without overflow.',
        'Text inside the arc scales with the drawing; the label and description below wrap.',
      ],
    },
    theme: {
      items: [
        'Track uses `--mn-color-bg-muted`; value and bands use the status and accent solid tokens; the number uses the matching strong token for contrast.',
        'Forced colours: the track is GrayText, the value, bands, needle and text CanvasText; band names carry the meaning.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With bands',
          render: () => (
            <Gauge
              value={fixture.data.value}
              bands={LOAD_BANDS}
              label={fixture.data.label}
              description={fixture.data.description}
              valueFormat={fixture.data.valueFormat as Intl.NumberFormatOptions}
              locale={fixture.data.locale}
            />
          ),
        },
        {
          title: 'Needle with a target',
          render: () => (
            <Gauge
              value={0.62}
              max={1}
              indicator="needle"
              bands={[
                { from: 0, to: 0.5, tone: 'danger', label: 'Below plan' },
                { from: 0.5, to: 0.8, tone: 'warning', label: 'Close' },
                { from: 0.8, to: 1, tone: 'success', label: 'On plan' },
              ]}
              target={0.8}
              label="Quarterly target"
              valueFormat={{ style: 'percent' }}
              locale="en-US"
            />
          ),
        },
        {
          title: 'Plain, small and empty',
          render: () => (
            <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
              <Gauge value={340} max={500} label="Storage (GB)" size={140} locale="en-US" />
              <Gauge value={null} label="Error budget" size={140} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name bands, so the meaning is in words as well as colour.',
        'Use `valueFormat` for units and percentages.',
        'Use a fixed tone (`accent`) for readings with no good or bad meaning.',
      ],
      dont: [
        'Put several gauges side by side to compare values; use bars.',
        'Use bands to decorate a scale that has no thresholds.',
        'Animate the needle on load.',
      ],
    },
    related: {
      items: [
        { id: 'score-ring', reason: 'A full-circle score out of a maximum, more compact.' },
        { id: 'progress-bar', reason: 'Progress through a task, or a horizontal meter.' },
        { id: 'stat-card', reason: 'A headline number with its trend.' },
        { id: 'sparkline', reason: 'The same reading over time.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: ['Number formatting comes from `src/internal/charts`.'],
    },
    testing: {
      unit: [
        'Meter role, name, value attributes, value text, description; no axe violations.',
        'Band tone, name and spoken value; the top of the scale belongs to the last band.',
        'Needle, target marker (description) and fixed tone.',
        'Clamped drawing with the real value as text; Intl percent formatting.',
        'Empty value: img role, dash, translated messages, no range.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
