import { useState } from 'react';
import { Button, EntityPicker, Field, FieldLabel } from '@rookcom/monarch-react';
import type { ComboboxOption } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { BoardIcon, GridIcon, ListIcon } from '../../../internal/docs-demo.js';

// Built lazily: docs files cannot create JSX at module scope.
const makeProjects = (): ComboboxOption[] => [
  {
    value: 'website',
    label: 'Website refresh',
    hint: 'WEB',
    group: 'Active',
    media: <BoardIcon />,
  },
  {
    value: 'mobile',
    label: 'Mobile onboarding',
    hint: 'MOB',
    group: 'Active',
    media: <ListIcon />,
  },
  {
    value: 'billing',
    label: 'Billing migration',
    hint: 'BIL',
    group: 'Active',
    media: <GridIcon />,
  },
  {
    value: 'research',
    label: 'Customer research',
    hint: 'RES',
    group: 'Paused',
    description: 'Paused until next quarter',
    media: <ListIcon />,
  },
];

const labels: ComboboxOption[] = [
  { value: 'bug', label: 'Bug' },
  { value: 'feature', label: 'Feature' },
  { value: 'docs', label: 'Documentation' },
  { value: 'perf', label: 'Performance' },
];

function ProjectDemo() {
  const [projects] = useState(makeProjects);
  const [value, setValue] = useState<string | null>('mobile');
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>Project</FieldLabel>
      <EntityPicker
        options={projects}
        value={value}
        onValueChange={setValue}
        placeholder="Choose a project"
        clearOption={{ label: 'No project' }}
        footer={({ close }) => (
          <Button variant="ghost" size="sm" block onClick={close}>
            Manage projects
          </Button>
        )}
      />
    </Field>
  );
}

function LabelsDemo() {
  const [values, setValues] = useState(['bug', 'perf']);
  const [options, setOptions] = useState(labels);
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>Labels</FieldLabel>
      <EntityPicker
        multiple
        options={options}
        values={values}
        onValuesChange={setValues}
        placeholder="Add labels"
        onCreate={(q) => {
          const option = { value: q.toLowerCase(), label: q };
          setOptions((list) => [...list, option]);
          return option;
        }}
      />
    </Field>
  );
}

function ChipDemo() {
  const [projects] = useState(makeProjects);
  const [value, setValue] = useState<string | null>('website');
  const current = projects.find((p) => p.value === value);
  return (
    <EntityPicker
      options={projects}
      value={value}
      onValueChange={setValue}
      size="sm"
      trigger={
        <Button
          size="xs"
          aria-label={`${current?.hint ?? 'None'}: project ${current?.label ?? 'none'}`}
        >
          {current?.hint ?? 'None'}
        </Button>
      }
    />
  );
}

export default {
  id: 'entity-picker',
  title: 'EntityPicker',
  summary:
    'Pick one or several records (projects, contacts, teams) from a searchable list in a popover.',
  sections: {
    purpose: {
      summary:
        'The "click the field, search, pick a record" pattern every Rook product rebuilt: a trigger (a field-style button or the product’s own chip) opens a popover with a search box and a listbox. The product injects the records, search, rendering and creation; EntityPicker owns the trigger, popover, keyboard and selection model.',
      useWhen: [
        'Picking a project, contact, company, team, status or space from a list that may be long or server-searched.',
        'The value is shown as a compact chip or field and edited in place (issue properties, ticket sidebars).',
        'Several records, with the list staying open while toggling (`multiple`).',
      ],
      avoidWhen: [
        'A short fixed list with no search: use Select.',
        'People: use AssigneePicker, which adds avatars, "you" and "Unassigned".',
        'Typing straight into a form field: use Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'No product data in Monarch: records arrive as `options` or from `onSearch`, and rich rows come from `media`, `description`, `hint` or `renderOption`.',
        'One picker for every record type, so keyboard behaviour and accessibility are the same everywhere.',
        'Prefix matches rank first (`sortByMatch`, on by default), so "in" finds India before Argentina.',
        'Single pick closes and returns focus to the trigger; multiple keeps the list open.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the trigger button. Native button attributes go to the default trigger.',
        'The panel is a Radix Popover (`role="dialog"`) containing an inline Combobox and an optional footer.',
        'Shares option, message and search types with Combobox (`ComboboxOption`, `ComboboxMessages`, `ComboboxSearchContext`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`trigger` replaces the default trigger; it must accept a ref and name itself, with its visible text in the name (for example `aria-label="WEB: project Website refresh"`).',
        '`footer` may be a function of `{ close, query }`, for actions such as "Create “query”" or "Manage projects".',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Trigger height from `--mn-control-*`; the search row uses sm or md.',
        },
        {
          prop: 'multiple',
          values: ['false', 'true'],
          description: 'One record, or several shown as "first +N".',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Popover alignment against the trigger.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Placeholder',
          attribute: 'data-placeholder',
          description: 'Nothing selected: the placeholder in muted text.',
        },
        {
          name: 'Open',
          attribute: 'aria-expanded="true" on the trigger',
          description: 'The popover with search and list is shown.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected="true"',
          description: 'Check mark on chosen records.',
        },
        {
          name: 'Loading / error / empty',
          attribute: 'role="status"',
          description:
            'From Combobox: "Loading…", a retry after a failed search, or `emptyMessage`.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid',
          description: 'Danger border, from `invalid` or the Field.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'The trigger cannot open the picker.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger (or Enter / Space on it) opens the popover and focuses the search box.',
        'Typing filters (or calls `onSearch` after the debounce); arrows move, Enter or a click picks.',
        'Single: picking closes the popover and returns focus to the trigger. Multiple: picking toggles and the list stays open.',
        '`clearOption` adds a first option such as "No project"; `onCreate` adds "Create “text”".',
        'Escape or clicking outside closes without changing the value.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown (trigger)', action: 'Opens the picker.' },
        { keys: 'ArrowDown / ArrowUp', action: 'Moves the active record.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten, or to the first or last record.',
        },
        { keys: 'Enter', action: 'Picks or toggles the active record.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
        { keys: 'Tab', action: 'Moves to the footer actions, if any.' },
      ],
    },
    accessibility: {
      role: 'select-only combobox (the trigger) opening a dialog that holds a search combobox and a listbox',
      name: 'The Field label or `aria-label`; the selected value is the trigger’s content.',
      items: [
        'The default trigger follows the APG select-only combobox: `role="combobox"`, named by its label, with `aria-expanded`, `aria-haspopup="dialog"` and `aria-controls`; its text is the value.',
        'The popover is a named dialog; focus moves into the search box and back to the trigger on close.',
        'The search box is an APG combobox with `aria-activedescendant`; groups are labelled `role="group"`.',
        'With `multiple` the listbox is `aria-multiselectable`.',
      ],
    },
    responsive: {
      items: [
        'The popover is at least 16rem wide (or the trigger width) and never wider than the viewport minus 2rem.',
        'The trigger truncates long labels with an ellipsis; the list scrolls within 18rem.',
      ],
    },
    theme: {
      items: [
        'Trigger, panel and options use semantic tokens shared with Input and the menu styles; light, dark and more-contrast themes apply.',
        'The popover entrance animation respects reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Project with groups, clear option and footer',
          stack: true,
          render: () => <ProjectDemo />,
          code: `<Field>
  <FieldLabel>Project</FieldLabel>
  <EntityPicker
    options={projects} // { value, label, hint, group, media }
    value={value}
    onValueChange={setValue}
    placeholder="Choose a project"
    clearOption={{ label: 'No project' }}
    footer={({ close }) => <button onClick={close}>Manage projects</button>}
  />
</Field>`,
        },
        {
          title: 'Multiple with create',
          stack: true,
          render: () => <LabelsDemo />,
          code: `<EntityPicker
  multiple
  options={options}
  values={values}
  onValuesChange={setValues}
  onCreate={(q) => {
    const option = { value: q.toLowerCase(), label: q };
    setOptions((list) => [...list, option]);
    return option;
  }}
/>`,
        },
        {
          title: 'Chip trigger',
          render: () => <ChipDemo />,
          code: `<EntityPicker
  options={projects}
  value={value}
  onValueChange={setValue}
  trigger={<Button size="xs" aria-label={\`\${code}: project \${label}\`}>{code}</Button>}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use `onSearch` with the signal for server-backed records; pass `valueOptions` for selected records not in the results.',
        'Put management actions ("Manage projects", "Invite people") in the footer.',
        'Name a custom trigger with its value and keep its visible text in the name, such as "Open: status".',
      ],
      dont: [
        'Do not build a second picker with checkboxes in a list; use `multiple`.',
        'Do not put product fetching inside a wrapper that refetches on every render; memoise or cache in the product.',
      ],
    },
    related: {
      items: [
        {
          id: 'combobox',
          reason: 'The search and list it is built on; use directly for an input-style field.',
        },
        { id: 'assignee-picker', reason: 'People preset with avatars.' },
        { id: 'select', reason: 'Short lists without search.' },
        { id: 'popover', reason: 'The floating panel.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-popover', 'react'],
      components: ['combobox', 'form-field'],
      notes: ['The trigger uses `.mn-input`; the panel uses `.mn-menu`.'],
    },
    testing: {
      unit: [
        'Opens from a labelled trigger, focuses the search, picks with Enter, closes and refocuses the trigger.',
        'Groups and prefix ranking; Escape closes without a change.',
        'Multiple stays open, toggles and summarises as "first +N".',
        'Clear option, footer with close and query, custom trigger.',
        'Async search plus create keeps the picked label; disabled and invalid from Field.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/entity-picker (closed and open)',
        'Cross-browser axe: /components/entity-picker',
      ],
    },
  },
} satisfies ComponentDocs;
