import { useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  IconButton,
} from '@rookcom/monarch-react';
import { FileCard } from '@rookcom/monarch-react/data';
import { MoreIcon } from '../../../internal/docs-demo.js';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function CardActions({ name }: { name: string }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <IconButton label={`More actions for ${name}`} icon={<MoreIcon />} size="sm" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem>Download</DropdownMenuItem>
        <DropdownMenuItem>Rename</DropdownMenuItem>
        <DropdownMenuItem tone="danger">Move to trash</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function SelectableCard() {
  const [selected, setSelected] = useState(false);
  return (
    <div style={{ width: 220 }}>
      <FileCard
        name="site-survey.pdf"
        size={3_400_000}
        href="#"
        selectable
        selected={selected}
        onSelectedChange={setSelected}
        actions={<CardActions name="site-survey.pdf" />}
      />
    </div>
  );
}

export default {
  id: 'file-card',
  title: 'FileCard',
  summary:
    'One file as a card or row: preview or type icon, name, type and size, selection and actions.',
  sections: {
    purpose: {
      summary:
        'Presents a stored file so people can recognise it, open it, select it and act on it. Needed by: Rook Atlas (`components/atlas/files/FileRows.tsx`, `FileBits.tsx` `FileThumb`), Rook Intellect (`components/intellect/files/*`), Rook Tickets attachments and any product with a files area; FileList composes it.',
      useWhen: [
        'A grid or list of stored files, documents or media.',
        'One file shown on its own with its actions (a linked document on a record).',
      ],
      avoidWhen: [
        'A file attached inline to something being written: use AttachmentChip.',
        'Upload progress for many files: use UploadQueue.',
        'Showing the content itself: use FilePreview.',
      ],
    },
    designPrinciples: {
      items: [
        'The name is the one primary control and its hit area covers the card, so there is one tab stop to open it; checkbox and actions sit above it.',
        'Type and size are written out (“PDF document · 2 MB”), never only implied by an icon or colour.',
        'A broken or unsafe thumbnail quietly falls back to the type icon.',
        'Actions are a slot: the product decides which menu or buttons appear, Monarch lays them out.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`href` makes the name a link (http(s), blob or relative only); `onOpen` without `href` makes it a button.',
        '`onSelectedChange(selected, { shiftKey })` reports Shift so lists can select ranges.',
        'The ref is the root `<div>`; other div attributes pass through.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Atlas `FileRow`/`FileThumb`: `file.name` → `name`, `file.contentType` → `contentType`, `file.size` → `size`, a signed thumb URL → `thumbnailSrc`, `FileStateBadges` → `badges`, the row menu → `actions`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['card', 'row'],
          description:
            '`card` (default) puts a 4:3 preview above the name for grids; `row` is one 56px line with a 40px tile for lists.',
        },
        {
          prop: 'selectable',
          values: ['true', 'false'],
          description:
            'Adds a selection checkbox (top-start corner in cards, first column in rows).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Accent border and subtle accent background; the checkbox is checked.',
        },
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description: 'Has a link or `onOpen`: hover raises the card.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Muted text; open and select are disabled.',
        },
        { name: 'Family', attribute: 'data-family', description: 'The detected file family.' },
        { name: 'Focus', description: 'Focusing the name rings the whole card.' },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on the card except the checkbox and actions opens the file.',
        'Clicking or pressing Space on the checkbox toggles selection.',
        'A thumbnail that fails to load is replaced by the type icon.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the name, then the checkbox, then each action.' },
        { keys: 'Enter', action: 'Opens the file (link or button).' },
        { keys: 'Space', action: 'Toggles the checkbox when it has focus.' },
      ],
    },
    accessibility: {
      role: 'link or button (name); checkbox; group (actions)',
      name: 'The file name; the checkbox is “Select {name}” and the actions group “Actions for {name}”.',
      items: [
        'The name is described by the type, size and meta line.',
        'The thumbnail and type icon are decorative: the name and type label say the same.',
        'Selection is shown by the checkbox state, not only by the background.',
      ],
    },
    responsive: {
      items: [
        'Cards fill their grid cell; names truncate with an ellipsis.',
        'Rows keep icon, name and actions on one line down to 320px; meta wraps under the name.',
        'On coarse pointers the checkbox target is 44px and rows are 60px tall.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface`, preview `--mn-color-bg-subtle`, borders `--mn-color-border-default`.',
        'Selected uses `--mn-color-accent-border` and `--mn-color-accent-subtle`.',
        'Forced colours: `CanvasText` border, a 2px `Highlight` border when selected.',
      ],
    },
    examples: {
      items: [
        { title: 'Selectable card with actions', render: () => <SelectableCard /> },
        {
          title: 'Cards',
          render: () => (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                gap: 12,
                width: '100%',
              }}
            >
              <FileCard name="brand-guide.pdf" size={5_200_000} onOpen={() => {}} />
              <FileCard
                name="budget.xlsx"
                size={84_000}
                onOpen={() => {}}
                meta="Edited 2 days ago"
              />
              <FileCard name="team-photo.jpg" size={2_100_000} onOpen={() => {}} />
            </div>
          ),
        },
        {
          title: 'Rows',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 520 }}>
              <FileCard
                layout="row"
                name="contract-draft.docx"
                size={120_000}
                onOpen={() => {}}
                actions={<CardActions name="contract-draft.docx" />}
              />
              <FileCard
                layout="row"
                name="release-notes.md"
                size={6_400}
                onOpen={() => {}}
                badges={<span>Shared</span>}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give `size` and a `meta` such as the last edit, so files can be told apart.',
        'Put destructive actions in the actions menu with a confirmation or undo.',
      ],
      dont: [
        'Do not put a second link or button in the body; use `actions`.',
        'Do not rely on the selected background alone; keep the checkbox visible when selection is possible.',
      ],
    },
    related: {
      items: [
        { id: 'file-list', reason: 'A list or grid of cards with selection and bulk actions.' },
        { id: 'file-type-icon', reason: 'The fallback icon.' },
        { id: 'attachment-chip', reason: 'The compact form for inline attachments.' },
        { id: 'file-preview', reason: 'Shows the file’s content once opened.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['checkbox', 'file-type-icon', 'file-upload'],
    },
    testing: {
      unit: [
        'Link name, description from type and size, ref and data attributes.',
        'Open button from the keyboard in row layout.',
        'Named selection checkbox reporting Shift; selected state.',
        'Safe thumbnails only, fallback on error.',
        'Actions group, badges, meta, translated type label, unsafe links dropped, disabled; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
