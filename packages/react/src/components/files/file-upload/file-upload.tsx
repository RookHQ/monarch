'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type ClipboardEvent,
  type DragEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { useFieldContext } from '../../forms/form-field/index.js';

/** Why a file was refused. */
export type FileRejectionCode =
  'file-invalid-type' | 'file-too-large' | 'too-many-files' | 'custom';

/** A refused file and the reason, with a message ready to show. */
export interface FileRejection {
  file: File;
  code: FileRejectionCode;
  message: string;
}

/** Limits a set of files is checked against. */
export interface FileValidationOptions {
  /** Same syntax as the input `accept` attribute: `image/*,.pdf,application/zip`. */
  accept?: string | undefined;
  /** Largest allowed size per file, in bytes. */
  maxSize?: number | undefined;
  /** Most files allowed in total, counting `currentCount`. */
  maxFiles?: number | undefined;
  /** Files already attached, which count towards `maxFiles`. Default 0. */
  currentCount?: number | undefined;
  /** Extra check. Return a message to refuse the file, or null to allow it. */
  validate?: ((file: File) => string | null | undefined) | undefined;
  /** Override the messages. */
  messages?: Partial<FileUploadMessages>;
}

/** Labels, hints and error messages. Override them to translate. */
export interface FileUploadMessages {
  /** Main text in the drop zone. */
  label: string;
  /** Text of the browse button. */
  browse: string;
  /** Shown while files are dragged over the zone. */
  dropHere: string;
  /** Shown while dragged files do not match `accept`. */
  dropRejected: string;
  invalidType: (name: string) => string;
  tooLarge: (name: string, maxSize: string) => string;
  tooMany: (name: string, maxFiles: number) => string;
  /** Generated hint from the limits, when no `hint` is given. */
  limits: (parts: { maxSize?: string; maxFiles?: number }) => string;
  /** Hint shown when `currentCount` has reached `maxFiles`. */
  limitReached: (maxFiles: number) => string;
  /** Announced after files are accepted. */
  added: (count: number) => string;
  /** Formats a size in bytes. */
  formatBytes: (bytes: number) => string;
}

/** Formats bytes as "512 B", "1.5 KB", "10 MB" (base 1024). */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  const rounded = unit === 0 ? value : Math.round(value * 10) / 10;
  return `${rounded} ${units[unit]}`;
}

const DEFAULT_MESSAGES: FileUploadMessages = {
  label: 'Drag and drop files here',
  browse: 'Browse files',
  dropHere: 'Drop to upload',
  dropRejected: 'This file type is not allowed',
  invalidType: (name) => `${name} is not an allowed file type.`,
  tooLarge: (name, max) => `${name} is larger than ${max}.`,
  tooMany: (name, max) => `${name} was not added: up to ${max} ${max === 1 ? 'file' : 'files'}.`,
  limits: ({ maxSize, maxFiles }) =>
    [
      maxSize ? `Up to ${maxSize} each` : '',
      maxFiles ? `${maxFiles} ${maxFiles === 1 ? 'file' : 'files'} max` : '',
    ]
      .filter(Boolean)
      .join(' · '),
  limitReached: (max) => `Limit reached: ${max} ${max === 1 ? 'file' : 'files'} attached.`,
  added: (n) => `${n} ${n === 1 ? 'file' : 'files'} added.`,
  formatBytes,
};

/** Whether `file` matches an `accept` string (MIME types, wildcards, extensions). */
export function fileMatchesAccept(file: { name: string; type: string }, accept?: string): boolean {
  if (!accept?.trim()) return true;
  const name = file.name.toLowerCase();
  const type = (file.type || '').toLowerCase();
  return accept
    .split(',')
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean)
    .some((rule) => {
      if (rule.startsWith('.')) return name.endsWith(rule);
      if (rule.endsWith('/*')) return type.startsWith(rule.slice(0, -1));
      return type === rule;
    });
}

/**
 * Splits files into accepted and refused against `accept`, `maxSize`,
 * `maxFiles` and `validate`. Files over the count limit are refused in order,
 * after the other checks, so the first valid files are kept.
 */
export function validateFiles(
  files: readonly File[],
  options: FileValidationOptions = {},
): { accepted: File[]; rejected: FileRejection[] } {
  const m = { ...DEFAULT_MESSAGES, ...options.messages };
  const accepted: File[] = [];
  const rejected: FileRejection[] = [];
  const room =
    options.maxFiles === undefined
      ? Infinity
      : Math.max(0, options.maxFiles - (options.currentCount ?? 0));
  for (const file of files) {
    if (!fileMatchesAccept(file, options.accept)) {
      rejected.push({ file, code: 'file-invalid-type', message: m.invalidType(file.name) });
    } else if (options.maxSize !== undefined && file.size > options.maxSize) {
      rejected.push({
        file,
        code: 'file-too-large',
        message: m.tooLarge(file.name, m.formatBytes(options.maxSize)),
      });
    } else {
      const custom = options.validate?.(file);
      if (custom) rejected.push({ file, code: 'custom', message: custom });
      else if (accepted.length >= room)
        rejected.push({
          file,
          code: 'too-many-files',
          message: m.tooMany(file.name, options.maxFiles ?? 0),
        });
      else accepted.push(file);
    }
  }
  return { accepted, rejected };
}

export interface FileUploadProps
  extends
    Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'children'>,
    Omit<FileValidationOptions, 'messages'> {
  /**
   * Called with every pick, drop or paste: the files that passed the checks
   * and the ones that did not. The product uploads `accepted` and reports
   * progress (for example through UploadQueue).
   */
  onFiles?: (accepted: File[], rejected: FileRejection[]) => void;
  /** Allow choosing several files. Default: true unless `maxFiles` is 1. */
  multiple?: boolean;
  /** Also accept files pasted (Ctrl/Cmd+V) while focus is inside the component. */
  paste?: boolean;
  disabled?: boolean;
  /** Mark the zone invalid, for example when a required file is missing. */
  invalid?: boolean;
  /** `zone` (default): a bordered drop area. `compact`: one line, for tight spaces. */
  variant?: 'zone' | 'compact';
  /** Icon at the start of the zone. Decorative. */
  icon?: ReactNode;
  /** Text under the label. Default: generated from `maxSize` and `maxFiles`. */
  hint?: ReactNode;
  /**
   * Show refusal messages under the zone (default true). Set false to show
   * them yourself from `onFiles`.
   */
  showErrors?: boolean;
  /** The input `capture` attribute, for camera capture on phones. */
  capture?: 'user' | 'environment';
  /** Name of the hidden file input. */
  name?: string;
  /** Override labels and messages. */
  messages?: Partial<FileUploadMessages>;
}

function UploadGlyph() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M12 15V4" />
      <path d="m7 9 5-5 5 5" />
      <path d="M20 15v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3" />
    </svg>
  );
}

/** Whether a drag carries files whose types all match `accept` (best effort: types only). */
function dragMatches(e: DragEvent, accept?: string): boolean {
  const items = Array.from(e.dataTransfer?.items ?? []).filter((i) => i.kind === 'file');
  if (!accept || items.length === 0) return true;
  // Extensions are unknown until drop; only reject when every rule is a MIME rule.
  const rules = accept.split(',').map((s) => s.trim());
  if (rules.some((r) => r.startsWith('.'))) return true;
  return items.every((i) => !i.type || fileMatchesAccept({ name: '', type: i.type }, accept));
}

/**
 * A drop zone with a browse button for choosing files. It checks type, size
 * and count, shows clear messages for refused files, and hands the accepted
 * ones to the product, which does the upload.
 */
export const FileUpload = forwardRef<HTMLDivElement, FileUploadProps>(function FileUpload(
  {
    onFiles,
    accept,
    maxSize,
    maxFiles,
    currentCount = 0,
    validate,
    multiple,
    paste = false,
    disabled: disabledProp,
    invalid: invalidProp,
    variant = 'zone',
    icon,
    hint,
    showErrors = true,
    capture,
    name,
    messages: messagesProp,
    className,
    onDragEnter,
    onDragOver,
    onDragLeave,
    onDrop,
    onPaste,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const field = useFieldContext();
  const disabled = disabledProp ?? field?.disabled ?? false;
  const full = maxFiles !== undefined && currentCount >= maxFiles;
  const blocked = disabled || full;
  const id = useId();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const depth = useRef(0);
  const [dragState, setDragState] = useState<'accept' | 'reject' | null>(null);
  const [rejections, setRejections] = useState<FileRejection[]>([]);
  const [status, setStatus] = useState('');
  const allowMultiple = multiple ?? maxFiles !== 1;
  const invalid = invalidProp ?? field?.invalid ?? false;

  const take = (list: readonly File[]) => {
    if (blocked || list.length === 0) return;
    const files = allowMultiple ? list : list.slice(0, 1);
    const extra = allowMultiple ? [] : list.slice(1);
    const result = validateFiles(files, {
      accept,
      maxSize,
      maxFiles,
      currentCount,
      validate,
      messages: m,
    });
    const rejected = [
      ...result.rejected,
      ...extra.map((file) => ({
        file,
        code: 'too-many-files' as const,
        message: m.tooMany(file.name, 1),
      })),
    ];
    setRejections(rejected);
    setStatus(result.accepted.length ? m.added(result.accepted.length) : '');
    onFiles?.(result.accepted, rejected);
  };

  const generatedHint = m.limits({
    ...(maxSize !== undefined ? { maxSize: m.formatBytes(maxSize) } : {}),
    ...(maxFiles !== undefined ? { maxFiles } : {}),
  });
  const hintContent =
    full && maxFiles !== undefined ? m.limitReached(maxFiles) : (hint ?? (generatedHint || null));
  const hintId = `${id}-hint`;
  const errorsId = `${id}-errors`;
  const labelId = `${id}-label`;
  const buttonId = field?.controlId ?? `${id}-browse`;
  const describedBy =
    [
      hintContent ? hintId : null,
      field?.hasDescription ? field.descriptionId : null,
      field?.hasError && invalid ? field.errorId : null,
      showErrors && rejections.length ? errorsId : null,
    ]
      .filter(Boolean)
      .join(' ') || undefined;

  const hasFiles = (e: DragEvent) => Array.from(e.dataTransfer?.types ?? []).includes('Files');

  return (
    // The drop and paste handlers are an extra for pointer users; the browse
    // button is the keyboard and screen reader path, so the root needs no role.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={ref}
      {...rest}
      className={cx('mn-file-upload', className)}
      data-variant={variant}
      data-drag={dragState ?? undefined}
      data-disabled={blocked || undefined}
      data-invalid={invalid || rejections.length > 0 || undefined}
      onDragEnter={(e) => {
        onDragEnter?.(e);
        if (blocked || !hasFiles(e)) return;
        e.preventDefault();
        depth.current++;
        setDragState(dragMatches(e, accept) ? 'accept' : 'reject');
      }}
      onDragOver={(e) => {
        onDragOver?.(e);
        if (blocked || !hasFiles(e)) return;
        e.preventDefault();
        if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy';
      }}
      onDragLeave={(e) => {
        onDragLeave?.(e);
        if (blocked) return;
        depth.current = Math.max(0, depth.current - 1);
        if (depth.current === 0) setDragState(null);
      }}
      onDrop={(e) => {
        onDrop?.(e);
        if (blocked || !e.dataTransfer?.files) return;
        e.preventDefault();
        depth.current = 0;
        setDragState(null);
        take(Array.from(e.dataTransfer.files));
      }}
      onPaste={(e: ClipboardEvent<HTMLDivElement>) => {
        onPaste?.(e);
        if (!paste || blocked) return;
        const files = Array.from(e.clipboardData?.files ?? []);
        if (!files.length) return;
        e.preventDefault();
        take(files);
      }}
    >
      <div className="mn-file-upload__zone">
        <span className="mn-file-upload__icon" aria-hidden>
          {icon ?? <UploadGlyph />}
        </span>
        <div className="mn-file-upload__text">
          <p id={labelId} className="mn-file-upload__label">
            {dragState === 'accept'
              ? m.dropHere
              : dragState === 'reject'
                ? m.dropRejected
                : m.label}
          </p>
          {hintContent ? (
            <p id={hintId} className="mn-file-upload__hint">
              {hintContent}
            </p>
          ) : null}
        </div>
        <Button
          id={buttonId}
          type="button"
          variant="secondary"
          size="sm"
          className="mn-file-upload__browse"
          disabled={blocked}
          aria-labelledby={field ? `${field.labelId} ${buttonId}-text` : undefined}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          onClick={() => inputRef.current?.click()}
        >
          <span id={`${buttonId}-text`}>{m.browse}</span>
        </Button>
        <input
          ref={inputRef}
          type="file"
          className="mn-visually-hidden"
          tabIndex={-1}
          aria-hidden
          accept={accept}
          multiple={allowMultiple}
          capture={capture}
          name={name}
          disabled={blocked}
          onChange={(e) => {
            const files = Array.from(e.currentTarget.files ?? []);
            // Clear so choosing the same file again still fires a change.
            e.currentTarget.value = '';
            take(files);
          }}
        />
      </div>
      <div id={errorsId} className="mn-file-upload__errors" aria-live="polite">
        {showErrors && rejections.length ? (
          <ul className="mn-file-upload__error-list">
            {rejections.map((r, i) => (
              // A file can be refused twice in one pick (same name); order is identity.
              <li key={i} className="mn-file-upload__error" data-code={r.code}>
                <ErrorGlyph />
                <span>{r.message}</span>
              </li>
            ))}
          </ul>
        ) : null}
      </div>
      <span className="mn-visually-hidden" aria-live="polite">
        {status}
      </span>
    </div>
  );
});
