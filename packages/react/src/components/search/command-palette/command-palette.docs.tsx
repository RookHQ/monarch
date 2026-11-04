import { useState } from 'react';
import { Button, Kbd } from '@rookcom/monarch-react';
import {
  CommandPalette,
  formatHotkey,
  type CommandPaletteItem,
  type CommandPaletteSource,
} from '@rookcom/monarch-react/data';
import {
  BoardIcon,
  ListIcon,
  PencilIcon,
  PlusIcon,
  TrashIcon,
} from '../../../internal/docs-demo.js';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Built lazily: docs files cannot create JSX at module scope. */
const commands = (): CommandPaletteItem[] => [
  { id: 'new', label: 'Create task', group: 'Actions', icon: <PlusIcon />, shortcut: 'c' },
  { id: 'rename', label: 'Rename', group: 'Actions', icon: <PencilIcon />, shortcut: 'f2' },
  {
    id: 'archive',
    label: 'Archive',
    group: 'Actions',
    icon: <TrashIcon />,
    keywords: ['delete', 'remove'],
    tone: 'danger',
  },
  { id: 'board', label: 'Board', group: 'Go to', icon: <BoardIcon />, shortcut: ['G', 'B'] },
  { id: 'list', label: 'My tasks', group: 'Go to', icon: <ListIcon />, shortcut: ['G', 'M'] },
  {
    id: 'settings',
    label: 'Settings',
    group: 'Go to',
    description: 'Workspace',
    shortcut: 'mod+,',
  },
];

const RECORDS = [
  'Quarterly planning notes',
  'Onboarding checklist',
  'Release checklist',
  'Pricing page copy',
  'Support rota',
  'Quarterly report draft',
];

/** A stand-in for an app's search API: filters sample titles after a short delay. */
const DOCUMENTS: CommandPaletteSource = {
  id: 'documents',
  group: 'Documents',
  minQueryLength: 2,
  search: (query, signal) =>
    new Promise((resolve, reject) => {
      const t = setTimeout(() => {
        resolve(
          RECORDS.filter((r) => r.toLowerCase().includes(query.toLowerCase())).map((r) => ({
            id: r,
            label: r,
            icon: <ListIcon />,
          })),
        );
      }, 350);
      signal.addEventListener('abort', () => {
        clearTimeout(t);
        reject(new DOMException('Aborted', 'AbortError'));
      });
    }),
};

function GroupsDemo() {
  const [last, setLast] = useState<string | null>(null);
  return (
    <>
      <CommandPalette
        items={commands()}
        trigger={
          <Button variant="secondary">
            Search commands
            {formatHotkey('mod+k').map((k) => (
              <Kbd key={k}>{k}</Kbd>
            ))}
          </Button>
        }
        onItemSelect={(item) => setLast(item.label)}
      />
      <span>{last ? `Ran: ${last}` : 'Press ⌘K or Ctrl+K anywhere on this page.'}</span>
    </>
  );
}

function RecentDemo() {
  const [recent, setRecent] = useState<CommandPaletteItem[]>([
    { id: 'Support rota', label: 'Support rota', icon: <ListIcon /> },
  ]);
  return (
    <CommandPalette
      hotkey={false}
      items={commands()}
      sources={[DOCUMENTS]}
      recent={recent}
      onItemSelect={(item, { page }) => {
        if (page === null && item.group === undefined)
          setRecent((r) => [item, ...r.filter((x) => x.id !== item.id)].slice(0, 5));
      }}
      trigger={<Button variant="secondary">Search documents</Button>}
    />
  );
}

function PagesDemo() {
  const [project, setProject] = useState('Website');
  const projects = ['Website', 'Mobile app', 'Design system', 'Internal tools'];
  return (
    <>
      <CommandPalette
        hotkey={false}
        items={[
          { id: 'move', label: 'Move to…', group: 'Task', page: 'move', shortcut: 'm' },
          { id: 'rename', label: 'Rename', group: 'Task', icon: <PencilIcon /> },
        ]}
        pages={{
          move: {
            title: 'Move to',
            placeholder: 'Choose a project…',
            items: projects.map((p) => ({
              id: p,
              label: p,
              disabled: p === project,
              onSelect: () => setProject(p),
            })),
          },
        }}
        trigger={<Button variant="secondary">Task actions</Button>}
      />
      <span>Project: {project}</span>
    </>
  );
}

export default {
  id: 'command-palette',
  title: 'CommandPalette',
  summary:
    'A ⌘K / Ctrl+K palette: grouped commands, recent items, async search, nested pages and shortcut hints.',
  sections: {
    purpose: {
      summary:
        'Lets keyboard users jump anywhere, run any action or find any record from one search field, without leaving the keyboard. Import from `@rookcom/monarch-react/data`.',
      useWhen: [
        'An app-wide “go to / do / find” launcher opened with ⌘K or Ctrl+K.',
        'A quick switcher over records the app searches on the server, grouped by type.',
        'A short chain of choices from the keyboard, such as “Move to…” then a project (nested pages).',
      ],
      avoidWhen: [
        'Picking one value for a form field: use Combobox.',
        'Filtering the list on the page: use SearchInput or FilterBar.',
        'The only way to reach an action: every command must also exist in the visible interface.',
      ],
    },
    designPrinciples: {
      items: [
        'The app owns the data. Commands, recent items and searches are passed in; Monarch owns keyboard, focus, filtering, debouncing, cancellation and announcements.',
        'One field, one list: the input keeps focus and the active result moves with `aria-activedescendant`, so typing never stops.',
        'Results are grouped by kind (“Actions”, “Go to”, “Documents”) with quiet headings; the active row is a muted fill, danger commands are red.',
        'Shortcut hints sit at the end of each row in Kbd, so the palette teaches the shortcuts it lists.',
        'Old results stay while new ones load, so the list does not flicker; a spinner replaces the search icon.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Monarch Dialog (Radix Dialog): focus is trapped, the page behind is inert, and focus returns to the trigger on close. Without a `trigger`, focus returns to where it was.',
        '`items`, `recent` and page `items` are `CommandPaletteItem`s: `{ id, label, description?, icon?, group?, keywords?, shortcut?, page?, disabled?, tone?, keepOpen?, onSelect? }`.',
        '`sources` are `CommandPaletteSource`s: `{ id, group, search(query, signal), minQueryLength?, debounceMs? }`. Each source is debounced and aborted on its own.',
        '`pages` maps a key to `{ title, placeholder?, items?, sources? }`; an item with `page` opens it.',
        '`useHotkey(hotkey, handler, { enabled, inFields })`, `formatHotkey(hotkey)`, `matchesHotkey(event, hotkey)` and `isTypingTarget(target)` are exported for the app’s own shortcuts and shortcut hints.',
        '`defaultCommandFilter(item, query)` is the default ranking, for apps that wrap it.',
        'App-wide shortcut tables: compose `useHotkey`; a registry is not part of Monarch yet.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'item.tone',
          values: ['danger'],
          description:
            'A destructive command (`data-tone="danger"`): red text and a red-tinted active fill.',
        },
        {
          prop: 'item.page',
          values: ['string'],
          description:
            'The item opens a nested page. A chevron marks it; the page title appears as a chip before the input.',
        },
        {
          prop: 'footer',
          values: ['default hints', 'ReactNode', 'null'],
          description:
            'The bottom bar. Default: Kbd hints for ↑↓, ↵ and Esc. Hidden on phones when it only holds the default hints.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active result',
          attribute: 'aria-selected="true", data-highlighted',
          description: 'The result Enter would run; muted fill (red tint for danger).',
        },
        {
          name: 'Disabled result',
          attribute: 'aria-disabled="true"',
          description: 'Shown in the disabled colour, skipped by the arrow keys, not clickable.',
        },
        {
          name: 'Loading',
          description:
            'A source is searching: the search icon becomes a spinner and “Searching…” is announced. Previous results stay.',
        },
        {
          name: 'No results',
          description:
            'The query matches nothing: `messages.empty` or `emptyState` is shown and announced.',
        },
        {
          name: 'Source error',
          description:
            'A source rejected: “Could not load documents.” with a Retry button, in the danger colour.',
        },
        {
          name: 'Nested page',
          attribute: 'data-page="<key>"',
          description:
            'The page title chip shows before the input; the input is renamed “Command palette: <page>”.',
        },
      ],
    },
    interaction: {
      items: [
        'The hotkey (default `mod+k`: ⌘K on Apple platforms, Ctrl+K elsewhere) toggles the palette from anywhere. Pass an array for alternatives such as `["mod+k", "/"]`, or `false` to bind it yourself.',
        'Single-key hotkeys such as `/` never fire while typing in a field or contenteditable; modified ones do unless `hotkeyInFields={false}`. Keys during IME composition are ignored.',
        'Every open starts on the first page with an empty query. With an empty query the `recent` group comes first.',
        'Typing filters `items` with the default ranking (every word must match label, description or keywords; label prefixes first) or your `filter`. Groups keep the order their first item appears in; ungrouped items come first.',
        'Each source waits `debounceMs` (200) after the last keystroke, searches once the trimmed query is `minQueryLength` (1) long, and aborts its request when the query, page or open state changes.',
        'Choosing an item runs its `onSelect`, then `onItemSelect`, then opens its page, or closes the palette unless `keepOpen`.',
        'Pointer: moving over a row makes it active; clicking runs it. Rows never take focus.',
      ],
    },
    keyboard: {
      items: [
        { keys: '⌘K / Ctrl+K (configurable)', action: 'Opens or closes the palette.' },
        {
          keys: 'ArrowDown / ArrowUp',
          action: 'Moves the active result, skipping disabled ones and wrapping at the ends.',
        },
        {
          keys: 'PageDown / PageUp',
          action: 'Moves the active result five rows, stopping at the ends.',
        },
        { keys: 'Enter', action: 'Runs the active result, or opens its nested page.' },
        { keys: 'Backspace (empty query)', action: 'Leaves the nested page.' },
        {
          keys: 'Escape',
          action: 'Leaves the nested page; on the first page, closes the palette.',
        },
        {
          keys: 'Tab',
          action:
            'Moves between the input and any page chip or Retry button; focus stays inside the dialog.',
        },
      ],
      notes: [
        'Home and End stay with the input (caret to start or end), as in the APG combobox pattern.',
      ],
    },
    accessibility: {
      role: 'dialog containing combobox + listbox (option, group)',
      name: 'The dialog and input are named by `messages.title` (“Command palette”); the listbox by `messages.results`; each group by its heading.',
      items: [
        'APG combobox with listbox popup: the input has `aria-expanded="true"`, `aria-controls` and `aria-activedescendant`; the active option has `aria-selected="true"`.',
        'Result count, “No results” and “Searching…” are announced through a polite live region after results settle.',
        'Shortcut hints and icons are `aria-hidden`, so an option’s name is its label and description.',
        'The default footer is `aria-hidden`: it repeats the keyboard model for sighted users only.',
        'Verified with axe in jsdom (open, empty and nested-page states) and in Chromium, Firefox and WebKit on /components/command-palette.',
      ],
    },
    responsive: {
      items: [
        'Up to 40rem wide, near the top of the viewport, capped at 34rem tall; the result list scrolls.',
        'Below 640px it fills the screen (Dialog `fullScreenOnPhone`), keeping clear of notches, and hides shortcut hints and the default footer.',
        'Rows grow to `--mn-control-lg` on coarse pointers so they are easy to tap.',
      ],
    },
    theme: {
      items: [
        'Monarch semantic tokens only: the raised surface and border of Dialog, `--mn-color-bg-muted` for the active row, `--mn-color-danger-*` for danger rows.',
        'In forced-colours mode the active row gets a Highlight outline.',
        'The entrance animation is removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Commands, groups and shortcut hints',
          render: () => <GroupsDemo />,
        },
        {
          title: 'Recent items and async search',
          render: () => <RecentDemo />,
        },
        {
          title: 'Nested page: Move to…',
          render: () => <PagesDemo />,
        },
        {
          title: 'App shortcuts with useHotkey',
          render: () => (
            <p>
              <code>useHotkey</code> binds the same hotkey strings the palette displays, with the
              same rule for text fields.
            </p>
          ),
          code: `useHotkey('c', () => openCreateTask());            // ignored while typing
useHotkey(['mod+k', '/'], () => setPaletteOpen(true));
<Kbd>{formatHotkey('mod+shift+p').join('')}</Kbd> // ⌘⇧P or CtrlShiftP`,
        },
      ],
    },
    doDont: {
      do: [
        'Put every command in the visible interface too; the palette is a shortcut, not the only path.',
        'Group results by kind and keep labels short verbs or nouns (“Create task”, “Settings”).',
        'Abort your request with the given `signal` so stale responses never cost bandwidth.',
        'Keep recent items yourself from `onItemSelect`, guarding storage access with try/catch.',
      ],
      dont: [
        'Do not bind a single letter as the palette hotkey without checking it clashes with nothing; it already never fires while typing.',
        'Do not render more than one palette with the same hotkey on a page.',
        'Do not show shortcut hints for shortcuts the app does not actually bind.',
        'Do not put product data or API calls in Monarch: pass them in as items and sources.',
      ],
    },
    related: {
      items: [
        {
          id: 'combobox',
          reason: 'Picks a value for a form field; the palette runs commands and navigates.',
        },
        { id: 'dialog', reason: 'The modal the palette is built on.' },
        { id: 'kbd', reason: 'Draws the shortcut hints and footer keys.' },
        { id: 'search-input', reason: 'Filters the list on the current page instead.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['dialog', 'kbd', 'spinner'],
    },
    testing: {
      unit: [
        'The hotkey opens it; dialog, combobox, listbox and named groups are wired (aria-controls, aria-activedescendant, aria-selected); axe in open, empty and nested-page states.',
        'Arrow keys skip disabled results and wrap; Enter runs `onSelect` and `onItemSelect` and closes; PageUp/PageDown jump; `keepOpen` keeps it open; pointer hover moves the active row.',
        'Filtering by label, description and keywords with ranking; empty state announced.',
        'Recent group shows only with an empty query.',
        'Shortcut hints render as Kbd and stay out of the option name; default footer hints.',
        'Nested pages: placeholder, renamed input, back chip; Backspace and Escape leave the page before Escape closes; selections report their page.',
        'Async sources: minimum length, one debounced request per settled query, stale results kept while loading, the previous request aborted on new input and on close, error with Retry.',
        'Hotkey: single keys ignored in fields, modified keys work in fields unless `hotkeyInFields={false}`, `hotkey={false}` turns it off, it toggles closed.',
        'Controlled `open` and `search`, a `trigger`, and a fresh query on reopen.',
        'Helpers: `formatHotkey` per platform, `matchesHotkey` with symbols and Shift, `isTypingTarget`, `defaultCommandFilter` ranking and accent folding.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/command-palette, plus open-state shots at 1280 and 390',
        'Cross-browser axe: /components/command-palette',
      ],
    },
  },
} satisfies ComponentDocs;
