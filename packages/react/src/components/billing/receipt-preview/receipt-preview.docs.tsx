import { ReceiptPreview } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SAMPLE_RECEIPT = `data:image/svg+xml;utf8,${encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="320" viewBox="0 0 240 320"><rect width="240" height="320" fill="white"/><g font-family="monospace" font-size="12" fill="black"><text x="20" y="40">SAMPLE STORE</text><text x="20" y="80">Coffee        4.50</text><text x="20" y="100">Sandwich      8.25</text><text x="20" y="140">TOTAL        12.75</text></g></svg>',
)}`;

/** Simulates an authorised fetch that returns a Blob after a short delay. */
const loadSample = () =>
  new Promise<Blob>((resolve) =>
    setTimeout(
      () =>
        resolve(
          new Blob(
            [
              '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="160"><rect width="240" height="160" fill="white"/><text x="20" y="80" font-family="monospace">Parking  6.00</text></svg>',
            ],
            { type: 'image/svg+xml' },
          ),
        ),
      600,
    ),
  );

export default {
  id: 'receipt-preview',
  title: 'ReceiptPreview',
  summary:
    'A View receipt button that previews an uploaded image or PDF, loading private files only when opened.',
  sections: {
    purpose: {
      summary:
        'Lets approvers and claimants check the receipt behind an expense without leaving the claim, with a safe path for formats the browser cannot show. Needed by: People (expense claims).',
      useWhen: [
        'An expense, bill or reimbursement line has an attached receipt or invoice scan.',
        'The file is private and must be fetched with authorisation only when someone opens it.',
      ],
      avoidWhen: [
        'Uploading a receipt: use FileUpload or MediaUpload.',
        'Browsing many documents: use a file list or ReportViewer.',
      ],
    },
    designPrinciples: {
      items: [
        'No fetching in Monarch: the product passes a URL or a `load` callback, so its audited, authorised endpoint stays in charge.',
        'Fetch late, release early: `load` runs only on open, and object URLs Monarch creates are revoked when the dialog closes.',
        'Never a dead end: every state offers Download or Try again, and unsupported formats say why.',
        'PDFs open in a new tab by default, because embedded PDF viewers are unreliable on phones.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and button attributes (variant, size, disabled) go to the trigger Button. Defaults: `variant="ghost"`, `size="sm"`.',
        'With neither `src` nor `load`, the component renders the `none` text (“No receipt”) instead of a button.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`load` may return a Blob (Monarch creates and revokes the object URL) or a URL string (the product owns it).',
        'For lazily loaded PDFs in tab mode, a blank tab is opened during the click so popup blockers allow it, then pointed at the file; its object URL is revoked after two minutes.',
        'Migrating from the app copy (People `components/expenses/ReceiptPreview.tsx`): `wsId` and `path` become `load={() => apiClient.get(wsPath(wsId, path), { responseType: "blob" }).then((r) => r.data)}`; `mimeType` and `label` (now `messages.view`) are unchanged; the error toast becomes `onLoadError` (the dialog now shows the error with Try again); the “No receipt” text beside the button is built in.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'pdf',
          values: ['tab', 'dialog'],
          description: 'Open PDFs in a new browser tab (default) or embed them in the dialog.',
        },
        {
          prop: 'variant / size',
          values: ['Button variants', 'xs | sm | md | lg'],
          description: 'Passed to the trigger Button.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-status="loading"',
          description: 'A labelled spinner while `load` runs.',
        },
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description: 'Image or PDF shown with Download.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: '`load` failed: an announced message with Try again.',
        },
        {
          name: 'Unsupported',
          attribute: 'data-unsupported',
          description:
            'HEIC photos, undecodable images and other types explain why and offer Download.',
        },
        { name: 'No receipt', description: 'No `src` or `load`: muted “No receipt” text.' },
      ],
    },
    interaction: {
      items: [
        'Clicking the button opens the dialog and loads the file, or opens a PDF in a new tab.',
        'If a lazily loaded PDF fails, the dialog opens with the error and Try again.',
        'Download saves the file under `fileName` or `receipt.<extension>`.',
        'Closing the dialog returns focus to the button and releases the object URL.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Open the preview (or the PDF tab).' },
        { keys: 'Tab', action: 'Move between Download, Try again and Close inside the dialog.' },
        { keys: 'Escape', action: 'Close the dialog and return focus to the button.' },
      ],
    },
    accessibility: {
      role: 'button that opens a dialog (aria-haspopup="dialog")',
      name: '“View receipt”; in PDF tab mode “View receipt (opens in a new tab)”.',
      items: [
        'The dialog is named “Receipt”; the image has the same alternative text, replaceable through `messages.imageAlt` with a more specific one (merchant and date).',
        'The loading spinner is a labelled status; load errors are announced by InlineErrorRetry.',
        'An embedded PDF falls back to a link when the browser has no viewer.',
      ],
    },
    responsive: {
      items: [
        'The dialog is capped to the viewport; images scale to fit and stay at most 70% of the screen height.',
        'At 390px the default PDF tab mode avoids a cramped embedded viewer.',
        'The button and dialog actions are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Images sit on `--mn-color-bg-subtle` so transparent scans stay readable in dark themes.',
        'Messages use `--mn-color-text-muted`; links `--mn-color-text-link`.',
        'Forced-colours mode outlines the image and PDF with CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Image from a URL',
          render: () => (
            <ReceiptPreview mimeType="image/svg+xml" src={SAMPLE_RECEIPT} fileName="lunch.svg" />
          ),
        },
        {
          title: 'Loaded on open',
          render: () => <ReceiptPreview mimeType="image/svg+xml" load={loadSample} />,
        },
        {
          title: 'Unsupported format and no receipt',
          render: () => (
            <>
              <ReceiptPreview mimeType="text/csv" src="data:text/csv,amount%0A12.75" />
              <ReceiptPreview mimeType="image/png" src={null} />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `load` for private files so they are fetched only when someone asks.',
        'Give a specific `messages.imageAlt` when you know the merchant or date.',
        'Keep the stored file as uploaded (HEIC included); the preview explains what it cannot show.',
      ],
      dont: [
        'Do not put public, unexpiring links to private receipts in `src`.',
        'Do not fetch the file on page load just to show the button.',
        'Do not embed PDFs in the dialog for phone-first flows.',
      ],
    },
    related: {
      items: [
        { id: 'file-upload', reason: 'Attaching the receipt in the first place.' },
        { id: 'dialog', reason: 'The overlay used for the preview.' },
        { id: 'inline-error-retry', reason: 'The error row shown when loading fails.' },
        { id: 'payslip-view', reason: 'Other People pay documents.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dialog', 'inline-error-retry', 'spinner'],
    },
    testing: {
      unit: [
        'Image from `src` in a named dialog with Download and the given file name; focus returns on Escape.',
        'Lazy `load` with a Blob: spinner, object URL, default file name, revoked on close.',
        'Failed load shows an error, reports `onLoadError`, and Try again recovers.',
        'HEIC and unsupported types explain themselves.',
        'PDFs open in a new tab (direct and pre-opened tab for lazy loads) or embed with a fallback link.',
        'No-receipt text, custom messages, controlled open state, axe and SSR.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/receipt-preview'],
    },
  },
} satisfies ComponentDocs;
