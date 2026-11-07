import { SlaIndicator, SlaTargetList } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A fixed reference time so the examples read the same on every visit. */
const NOW = Date.UTC(2026, 0, 12, 10, 0);
const MIN = 60_000;

export default {
  id: 'sla-indicator',
  title: 'SlaIndicator',
  summary:
    'A service-level target at a glance: on track, at risk, breached, paused or met, with a live countdown.',
  sections: {
    purpose: {
      summary:
        'Shows whether a response or resolution target is on track, at risk, breached, paused or met, with the time left or overdue, and keeps the countdown honest between refetches. `SlaTargetList` lists every target of one item. Needed by: Tickets (inbox rows, ticket header, SLA side panel, home dashboard).',
      useWhen: [
        'An item has a deadline promised to someone else: first response, next response, resolution, approval within a working day.',
        'A queue needs the most urgent items to stand out: breached is deliberately the loudest chip in a list.',
        'A detail panel lists several targets for one item with their states.',
      ],
      avoidWhen: [
        'The date is a plain due date with no promise attached: use RelativeTime or a date.',
        'The status is a workflow status (open, pending, solved): use StatusBadge.',
        'You need the history of SLA events: use ActivityTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'State comes from the server; the component only re-derives the countdown, and never keeps showing "on track" after the due time has passed.',
        'Colour is never the only signal: every state has an icon and text, and the state name is always in the accessible text.',
        'Breached is solid danger on purpose, at risk is warning, paused and none are quiet.',
        'Hydration-safe: pass `now` from the server for identical markup; without it the clock is read after mount.',
        'Product words stay in the product: target names ("First response") and translations come in through props.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`SlaIndicator` renders a `<span>`; the ref and other span attributes go to it. It sets `title` to the absolute due time unless you pass your own.',
        '`SlaTargetList` renders a `<ul>` of targets, each with an `SlaIndicator` in the `text` variant; finished targets show when they completed.',
        '`resolveSlaState(state, dueAt, now, warnBefore?)` and `formatSlaDuration(ms)` are exported for sorting, filtering and summaries that must agree with the chip.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Tickets `SlaBadge` in `components/tickets/chips.tsx`): `sla.state` → `state` (`ok` → `on-track`, `at_risk` → `at-risk`); `sla.nextDueAt` → `dueAt`; `METRIC_LABEL[sla.nextMetric]` → `target`; `sla.breachedBefore` → `breachedBefore`; `showNone` is unchanged; the local `useNow()` 30-second tick is `live` (default `true`); the tooltip text is now the `title` and visually hidden text.',
        'Migrating the Tickets `SlaPanel` deadline list: each `{ metric, dueAt, state, completedAt }` becomes an `SlaTarget` `{ id: metric, label: METRIC_LABEL[metric], state, dueAt, completedAt }`; `running` → `on-track`. The policy name, loading and error states and the SLA history stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['chip', 'text'],
          description:
            '`chip` (default): a compact pill for list rows and headers; breached is solid danger. `text`: icon and text only, for panels and tables.',
        },
        {
          prop: 'showNone',
          values: ['false', 'true'],
          description: 'How `none` reads: a quiet dash (default) or "No SLA".',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'On track',
          attribute: 'data-state="on-track"',
          description: 'Running with time to spare: muted text with a clock and "2h 15m left".',
        },
        {
          name: 'At risk',
          attribute: 'data-state="at-risk"',
          description: 'Running and close to due (server state or `warnBefore`): warning colours.',
        },
        {
          name: 'Breached',
          attribute: 'data-state="breached"',
          description: 'Past due: "Overdue 40m" with an error icon; the chip is solid danger.',
        },
        {
          name: 'Paused',
          attribute: 'data-state="paused"',
          description: 'The clock is stopped: "Paused" with a pause icon, no countdown.',
        },
        {
          name: 'Met',
          attribute: 'data-state="met"',
          description: 'Achieved in time: success colour and a check.',
        },
        {
          name: 'None',
          attribute: 'data-state="none"',
          description: 'No target applies: a dash, or "No SLA" with `showNone`.',
        },
      ],
    },
    interaction: {
      items: [
        'Not interactive: the indicator is text. Hovering shows the absolute due time (`title`).',
        'With `live` (default), a running target re-derives its countdown every 30 seconds (or your interval) and turns breached when time runs out. It stops ticking when paused, met or none, or when `now` is given.',
        '`onStateChange` fires when the derived state changes after mount, so a product can refetch when a countdown runs out.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: there is nothing to focus.'],
    },
    accessibility: {
      role: 'none (text); status for the optional announcer',
      name: 'Its text: "First response: 2h 15m left (On track), due 12 Jan 2026, 10:00".',
      items: [
        'The icon is decorative; the state name is always in the text, visibly or visually hidden.',
        'The target name and absolute due time are visually hidden text, so screen readers get the full sentence without a tooltip.',
        '`announceChanges` adds a polite live region that speaks only state changes, never every tick. Use it on one prominent indicator, not on every row.',
        'In forced-colours mode the chip keeps a border and breached is drawn in inverted system colours.',
      ],
    },
    responsive: {
      items: [
        'The chip does not wrap; long labels are cut with an ellipsis inside the space the row gives it.',
        'Target list rows wrap the state under the label at 390px.',
      ],
    },
    theme: {
      items: [
        'Colours are semantic tokens only: warning subtle/strong/border for at risk, danger solid/on-solid for a breached chip, success strong for met, text muted and subtle for running and none.',
        'Light, dark and high-contrast themes apply automatically.',
        'No motion: nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every state',
          render: () => (
            <>
              <SlaIndicator state="on-track" dueAt={NOW + 135 * MIN} now={NOW} />
              <SlaIndicator state="at-risk" dueAt={NOW + 20 * MIN} now={NOW} />
              <SlaIndicator state="breached" dueAt={NOW - 40 * MIN} now={NOW} />
              <SlaIndicator state="paused" />
              <SlaIndicator state="met" />
              <SlaIndicator state="none" showNone />
            </>
          ),
        },
        {
          title: 'At risk from warnBefore, text variant',
          render: () => (
            <SlaIndicator
              state="on-track"
              dueAt={NOW + 25 * MIN}
              now={NOW}
              warnBefore={60 * MIN}
              target="Resolution"
              variant="text"
            />
          ),
        },
        {
          title: 'Target list for a side panel',
          stack: true,
          render: () => (
            <SlaTargetList
              aria-label="Service-level targets"
              now={NOW}
              warnBefore={30 * MIN}
              targets={[
                { id: 'first', label: 'First response', state: 'met', completedAt: NOW - 50 * MIN },
                { id: 'next', label: 'Next response', state: 'on-track', dueAt: NOW + 15 * MIN },
                {
                  id: 'resolution',
                  label: 'Resolution',
                  state: 'on-track',
                  dueAt: NOW + 26 * 60 * MIN,
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the server state and due time; let the indicator keep the countdown current.',
        'Name the target with `target` when an item has more than one.',
        'Pass `now` when rendering on the server for markup that matches the client.',
      ],
      dont: [
        'Do not use colour alone to show urgency in your own row styling; the indicator already carries the text.',
        'Do not turn on `announceChanges` for every row of a list.',
        'Do not compute breached separately for sorting; use `resolveSlaState` so lists and chips agree.',
      ],
    },
    related: {
      items: [
        { id: 'relative-time', reason: 'A plain "3 hours ago" timestamp with no target attached.' },
        { id: 'status-badge', reason: 'Workflow status of the item itself.' },
        { id: 'escalation-policy-editor', reason: 'Editing the rules that create SLA targets.' },
        { id: 'inbox-row', reason: 'The queue row that usually carries this chip.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: ['Uses `Intl.DateTimeFormat` for the absolute due time.'],
    },
    testing: {
      unit: [
        'Duration formatting and state derivation (breached when overdue, at risk inside `warnBefore`, finished states unchanged).',
        'Visible and hidden text for every state, target and due time; axe passes.',
        'Live ticking with fake timers, `onStateChange`, and a polite announcement of the state change.',
        'Translated messages; the target list (completed, running, paused, empty); server rendering with an injected clock.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
