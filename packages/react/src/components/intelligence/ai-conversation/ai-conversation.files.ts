/**
 * Attachments: turning the `File`s a person attached into `file` parts the
 * model service can read, by uploading them (`uploadFile`) or, only when a
 * product opts in, by inlining small files as `data:` URLs. Files are never
 * dropped silently: without either, sending reports an error instead.
 */
import type { AiFilePart } from './ai-conversation.types.js';
import type { AiConversationMessages } from './ai-conversation.errors.js';

/** What `uploadFile` receives besides the file. */
export interface AiUploadContext {
  /** Aborted when the person presses Stop or the conversation unmounts. */
  signal: AbortSignal;
  /** Report progress from 0 to 1; shown through `useAiConversation().uploads`. */
  onProgress: (fraction: number) => void;
}

/** Uploads one file to your service and returns the part that refers to it (`url` or `fileId`). */
export type AiUploadFile = (file: File, context: AiUploadContext) => Promise<AiFilePart>;

/**
 * Opt-in inlining of small files as `data:` URLs, for services that take the
 * bytes in the request. Off by default: inlined files make every later
 * request in the conversation larger.
 */
export interface AiInlineFilesOptions {
  /** Largest file to inline, in bytes. */
  maxBytes: number;
  /** MIME types allowed, `image/*` style wildcards included. Default: any. */
  mediaTypes?: readonly string[];
}

/**
 * One attachment on its way: the shape AiAttachmentTray takes
 * (`AiAttachmentTrayFile`), so `uploads` can be passed straight to it.
 */
export interface AiUploadState {
  id: string;
  name: string;
  size: number;
  type: string;
  /** From 0 to 1. */
  progress: number;
  error?: string;
}

/** True for an `AiFilePart` (already uploaded), false for a browser `File`. */
export function isFilePart(value: AiFilePart | File): value is AiFilePart {
  return (
    typeof value === 'object' &&
    value !== null &&
    (value as { type?: unknown }).type === 'file' &&
    typeof (value as { mediaType?: unknown }).mediaType === 'string'
  );
}

/** Whether `mediaType` matches one of `patterns` (`image/*`, `application/pdf`). */
export function matchesMediaType(mediaType: string, patterns?: readonly string[]): boolean {
  if (!patterns?.length) return true;
  const type = mediaType.toLowerCase();
  return patterns.some((p) => {
    const want = p.toLowerCase().trim();
    return want.endsWith('/*') ? type.startsWith(want.slice(0, -1)) : type === want;
  });
}

function toBase64(bytes: Uint8Array): string {
  let binary = '';
  const step = 0x8000;
  for (let i = 0; i < bytes.length; i += step)
    binary += String.fromCharCode(...bytes.subarray(i, i + step));
  return btoa(binary);
}

/** Reads a file into a `data:` URL. Rejects when aborted. */
export async function readFileAsDataUrl(file: File, signal?: AbortSignal): Promise<string> {
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError');
  const type = file.type || 'application/octet-stream';
  if (typeof file.arrayBuffer === 'function') {
    const bytes = new Uint8Array(await file.arrayBuffer());
    if (signal?.aborted) throw new DOMException('Aborted', 'AbortError');
    return `data:${type};base64,${toBase64(bytes)}`;
  }
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    const onAbort = () => {
      reader.abort();
      reject(new DOMException('Aborted', 'AbortError'));
    };
    signal?.addEventListener('abort', onAbort, { once: true });
    reader.onload = () => {
      signal?.removeEventListener('abort', onAbort);
      resolve(String(reader.result));
    };
    reader.onerror = () => {
      signal?.removeEventListener('abort', onAbort);
      reject(reader.error ?? new Error('read failed'));
    };
    reader.readAsDataURL(file);
  });
}

export type AiAttachmentResult =
  | { ok: true; parts: AiFilePart[] }
  | {
      ok: false;
      code: 'upload-failed' | 'attachments-unsupported';
      message: string;
      aborted: boolean;
    };

export interface ResolveAttachmentsOptions {
  uploadFile?: AiUploadFile | undefined;
  inlineFiles?: AiInlineFilesOptions | undefined;
  signal: AbortSignal;
  /** Progress per file, by its index in `files`. */
  onProgress?: ((index: number, fraction: number) => void) | undefined;
  messages: Pick<
    AiConversationMessages,
    'uploadFailed' | 'attachmentsUnsupported' | 'fileTooLarge'
  >;
}

/**
 * Turns attachments into file parts, in order and in parallel. Parts pass
 * through; `File`s are uploaded with `uploadFile`, or inlined when
 * `inlineFiles` allows them. Anything else fails the whole send, so no file
 * is ever dropped without the person knowing.
 */
export async function resolveAttachments(
  files: readonly (AiFilePart | File)[],
  options: ResolveAttachmentsOptions,
): Promise<AiAttachmentResult> {
  const { uploadFile, inlineFiles, signal, onProgress, messages } = options;
  const raw = files.filter((f): f is File => !isFilePart(f));
  if (raw.length && !uploadFile) {
    if (!inlineFiles)
      return {
        ok: false,
        code: 'attachments-unsupported',
        message: messages.attachmentsUnsupported,
        aborted: false,
      };
    const refused = raw.find(
      (f) => f.size > inlineFiles.maxBytes || !matchesMediaType(f.type, inlineFiles.mediaTypes),
    );
    if (refused)
      return {
        ok: false,
        code: 'attachments-unsupported',
        message: messages.fileTooLarge(refused.name),
        aborted: false,
      };
  }
  try {
    const parts = await Promise.all(
      files.map(async (f, index): Promise<AiFilePart> => {
        if (isFilePart(f)) return f;
        const progress = (fraction: number) =>
          onProgress?.(index, Math.min(1, Math.max(0, fraction)));
        progress(0);
        const part = uploadFile
          ? await uploadFile(f, { signal, onProgress: progress })
          : {
              type: 'file' as const,
              name: f.name,
              mediaType: f.type || 'application/octet-stream',
              size: f.size,
              url: await readFileAsDataUrl(f, signal),
            };
        progress(1);
        return part;
      }),
    );
    return { ok: true, parts };
  } catch {
    return {
      ok: false,
      code: 'upload-failed',
      message: messages.uploadFailed,
      aborted: signal.aborted,
    };
  }
}
