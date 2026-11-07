import { useState } from 'react';
import {
  AiContextChips,
  AiMentionContext,
  AiPromptInput,
  aiMentionToContextChip,
  combineAiPromptMenus,
  useAiMentionContext,
  useAiSlashCommands,
  type AiContextChip,
  type AiMentionItem,
  type AiMentionSearch,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DIRECTORY: AiMentionItem[] = [
  { id: 'doc-plan', kind: 'doc', label: 'Quarterly plan', description: 'Planning', tokens: 4200 },
  { id: 'doc-notes', kind: 'doc', label: 'Launch notes', description: 'Product', tokens: 1800 },
  { id: 'p-ana', kind: 'person', label: 'Ana Lopez', description: 'Design lead', tokens: 120 },
  { id: 'p-sam', kind: 'person', label: 'Sam Okafor', description: 'Engineering', tokens: 110 },
  { id: 'r-1042', kind: 'record', label: 'Account 1042', description: 'Customer', tokens: 300 },
  { id: 'app-cal', kind: 'app', label: 'Calendar', description: 'Next 14 days', tokens: 900 },
  {
    id: 'doc-board',
    kind: 'doc',
    label: 'Board minutes',
    description: 'No access for the assistant',
    disabled: true,
  },
];

const wait = (ms: number, signal: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    const t = window.setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      window.clearTimeout(t);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });

// A stand-in for a server search: 300 ms latency, aborted as the person keeps typing.
const search: AiMentionSearch = async (query, { signal }) => {
  await wait(300, signal);
  const q = query.toLowerCase();
  return DIRECTORY.filter((i) => i.label.toLowerCase().includes(q));
};

function MentionDemo() {
  const [context, setContext] = useState<AiContextChip[]>([
    { id: 'page', kind: 'page', label: 'Current page', removable: false, tokens: 650 },
  ]);
  const [sent, setSent] = useState('');
  const mention = useAiMentionContext({
    search,
    exclude: context.map((c) => c.id),
    onSelect: (item) => {
      setContext((list) => [...list, aiMentionToContextChip(item)]);
    },
  });
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-2)',
        width: '100%',
        maxWidth: '40rem',
        paddingBlockStart: '16rem',
      }}
    >
      <AiContextChips
        items={context}
        onRemove={(id) => setContext((list) => list.filter((c) => c.id !== id))}
      />
      <AiPromptInput
        label="Message the assistant"
        placeholder="Type @ to add a document, person, record or app"
        onSubmit={(text) => setSent(`${text} (with ${context.length} context items)`)}
        {...mention}
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

function CombinedDemo() {
  const [context, setContext] = useState<AiContextChip[]>([]);
  const mention = useAiMentionContext({
    search,
    exclude: context.map((c) => c.id),
    onSelect: (item) => setContext((list) => [...list, aiMentionToContextChip(item)]),
  });
  const slash = useAiSlashCommands(
    [
      { id: 'summarize', name: 'summarize', description: 'Summarize the context' },
      { id: 'compare', name: 'compare', description: 'Compare two documents' },
    ],
    (command) => `/${command.name} `,
  );
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-2)',
        width: '100%',
        maxWidth: '40rem',
        paddingBlockStart: '16rem',
      }}
    >
      <AiContextChips
        items={context}
        onRemove={(id) => setContext((list) => list.filter((c) => c.id !== id))}
      />
      <AiPromptInput
        label="Message the assistant"
        placeholder="Type / for commands or @ for context"
        onSubmit={() => {}}
        {...combineAiPromptMenus(slash, mention)}
      />
    </div>
  );
}

export default {
  id: 'ai-mention-context',
  title: 'AiMentionContext',
  summary: 'The `@` menu that adds documents, people, records and apps to the AI’s context.',
  sections: {
    purpose: {
      summary:
        'Lets people point the assistant at exactly what it should read: typing `@` in the prompt searches documents, people, records and apps, and picking one adds it as a context chip. `useAiMentionContext` wires the search, keys and insertion into AiPromptInput.',
      useWhen: [
        'An assistant can read workspace content and people should choose which.',
        'Answers improve with explicit grounding ("compare @Quarterly plan with @Launch notes").',
      ],
      avoidWhen: [
        'Notifying a person in a message to other people: use mention-picker in the composer.',
        'Commands: use ai-slash-commands.',
        'Choosing the whole knowledge base an assistant uses: use AiKnowledgeScope.',
      ],
    },
    designPrinciples: {
      items: [
        'Mentions produce context, not text: a pick becomes an AiContextChips chip the person can see and remove.',
        'The same keyboard model as the mention picker: focus stays in the prompt, arrows move, Enter or Tab pick.',
        'Results are grouped by kind with visible headings, and each shows its token weight.',
        'Searching is debounced and aborted as people type, with honest loading, empty and error states.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAiMentionContext({ search, onSelect, exclude?, char?, debounceMs?, messages?, locale? })` returns `{ triggers, renderTriggerMenu, textareaProps }`; spread it onto AiPromptInput, or merge it with slash commands via `combineAiPromptMenus`.',
        '`search(query, { signal })` is called after the debounce, also for an empty query (right after `@`), and may return a promise; abort work when `signal` fires.',
        '`onSelect(item)` adds the item to your context list; return text to keep in the prompt (for example `"@Ana Lopez "`) or nothing to remove the typed `@query`.',
        '`aiMentionToContextChip(item)` maps a result to an AiContextChips item (documents become `file` chips).',
        'AiMentionContext is the presentational menu; `AI_MENTION_CONTEXT_MESSAGES` holds the English defaults and `messages.groups` merges per key.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'items[].kind',
          values: ['doc', 'person', 'record', 'app'],
          description:
            'Groups the results (in this order) and picks the icon: an avatar for people, a glyph otherwise.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-status="loading"',
          description: 'Spinner and "Searching…" while the debounced search runs.',
        },
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description: 'The grouped listbox, or "Nothing matches “…”" when empty.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: '"Couldn’t load results." with a Retry button.',
        },
        {
          name: 'Highlighted / disabled option',
          attribute: 'data-highlighted, data-disabled',
          description:
            'The option Enter would pick; disabled ones are skipped and cannot be picked.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing `@` at the start of a word opens the menu and searches; each change of the query cancels the previous search.',
        'Ids in `exclude` (what is already in the context) are left out of the results.',
        'Clicking an option picks it; focus and the caret stay in the prompt.',
        'A space, moving the caret away or Escape closes the menu.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Down / Arrow Up',
          action:
            'Moves through the results in display order, wrapping and skipping disabled ones.',
        },
        { keys: 'Enter / Tab', action: 'Adds the highlighted result to the context.' },
        { keys: 'Escape', action: 'Closes the menu.' },
      ],
      notes: ['While results are loading Enter is left to the input, so it sends the message.'],
    },
    accessibility: {
      role: 'listbox with groups',
      name: '"Add context" (messages.label); groups are named by their headings',
      items: [
        'The prompt keeps its textbox role with `aria-autocomplete="list"`, `aria-controls` and `aria-activedescendant` while results are shown.',
        'Options are named by their label and described by their description; token counts are visual only.',
        'A polite status announces "Searching…", the number of results, no matches or the error.',
        'Avatars and glyphs are decorative.',
      ],
    },
    responsive: {
      items: [
        'The menu spans the input’s width and scrolls inside itself (at most 20rem or half the viewport); labels truncate.',
        'Below 30rem token counts are hidden to keep labels readable.',
        'On coarse pointers options and Retry are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Panel: `--mn-color-bg-raised`, `--mn-color-border-default`, `--mn-shadow-md`; groups separated by `--mn-color-border-subtle`.',
        'Highlight: `--mn-color-ai-subtle`, glyph in `--mn-color-ai-strong`. Errors in `--mn-color-danger-strong`.',
        'In forced colours the highlighted option gets a Highlight outline, disabled text uses GrayText and Retry uses LinkText.',
      ],
    },
    examples: {
      items: [
        { title: '@ adds context chips', stack: true, render: () => <MentionDemo /> },
        { title: 'With slash commands', stack: true, render: () => <CombinedDemo /> },
        {
          title: 'The menu on its own',
          stack: true,
          render: () => (
            <AiMentionContext items={DIRECTORY} activeId="doc-plan" onSelect={() => {}} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show every pick as a chip with AiContextChips so people see and can remove it.',
        'Return results the assistant is allowed to read; show the rest as disabled with a reason.',
        'Pass token estimates so heavy documents are visible before sending.',
      ],
      dont: [
        'Do not add context silently without a chip.',
        'Do not run an unaborted search per keystroke: honour `signal`.',
        'Do not use it to notify people; the assistant reads, nobody is pinged.',
      ],
    },
    related: {
      items: [
        { id: 'ai-context-chips', reason: 'Shows what the mentions added.' },
        {
          id: 'ai-slash-commands',
          reason: 'The `/` menu; combine both with `combineAiPromptMenus`.',
        },
        { id: 'ai-prompt-input', reason: 'The input it extends.' },
        {
          id: 'mention-picker',
          reason: 'The keyboard model it follows, for people-to-people mentions.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-context-chips', 'avatar', 'spinner'],
    },
    testing: {
      unit: [
        '`aiMentionToContextChip` mapping.',
        'Grouped, named listbox; option names and descriptions; disabled options; loading, empty and error with Retry.',
        'Through AiPromptInput: searching on `@`, ARIA on the textarea, arrow keys in display order, Enter and Tab adding chips, `exclude`.',
        'Inserted text and pointer picking keep focus; error then retry; earlier searches aborted.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
