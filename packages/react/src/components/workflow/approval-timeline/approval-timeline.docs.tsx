import { ApprovalTimeline } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-03-10T12:00:00Z';

export default {
  id: 'approval-timeline',
  title: 'ApprovalTimeline',
  summary:
    'The history and outlook of one approval: submission, each step’s decision, who decides now and what comes next.',
  sections: {
    purpose: {
      summary:
        'Shows where a request stands in its approval chain, with the decider, time and comment of every decided step. Needed by: People (expense claims, leave requests, timesheets).',
      useWhen: [
        'A request detail page or side panel must show who approved it, who is deciding now and what is left.',
        'The product has an approval engine that returns ordered steps with statuses.',
      ],
      avoidWhen: [
        'You are editing the steps a chain will use: use ApprovalChainEditor.',
        'You need a list of requests waiting on the viewer: use ApprovalQueue.',
        'The history is a general activity log rather than an approval: use an activity feed.',
      ],
    },
    designPrinciples: {
      items: [
        'Status is never colour alone: each step has a marker glyph and visible status text.',
        'Decided steps name the person and the time; undecided steps name who can decide.',
        'Skipped and cancelled steps stay in the list with a dashed marker and the reason, so the chain is never a mystery.',
        'Read-only and data-driven: the product maps its engine’s statuses to the seven `ApprovalStepStatus` values.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`steps` is ordered; put a `submitted` step first when the submission time matters.',
        'Times use RelativeTime and switch to absolute dates after `absoluteAfter` (7 days by default). Pass `now` for identical server rendering.',
        'The ref is the root `<ol>` (or a `<div>` when there are no steps).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (People `ApprovalTimelineView`): `timeline.steps` → `steps`, each `{ kind }` → `label` (map the kind to text in the product), `status` APPROVED / REJECTED / ACTIVE / WAITING / SKIPPED → `approved` / `rejected` / `pending` / `waiting` / `skipped`, `decidedBy` → `actor`, `decidedAt` → `at`, `skipReason` → `note`. `submittedAt` becomes a first step with `status: "submitted"`. `timeline = null` → `steps={[]}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showAvatars',
          values: ['true', 'false'],
          description: 'Show a small avatar next to the person who decided. Default true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Submitted / approved',
          attribute: 'data-status="submitted" | "approved"',
          description: 'Filled success marker with a check; the rail below turns success.',
        },
        {
          name: 'Rejected',
          attribute: 'data-status="rejected"',
          description: 'Filled danger marker with a cross; status text in danger.',
        },
        {
          name: 'Pending',
          attribute: 'data-status="pending", aria-current="step"',
          description:
            'The step waiting for a decision now: warning marker and the people who can decide.',
        },
        {
          name: 'Waiting',
          attribute: 'data-status="waiting"',
          description: 'A later step that has not started; muted label.',
        },
        {
          name: 'Skipped / cancelled',
          attribute: 'data-status="skipped" | "cancelled"',
          description: 'Dashed marker and the reason in `note`.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No steps: shows `messages.empty`.',
        },
      ],
    },
    interaction: {
      items: ['Not interactive: the timeline only displays the steps.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: nothing receives focus.'],
    },
    accessibility: {
      role: 'list (ol)',
      name: '`messages.label`, "Approval timeline" by default.',
      items: [
        'Each step is a list item whose text reads label, status, person and time in order.',
        'The step being decided has `aria-current="step"`.',
        'Markers are decorative (`aria-hidden`); the visible status text carries the meaning.',
        'Times are `<time datetime>` elements.',
      ],
    },
    responsive: {
      items: [
        'Single column; labels, names and comments wrap, so it works at 320px.',
        'Status text wraps under the label when there is no room beside it.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: success, danger and warning solids for markers, `--mn-color-bg-muted` for comments.',
        'Forced colours draw decided markers in `Highlight` and the rail in `CanvasText`.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Waiting on a step',
          stack: true,
          render: () => (
            <ApprovalTimeline
              now={NOW}
              steps={[
                {
                  id: 's',
                  label: 'Request',
                  status: 'submitted',
                  actor: { name: 'Alex Morgan' },
                  at: '2026-03-09T09:00:00Z',
                },
                {
                  id: '1',
                  label: 'Reporting manager',
                  status: 'approved',
                  actor: { name: 'Sam Lee' },
                  at: '2026-03-10T08:30:00Z',
                  comment: 'Matches the receipts.',
                },
                {
                  id: '2',
                  label: 'Department head',
                  status: 'skipped',
                  note: 'Same person as the requester.',
                },
                {
                  id: '3',
                  label: 'Finance check',
                  status: 'pending',
                  approvers: [{ name: 'Jo Park' }, { name: 'Ana Ruiz' }],
                },
                { id: '4', label: 'Final sign-off', status: 'waiting' },
              ]}
            />
          ),
        },
        {
          title: 'Rejected',
          stack: true,
          render: () => (
            <ApprovalTimeline
              now={NOW}
              steps={[
                {
                  id: 's',
                  label: 'Request',
                  status: 'submitted',
                  actor: { name: 'Alex Morgan' },
                  at: '2026-03-10T07:00:00Z',
                },
                {
                  id: '1',
                  label: 'Reporting manager',
                  status: 'rejected',
                  actor: { name: 'Sam Lee' },
                  at: '2026-03-10T11:00:00Z',
                  comment: 'Please attach the hotel invoice.',
                },
              ]}
            />
          ),
        },
        {
          title: 'Not submitted',
          render: () => <ApprovalTimeline steps={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Show every step of the chain, including skipped ones, with the reason.',
        'Include the decider’s comment, especially on a rejection.',
        'Pass `now` from the server when you render on the server.',
      ],
      dont: [
        'Do not hide waiting steps: people want to know what is left.',
        'Do not put approve or reject buttons inside the timeline; place them next to it.',
      ],
    },
    related: {
      items: [
        { id: 'approval-chain-editor', reason: 'Define the steps a chain uses.' },
        { id: 'approval-queue', reason: 'Requests waiting on the viewer, with decide actions.' },
        { id: 'stepper', reason: 'Steps of a flow the viewer moves through themselves.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'relative-time'],
    },
    testing: {
      unit: [
        'Every step renders with its status text, decider, relative time, comment, note and approvers; axe passes.',
        'The pending step has `aria-current="step"`.',
        'Empty steps show the empty message.',
        'Messages translate; avatars can be hidden; the ref and attributes are forwarded.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
