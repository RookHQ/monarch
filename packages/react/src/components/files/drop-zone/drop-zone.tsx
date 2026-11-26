'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type DragEvent as ReactDragEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  fileMatchesAccept,
  validateFiles,
  type FileRejection,
  type FileUploadMessages,
} from '../file-upload/index.js';

/** Every string a drop zone shows or announces. */
export interface DropZoneMessages {
  /** Overlay text while acceptable files are dragged over. */
  dropHere: string;
  /** Overlay text while the dragged files’ types are not allowed. */
  dropRejected: string;
  /** Text of the button that opens the file picker (the keyboard and screen reader path). */
  browse: string;
  /** Announced after files are accepted. */
  added: (count: number) => string;
  /** Announced with the refusal messages. */
  refused: (count: number) => string;
  /** Validation messages, shared with FileUpload (`invalidType`, `tooLarge`, `tooMany`, `formatBytes`). */
  validation?: Partial<FileUploadMessages> | undefined;
}

export const DROP_ZONE_MESSAGES: DropZoneMessages = {
  dropHere: 'Drop files to upload',
  dropRejected: 'These files are not allowed here',
  browse: 'Upload files',
  added: (n) => `${n} ${n === 1 ? 'file' : 'files'} added.`,
  refused: (n) => `${n} ${n === 1 ? 'file was' : 'files were'} not added.`,
};

export interface DropZoneProps extends HTMLAttributes<HTMLDivElement> {
  /** Receives the accepted and refused files of every drop or pick. The component never uploads. */
  onFiles?: ((accepted: File[], rejected: FileRejection[]) => void) | undefined;
  /**
   * `region` (default): files dropped on this element. `page`: files dropped
   * anywhere in the window while it is mounted, with a full-window overlay.
   */
  scope?: 'region' | 'page' | undefined;
  /** Allowed types, input syntax: `image/*,.pdf`. Checked again after drop. */
  accept?: string | undefined;
  /** Largest size per file, in bytes. */
  maxSize?: number | undefined;
  /** Most files in total, counting `currentCount`. */
  maxFiles?: number | undefined;
  /** Files already attached, counted towards `maxFiles`. */
  currentCount?: number | undefined;
  /** Extra synchronous check; return a message to refuse a file. */
  validate?: ((file: File) => string | null | undefined) | undefined;
  /** Allow several files per drop or pick. Default true. */
  multiple?: boolean | undefined;
  /** Ignore drops and disable the button. */
  disabled?: boolean | undefined;
  /**
   * Show the upload button above the content. Default true. Set false only
   * when the page already has another visible way to pick files (a FileUpload
   * or a toolbar button), because dragging is not available to everyone.
   */
  showButton?: boolean | undefined;
  /** Extra content next to the button, such as a hint ("PDF up to 10 MB"). */
  toolbar?: ReactNode;
  /** List refused files under the button. Default true. */
  showErrors?: boolean | undefined;
  /** The content the zone covers, such as a FileList. */
  children?: ReactNode;
  /** Translated strings. */
  messages?: Partial<DropZoneMessages> | undefined;
}

const hasFiles = (e: { dataTransfer: DataTransfer | null }) =>
  Array.from(e.dataTransfer?.types ?? []).includes('Files');

/** Whether a drag carries files whose MIME types all match `accept` (extensions are unknown until drop). */
function dragAllowed(e: { dataTransfer: DataTransfer | null }, accept?: string): boolean {
  const items = Array.from(e.dataTransfer?.items ?? []).filter((i) => i.kind === 'file');
  if (!accept || items.length === 0) return true;
  if (accept.split(',').some((r) => r.trim().startsWith('.'))) return true;
  return items.every((i) => !i.type || fileMatchesAccept({ name: '', type: i.type }, accept));
}

/**
 * Turns a region (a file list, a chat thread) or the whole page into a drop
 * target for files, with an overlay while files are dragged over and an
 * upload button as the accessible alternative. It checks files with the same
 * rules and messages as FileUpload and hands them to the product.
 */
export const DropZone = forwardRef<HTMLDivElement, DropZoneProps>(function DropZone(
  {
    onFiles,
    scope = 'region',
    accept,
    maxSize,
    maxFiles,
    currentCount = 0,
    validate,
    multiple = true,
    disabled = false,
    showButton = true,
    toolbar,
    showErrors = true,
    children,
    messages: messagesProp,
    className,
    onDragEnter,
    onDragOver,
    onDragLeave,
    onDrop,
    ...rest
  },
  ref,
) {
  const m = { ...DROP_ZONE_MESSAGES, ...messagesProp };
  const id = useId();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const depth = useRef(0);
  const [drag, setDrag] = useState<'accept' | 'reject' | null>(null);
  const [rejections, setRejections] = useState<FileRejection[]>([]);
  const [status, setStatus] = useState('');
  const full = maxFiles !== undefined && currentCount >= maxFiles;
  const blocked = disabled || full;

  // Latest values for the window listeners without re-subscribing every render.
  const take = useRef<(files: File[]) => void>(() => {});
  take.current = (list) => {
    if (blocked || !list.length) return;
    // A single-file zone takes the first valid file and refuses the rest with the count message.
    const limit = multiple
      ? maxFiles
      : Math.min(maxFiles ?? Number.POSITIVE_INFINITY, currentCount + 1);
    const result = validateFiles(list, {
      accept,
      maxSize,
      maxFiles: limit,
      currentCount,
      validate,
      ...(m.validation ? { messages: m.validation } : {}),
    });
    const rejected = result.rejected;
    setRejections(rejected);
    setStatus(
      [
        result.accepted.length ? m.added(result.accepted.length) : '',
        rejected.length ? m.refused(rejected.length) : '',
      ]
        .filter(Boolean)
        .join(' '),
    );
    onFiles?.(result.accepted, rejected);
  };
  const acceptRef = useRef(accept);
  acceptRef.current = accept;
  const blockedRef = useRef(blocked);
  blockedRef.current = blocked;

  useEffect(() => {
    if (scope !== 'page') return;
    // The listeners stay on while the zone is disabled or full: the browser
    // default for a dropped file (opening it, leaving the page) is always
    // cancelled, and only the delivery of the files is skipped.
    let count = 0;
    const enter = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      if (blockedRef.current) return;
      count++;
      setDrag(dragAllowed(e, acceptRef.current) ? 'accept' : 'reject');
    };
    const over = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      // A nested target that already handled the drag keeps its own drop effect.
      if (e.dataTransfer && !blockedRef.current) e.dataTransfer.dropEffect = 'copy';
    };
    const leave = () => {
      count = Math.max(0, count - 1);
      if (count === 0) setDrag(null);
    };
    const drop = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      // A drop target inside the page (a FileUpload, another DropZone, an editor)
      // has already taken these files; taking them here too would upload them twice.
      const handled = e.defaultPrevented;
      e.preventDefault();
      count = 0;
      setDrag(null);
      if (!handled && !blockedRef.current) take.current(Array.from(e.dataTransfer?.files ?? []));
    };
    window.addEventListener('dragenter', enter);
    window.addEventListener('dragover', over);
    window.addEventListener('dragleave', leave);
    window.addEventListener('drop', drop);
    return () => {
      window.removeEventListener('dragenter', enter);
      window.removeEventListener('dragover', over);
      window.removeEventListener('dragleave', leave);
      window.removeEventListener('drop', drop);
      setDrag(null);
    };
  }, [scope]);

  // A zone that becomes disabled or full mid-drag drops its overlay.
  useEffect(() => {
    if (blocked) {
      depth.current = 0;
      setDrag(null);
    }
  }, [blocked]);

  const region = scope === 'region';
  const regionHandlers = {
    onDragEnter: (e: ReactDragEvent<HTMLDivElement>) => {
      onDragEnter?.(e);
      if (!region || !hasFiles(e)) return;
      e.preventDefault();
      if (blocked) return;
      depth.current++;
      setDrag(dragAllowed(e, accept) ? 'accept' : 'reject');
    },
    onDragOver: (e: ReactDragEvent<HTMLDivElement>) => {
      onDragOver?.(e);
      if (!region || !hasFiles(e)) return;
      e.preventDefault();
      e.dataTransfer.dropEffect = blocked ? 'none' : 'copy';
    },
    onDragLeave: (e: ReactDragEvent<HTMLDivElement>) => {
      onDragLeave?.(e);
      if (!region || blocked) return;
      depth.current = Math.max(0, depth.current - 1);
      if (depth.current === 0) setDrag(null);
    },
    onDrop: (e: ReactDragEvent<HTMLDivElement>) => {
      onDrop?.(e);
      if (!region || !hasFiles(e)) return;
      e.preventDefault();
      depth.current = 0;
      setDrag(null);
      if (!blocked) take.current(Array.from(e.dataTransfer.files));
    },
  };

  const errorsId = `${id}-errors`;

  return (
    // Dragging is an extra for pointer users; the upload button is the
    // keyboard and screen reader path, so the root needs no role.
    <div
      ref={ref}
      {...rest}
      className={cx('mn-drop-zone', className)}
      data-scope={scope}
      data-drag={drag ?? undefined}
      data-disabled={blocked || undefined}
      {...regionHandlers}
    >
      {showButton || toolbar ? (
        <div className="mn-drop-zone__bar">
          {showButton ? (
            <Button
              type="button"
              size="sm"
              disabled={blocked}
              aria-describedby={showErrors && rejections.length ? errorsId : undefined}
              onClick={() => inputRef.current?.click()}
            >
              {m.browse}
            </Button>
          ) : null}
          {toolbar}
        </div>
      ) : null}
      <input
        ref={inputRef}
        type="file"
        className="mn-visually-hidden"
        tabIndex={-1}
        aria-hidden
        accept={accept}
        multiple={multiple}
        disabled={blocked}
        onChange={(e) => {
          const files = Array.from(e.currentTarget.files ?? []);
          e.currentTarget.value = '';
          take.current(files);
        }}
      />
      {showErrors && rejections.length ? (
        <ul id={errorsId} className="mn-drop-zone__errors">
          {rejections.map((r, i) => (
            <li key={`${r.file.name}-${i}`} className="mn-drop-zone__error">
              <ErrorGlyph />
              <span>{r.message}</span>
            </li>
          ))}
        </ul>
      ) : null}
      <div className="mn-visually-hidden" role="status">
        {status}
      </div>
      {children}
      {drag ? (
        <div className="mn-drop-zone__overlay" aria-hidden>
          <span className="mn-drop-zone__overlay-text">
            {drag === 'accept' ? m.dropHere : m.dropRejected}
          </span>
        </div>
      ) : null}
    </div>
  );
});
