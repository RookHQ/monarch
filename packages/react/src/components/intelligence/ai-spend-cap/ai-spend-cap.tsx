'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ErrorGlyph,
  InfoGlyph,
  LockGlyph,
  PencilGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { currencyDigits, formatMoney } from '../../billing/money/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { InlineNotice } from '../../feedback/inline-notice/index.js';
import { CurrencyInput } from '../../forms/currency-input/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../forms/form-field/index.js';
import { parseNumber, roundTo } from '../../forms/number-input/index.js';
import { RadioGroup } from '../../forms/radio-group/index.js';

/* ------------------------------------------------------------------------ */
/* Model                                                                    */
/* ------------------------------------------------------------------------ */

/**
 * What the integer amounts count. `minor` is the currency's minor unit (pence,
 * cents; yen for a currency with none). `micros` is millionths of the major
 * unit (`12_400_000` is 12.40), for usage priced in fractions of a cent.
 */
export type AiSpendCapUnit = 'minor' | 'micros';

/** The window the limit applies to; spending starts again from zero each period. */
export type AiSpendCapPeriod = 'day' | 'week' | 'month';

/** What happens at the limit: `pause` stops and asks, `notify` keeps going and tells the person. */
export type AiSpendCapBehaviour = 'pause' | 'notify';

/** Where spending stands against the limit. */
export type AiSpendCapState = 'ok' | 'warning' | 'reached' | 'no-limit';

/** The settings a person can change. `onSave` receives one of these. */
export interface AiSpendCapSettings {
  /** The limit as an integer in the component's `unit`, or `null` for no limit. */
  limit: number | null;
  /** Share of the limit (0–1) at which to warn. */
  warnAt: number;
  /** What happens when the limit is reached. */
  behaviour: AiSpendCapBehaviour;
}

/** One row of the breakdown: what an agent or a feature has spent this period. */
export interface AiSpendCapBreakdownRow {
  /** Stable key for the row. */
  id: string;
  /** The agent or feature, e.g. "Research agent" or "Meeting summaries". */
  label: string;
  /** Spent so far this period, as an integer in the component's `unit`. */
  spent: number;
}

const MICROS_DIGITS = 6;

/**
 * The state of a cap: `no-limit` when `limit` is `null`, `reached` at or over
 * the limit (a limit of zero is always reached), `warning` at or above
 * `warnAt` (a 0–1 share of the limit, default `0.8`), otherwise `ok`.
 */
export function aiSpendCapState(cap: {
  spent: number;
  limit: number | null | undefined;
  warnAt?: number | undefined;
}): AiSpendCapState {
  const { spent, limit, warnAt = 0.8 } = cap;
  if (limit === null || limit === undefined) return 'no-limit';
  const used = Math.max(0, spent);
  if (!(limit > 0) || used >= limit) return 'reached';
  // Integer comparison where possible: 80 of 100 is at 0.8 whatever the float says.
  if (used >= limit * warnAt || used / limit >= warnAt) return 'warning';
  return 'ok';
}

/** An integer amount as an exact decimal string of major units: `1240` minor GBP → `"12.40"`. */
function toDecimal(amount: number, currency: string, unit: AiSpendCapUnit): string {
  const digits = unit === 'micros' ? MICROS_DIGITS : currencyDigits(currency);
  const whole = Math.round(amount);
  const negative = whole < 0;
  const raw = Math.abs(whole).toString();
  if (digits === 0) return `${negative ? '-' : ''}${raw}`;
  const padded = raw.padStart(digits + 1, '0');
  return `${negative ? '-' : ''}${padded.slice(0, -digits)}.${padded.slice(-digits)}`;
}

/**
 * Formats an integer amount (minor units or micros) as money with
 * `Intl.NumberFormat`, without going through a float: `1240` GBP in `en-GB` is
 * "£12.40", in `de-DE` "12,40 £".
 */
export function formatAiSpendAmount(
  amount: number,
  options: {
    currency: string;
    unit?: AiSpendCapUnit | undefined;
    locale?: string | undefined;
  },
): string {
  const { currency, unit = 'minor', locale } = options;
  return formatMoney(toDecimal(amount, currency, unit), currency, {
    ...(locale !== undefined && { locale }),
  });
}

/** An integer amount as a major-unit number for an amount field: `1240` minor GBP → `12.4`. */
export function aiSpendAmountToMajor(
  amount: number,
  currency: string,
  unit: AiSpendCapUnit = 'minor',
): number {
  return roundTo(Number(toDecimal(amount, currency, unit)), currencyDigits(currency));
}

/**
 * A typed major-unit amount as an integer in `unit`, rounded to the
 * currency's own decimals first: `12.4` GBP → `1240` minor or `12_400_000` micros.
 */
export function aiSpendAmountFromMajor(
  major: number,
  currency: string,
  unit: AiSpendCapUnit = 'minor',
): number {
  const digits = currencyDigits(currency);
  const shift = unit === 'micros' ? MICROS_DIGITS : digits;
  return Math.round(Number(`${roundTo(major, digits)}e${shift}`));
}

/* ------------------------------------------------------------------------ */
/* Messages                                                                 */
/* ------------------------------------------------------------------------ */

/** Every visible and assistive-technology string. English defaults. */
export interface AiSpendCapMessages {
  /** Heading; also names the region. */
  title: string;
  /** The period in running text, e.g. "this month". */
  period: (period: AiSpendCapPeriod) => string;
  /** The state shown beside the title and announced when it changes. */
  state: (state: AiSpendCapState) => string;
  /** Amount line and the bar's value text, e.g. "£12.40 of £50.00 this month". */
  spentOfLimit: (spent: string, limit: string, period: string) => string;
  /** Amount line without a limit, e.g. "£12.40 spent this month". */
  spentNoLimit: (spent: string, period: string) => string;
  /** Accessible name of the bar. */
  meterLabel: string;
  /** What is left before the limit, e.g. "£37.60 left". */
  remaining: (amount: string) => string;
  /** How far spending is past the limit, e.g. "£2.10 over the limit". */
  over: (amount: string) => string;
  /** When the warning starts, e.g. "Warns at 80%". */
  warnsAt: (percent: string) => string;
  /** One line on what happens at the limit. */
  behaviourSummary: (behaviour: AiSpendCapBehaviour) => string;
  /** When the period starts again, from the formatted date. */
  resets: (date: string) => string;
  /** Caption and accessible name of the breakdown list. */
  breakdownLabel: string;
  /** Button that opens the form when a limit is set. */
  edit: string;
  /** Button that opens the form when there is no limit. */
  setLimit: string;
  /** Lead-in of the notice shown when the limit is reached and AI is paused. */
  pausedTitle: string;
  /** Body of that notice. */
  pausedBody: (limit: string, period: string) => string;
  /** Notice action for people who can edit: opens the form. */
  raiseLimit: string;
  /** Action for people who cannot edit: calls `onRequestIncrease`. */
  requestIncrease: string;
  /** Read-only explanation of who can change the limit. */
  readOnly: string;
  /** Accessible name of the edit form. */
  formLabel: string;
  /** Label of the amount field. */
  amountLabel: (period: string) => string;
  /** Help under the amount field. */
  amountHint: (spent: string, period: string) => string;
  /** Shown by the amount field when the text is not an amount. */
  errorUnreadable: string;
  /** The amount is empty. */
  errorRequired: string;
  /** The amount is zero or less. */
  errorPositive: string;
  /** The amount is above `maxLimit`. */
  errorMax: (max: string) => string;
  /** The amount is below what is already spent; saving again confirms it. */
  belowSpent: (spent: string, period: string, behaviour: AiSpendCapBehaviour) => string;
  /** Label of the warn-at choice. */
  warnAtLabel: string;
  /** One warn-at option, e.g. "80% of the limit". */
  warnAtOption: (percent: string) => string;
  /** Label of the behaviour radio group. */
  behaviourLabel: string;
  /** Name of one behaviour choice. */
  behaviourOption: (behaviour: AiSpendCapBehaviour) => string;
  /** Plain-language description of one behaviour choice. */
  behaviourDescription: (behaviour: AiSpendCapBehaviour) => string;
  /** Save button. */
  save: string;
  /** Save button after the below-spent warning. */
  saveAnyway: string;
  /** Cancel button. */
  cancel: string;
  /** Button that starts removing the limit. */
  removeLimit: string;
  /** Question asked before the limit is removed. */
  removeConfirmTitle: string;
  /** What removing the limit means. */
  removeConfirmBody: string;
  /** Confirms the removal. */
  removeConfirm: string;
  /** Backs out of the removal. */
  removeCancel: string;
  /** Announced once when the state changes. */
  announce: (title: string, state: string, amount: string) => string;
}

export const AI_SPEND_CAP_MESSAGES: AiSpendCapMessages = {
  title: 'AI spending limit',
  period: (period) => (period === 'day' ? 'today' : period === 'week' ? 'this week' : 'this month'),
  state: (state) =>
    state === 'ok'
      ? 'Within limit'
      : state === 'warning'
        ? 'Close to the limit'
        : state === 'reached'
          ? 'Limit reached'
          : 'No limit set',
  spentOfLimit: (spent, limit, period) => `${spent} of ${limit} ${period}`,
  spentNoLimit: (spent, period) => `${spent} spent ${period}`,
  meterLabel: 'Spent against the limit',
  remaining: (amount) => `${amount} left`,
  over: (amount) => `${amount} over the limit`,
  warnsAt: (percent) => `Warns at ${percent}`,
  behaviourSummary: (behaviour) =>
    behaviour === 'pause'
      ? 'At the limit, AI pauses and asks'
      : 'At the limit, AI keeps going and tells you',
  resets: (date) => `Resets ${date}`,
  breakdownLabel: 'Where it was spent',
  edit: 'Edit limit',
  setLimit: 'Set a limit',
  pausedTitle: 'AI is paused.',
  pausedBody: (limit, period) =>
    `The ${limit} limit for ${period} has been reached. Nothing more is spent until the limit is raised or the period resets.`,
  raiseLimit: 'Raise limit',
  requestIncrease: 'Request a higher limit',
  readOnly: 'Only a workspace admin can change this limit.',
  formLabel: 'Edit AI spending limit',
  amountLabel: () => 'Limit',
  amountHint: (spent, period) => `${spent} spent ${period} so far.`,
  errorUnreadable: 'Enter an amount, such as 50.',
  errorRequired: 'Enter an amount. To have no limit, use Remove limit.',
  errorPositive: 'Enter an amount greater than zero.',
  errorMax: (max) => `Enter ${max} or less.`,
  belowSpent: (spent, period, behaviour) =>
    behaviour === 'pause'
      ? `This is below the ${spent} already spent ${period}, so AI will pause straight away. Save again to confirm.`
      : `This is below the ${spent} already spent ${period}, so the limit counts as reached straight away. Save again to confirm.`,
  warnAtLabel: 'Warn me at',
  warnAtOption: (percent) => `${percent} of the limit`,
  behaviourLabel: 'When the limit is reached',
  behaviourOption: (behaviour) =>
    behaviour === 'pause' ? 'Pause and ask' : 'Keep going and tell me',
  behaviourDescription: (behaviour) =>
    behaviour === 'pause'
      ? 'AI stops. Nothing more is spent until someone raises the limit or the period resets.'
      : 'AI carries on and you are notified. Spending can go over the limit.',
  save: 'Save',
  saveAnyway: 'Save anyway',
  cancel: 'Cancel',
  removeLimit: 'Remove limit',
  removeConfirmTitle: 'Remove the spending limit?',
  removeConfirmBody: 'AI will be able to spend without a cap until a new limit is set.',
  removeConfirm: 'Remove limit',
  removeCancel: 'Keep limit',
  announce: (title, state, amount) => `${title}: ${state}. ${amount}`,
};

const DEFAULT_WARN_AT_OPTIONS = [0.5, 0.75, 0.8, 0.9] as const;

/* ------------------------------------------------------------------------ */
/* Component                                                                */
/* ------------------------------------------------------------------------ */

export interface AiSpendCapProps extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'> {
  /** Spent so far in the current period: an integer in `unit`, never a float. */
  spent: number;
  /** The limit as an integer in `unit`, or `null` when no limit is set. */
  limit: number | null;
  /** ISO 4217 currency code of every amount, e.g. `GBP`. */
  currency: string;
  /** What the integers count: `minor` units (default) or `micros`, millionths of the major unit. */
  unit?: AiSpendCapUnit | undefined;
  /** The window the limit applies to. */
  period: AiSpendCapPeriod;
  /** Share of the limit (0–1) at which the state becomes `warning`. Default `0.8`. */
  warnAt?: number | undefined;
  /** What happens at the limit: `pause` (default) stops and asks, `notify` keeps going. */
  behaviour?: AiSpendCapBehaviour | undefined;
  /** When the period starts again (ISO string, Date or epoch milliseconds). */
  resetsAt?: string | number | Date | undefined;
  /** Spending per agent or per feature, shown as a list under the bar. */
  breakdown?: readonly AiSpendCapBreakdownRow[] | undefined;
  /** Whether this person may change the limit. Default `true`; `false` is a read-only view. */
  canEdit?: boolean | undefined;
  /** Whether the edit form is open (controlled). */
  editing?: boolean | undefined;
  /** Whether the edit form starts open (uncontrolled). Default `false`. */
  defaultEditing?: boolean | undefined;
  /** Called when the form opens or closes. */
  onEditingChange?: ((editing: boolean) => void) | undefined;
  /**
   * Called with the new settings on Save, or with `limit: null` once removing
   * the limit is confirmed. Nothing is applied by the component: store the
   * settings and pass them back as props.
   */
  onSave?: ((settings: AiSpendCapSettings) => void) | undefined;
  /** Shows the Save button as busy and locks the form while your save is in flight. */
  saving?: boolean | undefined;
  /** The highest limit this person may set, as an integer in `unit`. */
  maxLimit?: number | undefined;
  /** The warn-at choices as 0–1 shares. Default 50%, 75%, 80% and 90%; the current value is always offered. */
  warnAtOptions?: readonly number[] | undefined;
  /** For people who cannot edit: shows a "Request a higher limit" button that calls this. */
  onRequestIncrease?: (() => void) | undefined;
  /** BCP 47 locale for money, percentages and dates, e.g. `en-GB`. Default: the runtime locale. */
  locale?: string | undefined;
  /** IANA time zone for the reset date. Pass it for identical server and client rendering. */
  timeZone?: string | undefined;
  /** Extra content under the summary, e.g. a link to the full usage report. */
  children?: ReactNode;
  /** Replaces any of the English strings. */
  messages?: Partial<AiSpendCapMessages> | undefined;
}

const STATE_ICON: Record<AiSpendCapState, ReactNode> = {
  ok: <SuccessGlyph />,
  warning: <WarningGlyph />,
  reached: <ErrorGlyph />,
  'no-limit': <InfoGlyph />,
};

const STATE_TONE = {
  ok: 'neutral',
  warning: 'warning',
  reached: 'danger',
  'no-limit': 'neutral',
} as const;

function formatReset(
  value: string | number | Date | undefined,
  period: AiSpendCapPeriod,
  locale: string | undefined,
  timeZone: string | undefined,
): { iso: string; text: string } | null {
  if (value === undefined) return null;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  const text = new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'long',
    ...(period === 'day' && { hour: 'numeric', minute: '2-digit' }),
    ...(timeZone !== undefined && { timeZone }),
  }).format(date);
  return { iso: date.toISOString(), text };
}

/**
 * A spending limit for AI usage: what has been spent against the limit this
 * period, whether that is fine, close or reached, and a form to change the
 * limit, when to warn and what happens at the limit. It reports decisions
 * through `onSave`; it never bills, blocks or fetches anything itself.
 */
export const AiSpendCap = forwardRef<HTMLElement, AiSpendCapProps>(function AiSpendCap(
  {
    spent,
    limit,
    currency,
    unit = 'minor',
    period,
    warnAt = 0.8,
    behaviour = 'pause',
    resetsAt,
    breakdown,
    canEdit = true,
    editing: editingProp,
    defaultEditing = false,
    onEditingChange,
    onSave,
    saving = false,
    maxLimit,
    warnAtOptions = DEFAULT_WARN_AT_OPTIONS,
    onRequestIncrease,
    locale,
    timeZone,
    children,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_SPEND_CAP_MESSAGES, ...messagesProp };
  const titleId = useId();
  const breakdownId = useId();
  const rootRef = useRef<HTMLElement | null>(null);
  const editButtonRef = useRef<HTMLButtonElement | null>(null);

  const [editingInner, setEditingInner] = useState(defaultEditing);
  const editing = canEdit && (editingProp !== undefined ? editingProp : editingInner);
  const setEditing = (next: boolean) => {
    if (editingProp === undefined) setEditingInner(next);
    onEditingChange?.(next);
  };

  const money = (amount: number) => formatAiSpendAmount(amount, { currency, unit, locale });
  const percent = (share: number) =>
    new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 0 }).format(share);

  const state = aiSpendCapState({ spent, limit, warnAt });
  const used = Math.max(0, spent);
  const periodText = m.period(period);
  const amountText =
    limit === null
      ? m.spentNoLimit(money(used), periodText)
      : m.spentOfLimit(money(used), money(limit), periodText);
  const reset = formatReset(resetsAt, period, locale, timeZone);
  const paused = state === 'reached' && behaviour === 'pause';

  // Announce a change of state once, never the state it first rendered with.
  const previousState = useRef<AiSpendCapState | null>(null);
  const [live, setLive] = useState('');
  useEffect(() => {
    const before = previousState.current;
    previousState.current = state;
    if (before === null || before === state) return;
    setLive(m.announce(m.title, m.state(state), amountText));
    // Only a change of state is news; the amount moves on every request.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  // Closing the form puts focus back on the button that opens it, if focus was in here.
  const wasEditing = useRef(editing);
  useEffect(() => {
    const before = wasEditing.current;
    wasEditing.current = editing;
    if (!before || editing) return;
    const active = document.activeElement;
    if (!active || active === document.body || rootRef.current?.contains(active)) {
      editButtonRef.current?.focus();
    }
  }, [editing]);

  const requestButton =
    !canEdit && onRequestIncrease ? (
      <Button size="sm" variant="secondary" onClick={onRequestIncrease}>
        {m.requestIncrease}
      </Button>
    ) : null;

  return (
    <section
      ref={(el) => {
        rootRef.current = el;
        if (typeof ref === 'function') ref(el);
        else if (ref) ref.current = el;
      }}
      // A named group, not a region: a page may show several, and landmarks must be unique.
      role="group"
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-ai-spend-cap', className)}
      data-state={state}
      data-behaviour={behaviour}
      data-editing={editing || undefined}
      data-readonly={!canEdit || undefined}
    >
      <header className="mn-ai-spend-cap__header">
        <div className="mn-ai-spend-cap__heading">
          <h3 id={titleId} className="mn-ai-spend-cap__title">
            {m.title}
          </h3>
          <span className="mn-ai-spend-cap__state">
            <span className="mn-ai-spend-cap__state-icon" aria-hidden>
              {STATE_ICON[state]}
            </span>
            {m.state(state)}
          </span>
        </div>
        {canEdit && !editing ? (
          <Button
            ref={editButtonRef}
            size="sm"
            variant="secondary"
            className="mn-ai-spend-cap__edit"
            leadingIcon={<PencilGlyph />}
            onClick={() => setEditing(true)}
          >
            {limit === null ? m.setLimit : m.edit}
          </Button>
        ) : null}
      </header>

      <p className="mn-ai-spend-cap__amount">{amountText}</p>

      {limit !== null && limit > 0 ? (
        <Progress
          className="mn-ai-spend-cap__bar"
          semantics="meter"
          size="sm"
          label={m.meterLabel}
          value={Math.min(used, limit)}
          max={limit}
          tone={STATE_TONE[state]}
          valueText={amountText}
        />
      ) : null}

      <p className="mn-ai-spend-cap__meta">
        {limit !== null ? (
          <>
            <span>
              {used > limit ? m.over(money(used - limit)) : m.remaining(money(limit - used))}
            </span>
            <span>{m.warnsAt(percent(warnAt))}</span>
            <span>{m.behaviourSummary(behaviour)}</span>
          </>
        ) : null}
        {reset ? <time dateTime={reset.iso}>{m.resets(reset.text)}</time> : null}
      </p>

      {paused ? (
        <InlineNotice
          className="mn-ai-spend-cap__notice"
          tone="danger"
          title={m.pausedTitle}
          action={
            canEdit ? (
              editing ? null : (
                <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>
                  {m.raiseLimit}
                </Button>
              )
            ) : (
              requestButton
            )
          }
        >
          {m.pausedBody(money(limit ?? 0), periodText)}
        </InlineNotice>
      ) : null}

      {breakdown && breakdown.length ? (
        <div className="mn-ai-spend-cap__breakdown">
          <p id={breakdownId} className="mn-ai-spend-cap__breakdown-label">
            {m.breakdownLabel}
          </p>
          <ul className="mn-ai-spend-cap__rows" aria-labelledby={breakdownId}>
            {breakdown.map((row) => (
              <li key={row.id} className="mn-ai-spend-cap__row">
                <span className="mn-ai-spend-cap__row-label" dir="auto">
                  {row.label}
                </span>
                <span className="mn-ai-spend-cap__row-amount">{money(Math.max(0, row.spent))}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {children}

      {editing ? (
        <AiSpendCapEditor
          m={m}
          spent={used}
          limit={limit}
          currency={currency}
          unit={unit}
          periodText={periodText}
          warnAt={warnAt}
          behaviour={behaviour}
          maxLimit={maxLimit}
          warnAtOptions={warnAtOptions}
          saving={saving}
          locale={locale}
          money={money}
          percent={percent}
          onSave={(settings) => {
            onSave?.(settings);
            setEditing(false);
          }}
          onCancel={() => setEditing(false)}
        />
      ) : null}

      {!canEdit ? (
        <div className="mn-ai-spend-cap__readonly">
          <p className="mn-ai-spend-cap__readonly-text">
            <span className="mn-ai-spend-cap__readonly-icon" aria-hidden>
              <LockGlyph />
            </span>
            {m.readOnly}
          </p>
          {paused ? null : requestButton}
        </div>
      ) : null}

      {/* One polite line per change of state, not per amount. */}
      <div role="status" className="mn-visually-hidden">
        {live}
      </div>
    </section>
  );
});

/* ------------------------------------------------------------------------ */
/* Edit form                                                                */
/* ------------------------------------------------------------------------ */

interface EditorProps {
  m: AiSpendCapMessages;
  spent: number;
  limit: number | null;
  currency: string;
  unit: AiSpendCapUnit;
  periodText: string;
  warnAt: number;
  behaviour: AiSpendCapBehaviour;
  maxLimit: number | undefined;
  warnAtOptions: readonly number[];
  saving: boolean;
  locale: string | undefined;
  money: (amount: number) => string;
  percent: (share: number) => string;
  onSave: (settings: AiSpendCapSettings) => void;
  onCancel: () => void;
}

/** The draft lives here, so it starts from the saved settings each time the form opens. */
function AiSpendCapEditor({
  m,
  spent,
  limit,
  currency,
  unit,
  periodText,
  warnAt,
  behaviour,
  maxLimit,
  warnAtOptions,
  saving,
  locale,
  money,
  percent,
  onSave,
  onCancel,
}: EditorProps) {
  const amountRef = useRef<HTMLInputElement | null>(null);
  const removeRef = useRef<HTMLButtonElement | null>(null);
  const keepRef = useRef<HTMLButtonElement | null>(null);
  const confirmTitleId = useId();
  const confirmBodyId = useId();

  const shares = Array.from(new Set([...warnAtOptions, warnAt]))
    .filter((share) => share > 0 && share < 1)
    .sort((a, b) => a - b);
  // A saved share the list cannot offer (0, 1, out of range) starts on the
  // nearest choice, so the value that is saved is always the one that is shown.
  const [draftWarnAt, setDraftWarnAt] = useState(() =>
    shares.includes(warnAt) || shares.length === 0
      ? warnAt
      : shares.reduce((best, share) =>
          Math.abs(share - warnAt) < Math.abs(best - warnAt) ? share : best,
        ),
  );
  const [draftBehaviour, setDraftBehaviour] = useState(behaviour);
  const [error, setError] = useState<string | null>(null);
  /** The below-spent amount the person has been warned about; saving it again confirms. */
  const [warnedBelow, setWarnedBelow] = useState<number | null>(null);
  const [removing, setRemoving] = useState(false);

  useEffect(() => {
    amountRef.current?.focus();
  }, []);

  const wasRemoving = useRef(false);
  useEffect(() => {
    if (removing) keepRef.current?.focus();
    else if (wasRemoving.current) removeRef.current?.focus();
    wasRemoving.current = removing;
  }, [removing]);

  const fail = (message: string) => {
    setWarnedBelow(null);
    setError(message);
    amountRef.current?.focus();
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (saving || removing) return;
    // Read what is typed, not the last committed value: the field never commits bad text.
    const major = parseNumber(amountRef.current?.value ?? '', locale);
    if (major === null) return fail(m.errorRequired);
    if (Number.isNaN(major)) {
      // The amount field explains unreadable text itself.
      setWarnedBelow(null);
      setError(null);
      amountRef.current?.focus();
      return;
    }
    const next = aiSpendAmountFromMajor(major, currency, unit);
    if (!(next > 0)) return fail(m.errorPositive);
    if (maxLimit !== undefined && next > maxLimit) return fail(m.errorMax(money(maxLimit)));
    if (next < spent && warnedBelow !== next) {
      setWarnedBelow(next);
      setError(m.belowSpent(money(spent), periodText, draftBehaviour));
      return;
    }
    onSave({ limit: next, warnAt: draftWarnAt, behaviour: draftBehaviour });
  };

  const locked = saving || removing;

  return (
    <form className="mn-ai-spend-cap__form" aria-label={m.formLabel} noValidate onSubmit={submit}>
      <Field invalid={error !== null && warnedBelow === null}>
        <FieldLabel>{m.amountLabel(periodText)}</FieldLabel>
        <CurrencyInput
          ref={amountRef}
          currency={currency}
          locale={locale}
          allowNegative
          defaultValue={limit === null ? null : aiSpendAmountToMajor(limit, currency, unit)}
          disabled={locked}
          messages={{ invalid: m.errorUnreadable }}
          onInput={() => {
            if (error !== null) setError(null);
            if (warnedBelow !== null) setWarnedBelow(null);
          }}
        />
        <FieldDescription>{m.amountHint(money(spent), periodText)}</FieldDescription>
        <FieldError>{error}</FieldError>
      </Field>

      <Field>
        <FieldLabel>{m.warnAtLabel}</FieldLabel>
        <NativeSelect
          value={String(draftWarnAt)}
          disabled={locked}
          onChange={(event) => setDraftWarnAt(Number(event.target.value))}
          options={shares.map((share) => ({
            value: String(share),
            label: m.warnAtOption(percent(share)),
          }))}
        />
      </Field>

      <RadioGroup
        label={m.behaviourLabel}
        value={draftBehaviour}
        disabled={locked}
        onValueChange={(value) => setDraftBehaviour(value === 'notify' ? 'notify' : 'pause')}
        options={(['pause', 'notify'] as const).map((value) => ({
          value,
          label: m.behaviourOption(value),
          description: m.behaviourDescription(value),
        }))}
      />

      {removing ? (
        <div
          className="mn-ai-spend-cap__confirm"
          role="group"
          aria-labelledby={confirmTitleId}
          aria-describedby={confirmBodyId}
        >
          <p className="mn-ai-spend-cap__confirm-text">
            <strong id={confirmTitleId}>{m.removeConfirmTitle}</strong>{' '}
            <span id={confirmBodyId}>{m.removeConfirmBody}</span>
          </p>
          <div className="mn-ai-spend-cap__confirm-actions">
            <Button
              ref={keepRef}
              type="button"
              size="sm"
              variant="secondary"
              aria-describedby={confirmBodyId}
              onClick={() => setRemoving(false)}
            >
              {m.removeCancel}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="danger"
              loading={saving}
              onClick={() => onSave({ limit: null, warnAt, behaviour })}
            >
              {m.removeConfirm}
            </Button>
          </div>
        </div>
      ) : (
        <div className="mn-ai-spend-cap__actions">
          {limit !== null ? (
            <Button
              ref={removeRef}
              type="button"
              size="sm"
              variant="danger-subtle"
              className="mn-ai-spend-cap__remove"
              disabled={saving}
              onClick={() => setRemoving(true)}
            >
              {m.removeLimit}
            </Button>
          ) : null}
          <span className="mn-ai-spend-cap__actions-main">
            <Button type="button" size="sm" variant="ghost" disabled={saving} onClick={onCancel}>
              {m.cancel}
            </Button>
            <Button type="submit" size="sm" variant="primary" loading={saving}>
              {warnedBelow !== null ? m.saveAnyway : m.save}
            </Button>
          </span>
        </div>
      )}
    </form>
  );
}
