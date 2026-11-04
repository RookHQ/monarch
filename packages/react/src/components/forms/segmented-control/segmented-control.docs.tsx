import { useState } from 'react';
import { SegmentedControl, ToggleGroup } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { BoardIcon, GridIcon, ListIcon } from '../../../internal/docs-demo.js';

function ViewSwitch() {
  const [view, setView] = useState<'list' | 'board' | 'grid'>('list');
  return (
    <SegmentedControl
      aria-label="View"
      value={view}
      onValueChange={setView}
      options={[
        { value: 'list', label: 'List', icon: <ListIcon /> },
        { value: 'board', label: 'Board', icon: <BoardIcon /> },
        { value: 'grid', label: 'Grid', icon: <GridIcon /> },
      ]}
    />
  );
}

function Filters() {
  const [value, setValue] = useState<string[]>(['open']);
  return (
    <ToggleGroup
      aria-label="Show"
      size="sm"
      value={value}
      onValueChange={setValue}
      options={[
        { value: 'open', label: 'Open', count: 12 },
        { value: 'waiting', label: 'Waiting', count: 3 },
        { value: 'closed', label: 'Closed', count: 48 },
      ]}
    />
  );
}

export default {
  id: 'segmented-control',
  title: 'SegmentedControl',
  summary:
    'Single choice as a radio group with arrow keys; ToggleGroup for several on/off choices.',
  sections: {
    purpose: {
      summary:
        'Picks one of a few short, mutually exclusive options shown side by side: a view (list or board), a period (monthly or yearly), a scope. ToggleGroup is the same control for several independent on/off options.',
      useWhen: [
        'Two to five short options, all visible at once, and changing them updates the view immediately.',
        'A compact filter set where several options can be on (ToggleGroup).',
      ],
      avoidWhen: [
        'Each option shows its own panel of content: use Tabs.',
        'More than five options, or long labels: use Select.',
        'A single on/off setting: use Switch.',
      ],
    },
    designPrinciples: {
      items: [
        'One ARIA model per meaning: single choice is always a radio group, multiple choice is always toggle buttons. Never a tablist without panels.',
        'One Tab stop for the whole control; arrow keys move inside it.',
        'The selected option is a raised surface with a border, so selection is not shown by shade alone.',
        'Labels stay short; icons help but never replace the accessible name.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Both components take an `options` array of `SegmentedOption` and forward their ref to the group `<div>`.',
        'Both are generic over the option value: `value` and `onValueChange` are typed by the options you pass.',
        'An accessible name is required by the type: `aria-label` or `aria-labelledby`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'SegmentedControl: `value`/`onValueChange(value)` (controlled) or `defaultValue`; `name` adds a hidden input for forms.',
        'ToggleGroup: `value`/`onValueChange(values)` (controlled) or `defaultValue`, as arrays in option order.',
        'SegmentedOption: `value`, `label`, `icon`, `hideLabel` (icon-only), `count`, `suffix`, `disabled`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Control height from `--mn-control-*`. Default `md`.',
        },
        {
          prop: 'block',
          values: ['true', 'false'],
          description: 'Fill the container with equal-width options (`data-block`).',
        },
        {
          prop: 'option.hideLabel',
          values: ['true', 'false'],
          description: 'Icon-only option; the label stays the accessible name and the tooltip.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected / pressed',
          attribute: 'aria-checked="true" or aria-pressed="true", data-state="on"',
          description:
            'Raised surface with a border and shadow, so the state is not shown by colour alone.',
        },
        { name: 'Hover', description: 'Text darkens to the default text colour.' },
        { name: 'Focus', description: 'Focus ring on `:focus-visible`.' },
        {
          name: 'Disabled option or group',
          attribute: 'disabled, data-disabled, aria-disabled',
          description: 'Skipped by arrow keys; the whole group can be disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'SegmentedControl: clicking an option selects it. Moving with the arrow keys also selects (selection follows focus, as in a native radio group).',
        'ToggleGroup: clicking or pressing Space/Enter toggles an option; arrow keys only move focus.',
        'Arrow keys wrap around and skip disabled options. Left and Right follow the text direction.',
        'With `name`, the selected value is submitted with the surrounding form.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Enters the control at the selected option (or the first enabled one), then leaves it. ToggleGroup returns to the last focused option.',
        },
        { keys: '→ / ↓', action: 'Next option (SegmentedControl: and select it).' },
        { keys: '← / ↑', action: 'Previous option (SegmentedControl: and select it).' },
        { keys: 'Home / End', action: 'First / last enabled option.' },
        {
          keys: 'Space',
          action: 'Select (SegmentedControl) or toggle (ToggleGroup) the focused option.',
        },
        { keys: 'Enter', action: 'ToggleGroup: toggle the focused option.' },
      ],
    },
    accessibility: {
      role: 'radiogroup with radio items (SegmentedControl); group with toggle buttons (ToggleGroup)',
      name: '`aria-label` or `aria-labelledby` on the group (required); each option is named by its label, plus its count.',
      items: [
        'Follows the WAI-ARIA APG radio group and toolbar-style roving tabindex patterns.',
        'Icon-only options keep the label as visually hidden text and as a tooltip.',
        'Counts are separated by a space in the accessible name: "Open 12".',
        'On touch devices options are at least 44px tall.',
      ],
    },
    responsive: {
      items: [
        'Never wider than its container: if the options do not fit, the track scrolls horizontally and the focused option scrolls into view.',
        '`block` shares the full width equally, for example in a phone-width dialog.',
        'Taller hit areas under `pointer: coarse`.',
      ],
    },
    theme: {
      items: [
        'Track, surface, border and text are Monarch tokens, so every theme and more-contrast mode applies.',
        'Transitions are removed under `prefers-reduced-motion: reduce`; forced-colours mode outlines the selected option.',
      ],
    },
    examples: {
      items: [
        {
          title: 'View switch',
          render: () => <ViewSwitch />,
        },
        {
          title: 'Sizes, icon-only, suffix',
          stack: true,
          render: () => (
            <>
              <SegmentedControl
                aria-label="Layout"
                size="sm"
                defaultValue="grid"
                options={[
                  { value: 'list', label: 'List', icon: <ListIcon />, hideLabel: true },
                  { value: 'grid', label: 'Grid', icon: <GridIcon />, hideLabel: true },
                ]}
              />
              <SegmentedControl
                aria-label="Billing period"
                size="lg"
                defaultValue="monthly"
                options={[
                  { value: 'monthly', label: 'Monthly' },
                  { value: 'yearly', label: 'Yearly', suffix: '2 months free' },
                ]}
              />
              <SegmentedControl
                aria-label="Range"
                block
                defaultValue="7d"
                options={[
                  { value: '1d', label: '1 day' },
                  { value: '7d', label: '7 days' },
                  { value: '30d', label: '30 days' },
                  { value: '90d', label: '90 days', disabled: true },
                ]}
              />
            </>
          ),
        },
        {
          title: 'ToggleGroup with counts',
          render: () => <Filters />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep labels to one or two words and use the same size for all options.',
        'Name the group ("View", "Billing period") with `aria-label` or a visible heading and `aria-labelledby`.',
        'Use ToggleGroup when options are independent, SegmentedControl when they are exclusive.',
      ],
      dont: [
        'Do not use it as tabs: when each option shows a panel of content, use Tabs.',
        'Do not mix icon-only and labelled options in one control.',
        'Do not trigger slow or destructive work on selection; arrow keys select as they move.',
      ],
    },
    related: {
      items: [
        { id: 'tabs', reason: 'When each option has its own panel.' },
        { id: 'select', reason: 'More than five options or long labels.' },
        { id: 'switch', reason: 'A single on/off setting.' },
        { id: 'filter-bar', reason: 'Filters that combine search, chips and menus.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Labelled radio group with one checked radio.',
        'One Tab stop on the selected option, or the first enabled one.',
        'Arrow keys move and select, wrap, skip disabled options; Home/End; Up/Down.',
        'Left and Right reverse in RTL.',
        'Selects on click and Space; controlled mode follows the value prop only.',
        'Icon-only options and counts in the accessible name; form submission through `name`; disabled group.',
        'ToggleGroup: labelled group of toggle buttons; multi-select in option order; arrows move without toggling; Space and Enter toggle; remembers the Tab stop; controlled value.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/segmented-control'],
    },
  },
} satisfies ComponentDocs;
