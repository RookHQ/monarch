import { useState } from 'react';
import { IssueCard, type PriorityLevel, type StatusOption } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './issue-card.fixture.json';

const labels = fixture.data.labels as [string, string];
const STATUSES = fixture.data.statuses as StatusOption[];

function EditableCard() {
  const [status, setStatus] = useState<string>(fixture.data.status);
  const [priority, setPriority] = useState(fixture.data.priority as PriorityLevel);
  return (
    <div style={{ width: '100%', maxWidth: 300 }}>
      <IssueCard
        issueKey={fixture.data.key}
        title={fixture.data.title}
        href="#"
        status={STATUSES.find((s) => s.id === status)}
        statuses={STATUSES}
        onStatusChange={setStatus}
        priority={priority}
        onPriorityChange={setPriority}
        labels={[
          { id: 'a11y', name: labels[0] },
          { id: 'board', name: labels[1] },
        ]}
        dueDate={fixture.data.dueDate}
        today={fixture.data.today}
        assignee={{ name: fixture.data.assignee }}
      />
    </div>
  );
}

export default {
  id: 'issue-card',
  title: 'IssueCard',
  summary:
    'An issue on a board or list: key, title, status, priority, labels, due date and assignee.',
  sections: {
    purpose: {
      summary:
        'Summarises one issue so people can scan a board, see what is urgent or late and who owns it, and change status or priority without opening it. Needed by: Rook Stride (`components/issues/views/IssueBoard.tsx`, `IssueList.tsx`, `components/home/MyWorkHome.tsx`), Rook Tickets queues and KanbanBoard cards.',
      useWhen: ['Board columns, backlog lists and “my work” views of issues or tasks.'],
      avoidWhen: [
        'Dense tables with many fields: use DataTable with StatusPicker and PriorityPicker cells.',
        'The full issue: use a detail panel.',
      ],
    },
    designPrinciples: {
      items: [
        'One link (the title) opens the issue and covers the card; pickers sit above it as separate controls.',
        'Every signal has words: status name, priority name (spoken), “Overdue”, “Assigned to …”.',
        'Overdue is decided from the calendar day and never on the server unless `today` is given, so server and client output match.',
        'Done or cancelled issues are never overdue.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Status and priority are read-only unless `onStatusChange` (with `statuses`) or `onPriorityChange` is set.',
        '`isIssueOverdue(due, today)` is exported.',
        'The ref is the root `<article>`, named by the title.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride board cards: `identifier` → `issueKey`, `title`, the team state → `status` (see StatusPicker), `priorityFromNumber(issue.priority)` → `priority`, the member → `assignee` `{ name, avatarSrc: avatarUrl }`, `labelIds` mapped to `{ id, name }` → `labels`, `dueDate` as is.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No variant props. Content decides what shows: omit `priority`, `labels`, `dueDate` or `assignee` to hide them; pass `assignee={null}` to show “Unassigned”.',
      ],
    },
    states: {
      items: [
        {
          name: 'Overdue',
          attribute: 'data-overdue',
          description: 'Due before today and not done: danger date with “Overdue”.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Accent border and background.',
        },
        {
          name: 'Dragging',
          attribute: 'data-dragging',
          description: 'Lifted with a larger shadow (no tilt under reduced motion).',
        },
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description: 'Has a link or `onOpen`.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the card opens the issue; clicking the status or priority opens its picker instead.',
        'For drag and drop, wrap cards in KanbanBoard or SortableGrid and set `dragging`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the priority picker, the title, then the status picker.' },
        { keys: 'Enter', action: 'Opens the issue (on the title) or a picker.' },
      ],
    },
    accessibility: {
      role: 'article',
      name: 'The issue title.',
      items: [
        'Read-only priority is an image named “Priority: High”; read-only status is spoken as “Status: In progress”.',
        'Labels are a list named “Labels”.',
        'The assignee avatar is named “Assigned to {name}”, or “Unassigned”.',
      ],
    },
    responsive: {
      items: [
        'Fills its column; titles clamp to three lines; meta wraps.',
        'Pickers are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface`, border `--mn-color-border-default`, shadow `--mn-shadow-xs` (`--mn-shadow-lg` while dragging).',
        'Overdue uses `--mn-color-danger-strong`.',
        'Forced colours: `CanvasText` border, `Highlight` when selected or focused.',
      ],
    },
    examples: {
      items: [
        { title: 'Editable status and priority', render: () => <EditableCard /> },
        {
          title: 'Read-only, overdue and unassigned',
          render: () => (
            <div style={{ width: '100%', maxWidth: 300 }}>
              <IssueCard
                issueKey="OPS-41"
                title="Renew the staging certificate"
                onOpen={() => {}}
                status={STATUSES[1]}
                priority="urgent"
                dueDate="2026-11-02"
                today="2026-11-07"
                assignee={null}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `today` from the server when you render on the server.',
        'Keep titles as written by people; the card clamps them.',
      ],
      dont: [
        'Do not add more buttons to the card; put extra actions in the detail view or a context menu.',
        'Do not colour the whole card by priority.',
      ],
    },
    related: {
      items: [
        { id: 'status-picker', reason: 'Changes status from the card.' },
        { id: 'priority-picker', reason: 'Changes priority from the card.' },
        { id: 'kanban-board', reason: 'Board columns with drag and drop.' },
        { id: 'dependency-picker', reason: 'Blocking relations between issues.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'priority-picker', 'status-picker'],
    },
    testing: {
      unit: [
        'Overdue by calendar day.',
        'Article named by its link; key, read-only priority and status, labels, due date, assignee.',
        'Overdue in words, not when done; today worked out after hydration.',
        'Open button and in-place pickers in tab order; status change from the menu.',
        'Unsafe links dropped, selected and dragging states, translated; axe; server rendering without guessing today.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
