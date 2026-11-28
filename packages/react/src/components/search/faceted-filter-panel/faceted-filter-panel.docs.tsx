import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { FacetedFilterPanel, type Facet, type FacetValue } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './faceted-filter-panel.fixture.json';

const FACETS = fixture.data.facets as Facet[];
const SELECTED: FacetValue = Object.fromEntries(
  fixture.data.selected.map((entry) => [entry.facet, entry.values]),
);

function ControlledDemo() {
  const [value, setValue] = useState<FacetValue>({ status: ['open'], size: { max: 25 } });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', maxWidth: '18rem' }}>
      <FacetedFilterPanel facets={FACETS} value={value} onValueChange={setValue} />
      <code style={{ fontSize: 'var(--mn-text-xs)', overflowWrap: 'anywhere' }}>
        {JSON.stringify(value)}
      </code>
    </div>
  );
}

export default {
  id: 'faceted-filter-panel',
  title: 'FacetedFilterPanel',
  summary: 'Filter sidebar of collapsible facet groups: checkboxes with counts, ranges and clear.',
  sections: {
    purpose: {
      summary:
        'Lets people narrow a large result set by several attributes at once, seeing how many items each choice matches. Needed by: Rook Developers (API catalog filtered by category, provider and status facets), Rook People (audit log filtered by actor, record type and area facets), and search pages next to SearchResults.',
      useWhen: [
        'A search or catalog page whose backend returns facet counts.',
        'Several filters apply together and people benefit from seeing all of them at once.',
        'A numeric attribute needs a minimum and maximum (size, price, age).',
      ],
      avoidWhen: [
        'One or two filters above a table: use FilterBar chips.',
        'A single exclusive choice such as All / Open / Closed: use TabsFilter or SegmentedControl.',
        'Building nested AND/OR conditions: use QueryBuilder.',
      ],
    },
    designPrinciples: {
      items: [
        'Counts are part of each option’s name, so they are read, not just seen.',
        'Every group can be cleared on its own, and everything can be cleared at once; nothing needs a separate Apply step unless you add one in `footer`.',
        'Checked options never hide behind "Show more".',
        'The value is plain data (`{ facetId: string[] | { min, max } }`), easy to put in a URL or a SavedView.',
        'Built from Checkbox, Input and Button, so it matches every other form.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div role="group">`; other div attributes pass through.',
        '`facets` is an array of `CheckboxFacet` (`type: "checkbox"`, `options`, `maxVisible?`) and `RangeFacet` (`type: "range"`, `min?`, `max?`, `step?`, `unit?`). Both take `id`, `label` and `defaultOpen?`.',
        'Empty facets are removed from the value: clearing a group deletes its key, and clear-all sends `{}`.',
        '`facetActiveCount(facet, value)` returns how many filters a facet contributes, for a "Filters (3)" button that opens the panel in a Sheet on mobile.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from app code: the apps render one `Select` per facet (Rook Developers `FacetSelect`, Rook People audit filters). Each `FacetSelect` becomes a `CheckboxFacet` whose `options` are the facets (`{ value, label, count }`), and the `value`/`onChange` pairs become one `value`/`onValueChange`. There is no shadcn/ui faceted filter in the apps; shadcn’s data-table "faceted filter" example (popover of checkboxes with counts) maps to one CheckboxFacet.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'facets[].type',
          values: ['checkbox', 'range'],
          description: 'A checkbox list with counts, or minimum and maximum number inputs.',
        },
        {
          prop: 'hideTitle',
          values: ['true', 'false'],
          description:
            'Hides the "Filters" heading (for example inside a Sheet that has its own title) and names the group with `aria-label` instead.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Has active filters',
          attribute: 'data-active',
          description: 'At least one filter is set; "Clear all" is enabled.',
        },
        {
          name: 'Group open / closed',
          attribute: 'data-state="open" | "closed", aria-expanded',
          description: 'The chevron rotates and the group’s content is hidden when closed.',
        },
        {
          name: 'Group has selections',
          description: 'The toggle shows "2 selected" and a Clear button appears.',
        },
        {
          name: 'Zero-count option',
          attribute: 'data-empty',
          description: 'Muted text; still selectable so an existing filter can be removed.',
        },
        {
          name: 'Invalid range',
          attribute: 'aria-invalid',
          description:
            'The minimum is above the maximum: both inputs are invalid and an error is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Checking or unchecking an option, or typing a bound, calls `onValueChange` with the whole value at once.',
        'A group heading button expands or collapses the group.',
        '"Show N more" reveals options beyond `maxVisible` (default 6); checked options are always shown.',
        'Range inputs accept partial input while typing and keep only finite numbers in the value; an empty input removes that bound.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through group toggles, clear buttons, checkboxes and inputs.',
        },
        {
          keys: 'Enter / Space',
          action: 'Expands or collapses a group, or activates a clear or show-more button.',
        },
        { keys: 'Space', action: 'Toggles the focused checkbox.' },
        { keys: 'Arrow Up / Arrow Down', action: 'Steps a range input by `step`.' },
      ],
    },
    accessibility: {
      role: 'group (panel and each facet)',
      name: 'The panel is labelled by its title; each facet’s options are labelled by the group toggle.',
      items: [
        'Group toggles follow the APG disclosure pattern: a button inside a heading with `aria-expanded` and `aria-controls`.',
        'Each checkbox’s name includes its count ("Open 128").',
        'Per-group clear buttons have specific names ("Clear Status filter").',
        'Range inputs have visible labels; the unit is their description and the invalid-range message is announced with `role="alert"`.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; range inputs wrap under each other in narrow sidebars, so it fits at 390px without sideways scroll.',
        'Toggles, option rows and clear buttons are 44px tall on coarse pointers.',
        'On small screens put it in a Sheet behind a "Filters" button, with `hideTitle` and an Apply button in `footer` if needed.',
      ],
    },
    theme: {
      items: [
        'Dividers use `--mn-color-border-subtle`; counts and labels use `--mn-color-text-muted`.',
        'The "N selected" pill uses `--mn-color-accent-subtle` and `--mn-color-accent-strong`.',
        'The chevron rotation is instant under reduced motion and mirrors in right-to-left layouts.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Checkbox and range facets',
          render: () => (
            <div style={{ maxWidth: '18rem', width: '100%' }}>
              <FacetedFilterPanel facets={FACETS} defaultValue={SELECTED} />
            </div>
          ),
        },
        { title: 'Controlled value', render: () => <ControlledDemo /> },
        {
          title: 'Without title, with an Apply footer (for a Sheet)',
          render: () => (
            <div style={{ maxWidth: '18rem', width: '100%' }}>
              <FacetedFilterPanel
                facets={FACETS.slice(0, 1)}
                hideTitle
                footer={
                  <Button variant="primary" block>
                    Show results
                  </Button>
                }
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show counts from the backend for the current query so people avoid empty results.',
        'Keep the value in the URL so filtered searches can be shared and restored.',
        'Collapse rarely used facets with `defaultOpen: false`.',
      ],
      dont: [
        'Disable zero-count options that are checked; people must be able to remove them.',
        'Put more than about eight facets in one panel.',
        'Use it for one exclusive choice; use TabsFilter.',
      ],
    },
    related: {
      items: [
        { id: 'filter-bar', reason: 'Compact chips for a few filters above a list.' },
        { id: 'search-results', reason: 'The results the facets narrow.' },
        { id: 'saved-views', reason: 'Stores a facet value as a named view.' },
        { id: 'query-builder', reason: 'Nested AND/OR conditions instead of facets.' },
        { id: 'checkbox', reason: 'The control each option is built on.' },
        { id: 'sheet', reason: 'Holds the panel on small screens.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'checkbox', 'text-field'] },
    testing: {
      unit: [
        'Panel and groups are labelled; checkbox names include counts; unit describes the range inputs; clear-all disabled with no filters.',
        'Toggling options (uncontrolled), "2 selected" on the toggle, per-group clear.',
        'Space toggles a checkbox; Enter collapses and expands a group with aria-expanded.',
        'Show more / show fewer; checked options stay visible.',
        'Range typing, invalid range alert, clearing bounds removes the key.',
        'Controlled value and clear-all; facetActiveCount; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
