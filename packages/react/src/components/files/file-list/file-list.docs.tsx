import { useState } from 'react';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  IconButton,
  SegmentedControl,
} from '@rookcom/monarch-react';
import { FileList, type FileListItem } from '@rookcom/monarch-react/data';
import { MoreIcon } from '../../../internal/docs-demo.js';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FILES: FileListItem[] = [
  { id: '1', name: 'project-brief.pdf', size: 1_800_000, meta: 'Edited yesterday' },
  { id: '2', name: 'wireframes.png', size: 4_200_000, meta: 'Edited 3 days ago' },
  { id: '3', name: 'estimates.xlsx', size: 96_000, meta: 'Edited last week' },
  { id: '4', name: 'kickoff-recording.mp4', size: 182_000_000, meta: 'Edited last week' },
  { id: '5', name: 'source-assets.zip', size: 54_000_000, meta: 'Edited last month' },
];

function RowMenu({ item }: { item: FileListItem }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <IconButton label={`More actions for ${item.name}`} icon={<MoreIcon />} size="sm" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem>Download</DropdownMenuItem>
        <DropdownMenuItem>Rename</DropdownMenuItem>
        <DropdownMenuItem tone="danger">Move to trash</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function FilesDemo() {
  const [layout, setLayout] = useState<'list' | 'grid'>('list');
  const [selected, setSelected] = useState<string[]>([]);
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 640 }}>
      <SegmentedControl
        aria-label="Layout"
        value={layout}
        onValueChange={(v: string) => setLayout(v as 'list' | 'grid')}
        options={[
          { value: 'list', label: 'List' },
          { value: 'grid', label: 'Grid' },
        ]}
      />
      <FileList
        label="Project files"
        items={FILES}
        layout={layout}
        selectable
        selectedIds={selected}
        onSelectionChange={setSelected}
        onOpen={() => {}}
        renderActions={(item) => <RowMenu item={item} />}
        bulkActions={() => (
          <>
            <Button size="sm">Download</Button>
            <Button size="sm" variant="danger-subtle">
              Move to trash
            </Button>
          </>
        )}
      />
    </div>
  );
}

export default {
  id: 'file-list',
  title: 'FileList',
  summary:
    'A list or grid of files with selection, Shift ranges, per-file actions and bulk actions.',
  sections: {
    purpose: {
      summary:
        'Shows a folder’s or record’s files as rows or a grid of cards, lets people select several and act on them together. Needed by: Rook Atlas (`components/atlas/files/FilesPanel.tsx`, `FileRows.tsx`), Rook Intellect (`components/intellect/files/*`) and Rook Tickets attachments lists.',
      useWhen: [
        'A files area or the attachments of a record, with open, download and delete.',
        'People need to switch between a dense list and a visual grid.',
      ],
      avoidWhen: [
        'Sortable columns, many metadata fields or virtualised thousands of rows: use DataTable with a FileTypeIcon cell.',
        'Files attached to something being written: use AttachmentChip.',
      ],
    },
    designPrinciples: {
      items: [
        'Selection uses real checkboxes, so it works with Tab and Space and is announced; there is no hidden “selection mode”.',
        'Bulk actions appear only while something is selected, next to the count.',
        'Per-file and bulk actions are slots: the product decides what they are and confirms destructive ones.',
        'Each row is a FileCard, so list and grid read and behave the same.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Selection is controlled with `selectedIds` + `onSelectionChange`, or uncontrolled with `defaultSelectedIds`. Ids come back in item order.',
        '`bulkActions(selectedIds)` renders inside a `role="toolbar"`.',
        'The ref is the root `<div>`; other div attributes pass through.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Atlas `FilesPanel`: map each `StoredFile` to `{ id, name, size, contentType, thumbnailSrc, badges: <FileStateBadges /> }`; the row menu goes in `renderActions`, the selection bar in `bulkActions`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['list', 'grid'],
          description: '`list` (default) rows; `grid` responsive cards (at least 160px wide).',
        },
        {
          prop: 'selectable',
          values: ['true', 'false'],
          description: 'Adds checkboxes, select all, the count and the bulk actions bar.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Layout', attribute: 'data-layout', description: '`list` or `grid`.' },
        {
          name: 'Selecting',
          attribute: 'data-selecting',
          description: 'At least one file is selected; the count and bulk actions show.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No items: the `empty` content (or “No files yet.”) shows.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a checkbox toggles that file; Shift-clicking selects or clears every enabled file between it and the last one toggled.',
        'The select-all checkbox is mixed when some files are selected; clicking it selects all enabled files, or clears when all are selected.',
        'Disabled files are skipped by ranges and select all.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through select all, bulk actions, then each file’s name, checkbox and actions.',
        },
        { keys: 'Space', action: 'Toggles the focused checkbox; Shift+Space selects a range.' },
        { keys: 'Enter', action: 'Opens the focused file.' },
        { keys: 'Escape', action: 'Clears the selection while focus is in the list.' },
      ],
    },
    accessibility: {
      role: 'list of listitems; checkbox per file; toolbar for bulk actions',
      name: '`label` names the list; the toolbar is “Actions for selected files”.',
      items: [
        'The selection count is in a `role="status"` region, so changes are announced.',
        'Each checkbox is named “Select {file name}”.',
        'The select-all checkbox exposes the mixed state.',
      ],
    },
    responsive: {
      items: [
        'The grid fills the width with columns of at least 160px (one column on narrow phones).',
        'The toolbar wraps: bulk actions move under the count at 390px.',
        'Coarse pointers get 44px targets through FileCard and the toolbar height.',
      ],
    },
    theme: {
      items: [
        'Uses FileCard’s surfaces; the empty state has a dashed `--mn-color-border-default` border.',
        'Forced colours: the empty border is `CanvasText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Selectable list and grid with actions',
          stack: true,
          render: () => <FilesDemo />,
        },
        {
          title: 'Read-only list',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 520 }}>
              <FileList label="Attachments" items={FILES.slice(0, 3)} onOpen={() => {}} />
            </div>
          ),
        },
        {
          title: 'Empty',
          stack: true,
          render: () => <FileList items={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Name the list with `label` when the page has more than one.',
        'Confirm or offer undo for bulk deletes; say how many files are affected.',
      ],
      dont: [
        'Do not hide checkboxes until hover; touch and keyboard users need them.',
        'Do not use it for thousands of files without paging.',
      ],
    },
    related: {
      items: [
        { id: 'file-card', reason: 'Each row or card.' },
        { id: 'drop-zone', reason: 'Lets people drop new files onto the list.' },
        { id: 'empty-state', reason: 'A richer `empty` content.' },
        { id: 'data-table', reason: 'For many columns and sorting.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['checkbox', 'file-card'] },
    testing: {
      unit: [
        'Named list of rows, ref, open from the keyboard.',
        'Single selection, Shift ranges skipping disabled files, mixed and full select all, bulk toolbar, Escape clears.',
        'Controlled selection, grid layout, per-file actions.',
        'Empty state, custom empty, uncontrolled default, translated messages; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
