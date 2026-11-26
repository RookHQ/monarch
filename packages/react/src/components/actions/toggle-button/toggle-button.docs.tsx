import { useState } from 'react';
import { ToggleButton } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { FilterIcon, GridIcon } from '../../../internal/docs-demo.js';

function ControlledDemo() {
  const [archived, setArchived] = useState(false);
  return (
    <>
      <ToggleButton variant="outline" pressed={archived} onPressedChange={setArchived}>
        Show archived
      </ToggleButton>
      <span>{archived ? 'Archived items are listed.' : 'Archived items are hidden.'}</span>
    </>
  );
}

export default {
  id: 'toggle-button',
  title: 'ToggleButton',
  summary: 'A button that stays on or off, announced with aria-pressed.',
  sections: {
    purpose: {
      summary:
        'A single on/off control that looks like a button: a formatting toggle, a "Show archived" filter, a "Pin" action that stays pressed. Needed by: Rook Links (`src/components/ui/toggle.tsx`) and Rook ID (`rookid/components/ui/toggle.tsx`, `rookid/src/components/ui/toggle.tsx`), which use the shadcn/ui Toggle.',
      useWhen: [
        'The control switches something on or off and reads as an action in a toolbar or header.',
        'Several independent toggles sit together: put them in a ToggleGroup for one Tab stop.',
      ],
      avoidWhen: [
        'The setting is a preference saved in a form: use Switch or Checkbox.',
        'One of several mutually exclusive views is chosen: use SegmentedControl or a single ToggleGroup.',
        'The label would change between states ("Mute" / "Unmute"): use a Button whose label changes instead.',
      ],
    },
    designPrinciples: {
      items: [
        'The label names the thing being switched, never the state: assistive technology already reads "pressed".',
        'Pressed uses the accent subtle fill, so it reads as selected without competing with the primary action.',
        'Built on Button, so sizes, icons, loading and focus are identical to other buttons.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<button>`. Every Button prop passes through except `variant`, `asChild`, `brandColor` and `type` (always `button`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui Toggle: `pressed`, `defaultPressed`, `onPressedChange`, `disabled` and `variant` (`default` | `outline`) keep their names. shadcn `size="default"` is `size="md"`; `sm` and `lg` match.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['default', 'outline'],
          description:
            '`default` is borderless (Button ghost) until pressed; `outline` keeps a border (Button secondary) and turns it to the accent border when pressed. Exposed as `data-toggle-variant`.',
        },
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg'],
          description: 'Button heights from the `--mn-control-*` tokens. Default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Off',
          attribute: 'aria-pressed="false", data-state="off"',
          description: 'Muted text on the variant background.',
        },
        {
          name: 'On',
          attribute: 'aria-pressed="true", data-state="on"',
          description: 'Accent subtle background with accent text.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Inherited from Button: 50% opacity, not clickable.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy',
          description: 'Inherited from Button: stays focusable and ignores presses.',
        },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or Space flips the state and calls `onPressedChange` with the new value.',
        'Controlled with `pressed`: the state only changes when the owner passes the new value back.',
        'An `onClick` that calls `event.preventDefault()` cancels the toggle.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the button.' },
        { keys: 'Enter / Space', action: 'Toggles the pressed state.' },
      ],
    },
    accessibility: {
      role: 'button with aria-pressed',
      name: 'The visible label, or `aria-label` for an icon-only toggle.',
      items: [
        '`aria-pressed` is always present (`true` or `false`), so screen readers announce it as a toggle button.',
        'Keep the label constant; a changing label plus `aria-pressed` would announce a contradictory state.',
        'In forced-colors mode the pressed state uses the system Highlight colours.',
      ],
    },
    responsive: {
      items: [
        'On coarse pointers the button is at least 44 × 44 px.',
        'Labels do not wrap; use an icon-only toggle with `aria-label` in tight toolbars.',
      ],
    },
    theme: {
      items: [
        'Pressed colours are `--mn-color-accent-subtle` and `--mn-color-accent-strong`, so each product shows its own accent in light, dark and more-contrast themes.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Default and outline',
          render: () => (
            <>
              <ToggleButton defaultPressed>Bold</ToggleButton>
              <ToggleButton variant="outline">Show archived</ToggleButton>
              <ToggleButton variant="outline" leadingIcon={<FilterIcon />}>
                Filters
              </ToggleButton>
            </>
          ),
        },
        {
          title: 'Icon only',
          render: () => (
            <ToggleButton aria-label="Grid view" defaultPressed>
              <GridIcon />
            </ToggleButton>
          ),
        },
        { title: 'Controlled', render: () => <ControlledDemo /> },
      ],
    },
    doDont: {
      do: [
        'Name the subject: "Show archived", "Bold", "Pin".',
        'Group related toggles in a ToggleGroup so they share one Tab stop.',
      ],
      dont: [
        'Do not switch the label between "On" and "Off" while also using `aria-pressed`.',
        'Do not use a toggle for a setting that is saved with a form: use Switch.',
      ],
    },
    related: {
      items: [
        { id: 'toggle-group', reason: 'Several toggles with one Tab stop and arrow keys.' },
        { id: 'switch', reason: 'An on/off setting in a form or settings list.' },
        { id: 'button', reason: 'A one-off action without a persistent state.' },
        { id: 'segmented-control', reason: 'Choosing one of a few views.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Toggles `aria-pressed` and `data-state` on click, Enter and Space; the accessible name does not change.',
        'Works controlled, and stays unchanged when the owner rejects the change.',
        'Does not toggle while disabled or when `onClick` prevents the default.',
        'Maps `variant="outline"` and keeps Button sizing.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
