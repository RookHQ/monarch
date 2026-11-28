import { JSONViewer } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './json-viewer.fixture.json';

const payload = JSON.parse(fixture.data.payload) as { data: unknown };

const largeArray = Array.from({ length: 1200 }, (_, i) => ({
  index: i,
  status: i % 7 ? 'ok' : 'retry',
}));

export default {
  id: 'json-viewer',
  title: 'JSONViewer',
  summary: 'A collapsible, searchable tree for a JSON document with copy path and copy value.',
  sections: {
    purpose: {
      summary:
        'Shows a JSON payload (a webhook body, an audit change, an API response, a stored setting) as a tree the reader can open, search and copy from, instead of a wall of pretty-printed text. Needed by: Rook Developers (webhook and request payloads), and the audit detail views in Rook People (`AuditLogView`), Rook Intellect (settings/audit) and Rook Tickets (`RuleDetails`), which print `JSON.stringify(value, null, 2)` in a `<pre>` today.',
      useWhen: [
        'A payload is nested or long enough that readers need to fold parts of it.',
        'Readers copy a single field or its path into code, a filter or a support ticket.',
        'The document may be large (thousands of entries) or come from an untrusted source.',
      ],
      avoidWhen: [
        'Showing a short, fixed snippet of code: use CodeBlock.',
        'Editing JSON: this is read-only.',
        'Showing a handful of known fields: use a DescriptionList with labels people understand.',
      ],
    },
    designPrinciples: {
      items: [
        'Lazy by default: children render only when their parent is open, and each container renders in batches of `maxChildren` behind a "Show more" row.',
        'Never loops: a value that refers back to an ancestor shows "[Circular reference]"; copying uses the same cycle-safe serializer.',
        'Text only: keys and values are rendered as React text, never HTML, so untrusted payloads are safe.',
        'Copy acts on the selected node, from one toolbar, so rows stay single tab stops.',
        'Type colour is a hint, not the only cue: strings keep their quotes and containers show a count.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`formatJsonPath(segments, root?)` turns a path into `$.users[0]["first name"]` text; `stringifyJsonValue(value)` is the cycle-safe `JSON.stringify` used by Copy value.',
        'Pass parsed data: `value={JSON.parse(text)}`. Parse errors are the caller’s to show.',
        'Objects with `toJSON` (Dates) are shown as what JSON would write.',
        'Forwards its ref and other HTML attributes to the root `div`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Search walks the whole document once per query and stops after `maxMatches` matches.',
        'The tree scrolls inside a 28rem box; set a different `max-block-size` on `.mn-json-viewer__tree` through `className` if needed.',
        'Migrating from the app copies: replace `<pre>{JSON.stringify(value, null, 2)}</pre>` with `<JSONViewer value={value} />`. There is no shadcn/ui equivalent; none of the apps ship a JSON tree.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'searchable',
          values: ['true', 'false'],
          description: 'Shows the search field and match count. Default true.',
        },
        {
          prop: 'copyable',
          values: ['true', 'false'],
          description: 'Shows the selected path with Copy path and Copy value. Default true.',
        },
        {
          prop: 'defaultExpandDepth',
          values: ['0', '1', 'n'],
          description: 'How many levels start open. Default 1.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Expanded',
          attribute: 'aria-expanded',
          description: 'On objects and arrays with children.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected',
          description: 'The focused node; the footer shows its path.',
        },
        {
          name: 'Match',
          attribute: 'data-match',
          description:
            'The key or value contains the search text; matched text is wrapped in `<mark>`.',
        },
        {
          name: 'Value type',
          attribute: 'data-type',
          description:
            'object, array, string, number, boolean, null, undefined, other or circular.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a row selects it; clicking an object or array also opens or closes it.',
        'Typing in search opens every ancestor of a match and reveals matches hidden behind "Show more".',
        '"Show more" renders the next batch and moves focus to its first child.',
        'Collapse all closes everything below the root and clears the search.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves into the tree to the selected node (one tab stop), then to Copy path and Copy value.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Next or previous visible node.' },
        {
          keys: 'ArrowRight',
          action: 'Opens a closed node; on an open node, moves to its first child.',
        },
        { keys: 'ArrowLeft', action: 'Closes an open node; otherwise moves to its parent.' },
        { keys: 'Home / End', action: 'First or last visible node.' },
        { keys: 'Enter / Space', action: 'Toggles the node, or activates "Show more".' },
        { keys: '*', action: 'Opens every sibling of the focused node.' },
      ],
    },
    accessibility: {
      role: 'tree with treeitem rows',
      name: '`messages.label` (default "JSON"); each row is named "key: value".',
      items: [
        'Follows the WAI-ARIA APG tree view pattern with roving tabindex; rows carry `aria-level`, `aria-setsize` and `aria-posinset`, so hidden batches are still counted.',
        'The match count is a polite status; copy results are announced by CopyButton.',
        'The chevron is decorative; the open state is `aria-expanded`.',
        'Windows High Contrast: the selected row uses Highlight colours and matches use Mark.',
      ],
    },
    responsive: {
      items: [
        'Toolbar and footer wrap at 390px; the search field shrinks first.',
        'Long keys and values wrap anywhere instead of scrolling sideways.',
        'On coarse pointers each row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text use `--mn-color-bg-surface`, `--mn-color-border-subtle` and `--mn-color-text-*`.',
        'Value types use info (keys), success (strings), accent (numbers) and warning (booleans and null) strong text tokens.',
        'Matches use `--mn-color-warning-subtle`; RTL flips the chevron and indents from the inline start.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Webhook payload',
          stack: true,
          render: () => (
            <JSONViewer value={payload} defaultExpandDepth={fixture.data.expandDepth} />
          ),
        },
        {
          title: 'Large array in batches',
          stack: true,
          render: () => <JSONViewer value={largeArray} maxChildren={50} copyable={false} />,
        },
        {
          title: 'Paths without the $ prefix, no search',
          stack: true,
          render: () => <JSONViewer value={payload.data} rootPath="" searchable={false} />,
        },
      ],
    },
    doDont: {
      do: [
        'Parse JSON text first and show a clear error when it does not parse.',
        'Keep `maxChildren` around 100 so very large arrays stay responsive.',
        'Give it a label naming the document ("Request body") through `messages.label`.',
      ],
      dont: [
        'Do not use it for secrets: anything shown can be copied. Redact first.',
        'Do not expand everything by default on unknown payloads; set a small `defaultExpandDepth`.',
      ],
    },
    related: {
      items: [
        { id: 'code-block', reason: 'Short, fixed snippets as highlighted text.' },
        { id: 'tree-view', reason: 'General hierarchies of named items.' },
        { id: 'log-drawer', reason: 'A log entry’s details; put a JSONViewer in its body.' },
        { id: 'copy-button', reason: 'The copy actions it uses.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'search-input'],
    },
    testing: {
      unit: [
        'Named tree, root open one level, levels and counts, closed containers summarised.',
        'APG keyboard: arrows, Home/End, Enter/Space, ArrowLeft to parent, * opens siblings.',
        'Search highlights with <mark>, reports the count and opens ancestors.',
        'Copy path and Copy value for the selected node.',
        '"Show more" batches and moves focus.',
        'Circular references shown and serialised safely; path formatting; custom messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
