import {
  Button,
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PencilIcon, TrashIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'dropdown-menu',
  title: 'DropdownMenu',
  summary: 'Action menu with checkbox, radio and nested items.',
  sections: {
    purpose: {
      summary: 'A list of actions or options that opens from a trigger button.',
      useWhen: ['Several related actions behind one button ("Actions", "…").'],
      avoidWhen: ['Choosing a form value: use Select or Combobox.'],
    },
    designPrinciples: {
      items: [
        '`tone="danger"` for destructive items, paired with a confirmation step (ConfirmDialog). The whole item, icon included, takes the danger colour, so the tone does not rely on the label alone.',
        'Destructive items go last, after a separator, so they are not hit by accident.',
        'Shortcut hints are display only; the app binds the keys.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Root, Trigger, Group, RadioGroup and Sub are the Radix DropdownMenu parts.',
        'Content defaults to `align="start"` and `sideOffset={4}` and renders in a portal.',
        'With `asChild`, DropdownMenuItem renders its child (for example a link) and ignores `icon` and `shortcut`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone (DropdownMenuItem)',
          values: ['default', 'danger'],
          description:
            'Exposed as `data-tone`. `danger` colours the label and icon with the danger text token and highlights with the danger surface.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Highlighted',
          attribute: 'data-highlighted',
          description: 'Item under focus or pointer.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Skipped by keyboard navigation.',
        },
        { name: 'Checked', description: 'Checkbox and radio items show a check or dot indicator.' },
      ],
    },
    interaction: {
      items: [
        'Selecting an item calls `onSelect` and closes the menu.',
        'Sub-menus open from DropdownMenuSubTrigger.',
        'To confirm a destructive item, open a ConfirmDialog from its `onSelect`; the menu closes first and the dialog returns focus to the menu trigger.',
        'Controlled with `open` + `onOpenChange`, or uncontrolled (`defaultOpen`).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Enter / Space / ArrowDown',
          action: 'On the trigger, opens the menu and focuses the first item.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Moves between items.' },
        { keys: 'Typeahead', action: 'Jumps to the item starting with the typed text.' },
        { keys: 'ArrowRight / ArrowLeft', action: 'Opens or closes a sub-menu.' },
        { keys: 'Enter', action: 'Selects the focused item.' },
        { keys: 'Escape', action: 'Closes the menu and restores focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'menu, menuitem, menuitemcheckbox, menuitemradio',
      name: 'The trigger’s label.',
      items: [
        'Item icons and shortcut hints are `aria-hidden`.',
        'A destructive item is not announced as such by tone alone: name the action clearly ("Delete ticket").',
      ],
    },
    responsive: {
      items: [
        'Menus are at least 12rem and at most `min(22rem, 100vw - 2rem)` wide, so they fit a 320px screen.',
        'Radix flips the menu to the other side and shifts it along the edge when it would leave the viewport (`avoidCollisions`, on by default); pass `collisionPadding` to keep a margin.',
        'Items are at least `--mn-control-sm` tall.',
      ],
    },
    theme: {
      items: [
        'Styles are the shared `.mn-menu` rules in styles/menu.css (also used by Select and Combobox); semantic tokens only.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Actions',
          render: () => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button trailingIcon={<MoreIcon />}>Actions</Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuLabel>Ticket</DropdownMenuLabel>
                <DropdownMenuItem shortcut="R">Rename</DropdownMenuItem>
                <DropdownMenuItem>Assign to me</DropdownMenuItem>
                <DropdownMenuCheckboxItem checked>Watching</DropdownMenuCheckboxItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem tone="danger">Delete</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
        {
          title: 'Destructive item with icons',
          render: () => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button>Document</Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuItem icon={<PencilIcon />} shortcut="E">
                  Edit
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem tone="danger" icon={<TrashIcon />}>
                  Move to trash
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Label the trigger with what the menu is about.',
        'Group items with labels and separators.',
      ],
      dont: [
        'Do not put form inputs inside a menu.',
        'Do not rely on the shortcut hint to bind keys.',
        'Do not run an irreversible action straight from a menu item without a confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'select', reason: 'Choosing one value for a form field.' },
        { id: 'popover', reason: 'Arbitrary floating content.' },
        { id: 'breadcrumb', reason: 'Uses a DropdownMenu for collapsed levels.' },
        { id: 'confirm-dialog', reason: 'The confirmation step for a destructive item.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-dropdown-menu', 'react'],
      components: [],
    },
    testing: {
      unit: [
        'Opens from the keyboard and focuses the first item; ArrowDown moves; Enter selects and closes.',
        'No axe violations in the open menu (jsdom).',
        'Destructive items expose `data-tone="danger"`, hide their icon from assistive technology, are reached with End and selected with Enter.',
        'Renders on the server (closed).',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: gallery/dropdown-menu, overlays/menu-light (open) and overlays/menu-danger-light-390 (open, phone width)',
        'Cross-browser axe: /components/dropdown-menu',
      ],
    },
  },
} satisfies ComponentDocs;
