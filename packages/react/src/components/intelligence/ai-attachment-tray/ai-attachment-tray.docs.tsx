import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiAttachmentTray,
  AiPromptInput,
  aiAttachmentTrayFiles,
  type AiAttachmentTrayFile,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const START: AiAttachmentTrayFile[] = [
  { id: 'brief', name: 'Project brief.pdf', size: 2_450_000, type: 'application/pdf' },
  { id: 'data', name: 'Survey results.csv', size: 384_000, type: 'text/csv', progress: 0.1 },
  {
    id: 'deck',
    name: 'Kickoff slides.pptx',
    size: 48_000_000,
    type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    error: 'Larger than the 25 MB limit',
  },
];

function UploadDemo() {
  const [files, setFiles] = useState(START);
  // Simulated upload: progress advances every 400 ms until the file is ready.
  useEffect(() => {
    const timer = window.setInterval(() => {
      setFiles((list) =>
        list.map((f) =>
          typeof f.progress === 'number' && f.progress < 1 && !f.error
            ? { ...f, progress: Math.min(1, f.progress + 0.15) }
            : f,
        ),
      );
    }, 400);
    return () => window.clearInterval(timer);
  }, []);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiAttachmentTray
        files={files}
        onRemove={(id) => setFiles((list) => list.filter((f) => f.id !== id))}
        onRetry={(id) =>
          setFiles((list) =>
            list.map((f) =>
              f.id === id ? { ...f, error: undefined, size: 9_800_000, progress: 0 } : f,
            ),
          )
        }
      />
      <Button size="sm" variant="ghost" onClick={() => setFiles(START)}>
        Reset
      </Button>
    </div>
  );
}

function PromptDemo() {
  const [sent, setSent] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiPromptInput
        label="Ask about a file"
        placeholder="Paste, drop or attach a file"
        accept="image/*,.pdf,.csv,.txt"
        onSubmit={(text, { files }) => setSent(`${text} (${files.length} files)`)}
        renderAttachments={(attachments, remove) => (
          <AiAttachmentTray files={aiAttachmentTrayFiles(attachments)} onRemove={remove} />
        )}
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-attachment-tray',
  title: 'AiAttachmentTray',
  summary: 'Attached files as tiles with thumbnails, upload progress, errors and Retry.',
  sections: {
    purpose: {
      summary:
        'Shows the files attached to a prompt before it is sent: a thumbnail or type badge, name, size, upload progress and failures with Retry, each removable. It plugs into AiPromptInput’s `renderAttachments` or works on its own.',
      useWhen: [
        'Files are uploaded before the prompt is sent and people need to see progress and errors.',
        'Image attachments should show a thumbnail.',
      ],
      avoidWhen: [
        'Context that is not a file (a selection, a record, a person): use AiContextChips.',
        'Large batch uploads outside a prompt: use UploadQueue.',
        'Files in a sent message: show them in the message with AiMessageView.',
      ],
    },
    designPrinciples: {
      items: [
        'Every state is text as well as colour: "Uploading 45%", the error reason, a progress bar with a name.',
        'Failures say why and offer Retry next to the file, not in a distant toast.',
        'Removing a file never loses focus.',
        'Thumbnails come only from safe URLs (http, https, relative or `blob:`); anything else shows the type badge.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiAttachmentTrayFiles(attachments)` maps AiPromptInput attachments to tray files, so `renderAttachments={(a, remove) => <AiAttachmentTray files={aiAttachmentTrayFiles(a)} onRemove={remove} />}` is all it takes.',
        'Pass `file` (a File) for image thumbnails without a `previewUrl`; object URLs are created after mount and revoked when the file leaves.',
        '`aiAttachmentStatus(file)` gives `uploading`, `error` or `ready`. `AI_ATTACHMENT_TRAY_MESSAGES` holds the English defaults; `size` defaults to `formatBytes`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants; each tile’s look follows its status.'],
    },
    states: {
      items: [
        {
          name: 'Uploading',
          attribute: 'data-status="uploading" and aria-busy on the item',
          description: '`progress` below 1: "Uploading N%" and a named progress bar.',
        },
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description:
            'No `progress` (or 1) and no error: the size is shown. "<name> uploaded" is announced after an upload.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description:
            'The reason in danger text with an icon, a Retry button when `onRetry` is set; announced assertively once.',
        },
        { name: 'Empty', attribute: 'data-empty on the root', description: 'No list is rendered.' },
      ],
    },
    interaction: {
      items: [
        'Remove calls `onRemove(id)`; when the file leaves `files`, focus moves to the next file’s remove button, else the previous, else `fallbackFocusRef` (or the tray).',
        'Retry calls `onRetry(id)`; reset `error` and `progress` to show the new upload.',
        'Long names are truncated visually; the full name is the tooltip and is always read in full.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through Retry and Remove on each file.' },
        { keys: 'Enter / Space', action: 'Retries or removes the focused file.' },
      ],
    },
    accessibility: {
      role: 'list',
      name: '"Attachments" (messages.label)',
      items: [
        'Remove buttons are named "Remove <name>"; Retry is named "Retry uploading <name>" and described by the error.',
        'Progress bars are named "Uploading <name>" with their value.',
        'Thumbnails and type badges are decorative; the name is text.',
        'Finished uploads and removals are announced politely, failures assertively.',
      ],
    },
    responsive: {
      items: [
        'Tiles wrap at up to 15rem each; below 30rem each tile takes the full width, so there is no horizontal scroll at 390px.',
        'On coarse pointers Remove and Retry are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Tiles: `--mn-color-bg-subtle`, `--mn-color-border-default`; badges on `--mn-color-bg-muted`.',
        'Errors: `--mn-color-danger-subtle`, `--mn-color-danger-border`, `--mn-color-danger-strong`.',
        'In forced colours tiles use Canvas and CanvasText, and failed tiles get a dashed border.',
      ],
    },
    examples: {
      items: [
        { title: 'Progress, error and retry', stack: true, render: () => <UploadDemo /> },
        { title: 'As AiPromptInput attachments', stack: true, render: () => <PromptDemo /> },
      ],
    },
    doDont: {
      do: [
        'Say why an upload failed and what limit applies.',
        'Block sending while required uploads are still running, or say they will be skipped.',
        'Pass `fallbackFocusRef` to return to the prompt when the last file is removed.',
      ],
      dont: [
        'Do not hide failed files; keep them until removed or retried.',
        'Do not pass `data:` or other unsafe preview URLs; they are ignored.',
        'Do not use it for non-file context.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-input', reason: 'Its `renderAttachments` slot.' },
        { id: 'ai-context-chips', reason: 'Non-file context such as selections and records.' },
        { id: 'upload-queue', reason: 'Large upload batches outside a prompt.' },
        { id: 'file-upload', reason: 'The drop zone and `formatBytes`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'file-upload', 'progress-bar'],
    },
    testing: {
      unit: [
        '`aiAttachmentStatus` and `aiAttachmentTrayFiles`.',
        'Named list; type badge, size, progress bar with name and value, error text, Retry named and described; unsafe thumbnails ignored.',
        'Remove from the keyboard keeps focus, then falls back; announcements for removed, uploaded and failed.',
        'Object URLs created and revoked; works as AiPromptInput `renderAttachments`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
