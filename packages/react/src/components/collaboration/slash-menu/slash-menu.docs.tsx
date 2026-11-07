import { useMemo, useState } from 'react';
import { Field, FieldLabel, Textarea } from '@rookcom/monarch-react';
import { SlashMenu, useSlashMenu, type SlashMenuItem } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import {
  CalendarIcon,
  GridIcon,
  ListIcon,
  PencilIcon,
  UsersIcon,
} from '../../../internal/docs-demo.js';

const GROUPS = ['Text', 'Lists', 'Insert'];

/** Built on demand: JSX at module scope would run when the docs index is read. */
const makeItems = (): SlashMenuItem[] => [
  {
    id: 'heading',
    label: 'Heading',
    description: 'Large section title',
    icon: <PencilIcon />,
    group: 'Text',
    keywords: ['title', 'h2'],
  },
  {
    id: 'quote',
    label: 'Quote',
    description: 'Indented quotation',
    icon: <PencilIcon />,
    group: 'Text',
  },
  {
    id: 'bullets',
    label: 'Bulleted list',
    description: 'A simple list',
    icon: <ListIcon />,
    group: 'Lists',
  },
  {
    id: 'checklist',
    label: 'Checklist',
    description: 'Items to tick off',
    icon: <ListIcon />,
    group: 'Lists',
    keywords: ['todo', 'task'],
  },
  {
    id: 'mention',
    label: 'Mention someone',
    description: 'Notify a teammate',
    icon: <UsersIcon />,
    group: 'Insert',
  },
  {
    id: 'date',
    label: 'Date',
    description: 'Insert today’s date',
    icon: <CalendarIcon />,
    group: 'Insert',
  },
  {
    id: 'table',
    label: 'Table',
    description: 'Not available in replies',
    icon: <GridIcon />,
    group: 'Insert',
    disabled: true,
  },
];

const MARKDOWN: Record<string, string> = {
  heading: '## ',
  quote: '> ',
  bullets: '- ',
  checklist: '- [ ] ',
  mention: '@',
};

function ComposerDemo() {
  const items = useMemo(makeItems, []);
  const [value, setValue] = useState('');
  const slash = useSlashMenu({
    items,
    value,
    onChange: setValue,
    groupOrder: GROUPS,
    onSelect: (item) =>
      item.id === 'date' ? new Date(2026, 0, 15).toDateString() + ' ' : MARKDOWN[item.id],
  });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '32rem' }}>
      {slash.open ? <SlashMenu {...slash.menuProps} /> : null}
      <Field>
        <FieldLabel>Message</FieldLabel>
        <Textarea {...slash.textareaProps} placeholder="Type / to insert a block" />
      </Field>
    </div>
  );
}

export default {
  id: 'slash-menu',
  title: 'SlashMenu',
  summary: 'The grouped `/` insert menu for composers and editors, with one hook for a textarea.',
  sections: {
    purpose: {
      summary:
        'Lets people insert blocks (headings, lists, quotes, mentions, embeds) by typing `/` at the start of a line. A grouped, filtered list opens next to the text field and is picked with the keyboard or pointer, while focus stays in the field. It behaves exactly like AiSlashCommands: same filter ranking, keys and ARIA. Needed by: Chat (composer block menu), Dossier (editor slash menu).',
      useWhen: [
        'A message composer or document editor offers many kinds of block to insert.',
        'Keyboard users should be able to format without leaving the text.',
      ],
      avoidWhen: [
        'Commands for an AI assistant prompt: use AiSlashCommands.',
        'Mentioning people or channels: use MentionPicker.',
        'App-wide navigation and actions: use a command palette.',
      ],
    },
    designPrinciples: {
      items: [
        'Focus never leaves the text field; the highlighted option is referenced with `aria-activedescendant`.',
        'Grouped for scanning, but one flat order for the arrow keys, so they cross group boundaries in the order shown.',
        'Hover only highlights; a click or Enter picks, so pointer and keyboard never disagree about what Enter will do.',
        'The product decides what a pick does: return text to insert, or run an action and return nothing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useSlashMenu({ items, value, onChange, onSelect, groupOrder? })` returns `{ open, query, textareaProps, menuProps, openMenu, close }`. Spread `textareaProps` on the `<textarea>` and render `<SlashMenu {...menuProps} />` while `open`.',
        '`onSelect(item)` may return text to put in place of the typed `/query`; return nothing to remove it.',
        '`useSlashMenuKeys({ items, onSelect, onClose?, groupOrder? })` is the highlight and key handling on its own, for a rich-text editor whose suggestion plugin detects the trigger. Its `handleKeyDown(event)` returns true when it used the key.',
        '`filterSlashMenuItems`, `groupSlashMenuItems`, `findSlashQuery` and `slashMenuOptionId` are the helpers the hook uses. `SLASH_MENU_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Chat `blocks/SlashMenu`, Dossier `document/SlashMenu`): `items` (BlockCommand / SlashCommand) become `SlashMenuItem` with `title` → `label` and the icon component rendered as an element (`icon: <ListIcon />`); the hard-coded `GROUP_ORDER` becomes `groupOrder`; `active` (a number) becomes `activeId`; `onActive(index)` becomes `onActiveChange(id)`; `onPick` / `command` become `onSelect`. Chat’s `slashQuery`, `filterCommands`, `orderForDisplay` and `listNav` are replaced by `findSlashQuery`, `filterSlashMenuItems`, `groupSlashMenuItems` and `useSlashMenuKeys`. Dossier’s `SlashMenuHandle.onKeyDown` becomes `useSlashMenuKeys().handleKeyDown`. Running the block (inserting Markdown, opening a picker) stays in the product’s `onSelect`.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. `char` changes the trigger and `position: "word-start"` lets it open after a space as well as at the start of a line.',
      ],
    },
    states: {
      items: [
        {
          name: 'Highlighted',
          attribute: 'data-highlighted on the option',
          description: 'The option Enter would pick; `aria-selected="true"`.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled on the option',
          description: 'Shown and skipped by the arrow keys; cannot be picked.',
        },
        { name: 'Empty', description: '"Nothing matches “/…”"; the listbox is not rendered.' },
        {
          name: 'Closed',
          description:
            'No menu; the textarea has no `aria-controls` or `aria-activedescendant`, and keys go to your own `onKeyDown`.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing `/` at the start of a line opens the menu; letters, digits and hyphens after it filter.',
        'A space, a selection, moving the caret away, leaving the field or Escape closes it.',
        'After Escape the menu stays closed until the query changes.',
        'A pick replaces `/query` with whatever `onSelect` returns and puts the caret after it.',
        '`openMenu()` inserts the trigger at the caret and opens the menu, for an "Insert" toolbar button.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'Moves the highlight across groups, skipping disabled items and wrapping.',
        },
        { keys: 'Enter / Tab', action: 'Picks the highlighted item.' },
        { keys: 'Escape', action: 'Closes the menu and leaves the typed text.' },
      ],
      notes: [
        'When nothing can be picked, Enter and Tab are left to the field (and your `onKeyDown`).',
        'Keys pressed during IME composition are ignored.',
      ],
    },
    accessibility: {
      role: 'listbox of options, grouped with role="group"',
      name: '"Insert block" (messages.label); each group by its heading',
      items: [
        'The textarea keeps its textbox role and gets `aria-autocomplete="list"`, `aria-controls` and `aria-activedescendant` while the menu has options.',
        'Each option is named by its label and described by its description.',
        'Disabled options have `aria-disabled="true"`.',
        'A polite status inside the menu reads the number of options as the list changes.',
      ],
    },
    responsive: {
      items: [
        'The menu is at most 22rem wide and fills narrower containers, so it fits at 390px.',
        'It scrolls inside itself (at most 20rem or half the viewport) and keeps the highlighted option in view.',
        'On coarse pointers each option is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Panel: `--mn-color-bg-raised`, `--mn-color-border-default`, `--mn-shadow-md`.',
        'Highlight: `--mn-color-accent-subtle`, icon in `--mn-color-accent-strong` (AiSlashCommands uses the AI colours instead).',
        'In forced colours the highlighted option gets a Highlight outline and disabled text uses GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Composer with a block menu', stack: true, render: () => <ComposerDemo /> },
        {
          title: 'The listbox on its own',
          render: () => (
            <SlashMenu
              items={makeItems()}
              groupOrder={GROUPS}
              activeId="checklist"
              onSelect={() => {}}
            />
          ),
        },
        {
          title: 'No matches',
          render: () => <SlashMenu items={[]} query="chart" onSelect={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give every item a short description of what it inserts.',
        'Keep groups few and in a stable order with `groupOrder`.',
        'Show items that do not apply here as disabled, with a description that says why.',
      ],
      dont: [
        'Do not move focus into the menu.',
        'Do not let hovering pick an item.',
        'Do not open the menu in the middle of a word; paths and fractions contain slashes.',
      ],
    },
    related: {
      items: [
        { id: 'ai-slash-commands', reason: 'The same menu for AI prompt commands.' },
        { id: 'mention-picker', reason: 'The `@` suggestion list for people and channels.' },
        { id: 'message-composer', reason: 'The composer it usually sits in.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Filter ranking matches AiSlashCommands; grouping order; slash detection at line start and word start.',
        'Named listbox, labelled groups, option names and descriptions, disabled options ignore clicks.',
        'Through a textarea: opening on `/`, filtering, Arrow keys across groups with wrapping, Enter and Tab picking, inserted text and caret focus.',
        'Escape closes until the query changes; no menu mid-line; `openMenu` inserts the trigger.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
