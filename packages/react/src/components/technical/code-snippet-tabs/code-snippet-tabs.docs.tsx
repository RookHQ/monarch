import { CodeSnippetTabs, type CodeSnippet } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './code-snippet-tabs.fixture.json';

const LIST_ITEMS: CodeSnippet[] = fixture.data.snippets;

const CREATE_ITEM: CodeSnippet[] = [
  {
    label: 'cURL',
    language: 'bash',
    code: 'curl https://api.example.com/v1/items \\\n  -H "Authorization: Bearer $API_KEY" \\\n  -d name="Sample item"',
  },
  {
    label: 'JavaScript',
    language: 'js',
    code: "await fetch('https://api.example.com/v1/items', {\n  method: 'POST',\n  body: JSON.stringify({ name: 'Sample item' }),\n});",
  },
];

export default {
  id: 'code-snippet-tabs',
  title: 'CodeSnippetTabs',
  summary:
    'The same code example in several languages, one tab each, with copy and an optional page-wide language sync.',
  sections: {
    purpose: {
      summary:
        "Shows one example (an API request, an install command) in several languages or tools, with a copy button per tab. It is a thin wrapper over CodeTabs from CodeBlock, which already does the tabs, highlighting and copy; this component takes `snippets` and adds `syncKey`, so every snippet set on a docs page follows the reader's language and remembers it. Needed by: Developers (marketing pages, product API pages and the Tickets, Geo and Dots docs, which use an app `CodeTabs` copy in `components/developers/CodeBlock.tsx`).",
      useWhen: [
        'Developer docs or settings show the same request in cURL, JavaScript, Python and so on.',
        'A page has several snippet sets and the reader should pick a language once.',
      ],
      avoidWhen: [
        'One language only: use CodeBlock.',
        'You do not need the language sync: CodeTabs (from the root entry) is the same widget.',
        'A whole install flow with verification: use InstallVerify.',
      ],
    },
    designPrinciples: {
      items: [
        'No second implementation: tabs, panels, highlighting, wrap and copy all come from CodeTabs and CodeBlock.',
        'Copy what you read: each tab has its own copy button, so the copied code is always the visible version.',
        'Pick once: with `syncKey`, choosing JavaScript in one set switches every set with that key that has JavaScript.',
        'Server-safe: storage is read only after mount, so the server and first client render agree.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Wraps CodeTabs: every CodeTabs prop except `samples` passes through (`label`, `value`, `defaultValue`, `onValueChange`, `lineNumbers`, `showWrapToggle`, `messages`).',
        "`CodeSnippet` is CodeTabs' `CodeSample` (`label`, `code`, `language?`, `value?`, `title?`).",
        'Forwards its ref to the CodeTabs root.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`syncKey` stores the choice in localStorage under `mn-code-snippet-tabs:<key>`. Blocked storage is ignored; the in-page sync still works.',
        'A set only follows a synced choice it has a tab for; otherwise it keeps its own.',
        '`value` (controlled) wins over the sync.',
        'Migrating from the Developers app `CodeTabs`: `samples` → `snippets` (same `label`, `language`, `code`), `title` → `label` (the tab list name; for a heading inside the block, set each snippet\'s `title`). The copy button, `role="tablist"` and panel are built in.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. `lineNumbers` and `showWrapToggle` pass through to each CodeBlock.',
      ],
    },
    states: {
      items: [
        {
          name: 'Selected tab',
          attribute: 'aria-selected="true" / data-state="active" on the tab',
          description: 'The visible snippet.',
        },
        {
          name: 'Synced',
          attribute: 'data-synced',
          description: 'The set follows other sets with the same `syncKey`.',
        },
        {
          name: 'Copied',
          attribute: 'from CodeBlock',
          description: 'The copy button confirms in place after copying.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a tab shows its snippet and calls `onValueChange`.',
        'Copy copies the visible snippet exactly as given.',
        'With `syncKey`, the choice is shared with every set with that key and restored on the next visit.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: "Moves to the selected tab, then into the panel's controls and code region.",
        },
        { keys: 'Arrow Left / Right', action: 'Selects the previous or next tab.' },
        { keys: 'Home / End', action: 'Selects the first or last tab.' },
        { keys: 'Enter / Space', action: 'Activates Copy or the wrap toggle.' },
      ],
    },
    accessibility: {
      role: 'tablist, tab, tabpanel; each code area is a focusable region',
      name: 'The tab list is named by `label` (default "Code samples"); each code region by the snippet `title` or label and the set `label` ("cURL, List items"), so several sets on a page stay distinct.',
      items: [
        'Follows the WAI-ARIA tabs pattern through Monarch Tabs.',
        'The code region is focusable so keyboard users can scroll long lines.',
        'Copy confirmation is announced politely.',
      ],
    },
    responsive: {
      items: [
        'The tab list scrolls sideways when tabs do not fit; the page never does.',
        'Long lines scroll inside the code region, or wrap with the wrap toggle, at 390px.',
        'Tabs and buttons keep 44px targets on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'All colours come from CodeBlock and Tabs tokens; this wrapper adds none.',
        'Forced colours use system colours for tabs, focus rings and code.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Request in three languages',
          stack: true,
          render: () => <CodeSnippetTabs snippets={LIST_ITEMS} label={fixture.data.label} />,
        },
        {
          title: 'Two sets kept on one language',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16 }}>
              <CodeSnippetTabs snippets={LIST_ITEMS} syncKey="docs-example" label="List items" />
              <CodeSnippetTabs
                snippets={CREATE_ITEM}
                syncKey="docs-example"
                label="Create an item"
              />
            </div>
          ),
        },
        {
          title: 'Line numbers',
          stack: true,
          render: () => (
            <CodeSnippetTabs snippets={LIST_ITEMS} lineNumbers label="With line numbers" />
          ),
        },
      ],
    },
    doDont: {
      do: [
        "Use one `syncKey` per site so a reader's language follows them across pages.",
        'Give each snippet a stable `value` when labels are translated.',
        'Keep the snippets equivalent: the same request in each language.',
      ],
      dont: [
        'Do not put secrets in snippets; use environment variables as in the examples.',
        'Do not mix unrelated examples in one set; use separate sets.',
      ],
    },
    related: {
      items: [
        { id: 'code-block', reason: 'One snippet, or CodeTabs without the language sync.' },
        { id: 'tabs', reason: 'The tab widget it is built on.' },
        { id: 'copy-button', reason: 'Copy a single value that is not code.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['code-block'],
    },
    testing: {
      unit: [
        'One named tab per snippet, rendered through CodeTabs; axe passes.',
        'Arrow keys switch tabs, `onValueChange` fires, and Copy copies the visible snippet.',
        'Sets with the same `syncKey` follow each other and store the choice; a set without that language keeps its own; sets without a key are unaffected.',
        'A stored choice is restored after mount.',
        'Controlled `value`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
