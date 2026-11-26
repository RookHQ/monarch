import { StatCard, StatCardGroup } from '@rookcom/monarch-react';
import { Sparkline } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'stat-card',
  title: 'StatCard',
  summary:
    'A KPI tile: label, formatted value, change versus a period, hint, help, sparkline and drill-down.',
  sections: {
    purpose: {
      summary:
        'A single key figure for a dashboard or overview, with how it changed, what it is compared with, and where to go for detail. StatCardGroup lays several out as a responsive KPI row.',
      useWhen: [
        'Headline metrics on a home, overview or reports page.',
        'A metric that links to the filtered list behind it ("7 overdue invoices").',
        'Metrics whose definition needs explaining ("How first response is calculated").',
      ],
      avoidWhen: [
        'A trend over time is the point: use a line or bar chart.',
        'Grouping several facts about one record: use DescriptionList.',
        'A share of a whole: use StackedProportionBar or a donut.',
      ],
    },
    designPrinciples: {
      items: [
        'The product decides what the number is; StatCard only formats it (`format`, Intl.NumberFormat, Money for currency) and never invents a change.',
        'Every state is honest: loading shows a placeholder, a missing value shows a dash read as "No data", a failure shows the error with an optional Try again.',
        'Direction is spoken, not only drawn: both the legacy `delta` and `change` (DeltaIndicator) announce the direction in words.',
        '`change` knows which direction is better (`goodDirection`), so falling costs are green.',
        'A linked card has one link, named by its label; the whole card is the click target.',
        'Additive upgrade: the 0.3 props (`delta`, `deltaDirection`, `deltaTone`, `caption`, `visual`) render exactly as before.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'All native `<div>` attributes and the ref go to the root `.mn-stat`.',
        '`StatCardGroup` is a grid `<div>`; with `label` it is a named `role="group"`.',
        '`change` takes DeltaIndicator props (from `@rookcom/monarch-react/charts`) without importing that entry.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'With `format="percent"` the value is a ratio: 0.12 is 12%. Without `format`, a number is shown as given (0.3 behaviour).',
        'If both `delta` and `change` are set, `change` wins.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description:
            '`sm` is compact for dense rows, `md` the default, `lg` a hero figure (`data-size`).',
        },
        {
          prop: 'variant',
          values: ['card', 'plain'],
          description:
            '`plain` drops the border, padding and surface for use inside another card (`data-variant`).',
        },
        {
          prop: 'tone',
          values: ['neutral', 'accent', 'success', 'warning', 'danger'],
          description:
            '`accent` tints the card; status tones colour the value and icon for thresholds (`data-tone`).',
        },
        {
          prop: 'format',
          values: ['number', 'percent', 'currency'],
          description:
            'Formats a numeric value with `locale`, `notation`, `formatOptions` and `currency`.',
        },
        {
          prop: 'semantics',
          values: ['text', 'description-list'],
          description:
            '`description-list` renders `<dl>` with the label as `<dt>` and value lines as `<dd>`.',
        },
        {
          prop: 'deltaDirection / deltaTone',
          values: ['up', 'down', 'flat'],
          description: 'The 0.3 pre-formatted delta, still supported.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description:
            'A placeholder replaces the value; the footer, hint and sparkline are hidden; `aria-busy="true"`.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: '`value` null or undefined: a dash read as `emptyLabel` ("No data").',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            '`error` replaces the value; with `onRetry` it is an InlineErrorRetry with Try again.',
        },
        {
          name: 'Linked',
          attribute: 'data-linked',
          description: '`href` makes the label a link stretched over the card.',
        },
        {
          name: 'With change',
          description:
            'A DeltaIndicator (or the 0.3 delta) and the comparison period in the footer.',
        },
      ],
    },
    interaction: {
      items: [
        'With `href`, clicking anywhere on the card follows the link; hover strengthens the border and underlines the label.',
        'With `help`, the info button opens a popover with the definition; it closes on Escape or outside click and returns focus to the button.',
        'Otherwise StatCard is read-only.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the label link (with `href`), then the help button (with `help`).',
        },
        { keys: 'Enter', action: 'Follows the link, or opens the help popover.' },
        { keys: 'Space', action: 'Opens the help popover.' },
        { keys: 'Escape', action: 'Closes the help popover and returns focus to its button.' },
      ],
      notes: ['A card without `href`, `help` or `onRetry` has no focusable parts.'],
    },
    accessibility: {
      role: 'none by default; `semantics="description-list"` gives a `<dl>` term and descriptions',
      name: 'The label; a linked card’s link is named by the label alone, not the whole card.',
      items: [
        'Change direction is spoken ("up 12% vs last month, better"); arrows are `aria-hidden`.',
        'Compact values ("1.3M") are read in full ("1,250,000").',
        'The help button is named "About {label}" (or `helpLabel`) and exposes `aria-expanded`.',
        'Icons, `visual` and `sparkline` are `aria-hidden`: never put the only copy of data there.',
        'Tone is colour; say what it means in `hint` ("Above the 5% target").',
        'Loading sets `aria-busy`; the placeholder is hidden from assistive technology.',
        'The linked card shows a focus ring around the whole card when its link has keyboard focus.',
      ],
    },
    responsive: {
      items: [
        'A card fills its grid cell; long labels wrap and the visual slot does not shrink.',
        'StatCardGroup fits as many cards as `minItemWidth` (12rem) allows, down to one column at 320px.',
        'With `columns`, it shows one column below 30rem, at most two up to 48rem, then `columns`.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text use semantic tokens; status tones use the `-strong` text tokens, which meet AA in light, dark and high-contrast themes.',
        '`accent` uses `--mn-color-accent-subtle` and `--mn-color-accent-border`.',
        'The sparkline slot is coloured `--mn-color-data-1` (series palette), never a status colour.',
      ],
    },
    examples: {
      items: [
        {
          title: 'KPI row with changes',
          stack: true,
          render: () => (
            <StatCardGroup label="Last 30 days" columns={3} style={{ width: '100%' }}>
              <StatCard
                label="Revenue"
                value={1_284_500}
                format="currency"
                currency="USD"
                notation="compact"
                locale="en-US"
                change={{ value: 0.124, locale: 'en-US' }}
                caption="vs previous 30 days"
              />
              <StatCard
                label="Cost per lead"
                value={41.2}
                format="currency"
                currency="USD"
                locale="en-US"
                change={{ current: 41.2, previous: 50.4, goodDirection: 'down', locale: 'en-US' }}
                caption="vs previous 30 days"
              />
              <StatCard
                label="Reply rate"
                value={0.081}
                format="percent"
                locale="en-US"
                change={{ value: 0, locale: 'en-US' }}
                caption="vs previous 30 days"
                hint="212 of 2,617 contacts"
              />
            </StatCardGroup>
          ),
        },
        {
          title: 'Help, unit, sparkline and drill-down',
          stack: true,
          render: () => (
            <StatCardGroup columns={2} style={{ width: '100%' }}>
              <StatCard
                label="First response"
                value={2.4}
                unit="h"
                format="number"
                locale="en-US"
                help="Median time from a ticket being opened to the first reply by an agent, in business hours."
                change={{ current: 2.4, previous: 3.1, goodDirection: 'down', locale: 'en-US' }}
                caption="vs last week"
                sparkline={
                  <Sparkline values={[3.4, 3.1, 3.3, 2.9, 2.8, 2.6, 2.4]} responsive height={28} />
                }
              />
              <StatCard
                label="Overdue invoices"
                value={7}
                href="#/components/stat-card"
                tone="danger"
                hint="3 more than last month"
              />
            </StatCardGroup>
          ),
        },
        {
          title: 'Sizes and plain variant',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1rem', width: '100%' }}>
              <StatCard
                size="lg"
                label="Uptime, 30 days"
                value={0.9996}
                format="percent"
                formatOptions={{ maximumFractionDigits: 2 }}
                locale="en-US"
                change={{
                  value: 0.02,
                  format: 'number',
                  unit: 'pt',
                  formatOptions: { maximumFractionDigits: 2 },
                  locale: 'en-US',
                }}
                caption="vs previous 30 days"
              />
              <StatCardGroup columns={3}>
                <StatCard size="sm" label="Open" value="42" />
                <StatCard size="sm" label="Pending" value="9" />
                <StatCard size="sm" label="Resolved" value="118" tone="success" />
              </StatCardGroup>
              <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                <StatCard
                  variant="plain"
                  semantics="description-list"
                  label="Members"
                  value="24"
                  hint="3 invited"
                />
                <StatCard variant="plain" semantics="description-list" label="Spaces" value="6" />
              </div>
            </div>
          ),
        },
        {
          title: 'Loading, empty and error',
          stack: true,
          render: () => (
            <StatCardGroup columns={3} style={{ width: '100%' }}>
              <StatCard label="Signups" value={null} loading />
              <StatCard label="NPS" value={null} hint="Needs at least 20 responses" />
              <StatCard
                label="Churn"
                value={null}
                error="Couldn't load churn."
                onRetry={() => undefined}
              />
            </StatCardGroup>
          ),
        },
        {
          title: '0.3 delta props',
          render: () => (
            <>
              <StatCard
                label="Open tickets"
                value="42"
                delta="+5"
                deltaDirection="up"
                deltaTone="danger"
                caption="vs last week"
                style={{ width: '16rem' }}
              />
              <StatCard
                label="Resolved"
                value="118"
                delta="+9"
                deltaDirection="up"
                caption="vs last week"
                style={{ width: '16rem' }}
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass raw numbers with `format` and the viewer’s `locale`, or a value the product has already formatted.',
        'Set `goodDirection: "down"` in `change` for costs, latency and churn.',
        'Always pair a change with its comparison period (`caption`).',
        'Pass `null` when there is no value yet, and `error` when it failed to load.',
      ],
      dont: [
        'Do not show a placeholder number (0, 0 MB) when the data is not available.',
        'Do not put the only copy of information in `visual` or `sparkline`; they are hidden from assistive technology.',
        'Do not use `tone` as the only way to show a threshold was crossed.',
        'Do not nest other links or buttons in a linked card apart from `help`.',
      ],
    },
    related: {
      items: [
        { id: 'delta-indicator', reason: 'The change on its own, in tables and rows.' },
        { id: 'money', reason: 'How currency values are formatted.' },
        {
          id: 'sparkline',
          reason: 'The trend line for the `sparkline` slot (`@rookcom/monarch-react/charts`).',
        },
        { id: 'description-list', reason: 'Several facts about one record.' },
        { id: 'card', reason: 'General content surface.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'money',
        'delta-indicator',
        'inline-error-retry',
        'skeleton',
        'visually-hidden',
        'popover',
      ],
    },
    testing: {
      unit: [
        'Reads the 0.3 delta direction through visually hidden text and applies `deltaTone`; legacy markup unchanged.',
        'Formats number, percent, currency (Money) and compact values with the full value spoken.',
        'Unit and hint; polarity-aware `change` with the caption as its comparison.',
        'Help popover opens with Enter, closes with Escape and returns focus; custom help label.',
        'Drill-down link named by the label, reachable with Tab, custom link component.',
        'Loading (aria-busy), empty ("No data") and error with Try again.',
        'description-list semantics, tone/size/variant attributes, ref forwarding.',
        'StatCardGroup group role, column variables and minimum width.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/stat-card',
        'Cross-browser axe: /components/stat-card',
      ],
    },
  },
} satisfies ComponentDocs;
