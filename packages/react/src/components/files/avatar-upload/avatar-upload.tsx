'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { validateFiles, type FileUploadMessages } from '../file-upload/index.js';

/** Every string an avatar upload shows or announces. */
export interface AvatarUploadMessages {
  /** Button that opens the picker when there is no picture. */
  upload: string;
  /** Button that opens the picker when there is a picture. */
  change: string;
  /** Button that removes the picture. */
  remove: string;
  /** Accessible name of the crop area. */
  cropArea: string;
  /** Instructions for the crop area, read as its description. */
  cropHint: string;
  /** Label of the zoom slider. */
  zoom: string;
  /** Confirms the crop. */
  save: string;
  /** Leaves the crop without saving. */
  cancel: string;
  /** Shown when saving throws or rejects. */
  saveFailed: string;
  /** Validation messages, shared with FileUpload. */
  validation?: Partial<FileUploadMessages> | undefined;
}

export const AVATAR_UPLOAD_MESSAGES: AvatarUploadMessages = {
  upload: 'Upload photo',
  change: 'Change photo',
  remove: 'Remove photo',
  cropArea: 'Photo position',
  cropHint: 'Drag, or use the arrow keys, to move the photo. Use plus and minus to zoom.',
  zoom: 'Zoom',
  save: 'Save photo',
  cancel: 'Cancel',
  saveFailed: 'The photo could not be saved. Try again.',
};

/** The crop applied to the picked image, in the image's own pixels. */
export interface AvatarCrop {
  x: number;
  y: number;
  size: number;
}

/** Zoom limits of the crop editor. */
export const AVATAR_UPLOAD_MIN_ZOOM = 1;
export const AVATAR_UPLOAD_MAX_ZOOM = 4;

/** Position of the picture in the crop area, in viewport units (the viewport is 1 wide). */
interface View {
  zoom: number;
  /** Offset of the image centre from the viewport centre. */
  x: number;
  y: number;
}

/**
 * The square of the image (in its own pixels) that a view shows. The image
 * covers the viewport at zoom 1 (its short side fills it).
 */
export function avatarCropRect(
  natural: { width: number; height: number },
  view: { zoom: number; x: number; y: number },
): AvatarCrop {
  const short = Math.min(natural.width, natural.height);
  const size = short / view.zoom;
  // One viewport unit is `size` image pixels.
  return {
    x: natural.width / 2 - view.x * size - size / 2,
    y: natural.height / 2 - view.y * size - size / 2,
    size,
  };
}

function clampView(natural: { width: number; height: number } | null, v: View): View {
  const zoom = Math.min(AVATAR_UPLOAD_MAX_ZOOM, Math.max(AVATAR_UPLOAD_MIN_ZOOM, v.zoom));
  if (!natural) return { zoom, x: 0, y: 0 };
  const short = Math.min(natural.width, natural.height);
  const w = (natural.width / short) * zoom;
  const h = (natural.height / short) * zoom;
  const maxX = (w - 1) / 2;
  const maxY = (h - 1) / 2;
  return {
    zoom,
    x: Math.min(maxX, Math.max(-maxX, v.x)),
    y: Math.min(maxY, Math.max(-maxY, v.y)),
  };
}

export interface AvatarUploadProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The person's or entity's name, for the initials fallback and the picture's alt text. */
  name: string;
  /** The current picture. */
  src?: string | null | undefined;
  /** `circle` (default) for people, `square` for workspaces and organisations. Also the crop mask. */
  shape?: 'circle' | 'square' | undefined;
  /**
   * Receives the cropped square picture and the original file. Return a
   * promise to keep the editor open with a busy Save button until it settles;
   * a rejection shows `messages.saveFailed`.
   */
  onChange?: ((blob: Blob, file: File, crop: AvatarCrop) => void | Promise<void>) | undefined;
  /** Removes the picture. Without it there is no remove button. */
  onRemove?: (() => void) | undefined;
  /** Allowed types. Default: PNG, JPEG, WebP and GIF. */
  accept?: string | undefined;
  /** Largest file accepted, in bytes. */
  maxSize?: number | undefined;
  /** Width and height of the output picture in pixels. Default 512. */
  outputSize?: number | undefined;
  /** Output format. Default `image/png`. */
  outputType?: 'image/png' | 'image/jpeg' | 'image/webp' | undefined;
  /** Quality 0–1 for JPEG and WebP. Default 0.9. */
  outputQuality?: number | undefined;
  /** Disables every control. */
  disabled?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<AvatarUploadMessages> | undefined;
}

const STEP = 0.04;

/**
 * Picks a profile or workspace picture and crops it to a circle (or square)
 * before handing it over: drag or use the arrow keys to position, a slider or
 * plus and minus to zoom. Produces a square image with a canvas; no crop
 * library.
 */
export const AvatarUpload = forwardRef<HTMLDivElement, AvatarUploadProps>(function AvatarUpload(
  {
    name,
    src,
    shape = 'circle',
    onChange,
    onRemove,
    accept = 'image/png,image/jpeg,image/webp,image/gif',
    maxSize,
    outputSize = 512,
    outputType = 'image/png',
    outputQuality = 0.9,
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AVATAR_UPLOAD_MESSAGES, ...messagesProp };
  const id = useId();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const imgRef = useRef<HTMLImageElement | null>(null);
  const changeRef = useRef<HTMLButtonElement | null>(null);
  const drag = useRef<{ id: number; x: number; y: number } | null>(null);
  const [picked, setPicked] = useState<{ file: File; url: string } | null>(null);
  const [natural, setNatural] = useState<{ width: number; height: number } | null>(null);
  const [view, setView] = useState<View>({ zoom: 1, x: 0, y: 0 });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  // Set synchronously so a second Enter or click cannot start a second save
  // before the first one has re-rendered the busy state.
  const savingRef = useRef(false);
  const returnFocus = useRef(false);

  // Revoke the object URL when it is replaced or the component unmounts.
  useEffect(() => {
    if (!picked) return;
    return () => URL.revokeObjectURL(picked.url);
  }, [picked]);

  // After the editor closes, return focus to the change button.
  useEffect(() => {
    if (!picked && returnFocus.current) {
      returnFocus.current = false;
      changeRef.current?.focus();
    }
  }, [picked]);

  // Move focus into the editor when it opens (after the file dialog has returned focus).
  useEffect(() => {
    if (!picked) return;
    const t = setTimeout(() => viewportRef.current?.focus(), 0);
    return () => clearTimeout(t);
  }, [picked]);

  // `next` reads the latest view, so moves that arrive between two renders
  // (pointer moves are batched) add up instead of overwriting each other.
  const update = (next: Partial<View> | ((v: View) => Partial<View>)) =>
    setView((v) => clampView(natural, { ...v, ...(typeof next === 'function' ? next(v) : next) }));

  const pick = (files: File[]) => {
    if (!files.length) return;
    const { accepted, rejected } = validateFiles(files.slice(0, 1), {
      accept,
      maxSize,
      ...(m.validation ? { messages: m.validation } : {}),
    });
    const file = accepted[0];
    if (!file) {
      setError(rejected[0]?.message ?? '');
      return;
    }
    setError('');
    setNatural(null);
    setView({ zoom: 1, x: 0, y: 0 });
    setPicked({ file, url: URL.createObjectURL(file) });
  };

  const close = () => {
    returnFocus.current = true;
    savingRef.current = false;
    setPicked(null);
    setSaving(false);
  };

  const save = async () => {
    if (!picked || !natural || disabled || savingRef.current) return;
    const crop = avatarCropRect(natural, view);
    savingRef.current = true;
    setSaving(true);
    setError('');
    try {
      const blob = await renderCrop(imgRef.current, crop, outputSize, outputType, outputQuality);
      await onChange?.(blob ?? picked.file, picked.file, crop);
      close();
    } catch {
      savingRef.current = false;
      setSaving(false);
      setError(m.saveFailed);
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    // While saving (or disabled) the crop is fixed: the image being stored must not move,
    // Enter must not save twice and Escape must not drop an upload that is under way.
    if (savingRef.current || disabled) return;
    const step = e.shiftKey ? STEP * 4 : STEP;
    const moves: Record<string, (v: View) => Partial<View>> = {
      ArrowLeft: (v) => ({ x: v.x - step }),
      ArrowRight: (v) => ({ x: v.x + step }),
      ArrowUp: (v) => ({ y: v.y - step }),
      ArrowDown: (v) => ({ y: v.y + step }),
      '+': (v) => ({ zoom: v.zoom + 0.1 }),
      '=': (v) => ({ zoom: v.zoom + 0.1 }),
      '-': (v) => ({ zoom: v.zoom - 0.1 }),
      Home: () => ({ x: 0, y: 0, zoom: 1 }),
    };
    const next = moves[e.key];
    if (next) {
      e.preventDefault();
      update(next);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      void save();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      close();
    }
  };

  const onPointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (e.button > 0 || savingRef.current || disabled) return;
    drag.current = { id: e.pointerId, x: e.clientX, y: e.clientY };
    e.currentTarget.setPointerCapture?.(e.pointerId);
  };
  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId) return;
    const width = e.currentTarget.clientWidth || 1;
    const dx = (e.clientX - d.x) / width;
    const dy = (e.clientY - d.y) / width;
    drag.current = { id: d.id, x: e.clientX, y: e.clientY };
    update((v) => ({ x: v.x + dx, y: v.y + dy }));
  };
  const endDrag = () => {
    drag.current = null;
  };

  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;
  const zoomId = `${id}-zoom`;

  // Image box in viewport percentages.
  const short = natural ? Math.min(natural.width, natural.height) : 1;
  const w = natural ? (natural.width / short) * view.zoom : view.zoom;
  const h = natural ? (natural.height / short) * view.zoom : view.zoom;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-avatar-upload', className)}
      data-shape={shape}
      data-editing={picked ? '' : undefined}
    >
      {picked ? (
        <div className="mn-avatar-upload__editor">
          {/* A focusable 2-D position control: arrow keys pan, plus and minus zoom (described by the hint). */}
          {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
          <div
            ref={viewportRef}
            className="mn-avatar-upload__viewport"
            role="group"
            aria-roledescription="crop area"
            aria-label={m.cropArea}
            aria-describedby={hintId}
            // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
            tabIndex={0}
            onKeyDown={onKeyDown}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={endDrag}
            onPointerCancel={endDrag}
          >
            <img
              ref={imgRef}
              className="mn-avatar-upload__image"
              src={picked.url}
              alt=""
              draggable={false}
              onLoad={(e) => {
                const img = e.currentTarget;
                setNatural({ width: img.naturalWidth || 1, height: img.naturalHeight || 1 });
              }}
              style={{
                width: `${w * 100}%`,
                height: `${h * 100}%`,
                left: `${(0.5 + view.x - w / 2) * 100}%`,
                top: `${(0.5 + view.y - h / 2) * 100}%`,
              }}
            />
            <span className="mn-avatar-upload__mask" aria-hidden />
          </div>
          <p id={hintId} className="mn-avatar-upload__hint">
            {m.cropHint}
          </p>
          <div className="mn-avatar-upload__zoom">
            <label htmlFor={zoomId}>{m.zoom}</label>
            <input
              className="mn-avatar-upload__zoom-input"
              id={zoomId}
              type="range"
              min={AVATAR_UPLOAD_MIN_ZOOM}
              max={AVATAR_UPLOAD_MAX_ZOOM}
              step={0.01}
              value={view.zoom}
              aria-valuetext={`${Math.round(view.zoom * 100)}%`}
              disabled={disabled || saving}
              onChange={(e) => update({ zoom: Number(e.currentTarget.value) })}
            />
          </div>
          <div className="mn-avatar-upload__actions">
            <Button
              type="button"
              variant="primary"
              size="sm"
              loading={saving}
              disabled={disabled || !natural}
              onClick={() => void save()}
            >
              {m.save}
            </Button>
            <Button type="button" variant="ghost" size="sm" disabled={saving} onClick={close}>
              {m.cancel}
            </Button>
          </div>
        </div>
      ) : (
        <div className="mn-avatar-upload__current">
          <Avatar name={name} src={src ?? null} size="xl" shape={shape} />
          <div className="mn-avatar-upload__buttons">
            <Button
              ref={changeRef}
              type="button"
              size="sm"
              disabled={disabled}
              aria-describedby={error ? errorId : undefined}
              onClick={() => inputRef.current?.click()}
            >
              {src ? m.change : m.upload}
            </Button>
            {src && onRemove ? (
              <Button
                type="button"
                size="sm"
                variant="ghost"
                disabled={disabled}
                onClick={onRemove}
              >
                {m.remove}
              </Button>
            ) : null}
          </div>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        className="mn-visually-hidden"
        tabIndex={-1}
        aria-hidden
        accept={accept}
        disabled={disabled}
        onChange={(e) => {
          const files = Array.from(e.currentTarget.files ?? []);
          e.currentTarget.value = '';
          pick(files);
        }}
      />
      <p id={errorId} className="mn-avatar-upload__error" role="alert">
        {error}
      </p>
    </div>
  );
});

/** Draws the crop onto a square canvas. Resolves null where canvas is unavailable. */
function renderCrop(
  img: HTMLImageElement | null,
  crop: AvatarCrop,
  size: number,
  type: string,
  quality: number,
): Promise<Blob | null> {
  if (!img || typeof document === 'undefined') return Promise.resolve(null);
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  let ctx: CanvasRenderingContext2D | null = null;
  try {
    ctx = canvas.getContext('2d');
  } catch {
    ctx = null;
  }
  if (!ctx || typeof canvas.toBlob !== 'function') return Promise.resolve(null);
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(img, crop.x, crop.y, crop.size, crop.size, 0, 0, size, size);
  return new Promise((resolve) => canvas.toBlob((b) => resolve(b), type, quality));
}
