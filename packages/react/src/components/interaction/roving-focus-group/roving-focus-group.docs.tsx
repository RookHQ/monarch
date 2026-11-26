import { useState } from 'react';
import { Button, RovingFocusGroup } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ToolbarDemo() {
  return (
    <RovingFocusGroup role="toolbar" aria-label="Text formatting">
      <Button variant="ghost" size="sm">
        Bold
      </Button>
      <Button variant="ghost" size="sm">
        Italic
      </Button>
      <Button variant="ghost" size="sm" disabled>
        Strikethrough
      </Button>
      <Button variant="ghost" size="sm">
        Link
      </Button>
    </RovingFocusGroup>
  );
}

const SWATCHES = ['Slate', 'Blue', 'Green', 'Amber', 'Red', 'Violet', 'Teal', 'Rose', 'Grey'];

function GridDemo() {
  const [chosen, setChosen] = useState('Blue');
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <RovingFocusGroup
        orientation="grid"
        columns={3}
        role="group"
        aria-label="Label colour"
        style={{ display: 'grid', gridTemplateColumns: 'repeat(3, max-content)', gap: 8 }}
      >
        {SWATCHES.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={s === chosen ? 'primary' : 'secondary'}
            aria-pressed={s === chosen}
            data-mn-roving-item=""
            onClick={() => setChosen(s)}
          >
            {s}
          </Button>
        ))}
      </RovingFocusGroup>
      <span style={{ fontSize: 'var(--mn-text-sm)' }}>Chosen: {chosen}</span>
    </div>
  );
}

export default {
  id: 'roving-focus-group',
  title: 'RovingFocusGroup',
  summary:
    'Roving tabindex for toolbars, option sets and grids: one Tab stop, arrow keys inside, Home and End to the ends.',
  sections: {
    purpose: {
      summary:
        'Composite widgets (toolbars, segmented choices, swatch grids, card grids) should be one Tab stop with arrow-key movement inside, as WAI-ARIA APG describes. Rook apps set `tabIndex={-1}` by hand in many places; this is the shared helper. Needed by: every Rook frontend that builds its own toolbar or grid of choices.',
      useWhen: [
        'A toolbar of buttons, a set of chips or swatches, or a grid of tiles the user moves through with arrows.',
        'A custom composite widget that needs APG roving tabindex without its own key handling (`useRovingFocus`).',
      ],
      avoidWhen: [
        'A form or a page section: users expect Tab to reach every field.',
        'Tabs, menus, radio groups, Select, Combobox, SegmentedControl, Toolbar: those Monarch components already manage focus.',
      ],
    },
    designPrinciples: {
      items: [
        'One Tab stop: the active item has `tabindex="0"`, every other item `-1`; returning with Tab lands on the item used last.',
        'Starts on the selected item (`aria-selected`, `aria-checked` or `aria-current`) when there is one.',
        'Headless: it manages focus only. The role, the name and the look belong to the caller.',
        'Text fields keep their own caret keys: Left, Right, Home and End are not taken from an input.',
        'Disabled items are skipped by arrows; in a grid they keep their cell so rows stay aligned.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`RovingFocusGroup` renders a `<div class="mn-roving-focus-group" data-orientation>`; `useRovingFocus(ref, options)` returns `{ onKeyDown }` for any element.',
        'Items: elements marked `data-mn-roving-item` (export `ROVING_ITEM_ATTRIBUTE`), or every focusable control when none are marked, or `itemSelector`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Radix `RovingFocusGroup`: `orientation`, `loop` and `dir` are the same; items are marked with `data-mn-roving-item` instead of wrapping each in `RovingFocusGroupItem`; `grid` orientation is added.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical', 'both', 'grid'],
          description:
            'Left/Right, Up/Down, all four arrows in one sequence, or 2D movement by `columns` (measured from the layout when not given).',
        },
        {
          prop: 'loop',
          values: ['true', 'false'],
          description: 'Wrap at the ends (not in grids).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active item',
          attribute: 'tabindex="0" on one item',
          description: 'The item Tab lands on; it follows focus.',
        },
        {
          name: 'Orientation',
          attribute: 'data-orientation',
          description:
            'Horizontal groups lay items in a wrapping row, vertical in a column; `both` and `grid` leave layout to you.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking or focusing an item makes it the active item.',
        'Items added, removed or disabled later are picked up (a MutationObserver keeps tabindex in sync).',
        'Nested groups manage their own items; the outer group skips them.',
        'For roles that accept it (`toolbar`, `listbox`, `tablist`, `menu`, `menubar`), `aria-orientation` is set from `orientation`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift + Tab', action: 'Enters the group on the active item; leaves it.' },
        {
          keys: 'ArrowRight / ArrowLeft',
          action: 'Next or previous item (horizontal, both, grid row); swapped in RTL.',
        },
        {
          keys: 'ArrowDown / ArrowUp',
          action: 'Next or previous item (vertical, both); the cell below or above (grid).',
        },
        {
          keys: 'Home / End',
          action: 'First or last item; in a grid, the first or last in the row.',
        },
        {
          keys: 'Ctrl + Home / Ctrl + End',
          action: 'Grid: the first or last item of the whole grid.',
        },
      ],
    },
    accessibility: {
      role: 'set by the caller: toolbar, listbox, radiogroup, grid or group',
      name: '`aria-label` or `aria-labelledby` on the group.',
      items: [
        'Follows the WAI-ARIA APG roving tabindex technique for composite widgets.',
        'Items keep their own roles and states (`aria-pressed`, `aria-checked`, `aria-selected`).',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Horizontal groups wrap at narrow widths. In `grid` without `columns`, rows are measured from the layout, so arrows follow the wrapped grid at any width.',
      ],
    },
    theme: {
      items: [
        'No colours of its own. Marked items get the Monarch focus ring if they do not have one; Highlight in forced-colours mode.',
      ],
    },
    examples: {
      items: [
        { title: 'Toolbar', render: () => <ToolbarDemo /> },
        { title: 'Grid of choices', render: () => <GridDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give the group a role and an accessible name.',
        'Pass `columns` when the grid has a fixed number of columns.',
      ],
      dont: [
        'Do not put a whole form in a roving group.',
        'Do not wrap Monarch components that already manage their own focus.',
      ],
    },
    related: {
      items: [
        {
          id: 'toolbar',
          reason: 'A ready toolbar layout with search and filter slots and roving focus.',
        },
        { id: 'segmented-control', reason: 'A single-choice set with radio semantics built in.' },
        { id: 'focus-trap', reason: 'Contains Tab inside a modal surface instead.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'One Tab stop and aria-orientation; axe.',
        'Arrows, disabled items skipped, loop and no loop, Home/End, Tab out and back to the last item.',
        'Vertical orientation, RTL, and grid movement by columns with Ctrl+Home/End.',
        'Text inputs keep their arrows; marked items and nested groups; starts on the checked item and reports the index.',
        'Items added later; the hook on a list; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
