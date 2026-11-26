import { useState } from 'react';
import {
  PRIORITY_LEVELS,
  PriorityGlyph,
  PriorityPicker,
  type PriorityLevel,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function PriorityDemo({ compact }: { compact?: boolean }) {
  const [value, setValue] = useState<PriorityLevel>('medium');
  return (
    <PriorityPicker
      value={value}
      onValueChange={setValue}
      showLabel={!compact}
      size={compact ? 'sm' : 'md'}
    />
  );
}

export default {
  id: 'priority-picker',
  title: 'PriorityPicker',
  summary:
    'A menu button for issue priority; every level has its own shape and name, never colour only.',
  sections: {
    purpose: {
      summary:
        'Shows and changes how urgent an issue is, on the urgent / high / medium / low / none scale used by Rook Stride. Needed by: Rook Stride (`components/issues/pickers.tsx` `PriorityPicker`, `PriorityIcon`; `lib/utils/stride.ts` `PRIORITY_META`), Rook Tickets.',
      useWhen: ['Issue rows, cards and detail panels where priority can be changed.'],
      avoidWhen: [
        'Read-only priority: use PriorityGlyph with the level name.',
        'Severity of incidents or alerts: use the status tones of Alert or StatusBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'Shape first: an exclamation tile, three, two or one filled bars, or dashes, so priority reads in greyscale.',
        'Colour adds emphasis only to urgent (danger) and high (warning).',
        'Same menu-button pattern as StatusPicker, with `menuitemradio` items.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`priorityFromNumber(n)` and `priorityToNumber(level)` convert the 0–4 scale (0 none, 1 urgent … 4 low) stored by Stride.',
        '`PriorityGlyph({ priority })` and `PRIORITY_LEVELS` are exported.',
        'The ref is the trigger `<button>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride’s `PriorityPicker({ value: number | null, onChange })`: `value={priorityFromNumber(issue.priority)}` and `onValueChange={(l) => onChange(priorityToNumber(l))}`; `PRIORITY_META` labels become `messages.levels`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showLabel',
          values: ['true', 'false'],
          description: 'Name and chevron (default), or glyph only.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '24 or 32px trigger (44px on coarse pointers).',
        },
        {
          prop: 'levels',
          values: [...PRIORITY_LEVELS],
          description: 'Offer a subset, in your order.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Priority', attribute: 'data-priority', description: 'The current level.' },
        {
          name: 'Icon only',
          attribute: 'data-icon-only',
          description: 'No visible name; the accessible name still says it.',
        },
        { name: 'Disabled', description: 'The trigger is disabled.' },
      ],
    },
    interaction: { items: ['Choosing a level calls `onValueChange` and closes the menu.'] },
    keyboard: {
      items: [
        { keys: 'Enter / Space / Arrow Down', action: 'Open the menu.' },
        { keys: 'Arrow Up / Down', action: 'Move between levels.' },
        { keys: 'Letters', action: 'Jump to a level by name.' },
        { keys: 'Enter / Space', action: 'Choose the level.' },
        { keys: 'Escape', action: 'Close and return focus.' },
      ],
    },
    accessibility: {
      role: 'button with `aria-haspopup="menu"`; menu of menuitemradio',
      name: '“Priority: {level}”.',
      items: [
        'Glyphs are decorative; level names carry the meaning.',
        'The current level is checked.',
      ],
    },
    responsive: { items: ['Compact trigger; 44px on coarse pointers.'] },
    theme: {
      items: [
        'Urgent `--mn-color-danger-strong`, high `--mn-color-warning-strong`, medium and low `--mn-color-text-default`, none `--mn-color-text-muted`.',
        'Forced colours: `CanvasText` glyphs; shapes still differ.',
      ],
    },
    examples: {
      items: [
        { title: 'Priority picker', render: () => <PriorityDemo /> },
        { title: 'Glyph only', render: () => <PriorityDemo compact /> },
        {
          title: 'Read-only glyphs with names',
          render: () => (
            <ul style={{ display: 'grid', gap: 6, margin: 0, padding: 0, listStyle: 'none' }}>
              {PRIORITY_LEVELS.map((p) => (
                <li key={p} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <PriorityGlyph priority={p} />
                  {p}
                </li>
              ))}
            </ul>
          ),
        },
      ],
    },
    doDont: {
      do: ['Use the same scale across products so filters and reports agree.'],
      dont: [
        'Do not show priority as a coloured dot alone.',
        'Do not add levels between the five.',
      ],
    },
    related: {
      items: [
        { id: 'status-picker', reason: 'The same pattern for status.' },
        { id: 'issue-card', reason: 'Shows priority with the issue.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dropdown-menu'] },
    testing: {
      unit: [
        'Number conversion round trip.',
        'Trigger name and data attribute.',
        'Keyboard open, item order, checked state, choose.',
        'Subset of levels, glyph-only name, translated and disabled.',
        'Distinct shape per level; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
