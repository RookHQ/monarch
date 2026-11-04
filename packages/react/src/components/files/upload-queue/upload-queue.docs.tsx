import { useState } from 'react';
import { UploadQueue, type UploadQueueItem } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MB = 1024 * 1024;

const SAMPLE: UploadQueueItem[] = [
  { id: '1', name: 'quarterly-report.pdf', size: 2.4 * MB, status: 'done' },
  { id: '2', name: 'site-photo-01.jpg', size: 4.1 * MB, status: 'uploading', progress: 62 },
  { id: '3', name: 'site-photo-02.jpg', size: 3.8 * MB, status: 'queued' },
  {
    id: '4',
    name: 'floor-plan.dwg',
    size: 18 * MB,
    status: 'failed',
    error: 'Upload interrupted. Check your connection and try again.',
  },
  {
    id: '5',
    name: 'archive.zip',
    size: 120 * MB,
    status: 'failed',
    error: 'Larger than the 100 MB limit.',
    retryable: false,
  },
];

function QueueDemo() {
  const [items, setItems] = useState(SAMPLE);
  const set = (id: string, patch: Partial<UploadQueueItem>) =>
    setItems((l) => l.map((i) => (i.id === id ? { ...i, ...patch } : i)));
  return (
    <UploadQueue
      style={{ width: '100%', maxWidth: 520 }}
      items={items}
      onCancel={(id) => set(id, { status: 'cancelled' })}
      onRetry={(id) =>
        setItems((l) =>
          l.map((i) => {
            if (i.id !== id) return i;
            const { error: _error, ...rest } = i;
            return { ...rest, status: 'queued' };
          }),
        )
      }
      onRemove={(id) => setItems((l) => l.filter((i) => i.id !== id))}
      onClearFinished={() =>
        setItems((l) => l.filter((i) => i.status === 'uploading' || i.status === 'queued'))
      }
    />
  );
}

export default {
  id: 'upload-queue',
  title: 'UploadQueue',
  summary:
    'The list of uploads in flight: progress, status, cancel, retry, remove and a total summary.',
  sections: {
    purpose: {
      summary:
        'Shows each file the product is uploading with its progress and status, lets people cancel, retry or remove uploads, and sums up the batch. Completion and failures are announced.',
      useWhen: [
        'Several files upload at once, or uploads take long enough that people need to see progress.',
        'Uploads can fail for reasons people can act on (connection, size, quota) and should be retried.',
        'Pair with FileUpload, which hands over the files this list shows.',
      ],
      avoidWhen: [
        'A single small file uploads in a moment: a loading button or Progress is enough.',
        'The list shows stored files rather than uploads in progress: use a file list or table.',
      ],
    },
    designPrinciples: {
      items: [
        'The product owns the transfer and the state; the queue only displays `items` and reports actions by id.',
        'Status is text and icon, never colour alone: "Uploading… 62%", "Uploaded", the error message.',
        'Actions appear only when they make sense and when the product handles them: cancel while active, retry for failed or cancelled uploads that can succeed, remove once finished.',
        'A refusal that cannot succeed unchanged (too large, wrong type) sets `retryable: false`, so Retry is not offered.',
        'Progress reuses Progress, so bars look and announce the same everywhere.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Items are `{ id, name, status, size?, progress?, error?, retryable?, thumbnail?, icon?, detail? }`; `status` is `queued`, `uploading`, `done`, `failed` or `cancelled`, and `progress` is 0–100 (omit for an indeterminate bar).',
        '`onCancel`, `onRetry` and `onRemove` receive the item id; each button appears only when its handler is given. `onClearFinished` adds a header button.',
        '`countUploads(items)` and `totalUploadProgress(items)` are exported: the total leaves out failed and cancelled uploads and is weighted by size when every upload has one.',
        'The ref is the root `<section>`; other attributes pass through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'showSummary',
          values: ['true', 'false'],
          description:
            'Show the header with title, summary ("1 of 4 uploaded · 1 failed") and total progress (default). Without it the section is named by `messages.title`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Queued',
          attribute: 'data-status="queued"',
          description: '"Waiting", with Cancel.',
        },
        {
          name: 'Uploading',
          attribute: 'data-status="uploading"',
          description:
            'A Progress bar (determinate or indeterminate) and "Uploading… n%", with Cancel.',
        },
        {
          name: 'Done',
          attribute: 'data-status="done"',
          description: 'Success icon and "Uploaded", with Remove.',
        },
        {
          name: 'Failed',
          attribute: 'data-status="failed"',
          description:
            'Error icon and the product’s message, with Retry (unless `retryable` is false) and Remove.',
        },
        {
          name: 'Cancelled',
          attribute: 'data-status="cancelled"',
          description: 'Muted name and "Cancelled", with Retry and Remove.',
        },
        {
          name: 'Active batch',
          attribute: 'data-active',
          description:
            'Some uploads are queued or uploading; the header shows the total progress bar.',
        },
      ],
    },
    interaction: {
      items: [
        'Buttons call the product’s handlers; the queue changes only when new `items` arrive.',
        'When a row is removed while focus is in it, focus moves to the next row (or the previous one, or the heading when the list is empty).',
        'Moving to done or failed is announced; when the last active upload finishes in a batch of several, a summary is announced too.',
        '`detail` replaces the status text, for extra steps such as "Checking for viruses".',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves between the Retry, Cancel and Remove buttons of each row, and Clear finished.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'region (section) labelled by its heading; a list of uploads',
      name: 'The heading, "Uploads" by default (`messages.title`).',
      items: [
        'Each progress bar is named "Upload progress of {file}"; the total bar is "Total upload progress".',
        'Buttons are named with the file: "Cancel upload of photo.jpg", "Retry upload of photo.jpg", "Remove photo.jpg from the list". Retry keeps its visible word in its name.',
        'A polite live region announces completed and failed uploads and the end of the batch.',
        'Thumbnails are decorative (`alt=""`); the file name is the text.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/upload-queue.',
      ],
    },
    responsive: {
      items: [
        'Rows wrap: long names break anywhere, the size wraps under the name, and the actions stay at the end.',
        'The header wraps the Clear finished button and keeps the total bar full width. Works at 320px.',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        'Done uses `--mn-color-success-strong`, failed `--mn-color-danger-strong`; both also have an icon and text.',
        'Progress bars follow Progress theming, including reduced motion for the indeterminate bar.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Mixed statuses',
          stack: true,
          render: () => <QueueDemo />,
          code: `<UploadQueue
  items={uploads}
  onCancel={(id) => uploader.cancel(id)}
  onRetry={(id) => uploader.retry(id)}
  onRemove={(id) => uploader.remove(id)}
  onClearFinished={() => uploader.clearFinished()}
/>`,
        },
        {
          title: 'Without the header, with a custom step',
          stack: true,
          render: () => (
            <UploadQueue
              style={{ width: '100%', maxWidth: 520 }}
              showSummary={false}
              aria-label="Banner uploads"
              items={[
                {
                  id: 'a',
                  name: 'banner-wide.png',
                  size: 1.2 * MB,
                  status: 'done',
                  detail: 'Checking for viruses',
                },
                { id: 'b', name: 'banner-square.png', size: 0.8 * MB, status: 'uploading' },
              ]}
            />
          ),
          code: `<UploadQueue
  showSummary={false}
  aria-label="Banner uploads"
  items={[
    { id: 'a', name: 'banner-wide.png', status: 'done', thumbnail: previewUrl, detail: 'Checking for viruses' },
    { id: 'b', name: 'banner-square.png', status: 'uploading' }, // no progress yet: indeterminate
  ]}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Report real progress (bytes sent) when you have it; omit `progress` rather than invent it.',
        'Write failure messages people can act on, and set `retryable: false` when retrying cannot help.',
        'Revoke object URLs you pass as `thumbnail` when the row is removed.',
      ],
      dont: [
        'Do not remove failed uploads automatically; let people retry or dismiss them.',
        'Do not show refusals the FileUpload already explained as failed uploads.',
      ],
    },
    related: {
      items: [
        { id: 'file-upload', reason: 'Picks and checks the files before they reach the queue.' },
        { id: 'progress-bar', reason: 'One progress bar for a single task.' },
        {
          id: 'toast',
          reason: 'A short confirmation once a background upload finishes, when no list is shown.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'progress-bar', 'file-upload'],
      notes: ['Uses `formatBytes` from FileUpload for sizes.'],
    },
    testing: {
      unit: [
        'Counting by status and the size-weighted total progress.',
        'Rows show status text, per-file and total progress, sizes and the summary; axe passes.',
        'Cancel, Retry and Remove appear by status and handler, are named with the file and report the id; non-retryable failures get no Retry.',
        'Announces done and failed uploads and the end of the batch; the total bar goes when nothing is active.',
        'Focus moves to the next row, then the heading, when rows are removed.',
        'Clear finished, indeterminate progress, decorative thumbnails, custom detail; no header with a named region; translated messages; server rendering.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/upload-queue',
        'Cross-browser axe: /components/upload-queue',
      ],
    },
  },
} satisfies ComponentDocs;
