import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { ApprovalQueue, type ApprovalQueueItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-03-10T12:00:00Z';

const ITEMS: ApprovalQueueItem[] = [
  {
    id: '1',
    requester: { name: 'Alex Morgan', detail: 'Design' },
    title: 'Expense claim',
    summary: '3 receipts, travel',
    submittedAt: '2026-03-08T09:00:00Z',
    overdue: true,
  },
  {
    id: '2',
    requester: { name: 'Sam Lee', detail: 'Engineering' },
    title: 'Leave, 2 days',
    summary: '14 to 15 April',
    submittedAt: '2026-03-10T08:15:00Z',
  },
  {
    id: '3',
    requester: { name: 'Jo Park', detail: 'Operations' },
    title: 'Timesheet',
    summary: 'Week of 2 March, 41 h',
    submittedAt: '2026-03-09T17:40:00Z',
    badges: (
      <Badge size="sm" tone="warning">
        Resubmitted
      </Badge>
    ),
  },
];

function QueueDemo() {
  const [items, setItems] = useState(ITEMS);
  const drop = (ids: string[]) => setItems((cur) => cur.filter((i) => !ids.includes(i.id)));
  return (
    <ApprovalQueue
      style={{ width: '100%', maxWidth: 720 }}
      now={NOW}
      items={items}
      searchable
      onApprove={async (item: ApprovalQueueItem) => {
        await new Promise((r) => setTimeout(r, 600));
        drop([item.id]);
      }}
      onReject={(item: ApprovalQueueItem) => drop([item.id])}
      onApproveMany={(list: ApprovalQueueItem[]) => drop(list.map((i) => i.id))}
      onReview={() => {}}
    />
  );
}

export default {
  id: 'approval-queue',
  title: 'ApprovalQueue',
  summary:
    'Requests waiting on the viewer, with approve, send back with a reason, review, search and bulk approval.',
  sections: {
    purpose: {
      summary:
        'Gives approvers one place to decide requests: who asked, what, when, whether it is overdue, and the actions to approve or send back. Needed by: People (manager dashboard, timesheet approvals, the approvals inbox), Links (admin approvals).',
      useWhen: [
        'A manager or admin has a list of requests that need their decision.',
        'A dashboard card shows the first few waiting requests with a "See all" link (`limit` + `footer`).',
      ],
      avoidWhen: [
        'You are showing the steps of one request: use ApprovalTimeline.',
        'Items need many columns, sorting and paging: use DataTable with row actions.',
      ],
    },
    designPrinciples: {
      items: [
        'Approve is one click; sending back always confirms and, by default, asks for a reason the requester will see.',
        'Every action names the request it acts on ("Approve request from Sam Lee").',
        'The product performs the decision and removes the request; the queue shows pending, errors and announcements around it.',
        'Bulk approval is opt-in (`onApproveMany`) and always confirms with the count.',
        'Requests the viewer can see but not decide (`canDecide: false`) keep their row without decide actions.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onApprove`, `onReject` and `onApproveMany` may return promises: approve shows a spinner on the row; send back and bulk approve keep the confirmation pending and show a rejection inside it.',
        'Actions appear only when their callback is given; `onApproveMany` adds the selection checkboxes.',
        'Search matches `searchText`, or the requester name plus a text `title`.',
        'The ref is the root `<div>`, which takes focus (`tabIndex=-1`) when a decided row disappears.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies: People `dashboards/ApprovalQueue` `items[].approvalId` → `id`, `requester` → `requester`, `subjectLabel` → `title`, `overdue` → `overdue`, `compact` → `limit={4}`, the "See all" link → `footer`; the `useMutation` calls move into `onApprove` / `onReject`, and its ConfirmDialog with `reason` is built in. People `timesheets/ApprovalQueue`: the search input → `searchable`, the checkboxes and "Approve selected" → `onApproveMany`, Review → `onReview` (open the product’s side sheet), period, hours and billable → `summary`, "Resubmitted" and "HR override" → `badges`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'rejectReason',
          values: ['required', 'optional', 'none'],
          description: 'Whether sending back asks for a reason. Default `required`.',
        },
        {
          prop: 'searchable',
          values: ['true', 'false'],
          description: 'A search box over the list. Default false.',
        },
        {
          prop: 'limit',
          values: ['number'],
          description: 'Show only the first rows, for a dashboard card.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Overdue',
          attribute: 'data-overdue',
          description: 'A "Past due" danger badge on the row.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Tinted row while checked for bulk approval.',
        },
        {
          name: 'Deciding',
          attribute: 'data-busy',
          description: 'Approve shows a spinner (`aria-busy`) and send back is disabled.',
        },
        { name: 'Failed', description: 'An inline alert on the row after a rejected `onApprove`.' },
        { name: 'Empty', description: 'EmptyState "Nothing to review".' },
        { name: 'No matches', description: 'Filtered EmptyState with "Clear filters".' },
      ],
    },
    interaction: {
      items: [
        'Approve calls `onApprove`; on success the result is announced and the product removes the row.',
        'Send back opens a danger confirmation with the reason field focused; confirm calls `onReject(item, reason)`.',
        'Select all checks every decidable visible row; Approve selected confirms, then calls `onApproveMany`.',
        'When the row that had focus disappears, focus moves to the queue so keyboard users are not sent to the top of the page.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through search, select all, Approve selected, then each row’s checkbox and actions.',
        },
        { keys: 'Space', action: 'Toggles a checkbox.' },
        { keys: 'Enter / Space', action: 'Activates a button.' },
        {
          keys: 'Escape',
          action:
            'Closes a confirmation without deciding; focus returns to the button that opened it.',
        },
      ],
    },
    accessibility: {
      role: 'list of requests; confirmations are alertdialogs',
      name: '`messages.label`, "Requests waiting for you" by default.',
      items: [
        'Action buttons keep their visible text inside a longer accessible name that names the requester.',
        'Decisions are announced in a polite status region; the selected count is a polite live region.',
        'Row errors use `role="alert"`.',
        'The search box is a labelled `searchbox`.',
      ],
    },
    responsive: {
      items: [
        'Rows wrap: at narrow widths the actions move under the text and take the full row.',
        'On coarse pointers, buttons and the search box are at least 44px tall. Works at 320px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: success buttons for approve, danger badge for overdue, accent tint for selection.',
        'Forced colours outline selected rows with `Highlight`.',
        'Reduced motion removes the row background transition.',
      ],
    },
    examples: {
      items: [
        { title: 'Inbox with search and bulk approval', stack: true, render: () => <QueueDemo /> },
        {
          title: 'Dashboard card',
          stack: true,
          render: () => (
            <ApprovalQueue
              style={{ width: '100%', maxWidth: 480 }}
              now={NOW}
              items={ITEMS}
              limit={2}
              onApprove={() => {}}
              onReject={() => {}}
              footer={<a href="#approvals">See all 3 requests</a>}
            />
          ),
        },
        {
          title: 'Empty',
          render: () => <ApprovalQueue items={[]} onApprove={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Remove a request from `items` as soon as it is decided.',
        'Require a reason when sending back, so the requester knows what to change.',
        'Pass `now` from the server for identical server rendering.',
      ],
      dont: [
        'Do not let people approve their own requests; filter them out or set `canDecide: false`.',
        'Do not use bulk approval for requests that need individual review.',
      ],
    },
    related: {
      items: [
        { id: 'approval-timeline', reason: 'The steps of one request.' },
        { id: 'approval-chain-editor', reason: 'Define who approves.' },
        { id: 'confirm-dialog', reason: 'The confirmation used for send back and bulk approval.' },
        { id: 'data-table', reason: 'Many columns, sorting and paging.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'avatar',
        'badge',
        'button',
        'checkbox',
        'confirm-dialog',
        'empty-state',
        'relative-time',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Rows show requester, detail, title, time and overdue; actions are named per requester; axe passes.',
        'Approve shows a pending state and announces success; a failure shows a row alert.',
        'Send back requires a reason, focuses the field and returns focus; `rejectReason="none"` skips the field.',
        'Focus moves to the queue when the decided row disappears.',
        'Keyboard selection, indeterminate select all and confirmed bulk approval.',
        'Search, no-match state with clear filters, `limit`, `footer`, empty state, server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
