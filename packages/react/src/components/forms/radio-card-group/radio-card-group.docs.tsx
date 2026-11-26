import { useState } from 'react';
import {
  Field,
  FieldDescription,
  FieldLabel,
  RadioCardGroup,
  type RadioCardGroupProps,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { BoardIcon, CalendarIcon, ListIcon, UsersIcon } from '../../../internal/docs-demo.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './radio-card-group.fixture.json';

function ChannelType() {
  const [value, setValue] = useState('open');
  return (
    <Field>
      <FieldLabel>Who can join</FieldLabel>
      <FieldDescription>You can change this later in the channel settings.</FieldDescription>
      <RadioCardGroup
        name="visibility"
        value={value}
        onValueChange={setValue}
        options={[
          {
            value: 'open',
            title: 'Open',
            description: 'Anyone in the workspace can find and join.',
            icon: <UsersIcon />,
          },
          {
            value: 'invite',
            title: 'Invite only',
            description: 'Only people you add can see the channel.',
            icon: <CalendarIcon />,
          },
        ]}
      />
    </Field>
  );
}

export default {
  id: 'radio-card-group',
  title: 'RadioCardGroup',
  summary: 'One choice from a few options shown as selectable cards with title, text and icon.',
  sections: {
    purpose: {
      summary:
        'Presents a small set of exclusive options as cards, each with a title, a description, an optional icon and an optional meta such as a price, so people can compare them before choosing. Needed by: Rook Chat (channel visibility in CreateChannelDialog, hand-built `sr-only` radio cards), Rook Calendar and Rook Developers settings that pick a mode with an explanation.',
      useWhen: [
        'Two to six options that each need a sentence of explanation or a price to compare.',
        'A key decision in a setup flow (plan, delivery, visibility, layout).',
      ],
      avoidWhen: [
        'Short labels with no explanation: use RadioGroup.',
        'An immediately applied view switch: use SegmentedControl.',
        'Several cards can be selected: use CheckboxGroup.',
        'Long lists: use Select or Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on RadioGroup: the same native radios, keyboard model, form submission and Field wiring, with a card layout.',
        'The whole card is the click target; a visible radio still shows the selection, so it is never colour alone.',
        'The title is the accessible name; the meta and description are the description, so screen readers hear the name first.',
        'Cards stack on narrow screens regardless of `columns`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `role="radiogroup"` `<div>` rendered by RadioGroup.',
        'Takes every RadioGroup prop except `options`, `children` and `orientation`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`options` is an array of `RadioCardOption`: `value`, `title`, `description`, `icon`, `meta`, `disabled`.',
        '`value` + `onValueChange` (controlled) or `defaultValue`; `name` submits the value with a form.',
        'Migrating from shadcn/ui: apps build radio cards from `RadioGroupItem` plus a styled `<Label>` (or an `sr-only` input inside a label). Move each card’s heading to `title`, its hint to `description`, its icon to `icon`, and drop the hand-written checked styles.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['1', '2', '3'],
          description:
            'Cards per row from 40rem wide (`data-columns`). Default `1`. Below 40rem cards always stack.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: 'data-state="checked" on the card, :checked on the radio',
          description: 'Accent border, accent-subtle surface and a filled radio.',
        },
        { name: 'Hover', description: 'Stronger border.' },
        {
          name: 'Focus',
          attribute: ':has(:focus-visible)',
          description: 'The focus ring is drawn around the whole card.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Muted surface and text; arrow keys skip the card.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid and aria-invalid on the group',
          description: 'Danger-coloured card borders; the Field error is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on a card selects it and calls `onValueChange`.',
        'Arrow keys move between cards and select, skipping disabled ones and wrapping.',
        'With `name`, the selected value submits with the surrounding form.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves into the group at the selected card (or the first enabled one), then out.',
        },
        { keys: '↓ / →', action: 'Next enabled card, selected.' },
        { keys: '↑ / ←', action: 'Previous enabled card, selected.' },
        { keys: 'Space', action: 'Selects the focused card when none is selected.' },
      ],
    },
    accessibility: {
      role: 'radiogroup containing native radio inputs',
      name: 'Group: the `label` prop, the Field’s FieldLabel or `aria-label`. Each card: its title (`aria-labelledby`).',
      items: [
        'Follows the WAI-ARIA APG radio group pattern with native radios.',
        'Meta and description are linked to the radio with `aria-describedby`.',
        'Icons are decorative (`aria-hidden`).',
        'Cards are at least 44px tall, so they meet touch target size.',
      ],
    },
    responsive: {
      items: [
        'One column under 40rem, `columns` from 40rem; no horizontal scroll at 390px.',
        'Titles, meta and descriptions wrap; the meta drops under the title when space runs out.',
      ],
    },
    theme: {
      items: [
        'Borders, surfaces, accent and text are Monarch tokens, so every theme and dark mode apply.',
        'Forced-colours mode outlines the selected card with Highlight; transitions are removed under `prefers-reduced-motion: reduce`.',
        'Logical spacing mirrors the layout in RTL.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With meta and description',
          render: () => (
            <RadioCardGroup
              label={fixture.data.label}
              defaultValue={fixture.data.selected}
              options={fixture.data.options as unknown as RadioCardGroupProps['options']}
            />
          ),
        },
        { title: 'In a Field with icons', render: () => <ChannelType /> },
        {
          title: 'Two columns',
          stack: true,
          render: () => (
            <RadioCardGroup
              label="Default layout"
              columns={2}
              defaultValue="list"
              options={[
                {
                  value: 'list',
                  title: 'List',
                  description: 'Dense rows, best for scanning.',
                  icon: <ListIcon />,
                },
                {
                  value: 'board',
                  title: 'Board',
                  description: 'Columns by status.',
                  icon: <BoardIcon />,
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep titles short and parallel, and use the description for the difference between options.',
        'Preselect the recommended option when there is one.',
        'Use the same kind of meta on every card (all prices, or none).',
      ],
      dont: [
        'Do not put links or buttons inside a card: the whole card is a label.',
        'Do not use more than about six cards; use Select.',
        'Do not rely on the icon alone to tell options apart.',
      ],
    },
    related: {
      items: [
        { id: 'radio-group', reason: 'The same choice as a compact list.' },
        { id: 'segmented-control', reason: 'An immediately applied view switch.' },
        { id: 'checkbox-group', reason: 'Several options can be chosen.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['radio-group'],
    },
    testing: {
      unit: [
        'Labelled radiogroup; cards named by title and described by meta and description; decorative icon.',
        'Clicking any part of a card selects it.',
        'Arrow keys move and select, skip disabled cards and wrap.',
        'Field integration (label, invalid, error), `columns`, and form submission under `name`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
