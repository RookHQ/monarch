import { useState } from 'react';
import { ToggleGroup, ToggleGroupItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { BoardIcon, GridIcon, ListIcon } from '../../../internal/docs-demo.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './toggle-group.fixture.json';

const data = fixture.data as unknown as {
  label: string;
  selected: string[];
  options: { value: string; label: string }[];
};

function SingleDemo() {
  const [view, setView] = useState<string>('list');
  return (
    <>
      <ToggleGroup type="single" required aria-label="View" value={view} onValueChange={setView}>
        <ToggleGroupItem value="list" aria-label="List">
          <ListIcon />
        </ToggleGroupItem>
        <ToggleGroupItem value="board" aria-label="Board">
          <BoardIcon />
        </ToggleGroupItem>
        <ToggleGroupItem value="grid" aria-label="Grid">
          <GridIcon />
        </ToggleGroupItem>
      </ToggleGroup>
      <span>Showing the {view} view.</span>
    </>
  );
}

export default {
  id: 'toggle-group',
  title: 'ToggleGroup',
  summary: 'Toggle buttons with one Tab stop: single or multiple pressed items.',
  sections: {
    purpose: {
      summary:
        'Groups related toggle buttons, such as text formatting, list filters or view options, into one keyboard stop. Needed by: Rook Links (`src/components/ui/toggle-group.tsx`) and Rook ID (`rookid/components/ui/toggle-group.tsx`, `rookid/src/components/ui/toggle-group.tsx`), which use the shadcn/ui ToggleGroup.',
      useWhen: [
        'Several independent on/off options belong together (Bold, Italic, Underline).',
        'A toolbar-style choice where at most one option is pressed and none is allowed (`type="single"`).',
      ],
      avoidWhen: [
        'Exactly one of a few views must always be chosen and shown as a track: use SegmentedControl.',
        'The options are form settings saved with a Save button: use CheckboxGroup or RadioGroup.',
        'There are more than about seven options: use a Select or MultiSelect.',
      ],
    },
    designPrinciples: {
      items: [
        'One Tab stop for the whole group; arrow keys move inside it, as in the WAI-ARIA toolbar pattern.',
        'Every item is a ToggleButton, so pressed styling and sizes match standalone toggles.',
        'The group always has an accessible name; the type system requires `aria-label` or `aria-labelledby`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'ToggleGroup forwards its ref to the group `<div>`; ToggleGroupItem forwards its ref to its `<button>`.',
        'Pass items as `ToggleGroupItem` children, or as `options` (the same shape as SegmentedControl options).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`type="single"`: `value` is a string, `""` when nothing is pressed. `type="multiple"` (default): `value` is an array.',
        'Migrating from shadcn/ui ToggleGroup: `type`, `value`, `defaultValue`, `onValueChange`, `disabled`, `variant`, `size`, `orientation`, `loop` and `dir` keep their names, and items stay `ToggleGroupItem value=…`. Add an `aria-label` to the group. shadcn `size="default"` is `md`. `rovingFocus={false}` has no equivalent: roving focus is always on.',
        'Migrating from the earlier Monarch `ToggleGroup` in segmented-control: the `options`, `value`, `defaultValue`, `onValueChange`, `size`, `disabled` and `dir` props work unchanged; `block` is not supported.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'type',
          values: ['multiple', 'single'],
          description:
            '`multiple` (default) lets any number be pressed; `single` keeps at most one. Exposed as `data-type`.',
        },
        {
          prop: 'variant',
          values: ['default', 'outline'],
          description: 'Passed to every ToggleButton item.',
        },
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg'],
          description: 'Passed to every item. Default `md`.',
        },
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical'],
          description:
            'Lays items out in a row or a column and picks the arrow keys. Exposed as `data-orientation`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pressed item',
          attribute: 'aria-pressed="true", data-state="on"',
          description: 'Accent subtle background.',
        },
        {
          name: 'Tab stop',
          attribute: 'tabindex="0"',
          description:
            'The last focused item, else the first pressed one, else the first enabled one. Others have tabindex -1.',
        },
        {
          name: 'Disabled',
          attribute: 'aria-disabled on the group, disabled on items',
          description: 'Disabled items are skipped by the arrow keys.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking an item toggles it. In `single` mode pressing another item releases the previous one; pressing the pressed item releases it unless `required` is set.',
        'With `options`, the value array is kept in option order.',
        'Focus wraps from the last to the first item unless `loop={false}`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves into and out of the group (one stop).' },
        {
          keys: 'Arrow Right / Arrow Left',
          action: 'Next or previous item when horizontal; reversed in right-to-left.',
        },
        { keys: 'Arrow Down / Arrow Up', action: 'Next or previous item when vertical.' },
        { keys: 'Home / End', action: 'First or last enabled item.' },
        { keys: 'Enter / Space', action: 'Toggles the focused item.' },
      ],
    },
    accessibility: {
      role: 'group containing buttons with aria-pressed',
      name: '`aria-label` or `aria-labelledby` on the group (required by the type); each item’s text or `aria-label`.',
      items: [
        'Follows the WAI-ARIA toolbar keyboard model: one Tab stop with roving `tabindex`.',
        'Disabled items are not focusable and are skipped by arrow keys.',
        'On the server every item is a tab stop, so the group stays reachable before hydration.',
        'Icon-only options from `options` use `label` as the accessible name and tooltip.',
      ],
    },
    responsive: {
      items: [
        'Horizontal groups wrap onto a second line instead of scrolling at narrow widths.',
        'Items are at least 44 × 44 px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Uses ToggleButton colours: accent subtle and accent strong when pressed, so each product’s accent applies in every theme.',
        'In forced-colors mode pressed items use Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Multiple (formatting)',
          render: () => (
            <ToggleGroup aria-label={data.label} defaultValue={data.selected}>
              {data.options.map((o) => (
                <ToggleGroupItem key={o.value} value={o.value}>
                  {o.label}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          ),
        },
        { title: 'Single, required (view)', render: () => <SingleDemo /> },
        {
          title: 'Options shorthand, outline',
          render: () => (
            <ToggleGroup
              aria-label="Status filters"
              variant="outline"
              size="sm"
              defaultValue={['open']}
              options={[
                { value: 'open', label: 'Open', count: 12 },
                { value: 'pending', label: 'Pending', count: 4 },
                { value: 'closed', label: 'Closed', disabled: true },
              ]}
            />
          ),
        },
        {
          title: 'Vertical',
          render: () => (
            <ToggleGroup aria-label="Map layers" orientation="vertical" defaultValue={['roads']}>
              <ToggleGroupItem value="roads">Roads</ToggleGroupItem>
              <ToggleGroupItem value="rivers">Rivers</ToggleGroupItem>
              <ToggleGroupItem value="labels">Labels</ToggleGroupItem>
            </ToggleGroup>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Label the group ("Text style", "Status filters").',
        'Use `type="single"` with `required` when a choice must always be made but the control should look like toggles.',
      ],
      dont: [
        'Do not mix actions (Delete) with toggles in one group; put actions in a ButtonGroup or Toolbar.',
        'Do not change item labels when they are pressed.',
      ],
    },
    related: {
      items: [
        { id: 'toggle-button', reason: 'A single toggle.' },
        { id: 'segmented-control', reason: 'One required choice shown as a track.' },
        { id: 'button-group', reason: 'Joined action buttons without a pressed state.' },
        { id: 'checkbox-group', reason: 'Multiple choices inside a form.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['toggle-button'] },
    testing: {
      unit: [
        'Labelled group, multiple by default, with `aria-pressed` items and `onValueChange` arrays.',
        'One Tab stop; arrows, Home and End move and skip disabled items; focus wraps; Shift+Tab returns to the last focused item.',
        'Single type keeps one pressed, releases on a second press, and not when `required`.',
        'Vertical orientation uses Up/Down; right-to-left reverses Left/Right; `loop={false}` stops at the ends.',
        '`options` shorthand renders icon-only labels and counts and keeps option order.',
        '`disabled` disables every item.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
