'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '../../data-display/card/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import type { DateInput } from '../../data-display/relative-time/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Field, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';

/**
 * Where the export is:
 * - `idle`: nothing requested yet; the request form is shown.
 * - `requested`: the request was received and is queued.
 * - `preparing`: the file is being built.
 * - `ready`: the file can be downloaded until `expiresAt`.
 * - `expired`: the download is no longer available; the person can request again.
 * - `failed`: the export could not be built; the person can request again.
 */
export type DataExportStatus = 'idle' | 'requested' | 'preparing' | 'ready' | 'expired' | 'failed';

/** One kind of data the person can choose to include. */
export interface DataExportScopeOption {
  /** Stable id passed to `onRequest`, e.g. `messages`. */
  id: string;
  /** Short name, e.g. "Messages and comments". */
  label: string;
  /** One line on what it contains. */
  description?: string;
}

/** One file format the person can choose. */
export interface DataExportFormatOption {
  /** Value passed to `onRequest`, e.g. `json`, `csv` or `zip`. */
  value: string;
  /** Visible label, e.g. "JSON (machine-readable)". */
  label: string;
}

/** What the person chose when requesting the export. */
export interface DataExportRequestDetails {
  /** Ids of the chosen scope options, in option order. Empty when there is no `scope`. */
  scope: string[];
  /** The chosen format value, when `formats` is given. */
  format?: string;
}

/** Every visible string of DataExportRequest. Pass any subset to translate or reword. */
export interface DataExportRequestMessages {
  /** Card title. */
  title: string;
  /** Sentence under the title. */
  description: string;
  /** The status badge text for each status. */
  statuses: Record<DataExportStatus, string>;
  /** Button that requests the first export. */
  request: string;
  /** Button that requests a new export after one expired or failed. */
  requestAgain: string;
  /** Download button. */
  download: string;
  /** Legend of the scope checklist. */
  scopeLegend: string;
  /** Shown when no scope option is ticked. */
  scopeRequired: string;
  /** Label of the format select. */
  formatLabel: string;
  /** Status line while requested. `date` is a `<time>` element, or null without `requestedAt`. */
  requested: (date: ReactNode | null) => ReactNode;
  /** Status line while preparing. */
  preparing: (date: ReactNode | null) => ReactNode;
  /** Accessible name of the indeterminate progress bar. */
  preparingProgress: string;
  /** Status line when ready. */
  ready: (readyAt: ReactNode | null, expiresAt: ReactNode | null) => ReactNode;
  /** Status line when expired. */
  expired: (expiresAt: ReactNode | null) => ReactNode;
  /** Status line when failed. */
  failed: string;
  /** Shown when `onRequest` rejects without an Error message. */
  requestFailed: string;
}

const DEFAULT_MESSAGES: DataExportRequestMessages = {
  title: 'Download your data',
  description: 'Get a copy of the data in your account.',
  statuses: {
    idle: 'Not requested',
    requested: 'Requested',
    preparing: 'Preparing',
    ready: 'Ready',
    expired: 'Expired',
    failed: 'Failed',
  },
  request: 'Request export',
  requestAgain: 'Request a new export',
  download: 'Download',
  scopeLegend: 'Include',
  scopeRequired: 'Choose at least one kind of data.',
  formatLabel: 'Format',
  requested: (date) =>
    date ? <>We received your request on {date}.</> : 'We received your request.',
  preparing: (date) =>
    date ? (
      <>Your export is being prepared. Requested on {date}.</>
    ) : (
      'Your export is being prepared.'
    ),
  preparingProgress: 'Preparing your export',
  ready: (readyAt, expiresAt) => (
    <>
      Your export is ready.{readyAt ? <> Prepared on {readyAt}.</> : null}
      {expiresAt ? <> You can download it until {expiresAt}.</> : null}
    </>
  ),
  expired: (expiresAt) =>
    expiresAt ? (
      <>The download expired on {expiresAt}. Request a new export to get your data.</>
    ) : (
      'The download has expired. Request a new export to get your data.'
    ),
  failed: 'Your export could not be prepared. Request it again.',
  requestFailed: 'The request did not go through. Try again.',
};

const STATUS_TONE: Record<DataExportStatus, Tone> = {
  idle: 'neutral',
  requested: 'info',
  preparing: 'info',
  ready: 'success',
  expired: 'neutral',
  failed: 'danger',
};

const DEFAULT_DATE_FORMAT: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' };

export interface DataExportRequestProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Where the export is. Default `idle`. Exposed as `data-status`. */
  status?: DataExportStatus;
  /** When the export was requested. */
  requestedAt?: DateInput;
  /** When the file became ready. */
  readyAt?: DateInput;
  /** When the download stops working. */
  expiresAt?: DateInput;
  /** Link to the file when `ready`. Rendered as a download link. */
  downloadHref?: string;
  /** Called when the download button or link is used, e.g. to fetch a signed URL. */
  onDownload?: () => void;
  /**
   * Requests an export with the chosen scope and format. Return a promise to
   * show the button as busy until it settles; a rejection shows its message.
   * The product then moves `status` on. Without it no request button is shown.
   */
  onRequest?: (details: DataExportRequestDetails) => unknown;
  /** Kinds of data the person can choose to include. All are ticked at first. */
  scope?: readonly DataExportScopeOption[];
  /** File formats to choose from. No select without it. */
  formats?: readonly DataExportFormatOption[];
  /** The format chosen at first. Default: the first of `formats`. */
  defaultFormat?: string;
  /**
   * How long preparing takes and how the person will hear about it, e.g.
   * "It can take up to 48 hours. We will email you when it is ready."
   * Shown before requesting and while requested or preparing.
   */
  preparationNote?: ReactNode;
  /** Extra detail when `failed`, e.g. the reason from the server. */
  failureReason?: ReactNode;
  /** Controlled busy state of the request button, for products that track it themselves. */
  pending?: boolean;
  /** Turns a rejected `onRequest` into a message. Default: the Error's message, or `messages.requestFailed`. */
  errorMessage?: (error: unknown) => ReactNode;
  /** BCP 47 locale for dates. Default: the runtime locale. Pass it for server rendering. */
  locale?: string;
  /** IANA time zone for dates. Default: the viewer's zone. Pass it for server rendering. */
  timeZone?: string;
  /** `Intl.DateTimeFormat` options for dates. Default `{ dateStyle: 'medium', timeStyle: 'short' }`. */
  dateFormat?: Intl.DateTimeFormatOptions;
  /** Heading element of the title. Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  messages?: Partial<DataExportRequestMessages>;
}

/**
 * A "Download your data" card for account or privacy settings. The person
 * picks what to include and the format, requests an export, follows its
 * progress in words, and downloads the file until it expires. The product
 * builds the file; the card shows each status the product reports.
 */
export const DataExportRequest = forwardRef<HTMLDivElement, DataExportRequestProps>(
  function DataExportRequest(
    {
      status = 'idle',
      requestedAt,
      readyAt,
      expiresAt,
      downloadHref,
      onDownload,
      onRequest,
      scope,
      formats,
      defaultFormat,
      preparationNote,
      failureReason,
      pending: pendingProp,
      errorMessage,
      locale,
      timeZone,
      dateFormat = DEFAULT_DATE_FORMAT,
      headingLevel = 'h3',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages: DataExportRequestMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      statuses: { ...DEFAULT_MESSAGES.statuses, ...messagesProp?.statuses },
    };
    const auto = useId();
    const titleId = `mn-data-export-request-${auto}`;
    const scopeErrorId = `${titleId}-scope-error`;
    const [chosen, setChosen] = useState<ReadonlySet<string>>(
      () => new Set(scope?.map((s) => s.id) ?? []),
    );
    const [format, setFormat] = useState(defaultFormat ?? formats?.[0]?.value ?? '');
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [scopeError, setScopeError] = useState(false);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const formatter = useMemo(
      () => new Intl.DateTimeFormat(locale, { ...dateFormat, ...(timeZone ? { timeZone } : {}) }),
      [locale, timeZone, dateFormat],
    );
    const when = (input: DateInput | undefined): ReactNode | null => {
      if (input === undefined) return null;
      const date = input instanceof Date ? input : new Date(input);
      if (Number.isNaN(date.getTime())) return null;
      return (
        <time dateTime={date.toISOString()} suppressHydrationWarning>
          {formatter.format(date)}
        </time>
      );
    };

    const pending = pendingProp ?? running;
    const canRequest =
      onRequest !== undefined && (status === 'idle' || status === 'expired' || status === 'failed');
    const showNote =
      preparationNote !== undefined &&
      (status === 'idle' || status === 'requested' || status === 'preparing');

    const submit = (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!onRequest || pending) return;
      const ids = scope ? scope.filter((s) => chosen.has(s.id)).map((s) => s.id) : [];
      if (scope && scope.length > 0 && ids.length === 0) {
        setScopeError(true);
        event.currentTarget.querySelector<HTMLElement>('[role="checkbox"]')?.focus();
        return;
      }
      setScopeError(false);
      setFailure(null);
      const toMessage = (error: unknown): ReactNode =>
        errorMessage
          ? errorMessage(error)
          : error instanceof Error && error.message
            ? error.message
            : messages.requestFailed;
      let result: unknown;
      try {
        result = onRequest({ scope: ids, ...(formats?.length ? { format } : {}) });
      } catch (error) {
        setFailure(toMessage(error));
        return;
      }
      if (!isThenable(result)) return;
      setRunning(true);
      Promise.resolve(result).then(
        () => {
          if (mounted.current) setRunning(false);
        },
        (error: unknown) => {
          if (!mounted.current) return;
          setRunning(false);
          setFailure(toMessage(error));
        },
      );
    };

    let statusLine: ReactNode = null;
    if (status === 'requested') statusLine = messages.requested(when(requestedAt));
    else if (status === 'preparing') statusLine = messages.preparing(when(requestedAt));
    else if (status === 'ready') statusLine = messages.ready(when(readyAt), when(expiresAt));
    else if (status === 'expired') statusLine = messages.expired(when(expiresAt));
    else if (status === 'failed') statusLine = messages.failed;

    const download =
      status === 'ready' && (downloadHref || onDownload) ? (
        downloadHref ? (
          <Button asChild variant="primary">
            <a href={downloadHref} download onClick={onDownload}>
              {messages.download}
            </a>
          </Button>
        ) : (
          <Button variant="primary" onClick={onDownload}>
            {messages.download}
          </Button>
        )
      ) : null;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-data-export-request', className)}
        data-status={status}
        data-pending={pending || undefined}
      >
        <Card aria-labelledby={titleId} role="group">
          <CardHeader className="mn-data-export-request__header">
            <div className="mn-data-export-request__heading">
              <CardTitle as={headingLevel} id={titleId}>
                {messages.title}
              </CardTitle>
              <Badge tone={STATUS_TONE[status]} dot className="mn-data-export-request__badge">
                {messages.statuses[status]}
              </Badge>
            </div>
            <CardDescription>{messages.description}</CardDescription>
          </CardHeader>
          <CardContent className="mn-data-export-request__content">
            <div className="mn-data-export-request__status" aria-live="polite">
              {statusLine ? (
                <p className="mn-data-export-request__status-text">{statusLine}</p>
              ) : null}
              {status === 'failed' && failureReason ? (
                <p className="mn-data-export-request__reason">{failureReason}</p>
              ) : null}
            </div>
            {status === 'preparing' ? (
              <Progress
                label={messages.preparingProgress}
                size="sm"
                className="mn-data-export-request__progress"
              />
            ) : null}
            {showNote ? <p className="mn-data-export-request__note">{preparationNote}</p> : null}
            {canRequest ? (
              <form
                className="mn-data-export-request__form"
                onSubmit={submit}
                aria-labelledby={titleId}
                aria-busy={pending || undefined}
                noValidate
              >
                {scope && scope.length > 0 ? (
                  <fieldset
                    className="mn-data-export-request__scope"
                    aria-describedby={scopeError ? scopeErrorId : undefined}
                    aria-invalid={scopeError}
                  >
                    <legend className="mn-data-export-request__legend">
                      {messages.scopeLegend}
                    </legend>
                    {scope.map((option) => (
                      <Checkbox
                        key={option.id}
                        name="scope"
                        value={option.id}
                        label={option.label}
                        {...(option.description ? { description: option.description } : {})}
                        checked={chosen.has(option.id)}
                        invalid={scopeError}
                        onCheckedChange={(checked) => {
                          setChosen((prev) => {
                            const next = new Set(prev);
                            if (checked === true) next.add(option.id);
                            else next.delete(option.id);
                            return next;
                          });
                          if (checked === true) setScopeError(false);
                        }}
                      />
                    ))}
                    {scopeError ? (
                      <p id={scopeErrorId} className="mn-data-export-request__error">
                        {messages.scopeRequired}
                      </p>
                    ) : null}
                  </fieldset>
                ) : null}
                {formats && formats.length > 0 ? (
                  <Field className="mn-data-export-request__format">
                    <FieldLabel>{messages.formatLabel}</FieldLabel>
                    <NativeSelect
                      name="format"
                      value={format}
                      onChange={(e) => setFormat(e.target.value)}
                      options={formats}
                    />
                  </Field>
                ) : null}
                {failure ? (
                  <Alert tone="danger" live="assertive" className="mn-data-export-request__failure">
                    {failure}
                  </Alert>
                ) : null}
                <div className="mn-data-export-request__actions">
                  <Button type="submit" variant="primary" loading={pending}>
                    {status === 'idle' ? messages.request : messages.requestAgain}
                  </Button>
                </div>
              </form>
            ) : null}
          </CardContent>
          {download ? (
            <CardFooter className="mn-data-export-request__footer">{download}</CardFooter>
          ) : null}
        </Card>
      </div>
    );
  },
);
