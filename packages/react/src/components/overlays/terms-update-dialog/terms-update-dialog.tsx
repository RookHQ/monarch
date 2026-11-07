'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Link } from '../../navigation/link/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  type DialogContentProps,
} from '../dialog/index.js';

/** An updated document, linked from the dialog. */
export interface TermsUpdateDocument {
  /** Link text, e.g. "Terms of Service". */
  label: string;
  /** Where the new version is published. */
  href: string;
  /** The new version, shown after the label when given. */
  version?: string;
}

export interface TermsUpdateDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'closeLabel' | 'closeOnOutsideClick'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`). Usually the dialog opens on its own. */
  trigger?: ReactElement;
  /**
   * `blocking`: the person must accept to continue. Escape, outside clicks and
   * the close button do nothing; the only other way out is `onDecline`.
   * `informational`: a notice. It can be dismissed, and "Got it" calls `onAccept`.
   */
  mode?: 'blocking' | 'informational';
  /** Default "We’ve updated our terms". Becomes the accessible name. */
  title?: ReactNode;
  /** What changed, in plain words. Becomes the accessible description. */
  summary: ReactNode;
  /** When the new terms take effect. A `YYYY-MM-DD` string is read as a calendar date. */
  effectiveDate: Date | string;
  /** Locale for the date. Default: the runtime locale. Pass it when rendering on the server. */
  locale?: string;
  /** Builds the date line from the formatted date. Default: "Effective from 1 December 2026". */
  effectiveLabel?: (date: string) => ReactNode;
  /** The updated documents, as links. */
  documents?: readonly TermsUpdateDocument[];
  /** Heading of the document list. Default "Read the full documents". */
  documentsLabel?: ReactNode;
  /** Builds a version note after a document link. Default: "(version 7)". */
  versionLabel?: (version: string) => ReactNode;
  /** Where the links open. Default: the same tab. */
  target?: string;
  /**
   * Records the acceptance. Return a promise to keep the dialog open and
   * pending until it settles; on rejection the error is shown and the person
   * can retry. The dialog closes on success.
   */
  onAccept: () => unknown;
  /** Label of the accept button. Default "Accept and continue" (blocking) or "Got it" (informational). */
  acceptLabel?: ReactNode;
  /**
   * The way out of a blocking dialog without accepting, e.g. sign out or
   * contact support. Shows a secondary button. May return a promise.
   */
  onDecline?: () => unknown;
  /** Label of the decline button, e.g. "Sign out". Default "Decline". */
  declineLabel?: ReactNode;
  /** Controlled pending state. */
  pending?: boolean;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode;
  /** Turns a rejection into a message. Default: the Error's message, or a generic line. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Accessible label of the close button (informational mode only). Default "Close". */
  closeLabel?: string | null;
  /** Dialog width. Default `md`. */
  size?: NonNullable<DialogContentProps['size']>;
}

const DEFAULT_ERROR = 'That did not work. Try again.';

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : DEFAULT_ERROR;
}

const CALENDAR_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Formats the effective date; a bare `YYYY-MM-DD` keeps its calendar day in every time zone. */
export function formatEffectiveDate(
  value: Date | string,
  locale?: string,
): { text: string; iso: string } {
  const calendar = typeof value === 'string' && CALENDAR_DATE.test(value);
  const date =
    typeof value === 'string' ? new Date(calendar ? `${value}T00:00:00Z` : value) : value;
  if (Number.isNaN(date.getTime())) return { text: String(value), iso: '' };
  const text = new Intl.DateTimeFormat(locale, {
    dateStyle: 'long',
    ...(calendar ? { timeZone: 'UTC' } : {}),
  }).format(date);
  return { text, iso: calendar ? (value as string) : date.toISOString() };
}

/**
 * Tells the person that the terms or a policy changed since they last
 * accepted, with a summary, the effective date and links to the documents.
 * `blocking` mode cannot be dismissed without accepting (except through an
 * optional decline action such as signing out); `informational` mode is a
 * dismissible notice. Accepting can be async, with pending and error states.
 */
export const TermsUpdateDialog = forwardRef<
  ElementRef<typeof DialogContent>,
  TermsUpdateDialogProps
>(function TermsUpdateDialog(
  {
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    trigger,
    mode = 'blocking',
    title = 'We’ve updated our terms',
    summary,
    effectiveDate,
    locale,
    effectiveLabel = (date) => `Effective from ${date}`,
    documents = [],
    documentsLabel = 'Read the full documents',
    versionLabel = (version) => `(version ${version})`,
    target,
    onAccept,
    acceptLabel,
    onDecline,
    declineLabel = 'Decline',
    pending: pendingProp,
    error: errorProp,
    errorMessage = defaultErrorMessage,
    closeLabel = 'Close',
    size = 'md',
    onEscapeKeyDown,
    onInteractOutside,
    className,
    ...rest
  },
  ref,
) {
  const blocking = mode === 'blocking';
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [running, setRunning] = useState<'accept' | 'decline' | null>(null);
  const [failure, setFailure] = useState<ReactNode>(null);
  const mounted = useRef(true);
  const errorId = useId();
  const docsId = useId();

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (open) setFailure(null);
  }, [open]);

  const pending = pendingProp ?? running !== null;
  const shownError = errorProp ?? failure;

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const run = async (kind: 'accept' | 'decline') => {
    if (pending) return;
    const action = kind === 'accept' ? onAccept : onDecline;
    if (!action) return;
    setFailure(null);
    try {
      const result = action();
      if (isThenable(result)) {
        setRunning(kind);
        await result;
      }
    } catch (error) {
      if (mounted.current) {
        setRunning(null);
        setFailure(errorMessage(error));
      }
      return;
    }
    if (!mounted.current) return;
    setRunning(null);
    if (kind === 'accept') setOpen(false);
  };

  const { text: dateText, iso } = formatEffectiveDate(effectiveDate, locale);

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next && (pending || blocking)) return;
        setOpen(next);
      }}
    >
      {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
      <DialogContent
        ref={ref}
        {...rest}
        size={size}
        busy={pending}
        closeLabel={blocking ? null : closeLabel}
        closeOnOutsideClick={!blocking}
        data-mode={mode}
        className={cx('mn-terms-update-dialog', className)}
        onEscapeKeyDown={(event) => {
          onEscapeKeyDown?.(event);
          if (blocking) event.preventDefault();
        }}
        onInteractOutside={(event) => {
          onInteractOutside?.(event);
          if (blocking) event.preventDefault();
        }}
      >
        <DialogHeader className="mn-terms-update-dialog__header">
          <DialogTitle>{title}</DialogTitle>
          <p className="mn-terms-update-dialog__date">
            <time dateTime={iso || undefined}>{effectiveLabel(dateText)}</time>
          </p>
        </DialogHeader>
        <DialogBody className="mn-terms-update-dialog__body">
          <DialogDescription asChild>
            <div className="mn-terms-update-dialog__summary">{summary}</div>
          </DialogDescription>
          {documents.length > 0 ? (
            <section className="mn-terms-update-dialog__documents" aria-labelledby={docsId}>
              <h3 id={docsId} className="mn-terms-update-dialog__documents-label">
                {documentsLabel}
              </h3>
              <ul className="mn-terms-update-dialog__list">
                {documents.map((doc) => (
                  <li key={doc.href}>
                    <Link href={doc.href} target={target}>
                      {doc.label}
                    </Link>
                    {doc.version !== undefined ? (
                      <span className="mn-terms-update-dialog__version">
                        {' '}
                        {versionLabel(doc.version)}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </DialogBody>
        {shownError ? (
          <div className="mn-terms-update-dialog__error">
            <Alert tone="danger" live="assertive" id={errorId}>
              {shownError}
            </Alert>
          </div>
        ) : null}
        <DialogFooter className="mn-terms-update-dialog__footer">
          {onDecline ? (
            <Button
              loading={running === 'decline'}
              disabled={pending && running !== 'decline'}
              onClick={() => void run('decline')}
            >
              {declineLabel}
            </Button>
          ) : null}
          <Button
            variant="primary"
            loading={pending && running !== 'decline'}
            disabled={running === 'decline'}
            aria-describedby={shownError ? errorId : undefined}
            onClick={() => void run('accept')}
          >
            {acceptLabel ?? (blocking ? 'Accept and continue' : 'Got it')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
});
