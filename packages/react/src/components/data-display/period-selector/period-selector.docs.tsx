import { useState } from 'react';
import { PeriodSelector, type PeriodValue } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ControlledDemo() {
  const [value, setValue] = useState<PeriodValue>({ preset: '30d' });
  const [range, setRange] = useState<string>('2026-10-09 – 2026-11-07');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <PeriodSelector
        today="2026-11-07"
        value={value}
        onValueChange={(next, dates) => {
          setValue(next);
          setRange(dates ? `${dates.start} – ${dates.end}` : 'choose dates');
        }}
        presets={['7d', '30d', '90d', 'this-month', 'last-month', 'custom']}
        maxDays={366}
      />
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>Query: {range}</p>
    </div>
  );
}

export default {
  id: 'period-selector',
  title: 'PeriodSelector',
  summary: 'Picks the period a dashboard or report covers: presets or a custom date range.',
  sections: {
    purpose: {
      summary:
        'Lets people choose what period the figures cover (today, the last 7 or 30 days, this quarter, a custom range) and gives the app the resolved start and end dates. Custom ranges use DateRangePicker. Needed by: Tickets, Dots, Pulse, GTM, Atlas, People, Developers.',
      useWhen: [
        'A dashboard, report or chart can be scoped to a period.',
        'You want consistent preset names and date maths across apps.',
      ],
      avoidWhen: [
        'A single date: use DatePicker.',
        'Any range with no presets: use DateRangePicker directly.',
      ],
    },
    designPrinciples: {
      items: [
        'Presets are rolling and include today: "Last 7 days" is the seven days ending today.',
        'The app receives dates, not labels, so every app computes periods the same way.',
        'Switching to Custom starts from the dates already shown, so nothing jumps.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`resolvePeriod(value, { today, weekStartsOn })` returns `{ start, end }` (both ends included) for any value; use it on the server to run the query.',
        '`formatPeriodRange(range, locale)` writes a range for display.',
        'The root is a `<div role="group">`; native attributes pass through and the ref is forwarded.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Tickets `PRESETS` (`7d`, `30d`, `90d`, `this_month`, `last_month`, `custom` with two date inputs and a 366-day check in `reports/ReportsPage.tsx`) → `presets={["7d","30d","90d","this-month","last-month","custom"]}` with `maxDays={366}`, and `presetRange()` → `resolvePeriod()`. Dots `PERIOD_OPTIONS` (30/90/180/365 days in a native select, `lib/hooks/useDashboardOverview.ts`) → `variant="select"` with `presets={["30d","90d","180d","365d"]}`. Atlas report `from`/`to` date inputs → `presets` including `custom`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['segmented', 'select'],
          description: '`segmented` (default) shows every preset; `select` is a compact dropdown.',
        },
        { prop: 'size', values: ['sm', 'md'], description: 'Control height. Default `sm`.' },
        {
          prop: 'presets',
          values: [
            'today',
            'yesterday',
            '7d',
            '14d',
            '30d',
            '90d',
            '180d',
            '365d',
            'this-week',
            'this-month',
            'last-month',
            'this-quarter',
            'last-quarter',
            'this-year',
            'custom',
          ],
          description: 'The presets offered, in order.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Preset',
          attribute: 'data-preset',
          description:
            'The selected preset; with `showRange` its dates are shown after the control.',
        },
        {
          name: 'Custom',
          attribute: 'data-preset="custom"',
          description: 'A DateRangePicker follows the presets.',
        },
        { name: 'Disabled', description: 'Every control is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Choosing a preset calls `onValueChange(value, range)` at once.',
        'Choosing Custom keeps the current dates and shows the range picker; a completed range calls `onValueChange` again.',
        'Custom ranges cannot end after `max` (default today) and respect `min` and `maxDays`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Arrow keys', action: 'Move between presets and select them (segmented).' },
        {
          keys: 'Enter / Space, arrows, Escape',
          action: 'Open, move through and close the list (select).',
        },
        { keys: 'Tab', action: 'Moves from the presets to the custom date fields.' },
      ],
    },
    accessibility: {
      role: 'group containing a radiogroup (segmented) or a combobox (select)',
      name: '`aria-label`, default "Period"',
      items: [
        'Presets are radios with their visible names.',
        'The custom range is a group named "Custom period" with named start and end fields.',
        'With `showRange` the select describes itself with the resolved dates.',
      ],
    },
    responsive: {
      items: [
        'The segmented control scrolls sideways inside itself when presets do not fit; the page never scrolls at 390px.',
        'Use `variant="select"` in toolbars and on narrow layouts with many presets.',
      ],
    },
    theme: {
      items: [
        'Uses SegmentedControl, Select and DateRangePicker tokens; the range text uses `--mn-color-text-muted`.',
        'In forced colours the controls follow system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Report periods with a custom range',
          stack: true,
          render: () => <ControlledDemo />,
        },
        {
          title: 'Compact select with the dates shown',
          render: () => (
            <PeriodSelector
              today="2026-11-07"
              variant="select"
              presets={['30d', '90d', '180d', '365d']}
              showRange
            />
          ),
        },
        {
          title: 'Dashboard defaults',
          render: () => <PeriodSelector today="2026-11-07" />,
        },
      ],
    },
    doDont: {
      do: [
        'Run queries from the `range` passed to `onValueChange` (or `resolvePeriod` on the server).',
        'Pass `today` from the workspace time zone when it differs from the device’s.',
        'Keep the period in the URL so a view can be shared.',
      ],
      dont: [
        'Do not offer more than six presets in the segmented variant.',
        'Do not compute "last 30 days" separately in each app.',
      ],
    },
    related: {
      items: [
        { id: 'date-range-picker', reason: 'Any range, with its own presets.' },
        { id: 'segmented-control', reason: 'A generic single choice.' },
        { id: 'report-viewer', reason: 'Put the selector in its filters.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['date-range-picker', 'segmented-control', 'select'],
    },
    testing: {
      unit: [
        'Named group of preset radios; props and ref forwarded.',
        'Preset changes report the value and resolved dates, by pointer and arrow keys.',
        'Custom shows a named range picker seeded with the previous dates.',
        'Given presets only, resolved range text, select variant with a named combobox.',
        'resolvePeriod for every calendar preset and custom; messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
