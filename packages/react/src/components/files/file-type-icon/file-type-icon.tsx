import { forwardRef, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';

/** The families of file a FileTypeIcon can draw. */
export type FileTypeFamily =
  | 'image'
  | 'video'
  | 'audio'
  | 'pdf'
  | 'document'
  | 'spreadsheet'
  | 'presentation'
  | 'archive'
  | 'code'
  | 'text'
  | 'other';

/** Every family, in a stable order (useful for legends and tests). */
export const FILE_TYPE_FAMILIES: readonly FileTypeFamily[] = [
  'image',
  'video',
  'audio',
  'pdf',
  'document',
  'spreadsheet',
  'presentation',
  'archive',
  'code',
  'text',
  'other',
];

const EXTENSIONS: Record<Exclude<FileTypeFamily, 'other'>, readonly string[]> = {
  image: [
    'png',
    'jpg',
    'jpeg',
    'gif',
    'webp',
    'avif',
    'svg',
    'bmp',
    'ico',
    'heic',
    'heif',
    'tif',
    'tiff',
  ],
  video: ['mp4', 'm4v', 'mov', 'webm', 'mkv', 'avi', 'ogv', 'wmv'],
  audio: ['mp3', 'wav', 'ogg', 'oga', 'm4a', 'aac', 'flac', 'opus', 'weba'],
  pdf: ['pdf'],
  document: ['doc', 'docx', 'odt', 'rtf', 'pages'],
  spreadsheet: ['xls', 'xlsx', 'ods', 'csv', 'tsv', 'numbers'],
  presentation: ['ppt', 'pptx', 'odp', 'key'],
  archive: ['zip', 'rar', '7z', 'tar', 'gz', 'tgz', 'bz2', 'xz'],
  code: [
    'js',
    'jsx',
    'ts',
    'tsx',
    'mjs',
    'cjs',
    'json',
    'html',
    'htm',
    'css',
    'scss',
    'py',
    'rb',
    'go',
    'rs',
    'java',
    'kt',
    'swift',
    'c',
    'h',
    'cpp',
    'cs',
    'php',
    'sh',
    'sql',
    'yaml',
    'yml',
    'xml',
    'toml',
  ],
  text: ['txt', 'md', 'markdown', 'log', 'ini'],
};

const BY_EXTENSION = new Map<string, FileTypeFamily>();
for (const [family, list] of Object.entries(EXTENSIONS)) {
  for (const ext of list) BY_EXTENSION.set(ext, family as FileTypeFamily);
}

const MIME_RULES: readonly [RegExp, FileTypeFamily][] = [
  [/^image\//, 'image'],
  [/^video\//, 'video'],
  [/^audio\//, 'audio'],
  [/^application\/pdf$/, 'pdf'],
  [/(spreadsheet|ms-excel|text\/csv|tab-separated)/, 'spreadsheet'],
  [/(presentation|ms-powerpoint)/, 'presentation'],
  [/(wordprocessing|msword|opendocument\.text|rtf)/, 'document'],
  [/(zip|x-tar|gzip|x-7z|x-rar|x-bzip|x-xz|compressed)/, 'archive'],
  [/(javascript|typescript|json|xml|x-sh|x-python|html|css|x-yaml|sql)/, 'code'],
  [/^text\//, 'text'],
];

/** The lower-case extension of a file name, without the dot ('' when there is none). */
export function fileExtension(name: string | undefined | null): string {
  if (!name) return '';
  const base = name.split(/[\\/]/).pop() ?? '';
  const dot = base.lastIndexOf('.');
  return dot > 0 ? base.slice(dot + 1).toLowerCase() : '';
}

/**
 * Works out the family of a file from its MIME type and name. The MIME type
 * wins when it is specific; `application/octet-stream` and empty types fall
 * back to the extension.
 */
export function getFileTypeFamily(
  file: { name?: string | undefined; type?: string | undefined } | string,
): FileTypeFamily {
  const name = typeof file === 'string' ? file : file.name;
  const raw = typeof file === 'string' ? '' : (file.type ?? '');
  const type = (raw.toLowerCase().split(';')[0] ?? '').trim();
  if (type && type !== 'application/octet-stream') {
    for (const [rule, family] of MIME_RULES) if (rule.test(type)) return family;
  }
  return BY_EXTENSION.get(fileExtension(name)) ?? 'other';
}

/** The accessible label of each family. */
export type FileTypeIconMessages = Record<FileTypeFamily, string>;

export const FILE_TYPE_ICON_MESSAGES: FileTypeIconMessages = {
  image: 'Image',
  video: 'Video',
  audio: 'Audio',
  pdf: 'PDF document',
  document: 'Document',
  spreadsheet: 'Spreadsheet',
  presentation: 'Presentation',
  archive: 'Archive',
  code: 'Code',
  text: 'Text file',
  other: 'File',
};

const svg = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

/** The page outline with a folded corner, shared by every family. */
const PAGE = <path d="M14 2.75H6.75a2 2 0 0 0-2 2v14.5a2 2 0 0 0 2 2h10.5a2 2 0 0 0 2-2V8z" />;
const FOLD = <path d="M14 2.75V8h5.25" />;

const MARKS: Record<FileTypeFamily, ReactNode> = {
  image: (
    <>
      <circle cx="9.5" cy="11" r="1.25" />
      <path d="M7.5 18l3.5-3.5 2 2 1.75-1.75 2 3.25" />
    </>
  ),
  video: <path d="M10 11.25v5.5l4.5-2.75z" />,
  audio: (
    <>
      <path d="M11 17.25v-5.5l4-1v5" />
      <circle cx="9.75" cy="17.25" r="1.25" />
      <circle cx="13.75" cy="15.75" r="1.25" />
    </>
  ),
  pdf: <path d="M8.5 17.5c1.5-1 3-3.75 3.5-6.5.25 2.25 1.75 4.5 3.5 5-1.75-.25-4.75 0-7 1.5z" />,
  document: <path d="M8.5 12h7M8.5 15h7M8.5 18h4.5" />,
  spreadsheet: (
    <>
      <rect x="8" y="11" width="8" height="7.5" rx="0.5" />
      <path d="M8 14.75h8M12 11v7.5" />
    </>
  ),
  presentation: (
    <>
      <rect x="8" y="11" width="8" height="5.5" rx="0.5" />
      <path d="M12 16.5v2" />
    </>
  ),
  archive: <path d="M11 3v1.5M12.5 6v1.5M11 9v1.5M12.5 12v1.5M10.75 15.5h2v3h-2z" />,
  code: <path d="M10 12l-2 2.5 2 2.5M14 12l2 2.5-2 2.5" />,
  text: <path d="M8.5 12h7M8.5 15h7M8.5 18h7" />,
  other: null,
};

/** The glyph alone, for use inside other components. Decorative (aria-hidden). */
export const FileTypeGlyph = forwardRef<
  SVGSVGElement,
  { family: FileTypeFamily } & Omit<SVGProps<SVGSVGElement>, 'ref'>
>(function FileTypeGlyph({ family, ...props }, ref) {
  return (
    <svg {...svg} {...props} ref={ref}>
      {PAGE}
      {FOLD}
      {MARKS[family]}
    </svg>
  );
});
FileTypeGlyph.displayName = 'FileTypeGlyph';

export interface FileTypeIconProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The file name; its extension decides the family when the MIME type does not. */
  name?: string | undefined;
  /** The MIME type (`File.type`, a `Content-Type` header). Takes precedence over the extension. */
  contentType?: string | undefined;
  /** Set the family directly instead of working it out from `name` and `contentType`. */
  family?: FileTypeFamily | undefined;
  /** Rendered size. Default `md` (20px). `sm` is 16px, `lg` is 32px with a tinted tile behind it. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Accessible label. Default: the family's label from `messages` ("PDF document"). */
  label?: string | undefined;
  /**
   * Hide the icon from assistive technology, when the file type is already
   * said in nearby text. Default false: the icon is an image with a label.
   */
  decorative?: boolean | undefined;
  /** Translated family labels. */
  messages?: Partial<FileTypeIconMessages> | undefined;
}

/**
 * A glyph for a file's type family (image, PDF, spreadsheet, archive and so
 * on), worked out from its MIME type and name, with an accessible label so
 * the type is never conveyed by colour or shape alone.
 */
export const FileTypeIcon = forwardRef<HTMLSpanElement, FileTypeIconProps>(function FileTypeIcon(
  {
    name,
    contentType,
    family: familyProp,
    size = 'md',
    label,
    decorative = false,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const family = familyProp ?? getFileTypeFamily({ name, type: contentType });
  const text = label ?? { ...FILE_TYPE_ICON_MESSAGES, ...messages }[family];
  return (
    <span
      ref={ref}
      {...(decorative ? { 'aria-hidden': true } : { role: 'img', 'aria-label': text })}
      {...rest}
      className={cx('mn-file-type-icon', className)}
      data-family={family}
      data-size={size}
    >
      <FileTypeGlyph family={family} className="mn-file-type-icon__glyph" />
    </span>
  );
});
