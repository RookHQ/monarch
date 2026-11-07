import { useState } from 'react';
import { Avatar, Badge } from '@rookcom/monarch-react';
import { InboxList, InboxRow } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { InboxIcon, UsersIcon } from '../../../internal/docs-demo.js';

const NOW = new Date(2026, 4, 10, 12);

const ROWS = [
  {
    id: '41',
    title: 'Jordan Kim',
    preview: 'Can I change the delivery address on my order?',
    at: new Date(2026, 4, 10, 11, 55),
    unread: true,
    status: 'Open',
    tone: 'info' as const,
  },
  {
    id: '40',
    title: 'Riley Chen',
    preview: 'Thanks, that fixed it.',
    at: new Date(2026, 4, 10, 9, 10),
    unread: false,
    status: 'Solved',
    tone: 'success' as const,
  },
  {
    id: '38',
    title: 'Taylor Brooks',
    preview: 'The invoice still shows the old company name.',
    at: new Date(2026, 4, 8, 16, 0),
    unread: false,
    status: 'Waiting',
    tone: 'warning' as const,
  },
];

function InboxDemo() {
  const [open, setOpen] = useState('41');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  return (
    <div style={{ width: '100%', maxWidth: '26rem' }}>
      <InboxList aria-label={`Open conversations, ${ROWS.length}`}>
        {ROWS.map((row) => (
          <InboxRow
            key={row.id}
            title={row.title}
            href={`#conversation-${row.id}`}
            preview={row.preview}
            timestamp={row.at}
            now={NOW}
            unread={row.unread && open !== row.id}
            current={open === row.id}
            icon={<InboxIcon />}
            iconLabel="via email"
            badges={
              <Badge size="sm" tone={row.tone}>
                {row.status}
              </Badge>
            }
            trailing={
              <>
                <span>#{row.id}</span>
                <Avatar name="Alex Morgan" size="xs" />
              </>
            }
            onOpen={() => setOpen(row.id)}
            selected={selected.has(row.id)}
            selecting={selected.size > 0}
            onSelectedChange={(on) =>
              setSelected((s) => {
                const next = new Set(s);
                if (on) next.add(row.id);
                else next.delete(row.id);
                return next;
              })
            }
          />
        ))}
      </InboxList>
    </div>
  );
}

export default {
  id: 'inbox-row',
  title: 'InboxRow',
  summary: 'One conversation in an inbox: name, age, preview, markers, unread and selection.',
  sections: {
    purpose: {
      summary:
        'A dense, scannable row for one conversation, ticket or thread in an inbox list: who it is with, how long ago it changed, the latest message, status markers, and whether it is unread, open or selected. The row is a real link, so opening in a new tab works, and with `onSelectedChange` its avatar turns into a checkbox for bulk actions (long-press on touch). Needed by: Tickets (inbox list), Chat (activity and conversation lists).',
      useWhen: [
        'A list of conversations or tickets that open in a pane or page.',
        'People triage many items and need unread, status and bulk selection at a glance.',
      ],
      avoidWhen: [
        'Notifications in a popover: use NotificationCenter.',
        'Messages inside a conversation: use MessageRow.',
        'Tabular data with sortable columns: use a data table.',
      ],
    },
    designPrinciples: {
      items: [
        'A real link: middle-click, Ctrl or Cmd-click and "open in new tab" behave as people expect; a plain click can open in place.',
        'Unread is bold text plus a dot or count, never colour alone, and is part of the link name.',
        'Fixed structure (title line, preview line, meta line) so rows can have a fixed height in a virtualised list.',
        'Selection never hides behind hover only: checkboxes show on touch screens and whenever anything is selected.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Put rows in `InboxList` and name it with `aria-label`, including the count if known.',
        '`formatCompactAge(date, now?, locale?, units?)` is the "5m / 3h / 2d" age. `INBOX_ROW_MESSAGES` holds the English defaults, including the age units.',
        'Keyboard cursor and list-level shortcuts (J/K, x to select) belong to the list; mark the cursor row with `active`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Tickets `components/inbox/InboxRow.tsx`): `t` is split into `title` (`displayName(t)`), `preview` (`previewText(t)`), `timestamp` (`awaitingAgent ? lastCustomerMessageAt : lastActivityAt`), `unread` (`t.awaitingAgent`), `icon`/`iconLabel` (the channel), `badges` (status, SlaBadge, PriorityBadge) and `trailing` (`#number` and assignee avatar). `open` → `current`; `active`, `selected`, `selecting` and `href` are unchanged; `onOpen(id)` → `onOpen(event)`; `onToggle(id, shift)` → `onSelectedChange(selected, { shiftKey })`. Positioning (`top`, `ROW_HEIGHT`) moves to `style` from the virtual list. Chat’s `ActivityGroupRow` maps the same way, with the grouped count in `preview` or `badges`.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. Without `onSelectedChange` there is no checkbox; `avatar={null}` drops the leading avatar.',
      ],
    },
    states: {
      items: [
        {
          name: 'Unread',
          attribute: 'data-unread',
          description: 'Bold title and preview, with a dot or a count badge.',
        },
        {
          name: 'Current',
          attribute: 'data-current',
          description:
            'Open in the pane: accent background, a bar on the leading edge and `aria-current`.',
        },
        {
          name: 'Active',
          attribute: 'data-active',
          description: 'The list’s keyboard cursor is on the row.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Checkbox ticked, accent background.',
        },
        {
          name: 'Selecting',
          attribute: 'data-selecting',
          description: 'Something is selected: every checkbox shows and a plain click selects.',
        },
      ],
    },
    interaction: {
      items: [
        'A plain click opens the conversation with `onOpen`; Ctrl, Cmd or middle click keep the browser’s behaviour.',
        'Shift-click on the row, or any click while selecting, toggles selection instead of opening.',
        'A long press (500ms) on touch screens selects, and the click that follows is ignored.',
        'The checkbox appears in place of the avatar on hover, focus, touch screens and while selecting.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the checkbox (when selectable), then the row link.' },
        { keys: 'Space / Enter on the checkbox', action: 'Toggles selection.' },
        { keys: 'Enter on the link', action: 'Opens the conversation.' },
      ],
    },
    accessibility: {
      role: 'listitem containing a link (and a checkbox when selectable)',
      name: 'The link reads the title, channel, unread state, full time, preview and markers',
      items: [
        'The checkbox is a button with `role="checkbox"` named "Select …" (messages.select).',
        'The compact age is hidden from screen readers in favour of the full date and time.',
        'The open conversation has `aria-current="true"`.',
        'Avatars and icons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Title and preview truncate to one line each, so rows keep their height at 390px.',
        'The link is at least 44px tall and the checkbox column 48px wide.',
        'On devices without hover the checkbox is always visible.',
      ],
    },
    theme: {
      items: [
        'Row: `--mn-color-bg-surface`, hover `--mn-color-bg-subtle`, active `--mn-color-bg-muted`, divider `--mn-color-border-subtle`.',
        'Current and selected: `--mn-color-accent-subtle`, with `--mn-color-accent-solid` for the bar, dot, count and checkbox.',
        'In forced colours the bar, dot, count and ticked checkbox use Highlight; transitions stop under reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Inbox with selection', stack: true, render: () => <InboxDemo /> },
        {
          title: 'Unread count, no selection',
          render: () => (
            <div style={{ width: '100%', maxWidth: '26rem' }}>
              <InboxList aria-label="Channels">
                <InboxRow
                  title="Design reviews"
                  href="#channel-design"
                  preview="Sam: the new header is ready for a look"
                  timestamp={new Date(2026, 4, 10, 11, 20)}
                  now={NOW}
                  unread={12}
                  avatar={{ name: 'Design reviews' }}
                  icon={<UsersIcon />}
                  iconLabel="channel"
                />
              </InboxList>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the preview to plain text from the latest message.',
        'Use `badges` for at most two or three markers that change what people do next.',
        'Give the list an `aria-label` with its view name and count.',
      ],
      dont: [
        'Do not put buttons inside the link; use the list’s bulk actions or the opened pane.',
        'Do not show unread with colour alone.',
        'Do not swallow Ctrl or Cmd clicks.',
      ],
    },
    related: {
      items: [
        { id: 'virtual-message-list', reason: 'Virtualising long lists.' },
        { id: 'notification-center', reason: 'Notifications in a popover.' },
        { id: 'message-row', reason: 'Messages inside a conversation.' },
        { id: 'avatar', reason: 'The leading avatar.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar'] },
    testing: {
      unit: [
        'Compact ages from minutes to dates.',
        'List item with one link, full accessible name, `aria-current`, unread count capped at 99+.',
        'Plain click opens in place; Ctrl-click is left to the browser.',
        'Checkbox, shift-click and click-while-selecting toggle selection; keyboard on the checkbox.',
        'Long-press selects on touch and swallows the click; badges, trailing and no avatar.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
