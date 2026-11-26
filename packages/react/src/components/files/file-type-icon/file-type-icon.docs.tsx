import { FILE_TYPE_FAMILIES, FileTypeIcon } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'file-type-icon',
  title: 'FileTypeIcon',
  summary: 'A labelled glyph for a file’s type family, worked out from its MIME type and name.',
  sections: {
    purpose: {
      summary:
        'Shows what kind of file something is (image, PDF, spreadsheet, archive…) before it is opened, with a label so the type is announced and never shown by colour alone. Needed by: Rook Atlas and Rook Intellect file lists (`components/*/files/FileBits.tsx` map lucide icons by hand), and Monarch’s FileCard, FileList and AttachmentChip.',
      useWhen: [
        'A row, card or chip names a file and should show its type at a glance.',
        'A thumbnail is not available yet (or the file has no visual preview) and something must hold its place.',
      ],
      avoidWhen: [
        'The real picture is available: show the thumbnail and keep the icon as the fallback.',
        'You need a generic action icon (upload, download): use the app’s icon set.',
      ],
    },
    designPrinciples: {
      items: [
        'One page outline for every family, with a small mark inside, so icons line up in lists and read as “a file” first.',
        'The type is always named: the icon is an image with a label unless `decorative` says nearby text already names it.',
        'The MIME type wins when it is specific; the extension is the fallback, because drops and storage often report `application/octet-stream`.',
        'Colour is a secondary hint drawn from the categorical data palette, never a status colour.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`getFileTypeFamily(file | name)` returns the family for `{ name, type }` (a `File` works) or a bare name; `fileExtension(name)` returns the lower-case extension.',
        '`FileTypeGlyph` is the bare decorative SVG for use inside other components; `FILE_TYPE_FAMILIES` lists every family and `FILE_TYPE_ICON_MESSAGES` holds the English labels.',
        'The ref is the root `<span>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Atlas/Intellect `FileTypeIcon({ name, contentType })`: the same two props work unchanged. Their kinds map as `doc`→`document`, `sheet`→`spreadsheet`, `slides`→`presentation`, `media`→`audio`/`video`, `other`→`other`. Drop the `className="h-5 w-5"` sizing and use `size`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description:
            '`sm` 16px for chips, `md` 20px (default) for rows, `lg` a 40px tinted tile for cards and empty previews.',
        },
        {
          prop: 'family',
          values: [...FILE_TYPE_FAMILIES],
          description: 'Normally worked out from `contentType` and `name`; set it to override.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Family',
          attribute: 'data-family',
          description: 'The detected family; drives the mark and the tint.',
        },
        { name: 'Size', attribute: 'data-size', description: '`sm`, `md` or `lg`.' },
      ],
    },
    interaction: { items: [], notes: ['Not interactive: it is an image.'] },
    keyboard: { items: [], notes: ['Not focusable; it has no actions.'] },
    accessibility: {
      role: 'img (none when `decorative`)',
      name: 'The family label, e.g. "PDF document"; override with `label` or `messages`.',
      items: [
        'The SVG inside is `aria-hidden`; the wrapper carries `role="img"` and the label.',
        'Use `decorative` when the same text sits next to it (for example a “PDF” column), so it is not read twice.',
        'The type is never colour-only: shape and label carry it; forced-colours mode draws it in `CanvasText`.',
      ],
    },
    responsive: {
      items: ['Fixed size per variant; never shrinks inside flex rows (`flex-shrink: 0`).'],
    },
    theme: {
      items: [
        'Tints come from `--mn-color-data-1` … `-5` (non-text graphics, 3:1) with `--mn-color-text-muted` for archives and other files.',
        'The `lg` tile uses `--mn-color-bg-muted` and `--mn-radius-md`.',
        'Forced colours: `CanvasText` glyph and an outlined tile.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every family',
          render: () => (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
              {FILE_TYPE_FAMILIES.map((f) => (
                <FileTypeIcon key={f} family={f} size="lg" />
              ))}
            </div>
          ),
        },
        {
          title: 'From a name and MIME type',
          render: () => (
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <FileTypeIcon name="budget-2026.xlsx" size="sm" />
              <FileTypeIcon name="photo.heic" />
              <FileTypeIcon name="export.bin" contentType="application/zip" />
            </div>
          ),
        },
        {
          title: 'Decorative, next to text',
          render: () => (
            <span style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
              <FileTypeIcon name="handbook.pdf" decorative />
              handbook.pdf · PDF
            </span>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass both `name` and `contentType` when you have them.',
        'Use `decorative` when the type is already written next to the icon.',
      ],
      dont: [
        'Do not use the tint to mean anything else (status, ownership).',
        'Do not use it as a button; wrap it in a labelled control instead.',
      ],
    },
    related: {
      items: [
        { id: 'file-card', reason: 'Shows the icon with name, size and actions.' },
        { id: 'attachment-chip', reason: 'A compact chip that uses the `sm` icon.' },
        { id: 'file-list', reason: 'Lists files with this icon in each row.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Family detection from MIME types (with parameters), extensions and missing data; extension parsing.',
        'Role, label, ref and data attributes; every family renders with a label; custom label, translated messages and decorative mode.',
        'axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
