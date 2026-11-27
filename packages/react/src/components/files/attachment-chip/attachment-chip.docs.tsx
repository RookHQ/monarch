import { useState } from 'react';
import { AttachmentChip } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './attachment-chip.fixture.json';

function RemovableChips() {
  const [files, setFiles] = useState(
    fixture.data.files.map((file, i) => ({ id: String(i + 1), ...file })),
  );
  return (
    <ul
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 8,
        margin: 0,
        padding: 0,
        listStyle: 'none',
      }}
    >
      {files.map((f) => (
        <li key={f.id} style={{ minWidth: 0, maxWidth: '100%' }}>
          <AttachmentChip
            name={f.name}
            size={f.size}
            onRemove={() => setFiles((l) => l.filter((x) => x.id !== f.id))}
          />
        </li>
      ))}
    </ul>
  );
}

export default {
  id: 'attachment-chip',
  title: 'AttachmentChip',
  summary: 'A compact chip for one attached file with type icon, size, upload progress and remove.',
  sections: {
    purpose: {
      summary:
        'Shows a file attached to a message, ticket reply or form in one compact row: what it is, how big, whether it is still uploading or failed, and a way to remove it. Needed by: Rook Tickets (`components/portal/Attachments.tsx`, `components/tickets/detail/MessageItem.tsx`), Rook Chat and the message composer, Rook Intellect and Rook Atlas uploads.',
      useWhen: [
        'Files are attached inline to something being written (a message, comment or reply).',
        'A sent message lists its attachments as links to open or download.',
      ],
      avoidWhen: [
        'Many files with several actions each: use FileList.',
        'A file shown with a large preview: use FileCard.',
        'A queue of uploads with totals and clear-finished: use UploadQueue.',
      ],
    },
    designPrinciples: {
      items: [
        'One line, one file: the name is the main content and is truncated, never wrapped, with the full name in a tooltip.',
        'Status is said in words (“Upload failed”, “42%”), not only by colour.',
        'The chip never uploads; the product sets `status` and `progress` and handles remove and retry.',
        'Statuses match UploadQueue (`queued`, `uploading`, `done`, `failed`, `cancelled`) so the two can share state.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`href` makes the name a link once `status` is `done`; it opens in a new tab, or downloads with `download`. Unsafe URLs (`javascript:`, `data:`) are not linked.',
        'The ref is the root `<div>`; other div attributes pass through. Put chips in a `<ul>` when there are several.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from app-made chips (Tickets `AttachmentList`, shadcn `Badge` + `X` button): `name`, `size` and `onRemove` map directly; replace a progress number in the label with `status="uploading"` and `progress`.',
        'From `AiAttachmentTray` files: `progress` → `progress`, `error` → `status="failed"` + `error`, `previewUrl` → `icon={<img src alt="" />}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['queued', 'uploading', 'done', 'failed', 'cancelled'],
          description:
            'Changes the meta text, shows a thin progress bar while uploading, and a retry button when failed.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status',
          description: 'One of the five upload statuses.',
        },
        {
          name: 'Failed',
          attribute: 'data-status="failed"',
          description: 'Danger border and background; the reason is announced.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Remove and retry are disabled.',
        },
        {
          name: 'Indeterminate',
          attribute: 'data-indeterminate',
          description:
            'Uploading with no `progress`: the bar slides (static under reduced motion).',
        },
      ],
    },
    interaction: {
      items: [
        'Remove calls `onRemove`; the product removes the file (and cancels its upload if needed).',
        'Retry appears only for failed uploads with `onRetry`.',
        'When removing moves focus out of the list, move focus to the next chip or the attach button.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the name link (when linked), then retry, then remove.' },
        { keys: 'Enter / Space', action: 'Activates the focused button or link.' },
      ],
    },
    accessibility: {
      role: 'group of a link and buttons; progressbar while uploading',
      name: 'Buttons are named “Remove {name}” and “Retry {name}”; the progress bar “Uploading {name}”.',
      items: [
        'The link is described by the size, so screen readers say “brief.pdf, 2 KB”.',
        'A failure is in a `role="alert"` so it is announced when it happens.',
        'The type icon is decorative because the name already carries the extension.',
      ],
    },
    responsive: {
      items: [
        'The chip never exceeds its container; the name truncates first.',
        'On coarse pointers the chip and its buttons are 44px tall.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface` with a `--mn-color-border-default` border; failed uses the danger subtle/border pair.',
        'Progress uses `--mn-color-accent-solid` on `--mn-color-bg-muted`.',
        'Forced colours: `CanvasText` border, dashed when failed, `Highlight` progress.',
      ],
    },
    examples: {
      items: [
        { title: 'Removable chips', render: () => <RemovableChips /> },
        {
          title: 'Upload states',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 8, justifyItems: 'start' }}>
              <AttachmentChip name="survey-results.csv" status="queued" onRemove={() => {}} />
              <AttachmentChip
                name="site-photo.jpg"
                status="uploading"
                progress={64}
                onRemove={() => {}}
              />
              <AttachmentChip name="recording.mp4" status="uploading" onRemove={() => {}} />
              <AttachmentChip
                name="archive.zip"
                status="failed"
                error="Larger than 25 MB"
                onRetry={() => {}}
                onRemove={() => {}}
              />
            </div>
          ),
        },
        {
          title: 'Sent attachment, as a download link',
          render: () => <AttachmentChip name="invoice-0042.pdf" size={88_000} href="#" download />,
        },
      ],
    },
    doDont: {
      do: [
        'Give every chip a `name` with its extension.',
        'Explain a failure in `error` (“Larger than 25 MB”), not just “failed”.',
      ],
      dont: [
        'Do not remove the file without a way back if the upload was expensive; offer an undo toast instead of a confirmation.',
        'Do not show size and percentage at the same time; the chip picks one.',
      ],
    },
    related: {
      items: [
        { id: 'upload-queue', reason: 'The fuller list of uploads with totals.' },
        { id: 'file-card', reason: 'One file with a preview area and actions.' },
        { id: 'file-type-icon', reason: 'The icon the chip draws.' },
        { id: 'file-upload', reason: 'Where files come from; also exports `formatBytes`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['file-upload', 'file-type-icon'],
    },
    testing: {
      unit: [
        'Name, size, ref and keyboard remove.',
        'Determinate and indeterminate progress bar with names and values.',
        'Failure announced as an alert with retry.',
        'Safe links only, download attribute, no link while uploading.',
        'Queued and cancelled text, disabled buttons, translated messages; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
