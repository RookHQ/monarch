import { useMemo, useState } from 'react';
import {
  AiPromptInput,
  AiSlashCommands,
  useAiSlashCommands,
  type AiSlashCommand,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { CalendarIcon, ListIcon, PencilIcon, SearchIcon } from '../../../internal/docs-demo.js';

/** Built on demand: JSX at module scope would run when the docs index is read. */
const makeCommands = (): AiSlashCommand[] => [
  {
    id: 'summarize',
    name: 'summarize',
    description: 'Summarize the open document',
    icon: <ListIcon />,
    keywords: ['shorten', 'recap'],
  },
  {
    id: 'rewrite',
    name: 'rewrite',
    description: 'Rewrite the selection in a different tone',
    icon: <PencilIcon />,
  },
  {
    id: 'search',
    name: 'search',
    description: 'Search connected documents',
    icon: <SearchIcon />,
  },
  {
    id: 'schedule',
    name: 'schedule',
    description: 'Needs a calendar connection',
    icon: <CalendarIcon />,
    disabled: true,
  },
];

function SlashDemo() {
  const [sent, setSent] = useState('');
  const commands = useMemo(makeCommands, []);
  const slash = useAiSlashCommands(commands, (command) => `/${command.name} `);
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-2)',
        width: '100%',
        maxWidth: '40rem',
        paddingBlockStart: '14rem',
      }}
    >
      <AiPromptInput
        label="Message the assistant"
        placeholder="Type / for commands"
        onSubmit={(text) => setSent(text)}
        {...slash}
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

function RunOnPickDemo() {
  const [ran, setRan] = useState('');
  const commands = useMemo(makeCommands, []);
  const slash = useAiSlashCommands(commands, (command) => {
    // Nothing returned: the typed "/query" is removed and the command runs at once.
    setRan(command.name);
  });
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-2)',
        width: '100%',
        maxWidth: '40rem',
        paddingBlockStart: '14rem',
      }}
    >
      <AiPromptInput label="Message the assistant" onSubmit={() => {}} {...slash} />
      {ran ? <p style={{ margin: 0 }}>Ran: /{ran}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-slash-commands',
  title: 'AiSlashCommands',
  summary: 'The `/` command menu for AiPromptInput: filtered listbox, keyboard picking, one hook.',
  sections: {
    purpose: {
      summary:
        'Lets people run assistant commands by typing `/` in the prompt: a filtered list of commands with descriptions opens above the input and is picked with the keyboard or pointer. `useAiSlashCommands` does all the wiring on top of AiPromptInput’s trigger extension points.',
      useWhen: [
        'An assistant has a fixed set of commands (summarize, rewrite, search) people should discover from the prompt.',
        'Power users want to start a task without leaving the keyboard.',
      ],
      avoidWhen: [
        'Referencing documents, people or records: use the `@` menu from ai-mention-context.',
        'A handful of first-turn ideas: use AiStarterPrompts.',
        'Commands for the whole app: use a command palette.',
      ],
    },
    designPrinciples: {
      items: [
        'Focus never leaves the textarea: options are referenced with `aria-activedescendant`, so typing keeps filtering.',
        'Every command says what it does; disabled commands stay visible so people learn they exist.',
        'Predictable filtering: name prefix first, then names that contain the text, then descriptions and keywords.',
        'The product decides what a pick does: insert text, or run the command and clear the query.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAiSlashCommands(commands, onSelect, options?)` returns `{ triggers, renderTriggerMenu, textareaProps }`; spread it onto AiPromptInput. (`triggers` is an array because that is AiPromptInput’s prop.) If you pass your own `textareaProps`, call the hook’s `onKeyDown` from yours.',
        '`onSelect(command)` may return text to put in place of the typed `/query` (for example `"/summarize "`); return nothing to remove the query.',
        '`combineAiPromptMenus(slash, mentions)` merges this with other trigger menus (such as `useAiMentionContext`) into one set of props.',
        '`filterAiSlashCommands(commands, query)` is the ranking used by the hook.',
        'AiSlashCommands itself is the presentational listbox; render it directly only for a custom integration. `AI_SLASH_COMMANDS_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants. `options.char` changes the trigger (default `/`).'],
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
        { name: 'Empty', description: '"No commands match “/…”"; the listbox is not rendered.' },
        {
          name: 'Closed',
          description:
            'No menu; the textarea has no `aria-controls` or `aria-activedescendant`, and Enter sends.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing `/` at the start of a word opens the menu; the text after it filters.',
        'Clicking an option picks it; the pointer highlights as it moves. Focus and the caret stay in the textarea.',
        'A space, moving the caret away or Escape closes the menu (AiPromptInput’s trigger rules).',
        'After a pick the menu closes, the returned text replaces `/query`, and the caret goes after it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'Moves the highlight, skipping disabled commands and wrapping.',
        },
        { keys: 'Enter / Tab', action: 'Picks the highlighted command instead of sending.' },
        { keys: 'Escape', action: 'Closes the menu; the next Escape stops a running response.' },
      ],
      notes: [
        'When nothing can be picked (no match, or only disabled ones), Enter is left to the input and sends.',
      ],
    },
    accessibility: {
      role: 'listbox of options',
      name: '"Commands" (messages.label)',
      items: [
        'The textarea keeps its textbox role and gets `aria-autocomplete="list"`, `aria-controls` and `aria-activedescendant` while the menu is open (a `<textarea>` may not take the combobox role).',
        'Each option is named by its `/name` and described by its description.',
        'Disabled options have `aria-disabled="true"`.',
        'A polite status inside the menu reads the number of commands as the list changes.',
      ],
    },
    responsive: {
      items: [
        'The menu spans the input’s width, wraps long names and scrolls inside itself (at most 18rem or half the viewport).',
        'On coarse pointers each option is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Panel: `--mn-color-bg-raised`, `--mn-color-border-default`, `--mn-shadow-md`.',
        'Highlight: `--mn-color-ai-subtle`, icon in `--mn-color-ai-strong`. Names use `--mn-font-mono`.',
        'In forced colours the highlighted option gets a Highlight outline and disabled text uses GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Insert the command', stack: true, render: () => <SlashDemo /> },
        { title: 'Run on pick', stack: true, render: () => <RunOnPickDemo /> },
        {
          title: 'The listbox on its own',
          stack: true,
          render: () => (
            <AiSlashCommands commands={makeCommands()} activeId="rewrite" onSelect={() => {}} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write descriptions as what the command does to the person’s content.',
        'Keep names short, lower-case single words.',
        'Show commands that need setup as disabled with a description that says why.',
      ],
      dont: [
        'Do not open a dialog from a pick without saying so in the description.',
        'Do not use `/` commands for things that are not commands, such as mentions.',
        'Do not move focus into the menu.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-input', reason: 'The input whose trigger extension points it uses.' },
        { id: 'ai-mention-context', reason: 'The `@` menu, combinable with this one.' },
        { id: 'ai-starter-prompts', reason: 'Suggested first prompts.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        '`filterAiSlashCommands` ranking.',
        'Named listbox, option names and descriptions, disabled options ignore clicks.',
        'Through AiPromptInput: opening on `/`, filtering, Arrow keys, Enter and Tab picking, `aria-controls` and `aria-activedescendant`.',
        'Inserted text or removed query, Escape closing, Enter sending when nothing matches, pointer picking keeps focus.',
        '`combineAiPromptMenus` routing two triggers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
