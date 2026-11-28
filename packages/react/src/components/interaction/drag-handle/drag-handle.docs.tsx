import { useState, type KeyboardEvent } from 'react';
import { DragHandle, type DragHandleProps, type DragHandleState } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './drag-handle.fixture.json';

const handles = fixture.data.handles as readonly {
  label: string;
  state: string;
  axis: string;
  size: string;
  disabled?: boolean;
}[];

function StatesDemo() {
  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
      {handles.map((handle) => (
        <DragHandle
          key={handle.label}
          label={handle.label}
          state={handle.state as DragHandleState}
          axis={handle.axis as NonNullable<DragHandleProps['axis']>}
          size={handle.size as NonNullable<DragHandleProps['size']>}
          {...(handle.disabled && { disabled: true })}
        />
      ))}
    </div>
  );
}

function KeyboardDemo() {
  const [state, setState] = useState<DragHandleState>('idle');
  const onKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      setState((s) => (s === 'grabbed' ? 'dropped' : 'grabbed'));
    } else if (e.key === 'Escape') setState('idle');
  };
  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      <DragHandle label="Reorder Weekly report" state={state} onKeyDown={onKeyDown} />
      <span style={{ fontSize: 'var(--mn-text-sm)' }}>Weekly report ({state})</span>
    </div>
  );
}

export default {
  id: 'drag-handle',
  title: 'DragHandle',
  summary:
    'The accessible grip button an item is dragged by, with idle, picked-up and dropped states.',
  sections: {
    purpose: {
      summary:
        'Every drag interaction needs the same handle: a labelled button with keyboard instructions, a large touch target and clear picked-up state. Rook apps draw a bare `GripVertical` icon today (rookbase, rookcalendar, rookchat, rookdossier, rookdots, rookid, rookintellect, rookrecruit, rookstride, rooktickets frontends and Rook Links). Needed by: those, and by SortableGrid.',
      useWhen: [
        'Building a drag interaction Monarch does not ship (a board column, a canvas) and you need the handle part.',
        'Replacing an icon-only grip that has no name or keyboard support.',
      ],
      avoidWhen: [
        'Reordering a list or a grid: ReorderableList and SortableGrid already include handles and the move logic.',
        'Resizing: use the separator in ResizablePanels.',
      ],
    },
    designPrinciples: {
      items: [
        'The handle shows state; it does not move anything. The component that moves items passes `state` and owns pointer and key handling.',
        'Named after the item: “Reorder Weekly report”, never just “Drag”.',
        'Keyboard instructions are part of the handle (`aria-describedby`), or one shared description for many handles.',
        'A 44px target on touch screens, and `touch-action: none` so dragging the handle never scrolls the page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<button type="button" class="mn-drag-handle">` with `aria-pressed` and a hidden instructions element. Other props (pointer and key handlers) go to the button.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from @dnd-kit: spread `listeners` and `attributes` from `useSortable` onto the handle, and pass `label` and `state={isDragging ? "grabbed" : "idle"}`. Drop the separate `GripVertical` icon.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'axis',
          values: ['vertical', 'horizontal', 'both'],
          description: 'Grip dots for lists, rows or grids.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '24px or 32px; both become 44px on coarse pointers.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-state="idle"',
          description: 'Muted grip; hover tints it.',
        },
        {
          name: 'Picked up',
          attribute: 'data-state="grabbed", aria-pressed="true"',
          description: 'Accent tint and a grabbing cursor.',
        },
        {
          name: 'Dropped',
          attribute: 'data-state="dropped"',
          description: 'A success tint the owner shows briefly after a drop.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Disabled colour and not-allowed cursor.',
        },
        { name: 'Focus', description: 'Monarch focus ring on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Pointer: the owner listens for pointerdown/move/up on the handle (pointer capture recommended).',
        'Keyboard: the owner handles Space/Enter to pick up and drop, arrows to move and Escape to cancel; the default instructions describe exactly that.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the handle.' },
        {
          keys: 'Space / Enter',
          action: 'Activates the button; owners use it to pick up and drop.',
        },
      ],
      notes: ['Movement keys are implemented by the owning component, not by the handle.'],
    },
    accessibility: {
      role: 'button (toggle: aria-pressed)',
      name: 'The required `label` prop.',
      items: [
        'Described by the keyboard instructions (`messages.instructions`), or by your shared `aria-describedby` with `describe={false}`.',
        '`aria-pressed="true"` while picked up, so screen readers say “pressed”.',
        'The grip glyph is decorative and hidden.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: ['Fixed size inline; 44px square on coarse pointers.'],
    },
    theme: {
      items: [
        'Idle `--mn-color-text-muted`; picked up `--mn-color-accent-subtle` / `--mn-color-accent-strong`; dropped `--mn-color-success-subtle` / `--mn-color-success-strong`.',
        'Colour transitions are removed under reduced motion; forced-colours mode uses ButtonText, Highlight and GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'States, axes and sizes', render: () => <StatesDemo /> },
        { title: 'Driven by its owner from the keyboard', render: () => <KeyboardDemo /> },
      ],
    },
    doDont: {
      do: [
        'Name every handle after its item.',
        'Show `dropped` for about a second after a drop, then go back to `idle`.',
      ],
      dont: [
        'Do not make the whole row draggable instead of the handle.',
        'Do not use the handle without keyboard support in the owner.',
      ],
    },
    related: {
      items: [
        { id: 'reorderable-list', reason: 'A complete reorderable list.' },
        { id: 'sortable-grid', reason: 'A complete reorderable grid of tiles.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Named, described button, not pressed at rest; axe.',
        'Grabbed and dropped states; axe.',
        'Key and click handlers reach the button.',
        'Disabled, size, axis, custom icon; shared description; translated instructions; ref; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
