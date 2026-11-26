import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, WarningGlyph } from '../../../internal/icons.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { InlineNotice } from '../../feedback/inline-notice/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

/**
 * Where a custom domain is in its connection:
 * `dns` waiting for the routing record, `ownership` waiting for a
 * verification record, `certificate` issuing HTTPS, `page` checking the site
 * loads, `live` done.
 */
export type DomainStage = 'dns' | 'ownership' | 'certificate' | 'page' | 'live';

/** The three steps the user sees. */
export type DomainStep = 'dns' | 'certificate' | 'live';

export type DomainStepState = 'done' | 'current' | 'todo' | 'error';

/** Something the product found while checking the domain. */
export interface DomainProblem {
  /** Machine code from the product, kept for keys and analytics. */
  code?: string | undefined;
  /** What is wrong and how to fix it, in plain words. */
  message: ReactNode;
  /**
   * `blocking` (default) is a mistake the user must fix and is shown as a
   * warning; `info` is a normal wait ("certificate pending") shown quietly.
   */
  severity?: 'blocking' | 'info' | undefined;
  /** The step the problem belongs to. Default: the current step. */
  step?: DomainStep | undefined;
}

/** What public DNS shows for the host right now. */
export interface DomainDnsSnapshot {
  cnames?: readonly string[] | undefined;
  a?: readonly string[] | undefined;
  aaaa?: readonly string[] | undefined;
}

export interface DomainProgressMessages {
  dnsTitle: (stage: DomainStage, state: DomainStepState) => string;
  dnsDetail: (stage: DomainStage, state: DomainStepState) => string;
  certificateTitle: (state: DomainStepState) => string;
  certificateDetail: (state: DomainStepState) => string;
  liveTitle: (state: DomainStepState) => string;
  liveDetail: (state: DomainStepState, host: string) => string;
  /** Visually hidden state after each step title. */
  stateLabel: (state: DomainStepState) => string;
  /** Accessible name of the step list. */
  listLabel: (host: string) => string;
  /** Announced when the step changes. */
  announce: (index: number, total: number, title: string, stateLabel: string) => string;
  /** The DNS snapshot sentence. `seen` is preformatted ("CNAME → x"). */
  dnsSeen: (host: string, seen: string) => ReactNode;
  noRecord: string;
  /** Shown while `rechecking`, by how the product will tell the user. */
  rechecking: (notify: 'email' | 'in-app' | null | undefined) => string;
  /** "Checked {time}." */
  checked: (time: ReactNode) => ReactNode;
}

const DEFAULT_MESSAGES: DomainProgressMessages = {
  dnsTitle: (stage, state) =>
    state === 'done'
      ? 'DNS record found'
      : stage === 'ownership'
        ? 'Ownership record needed'
        : 'Waiting for your DNS record',
  dnsDetail: (_stage, state) =>
    state === 'done'
      ? 'Your domain points here.'
      : 'Add the record shown below at your DNS provider.',
  certificateTitle: (state) =>
    state === 'done' ? 'HTTPS certificate issued' : 'HTTPS certificate',
  certificateDetail: (state) =>
    state === 'done'
      ? 'Secure connection works.'
      : state === 'todo'
        ? 'Starts automatically once DNS is found.'
        : 'Being issued now, usually 1 to 5 minutes.',
  liveTitle: () => 'Live',
  liveDetail: (state, host) =>
    state === 'done'
      ? `Your site loads on ${host}.`
      : state === 'current'
        ? `Checking that your site loads on ${host}.`
        : `We load ${host} over HTTPS before calling it live.`,
  stateLabel: (state) =>
    state === 'done'
      ? 'done'
      : state === 'current'
        ? 'in progress'
        : state === 'error'
          ? 'needs attention'
          : 'not started',
  listLabel: (host) => `Progress for ${host}`,
  announce: (i, total, title, state) => `Step ${i} of ${total}: ${title}, ${state}.`,
  dnsSeen: (host, seen) => (
    <>
      Public DNS for <code>{host}</code> shows <code>{seen}</code>.
    </>
  ),
  noRecord: 'no record yet',
  rechecking: (notify) =>
    notify === 'email'
      ? 'We keep checking automatically, even after you close this page, and email you when it is live.'
      : notify === 'in-app'
        ? 'We keep checking automatically, even after you close this page, and notify you here when it is live.'
        : 'We keep checking automatically, even after you close this page.',
  checked: (time) => <>Checked {time}.</>,
};

/** Formats a DNS snapshot as one line ("CNAME → x" or "A → 1.2.3.4 · AAAA → …"). */
export function formatDnsSnapshot(found: DomainDnsSnapshot, empty = 'no record yet'): string {
  if (found.cnames?.length) return `CNAME → ${found.cnames[0]}`;
  if (found.a?.length) {
    const aaaa = found.aaaa?.length ? ` · AAAA → ${found.aaaa.join(', ')}` : '';
    return `A → ${found.a.join(', ')}${aaaa}`;
  }
  if (found.aaaa?.length) return `AAAA → ${found.aaaa.join(', ')}`;
  return empty;
}

/**
 * How often to re-check a domain that is not live yet: the product's
 * suggested delay in seconds kept between 15 s and 5 min (30 s when unknown).
 * `false` once live or when checking has stopped (`recheckAfterSec: null`).
 */
export function domainPollMs(
  stage: DomainStage | undefined,
  recheckAfterSec?: number | null,
): number | false {
  if (stage === undefined) return 15_000;
  if (stage === 'live' || recheckAfterSec === null) return false;
  return Math.min(300, Math.max(15, recheckAfterSec ?? 30)) * 1000;
}

const STEPS: readonly DomainStep[] = ['dns', 'certificate', 'live'];

function baseState(step: DomainStep, stage: DomainStage): DomainStepState {
  const dnsDone = stage !== 'dns' && stage !== 'ownership';
  const certDone = stage === 'page' || stage === 'live';
  if (step === 'dns') return dnsDone ? 'done' : 'current';
  if (step === 'certificate') return certDone ? 'done' : dnsDone ? 'current' : 'todo';
  return stage === 'live' ? 'done' : stage === 'page' ? 'current' : 'todo';
}

/** The state of each of the three steps for a stage and a list of problems. */
export function domainStepStates(
  stage: DomainStage,
  problems: readonly DomainProblem[] = [],
): Record<DomainStep, DomainStepState> {
  const states = {
    dns: baseState('dns', stage),
    certificate: baseState('certificate', stage),
    live: baseState('live', stage),
  };
  const current = STEPS.find((s) => states[s] === 'current');
  for (const p of problems) {
    if (p.severity === 'info') continue;
    const step = p.step ?? current;
    if (step && states[step] !== 'done') states[step] = 'error';
  }
  return states;
}

export interface DomainProgressProps extends HTMLAttributes<HTMLDivElement> {
  /** The custom hostname, e.g. `book.example.com`. */
  host: string;
  /** Where the domain is. */
  stage: DomainStage;
  /** What the last check found. Blocking problems mark their step and show as warnings. */
  problems?: readonly DomainProblem[] | undefined;
  /** What public DNS shows for `host` right now. `null` hides the line. */
  dnsFound?: DomainDnsSnapshot | null | undefined;
  /** When the product last checked. */
  checkedAt?: DateInput | undefined;
  /** Reference time for `checkedAt`, for identical server and client output. */
  now?: DateInput | undefined;
  /** The product keeps checking on its own. Default true. */
  rechecking?: boolean | undefined;
  /** How the product tells the user it went live. Changes the rechecking sentence. */
  notify?: 'email' | 'in-app' | null | undefined;
  /** Interface text, for translation. */
  messages?: Partial<DomainProgressMessages> | undefined;
}

/**
 * Stage-by-stage progress for connecting a custom domain: DNS record found,
 * HTTPS certificate issued, live. Shows the problems the product found, what
 * public DNS shows right now and that checking continues on its own.
 */
export const DomainProgress = forwardRef<HTMLDivElement, DomainProgressProps>(
  function DomainProgress(
    {
      host,
      stage,
      problems = [],
      dnsFound,
      checkedAt,
      now,
      rechecking = true,
      notify,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messages };
    const states = domainStepStates(stage, problems);
    const steps = STEPS.map((step) => {
      const state = states[step];
      const title =
        step === 'dns'
          ? m.dnsTitle(stage, state)
          : step === 'certificate'
            ? m.certificateTitle(state)
            : m.liveTitle(state);
      const detail =
        step === 'dns'
          ? m.dnsDetail(stage, state)
          : step === 'certificate'
            ? m.certificateDetail(state)
            : m.liveDetail(state, host);
      return { step, state, title, detail };
    });
    const blocking = problems.filter((p) => p.severity !== 'info');
    const info = problems.filter((p) => p.severity === 'info');
    const live = stage === 'live';
    const found = steps.findIndex((s) => s.state === 'current' || s.state === 'error');
    const activeIndex = found >= 0 ? found : steps.length - 1;
    const active = steps[activeIndex] ?? steps[0];
    const announceIndex = activeIndex + 1;

    return (
      <div ref={ref} {...rest} className={cx('mn-domain-progress', className)} data-stage={stage}>
        <ol className="mn-domain-progress__steps" aria-label={m.listLabel(host)}>
          {steps.map((s) => (
            <li
              key={s.step}
              className="mn-domain-progress__step"
              data-step={s.step}
              data-state={s.state}
              aria-current={s === active && !live ? 'step' : undefined}
            >
              <span className="mn-domain-progress__dot" aria-hidden>
                {s.state === 'done' ? (
                  <CheckGlyph />
                ) : s.state === 'error' ? (
                  <WarningGlyph />
                ) : s.state === 'current' ? (
                  <Spinner size="sm" />
                ) : null}
              </span>
              <div className="mn-domain-progress__text">
                <p className="mn-domain-progress__title">
                  {s.title}
                  <span className="mn-visually-hidden"> ({m.stateLabel(s.state)})</span>
                </p>
                <p className="mn-domain-progress__detail">{s.detail}</p>
              </div>
            </li>
          ))}
        </ol>

        <p className="mn-visually-hidden" role="status">
          {active
            ? m.announce(announceIndex, steps.length, active.title, m.stateLabel(active.state))
            : null}
        </p>

        {blocking.length > 0 ? (
          <div className="mn-domain-progress__problems">
            {blocking.map((p, i) => (
              <InlineNotice key={p.code ? `${p.code}-${i}` : i} tone="warning" live>
                {p.message}
              </InlineNotice>
            ))}
          </div>
        ) : info[0] ? (
          <InlineNotice tone="neutral" live>
            {info[0].message}
          </InlineNotice>
        ) : null}

        {!live && (dnsFound || rechecking || checkedAt !== undefined) ? (
          <div className="mn-domain-progress__meta">
            {dnsFound || rechecking ? (
              <p role="status">
                {dnsFound ? <>{m.dnsSeen(host, formatDnsSnapshot(dnsFound, m.noRecord))} </> : null}
                {rechecking ? m.rechecking(notify) : null}
              </p>
            ) : null}
            {checkedAt !== undefined ? (
              <p>
                {m.checked(
                  <RelativeTime
                    date={checkedAt}
                    {...(now !== undefined ? { now } : {})}
                    live
                    tooltip={false}
                  />,
                )}
              </p>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  },
);
