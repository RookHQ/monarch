import { useState } from 'react';
import { Tag, TagGroup, type TagProps } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './tag.fixture.json';

const items = fixture.data.items as readonly {
  tone: NonNullable<TagProps['tone']>;
  label: string;
}[];

function RemovableLabels() {
  const [labels, setLabels] = useState(['Design', 'Research', 'Billing', 'Mobile']);
  return (
    <TagGroup label="Labels">
      {labels.map((label) => (
        <Tag key={label} onRemove={() => setLabels((all) => all.filter((l) => l !== label))}>
          {label}
        </Tag>
      ))}
    </TagGroup>
  );
}

function FilterTags() {
  const [on, setOn] = useState<string[]>(['Open']);
  const toggle = (name: string) => (selected: boolean) =>
    setOn((all) => (selected ? [...all, name] : all.filter((n) => n !== name)));
  return (
    <TagGroup label="Status filters">
      {['Open', 'In progress', 'Blocked', 'Done'].map((name) => (
        <Tag key={name} selected={on.includes(name)} onSelectedChange={toggle(name)}>
          {name}
        </Tag>
      ))}
    </TagGroup>
  );
}

export default {
  id: 'tag',
  title: 'Tag',
  summary: 'A chip people act on: removable labels and selectable filters, distinct from Badge.',
  sections: {
    purpose: {
      summary:
        'Shows a piece of user data as a chip that people can remove or switch on and off: labels on a record, chosen recipients, quick filters. Needed by: Rook Calendar (`src/components/booking-pages/EditorSections.tsx` builds removable "Also notify" recipient chips by hand, a rounded `<li>` with an icon remove button).',
      useWhen: [
        'Labels or categories attached to a record that people can remove.',
        'A row of quick filters that people switch on and off (`selected`).',
        'Showing the values chosen in a picker, each removable.',
      ],
      avoidWhen: [
        'A read-only status or count: use Badge (or StatusBadge). Badge is never interactive; Tag is for things people act on.',
        'Typing new tags: use TagInput, which manages the text field and its chips.',
        'Choosing exactly one option from a few: use SegmentedControl or ToggleGroup.',
      ],
    },
    designPrinciples: {
      items: [
        'Tag vs Badge: Badge labels a status the system decides; Tag holds data the user owns and can change.',
        'Every action is a real button with a name: the toggle is `aria-pressed`, the remove button says "Remove {label}".',
        'Selected is not colour alone: a check mark appears and the border changes.',
        'Removing never loses focus inside a TagGroup.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Tag` forwards its ref to the root `<span>`; `TagGroup` to its `<div role="group">`.',
        'A Tag is selectable when any of `selected`, `defaultSelected` or `onSelectedChange` is given, and removable when `onRemove` is given.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from hand-built chips (`<span>{label}<button><X/></button></span>`): use `<Tag onRemove>` and drop the icon button; the name, focus ring and touch target come with it.',
        'Migrating from a shadcn/ui `Badge` with an `onClick` or a close icon: interactive chips become Tag; badges that only show status stay Badge.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            'Colour role of a static or removable tag. Default `neutral`. Selectable tags use the outline and accent look instead. `data-tone`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`md` (default) or `sm` for dense rows. `data-size`.',
        },
        {
          prop: 'icon',
          values: ['ReactNode'],
          description: 'A decorative icon before the label.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-selected, aria-pressed="true"',
          description: 'Accent border and fill with a check mark.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'The label is a toggle button.',
        },
        {
          name: 'Removable',
          attribute: 'data-removable',
          description: 'A remove button follows the label.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled',
          description: 'Both buttons are disabled; Backspace and Delete do nothing.',
        },
        { name: 'Focus', description: 'Each button shows the focus ring on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Activating a selectable tag toggles it and calls `onSelectedChange`.',
        'The remove button, Backspace or Delete call `onRemove`; the caller removes the tag from its data.',
        'Inside a TagGroup, after a removal focus moves to the tag now in the same place, else the previous one, else the group.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the toggle, then to the remove button of each tag.' },
        {
          keys: 'Enter / Space',
          action: 'Toggles a selectable tag, or presses the remove button.',
        },
        { keys: 'Backspace / Delete', action: 'Removes the focused tag when it is removable.' },
      ],
    },
    accessibility: {
      role: 'button with aria-pressed (selectable); group (TagGroup)',
      name: 'The tag text; the remove button is "Remove {label}" (`messages.remove`). TagGroup takes `label`.',
      items: [
        'Icons and the check mark are `aria-hidden`.',
        'A static tag is plain text, not a button, so it adds nothing to the tab order.',
        'TagGroup is focusable programmatically (`tabindex="-1"`) so focus has somewhere to go when the last tag is removed.',
      ],
    },
    responsive: {
      items: [
        'Tags wrap inside a TagGroup; a long label ends in an ellipsis instead of overflowing at 390px.',
        'On coarse pointers the toggle and remove button get a 44px hit area through an invisible extension, and the group gap grows.',
        'Padding and the remove button use logical properties, so right-to-left layouts mirror.',
      ],
    },
    theme: {
      items: [
        'Tones use the `subtle`, `strong` and `border` status tokens; selected uses `--mn-color-accent-subtle` and `--mn-color-accent-solid`.',
        'In forced colours borders and text use `CanvasText`, buttons `ButtonText`, selected a 2px `Highlight` border, disabled `GrayText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Tones',
          render: () => (
            <>
              {items.map((item) => (
                <Tag key={item.label} tone={item.tone}>
                  {item.label}
                </Tag>
              ))}
            </>
          ),
        },
        { title: 'Removable labels', render: () => <RemovableLabels /> },
        { title: 'Selectable filters', render: () => <FilterTags /> },
        {
          title: 'Small and disabled',
          render: () => (
            <>
              <Tag size="sm" onRemove={() => {}}>
                Small
              </Tag>
              <Tag disabled onRemove={() => {}}>
                Locked
              </Tag>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Wrap tags in a TagGroup with a `label` so the set has a name and removal keeps focus.',
        'Keep labels short; one or two words.',
        'Use selectable tags for filters that combine freely.',
      ],
      dont: [
        'Don’t use Tag for a status the user cannot change; use Badge.',
        'Don’t rely on tone alone to mean something; the label carries the meaning.',
        'Don’t put links or other buttons inside a tag.',
      ],
    },
    related: {
      items: [
        { id: 'badge', reason: 'Read-only status and metadata labels.' },
        { id: 'tag-input', reason: 'Typing and editing a list of tags in a field.' },
        { id: 'toggle-group', reason: 'A set of toggle buttons with arrow-key navigation.' },
        { id: 'multi-select', reason: 'Choosing several values from a long list.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders a static tag with tone and size, no buttons, and forwards its ref.',
        'Removable: named remove button, Backspace and Delete call `onRemove`.',
        'Selectable: a toggle button with `aria-pressed`, a check mark, Enter and Space.',
        'Selectable and removable at once, each with its own tab stop.',
        'Disabled tags neither toggle nor remove.',
        'Translates the remove label and falls back for non-text labels.',
        'TagGroup is a named group and moves focus to the next, previous or group after a removal.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
