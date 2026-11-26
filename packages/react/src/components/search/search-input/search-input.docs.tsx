import { useState } from 'react';
import { Field, FieldLabel, SearchInput } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FRUIT = ['Apple', 'Apricot', 'Banana', 'Blueberry', 'Cherry', 'Grape', 'Mango', 'Peach'];

function DebouncedDemo() {
  const [query, setQuery] = useState('');
  const [pending, setPending] = useState(false);
  const results = FRUIT.filter((f) => f.toLowerCase().includes(query.toLowerCase()));
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 300 }}>
      <SearchInput
        aria-label="Search fruit"
        placeholder="Search fruit"
        debounce={300}
        pending={pending}
        onValueChange={() => setPending(true)}
        onSearch={(q) => {
          setQuery(q);
          setPending(false);
        }}
      />
      <p style={{ margin: 0, fontSize: 13 }} aria-live="polite">
        {results.length} {results.length === 1 ? 'result' : 'results'}
        {query ? ` for “${query}”` : ''}
      </p>
    </div>
  );
}

export default {
  id: 'search-input',
  title: 'SearchInput',
  summary: 'Search box with icon, clear button, debounce, pending state and shortcut hint.',
  sections: {
    purpose: {
      summary:
        'A single-line search box for filtering a list or querying a server. It adds the parts every search field re-implements: a leading icon, a clear button, a debounced search callback, a pending state while results load and an optional keyboard shortcut.',
      useWhen: [
        'Filtering a table, list or grid as the user types.',
        'Querying a server, where `debounce` keeps requests to one per pause.',
        'A page-level search reached with a shortcut such as "/" or ⌘K.',
      ],
      avoidWhen: [
        'The user picks one value from a list: use Combobox.',
        'Free text that is not a query (a name, a title): use Input.',
        'A command palette with grouped actions and results: use a command menu.',
      ],
    },
    designPrinciples: {
      items: [
        'One callback per concern: `onValueChange` for every keystroke, `onSearch` for when to run the query.',
        'Clearing is always one step: the clear button or Escape, and both return focus to the field.',
        'Pending replaces the magnifier with a spinner in the same place, so the layout never shifts.',
        'The shortcut hint is decorative; `shortcutKeys` is what binds the key and announces it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `.mn-search-input` wrapper (styled as `.mn-input`) around a native `<input type="search">`. The ref and all native input props go to the input; `className` goes to the wrapper.',
        'Reads Field context through `useFieldControl`, like Input.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onChange` still receives the native change event; `onValueChange` receives the string.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Control height from `--mn-control-*`, as for Input. Default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'data-pending on the wrapper, aria-busy="true" on the input',
          description: 'The leading icon becomes a spinner while a search is in flight.',
        },
        {
          name: 'Has text',
          description: 'The clear button appears; the shortcut hint hides.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid on the wrapper, aria-invalid="true" on the input',
          description: 'Danger border, from `invalid` or the surrounding Field.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled on the wrapper, disabled on the input',
          description: 'Muted surface; no clear button.',
        },
        {
          name: 'Focus',
          description: 'The whole wrapper shows the Monarch focus ring while the input has focus.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing calls `onValueChange` at once and `onSearch` after `debounce` ms without typing (at once when `debounce` is 0).',
        'Enter calls `onSearch` immediately and cancels the waiting debounce.',
        'Clearing calls `onValueChange("")`, `onSearch("")` and `onClear`, and keeps focus in the field.',
        'With `shortcutKeys`, pressing the combination anywhere focuses the field and selects its text. Single-key shortcuts such as "/" are ignored while another field has focus.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'Runs the search now.' },
        {
          keys: 'Escape',
          action:
            'Clears the text and stops the event. On an empty field Escape is not handled and bubbles. Inside a Monarch Dialog or Popover, the overlay sees Escape first (Radix listens in the capture phase) and closes.',
        },
        { keys: 'Tab', action: 'Moves to the clear button while there is text, then onward.' },
        { keys: '`shortcutKeys`', action: 'Focuses the field from anywhere on the page.' },
      ],
    },
    accessibility: {
      role: 'searchbox (native input type="search")',
      name: 'A FieldLabel, or `aria-label` when the context makes the purpose clear ("Search documents"). The placeholder is not a label.',
      items: [
        'The clear button is a real button named by `clearLabel` (default "Clear search").',
        'Icons, the spinner and the shortcut hint are `aria-hidden`; `aria-keyshortcuts` announces the shortcut instead.',
        '`aria-busy` is set while pending. Announce the result count yourself in a polite live region, as in the example.',
        'Wrap a page-level search in `<search>` or `role="search"` yourself; the component does not add a landmark, because filters inside cards should not create one.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the icon, clear button and hint keep their size and the text field shrinks.',
        '`enterkeyhint="search"` shows a Search key on mobile keyboards.',
      ],
    },
    theme: {
      items: [
        'Colours, border and focus ring are Monarch semantic tokens, so light, dark and more-contrast themes apply automatically.',
        'The native WebKit cancel button is hidden so only the Monarch clear button shows.',
        'The clear button hover transition is removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Debounced, pending',
          stack: true,
          render: () => <DebouncedDemo />,
        },
        {
          title: 'Shortcut, sizes and Field',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 300 }}>
              <SearchInput
                aria-label="Search the page"
                shortcut="/"
                shortcutKeys="/"
                placeholder="Search"
              />
              <SearchInput aria-label="Filter rows" size="sm" defaultValue="overdue" />
              <Field>
                <FieldLabel>Find a member</FieldLabel>
                <SearchInput placeholder="Name or email" />
              </Field>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use `debounce` (250–400 ms) when each search hits a server.',
        'Show `pending` from the moment the user types until results arrive.',
        'Announce the number of results in a polite live region.',
      ],
      dont: [
        'Do not rely on the placeholder as the only label.',
        'Do not bind a single-key shortcut on pages where the user mostly types; prefer a modifier combination.',
        'Do not fetch in `onValueChange` when `onSearch` with `debounce` does it once per pause.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'Plain Input for text that is not a query.' },
        { id: 'combobox', reason: 'Pick one value from a filtered list.' },
        { id: 'filter-bar', reason: 'A row of filters that usually starts with a SearchInput.' },
        { id: 'kbd', reason: 'The shortcut hint is a Kbd.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'spinner', 'kbd'],
      notes: ['Uses the `.mn-input` styles from text-field for the wrapper.'],
    },
    testing: {
      unit: [
        'Renders a labelled native searchbox.',
        'Debounces `onSearch` while reporting every keystroke through `onValueChange`.',
        'Enter searches at once and cancels the waiting debounce; no debounce searches on every change.',
        'The clear button clears, calls `onSearch("")` and `onClear`, and keeps focus.',
        'Escape clears; a second Escape on an empty field reaches the page.',
        'Controlled mode.',
        'Pending sets `aria-busy` and shows the spinner.',
        'Single-key and modifier shortcuts focus the field; single keys are ignored while typing elsewhere.',
        'Reads Field label, error and disabled state; no clear button when disabled.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/search-input'],
    },
  },
} satisfies ComponentDocs;
