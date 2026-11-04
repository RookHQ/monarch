import { Sparkline } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const VISITS = [32, 41, 38, 45, 52, 49, 61, 58, 66, 72];
const LATENCY = [180, 190, 210, null, 240, 260, 320, 410];
const RANK = [14, 12, 12, 9, 7, 8, 5, 4];
const ROW = { display: 'flex', alignItems: 'center', gap: 12, fontSize: 14 } as const;

export default {
  id: 'sparkline',
  title: 'Sparkline',
  summary:
    'A word-sized trend line, area or sparkbar for tables, stat cards and lists, with gaps, invert, thresholds and a text alternative.',
  sections: {
    purpose: {
      summary:
        'Shows the shape of a recent trend in the space of a word, next to the number it belongs to, without axes or interaction.',
      useWhen: [
        'A trend column in a table (14-day latency per endpoint, rank history per keyword).',
        'A stat card or list row that needs "going up or down" at a glance.',
      ],
      avoidWhen: [
        'Readers need exact values, axes or comparison between series: use LineChart.',
        'The trend is the main content of a section: use LineChart in a ChartFrame.',
      ],
    },
    designPrinciples: {
      items: [
        'The number leads; the sparkline supports it. Show the value or delta in text beside it.',
        'Gaps break the line, so missing data never looks like a trend.',
        'Tone carries meaning only with words: thresholds add "Warning" or "Critical" to the text alternative.',
        'No axes, labels or tooltips: when you need them, it is a LineChart.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` (ref to it) holding an `aria-hidden` SVG; HTML attributes pass through.',
        'With `label` the span is `role="img"` with a generated "latest, low, high" summary (or `summary`); without it the sparkline is decorative.',
        'Fewer than two values (one for `bar`) show "Not enough data" (`labels.notEnoughData`) instead of a drawing.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['line', 'area', 'bar'],
          description: 'Line, line with a soft fill, or a sparkbar. `data-variant`.',
        },
        {
          prop: 'tone',
          values: ['current', 'neutral', 'accent', 'success', 'warning', 'danger'],
          description: '`current` (default) inherits the text colour. `data-tone`.',
        },
        {
          prop: 'color',
          values: ['1', '2', '3', '4', '5', 'neutral'],
          description: 'A data palette slot, to match a series elsewhere. Wins over `tone`.',
        },
        {
          prop: 'markers',
          values: ['last', 'all', 'none'],
          description: 'Dots on the latest point (default), every point or none.',
        },
        {
          prop: 'invert',
          values: ['true', 'false'],
          description: 'Smaller values drawn higher, for ranks where lower is better.',
        },
        {
          prop: 'responsive',
          values: ['true', 'false'],
          description: 'Fills the container width with a constant stroke width. `data-responsive`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Decorative',
          attribute: 'aria-hidden="true"',
          description: 'No `label`: hidden from assistive technology.',
        },
        {
          name: 'Labelled',
          attribute: 'role="img"',
          description: 'With `label`: an image with a text summary.',
        },
        {
          name: 'Threshold tone',
          attribute: 'data-tone="warning" | "danger"',
          description: 'The latest value crossed `thresholds`; the summary names the state.',
        },
        {
          name: 'Not enough data',
          attribute: 'data-empty',
          description: 'A short muted text instead of the drawing.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. Put the sparkline inside a link or row if the trend should open details.',
      ],
    },
    keyboard: { items: [], notes: ['Not focusable: it has no interaction of its own.'] },
    accessibility: {
      role: 'img (with `label`), otherwise hidden',
      name: '`label`, followed by the generated summary: "p95 latency: latest 410 ms, low 180 ms, high 410 ms. Critical".',
      items: [
        'Without a `label` the sparkline is `aria-hidden`; make sure the same information is in nearby text.',
        'Summary numbers use `Intl.NumberFormat` with `locale` and `valueFormat`, or `formatValue`.',
        'Threshold state is spelled out in the summary, not left to colour.',
        'Translate every word through `labels` (summary template, Warning, Critical, Not enough data).',
      ],
    },
    responsive: {
      items: [
        'Fixed size by default (80 × 24); `responsive` stretches to the container and redraws at its measured width.',
        'The stroke keeps its width when stretched (`vector-effect: non-scaling-stroke`).',
      ],
    },
    theme: {
      items: [
        'Tones use the intent `strong` tokens and `--mn-color-text-subtle`; slots use `--mn-color-data-*`. All follow light, dark and high contrast.',
        'No animation.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Line, area and sparkbar',
          render: () => (
            <>
              <Sparkline values={VISITS} />
              <Sparkline values={VISITS} variant="area" color={1} />
              <Sparkline values={VISITS} variant="bar" color={2} />
            </>
          ),
        },
        {
          title: 'In table rows: thresholds, gaps and an inverted rank',
          stack: true,
          render: () => (
            <>
              <div style={ROW}>
                <span>GET /search</span>
                <Sparkline
                  values={LATENCY}
                  label="p95 latency, last 8 days"
                  thresholds={{ warning: 300, danger: 400 }}
                  formatValue={(v) => `${v} ms`}
                />
                <span>410 ms</span>
              </div>
              <div style={ROW}>
                <span>“project tracker”</span>
                <Sparkline values={RANK} invert label="Rank, last 8 weeks" color={1} />
                <span>#4</span>
              </div>
            </>
          ),
        },
        {
          title: 'Responsive and not enough data',
          stack: true,
          render: () => (
            <>
              <Sparkline values={VISITS} variant="area" responsive height={40} color={3} />
              <Sparkline values={[12]} label="Score" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the current value or delta as text next to the sparkline.',
        'Give sparklines in one table a shared `domain` when rows should be compared.',
        'Pass `label` when the sparkline is the only place the trend appears.',
      ],
      dont: [
        'Rely on a green or red line alone to say good or bad.',
        'Use a sparkline where people need exact values; use LineChart.',
      ],
    },
    related: {
      items: [
        { id: 'line-chart', reason: 'The full chart with axes, tooltip and table.' },
        { id: 'stat-card', reason: 'A headline number that can carry a sparkline.' },
        {
          id: 'delta-indicator',
          reason: 'The change as text with direction, next to the sparkline.',
        },
        { id: 'data-table', reason: 'Tables are the most common home for sparklines.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: ['Paths, scales and palette come from `src/internal/charts`.'],
    },
    testing: {
      unit: [
        'Decorative by default (aria-hidden, last-point marker); no axe violations.',
        'With `label`: role img and a locale-formatted latest / low / high summary.',
        'Thresholds above and below set the tone and name the state.',
        'Gaps split the line; lone points get a dot.',
        '`invert` flips the drawing; a fixed `domain` positions values on the shared scale.',
        'Sparkbar and area variants.',
        'Not enough data shows text (with the label for screen readers) and no SVG.',
        'Palette slot, responsive width and ref forwarding.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/sparkline',
        'Cross-browser axe: /components/sparkline',
      ],
    },
  },
} satisfies ComponentDocs;
