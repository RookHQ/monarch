import { Button } from '@rookcom/monarch-react';
import { QuickSwitcher, type QuickSwitcherDestination } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { InboxIcon, UsersIcon } from '../../../internal/docs-demo.js';

const destinations: QuickSwitcherDestination[] = [
  { id: 'c-general', label: '#general', group: 'Channels', href: '#general', icon: <InboxIcon /> },
  {
    id: 'c-design',
    label: '#design-review',
    group: 'Channels',
    href: '#design',
    icon: <InboxIcon />,
  },
  {
    id: 'c-launch',
    label: '#launch-plan',
    group: 'Channels',
    href: '#launch',
    icon: <InboxIcon />,
  },
  {
    id: 'p-ana',
    label: 'Ana Silva',
    description: 'ana@example.com',
    group: 'People',
    href: '#ana',
    icon: <UsersIcon />,
  },
  {
    id: 'p-kofi',
    label: 'Kofi Mensah',
    description: 'kofi@example.com',
    group: 'People',
    href: '#kofi',
    icon: <UsersIcon />,
  },
];

export default {
  id: 'quick-switcher',
  title: 'QuickSwitcher',
  summary:
    '⌘K "jump to" switcher: type part of a name, press Enter, go there. Built on CommandPalette.',
  sections: {
    purpose: {
      summary:
        'Lets people jump straight to a conversation, project, person or document by typing a few letters. A thin, documented layer over CommandPalette with jump-to defaults; the product supplies the destinations and does the routing. Needed by: Chat (QuickSwitcher); any app with many named places.',
      useWhen: [
        'People move between many named places (channels, projects, documents) and know their names.',
        'A "search everywhere" fallback should sit under the instant matches.',
      ],
      avoidWhen: [
        'Running actions such as "Create issue": use CommandPalette, which supports commands, pages and shortcuts.',
        'Filtering one list or table: use SearchInput.',
      ],
    },
    designPrinciples: {
      items: [
        'Go, don’t act: every row is a destination; actions belong in CommandPalette.',
        'Forgiving matching: whole words rank first, but "gnl" still finds "general".',
        'Recent first: with an empty query it shows where the person was last.',
        'Data and routing stay in the product: `onNavigate` receives the destination.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Everything CommandPalette does applies: combobox and listbox pattern, focus trap, async `sources` with debounce, cancellation and retry, announcements.',
        '`quickSwitcherFilter(item, query)` is the default ranking, exported for reuse.',
        '`QUICK_SWITCHER_MESSAGES` holds the jump-to defaults; other strings fall back to CommandPalette’s.',
        'The ref goes to the dialog element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`destinations` and `recent` are `{ id, label, description?, icon?, group?, keywords?, href?, disabled? }[]`.',
        'Results from `sources` reach `onNavigate` as `{ id, label, description? }`; look them up by id or give each result its own `onSelect`.',
        'Migrating from the app copy (Chat `components/chat/QuickSwitcher`): map conversations to `destinations` (and the first ten to `recent`), turn `unifiedSearch` and task search into `sources` with one group each, route in `onNavigate` with the `href`, and replace the "Search messages for…" row with `onSearchAll`. Open state stays in the product store via `open` + `onOpenChange`; the ⌘K binding is built in (`hotkey`).',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants: it renders a CommandPalette with the `mn-quick-switcher` class.',
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The dialog is shown; focus is in the input.',
        },
        { name: 'Recent', description: 'Empty query: the "Recent" group shows `recent`.' },
        { name: 'Loading', description: 'A source is searching; announced by CommandPalette.' },
        {
          name: 'No matches',
          description: 'Nothing matches; `messages.empty` or `emptyState` shows.',
        },
      ],
    },
    interaction: {
      items: [
        '⌘K or Ctrl+K opens and closes it (configurable with `hotkey`).',
        'Typing filters destinations instantly and starts `sources` after their debounce.',
        'Enter or a click calls `onNavigate` and closes it; the "Search for …" row calls `onSearchAll` instead.',
        'Every opening starts with an empty query.',
      ],
    },
    keyboard: {
      items: [
        { keys: '⌘K / Ctrl+K', action: 'Opens or closes the switcher.' },
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'Moves through the results, skipping disabled ones and wrapping.',
        },
        { keys: 'Enter', action: 'Goes to the highlighted destination.' },
        { keys: 'Escape', action: 'Closes the switcher and returns focus.' },
      ],
    },
    accessibility: {
      role: 'dialog containing a combobox and a grouped listbox',
      name: '`messages.title` ("Jump to"), visually hidden.',
      items: [
        'Focus stays in the input; the highlighted row is exposed with `aria-activedescendant`.',
        'Groups are named by their headings ("Recent", "Channels", "People").',
        'Result counts and loading are announced in a polite live region (from CommandPalette).',
        'Icons and avatars are decorative; the label carries the name.',
      ],
    },
    responsive: {
      items: [
        'The dialog is at most the viewport width minus 2rem, so it fits 390px; long names truncate.',
      ],
    },
    theme: {
      items: [
        'Uses CommandPalette’s tokens and styles; avatar images in the icon slot are rounded.',
        'Forced colours: avatar images get a CanvasText outline.',
        'The open animation respects reduced motion (from CommandPalette).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Jump to a channel or person',
          render: () => (
            <QuickSwitcher
              destinations={destinations}
              recent={destinations.slice(0, 2)}
              hotkey={false}
              trigger={<Button>Jump to…</Button>}
              onNavigate={() => {}}
            />
          ),
        },
        {
          title: 'With "search everywhere"',
          render: () => (
            <QuickSwitcher
              destinations={destinations}
              hotkey={false}
              trigger={<Button>Search</Button>}
              onNavigate={() => {}}
              onSearchAll={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass only destinations the person can open.',
        'Keep `recent` short (about ten) and ordered by last visit.',
        'Use one `source` per kind of remote result so each gets its own group.',
      ],
      dont: [
        'Do not mix commands into the switcher; use CommandPalette.',
        'Do not mount both a QuickSwitcher and a CommandPalette on ⌘K; give one another hotkey.',
        'Do not fetch on every keystroke yourself; use `sources`.',
      ],
    },
    related: {
      items: [
        {
          id: 'command-palette',
          reason: 'Commands, nested pages and shortcuts; the switcher is built on it.',
        },
        { id: 'search-input', reason: 'Filtering a single list in the page.' },
        { id: 'keyboard-shortcuts-sheet', reason: 'Listing the app’s shortcuts, including ⌘K.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['command-palette'],
    },
    testing: {
      unit: [
        'Opens with Ctrl+K as a "Jump to" dialog with focus in the combobox and a Recent group; no axe violations.',
        'Loose letter matching ("gnl" → "#general"); Enter calls `onNavigate` and closes.',
        'Descriptions and keywords match.',
        '"Search for …" appears from two characters and calls `onSearchAll`.',
        'Source results reach `onNavigate`.',
        'Trigger with the hotkey off; translated strings; filter ranking.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
