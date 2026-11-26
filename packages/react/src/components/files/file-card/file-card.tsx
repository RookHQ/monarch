'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { formatBytes } from '../file-upload/index.js';
import {
  FILE_TYPE_ICON_MESSAGES,
  FileTypeIcon,
  getFileTypeFamily,
  type FileTypeIconMessages,
} from '../file-type-icon/index.js';

/** Every string a file card shows or announces. */
export interface FileCardMessages {
  /** Name of the selection checkbox. */
  select: (name: string) => string;
  /** Name of the actions group. */
  actions: (name: string) => string;
  /** Family labels, shown in the meta line ("PDF document · 2 MB"). */
  types?: Partial<FileTypeIconMessages> | undefined;
}

export const FILE_CARD_MESSAGES: FileCardMessages = {
  select: (name) => `Select ${name}`,
  actions: (name) => `Actions for ${name}`,
};

/** The data of one file, shared by FileCard and FileList. */
export interface FileCardFile {
  /** File name with its extension. */
  name: string;
  /** Size in bytes. */
  size?: number | undefined;
  /** MIME type; decides the icon and type label with the name. */
  contentType?: string | undefined;
  /** Where the file opens. Only http(s), blob and relative URLs are linked. */
  href?: string | undefined;
  /** A thumbnail image (http(s), blob or relative). Falls back to the type icon if it fails to load. */
  thumbnailSrc?: string | undefined;
  /** Extra meta after the size, such as `<RelativeTime date={…} />` or an owner. */
  meta?: ReactNode;
}

export interface FileCardProps
  extends FileCardFile, Omit<HTMLAttributes<HTMLDivElement>, 'children' | 'onSelect'> {
  /** `card` (default): preview above the name, for grids. `row`: one line, for lists. */
  layout?: 'card' | 'row' | undefined;
  /**
   * Opens the file. Makes the name a button when there is no `href`; with
   * `href` it runs on click before navigation (call `preventDefault` to stop it).
   */
  onOpen?: ((event: MouseEvent<HTMLElement>) => void) | undefined;
  /** Shows a selection checkbox. Controlled with `selected` and `onSelectedChange`. */
  selectable?: boolean | undefined;
  /** Whether the file is selected. */
  selected?: boolean | undefined;
  /** Called when the checkbox is toggled. The event lets FileList read Shift for range selection. */
  onSelectedChange?: ((selected: boolean, event: { shiftKey: boolean }) => void) | undefined;
  /** Actions such as a DropdownMenu trigger or IconButtons, placed in the corner (card) or at the end (row). */
  actions?: ReactNode;
  /** Badges or status shown under the name (for example “Shared”, “Scanning”). */
  badges?: ReactNode;
  /** Greys the card and disables open and selection. */
  disabled?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<FileCardMessages> | undefined;
}

/**
 * One file as a card or a row: thumbnail or type icon, name, type and size,
 * optional selection checkbox and actions. The name is the single primary
 * control; the whole card is its click target.
 */
export const FileCard = forwardRef<HTMLDivElement, FileCardProps>(function FileCard(
  {
    name,
    size,
    contentType,
    href,
    thumbnailSrc,
    meta,
    layout = 'card',
    onOpen,
    selectable = false,
    selected = false,
    onSelectedChange,
    actions,
    badges,
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...FILE_CARD_MESSAGES, ...messagesProp };
  const metaId = useId();
  // Shift state of the click that toggled the checkbox (Radix's onCheckedChange has no event).
  const shiftRef = useRef(false);
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const family = getFileTypeFamily({ name, type: contentType });
  const typeLabel = { ...FILE_TYPE_ICON_MESSAGES, ...messages.types }[family];
  const thumb =
    thumbnailSrc && thumbnailSrc !== failedSrc && isSafeImageSrc(thumbnailSrc)
      ? thumbnailSrc
      : undefined;
  const safeHref = href && isSafeUrl(href, ['http', 'https', 'blob']) ? href : undefined;

  const title =
    safeHref && !disabled ? (
      <a className="mn-file-card__title" href={safeHref} aria-describedby={metaId} onClick={onOpen}>
        {name}
      </a>
    ) : onOpen ? (
      <button
        type="button"
        className="mn-file-card__title"
        aria-describedby={metaId}
        disabled={disabled}
        onClick={onOpen}
      >
        {name}
      </button>
    ) : (
      <span className="mn-file-card__title">{name}</span>
    );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-file-card', className)}
      data-layout={layout}
      data-family={family}
      data-selected={(selectable && selected) || undefined}
      data-disabled={disabled || undefined}
      data-interactive={(!disabled && (safeHref || onOpen) && true) || undefined}
    >
      <div className="mn-file-card__preview">
        {thumb ? (
          <img
            className="mn-file-card__thumb"
            src={thumb}
            alt=""
            loading="lazy"
            decoding="async"
            onError={() => setFailedSrc(thumb)}
          />
        ) : (
          <FileTypeIcon family={family} size={layout === 'card' ? 'lg' : 'md'} decorative />
        )}
      </div>
      <div className="mn-file-card__body">
        {title}
        <span id={metaId} className="mn-file-card__meta">
          <span>{typeLabel}</span>
          {size !== undefined ? <span>{formatBytes(size)}</span> : null}
          {meta ? <span>{meta}</span> : null}
        </span>
        {badges ? <span className="mn-file-card__badges">{badges}</span> : null}
      </div>
      {selectable ? (
        <span
          className="mn-file-card__select"
          onClickCapture={(e) => {
            shiftRef.current = e.shiftKey;
          }}
        >
          <Checkbox
            checked={selected}
            disabled={disabled}
            aria-label={messages.select(name)}
            onCheckedChange={(v) => {
              const shiftKey = shiftRef.current;
              shiftRef.current = false;
              onSelectedChange?.(v === true, { shiftKey });
            }}
          />
        </span>
      ) : null}
      {actions ? (
        <div className="mn-file-card__actions" role="group" aria-label={messages.actions(name)}>
          {actions}
        </div>
      ) : null}
    </div>
  );
});
