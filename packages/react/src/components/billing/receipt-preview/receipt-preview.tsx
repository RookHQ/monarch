'use client';

import { forwardRef, useCallback, useEffect, useRef, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import { ArrowDownGlyph } from '../../../internal/icons.js';
import { Button, type ButtonProps } from '../../actions/button/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../../overlays/dialog/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

export interface ReceiptPreviewMessages {
  /** Trigger text. */
  view: string;
  /** Added to the trigger's accessible name when a PDF opens in a new tab. */
  opensInNewTab: string;
  /** Dialog title. */
  title: string;
  /** Alternative text of the image. */
  imageAlt: string;
  loading: string;
  /** Shown when `load` fails. */
  loadError: string;
  retry: string;
  download: string;
  /** Link to open a PDF in its own tab from the dialog. */
  openInNewTab: string;
  /** HEIC/HEIF image the browser cannot draw. */
  heicUnsupported: string;
  /** Any other image the browser could not draw. */
  imageUnsupported: string;
  /** A file type with no preview (not an image or PDF). */
  typeUnsupported: string;
  /** Shown instead of the trigger when there is no receipt. */
  none: string;
}

const DEFAULT_MESSAGES: ReceiptPreviewMessages = {
  view: 'View receipt',
  opensInNewTab: '(opens in a new tab)',
  title: 'Receipt',
  imageAlt: 'Receipt',
  loading: 'Loading receipt',
  loadError: 'The receipt could not be loaded.',
  retry: 'Try again',
  download: 'Download',
  openInNewTab: 'Open in a new tab',
  heicUnsupported:
    'This is a HEIC photo, which this browser cannot show. Download it to open it on your device.',
  imageUnsupported: 'This browser could not show the image. Download it to open it on your device.',
  typeUnsupported: 'There is no preview for this file type. Download it to open it.',
  none: 'No receipt',
};

type Status = 'idle' | 'loading' | 'ready' | 'error';

const isPdfType = (mime: string) => mime === 'application/pdf';
const isImageType = (mime: string) => mime.startsWith('image/');
const isHeicType = (mime: string) => /^image\/hei[cf]$/.test(mime);

function extensionFor(mime: string): string {
  const sub = mime.split('/')[1] ?? 'bin';
  return sub === 'jpeg' ? 'jpg' : (sub.split('+')[0] ?? 'bin');
}

function PaperclipGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M13 7.5 8.2 12.3a3 3 0 0 1-4.3-4.2l5-5a2 2 0 0 1 2.9 2.8l-5 5a1 1 0 0 1-1.4-1.4L10 4.9" />
    </svg>
  );
}

export interface ReceiptPreviewProps extends Omit<ButtonProps, 'children' | 'onError' | 'asChild'> {
  /** MIME type of the file, e.g. `image/jpeg`, `application/pdf`. Decides how it is shown. */
  mimeType: string;
  /**
   * A URL the browser may load directly (a signed link or an object URL you
   * manage). Use `load` instead when the file must be fetched with
   * authorisation only when someone opens it.
   */
  src?: string | null | undefined;
  /**
   * Fetches the file when the preview opens. Return a Blob (an object URL is
   * created and revoked for you) or a URL. Called again on Try again.
   */
  load?: (() => Promise<Blob | string>) | undefined;
  /** File name offered by Download. Default `receipt.<extension>`. */
  fileName?: string | undefined;
  /**
   * How a PDF opens: in a new browser tab (default, best on phones) or inside
   * the dialog with an embedded viewer.
   */
  pdf?: 'tab' | 'dialog' | undefined;
  /** Controlled open state of the dialog. */
  open?: boolean | undefined;
  /** Called when the dialog opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Called when `load` fails, for logging. The dialog shows the error either way. */
  onLoadError?: ((error: unknown) => void) | undefined;
  /** Replace any English text. */
  messages?: Partial<ReceiptPreviewMessages> | undefined;
}

/**
 * A "View receipt" button that previews an uploaded receipt: images in a
 * dialog with Download, PDFs in a new tab or an embedded viewer, and a clear
 * message for formats the browser cannot show (such as HEIC photos). It can
 * load the file lazily through a callback, so private files are only fetched
 * when someone asks to see them.
 */
export const ReceiptPreview = forwardRef<HTMLButtonElement, ReceiptPreviewProps>(
  function ReceiptPreview(
    {
      mimeType,
      src,
      load,
      fileName,
      pdf = 'tab',
      open: openProp,
      onOpenChange,
      onLoadError,
      messages: messagesProp,
      variant = 'ghost',
      size = 'sm',
      className,
      onClick,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [openState, setOpenState] = useState(false);
    const open = openProp ?? openState;
    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );
    const [status, setStatus] = useState<Status>('idle');
    const [url, setUrl] = useState<string | null>(null);
    const [broken, setBroken] = useState(false);
    const owned = useRef<string | null>(null);
    const trigger = useRef<HTMLButtonElement | null>(null);
    const setTriggerRef = useCallback(
      (node: HTMLButtonElement | null) => {
        trigger.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      },
      [ref],
    );
    const request = useRef(0);
    const mounted = useRef(true);

    const isPdf = isPdfType(mimeType);
    const available = Boolean(src) || Boolean(load);

    const release = useCallback(() => {
      if (owned.current) URL.revokeObjectURL(owned.current);
      owned.current = null;
    }, []);

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
        release();
      };
    }, [release]);

    /** Resolves the file to a URL. `keep` hands an owned object URL to the caller (a new tab). */
    const resolve = useCallback(
      async (keep = false): Promise<string | null> => {
        if (src) return src;
        if (!load) return null;
        const id = ++request.current;
        setStatus('loading');
        try {
          const result = await load();
          const next = typeof result === 'string' ? result : URL.createObjectURL(result);
          if (!mounted.current || id !== request.current) {
            if (typeof result !== 'string' && !keep) URL.revokeObjectURL(next);
            return keep ? next : null;
          }
          if (typeof result !== 'string' && !keep) {
            release();
            owned.current = next;
          }
          setBroken(false);
          setUrl(next);
          setStatus('ready');
          return next;
        } catch (error) {
          if (mounted.current && id === request.current) setStatus('error');
          onLoadError?.(error);
          return null;
        }
      },
      [src, load, release, onLoadError],
    );

    // Load when the dialog opens; drop the object URL when it closes.
    useEffect(() => {
      if (!open) {
        request.current += 1;
        release();
        setUrl(null);
        setStatus('idle');
        return;
      }
      if (src) {
        setUrl(src);
        setStatus('ready');
      } else if (status === 'idle') {
        void resolve();
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, src]);

    const openPdfTab = async () => {
      if (src) {
        window.open(src, '_blank', 'noopener');
        return;
      }
      // Open the tab synchronously so popup blockers allow it, then point it at the file.
      const tab = window.open('', '_blank');
      const next = await resolve(true);
      if (next && tab) {
        tab.opener = null;
        tab.location.href = next;
        if (next.startsWith('blob:')) setTimeout(() => URL.revokeObjectURL(next), 120_000);
        setStatus('idle');
      } else {
        tab?.close();
        if (next) window.location.assign(next);
        else setOpen(true);
      }
    };

    if (!available) {
      return <span className={cx('mn-receipt-preview__none', className)}>{m.none}</span>;
    }

    const pdfInTab = isPdf && pdf === 'tab';
    const download = fileName ?? `receipt.${extensionFor(mimeType)}`;

    let body;
    if (status === 'error') {
      body = (
        <InlineErrorRetry onRetry={() => resolve()} retryLabel={m.retry}>
          {m.loadError}
        </InlineErrorRetry>
      );
    } else if (status !== 'ready' || !url) {
      body = (
        <div className="mn-receipt-preview__loading">
          <Spinner label={m.loading} />
        </div>
      );
    } else if (isImageType(mimeType) && !broken) {
      body = (
        <img
          className="mn-receipt-preview__image"
          src={url}
          alt={m.imageAlt}
          onError={() => setBroken(true)}
        />
      );
    } else if (isPdf) {
      body = (
        <object
          className="mn-receipt-preview__pdf"
          data={url}
          type="application/pdf"
          aria-label={m.title}
        >
          <p className="mn-receipt-preview__message">
            <a href={url} target="_blank" rel="noopener noreferrer">
              {m.openInNewTab}
            </a>
          </p>
        </object>
      );
    } else {
      body = (
        <p className="mn-receipt-preview__message" data-unsupported="">
          {isImageType(mimeType)
            ? isHeicType(mimeType)
              ? m.heicUnsupported
              : m.imageUnsupported
            : m.typeUnsupported}
        </p>
      );
    }

    return (
      <>
        <Button
          ref={setTriggerRef}
          {...rest}
          variant={variant}
          size={size}
          leadingIcon={<PaperclipGlyph />}
          className={cx('mn-receipt-preview', className)}
          aria-label={pdfInTab ? `${m.view} ${m.opensInNewTab}` : undefined}
          aria-haspopup={pdfInTab ? undefined : 'dialog'}
          data-loading-receipt={status === 'loading' || undefined}
          onClick={(event) => {
            onClick?.(event);
            if (event.defaultPrevented) return;
            if (pdfInTab) void openPdfTab();
            else setOpen(true);
          }}
        >
          {m.view}
        </Button>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogContent
            size="lg"
            className="mn-receipt-preview__dialog"
            aria-describedby={undefined}
            onCloseAutoFocus={(event) => {
              // The dialog has no Radix trigger, so return focus to the button explicitly.
              event.preventDefault();
              trigger.current?.focus();
            }}
            footer={
              status === 'ready' && url ? (
                <Button asChild size="sm" leadingIcon={<ArrowDownGlyph />}>
                  <a href={url} download={download}>
                    {m.download}
                  </a>
                </Button>
              ) : undefined
            }
          >
            <DialogHeader>
              <DialogTitle>{m.title}</DialogTitle>
            </DialogHeader>
            <DialogBody className="mn-receipt-preview__body" data-status={status}>
              {body}
            </DialogBody>
          </DialogContent>
        </Dialog>
      </>
    );
  },
);
