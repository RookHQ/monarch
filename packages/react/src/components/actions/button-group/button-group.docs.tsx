import { Button, ButtonGroup, IconButton } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PlusIcon } from '../../../internal/docs-demo.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './button-group.fixture.json';

export default {
  id: 'button-group',
  title: 'ButtonGroup',
  summary: 'Visually joined related buttons in a labelled group.',
  sections: {
    purpose: {
      summary:
        'Joins a few related buttons into one visual unit (Previous / Next, Zoom in / Zoom out, Day / Week / Month links) and groups them for assistive technology. Needed by: none recorded; no Rook app has a shadcn/ui ButtonGroup copy yet.',
      useWhen: [
        'Two to five buttons act on the same thing and read better as one control.',
        'A primary action sits next to an icon button that belongs to it.',
      ],
      avoidWhen: [
        'The buttons have a pressed state: use ToggleGroup.',
        'The group needs arrow-key navigation, as in an editor toolbar: use Toolbar.',
        'A main action has alternatives in a menu: use SplitButton.',
      ],
    },
    designPrinciples: {
      items: [
        'Layout only: each button keeps its own variant, size, label and Tab stop.',
        'Joined by default; `attached={false}` gives the same grouping with a gap.',
        'Joins with logical properties, so the squared corners are correct in right-to-left layouts.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the group `<div>`; every native attribute passes through.',
        'Only direct children are joined, so wrap nothing between the group and its buttons.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui ButtonGroup: `orientation` keeps its name. Separators are not needed; borders are shared automatically.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical'],
          description: 'Row or column. Exposed as `data-orientation`.',
        },
        {
          prop: 'attached',
          values: ['true', 'false'],
          description:
            'Joined borders and squared inner corners (default), or separate buttons with a gap. Exposed as `data-attached`.',
        },
        {
          prop: 'block',
          values: ['true', 'false'],
          description: 'Fills the container and shares the width equally (`data-block`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Hover and focus',
          description:
            'The hovered or focused button is raised above its neighbours, so its border and focus ring are fully visible.',
        },
      ],
      notes: ['Button states (disabled, loading) belong to each button.'],
    },
    interaction: {
      items: [
        'No behaviour of its own: clicks go to the buttons.',
        'Joined buttons overlap by one pixel so shared borders are a single line.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab / Shift+Tab', action: 'Moves through the buttons one at a time.' }],
      notes: ['Arrow keys are not handled; use Toolbar or ToggleGroup for a single Tab stop.'],
    },
    accessibility: {
      role: 'group',
      name: '`aria-label` or `aria-labelledby`, when the buttons need context ("Pages", "Zoom").',
      items: [
        'Screen readers announce the group name when focus enters the first button.',
        'Each button keeps its own accessible name; icon-only buttons need `IconButton label`.',
      ],
    },
    responsive: {
      items: [
        'Buttons are at least 44 × 44 px on coarse pointers.',
        'Use `orientation="vertical"` or `block` when a row would overflow at 390 px.',
      ],
    },
    theme: {
      items: [
        'No colours of its own beyond a surface-coloured hairline between joined solid buttons; button tokens apply in every theme.',
        'In forced-colors mode joined buttons show ButtonText borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Joined',
          render: () => (
            <ButtonGroup aria-label={fixture.data.label}>
              <Button>{fixture.data.buttons[0]}</Button>
              <Button>{fixture.data.buttons[1]}</Button>
            </ButtonGroup>
          ),
        },
        {
          title: 'Primary with an icon button',
          render: () => (
            <ButtonGroup aria-label="New item">
              <Button variant="primary" leadingIcon={<PlusIcon />}>
                New item
              </Button>
              <IconButton variant="primary" label="More ways to add" icon={<MoreIcon />} />
            </ButtonGroup>
          ),
        },
        {
          title: 'Vertical and spaced',
          render: () => (
            <>
              <ButtonGroup aria-label="Zoom" orientation="vertical">
                <Button size="sm">Zoom in</Button>
                <Button size="sm">Zoom out</Button>
              </ButtonGroup>
              <ButtonGroup aria-label="Range" attached={false}>
                <Button size="sm">Day</Button>
                <Button size="sm">Week</Button>
                <Button size="sm">Month</Button>
              </ButtonGroup>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep groups short and the buttons the same size.',
        'Name the group when the buttons do not explain themselves.',
      ],
      dont: [
        'Do not use it to show which option is selected: use ToggleGroup or SegmentedControl.',
        'Do not put two primary buttons side by side unless they are one split action.',
      ],
    },
    related: {
      items: [
        { id: 'toggle-group', reason: 'Joined buttons with a pressed state and one Tab stop.' },
        { id: 'split-button', reason: 'A main action with a menu of alternatives.' },
        { id: 'menu-button', reason: 'A button that opens a menu of actions.' },
        { id: 'button', reason: 'The buttons inside the group.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Is a labelled group; each button keeps its own Tab stop and activates normally.',
        'Exposes orientation, attached and block as data attributes.',
        'Forwards its ref and native attributes.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
