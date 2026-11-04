import { Button, Input, Toolbar, ToolbarGroup } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'toolbar',
  title: 'Toolbar',
  summary: 'Search, controls and trailing actions above a list, with arrow-key focus movement.',
  sections: {
    purpose: {
      summary:
        'A `role="toolbar"` row for the controls above a list or board: a search slot, grouped controls, trailing controls and an optional filters row underneath.',
      useWhen: [
        'Several related controls act on the same list or view (search, filter, sort, export).',
      ],
      avoidWhen: [
        'A form with labelled fields: use Field or SettingsSection.',
        'One or two buttons: place them directly, no toolbar role needed.',
      ],
    },
    designPrinciples: {
      items: [
        'One Tab stop region with arrow keys inside, as the ARIA toolbar pattern expects.',
        'Text inputs keep their own Left/Right keys so typing is never hijacked.',
        'Trailing controls (`end`) align to the inline end.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native div attributes go to the inner `role="toolbar"` element; `className` goes to the outer `.mn-toolbar-wrap`.',
        'Your `onKeyDown` runs first; calling `preventDefault()` skips the built-in arrow handling.',
        'ToolbarGroup renders a `role="group"` div for visual grouping.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'search / children / end / filters',
          values: ['omitted', 'provided'],
          description:
            'Each slot renders its wrapper only when provided; `filters` renders below the toolbar row.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Static',
          description: 'No state of its own; controls in the slots own their states.',
        },
      ],
    },
    interaction: {
      items: [
        'Arrow and Home/End keys move focus among focusable elements whose nearest toolbar is this one (nested toolbars are skipped).',
        'Movement stops at the ends; it does not wrap.',
        'Left/Right are ignored while a text input has focus; checkbox, radio and button inputs still move.',
        'Focus is not roving-tabindex: every control stays in the Tab order.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Right / Arrow Left',
          action: 'Next / previous control (not inside text inputs).',
        },
        { keys: 'Home / End', action: 'First / last control, including from a text input.' },
        { keys: 'Tab', action: 'Moves through the controls in document order.' },
      ],
    },
    accessibility: {
      role: 'toolbar',
      name: '`label` (required), used as `aria-label`.',
      items: ['The type requires `label`, so the toolbar always has an accessible name.'],
    },
    responsive: {
      items: [
        'The row wraps; the search slot flexes between 10rem and 24rem.',
        'Trailing controls use `margin-inline-start: auto` and wrap under when space runs out.',
      ],
    },
    theme: { items: ['Layout only; colours come from the controls placed inside.'] },
    examples: {
      items: [
        {
          title: 'List tools',
          stack: true,
          render: () => (
            <Toolbar
              label="Task list tools"
              search={<Input aria-label="Search tasks" placeholder="Search tasks" />}
              end={<Button>Export</Button>}
            >
              <ToolbarGroup aria-label="View">
                <Button>Board</Button>
                <Button>List</Button>
              </ToolbarGroup>
            </Toolbar>
          ),
          code: `<Toolbar
  label="Task list tools"
  search={<Input aria-label="Search tasks" placeholder="Search tasks" />}
  end={<Button>Export</Button>}
>
  <ToolbarGroup aria-label="View">
    <Button>Board</Button>
    <Button>List</Button>
  </ToolbarGroup>
</Toolbar>`,
        },
      ],
    },
    doDont: {
      do: [
        'Name the toolbar after what it controls ("Ticket list tools").',
        'Put active filters in `filters`.',
      ],
      dont: [
        'Do not nest a toolbar inside another toolbar.',
        'Do not put unrelated page actions in a list toolbar.',
      ],
    },
    related: {
      items: [
        { id: 'filter-bar', reason: 'Active filters, usually in the `filters` slot.' },
        { id: 'search-input', reason: 'Planned search control for the `search` slot (wave 1).' },
        { id: 'page-header', reason: 'Page-wide actions sit above the toolbar.' },
        { id: 'data-table', reason: 'The list the toolbar usually controls.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Has role toolbar with the given label.',
        'Arrow Right, End and Home move focus; Left inside a text input stays in the input.',
        'ToolbarGroup renders a labelled group.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
