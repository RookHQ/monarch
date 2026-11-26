import { Badge } from '@rookcom/monarch-react';
import { Leaderboard } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'leaderboard',
  title: 'Leaderboard',
  summary: 'A ranked list with avatars, values, changes and bars relative to the leader.',
  sections: {
    purpose: {
      summary:
        'Ranks people, teams or things by one figure on a dashboard (tickets solved per agent, open load per person, best campaign steps by reply rate), with the change since last period and a bar relative to the leader. Needed by: Tickets, GTM, Dots, Recruit, Pulse.',
      useWhen: [
        'A dashboard compares a few entries by one measure.',
        'The viewer wants to see where they stand ("you").',
      ],
      avoidWhen: [
        'Many columns per entry: use DataTable.',
        'A share of a whole: use BarList or a DonutChart.',
        'Figures without ranking: use KPIRow.',
      ],
    },
    designPrinciples: {
      items: [
        'Ties are honest: equal values share a rank (1, 1, 3).',
        'The bar is decoration; the number and its unit carry the data.',
        'The viewer’s row is marked in words ("(you)") and `aria-current`, not only highlighted.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`rankEntries(entries, sort)` gives the same ranking for your own tables.',
        'Native `<div>` attributes pass through and the ref is forwarded.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Tickets "Team load" (`components/home/HomeDashboard.tsx` `TeamLoad`) → `entries` with `name`, `avatarSrc`, `value: open`, `badge` for "N breached", `current: isMe`, `valueLabel="open tickets"`, and `total={agentCount}` for "+N more agents". GTM "What is getting replies" (`components/dashboard/OutboundHome.tsx` `TopSteps`) → `entries` with `value` the reply rate, `format="percent"`, `secondary` the campaign and step, `showAvatars={false}`. People `BarList` (`reports/viz.tsx`) → `showAvatars={false}` with `limit`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sort',
          values: ['none', 'desc', 'asc'],
          description: 'Keep the given order, or rank largest or smallest first.',
        },
        {
          prop: 'format',
          values: ['number', 'percent'],
          description: 'How values without `display` are written.',
        },
        {
          prop: 'showAvatars / showBars / highlightTop',
          values: ['true', 'false'],
          description: 'Avatars, relative bars, and a filled marker for the top three.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', attribute: 'data-rank on each row', description: 'Ranked rows.' },
        {
          name: 'Current',
          attribute: 'data-current, aria-current',
          description: 'The viewer’s row, tinted and tagged "(you)".',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'Placeholder rows named "Loading ranking".',
        },
        {
          name: 'Empty / Error',
          attribute: 'data-state',
          description: '"Nothing to rank yet", or the error with Try again.',
        },
      ],
    },
    interaction: {
      items: [
        'Names with `href` are links; "N more" links to `moreHref`.',
        'Try again calls `onRetry`.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab / Shift+Tab', action: 'Moves through name links and the more link.' }],
      notes: ['Rows are not focusable on their own; only links are.'],
    },
    accessibility: {
      role: 'list (ordered)',
      name: '`label`, e.g. "Tickets solved by agent"',
      items: [
        'Each row reads "Rank 1: Name, 42 open tickets, up 12% better".',
        'Avatars are decorative because the name is beside them.',
        'Bars are hidden from assistive technology.',
      ],
    },
    responsive: {
      items: [
        'Names and secondary lines wrap; the value column keeps its width, so nothing scrolls at 390px.',
        'Links are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Bars use `--mn-color-data-1` on `--mn-color-bg-muted`; the current row `--mn-color-accent-subtle` with an accent border.',
        'In forced colours bars are CanvasText on Canvas and the current row is outlined in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Agents by tickets solved',
          stack: true,
          render: () => (
            <Leaderboard
              style={{ maxWidth: '28rem' }}
              label="Tickets solved this month"
              valueLabel="tickets solved"
              sort="desc"
              highlightTop
              total={9}
              moreHref="#report"
              limit={4}
              entries={[
                { id: 'a', name: 'Ada Moss', value: 64, change: { value: 0.12 } },
                {
                  id: 'b',
                  name: 'Bea Lund',
                  value: 51,
                  change: { value: -0.04 },
                  badge: (
                    <Badge tone="danger" size="sm">
                      2 breached
                    </Badge>
                  ),
                },
                { id: 'c', name: 'Cal Ortiz', value: 51, current: true },
                { id: 'd', name: 'Dee Park', value: 38, secondary: 'Billing team' },
              ]}
            />
          ),
        },
        {
          title: 'Best steps by reply rate, without avatars',
          stack: true,
          render: () => (
            <Leaderboard
              style={{ maxWidth: '28rem' }}
              label="Steps by reply rate"
              format="percent"
              showAvatars={false}
              entries={[
                { id: 's1', name: 'Follow-up 2', secondary: 'Spring launch · Step 3', value: 0.18 },
                { id: 's2', name: 'Intro', secondary: 'Partners · Step 1', value: 0.11 },
                { id: 's3', name: 'Check-in', secondary: 'Renewals · Step 2', value: 0.07 },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say what the value counts with `valueLabel`.',
        'Show the top five to ten and link to the full report.',
        'Use `sort="asc"` for times, where smaller is better.',
      ],
      dont: [
        'Do not rank people on figures they cannot influence.',
        'Do not hide ties by numbering 1, 2, 3 for equal values.',
      ],
    },
    related: {
      items: [
        { id: 'bar-list', reason: 'Labelled bars without ranks or avatars.' },
        { id: 'data-table', reason: 'Many columns per entry.' },
        { id: 'widget', reason: 'Frame the leaderboard with a title and period.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'delta-indicator', 'empty-state', 'inline-error-retry', 'skeleton'],
    },
    testing: {
      unit: [
        'Named ordered list with ranks, spoken units and links; props and ref forwarded.',
        'Viewer tagged with aria-current.',
        'DeltaIndicator changes and decorative bars relative to the leader.',
        'Ascending, given-order and tied ranks.',
        'limit with a more link; percent and display values; loading, error and empty.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
