import { useState } from 'react';
import { AiConversationList, type AiConversationSummary } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const HOUR = 3_600_000;
const DAY = 24 * HOUR;

function sample(): AiConversationSummary[] {
  const now = Date.now();
  return [
    { id: '1', title: 'Launch checklist', updatedAt: now - HOUR, preview: 'Week 9 is confirmed.' },
    { id: '2', title: 'Hiring plan', updatedAt: now - 2 * HOUR },
    { id: '3', title: 'Budget review', updatedAt: now - 3 * DAY, preview: 'Travel is 12% over.' },
    { id: '4', title: 'Team offsite agenda', updatedAt: now - 5 * DAY },
    { id: '5', title: 'Quarterly goals', updatedAt: now - 40 * DAY, pinned: true },
    { id: '6', title: 'Release notes draft', updatedAt: now - 60 * DAY },
  ];
}

function Demo() {
  const [items, setItems] = useState(sample);
  const [active, setActive] = useState('1');
  return (
    <div style={{ width: '100%', maxWidth: '18rem', height: 480 }}>
      <AiConversationList
        conversations={items}
        activeId={active}
        onSelect={setActive}
        onNew={() => {
          const id = String(Date.now());
          setItems((list) => [{ id, title: '', updatedAt: Date.now() }, ...list]);
          setActive(id);
        }}
        onRename={(id, title) =>
          setItems((list) => list.map((c) => (c.id === id ? { ...c, title } : c)))
        }
        onPin={(id, pinned) =>
          setItems((list) => list.map((c) => (c.id === id ? { ...c, pinned } : c)))
        }
        onDelete={(id) => setItems((list) => list.filter((c) => c.id !== id))}
      />
    </div>
  );
}

export default {
  id: 'ai-conversation-list',
  title: 'AiConversationList',
  summary: 'Conversation history grouped by recency, with search, rename, pin and delete.',
  sections: {
    purpose: {
      summary:
        'The history rail of an AI chat: past conversations grouped into Pinned, Today, Previous 7 days and Older, searchable, with the open one marked and per-conversation rename, pin and confirmed delete.',
      useWhen: [
        'A chat page where people return to earlier conversations.',
        'A sheet or popover listing conversations on small screens.',
      ],
      avoidWhen: [
        'A single docked assistant with one running conversation: use AiAssistantPanel.',
        'General record lists: use DataTable or a plain list.',
      ],
    },
    designPrinciples: {
      items: [
        'Recency first: groups match how people remember conversations.',
        'Deleting is always confirmed with a danger-toned dialog; nothing else is destructive.',
        'Quiet rows: the menu button appears on hover or focus with a fine pointer and is always visible on touch.',
        'Focus is never lost: after renaming, deleting or cancelling, it lands on a sensible row.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<nav>`; native attributes pass through and the ref is forwarded to it.',
        'Rename, Pin and Delete appear only when `onRename`, `onPin` and `onDelete` are given.',
        '`onDelete` may return a promise: the confirmation stays pending until it settles and shows the error on rejection.',
        'Without `onSearch` the list filters titles and previews itself; with it, the list shows `conversations` as given.',
        '`groupAiConversations(conversations, { now, timeZone })` is exported for custom layouts.',
        '`AI_CONVERSATION_LIST_MESSAGES` holds the English defaults; `messages.groups` merges per key.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['There are no visual variants.'] },
    states: {
      items: [
        {
          name: 'Active',
          attribute: 'aria-current="page" on the row, data-active on the item',
          description: 'The open conversation is highlighted.',
        },
        {
          name: 'Pinned',
          attribute: 'data-pinned',
          description: 'Listed in the Pinned group with a star.',
        },
        { name: 'Renaming', description: 'The row is a text field.' },
        { name: 'Searching', description: 'Only matching rows show; the count is announced.' },
        { name: 'No results', description: '“No conversations match …”.' },
        { name: 'Empty', description: '"No conversations yet."' },
      ],
    },
    interaction: {
      items: [
        'Choosing a row calls `onSelect(id)`.',
        'Rename turns the row into a field; Enter or leaving the field saves, Escape cancels; focus returns to the row.',
        'Pin moves the conversation to the Pinned group; Unpin moves it back by date.',
        'Delete opens a confirmation; on confirm `onDelete(id)` runs and focus moves to the conversation that took its place, or the search field.',
        'Cancelling the confirmation returns focus to where it was.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Arrow Down / Arrow Up', action: 'Next or previous conversation, across groups.' },
        { keys: 'Home / End', action: 'First or last conversation.' },
        { keys: 'Enter / Space', action: 'Opens the focused conversation.' },
        { keys: 'F2', action: 'Renames the focused conversation (with `onRename`).' },
        {
          keys: 'Delete',
          action: 'Deletes the focused conversation after confirmation (with `onDelete`).',
        },
        { keys: 'Escape', action: 'Cancels a rename, or clears the search field.' },
      ],
      notes: [
        'Each row’s menu button is a separate Tab stop; the shortcuts are exposed with aria-keyshortcuts.',
      ],
    },
    accessibility: {
      role: 'navigation',
      name: '"Conversation history" (aria-label)',
      items: [
        'Each group is a region named by its heading (level 3 by default).',
        'Rows are buttons named by the title and described by the preview; the open one has aria-current="page".',
        'Menu buttons are named "More actions for “<title>”".',
        'The search result count is announced in a polite status region.',
        'The delete confirmation is a ConfirmDialog (`role="alertdialog"`) with focus on Cancel.',
      ],
    },
    responsive: {
      items: [
        'Titles and previews truncate with an ellipsis, so a 390px sheet has no horizontal scroll.',
        'The groups scroll inside the component when it has a fixed height.',
        'On coarse pointers rows and menu buttons are at least 44px and the menu button is always visible.',
      ],
    },
    theme: {
      items: [
        'Hover: `--mn-color-bg-subtle`; active row: `--mn-color-accent-subtle` with `--mn-color-accent-strong` text.',
        'Group headings and previews: `--mn-color-text-muted`.',
        'In forced colours the active row is outlined in Highlight and menu buttons are always visible.',
      ],
    },
    examples: {
      items: [
        { title: 'History with rename, pin and delete', stack: true, render: () => <Demo /> },
      ],
    },
    doDont: {
      do: [
        'Keep titles short; generate them from the first exchange and let people rename them.',
        'Keep the list in a sheet on phones and a rail on wider screens.',
      ],
      dont: [
        'Do not delete without the confirmation.',
        'Do not reorder conversations while the person is reading the list, except when they pin or rename.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-conversation-header',
          reason: 'Heads the open conversation, with the same rename.',
        },
        { id: 'confirm-dialog', reason: 'The delete confirmation (useConfirm).' },
        { id: 'search-input', reason: 'The search field.' },
        { id: 'dropdown-menu', reason: 'The per-row menu.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'confirm-dialog', 'dropdown-menu', 'search-input'],
    },
    testing: {
      unit: [
        '`groupAiConversations` groups and sorts; empty groups left out.',
        'Navigation landmark, headed groups, active row, preview description, menus; props and ref forwarded.',
        'Click selects; arrow keys, Home and End move focus.',
        'Built-in filter with announced count and no-results; delegated `onSearch`.',
        'Rename from the menu and with F2; Enter saves, Escape cancels, focus returns.',
        'Pin and unpin; delete confirmed, focus moves to the next row; cancel deletes nothing.',
        'Empty state and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
