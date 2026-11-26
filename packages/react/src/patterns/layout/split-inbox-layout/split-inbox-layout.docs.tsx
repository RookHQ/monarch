import { useState } from 'react';
import { Badge, DescriptionItem, DescriptionList, EmptyState } from '@rookcom/monarch-react';
import {
  RecordHeader,
  SplitInboxContextToggle,
  SplitInboxLayout,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const conversations = [
  {
    id: 'c1',
    subject: 'Printer offline on floor 3',
    from: 'Grace Hopper',
    preview: 'It stopped after the update this morning.',
  },
  {
    id: 'c2',
    subject: 'Invoice shows the wrong address',
    from: 'Alan Turing',
    preview: 'Our billing address changed in March.',
  },
  {
    id: 'c3',
    subject: 'Reset two-factor',
    from: 'Katherine Johnson',
    preview: 'I replaced my phone and lost the codes.',
  },
];

const listStyle = { listStyle: 'none', margin: 0, padding: 'var(--mn-space-2)' } as const;
const itemStyle = (active: boolean) =>
  ({
    display: 'grid',
    gap: '0.125rem',
    width: '100%',
    padding: 'var(--mn-space-2) var(--mn-space-3)',
    border: 0,
    borderRadius: 'var(--mn-radius-md)',
    background: active ? 'var(--mn-color-accent-subtle)' : 'transparent',
    color: 'inherit',
    font: 'inherit',
    textAlign: 'start',
    cursor: 'pointer',
  }) as const;

function InboxDemo() {
  const [openId, setOpenId] = useState<string | null>('c1');
  const open = conversations.find((c) => c.id === openId) ?? null;
  return (
    <div
      style={{
        height: '26rem',
        width: '100%',
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
        overflow: 'hidden',
      }}
    >
      <SplitInboxLayout
        listLabel="Conversations"
        detailLabel="Conversation"
        contextLabel="Ticket details"
        defaultListSize={280}
        defaultContextSize={260}
        detailOpen={open !== null}
        onBack={() => setOpenId(null)}
        backLabel="Back to conversations"
        list={
          <ul style={listStyle}>
            {conversations.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  style={itemStyle(c.id === openId)}
                  aria-current={c.id === openId ? 'true' : undefined}
                  onClick={() => setOpenId(c.id)}
                >
                  <strong style={{ fontSize: 'var(--mn-text-sm)' }}>{c.subject}</strong>
                  <span
                    style={{ fontSize: 'var(--mn-text-xs)', color: 'var(--mn-color-text-muted)' }}
                  >
                    {c.from} · {c.preview}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        }
        detail={
          open ? (
            <div style={{ padding: 'var(--mn-space-4)' }}>
              <RecordHeader
                headingLevel="h2"
                title={open.subject}
                status={<Badge tone="info">Open</Badge>}
                subtitle={`From ${open.from}`}
                actions={
                  <SplitInboxContextToggle
                    showLabel="Show ticket details"
                    hideLabel="Hide ticket details"
                  />
                }
              />
              <p style={{ margin: 0, fontSize: 'var(--mn-text-sm)' }}>{open.preview}</p>
            </div>
          ) : (
            <EmptyState
              size="compact"
              headingLevel="h2"
              title="Pick a conversation"
              description="Choose one from the list."
            />
          )
        }
        context={
          <div style={{ padding: 'var(--mn-space-4)' }}>
            <DescriptionList columns={1}>
              <DescriptionItem term="Requester">{open?.from}</DescriptionItem>
              <DescriptionItem term="Priority">High</DescriptionItem>
              <DescriptionItem term="Assignee">{null}</DescriptionItem>
            </DescriptionList>
          </div>
        }
      />
    </div>
  );
}

export default {
  id: 'split-inbox-layout',
  title: 'SplitInboxLayout',
  summary:
    'The one-page inbox: list, detail and context panes, resizable and collapsible, single-pane with back navigation on phones.',
  sections: {
    purpose: {
      summary:
        'Lays out a whole inbox on one page: a list pane, a detail pane and an optional context pane (requester, properties, activity). Panes resize with the pointer and the keyboard and collapse; below 768px it becomes one pane with a back button, and below 1280px the context pane becomes a drawer. The product supplies every pane’s content and owns the selection and URL state. One powerful page instead of five similar ones.',
      useWhen: [
        'A queue of items people work through one by one: tickets, conversations, applications, review requests.',
        'The list, the open item and its context should stay on screen together.',
      ],
      avoidWhen: [
        'A plain list-and-detail without resizing or a context pane: use SplitLayout.',
        'A table people scan and sort: use DataTable on its own page.',
      ],
    },
    designPrinciples: {
      items: [
        'Content-agnostic: the layout never fetches, routes or selects; it shows what it is given.',
        'Every state is a prop: `detailOpen`, `listCollapsed`, `contextOpen` and the sizes can live in the URL or storage, with change callbacks.',
        'The same layout from 320px to wide screens: one pane, two panes, three panes.',
        'Panes are landmarks, and focus moves with what the user is looking at.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` with the list (`<aside>`), the detail (`<section>`) and the context (`<aside>`, or a Sheet below 1280px). Forwards its ref.',
        'The splitters are APG window splitters: `role="separator"` with `aria-valuenow` (px), `aria-valuemin`, `aria-valuemax` and `aria-controls`.',
        'SplitInboxContextToggle is the details button for the detail pane’s header: it toggles the column on wide screens and the drawer otherwise, with `aria-expanded`.',
        '`useSplitInboxLayout()` returns `{ layout, contextMode, contextVisible, setContextVisible, toggleContext, listCollapsed, setListCollapsed, focusPane, ids }` inside a pane (or `null`), for custom toggles such as a focus-mode button.',
        'Breakpoints are fixed (768px and 1280px) and shared by the stylesheet and the component, so the first render matches the server.',
        'Exported from `@rookcom/monarch-react/patterns`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'context',
          values: ['omitted', 'provided'],
          description: 'Two panes, or three with the context pane and its splitter.',
        },
        {
          prop: 'resizable',
          values: ['true', 'false'],
          description: 'Shows the splitters. Without them panes keep their sizes.',
        },
        {
          prop: 'layout (from the viewport)',
          values: ['split (768px and up)', 'single (below 768px)'],
          description: 'Exposed as `data-layout`. In `single`, `detailOpen` picks the pane.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Detail open',
          attribute: 'data-detail-open',
          description: 'Below 768px the detail pane replaces the list.',
        },
        {
          name: 'List collapsed (focus mode)',
          attribute: 'data-list-collapsed',
          description:
            'On wide screens the list is hidden; its splitter stays as a thin handle that expands it.',
        },
        {
          name: 'Context open',
          attribute: 'data-context-open',
          description: 'The context column shows (1280px and up).',
        },
        {
          name: 'Context drawer',
          description:
            'Below 1280px the context opens as a modal Sheet; it starts closed so a remembered "open" never opens a modal on load.',
        },
      ],
    },
    interaction: {
      items: [
        'Drag a splitter to resize its pane, clamped to the min and max; sizes are reported through `onListSizeChange` and `onContextSizeChange`.',
        'Clicking the collapsed list handle expands the list.',
        'Below 768px, opening an item (`detailOpen`) moves focus to the detail pane; going back returns focus to the element that opened it.',
        'Closing the drawer returns focus to the button that opened it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'F6 / Shift+F6',
          action:
            'Moves focus to the next or previous visible pane, back to the element focused there last.',
        },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'On a splitter, moves it by `resizeStep` (16px); with Shift, four times as far.',
        },
        {
          keys: 'Home / End',
          action: 'On a splitter, sets its pane to the minimum or maximum size.',
        },
        {
          keys: 'Enter',
          action:
            'On the list splitter, collapses or expands the list; on the context splitter, closes the context pane.',
        },
        { keys: 'Escape', action: 'Closes the context drawer.' },
      ],
    },
    accessibility: {
      role: 'complementary (list), region (detail), complementary (context) landmarks',
      name: '`listLabel`, `detailLabel` and `contextLabel`.',
      items: [
        'Each splitter is focusable and named (`listResizeLabel`, `contextResizeLabel`) with its value in pixels.',
        'Panes are programmatic focus targets (`tabindex="-1"`) with a visible focus ring when reached by F6.',
        'The drawer is a modal dialog titled by `contextLabel` with a named close button; focus is trapped inside it.',
        'Arrow keys follow the reading direction in right-to-left layouts.',
      ],
    },
    responsive: {
      items: [
        '1280px and up: list, detail and context side by side.',
        '768px to 1279px: list and detail; the context pane is a drawer.',
        'Below 768px: one pane at a time with a back button in the detail pane; splitters are hidden.',
        'The layout fills its container (`height: 100%`); give the container a height.',
      ],
    },
    theme: {
      items: [
        'Pane surfaces, borders and the splitter highlight use Monarch tokens, so every theme applies.',
        'In forced-colours mode the splitter highlight uses the system Highlight colour.',
        'The splitter highlight transition is removed with reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Inbox with list, conversation and details',
          stack: true,
          render: () => <InboxDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep the open item, focus mode and pane sizes in the URL or storage, and pass them in.',
        'Give the container a fixed height (the viewport minus the app header).',
        'Name the panes after their content: "Conversations", "Conversation", "Ticket details".',
      ],
      dont: [
        'Do not build separate pages for the list and the item; this is the one page.',
        'Do not move focus into the detail pane yourself on wide screens; the user may still be in the list.',
        'Do not put a second SplitInboxLayout inside a pane.',
      ],
    },
    related: {
      items: [
        { id: 'split-layout', reason: 'A simpler two-pane list and detail.' },
        { id: 'record-header', reason: 'The header of the open item in the detail pane.' },
        { id: 'sheet', reason: 'The drawer the context pane becomes on narrower screens.' },
        { id: 'empty-state', reason: 'The detail pane before anything is selected.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'sheet'],
    },
    testing: {
      unit: [
        'Renders the panes as labelled landmarks with two splitters; ref forwarding.',
        'List splitter: arrows, Shift+arrows, Home/End and clamping; Enter collapses and an arrow expands (uncontrolled).',
        'Controlled sizes change only through the parent.',
        'Pointer drag resizes both panes in the right direction, clamped; moves after release are ignored.',
        'Context splitter: ArrowLeft widens; Enter closes the pane and moves focus to the detail.',
        'SplitInboxContextToggle toggles the context column with aria-expanded and aria-controls.',
        'Below 1280px the context is a modal drawer, closed at first; Escape closes it and returns focus to the toggle.',
        'Below 768px one pane shows; opening moves focus to the detail and Back returns it to the opener.',
        'F6 and Shift+F6 cycle the visible panes, returning to the last focused element.',
        'A collapsed list is skipped by F6; useSplitInboxLayout exposes the state and is null outside.',
        'Renders on the server in the wide layout.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/split-inbox-layout',
        'Cross-browser axe: /components/split-inbox-layout',
      ],
    },
  },
} satisfies ComponentDocs;
