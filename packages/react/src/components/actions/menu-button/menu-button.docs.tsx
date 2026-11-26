import {
  DropdownMenuCheckboxItem,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  MenuButton,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PencilIcon, TrashIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'menu-button',
  title: 'MenuButton',
  summary: 'A button that opens a DropdownMenu, with the menu button ARIA pattern built in.',
  sections: {
    purpose: {
      summary:
        'Puts several related actions behind one button without wiring DropdownMenu, its trigger and the chevron by hand. Needed by: Rook People (`people/page.tsx`, `org/DepartmentTree.tsx`, `admin/PermissionsView.tsx`), Rook Calendar (`forms/page.tsx`) and about 50 other app files that compose shadcn/ui `DropdownMenuTrigger asChild` + `Button` themselves.',
      useWhen: [
        'A row, card or page header has more actions than fit as buttons ("Actions ▾", "⋯").',
        'An export or sort choice is made from a short list of actions.',
      ],
      avoidWhen: [
        'One action is the obvious default and the rest are alternatives: use SplitButton.',
        'The list selects a value that stays shown: use Select.',
        'The list is long or searchable: use Combobox or CommandPalette.',
      ],
    },
    designPrinciples: {
      items: [
        'The button looks like any Button, plus a chevron that turns when the menu is open.',
        'Actions can be data (`items`) or markup (DropdownMenu children); both render the same Monarch menu.',
        'Destructive items use `tone: "danger"` and should open a confirmation, never act immediately.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'MenuButton forwards its ref to the `<button>`. Button props (`variant`, `size`, `loading`, `disabled`, …) pass through.',
        'MenuButtonItems renders `MenuButtonEntry[]` inside any DropdownMenuContent; SplitButton uses it too.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui: replace `<DropdownMenu><DropdownMenuTrigger asChild><Button>Label <ChevronDown/></Button></DropdownMenuTrigger><DropdownMenuContent align>…` with `<MenuButton label="Label" align>…children…</MenuButton>`. `open`, `defaultOpen`, `onOpenChange` and `modal` map to the DropdownMenu root; `align` and `side` to the content.',
        '`items[].href` is checked with `isSafeUrl`; `javascript:` and other unsafe URLs render as a plain item without a link.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant / size',
          values: ['Button variants', 'xs', 'sm', 'md', 'lg'],
          description: 'Passed to the Button. Default `secondary`, or `ghost` when `iconOnly`.',
        },
        {
          prop: 'iconOnly',
          values: ['true', 'false'],
          description:
            'Square icon button ("⋯") with `label` as its accessible name and tooltip; no chevron. Exposed as `data-icon-only`.',
        },
        {
          prop: 'align / side',
          values: ['start', 'center', 'end', 'top', 'right', 'bottom', 'left'],
          description:
            'Menu placement. Default `start`, `bottom`; Radix flips it to stay on screen.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'aria-expanded="true", data-state="open"',
          description: 'The chevron turns 180°.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'The button is not clickable and the menu does not open.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy, data-loading',
          description: 'Shows the Button spinner, stays focusable, and does not open the menu.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer down, Enter, Space or Arrow Down opens the menu with focus on the first item.',
        'Choosing an item runs its `onSelect`, closes the menu and returns focus to the button.',
        'Clicking outside or pressing Escape closes the menu without choosing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / Arrow Down', action: 'Opens the menu on its first item.' },
        { keys: 'Arrow Down / Arrow Up', action: 'Moves between items (wraps).' },
        { keys: 'Home / End', action: 'First or last item.' },
        { keys: 'Type a letter', action: 'Jumps to the next item starting with it.' },
        { keys: 'Enter / Space', action: 'Chooses the focused item.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus to the button.' },
      ],
    },
    accessibility: {
      role: 'button (aria-haspopup="menu") controlling a menu',
      name: 'The visible `label`; with `iconOnly`, `label` becomes `aria-label`.',
      items: [
        'Follows the WAI-ARIA menu button pattern; the menu is labelled by the button (`aria-labelledby`).',
        'The chevron and item icons are decorative and hidden from assistive technology.',
        'A loading button keeps focus and does not open, so keyboard users keep their place.',
      ],
    },
    responsive: {
      items: [
        'The button is at least 44 × 44 px on coarse pointers.',
        'The menu stays inside the viewport (collision detection) and is at most the viewport width.',
      ],
    },
    theme: {
      items: [
        'Button and menu use semantic tokens, so light, dark and more-contrast themes apply; danger items use the danger tokens.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Items as data',
          render: () => (
            <MenuButton
              label="Actions"
              items={[
                { label: 'Rename', icon: <PencilIcon /> },
                { label: 'Duplicate', shortcut: '⌘D' },
                'separator',
                { label: 'Delete', icon: <TrashIcon />, tone: 'danger' },
              ]}
            />
          ),
        },
        {
          title: 'Icon only, aligned to the end',
          render: () => (
            <MenuButton
              label="More actions"
              iconOnly
              icon={<MoreIcon />}
              align="end"
              items={[{ label: 'Copy link' }, { label: 'Move to folder' }]}
            />
          ),
        },
        {
          title: 'Children',
          render: () => (
            <MenuButton label="View" variant="ghost" size="sm">
              <DropdownMenuLabel>Columns</DropdownMenuLabel>
              <DropdownMenuCheckboxItem checked>Owner</DropdownMenuCheckboxItem>
              <DropdownMenuCheckboxItem>Due date</DropdownMenuCheckboxItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem>Reset columns</DropdownMenuItem>
            </MenuButton>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Label the button with what the menu contains ("Export", "Actions").',
        'Put destructive items last, after a separator, with `tone: "danger"`.',
      ],
      dont: [
        'Do not hide the only primary action of a page inside a menu.',
        'Do not use a menu for navigation tabs or a selected value.',
      ],
    },
    related: {
      items: [
        { id: 'dropdown-menu', reason: 'The menu primitives MenuButton composes.' },
        { id: 'split-button', reason: 'A default action plus a menu of alternatives.' },
        { id: 'select', reason: 'Choosing a value that stays visible.' },
        { id: 'button-group', reason: 'Joins a MenuButton with related buttons.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'dropdown-menu'] },
    testing: {
      unit: [
        '`aria-haspopup` and `aria-expanded`; Enter opens on the first item; the menu is labelled by the button; Escape closes and returns focus; Arrow Down opens; Enter chooses.',
        'Accepts DropdownMenu children and a controlled `open`.',
        'Renders safe `href` items as links and drops unsafe URLs.',
        'Icon-only form uses `label` as its name and has no chevron.',
        'Does not open while disabled or loading; loading stays focusable.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
