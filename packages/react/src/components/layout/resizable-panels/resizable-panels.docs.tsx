import { useState } from 'react';
import { ResizableHandle, ResizablePanel, ResizablePanels } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const frame = {
  width: '100%',
  height: 240,
  border: '1px solid var(--mn-color-border-default)',
  borderRadius: 'var(--mn-radius-lg)',
} as const;
const pad = { padding: 12, fontSize: 'var(--mn-text-sm)' } as const;

function SavedLayoutDemo() {
  const [saved, setSaved] = useState<number[]>([30, 70]);
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <div style={frame}>
        <ResizablePanels defaultSizes={saved} onLayout={setSaved}>
          <ResizablePanel minSize={20} maxSize={50} collapsible>
            <div style={pad}>Folders</div>
          </ResizablePanel>
          <ResizableHandle withHandle />
          <ResizablePanel minSize={30}>
            <div style={pad}>Messages</div>
          </ResizablePanel>
        </ResizablePanels>
      </div>
      <span style={{ fontSize: 'var(--mn-text-sm)' }}>Saved layout: {saved.join(' / ')}</span>
    </div>
  );
}

export default {
  id: 'resizable-panels',
  title: 'ResizablePanels',
  summary:
    'Split panes resized by dragging or with the arrow keys, through an accessible separator; sizes are saved through a callback.',
  sections: {
    purpose: {
      summary:
        'Editors, inboxes and detail views let people choose how wide a list or preview is. The shadcn/ui copies in Rook Links (`src/components/ui/resizable.tsx`) and Rook ID (`rookid/components/ui/resizable.tsx`, `rookid/src/components/ui/resizable.tsx`) wrap the `react-resizable-panels` dependency. ResizablePanels has the same shape with no dependency, an APG window splitter and a save callback. Needed by: Rook Links and Rook ID.',
      useWhen: [
        'A list beside a detail or preview, where people want a different balance.',
        'An editor with a side panel (outline, properties, chat) that can be narrowed or collapsed.',
      ],
      avoidWhen: [
        'A fixed two-column page: use SplitLayout or Grid.',
        'Phones as the main target: offer a tab or a sheet instead, or set `stackOnNarrow`.',
      ],
    },
    designPrinciples: {
      items: [
        'Sizes are percentages, so a saved layout fits any window.',
        'Every separator is a WAI-ARIA window splitter: focusable, named, with the size of the panel it controls as its value.',
        'Keyboard and pointer reach the same sizes and the same limits.',
        'Saving is the app’s job: `onLayout` fires when a resize finishes, never on every pixel.',
        'A thin 1px line with a wide hit area (44px on touch), and an optional grip.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ResizablePanels` renders `<div class="mn-resizable-panels" data-direction>`; `ResizablePanel` `<div class="mn-resizable-panels__panel" data-size>`; `ResizableHandle` `<div role="separator" class="mn-resizable-panels__handle">`.',
        'Panels and handles must be direct children of the group, alternating. `ResizablePanelGroup` is the same component as `ResizablePanels`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (react-resizable-panels): `ResizablePanelGroup direction` and `onLayout` keep their names; `autoSaveId` becomes saving `onLayout` and passing `defaultSizes`; `Panel defaultSize/minSize/maxSize/collapsible/collapsedSize` keep their names (percent); `ResizableHandle withHandle` is the same.',
        'Not supported: imperative panel handles (`collapse()`, `resize()`), and pixel units.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'direction',
          values: ['horizontal', 'vertical'],
          description: 'Side by side, or stacked.',
        },
        {
          prop: 'withHandle',
          values: ['false', 'true'],
          description: 'A visible grip on the separator.',
        },
        {
          prop: 'collapsible',
          values: ['false', 'true'],
          description: 'The panel before a separator can collapse to `collapsedSize`.',
        },
        {
          prop: 'stackOnNarrow',
          values: ['false', 'true'],
          description: 'Below 768px, stack panels at natural height and hide separators.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Dragging',
          attribute: 'data-dragging on the separator',
          description: 'Accent line while a pointer drags.',
        },
        {
          name: 'Focus',
          description: 'The separator shows the focus ring colour and a ring on `:focus-visible`.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed on the panel',
          description: 'The panel is at its collapsed size and hidden.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, aria-disabled on the separator',
          description: 'Not draggable and out of the tab order.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer: press the separator and drag; pointer capture keeps the drag going outside it. `touch-action: none`, so dragging does not scroll on touch.',
        'Each separator resizes the two panels next to it, within both panels’ minimum and maximum.',
        'Dragging below half a collapsible panel’s minimum collapses it.',
        '`onLayout` fires on release and after each keyboard step.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to each separator.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action:
            'Horizontal: narrows or widens the panel before the separator by `keyboardStep` (5%); swapped in RTL.',
        },
        { keys: 'ArrowUp / ArrowDown', action: 'Vertical: shrinks or grows the panel above.' },
        { keys: 'Home / End', action: 'The panel before goes to its minimum or maximum.' },
        { keys: 'Enter', action: 'Collapses or restores a collapsible panel.' },
      ],
    },
    accessibility: {
      role: 'separator (focusable window splitter)',
      name: '`messages.handleLabel` (“Resize panels”), or `label` per separator, such as “Resize folder list”.',
      items: [
        '`aria-valuenow`, `aria-valuemin` and `aria-valuemax` give the size of the panel before it in percent; `aria-controls` points to that panel.',
        '`aria-orientation` is perpendicular to the split, as APG specifies.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Fills its parent, so give the parent a height.',
        'At 390px, set `stackOnNarrow` to stack panels and drop the separators, or render a different layout.',
        'Coarse pointers get a 44px-wide hit area around each separator.',
      ],
    },
    theme: {
      items: [
        'Line `--mn-color-border-default`; hover and drag `--mn-color-accent-solid`; focus `--mn-color-focus-ring`; grip on `--mn-color-bg-surface`.',
        'Colour transitions are removed under reduced motion; forced-colours mode uses CanvasText and Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Saved, collapsible layout',
          stack: true,
          render: () => <SavedLayoutDemo />,
        },
        {
          title: 'Vertical, three panels',
          stack: true,
          render: () => (
            <div style={{ ...frame, height: 300 }}>
              <ResizablePanels direction="vertical">
                <ResizablePanel defaultSize={50}>
                  <div style={pad}>Document</div>
                </ResizablePanel>
                <ResizableHandle />
                <ResizablePanel>
                  <div style={pad}>Comments</div>
                </ResizablePanel>
                <ResizableHandle />
                <ResizablePanel>
                  <div style={pad}>History</div>
                </ResizablePanel>
              </ResizablePanels>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Save `onLayout` sizes per user and pass them back as `defaultSizes`.',
        'Set minimum sizes that keep each panel usable.',
        'Name separators after what they resize when there are several.',
      ],
      dont: [
        'Do not wrap panels or handles in other elements: they must be direct children.',
        'Do not rely on resizing on phones.',
      ],
    },
    related: {
      items: [
        { id: 'split-layout', reason: 'A fixed two-column layout without resizing.' },
        { id: 'scroll-area', reason: 'A labelled scroll container for each panel.' },
        { id: 'sheet', reason: 'A side panel on small screens.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Sizes from defaults; separator orientation, value, limits, controls; axe.',
        'Arrow keys within limits, Home/End, onLayout per step.',
        'RTL and vertical keys; Enter collapses and restores.',
        'Pointer drag with one onLayout on release; collapse by dragging.',
        'Saved sizes, shadcn names, shared space across three panels, disabled separator.',
        'Narrow stacking attribute; ref; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
