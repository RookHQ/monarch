import { useEffect, useRef, useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import {
  FileUpload,
  UploadQueue,
  type UploadQueueItem,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MB = 1024 * 1024;

/** Simulates a transfer so the example shows progress; a real product reports its own. */
function UploadWithQueue() {
  const [items, setItems] = useState<UploadQueueItem[]>([]);
  const timers = useRef(new Map<string, ReturnType<typeof setInterval>>());
  useEffect(() => {
    const map = timers.current;
    return () => map.forEach((t) => clearInterval(t));
  }, []);

  const start = (id: string) => {
    clearInterval(timers.current.get(id));
    timers.current.set(
      id,
      setInterval(() => {
        setItems((list) =>
          list.map((i) => {
            if (i.id !== id || i.status !== 'uploading') return i;
            const progress = Math.min(100, (i.progress ?? 0) + 20);
            if (progress < 100) return { ...i, progress };
            clearInterval(timers.current.get(id));
            return { ...i, progress, status: 'done' };
          }),
        );
      }, 400),
    );
  };

  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 480 }}>
      <FileUpload
        accept="image/*,application/pdf"
        maxSize={10 * MB}
        maxFiles={5}
        currentCount={items.filter((i) => i.status !== 'cancelled').length}
        hint="Images or PDF, up to 10 MB each, 5 files max"
        paste
        onFiles={(accepted) => {
          const added = accepted.map((f) => ({
            id: `${f.name}-${f.lastModified}-${Math.random()}`,
            name: f.name,
            size: f.size,
            status: 'uploading' as const,
            progress: 0,
          }));
          setItems((list) => [...list, ...added]);
          added.forEach((a) => start(a.id));
        }}
      />
      {items.length ? (
        <UploadQueue
          items={items}
          onCancel={(id) => {
            clearInterval(timers.current.get(id));
            setItems((l) => l.map((i) => (i.id === id ? { ...i, status: 'cancelled' } : i)));
          }}
          onRetry={(id) => {
            setItems((l) =>
              l.map((i) => (i.id === id ? { ...i, status: 'uploading', progress: 0 } : i)),
            );
            start(id);
          }}
          onRemove={(id) => setItems((l) => l.filter((i) => i.id !== id))}
          onClearFinished={() => setItems((l) => l.filter((i) => i.status === 'uploading'))}
        />
      ) : null}
    </div>
  );
}

export default {
  id: 'file-upload',
  title: 'FileUpload',
  summary:
    'A drop zone and browse button that checks type, size and count, explains refusals and hands files to the product.',
  sections: {
    purpose: {
      summary:
        'Lets people choose files by dropping them, browsing or pasting, checks them against the limits the product sets, and gives the accepted files to the product, which uploads them.',
      useWhen: [
        'A form or screen accepts attachments, images or documents: profile photos, résumés, banners, ticket attachments.',
        'Limits on type, size or count must be explained before upload instead of failing on the server.',
        'Pair with UploadQueue to show the progress the product reports.',
      ],
      avoidWhen: [
        'Files are attached inline in a composer with its own attach button: use the composer’s button and call `validateFiles` yourself.',
        'The user picks from files already stored in the product: use a picker of existing items.',
      ],
    },
    designPrinciples: {
      items: [
        'The component never uploads. It emits `onFiles(accepted, rejected)`; the product transfers, and reports progress through UploadQueue.',
        'The browse button is always the keyboard and screen reader path; the drop zone is an extra for pointer users and is not focusable.',
        'Refusals are specific and name the file: "scan.png is larger than 10 MB.", not "Invalid file".',
        'Limits are stated up front in the hint (generated from `maxSize` and `maxFiles` unless `hint` is given).',
        'The same checks are available on their own as `validateFiles`, so composers and other entry points refuse files the same way.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onFiles(accepted, rejected)` runs on every pick, drop and paste. `rejected` items are `{ file, code, message }`, with `code` one of `file-invalid-type`, `file-too-large`, `too-many-files`, `custom`.',
        '`accept` uses the input syntax (`image/*,.pdf,application/zip`) and is checked again after the pick, because drops and pastes skip the browser’s filter.',
        '`maxFiles` counts `currentCount` (files already attached); when it is reached the zone is disabled and says so.',
        '`validateFiles(files, options)`, `fileMatchesAccept(file, accept)` and `formatBytes(bytes)` are exported for use elsewhere.',
        'Inside a Field, the browse button takes the Field label and description and reflects `invalid`.',
        'The ref is the root `<div>`; other div attributes and drag/paste handlers pass through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['zone', 'compact'],
          description:
            '`zone` (default): a centred, dashed drop area. `compact`: one row with the icon, text and button, for side panels and dense forms.',
        },
        {
          prop: 'multiple',
          values: ['true', 'false'],
          description:
            'Default true unless `maxFiles` is 1. A single-file upload refuses extra dropped files.',
        },
        {
          prop: 'paste',
          values: ['true', 'false'],
          description: 'Also take files pasted while focus is inside the component.',
        },
        {
          prop: 'showErrors',
          values: ['true', 'false'],
          description:
            'Show refusal messages under the zone (default), or leave them to the product.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Drag over',
          attribute: 'data-drag="accept"',
          description: 'Files are over the zone: accent border and background, "Drop to upload".',
        },
        {
          name: 'Drag rejected',
          attribute: 'data-drag="reject"',
          description:
            'The dragged files’ types do not match `accept` (when the browser exposes them): danger border, "This file type is not allowed".',
        },
        {
          name: 'Refused files',
          attribute: 'data-invalid',
          description: 'The last pick had refused files: their messages are listed under the zone.',
        },
        {
          name: 'Disabled or full',
          attribute: 'data-disabled',
          description:
            '`disabled`, or `currentCount` has reached `maxFiles` ("Limit reached"). The button is disabled and drops are ignored.',
        },
        {
          name: 'Focus',
          description: 'The browse button shows the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Browse opens the system file picker. The hidden input is cleared after each pick, so choosing the same file again works.',
        'Dropping files anywhere on the component takes them; dragging text or links over it does nothing.',
        'Each pick replaces the previous refusal messages.',
        'Files over the count limit are refused in order, after the type and size checks, so the first valid files are kept.',
        '`validate` adds a synchronous check of your own (return a message to refuse). Checks that need the network, such as virus scanning, belong in the upload flow and show in UploadQueue.',
        'On phones, `capture="environment"` or `"user"` opens the camera.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the browse button.' },
        { keys: 'Enter / Space', action: 'Opens the system file picker.' },
        {
          keys: 'Ctrl+V / Cmd+V',
          action: 'With `paste`: adds files from the clipboard while focus is inside.',
        },
      ],
    },
    accessibility: {
      role: 'button (browse); the zone has no role',
      name: '"Browse files" by default. Inside a Field it is "{Field label} Browse files".',
      items: [
        'The button is described by the hint, the Field description and any refusal messages, so they are read with it.',
        'Refusal messages sit in a polite live region and are announced when they appear; accepted files are announced as "2 files added."',
        'The file input is visually hidden, out of the tab order and hidden from assistive technology; the button is the single control.',
        'The drop zone is never the only way in (people with keyboards, switches or voice use the button).',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/file-upload.',
      ],
    },
    responsive: {
      items: [
        'The zone fills its container and wraps its text; it works at 320px.',
        '`compact` wraps the button under the text when the row is too narrow.',
        'Long file names in messages break anywhere rather than overflow.',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        'The zone uses `--mn-color-bg-subtle` with a dashed `--mn-color-border-strong` border; drag states use the accent and danger subtle/strong pairs.',
        'Reduced motion removes the border and background transition. Forced colours draw the drag state with `Highlight`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With an upload queue',
          stack: true,
          render: () => <UploadWithQueue />,
        },
        {
          title: 'In a field, single file',
          stack: true,
          render: () => (
            <Field style={{ width: '100%', maxWidth: 480 }}>
              <FieldLabel>Résumé</FieldLabel>
              <FileUpload accept=".pdf,.doc,.docx" maxFiles={1} maxSize={5 * MB} />
              <FieldDescription>
                PDF or Word document. Shared with the hiring team.
              </FieldDescription>
            </Field>
          ),
        },
        {
          title: 'Compact, and limit reached',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 480 }}>
              <FileUpload variant="compact" accept="image/*" maxSize={2 * MB} />
              <FileUpload variant="compact" maxFiles={3} currentCount={3} />
            </div>
          ),
        },
        {
          title: 'The same checks without the zone',
          stack: true,
          render: () => (
            <p style={{ margin: 0 }}>
              <code>validateFiles</code> returns <code>{'{ accepted, rejected }'}</code> with the
              same messages, for a composer’s own attach button.
            </p>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `accept`, `maxSize` and `maxFiles` to the limits the server enforces, so people learn them before uploading.',
        'Keep checking on the server too: client checks are for clarity, not security.',
        'Show the upload itself with UploadQueue, including failures and retry.',
      ],
      dont: [
        'Do not rely on drag and drop alone: keep the browse button visible.',
        'Do not show a generic "Upload failed" for a refusal the component already explained.',
        'Do not put business rules about storage (buckets, keys, quotas) in the component; pass the resulting limits.',
      ],
    },
    related: {
      items: [
        {
          id: 'upload-queue',
          reason: 'Shows the progress, status and retry of the files this component hands over.',
        },
        {
          id: 'form-field',
          reason: 'Gives the browse button a label, description and invalid state.',
        },
        { id: 'progress-bar', reason: 'A single progress bar, when a list is not needed.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field'],
    },
    testing: {
      unit: [
        'Accept rules (MIME, wildcard, extension), size, custom and count checks, with messages that name the file; byte formatting.',
        'Browse opens the picker from the keyboard; picked files are split into accepted and refused; messages describe the button and are cleared on the next pick; the input is reset.',
        'Single-file mode refuses extra files; drag-over accept and reject states; drops; paste only when enabled.',
        'Disabled and limit-reached states ignore input; errors can be left to the product.',
        'Field label, description and invalid state reach the button; compact variant, custom icon and translated messages; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/file-upload',
        'Cross-browser axe: /components/file-upload',
      ],
    },
  },
} satisfies ComponentDocs;
