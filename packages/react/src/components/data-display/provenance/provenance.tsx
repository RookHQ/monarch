import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { RelativeTime, type DateInput } from '../relative-time/index.js';

export interface ProvenanceMessages {
  /** Visually hidden prefix before the source, e.g. "Source:". */
  source: string;
  /** Visually hidden words between the source and the time, e.g. "checked". */
  checked: string;
}

export const defaultProvenanceMessages: ProvenanceMessages = {
  source: 'Source:',
  checked: 'checked',
};

export interface ProvenanceProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * Where the value came from, in words the reader understands: "Registry",
   * "Imported file", "Entered by hand". Nothing renders when it is empty.
   */
  source: ReactNode;
  /** When the value was last fetched, checked or computed. Invalid or missing dates are left out. */
  checkedAt?: DateInput | null | undefined;
  /** An icon for the source (decorative). */
  icon?: ReactNode;
  /** Link to the source record or system. Turns the source label into a link. Unsafe URLs (`javascript:` and so on) are ignored. */
  href?: string | undefined;
  /** `chip` (default) is a small bordered tag; `inline` is plain muted text for use inside sentences. */
  variant?: 'chip' | 'inline';
  /** Reference time for the relative text (pass the server's time for identical server rendering). */
  now?: DateInput | undefined;
  /** Locale for the relative and absolute time. */
  locale?: string | undefined;
  /** Show the absolute time on hover and focus (makes the time a tab stop). Default true. */
  timeTooltip?: boolean;
  messages?: Partial<ProvenanceMessages>;
}

function validDate(input: DateInput | null | undefined): input is DateInput {
  if (input === null || input === undefined || input === '') return false;
  const d = input instanceof Date ? input : new Date(input);
  return !Number.isNaN(d.getTime());
}

/**
 * Where a value came from and how fresh it is: "Registry · 2 hours ago".
 * Put it next to any value the product did not get from the user.
 */
export const Provenance = forwardRef<HTMLSpanElement, ProvenanceProps>(function Provenance(
  {
    source,
    checkedAt,
    icon,
    href,
    variant = 'chip',
    now,
    locale,
    timeTooltip = true,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultProvenanceMessages, ...messages };
  if (source === null || source === undefined || source === false || source === '') return null;
  const dated = validDate(checkedAt);
  const safeHref = href && isSafeUrl(href) ? href : undefined;
  return (
    <span ref={ref} {...rest} data-variant={variant} className={cx('mn-provenance', className)}>
      {icon ? (
        <span className="mn-provenance__icon" aria-hidden="true">
          {icon}
        </span>
      ) : null}
      <span className="mn-provenance__text">
        <span className="mn-visually-hidden">{m.source} </span>
        {safeHref ? (
          <a className="mn-provenance__source" href={safeHref}>
            {source}
          </a>
        ) : (
          <span className="mn-provenance__source">{source}</span>
        )}
        {dated ? (
          <>
            <span className="mn-provenance__sep" aria-hidden="true">
              {' · '}
            </span>
            <span className="mn-visually-hidden">, {m.checked} </span>
            <RelativeTime
              className="mn-provenance__time"
              date={checkedAt}
              tooltip={timeTooltip}
              {...(now !== undefined ? { now } : {})}
              {...(locale !== undefined ? { locale } : {})}
            />
          </>
        ) : null}
      </span>
    </span>
  );
});
