import { useState } from 'react';
import { InsightStrip, type InsightStripItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sample: InsightStripItem[] = [
  {
    id: 'expiring',
    severity: 'critical',
    origin: 'rule',
    title: '3 items expire within 30 days',
    why: 'Their end dates fall before 7 December and none has a renewal recorded.',
    evidence: (
      <ul style={{ margin: 0, paddingInlineStart: 'var(--mn-space-4)' }}>
        <li>Item A: 12 November</li>
        <li>Item B: 20 November</li>
        <li>Item C: 4 December</li>
      </ul>
    ),
    action: { label: 'Review renewals', onClick: () => {} },
  },
  {
    id: 'usage',
    severity: 'warning',
    origin: 'ai',
    title: 'Weekly use is falling',
    why: 'Active people fell from 48 to 29 over the last 30 days.',
  },
  {
    id: 'deadline',
    severity: 'info',
    origin: 'rule',
    title: 'Next deadline in 12 days',
    why: 'A response is due on 19 November.',
  },
  {
    id: 'target',
    severity: 'positive',
    origin: 'rule',
    title: 'Quarterly target reached',
    why: 'Bookings passed the target set for this quarter.',
  },
];

function DismissDemo() {
  const [items, setItems] = useState(sample);
  return (
    <InsightStrip
      title="Needs attention"
      insights={items}
      max={3}
      onDismiss={(id) => setItems((list) => list.filter((i) => i.id !== id))}
    />
  );
}

export default {
  id: 'insight-strip',
  title: 'InsightStrip',
  summary:
    'A row of rule-based and AI insights for a dashboard, sorted by severity, each labelled with its origin.',
  sections: {
    purpose: {
      summary:
        'Puts the few things worth acting on at the top of a dashboard (expiring items, falling usage, deadlines), each with why it was raised and one action. Built from AiInsightCallout so every insight says honestly whether a fixed rule or a model produced it. Needed by: Atlas, Intellect, Pulse.',
      useWhen: [
        'A dashboard computes "next actions" or insights and should surface them before the figures.',
        'Rule-based and AI insights are shown together and people need to tell them apart.',
      ],
      avoidWhen: [
        'A message about the page or an action: use Alert or a toast.',
        'One insight on a record page: use AiInsightCallout directly.',
        'A to-do list: use TodayPanel.',
      ],
    },
    designPrinciples: {
      items: [
        'Honest origin: "Rule-based" in neutral colours unless a model really produced the insight.',
        'Most severe first, and only a few at once; the rest behind "Show N more".',
        'Every insight explains itself; evidence stays collapsed until asked for.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`sortInsights(list)` gives the same severity order for your own lists.',
        'With `title` the root is a `<section>` named by it; otherwise a `<div>`. Native attributes pass through and the ref is forwarded.',
        '`calloutMessages` is passed to every AiInsightCallout for translation.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Atlas `NextActions` (`features/overview/components/OverviewView.tsx`) maps `severity` critical/high → `critical`, medium → `warning`, low → `info`; `title` → `title`; `message` → `why`; `reasons` → `evidence` (a list); the "Take action" link → `action`; `actions.slice(0, 6)` → `max={6}`; set `origin="rule"`. Intellect `Insights` (`components/intellect/dashboard/widgets.tsx`) maps `tone` warning/info/success → `warning`/`info`/`positive`; `detail` → `why`; the row link → `action`; set `origin="rule"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['grid', 'stack', 'scroll'],
          description:
            '`grid` (default) wraps into one to three columns by width; `stack` one column; `scroll` one sideways row. Exposed as `data-layout`.',
        },
        {
          prop: 'severity (per insight)',
          values: ['critical', 'warning', 'info', 'positive'],
          description: 'Icon, hidden prefix, tone and sort order.',
        },
        {
          prop: 'origin (per insight)',
          values: ['rule', 'ai'],
          description: '"Rule-based" or "AI insight", always visible.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'Insights in severity order.' },
        {
          name: 'Collapsed',
          attribute: 'aria-expanded="false" on the toggle',
          description: 'Only `max` insights shown, with "Show N more".',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'Three placeholder cards named "Loading insights".',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'The error, with Try again when `onRetry` is set.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description:
            '"Nothing needs attention right now.", a custom node, or nothing with `empty={null}`.',
        },
      ],
    },
    interaction: {
      items: [
        'Show N more / Show fewer toggles the hidden insights.',
        'Dismiss calls `onDismiss(id)`; after the app removes it, focus moves to the next insight’s dismiss button, or to the heading when none is left.',
        'Each action button calls its `onClick`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through each insight’s evidence, action and dismiss, then the toggle.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
    },
    accessibility: {
      role: 'region (with `title`), containing a list',
      name: '`title`',
      items: [
        'Insights are list items; each callout is a group named "Critical: {title}" and described by its reason.',
        'Callout titles are headings one level below the strip’s title.',
        'The toggle has `aria-expanded` and `aria-controls`.',
        'Focus never lands on `body` after a dismiss.',
      ],
    },
    responsive: {
      items: [
        'The grid layout drops to one column at 390px; `scroll` shows most of one card with the next peeking in.',
        'Callout controls are at least 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Callouts use the status tokens for severity and `--mn-color-ai-*` only for the AI chip.',
        'In forced colours callouts keep their borders and icons in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Dashboard insights with limit and dismiss',
          stack: true,
          render: () => <DismissDemo />,
        },
        {
          title: 'One sideways row',
          stack: true,
          render: () => <InsightStrip layout="scroll" insights={sample} />,
        },
        {
          title: 'Nothing to report',
          stack: true,
          render: () => <InsightStrip title="Needs attention" insights={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Set `origin` from where each insight came from.',
        'Limit what is shown with `max`; three to six is plenty.',
        'Give each insight one clear action.',
      ],
      dont: [
        'Do not label rule output as AI.',
        'Do not use the strip for system status or errors.',
        'Do not show the same insight on every page; show it where it can be acted on.',
      ],
    },
    related: {
      items: [
        { id: 'ai-insight-callout', reason: 'A single insight; the strip is built from it.' },
        { id: 'today-panel', reason: 'Tasks and events for today rather than insights.' },
        { id: 'kpi-row', reason: 'Headline figures, usually right below the strip.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-insight-callout', 'button', 'empty-state', 'inline-error-retry', 'skeleton'],
    },
    testing: {
      unit: [
        'Named region with a list of callouts sorted by severity and nested heading levels; props and ref forwarded.',
        'Rule-based and AI origins labelled honestly; given order kept with sort={false}.',
        'Show more toggle with aria-expanded.',
        'Focus moves to the next dismiss button, then the heading, after dismissals.',
        'Loading, error with retry and empty={null}.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
