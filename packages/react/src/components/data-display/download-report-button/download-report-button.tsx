'use client';
import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ArrowDownGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { Button, type ButtonVariant } from '../../actions/button/index.js';
import { Progress } from '../progress-bar/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** A file format the report can be downloaded in. */
export interface DownloadFormat {
  /** Passed to `onDownload`, e.g. `csv`, `xlsx`, `pdf`, `json`. */
  id: string;
  /** Short name shown in the button or menu, e.g. "CSV". */
  label: string;
  /** A hint in the menu, e.g. "Opens in any spreadsheet". */
  description?: string | undefined;
}

/**
 * What `onDownload` resolves to. Nothing (or `downloaded`) means the file was
 * saved. `queued` means a large file is being prepared in the background.
 */
export type DownloadResult = { status: 'downloaded' } | { status: 'queued'; message?: ReactNode };

/** The words DownloadReportButton shows and speaks. Override them to translate. */
export interface DownloadReportButtonMessages {
  /** Button text with several formats. */
  label: string;
  /** Button text with one format. Receives the format label. */
  single: (format: string) => string;
  /** Announced and shown while a file is prepared. Receives the format label. */
  preparing: (format: string) => string;
  /** Announced when the file is saved. Receives the format label. */
  downloaded: (format: string) => string;
  /** Shown when the file is prepared in the background. */
  queued: string;
  /** Default failure text. Receives the format label. */
  error: (format: string) => string;
  /** Accessible name of the progress bar. */
  progress: string;
}

export const DOWNLOAD_REPORT_BUTTON_MESSAGES: DownloadReportButtonMessages = {
  label: 'Download',
  single: (format) => `Download ${format}`,
  preparing: (format) => `Preparing ${format}…`,
  downloaded: (format) => `${format} downloaded`,
  queued:
    'The file is being prepared. You can leave this page; it will be ready to download shortly.',
  error: (format) => `The ${format} download failed.`,
  progress: 'Download progress',
};

export interface DownloadReportButtonProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onError'
> {
  /** The formats offered. One shows a plain button; several open a menu. Default: CSV only. */
  formats?: readonly DownloadFormat[] | undefined;
  /**
   * Fetches and saves the file in the chosen format. Return a promise: the
   * button stays busy until it settles, a rejection shows the error with Try
   * again, and resolving to a `DownloadResult` of `{ status: 'queued' }` says
   * the file is being prepared in the background. Resolving to nothing means
   * the file was saved.
   */
  onDownload: (format: string) => unknown;
  /** How far the download is, 0 to 1, while it runs. Without it the button only shows a spinner. */
  progress?: number | null | undefined;
  /** Turns a rejection into the text shown. Default: `messages.error(format)`. */
  errorMessage?: ((error: unknown, format: DownloadFormat) => ReactNode) | undefined;
  /** Button style. Default `secondary`. */
  variant?: ButtonVariant | undefined;
  /** Button size. Default `sm`. */
  size?: 'sm' | 'md' | undefined;
  /** Disables the button, e.g. while the report has no rows. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<DownloadReportButtonMessages> | undefined;
}

const DEFAULT_FORMATS: readonly DownloadFormat[] = [{ id: 'csv', label: 'CSV' }];

type Status =
  | { kind: 'idle' }
  | { kind: 'pending'; format: DownloadFormat }
  | { kind: 'done'; format: DownloadFormat }
  | { kind: 'queued'; format: DownloadFormat; message: ReactNode }
  | { kind: 'error'; format: DownloadFormat; message: ReactNode };

/**
 * Downloads a report: one button for a single format, or a menu of formats
 * (CSV, Excel, PDF…). Shows progress while the file is prepared, says when it
 * is saved or queued, and offers Try again when it fails.
 */
export const DownloadReportButton = forwardRef<HTMLDivElement, DownloadReportButtonProps>(
  function DownloadReportButton(
    {
      formats = DEFAULT_FORMATS,
      onDownload,
      progress,
      errorMessage,
      variant = 'secondary',
      size = 'sm',
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DOWNLOAD_REPORT_BUTTON_MESSAGES, ...messagesProp };
    const [status, setStatus] = useState<Status>({ kind: 'idle' });
    const mounted = useRef(true);
    const triggerRef = useRef<HTMLButtonElement | null>(null);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const pending = status.kind === 'pending';
    const single = formats.length === 1 ? formats[0] : undefined;

    const run = async (format: DownloadFormat) => {
      setStatus({ kind: 'pending', format });
      try {
        const result = (await onDownload(format.id)) as DownloadResult | undefined;
        if (!mounted.current) return;
        if (result && typeof result === 'object' && result.status === 'queued') {
          setStatus({ kind: 'queued', format, message: result.message ?? messages.queued });
        } else {
          setStatus({ kind: 'done', format });
        }
      } catch (error) {
        if (!mounted.current) return;
        setStatus({
          kind: 'error',
          format,
          message: errorMessage ? errorMessage(error, format) : messages.error(format.label),
        });
      }
    };

    const announcement =
      status.kind === 'pending'
        ? messages.preparing(status.format.label)
        : status.kind === 'done'
          ? messages.downloaded(status.format.label)
          : '';

    const buttonProps = {
      variant,
      size,
      loading: pending,
      // A loading button stays focusable; `loading` blocks a second click.
      disabled,
      leadingIcon: <ArrowDownGlyph />,
    } as const;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-download-report-button', className)}
        data-status={status.kind === 'idle' ? undefined : status.kind}
      >
        {single ? (
          <Button
            {...buttonProps}
            ref={triggerRef}
            className="mn-download-report-button__button"
            onClick={() => void run(single)}
          >
            {messages.single(single.label)}
          </Button>
        ) : (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                {...buttonProps}
                ref={triggerRef}
                className="mn-download-report-button__button"
                trailingIcon={<ChevronDownGlyph />}
              >
                {messages.label}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="mn-download-report-button__menu">
              {formats.map((format) => (
                <DropdownMenuItem key={format.id} onSelect={() => void run(format)}>
                  <span className="mn-download-report-button__format">
                    <span className="mn-download-report-button__format-label">{format.label}</span>
                    {format.description ? (
                      <span className="mn-download-report-button__format-description">
                        {format.description}
                      </span>
                    ) : null}
                  </span>
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
        {pending && typeof progress === 'number' ? (
          <Progress
            className="mn-download-report-button__progress"
            size="sm"
            label={messages.progress}
            value={Math.round(Math.max(0, Math.min(1, progress)) * 100)}
            max={100}
          />
        ) : null}
        {status.kind === 'queued' ? (
          <p className="mn-download-report-button__note">{status.message}</p>
        ) : null}
        {status.kind === 'error' ? (
          <InlineErrorRetry
            className="mn-download-report-button__error"
            onRetry={() => run(status.format)}
            returnFocusTo={() => triggerRef.current}
          >
            {status.message}
          </InlineErrorRetry>
        ) : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {status.kind === 'queued' ? status.message : announcement}
        </span>
      </div>
    );
  },
);
