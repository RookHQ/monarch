import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** A moment: a Date, epoch milliseconds or an ISO 8601 string. */
export type IncidentDateInput = Date | number | string;

/** The standard incident stages, in the order an incident normally moves through them. */
export type IncidentStage = 'investigating' | 'identified' | 'monitoring' | 'resolved';

/** Marker colour of a stage. Status tokens only, so every theme works. */
export type IncidentStageTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

export interface IncidentStageDefinition {
  /** Text shown for the stage ("Investigating"). The text always carries the meaning. */
  label: string;
  tone: IncidentStageTone;
}

export interface IncidentUpdate {
  /** Stable key. */
  id: string;
  /** A standard stage or a product-defined one from `stages`. */
  stage: IncidentStage | (string & {});
  /** The update text. Plain strings keep their line breaks. */
  body: ReactNode;
  /** When it was posted. */
  time: IncidentDateInput;
  /**
   * Posted by the system, not a person. Tagged "Automated" so readers are never
   * told a person wrote something they did not.
   */
  automated?: boolean;
  /** Who posted it, when the product shows authors (for example on an internal view). */
  author?: ReactNode;
}

export interface IncidentTimelineMessages {
  /** Accessible name of the list. Default "Incident updates". */
  label: string;
  /** Tag for system-posted updates. Default "Automated". */
  automated: string;
  /** Shown when there are no updates. Default "No updates posted yet". */
  empty: string;
  investigating: string;
  identified: string;
  monitoring: string;
  resolved: string;
}

const DEFAULT_MESSAGES: IncidentTimelineMessages = {
  label: 'Incident updates',
  automated: 'Automated',
  empty: 'No updates posted yet',
  investigating: 'Investigating',
  identified: 'Identified',
  monitoring: 'Monitoring',
  resolved: 'Resolved',
};

const STAGE_TONES: Record<IncidentStage, IncidentStageTone> = {
  investigating: 'danger',
  identified: 'warning',
  monitoring: 'info',
  resolved: 'success',
};

/** Resolves a stage key to its label and tone; unknown keys get a readable label and a neutral tone. */
export function resolveIncidentStage(
  stage: string,
  stages?: Readonly<Record<string, IncidentStageDefinition>>,
  messages: Partial<IncidentTimelineMessages> = {},
): IncidentStageDefinition {
  const custom = stages?.[stage];
  if (custom) return custom;
  const key = stage.toLowerCase() as IncidentStage;
  if (key in STAGE_TONES) {
    return { label: messages[key] ?? DEFAULT_MESSAGES[key], tone: STAGE_TONES[key] };
  }
  const words = stage.replace(/[_-]+/g, ' ').trim().toLowerCase();
  return { label: words ? words.charAt(0).toUpperCase() + words.slice(1) : stage, tone: 'neutral' };
}

function toDate(value: IncidentDateInput): Date {
  return value instanceof Date ? value : new Date(value);
}

/**
 * Formats a moment for incident text. The time zone defaults to UTC so the
 * server and the browser render the same characters, and readers in different
 * zones quote the same time.
 */
export function formatIncidentTime(
  value: IncidentDateInput,
  options: { locale?: string | undefined; timeZone?: string | undefined } = {},
): string {
  const date = toDate(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(options.locale, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: options.timeZone ?? 'UTC',
    timeZoneName: 'short',
  }).format(date);
}

export interface IncidentTimelineProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The updates, in the order to show them. Status pages usually show oldest first, so the incident reads as a story. */
  updates: readonly IncidentUpdate[];
  /** Product stages, or overrides for the standard ones (label and tone). */
  stages?: Readonly<Record<string, IncidentStageDefinition>> | undefined;
  /** `compact` tightens spacing for cards and side panels. Default `default`. */
  density?: 'default' | 'compact';
  /**
   * Announce updates added after the first render to screen readers. Turn on
   * for a page that refreshes while someone reads it. Default `false`.
   */
  live?: boolean;
  /** Content under the list, for example the product's form for posting an update. */
  footer?: ReactNode;
  /** BCP 47 locale for times. Default: the runtime locale. */
  locale?: string | undefined;
  /** IANA time zone for times. Default `UTC`, so every reader sees the same time. */
  timeZone?: string | undefined;
  /** Text overrides for translation. */
  messages?: Partial<IncidentTimelineMessages>;
}

/**
 * The story of one incident: each update with its stage (investigating,
 * identified, monitoring, resolved), time and text, joined by a connector
 * line. System-posted updates are tagged so readers can tell them from people.
 */
export const IncidentTimeline = forwardRef<HTMLDivElement, IncidentTimelineProps>(
  function IncidentTimeline(
    {
      updates,
      stages,
      density = 'default',
      live = false,
      footer,
      locale,
      timeZone,
      messages: messagesProp,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const label = ariaLabel ?? m.label;
    const last = updates[updates.length - 1];
    const current = last ? resolveIncidentStage(last.stage, stages, m) : undefined;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-incident-timeline', className)}
        data-density={density}
        data-stage={last ? String(last.stage).toLowerCase() : undefined}
        data-tone={current?.tone}
        data-empty={updates.length === 0 || undefined}
      >
        {updates.length === 0 ? (
          <p className="mn-incident-timeline__empty">{m.empty}</p>
        ) : (
          <ol
            className="mn-incident-timeline__list"
            aria-label={label}
            aria-live={live ? 'polite' : undefined}
            aria-relevant={live ? 'additions' : undefined}
          >
            {updates.map((u) => {
              const def = resolveIncidentStage(u.stage, stages, m);
              const date = toDate(u.time);
              const valid = !Number.isNaN(date.getTime());
              return (
                <li
                  key={u.id}
                  className="mn-incident-timeline__item"
                  data-stage={String(u.stage).toLowerCase()}
                  data-tone={def.tone}
                  data-automated={u.automated || undefined}
                >
                  <span className="mn-incident-timeline__marker" aria-hidden />
                  <div className="mn-incident-timeline__content">
                    <p className="mn-incident-timeline__header">
                      <span className="mn-incident-timeline__stage">{def.label}</span>
                      {valid ? (
                        <time className="mn-incident-timeline__time" dateTime={date.toISOString()}>
                          {formatIncidentTime(date, { locale, timeZone })}
                        </time>
                      ) : null}
                      {u.author ? (
                        <span className="mn-incident-timeline__author">{u.author}</span>
                      ) : null}
                      {u.automated ? (
                        <span className="mn-incident-timeline__tag">{m.automated}</span>
                      ) : null}
                    </p>
                    <div className="mn-incident-timeline__body">{u.body}</div>
                  </div>
                </li>
              );
            })}
          </ol>
        )}
        {footer ? <div className="mn-incident-timeline__footer">{footer}</div> : null}
      </div>
    );
  },
);
