import { useState } from 'react';
import { MediaUpload, type MediaUploadContext } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Pretends to store the file: reports progress, then returns a local object URL. */
function simulateUpload(file: File, { onProgress, signal }: MediaUploadContext): Promise<string> {
  return new Promise((resolve, reject) => {
    let pct = 0;
    const timer = window.setInterval(() => {
      pct += 20;
      onProgress(pct);
      if (pct >= 100) {
        window.clearInterval(timer);
        resolve(URL.createObjectURL(file));
      }
    }, 250);
    signal.addEventListener('abort', () => {
      window.clearInterval(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });
}

function ImageOnly() {
  const [url, setUrl] = useState<string | null>(null);
  return (
    <div style={{ maxWidth: 420 }}>
      <MediaUpload
        label="Cover image"
        description="Shown at the top of the page."
        value={url}
        onChange={setUrl}
        onUpload={simulateUpload}
        maxImageSize={10 * 1024 * 1024}
        videoUnavailable="Videos can’t be added to this item."
      />
    </div>
  );
}

function ImageOrVideo() {
  const [url, setUrl] = useState<string | null>(null);
  const [kind, setKind] = useState<'image' | 'video' | undefined>(undefined);
  return (
    <div style={{ maxWidth: 420 }}>
      <MediaUpload
        label="Tile media"
        kinds={['image', 'video']}
        value={url}
        valueKind={kind}
        onChange={(next, details) => {
          setUrl(next);
          setKind(details.kind ?? undefined);
        }}
        onUpload={simulateUpload}
        maxImageSize={10 * 1024 * 1024}
        maxVideoSize={20 * 1024 * 1024}
        maxVideoDuration={90}
      />
    </div>
  );
}

function Failing() {
  return (
    <div style={{ maxWidth: 420 }}>
      <MediaUpload
        label="Profile banner"
        onUpload={() =>
          new Promise((_, reject) =>
            window.setTimeout(
              () => reject(new Error('Storage is full. Remove an old file and try again.')),
              600,
            ),
          )
        }
      />
    </div>
  );
}

export default {
  id: 'media-upload',
  title: 'MediaUpload',
  summary:
    'One image or video field: drop or choose, checks, progress, preview, replace and remove.',
  sections: {
    purpose: {
      summary:
        'Collects a single image (or video) for a record, such as a cover, a tile or a banner. It checks type, size and video length before anything is sent, shows progress and a preview, and leaves storage to the product through `onUpload`. Needed by: Links.',
      useWhen: [
        'A form field that holds exactly one picture or clip.',
        'Media where the product needs size or length limits and a preview before saving.',
      ],
      avoidWhen: [
        'Several files or attachments: use FileUpload with UploadQueue.',
        'A profile picture with cropping: use an avatar-specific flow.',
      ],
    },
    designPrinciples: {
      items: [
        'Check before sending: type, size per kind and video length are refused with the file’s name and the limit.',
        'Monarch does no network or compression work: `onUpload` stores the file and `transform` can shrink it first.',
        'Plan or policy limits are the product’s words (`videoUnavailable`), shown before someone tries and again if they do.',
        'Focus is never lost: after an upload it moves to Replace, after Remove back to the chooser.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`MediaUpload` forwards its ref to the root `<div>` and composes FileUpload, Progress, Field and InlineNotice.',
        '`onUpload(file, { kind, onProgress, signal })` resolves with a URL, or `{ url, warnings }` to show warnings.',
        '`mediaKindOf(urlOrType)` guesses image or video; `readVideoDuration(file)` reads a clip’s length from its metadata.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` is controlled (or `defaultValue`); `onChange(url | null, { kind, file? })` reports uploads and removals.',
        'Limits are opt-in: `maxImageSize`, `maxVideoSize` (bytes) and `maxVideoDuration` (seconds).',
        'Migrating from the app copy (Links `MediaUpload`): `onMediaUploaded(url)` → `onChange(url)` (Remove sends `null` instead of `""`); `currentMedia` → `value`; `label` is unchanged; `allowVideo` → `kinds={["image", "video"]}`; `isPaidUser` and the "Pro+" hint → `videoUnavailable` with the product’s own words; `workspaceId`, `useAuth` and `secureFileUploadService.uploadFile` → your `onUpload` (call `onProgress` from its progress callback and return `{ url, warnings }`); `browser-image-compression` → `transform`; the 20 MB / 10 MB / 90 s limits → `maxVideoSize`, `maxImageSize`, `maxVideoDuration`; toasts → the built-in messages (or your own from `onChange`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kinds',
          values: ["['image']", "['image', 'video']", "['video']"],
          description: 'Which media the field takes; labels and hints follow.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: 'The drop zone with the hint.',
        },
        {
          name: 'Checking',
          attribute: 'data-state="checking"',
          description: 'Reading a video’s length.',
        },
        {
          name: 'Uploading',
          attribute: 'data-state="uploading"',
          description: 'A progress bar (indeterminate until progress is reported) with Cancel.',
        },
        {
          name: 'Filled',
          attribute: 'data-state="filled"',
          description: 'The preview with Replace and Remove.',
        },
        {
          name: 'Error',
          attribute: 'role="alert" (InlineNotice)',
          description: 'The refusal or upload error, announced assertively.',
        },
        { name: 'Disabled', attribute: 'disabled', description: 'Every control is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Drop a file on the zone, or press Choose and pick one.',
        'Cancel aborts `signal`; the field returns to its previous state and says "Upload cancelled".',
        'Replace opens the file picker again; Remove clears the value.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Choose (or Replace and Remove, or Cancel while uploading).' },
        { keys: 'Enter / Space', action: 'Opens the file picker or runs the focused action.' },
      ],
    },
    accessibility: {
      role: 'group of form controls inside a Field',
      name: 'The visible `label` (FieldLabel)',
      items: [
        'The progress bar is named "Uploading photo.png" and exposes its value.',
        'Success, cancel and removal are announced in a polite live region; errors are announced assertively.',
        'Image previews have a text alternative (`previewAlt`); video previews have native controls.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; at 390px the zone stacks and buttons wrap.',
        'Buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Zone and errors come from FileUpload and InlineNotice; the preview uses `--mn-color-border-default` and `--mn-color-bg-muted`.',
        'Forced colours: preview and progress borders use CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Image with a size limit, videos unavailable',
          render: () => <ImageOnly />,
          stack: true,
        },
        { title: 'Image or video with length limit', render: () => <ImageOrVideo />, stack: true },
        { title: 'Upload error', render: () => <Failing />, stack: true },
      ],
    },
    doDont: {
      do: [
        'Validate again on the server; client checks are for fast feedback.',
        'Honour `signal` in `onUpload` so Cancel really stops the transfer.',
        'Say why videos are unavailable in plain words, without plan jargon.',
      ],
      dont: [
        'Do not upload in `transform`; it only prepares the file.',
        'Do not use it for several files.',
      ],
    },
    related: {
      items: [
        {
          id: 'file-upload',
          reason: 'The drop zone it builds on; use it directly for several files.',
        },
        { id: 'upload-queue', reason: 'Progress for many uploads at once.' },
        { id: 'video-thumbnail', reason: 'Show uploaded videos in grids.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'progress-bar', 'inline-notice', 'form-field', 'file-upload'],
    },
    testing: {
      unit: [
        'mediaKindOf detects video by type and extension.',
        'Upload with progress, preview, announcement and focus on Replace; axe passes empty and filled.',
        'Remove clears the value, announces it and returns focus to the chooser.',
        'Refuses videos with the product’s reason; checks size per kind and video length (object URL revoked).',
        'Cancel aborts the signal; upload errors are shown.',
        'transform runs before upload; result warnings are listed; video values preview with controls.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
