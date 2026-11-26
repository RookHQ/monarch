import { useState } from 'react';
import {
  Button,
  ContextMenu,
  ContextMenuCheckboxItem,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuSeparator,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PencilIcon, TrashIcon } from '../../../internal/docs-demo.js';

const regionStyle = {
  display: 'grid',
  placeItems: 'center',
  minHeight: '8rem',
  padding: '1rem',
  border: '1px dashed var(--mn-color-border-default)',
  borderRadius: 'var(--mn-radius-lg)',
  color: 'var(--mn-color-text-muted)',
  textAlign: 'center',
} as const;

function FileTile() {
  const [last, setLast] = useState('Nothing yet');
  return (
    <div style={{ display: 'grid', gap: '0.5rem', width: '100%', maxWidth: '24rem' }}>
      <ContextMenu label="File actions">
        <ContextMenuTrigger style={regionStyle}>
          <Button variant="ghost">Quarterly report.pdf</Button>
          <span>Right-click, press Shift+F10 on the file, or long-press on touch.</span>
        </ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuItem icon={<PencilIcon />} shortcut="F2" onSelect={() => setLast('Rename')}>
            Rename
          </ContextMenuItem>
          <ContextMenuItem onSelect={() => setLast('Duplicate')}>Duplicate</ContextMenuItem>
          <ContextMenuSeparator />
          <ContextMenuItem tone="danger" icon={<TrashIcon />} onSelect={() => setLast('Delete')}>
            Delete
          </ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>
      <p style={{ margin: 0, fontSize: 'var(--mn-text-sm)' }}>Last action: {last}</p>
    </div>
  );
}

function CanvasMenu() {
  const [grid, setGrid] = useState(true);
  return (
    <ContextMenu label="Canvas">
      <ContextMenuTrigger tabIndex={0} aria-label="Canvas" style={regionStyle}>
        Canvas: focus it and press Shift+F10
      </ContextMenuTrigger>
      <ContextMenuContent>
        <ContextMenuLabel>View</ContextMenuLabel>
        <ContextMenuCheckboxItem checked={grid} onCheckedChange={(on) => setGrid(on === true)}>
          Show grid
        </ContextMenuCheckboxItem>
        <ContextMenuSub>
          <ContextMenuSubTrigger>Zoom</ContextMenuSubTrigger>
          <ContextMenuSubContent>
            <ContextMenuItem>50%</ContextMenuItem>
            <ContextMenuItem>100%</ContextMenuItem>
            <ContextMenuItem>200%</ContextMenuItem>
          </ContextMenuSubContent>
        </ContextMenuSub>
      </ContextMenuContent>
    </ContextMenu>
  );
}

export default {
  id: 'context-menu',
  title: 'ContextMenu',
  summary: 'A menu of actions for a region, opened by right-click, Shift+F10 or long press.',
  sections: {
    purpose: {
      summary:
        'Offers shortcuts to the actions of an item or area (a file, a row, a message, a canvas) where people expect them: on right-click, with the keyboard context-menu shortcut, and on long press. Needed by: Rook ID and Rook Links (shadcn/ui ContextMenu copies) and Rook Tickets (InboxRow suppresses the browser menu during a press and needs a real one).',
      useWhen: [
        'Desktop-style surfaces where right-click is expected: file lists, inbox rows, boards, canvases.',
        'The same actions are also available from a visible button or a "More" menu.',
      ],
      avoidWhen: [
        'The menu would be the only way to reach an action: add a visible DropdownMenu or button.',
        'A single primary action: use a Button.',
        'Plain text or links where people expect the browser menu (copy, open in new tab).',
      ],
    },
    designPrinciples: {
      items: [
        'One menu family: ContextMenu is DropdownMenu opened at the pointer, so items, keyboard, danger tone and styles are identical.',
        'Supplementary, never essential: every action also lives somewhere visible.',
        'Three ways to open, for mouse, keyboard and touch users alike.',
        'Focus goes back to where it was when the menu closes.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Parts: ContextMenu (state), ContextMenuTrigger (the region, a `<div>` or `asChild`), ContextMenuContent (the panel, forwards its ref to the `role="menu"` element).',
        'Item parts are the DropdownMenu parts under ContextMenu names: ContextMenuItem, ContextMenuCheckboxItem, ContextMenuRadioGroup, ContextMenuRadioItem, ContextMenuLabel, ContextMenuSeparator, ContextMenuGroup, ContextMenuSub, ContextMenuSubTrigger, ContextMenuSubContent.',
        'ContextMenuContent takes DropdownMenuContent props (`side`, `align`, `sideOffset`, `collisionPadding`, …); default is below and after the pointer.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'ContextMenu: `open`/`onOpenChange` (optional control), `modal` (default true), `label` (accessible name, default "Actions").',
        'ContextMenuTrigger: `disabled` leaves the browser menu in place; `asChild` merges onto your element.',
        'ContextMenuItem: `tone="danger"`, `icon`, `shortcut` (display only), `onSelect`, `disabled`.',
        'Migrating from shadcn/ui (`@/components/ui/context-menu`): the part names are the same. `ContextMenuShortcut` becomes the `shortcut` prop of ContextMenuItem, `inset` is not needed (icons align automatically), `ContextMenuPortal` is built in, and destructive items use `tone="danger"` instead of a red class.',
      ],
    },
    variants: {
      items: [],
      notes: ['No visual variants: the panel is the shared menu. Items have `tone="danger"`.'],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open" on the menu',
          description: 'The menu shows at the pointer or under the focused element.',
        },
        {
          name: 'Pressing',
          attribute: 'data-pressing on the region',
          description: 'A touch is being held; text selection is turned off until it ends.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled on the region',
          description: 'The browser’s own context menu shows instead.',
        },
        {
          name: 'Highlighted item',
          attribute: 'data-highlighted',
          description: 'Muted background (danger-subtle for danger items).',
        },
      ],
    },
    interaction: {
      items: [
        'Right-click inside the region opens the menu at the pointer and suppresses the browser menu.',
        'Shift+F10 or the Menu key, while the region or something inside it has focus, opens the menu under that element.',
        'Holding a touch for half a second opens the menu at the finger; moving more than 10px cancels (it is a scroll), and the tap that ends the long press does not activate what is underneath.',
        'Choosing an item runs `onSelect` and closes the menu; Escape or a click outside closes it.',
        'Focus returns to the element that had it before the menu opened.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Shift+F10 / Menu',
          action: 'Opens the menu for the focused element in the region.',
        },
        { keys: '↓ / ↑', action: 'Next / previous item, wrapping.' },
        { keys: 'Home / End', action: 'First / last item.' },
        { keys: 'Enter / Space', action: 'Runs the item (toggles a checkbox or radio item).' },
        { keys: '→ / ←', action: 'Opens / closes a submenu.' },
        { keys: 'Letters', action: 'Typeahead to the next matching item.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus.' },
      ],
    },
    accessibility: {
      role: 'menu with menuitem, menuitemcheckbox and menuitemradio children',
      name: 'The `label` prop of ContextMenu (default "Actions"), or `aria-label`/`aria-labelledby` on ContextMenuContent.',
      items: [
        'Follows the WAI-ARIA APG menu pattern through DropdownMenu (Radix).',
        'The positioning anchor is a hidden, unfocusable element; the region itself keeps its own role and name.',
        'The region adds no Tab stop. When it has no focusable content, give it `tabIndex={0}` and a name so keyboard users can reach it.',
        'Context menus are hard to discover, so every action must also be reachable from a visible control.',
      ],
    },
    responsive: {
      items: [
        'The menu flips and shifts to stay inside the viewport and scrolls when taller than the space available.',
        'Long press makes it work on phones and tablets; the iOS text callout is turned off on the region.',
        'Under `pointer: coarse` items are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Uses the shared `mn-menu` surface, border, shadow and highlight tokens, so every theme and dark mode apply.',
        'Forced-colours mode outlines the panel and highlights the active item with system colours; the open animation stops under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        { title: 'File actions', render: () => <FileTile /> },
        { title: 'Checkbox items and a submenu', render: () => <CanvasMenu /> },
      ],
    },
    doDont: {
      do: [
        'Mirror the actions of a visible "More" menu, in the same order.',
        'Mark destructive items with `tone="danger"` and confirm them (useConfirm or Popconfirm).',
        'Name the menu with `label` when a page has more than one.',
      ],
      dont: [
        'Do not hide actions only in a context menu.',
        'Do not attach it to text people want to copy, or to links.',
        'Do not nest submenus more than one level.',
      ],
    },
    related: {
      items: [
        {
          id: 'dropdown-menu',
          reason: 'The same menu opened from a button; the visible alternative.',
        },
        { id: 'popconfirm', reason: 'Confirm a low-stakes action chosen from the menu.' },
        { id: 'confirm-dialog', reason: 'Confirm a destructive action chosen from the menu.' },
        { id: 'command-palette', reason: 'Search every action by name.' },
      ],
    },
    dependencies: {
      packages: ['react', '@radix-ui/react-slot'],
      components: ['dropdown-menu'],
    },
    testing: {
      unit: [
        'Right-click opens a named menu, cancels the browser menu, and items run with arrows and Enter.',
        'Shift+F10 and the Menu key open it from focused content; Escape closes and returns focus.',
        'Long touch press opens it and swallows the following click; moving cancels the press.',
        'Disabled region leaves the browser menu alone.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
