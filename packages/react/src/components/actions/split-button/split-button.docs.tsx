import { DropdownMenuItem, SplitButton } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { ClockIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'split-button',
  title: 'SplitButton',
  summary: 'A main action joined to a menu of alternatives.',
  sections: {
    purpose: {
      summary:
        'Offers one obvious default action and keeps its variations one click away: "Save" with "Save as draft", "Send" with "Schedule send", "Export CSV" with other formats. Needed by: none recorded; no Rook app has a split button component yet.',
      useWhen: [
        'One action is used most of the time and two to six alternatives are variations of it.',
        'The alternatives are actions, not settings.',
      ],
      avoidWhen: [
        'There is no clear default: use MenuButton.',
        'The alternatives change what the main button does next time: use a Select next to a Button.',
        'There is only one alternative: show two buttons.',
      ],
    },
    designPrinciples: {
      items: [
        'The main half acts immediately; only the arrow opens a menu.',
        'Both halves share variant, size and disabled state so they read as one control.',
        'The arrow has its own accessible name that mentions the main action ("More options for Save").',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and Button props (`onClick`, `variant`, `size`, `loading`, `disabled`, `type`, …) go to the main button; `className` and `style` go to the wrapper.',
        'Alternatives come from `items` (MenuButtonEntry, shared with MenuButton) and/or `menuContent` (DropdownMenu children).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages.moreOptions` is a string or a function of the main label, so translations can place the label anywhere.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: [
            'primary',
            'secondary',
            'ghost',
            'danger',
            'danger-subtle',
            'success',
            'warning',
          ],
          description:
            'Applied to both halves. Solid variants get a surface-coloured hairline between them. Exposed as `data-variant` on the wrapper.',
        },
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg'],
          description: 'Applied to both halves. Default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Menu open',
          attribute: 'aria-expanded="true", data-state="open" on the arrow',
          description: 'The chevron turns 180°.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled on both buttons',
          description: 'Neither the action nor the menu is available.',
        },
        {
          name: 'Menu disabled',
          attribute: 'disabled on the arrow',
          description: 'The main action works; the alternatives are unavailable.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy on the main button',
          description:
            'The main button shows a spinner; the arrow stays focusable but does not open the menu.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the main half calls `onClick`.',
        'Clicking the arrow, or Enter, Space or Arrow Down on it, opens the menu on its first item; choosing an item runs its `onSelect` and returns focus to the arrow.',
        'The menu is aligned to the end of the control by default, under the arrow.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves from the main button to the arrow (two Tab stops).' },
        { keys: 'Enter / Space', action: 'On the main button: runs the action.' },
        {
          keys: 'Enter / Space / Arrow Down',
          action: 'On the arrow: opens the menu on its first item.',
        },
        { keys: 'Arrow keys, Home, End, letters', action: 'Move inside the open menu.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus to the arrow.' },
      ],
    },
    accessibility: {
      role: 'group containing a button and a menu button',
      name: 'The group and main button are named by the label; the arrow by `messages.moreOptions`.',
      items: [
        'The arrow follows the WAI-ARIA menu button pattern (`aria-haspopup="menu"`, `aria-expanded`).',
        'The chevron is decorative; the arrow’s name is set explicitly and shown as a tooltip.',
      ],
    },
    responsive: {
      items: [
        'Both halves are at least 44 × 44 px on coarse pointers.',
        'The main label can shrink but does not wrap; keep it short at 390 px.',
      ],
    },
    theme: {
      items: [
        'Uses Button tokens for every variant; the separator hairline uses `--mn-color-bg-surface`, so it adapts to dark and more-contrast themes.',
        'In forced-colors mode both halves show ButtonText borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Primary with alternatives',
          render: () => (
            <SplitButton
              variant="primary"
              items={[{ label: 'Save as draft' }, { label: 'Save and close' }]}
            >
              Save
            </SplitButton>
          ),
        },
        {
          title: 'Menu content as children',
          render: () => (
            <SplitButton
              menuContent={<DropdownMenuItem icon={<ClockIcon />}>Schedule send</DropdownMenuItem>}
            >
              Send
            </SplitButton>
          ),
        },
        {
          title: 'Sizes and states',
          render: () => (
            <>
              <SplitButton size="sm" items={[{ label: 'Export PDF' }]}>
                Export CSV
              </SplitButton>
              <SplitButton loading items={[{ label: 'Publish later' }]}>
                Publish
              </SplitButton>
              <SplitButton menuDisabled items={[{ label: 'Merge' }]}>
                Approve
              </SplitButton>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Make the main half the action most people want.',
        'Keep menu items as variations of the main action.',
      ],
      dont: [
        'Do not put unrelated or destructive actions in the menu.',
        'Do not change the main action to the last item chosen; people lose track of what it does.',
      ],
    },
    related: {
      items: [
        { id: 'menu-button', reason: 'A menu without a default action.' },
        { id: 'button-group', reason: 'Several equal actions joined together.' },
        { id: 'dropdown-menu', reason: 'The menu primitives.' },
        { id: 'button', reason: 'A single action.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'menu-button', 'dropdown-menu'],
    },
    testing: {
      unit: [
        'Main half runs `onClick`; the arrow is a named menu button that opens on Enter with focus on the first item, runs the chosen item and returns focus.',
        'Accepts `menuContent`, custom `messages` and a wrapper class name.',
        '`disabled` disables both halves; `menuDisabled` only the arrow.',
        'While loading, the arrow stays focusable but does not open.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
