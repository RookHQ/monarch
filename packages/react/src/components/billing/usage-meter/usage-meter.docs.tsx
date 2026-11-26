import { UsageMeter } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'usage-meter',
  title: 'UsageMeter',
  summary: 'Usage against limits: figures, quota meters and near/over-limit text per metered item.',
  sections: {
    purpose: {
      summary:
        'Shows how much of each metered quantity has been used in a period, against its limit when there is one: requests, messages, storage, seats, connection minutes. Needed by: Developers (Usage and API detail pages, per-product units).',
      useWhen: [
        'Plan quotas, API limits or seat counts with a known maximum.',
        'Usage counters without a limit, shown as tiles next to limited ones.',
      ],
      avoidWhen: [
        'Usage over time: use LineChart or BarChart.',
        'A single task’s progress: use Progress.',
      ],
    },
    designPrinciples: {
      items: [
        'A quota is a measurement, not a status: meters are neutral until near the limit, then warning, then danger over it.',
        'The level is always written (“Near the limit · 85% used”), so colour is never the only signal.',
        'Only real numbers: items without a limit show the count alone.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<section>`.',
        '`usageLevel(used, limit, warningAt)` returns `normal`, `warning`, `over` or `unlimited`, for products that need the same rule elsewhere.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Developers `components/console/UsageWidgets`): `StatTiles totals` → `items` such as `{ id: "requests", label: "Requests", used: totals.requests }` (error rate and latency stay as StatCards); `UnitTiles units` → one item per `unitEntries(units)` with `label: unitLabel(key)` and the hint as `description`; quotas from the API detail page add `limit`. `DailyBars` is a chart, not part of this component: use BarChart.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['grid', 'list'],
          description: 'Tiles that fill the width, or stacked rows in one bordered list.',
        },
        {
          prop: 'notation',
          values: ['standard', 'compact'],
          description: '`compact` writes 1.5K for the default formatter.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Level',
          attribute: 'data-level',
          description:
            'Per item: `normal`, `warning` (at `warningAt`, default 80%), `over`, or `unlimited` (no limit).',
        },
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'Skeleton tiles and `aria-busy`.',
        },
        {
          name: 'Error',
          attribute: 'data-error',
          description: 'InlineErrorRetry with `onRetry`, otherwise an alert.',
        },
      ],
    },
    interaction: {
      items: ['Not interactive apart from anything passed in `actions`.'],
    },
    keyboard: {
      items: [],
      notes: [
        'Nothing to operate; meters are read-only. Controls in `actions` keep their own keyboard support.',
      ],
    },
    accessibility: {
      role: 'region with a list of meters (`role="meter"`)',
      name: 'The `title` heading or `aria-label`; each meter is named by its item label.',
      items: [
        'Each meter has `aria-valuetext` such as “7,200 of 10,000 used”.',
        'A polite live region announces items that are near or over the limit when the data changes.',
        'Warning and over-limit lines have an icon and text, not just colour.',
      ],
    },
    responsive: {
      items: [
        'Tiles wrap with `auto-fill` and a 12rem minimum, down to one column at 390px.',
        'Long numbers wrap rather than overflow.',
      ],
    },
    theme: {
      items: [
        'Meters use Progress tones (`neutral`, `warning`, `danger`) from `--mn-*` tokens in every theme.',
        'Forced-colours mode keeps borders and writes levels in system text colour.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Quotas and counts',
          render: () => (
            <UsageMeter
              title="Usage"
              period="This month"
              resetsAt="2026-12-01T00:00:00Z"
              timeZone="UTC"
              locale="en-US"
              items={[
                { id: 'req', label: 'Requests', used: 4200, limit: 10000 },
                { id: 'msg', label: 'Messages', used: 8500, limit: 10000 },
                { id: 'gb', label: 'Storage', used: 12, limit: 10, unit: 'GB' },
                {
                  id: 'conv',
                  label: 'Conversations',
                  used: 31,
                  description: 'Conversations started',
                },
              ]}
            />
          ),
        },
        {
          title: 'List layout, compact numbers',
          render: () => (
            <UsageMeter
              aria-label="Seats and calls"
              layout="list"
              notation="compact"
              locale="en-US"
              items={[
                { id: 'calls', label: 'API calls', used: 1_520_000, limit: 2_000_000 },
                { id: 'seats', label: 'Seats', used: 18, limit: 25 },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give the unit when it is not a plain count.',
        'Show when the quota resets with `resetsAt`.',
        'Pass `format` for durations or bytes.',
      ],
      dont: [
        'Do not invent a limit to draw a meter; leave `limit` out.',
        'Do not colour a normal quota as success or accent.',
      ],
    },
    related: {
      items: [
        { id: 'progress-bar', reason: 'The meter inside each tile.' },
        { id: 'stat-card', reason: 'Single headline figures such as error rate or latency.' },
        { id: 'bar-chart', reason: 'Usage per day.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['progress-bar', 'inline-error-retry', 'skeleton'],
    },
    testing: {
      unit: [
        'Named region with one meter per limited item and value text; axe clean.',
        'Levels with text: normal percentage, near the limit, over the limit, unlimited.',
        'Polite live announcement of near and over-limit items.',
        'Reset date, compact numbers, custom formatter and `warningAt`.',
        '`usageLevel` boundaries; loading and error with retry; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
