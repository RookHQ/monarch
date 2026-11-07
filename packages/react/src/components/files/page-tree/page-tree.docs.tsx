import { useRef, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { PageTree, type PageTreeGroup } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const GROUPS: PageTreeGroup[] = [
  {
    id: 'start',
    label: 'Getting started',
    pages: [
      { id: 'welcome', title: 'Welcome' },
      { id: 'setup', title: 'Set up your account' },
    ],
  },
  {
    id: 'guides',
    label: 'Guides',
    pages: [
      { id: 'invite', title: 'Invite your team' },
      { id: 'import', title: 'Import data' },
      { id: 'reports', title: 'Build a report' },
    ],
  },
  { id: 'reference', label: 'Reference', pages: [{ id: 'glossary', title: 'Glossary' }] },
];

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function Reader() {
  const [active, setActive] = useState('setup');
  return (
    <div style={{ maxWidth: 280 }}>
      <PageTree
        groups={GROUPS}
        activePageId={active}
        onSelectPage={setActive}
        footer={
          <Button size="sm" variant="ghost">
            All documents
          </Button>
        }
      />
    </div>
  );
}

function Organiser() {
  const [groups, setGroups] = useState(GROUPS);
  const [active, setActive] = useState('welcome');
  const [collapsed, setCollapsed] = useState(false);
  const counter = useRef(0);
  return (
    <div style={{ maxWidth: 300 }}>
      <PageTree
        groups={groups}
        activePageId={active}
        onSelectPage={setActive}
        collapsed={collapsed}
        onCollapsedChange={setCollapsed}
        onReorder={(next) => setGroups(next)}
        onAddPage={(groupId) => {
          counter.current += 1;
          const page = { id: `new-${counter.current}`, title: '' };
          setGroups((gs) =>
            gs.map((g) => (g.id === groupId ? { ...g, pages: [...g.pages, page] } : g)),
          );
          setActive(page.id);
        }}
        onAddGroup={async (label) => {
          await wait(300);
          setGroups((gs) => [...gs, { id: `g-${Date.now()}`, label, pages: [] }]);
        }}
        onRenameGroup={async (id, label) => {
          await wait(300);
          setGroups((gs) => gs.map((g) => (g.id === id ? { ...g, label } : g)));
        }}
        onDeleteGroup={async (id) => {
          await wait(500);
          setGroups((gs) => gs.filter((g) => g.id !== id));
        }}
        onDeletePage={async (id) => {
          await wait(400);
          setGroups((gs) => gs.map((g) => ({ ...g, pages: g.pages.filter((p) => p.id !== id) })));
        }}
      />
    </div>
  );
}

export default {
  id: 'page-tree',
  title: 'PageTree',
  summary:
    'Grouped, numbered pages of a document: navigation, or an organiser with reorder, move, add and delete.',
  sections: {
    purpose: {
      summary:
        'Shows the pages of a multi-page document in named groups with running numbers. Read-only it is the document’s navigation; with callbacks it is the organiser where pages and groups are reordered, moved, added, renamed and deleted. Needed by: Dossier.',
      useWhen: [
        'The sidebar of a multi-page document, handbook or guide (viewer and editor).',
        'Any two-level list of sections and pages that people arrange.',
      ],
      avoidWhen: [
        'Deep hierarchies (folders in folders): use TreeView.',
        'An outline of headings within one page: use a table of contents.',
      ],
    },
    designPrinciples: {
      items: [
        'Numbers run across groups (01, 02, 03…), so "page 7" means the same thing everywhere.',
        'Every pointer action has a keyboard path: reorder with the handle and arrow keys, move between groups and reorder groups from menus.',
        'Controlled and optimistic: `onReorder` hands the new groups to set straight away; persist in the background.',
        'Deleting is confirmed; deleting a group that holds pages asks for its name.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`PageTree` renders a `<nav>` and forwards its ref there. Each group is a labelled `role="group"`.',
        'Editing reuses ReorderableList (within a group), DropdownMenu, `useConfirm` and `usePrompt`; it renders their dialogs itself.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onReorder(groups, move)` receives the whole new order and what moved (`kind`, `id`, `fromGroupId`, `toGroupId`, `toIndex`), so the product can persist with one call.',
        'Callbacks that return promises keep the prompt or confirmation pending and show its error on rejection.',
        'Migrating from the app copies (Dossier `EditorPagePane` and `DocSidebar`): `document.sections` → `groups` (`section.label` → `label`, `section.pages` → `pages`); `activePageId` and `onSelectPage` keep their names; `collapsed` / `onToggleCollapsed` → `collapsed` / `onCollapsedChange`; `useReorderDocument` + the local draft and drag state → `onReorder` (keep the draft as your `groups` state); `useCreatePage` → `onAddPage(groupId)`; `useCreateSection` + `prompt` → `onAddGroup(label)`; `useUpdateSection` + `prompt` → `onRenameGroup(id, label)`; `useDeleteSection` + `confirm` → `onDeleteGroup(id)`; `useDeletePage` + `confirm` → `onDeletePage(id)`; DocSidebar’s `Link href` → `page.href` with `renderLink`, and its foot links → `footer`; `mobileOpen` stays in the product (place the tree in a Sheet).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'numbered',
          values: ['true', 'false'],
          description: 'Running page numbers before titles.',
        },
        {
          prop: 'onReorder',
          values: ['undefined', 'function'],
          description:
            'Read-only navigation, or the organiser with handles and menus. Exposed as `data-editable`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page"',
          description: 'Accent text and a tinted row.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed',
          description: 'Only the Show pages button.',
        },
        {
          name: 'Moving',
          attribute: 'data-active (row)',
          description: 'A page picked up with the keyboard or dragged.',
        },
        { name: 'Empty group', description: '"Add first page" (editable) or "No pages yet."' },
      ],
    },
    interaction: {
      items: [
        'Choose a page to call `onSelectPage` (and follow its link if it has one).',
        'Drag a page’s handle, or focus it and use Space and the arrow keys, to reorder within its group.',
        'A page’s menu moves it to another group or deletes it; a group’s menu renames, moves up or down, or deletes it.',
        'The plus button adds a page to that group; Add group asks for a name.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Pages, handles and menus in order.' },
        {
          keys: 'Space (on a handle), then ArrowUp / ArrowDown, then Space',
          action: 'Pick up, move and drop a page; Escape cancels.',
        },
        {
          keys: 'Enter / Space (on a menu button)',
          action: 'Opens the page or group menu; arrow keys move through it.',
        },
      ],
    },
    accessibility: {
      role: 'navigation',
      name: '`messages.label` ("Pages")',
      items: [
        'Each group is a group named by its label; page numbers are visual only.',
        'The current page has `aria-current="page"`.',
        'Moves are announced ("Goals moved to Appendix."), including ReorderableList’s pick-up and drop messages.',
        'Delete and rename use Monarch’s confirmation and prompt dialogs, with focus returned afterwards.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; titles truncate.',
        'Rows and menu buttons are 44px on coarse pointers. On phones, place it in a Sheet.',
      ],
    },
    theme: {
      items: [
        'The current page uses `--mn-color-accent-subtle` and `--mn-color-accent-strong`; numbers use `--mn-font-mono`.',
        'Forced colours: the current page gets a Highlight outline.',
      ],
    },
    examples: {
      items: [
        { title: 'Navigation', render: () => <Reader /> },
        { title: 'Organiser', render: () => <Organiser />, stack: true },
      ],
    },
    doDont: {
      do: [
        'Update `groups` in `onReorder` straight away, then persist; roll back if saving fails.',
        'Keep at least one page in a document; the tree hides Delete on the last page.',
      ],
      dont: [
        'Do not use it for more than two levels.',
        'Do not drop the keyboard path by replacing the menus with drag only.',
      ],
    },
    related: {
      items: [
        { id: 'tree-view', reason: 'Deeper hierarchies.' },
        { id: 'reorderable-list', reason: 'The reorder behaviour within a group.' },
        { id: 'confirm-dialog', reason: 'Delete confirmations.' },
        { id: 'prompt-dialog', reason: 'Group names.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'reorderable-list',
        'confirm-dialog',
        'dropdown-menu',
        'prompt-dialog',
      ],
    },
    testing: {
      unit: [
        'Grouped pages with running numbers, current page and selection; axe passes.',
        'Links, renderLink and collapsing.',
        'Keyboard reorder within a group reports the move; axe passes in edit mode.',
        'Move a page to another group and move groups from menus, with announcements.',
        'Rename and add groups through the prompt; delete a group with type-to-confirm; delete a page with confirmation; no delete for the last page.',
        'Add pages; untitled pages and translations; renders on the server read-only and editable.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
