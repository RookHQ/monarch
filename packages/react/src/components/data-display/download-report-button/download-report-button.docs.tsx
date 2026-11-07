import { useState } from 'react';
import { DownloadReportButton } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

function ProgressDemo() {
  const [progress, setProgress] = useState<number | null>(null);
  return (
    <DownloadReportButton
      formats={[
        { id: 'csv', label: 'CSV', description: 'Opens in any spreadsheet' },
        { id: 'xlsx', label: 'Excel' },
        { id: 'pdf', label: 'PDF', description: 'For printing and sharing' },
      ]}
      progress={progress}
      onDownload={async () => {
        for (let p = 0; p <= 1; p += 0.25) {
          setProgress(p);
          await wait(300);
        }
        setProgress(null);
      }}
    />
  );
}

let attempts = 0;

export default {
  id: 'download-report-button',
  title: 'DownloadReportButton',
  summary: 'Downloads a report in one or several formats, with progress, a queued state and retry.',
  sections: {
    purpose: {
      summary:
        'One button for every report export: a single format or a menu of formats (CSV, Excel, PDF, JSON), busy while the file is prepared, optional progress, a clear message when a large file is prepared in the background, and Try again when it fails. Needed by: People, Atlas, Tickets, Developers, Pulse.',
      useWhen: [
        'A report, table or dashboard can be downloaded as a file.',
        'Large exports may be prepared in the background.',
      ],
      avoidWhen: [
        'A personal data export request (GDPR): use DataExportRequest.',
        'Copying a value: use CopyButton.',
      ],
    },
    designPrinciples: {
      items: [
        'Never silent: preparing, saved, queued and failed are each said, on screen or to assistive technology.',
        'The app owns the file: the button only calls `onDownload` and reflects its promise.',
        'A failure keeps the chosen format, so Try again repeats exactly what failed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onDownload(format)` may return a promise. Resolve to nothing when the file was saved, or to `{ status: "queued", message? }` when it is prepared in the background; reject to show the error.',
        'Native `<div>` attributes pass through to the wrapper and the ref is forwarded to it.',
        'Hidden when printing.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Atlas report "Export CSV" (`features/reports/components/ReportViewer.tsx`, `downloading` state + `toast.error`) → `onDownload={() => reportsApi.csv(...)}`; Tickets "Export CSV" (`components/reports/ReportsPage.tsx`, `exporting` state + toasts) the same; People `ExportDialog` (`components/reports/ExportDialog.tsx`) CSV/JSON choice → `formats`, its queued toast for more than 10,000 rows → resolve `{ status: "queued" }`; the reason field and audit notice stay in the People dialog, which can render this button as its confirm step.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'formats',
          values: ['one', 'several'],
          description: 'One format is a plain "Download CSV" button; several open a menu.',
        },
        {
          prop: 'variant',
          values: ['secondary', 'primary', 'ghost'],
          description: 'Button style. Default `secondary`.',
        },
        { prop: 'size', values: ['sm', 'md'], description: 'Button size. Default `sm`.' },
      ],
    },
    states: {
      items: [
        { name: 'Idle', description: 'Ready to download.' },
        {
          name: 'Pending',
          attribute: 'data-status="pending"',
          description:
            'The button is busy (`aria-busy`) and ignores clicks; with `progress` a progress bar shows.',
        },
        {
          name: 'Done',
          attribute: 'data-status="done"',
          description: '"CSV downloaded" is announced.',
        },
        {
          name: 'Queued',
          attribute: 'data-status="queued"',
          description: 'A note says the file is being prepared.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'The failure with Try again.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the button (one format) or a menu item calls `onDownload` with the format id.',
        'A second click while busy does nothing.',
        'Try again repeats the failed format; when the retry starts, focus returns to the download button.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Downloads, or opens the formats menu.' },
        {
          keys: 'Arrow keys / Enter / Escape',
          action: 'Move through, choose from and close the menu.',
        },
        { keys: 'Tab', action: 'Reaches Try again after a failure.' },
      ],
    },
    accessibility: {
      role: 'button (with a menu when there are several formats)',
      name: '"Download {format}" or "Download"',
      items: [
        'The busy button keeps focus (it uses `aria-disabled`, not `disabled`).',
        'Preparing, saved and queued messages are spoken by a polite status region.',
        'The progress bar is named "Download progress".',
      ],
    },
    responsive: {
      items: [
        'The note and error wrap under the button; nothing scrolls at 390px.',
        'The button is at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Button, menu, Progress and InlineErrorRetry tokens; the note uses `--mn-color-text-muted`.',
        'In forced colours the note is CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Several formats with progress', render: () => <ProgressDemo /> },
        {
          title: 'Queued for background preparation',
          render: () => (
            <DownloadReportButton onDownload={async () => ({ status: 'queued' as const })} />
          ),
        },
        {
          title: 'Fails once, then works',
          render: () => (
            <DownloadReportButton
              formats={[{ id: 'pdf', label: 'PDF' }]}
              onDownload={async () => {
                attempts += 1;
                await wait(400);
                if (attempts % 2 === 1) throw new Error('timeout');
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Return the download promise so the button can show progress and failure.',
        'Resolve `{ status: "queued" }` for exports prepared in the background, and say where the file will appear.',
        'Disable it when there is nothing to download.',
      ],
      dont: [
        'Do not also show a success toast; the button already announces it.',
        'Do not offer formats the server cannot produce for this report.',
      ],
    },
    related: {
      items: [
        { id: 'report-viewer', reason: 'Puts this button in a report header.' },
        { id: 'data-export-request', reason: 'Requesting a personal data export.' },
        { id: 'dropdown-menu', reason: 'A general action menu.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dropdown-menu', 'inline-error-retry', 'progress-bar'],
    },
    testing: {
      unit: [
        'Single format downloads, stays busy, ignores a second click and announces success; props and ref forwarded.',
        'Several formats in a keyboard-operable menu.',
        'Progress bar follows `progress` while pending.',
        'Queued result shows the background note.',
        'Failure with a custom message, Try again repeats the format and returns focus; disabled; messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
