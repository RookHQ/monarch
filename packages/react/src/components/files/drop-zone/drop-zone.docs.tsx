import { useState } from 'react';
import { DropZone, FileList, type FileListItem } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './drop-zone.fixture.json';

const MB = 1024 * 1024;
const seedFiles = fixture.data.files as FileListItem[];

function FolderWithDrop() {
  const [items, setItems] = useState<FileListItem[]>(seedFiles);
  return (
    <div style={{ width: '100%', maxWidth: 560 }}>
      <DropZone
        accept={fixture.data.accept.join(',')}
        maxSize={fixture.data.maxSize}
        toolbar={<span>{fixture.data.hint}</span>}
        onFiles={(accepted) =>
          setItems((list) => [
            ...list,
            ...accepted.map((f) => ({
              id: `${f.name}-${f.lastModified}-${list.length}`,
              name: f.name,
              size: f.size,
              contentType: f.type,
            })),
          ])
        }
      >
        <FileList label={fixture.data.listLabel} items={items} />
      </DropZone>
    </div>
  );
}

export default {
  id: 'drop-zone',
  title: 'DropZone',
  summary:
    'Makes a region or the whole page a drop target for files, with an upload button as the accessible path.',
  sections: {
    purpose: {
      summary:
        'Lets people drop files straight onto the place they belong (a folder’s file list, a conversation, a whole page) instead of a separate upload box, with the same checks and messages as FileUpload. Needed by: Rook Atlas and Rook Intellect (`components/*/files/UploadZone.tsx`), Rook Chat and Rook Tickets conversations.',
      useWhen: [
        'A file list or conversation should accept files dropped anywhere on it.',
        'The whole page should accept files (`scope="page"`), for example a drive or media library.',
      ],
      avoidWhen: [
        'A form field that takes files: use FileUpload, which has its own visible zone.',
        'A composer with an attach button: call `validateFiles` from that button and wrap the composer in a DropZone only if dropping should also work.',
      ],
    },
    designPrinciples: {
      items: [
        'Dragging is an extra; the upload button is always available unless the page has another visible picker.',
        'The overlay appears only while files (not text or links) are dragged, and says whether they will be accepted.',
        'Same rules as FileUpload: `accept`, `maxSize`, `maxFiles` and `validate` use `validateFiles`, so refusals read the same everywhere.',
        'The component never uploads: it hands accepted and refused files to `onFiles`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onFiles(accepted, rejected)` runs after each drop or pick; `rejected` items are `{ file, code, message }` as in FileUpload.',
        '`scope="page"` listens on `window` only while mounted and not disabled; mount one per page.',
        'The ref is the root `<div>`; the content goes in `children`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Atlas/Intellect `UploadZone` hand-made drag handlers (`depth` counter, `over` state, hidden input + “Choose files” button): replace them with `<DropZone onFiles={(accepted) => queue.add(accepted)} accept={…} disabled={full}>`; the module and verify controls go in `toolbar`, and the announcement of accepted files is built in.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'scope',
          values: ['region', 'page'],
          description:
            '`region` (default) covers this element; `page` takes drops anywhere in the window with a fixed overlay.',
        },
        {
          prop: 'showButton',
          values: ['true', 'false'],
          description:
            'Show the upload button (default). Turn off only when another visible picker exists.',
        },
        {
          prop: 'multiple',
          values: ['true', 'false'],
          description: 'Single-file zones keep the first valid file and refuse the rest.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Drag accepted',
          attribute: 'data-drag="accept"',
          description: 'Accent dashed overlay with “Drop files to upload”.',
        },
        {
          name: 'Drag refused',
          attribute: 'data-drag="reject"',
          description: 'The dragged types do not match `accept`: danger overlay.',
        },
        {
          name: 'Disabled or full',
          attribute: 'data-disabled',
          description:
            '`disabled`, or `currentCount` reached `maxFiles`: drops are ignored and the button is disabled.',
        },
        { name: 'Scope', attribute: 'data-scope', description: '`region` or `page`.' },
      ],
    },
    interaction: {
      items: [
        'Dragging files over the region (or page) shows the overlay; leaving hides it; dropping hands the files over.',
        'Dragging text, links or elements does nothing, so reordering inside the region still works.',
        'Refused files are listed under the button until the next drop or pick.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the upload button.' },
        { keys: 'Enter / Space', action: 'Opens the system file picker.' },
      ],
    },
    accessibility: {
      role: 'button (upload); the region has no role',
      name: '“Upload files” by default.',
      items: [
        'Results are announced in a `role="status"` region (“2 files added. 1 file was not added.”).',
        'The button is described by the refusal messages.',
        'The overlay is `aria-hidden`: it only mirrors the drag, which assistive technology users do not need.',
      ],
    },
    responsive: {
      items: [
        'The bar wraps the hint under the button at narrow widths.',
        'The page overlay is inset 12px from the window edges at every size.',
        'Touch devices rarely drag files, so the button carries the task there.',
      ],
    },
    theme: {
      items: [
        'Overlay: `--mn-color-accent-subtle` with a dashed `--mn-color-accent-strong` border; refused uses the danger pair.',
        'Reduced motion removes the overlay fade. Forced colours draw the overlay border in `Highlight` on `Canvas`.',
      ],
    },
    examples: {
      items: [
        { title: 'A file list that accepts drops', stack: true, render: () => <FolderWithDrop /> },
        {
          title: 'Single image, button only',
          render: () => <DropZone accept="image/*" multiple={false} maxSize={2 * MB} />,
        },
      ],
    },
    doDont: {
      do: [
        'Say the limits next to the button with `toolbar`.',
        'Show uploads with UploadQueue or AttachmentChip after `onFiles`.',
      ],
      dont: [
        'Do not hide the button unless another visible picker exists.',
        'Do not mount two page-scoped zones at once.',
      ],
    },
    related: {
      items: [
        {
          id: 'file-upload',
          reason: 'A visible zone with browse button for forms; shares `validateFiles`.',
        },
        { id: 'file-list', reason: 'A common child of the drop zone.' },
        { id: 'upload-queue', reason: 'Shows the progress of the files handed over.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'file-upload'] },
    testing: {
      unit: [
        'Button opens the picker from the keyboard; picked files are checked and announced; the input is reset.',
        'Overlay on file drag, hidden on leave and drop; drops handed over.',
        'Refused type while dragging; refusals listed, announced and describing the button.',
        'Single-file mode; disabled and full states.',
        'Page scope listens on the window and cleans up; non-file drags ignored; translated button; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
