import { Button, KeyboardShortcutsSheet, type KeyboardShortcut } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const shortcuts: KeyboardShortcut[] = [
  { keys: 'mod+k', label: 'Search and commands' },
  { keys: '?', label: 'Show this list' },
  { keys: 'c', label: 'Create task', group: 'Tasks' },
  { keys: ['j', 'arrowdown'], label: 'Next task', group: 'Tasks' },
  { keys: ['k', 'arrowup'], label: 'Previous task', group: 'Tasks' },
  { keys: 'x', label: 'Select the focused task', group: 'Tasks' },
  { keys: 'enter', label: 'Open the focused task', group: 'Tasks' },
  { keys: 'g i', label: 'Go to inbox', group: 'Navigation' },
  { keys: 'g p', label: 'Go to projects', group: 'Navigation' },
  { keys: 'escape', label: 'Close panel or clear selection', group: 'Navigation' },
];

export default {
  id: 'keyboard-shortcuts-sheet',
  title: 'KeyboardShortcutsSheet',
  summary:
    'The "?" list of an app’s keyboard shortcuts in a side sheet: grouped, filterable, platform-aware.',
  sections: {
    purpose: {
      summary:
        'Lists every keyboard shortcut an app supports in one place, opened with "?" or from a help menu. Keys show as ⌘ or Ctrl to match the platform and are read out by name. Needed by: Stride (Shortcuts), Chat (KeyboardShortcuts), Tickets (ShortcutsSheet).',
      useWhen: [
        'An app has global or list shortcuts that people should be able to discover.',
        'A settings or help page needs the same list (render it controlled from a button).',
      ],
      avoidWhen: [
        'One shortcut next to one control: show a Kbd in its tooltip or menu item.',
        'Running commands: use CommandPalette or QuickSwitcher.',
      ],
    },
    designPrinciples: {
      items: [
        'It lists, it does not bind: the product keeps its own key handlers; only the open hotkey lives here.',
        'One description of keys, everywhere: `mod+k` becomes ⌘K or Ctrl+K, `g i` becomes "G then I".',
        'Readable by ear: symbols are hidden from screen readers and replaced by names ("Command K").',
        'A sheet, not a modal over the work: it slides in from the side so the page stays in view.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`KeyboardShortcutsSheet` is controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`, `hotkey`). Its ref goes to the sheet element; other SheetContent props (`side`, `size`, …) pass through.',
        '`describeShortcutKeys(keys, { apple?, messages? })` returns the spoken form of a shortcut, for tooltips or `aria-keyshortcuts` helpers.',
        '`KEYBOARD_SHORTCUTS_SHEET_MESSAGES` holds the English defaults for `messages`.',
        'The open hotkey uses `useHotkey` from CommandPalette, so single keys never fire while typing in a field.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`keys` uses CommandPalette hotkey syntax: `+` joins a combination (`mod+shift+p`), a space separates the steps of a sequence (`g i`), and an array lists alternatives (`["j", "arrowdown"]`).',
        'Named keys: `enter`, `escape`, `space`, `tab`, `backspace`, `arrowup`, `arrowdown`, `arrowleft`, `arrowright`.',
        'Migrating from the app copies: Stride `SHORTCUTS: [keys, label][]`, Chat `{ keys, label }[]` and Tickets `{ group, items: [keys[], label][] }[]` all map to `{ keys, label, group? }[]`. Write `⌘ K` / `Mod K` as `mod+k`, `G then I` as `g i`, `J / K` as `["j", "k"]`, `Esc` as `escape`. Chat’s `Mod /` opener becomes `hotkey="mod+/"`. Keep the app’s own key handlers; replace only the dialog.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['right', 'left', 'top', 'bottom'],
          description: 'From Sheet. Default `right`.',
        },
        {
          prop: 'filterable',
          values: ['true', 'false'],
          description:
            'A filter field above the list. Default on when there are more than 8 shortcuts.',
        },
        {
          prop: 'apple',
          values: ['undefined', 'true', 'false'],
          description: 'Force Apple symbols or PC names; by default detected after mounting.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The sheet is shown; focus is inside it.',
        },
        { name: 'Filtered', description: 'Only matching shortcuts show; the count is announced.' },
        { name: 'No matches', description: 'The empty message shows and is announced.' },
      ],
    },
    interaction: {
      items: [
        'The hotkey (default "?") opens it from anywhere except text fields.',
        'Typing in the filter matches labels, group names and spoken key names ("command").',
        'Escape, the close button or an outside click closes it; focus returns to where it was.',
        'The filter clears each time it closes.',
      ],
    },
    keyboard: {
      items: [
        { keys: '?', action: 'Opens the sheet (configurable with `hotkey`).' },
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the filter and the close button; focus stays in the sheet.',
        },
        { keys: 'Escape', action: 'Closes the sheet.' },
      ],
    },
    accessibility: {
      role: 'dialog containing description lists (`dl`), one per group',
      name: 'The title ("Keyboard shortcuts").',
      items: [
        'Each group is a `section` named by its heading.',
        'Each shortcut is a term (what it does) and a definition (the keys).',
        'Key symbols are `aria-hidden`; a visually hidden text spells them out ("Command K", "G then I").',
        'Filter results are announced in a polite status region.',
        'Platform detection happens after mounting, so server and client markup match.',
      ],
    },
    responsive: {
      items: [
        'The sheet never exceeds the screen width; at 390px it fills the screen.',
        'Long labels wrap and the keys move under the label when there is no room.',
      ],
    },
    theme: {
      items: [
        'Keys use Kbd; rows are separated by the subtle border token.',
        'Forced colours: borders, group headings and joiners use CanvasText.',
        'The sheet’s slide-in respects reduced motion (from Sheet).',
      ],
    },
    examples: {
      items: [
        {
          title: 'From a help button',
          render: () => (
            <KeyboardShortcutsSheet
              shortcuts={shortcuts}
              trigger={<Button>Keyboard shortcuts</Button>}
              hotkey={false}
              description="Single-key shortcuts do not work while typing in a field."
            />
          ),
        },
        {
          title: 'Apple symbols, with a filter',
          render: () => (
            <KeyboardShortcutsSheet
              shortcuts={shortcuts}
              apple
              filterable
              hotkey={false}
              trigger={<Button>Shortcuts on macOS</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the list in one constant and use it for both the handlers and this sheet.',
        'Group shortcuts by where they work (Navigation, Lists, Editor).',
        'Mention in `description` when shortcuts do not work, such as while typing.',
      ],
      dont: [
        'Do not write platform-specific keys (⌘, Ctrl) yourself; use `mod`.',
        'Do not list shortcuts the app does not actually bind.',
        'Do not mount two sheets that both listen for "?".',
      ],
    },
    related: {
      items: [
        { id: 'kbd', reason: 'A single key or shortcut inline.' },
        { id: 'command-palette', reason: 'Running commands by name; shares the hotkey syntax.' },
        { id: 'sheet', reason: 'The side panel this is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['kbd', 'command-palette', 'search-input', 'sheet'],
    },
    testing: {
      unit: [
        'Opens from its trigger as a named, described dialog with ungrouped shortcuts first and named group sections; no axe violations; Escape returns focus.',
        'Shows ⌘ or Ctrl to match `apple`; spoken text says "Command K", "G then I", "J or Down arrow".',
        'The "?" hotkey opens it, except while typing in a field; a custom hotkey and `hotkey={false}` work.',
        'Filters by label, group and spoken key name; announces the count and the empty state.',
        'Short lists hide the filter; strings translate; `describeShortcutKeys` output.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
