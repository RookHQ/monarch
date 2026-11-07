import { useState } from 'react';
import { FavoriteToggle, FavoritesList, type FavoriteItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { BoardIcon, ListIcon, InboxIcon } from '../../../internal/docs-demo.js';

const pinned: FavoriteItem[] = [
  {
    id: 'project:1',
    label: 'Website refresh',
    href: '#favorites',
    icon: <BoardIcon />,
    current: true,
  },
  { id: 'view:2', label: 'My open tasks', href: '#favorites', icon: <ListIcon /> },
  { id: 'doc:3', label: 'Launch checklist', href: '#favorites', icon: <InboxIcon /> },
];

function EditableFavorites() {
  const [items, setItems] = useState(pinned);
  return (
    <div style={{ width: '16rem' }}>
      <FavoritesList
        items={items}
        onRemove={(item) => setItems((list) => list.filter((i) => i.id !== item.id))}
        onReorder={setItems}
        empty="Star a page to pin it here."
      />
    </div>
  );
}

function PageHeaderStar() {
  const [pinnedNow, setPinned] = useState(false);
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
      <strong>Website refresh</strong>
      <FavoriteToggle pressed={pinnedNow} onPressedChange={setPinned} />
    </span>
  );
}

export default {
  id: 'favorites-list',
  title: 'FavoritesList',
  summary:
    'Sidebar section of pinned pages, with an edit mode to reorder and remove; plus FavoriteToggle.',
  sections: {
    purpose: {
      summary:
        'Gives people one-click access to the projects, views and documents they pinned, in the sidebar or rail. The product resolves names and links and stores the list; Monarch renders it, marks the current page and handles editing. Needed by: Stride (sidebar Favorites and the star on pages); other workspace apps with pinned items.',
      useWhen: [
        'A sidebar section of items the person pinned themselves.',
        'A star button on a page header or row that pins it (`FavoriteToggle`).',
      ],
      avoidWhen: [
        'Recently visited pages: they are automatic, not chosen; use a plain list.',
        'The main navigation of the product: use the shell navigation.',
      ],
    },
    designPrinciples: {
      items: [
        'Links first: in normal use each row is just a link, with the current page marked.',
        'Editing is a mode: an Edit button reveals remove buttons and drag handles, so the sidebar stays calm.',
        'Data in, callbacks out: no fetching, no storage, no product routes.',
        'Disappears when empty by default, like any navigation section with nothing in it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`FavoritesList` renders a `nav` named by its heading; its ref goes to the `nav`.',
        'Edit mode appears only when `onRemove` or `onReorder` is given. Rows become a ReorderableList when `onReorder` is given.',
        '`FavoriteToggle` is an IconButton with `aria-pressed`; its ref goes to the button.',
        '`FAVORITES_LIST_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`items` is `{ id, label, href, icon?, current? }[]`, already resolved by the product.',
        '`linkAs` takes a router link (for example Next.js `Link`); it receives `href`, `className` and `aria-current`.',
        'Migrating from the app copy (Stride `components/shell/Favorites` and `FavoritesSection` in `layout/Sidebar`): keep `useFavorites()` in the product, map each favorite to `{ id: type + id, label, href, icon, current: isActive(href) }` and pass `onRemove={toggle}` and `onReorder={reorder}`. `FavoriteButton` → `<FavoriteToggle pressed={has(item)} onPressedChange={() => toggle(item)} />`; its name no longer changes with the state.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onRemove / onReorder',
          values: ['none', 'remove', 'reorder', 'both'],
          description:
            'Which editing tools the Edit mode offers. With neither, there is no Edit button.',
        },
        {
          prop: 'showIcon',
          values: ['true', 'false'],
          description: 'The star before the heading. Default `true`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'Rows show remove buttons and, with `onReorder`, drag handles.',
        },
        {
          name: 'Current',
          attribute: 'data-current',
          description: 'The row for the current page; also `aria-current="page"`.',
        },
        { name: 'Empty', description: 'Renders nothing, or the `empty` content when given.' },
        {
          name: 'Pinned',
          attribute: 'data-pressed',
          description: 'FavoriteToggle: the star is filled and `aria-pressed="true"`.',
        },
      ],
    },
    interaction: {
      items: [
        'Edit toggles edit mode; its label switches between "Edit favorites" and "Done".',
        'Remove calls `onRemove`, announces the removal and moves focus to the next row’s remove button (or Done).',
        'In edit mode with `onReorder`, rows can be dragged by the handle or moved with the keyboard; `onReorder` receives the new order.',
        'FavoriteToggle calls `onPressedChange` with the new state.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the links (or, when editing, the handles and remove buttons).',
        },
        { keys: 'Enter', action: 'Follows a link, or activates Edit, Done or Remove.' },
        {
          keys: 'Space, then Arrow Up / Down, then Space',
          action: 'On a handle: pick up, move and drop a row (from ReorderableList).',
        },
        { keys: 'Escape', action: 'While moving a row: cancels the move.' },
      ],
    },
    accessibility: {
      role: 'navigation containing a list of links',
      name: 'The visible heading ("Favorites", from `messages.title`).',
      items: [
        'The current page’s link has `aria-current="page"`.',
        'Remove buttons are named per item ("Remove Launch checklist from favorites").',
        'Removals are announced through a polite status region; focus never falls back to the page.',
        'Reordering uses ReorderableList, which announces pick-up, moves and drops.',
        'FavoriteToggle keeps one name and reports its state with `aria-pressed`.',
        'Rows, Edit and Remove are at least 44px tall on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'Long names are truncated with an ellipsis; the full name stays in the link text.',
        'Fills its container width, so it works in a 390px drawer.',
      ],
    },
    theme: {
      items: [
        'The current row uses the accent subtle token; hover uses the muted background.',
        'The pinned star uses the warning strong token.',
        'Forced colours: links use LinkText, the current row a Highlight outline.',
        'Reduced motion removes the hover transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Read-only sidebar section',
          render: () => (
            <div style={{ width: '16rem' }}>
              <FavoritesList items={pinned} />
            </div>
          ),
        },
        { title: 'Editable: remove and reorder', render: () => <EditableFavorites /> },
        { title: 'Star toggle on a page header', render: () => <PageHeaderStar /> },
      ],
    },
    doDont: {
      do: [
        'Resolve names in the product and skip items the person can no longer see.',
        'Use the same item id format in FavoriteToggle and FavoritesList.',
        'Pass a router link through `linkAs` so navigation stays client-side.',
      ],
      dont: [
        'Do not fetch inside the list; pass resolved items.',
        'Do not change the FavoriteToggle label with its state; `aria-pressed` already says it.',
        'Do not show remove buttons outside edit mode.',
      ],
    },
    related: {
      items: [
        { id: 'reorderable-list', reason: 'The reordering used in edit mode, for any list.' },
        { id: 'link', reason: 'A single inline link.' },
        { id: 'saved-views', reason: 'Saved filters and views inside one list or table.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'reorderable-list'],
    },
    testing: {
      unit: [
        'Named navigation of links with `aria-current="page"`; no Edit button when read-only; no axe violations.',
        'Renders nothing when empty unless `empty` is given.',
        'Custom link component.',
        'Edit mode removes items, announces them and moves focus to the next remove button; the section disappears when empty.',
        'Edit mode with `onReorder` moves rows with the keyboard.',
        'Controlled edit mode; translated strings; ref forwarding; renders on the server.',
        'FavoriteToggle: stable name, `aria-pressed` toggles by click and Space; no axe violations.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
