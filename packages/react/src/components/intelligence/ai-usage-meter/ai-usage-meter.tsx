import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Progress } from '../../data-display/progress-bar/index.js';

/** What a usage scope counts. */
export type AiUsageUnit = 'tokens' | 'credits';

/** How close a scope is to its limit. */
export type AiUsageState = 'ok' | 'warning' | 'exceeded';

/** One quota to show, e.g. this turn, this conversation or this month. */
export interface AiUsageScope {
  /** What the scope covers, e.g. "This turn", "Conversation" or "This month". */
  label: string;
  /** Amount used so far, in `unit`. */
  used: number;
  /** The quota. Without it the scope shows the amount only, with no bar and no warning. */
  limit?: number | undefined;
  /** What is counted. */
  unit: AiUsageUnit;
}

/** Every string AiUsageMeter shows or announces. English defaults. */
export interface AiUsageMeterMessages {
  /** Accessible name of the whole meter group. */
  label: string;
  /** The unit after a number, e.g. "tokens". */
  unit: (unit: AiUsageUnit, count: number) => string;
  /** Formats an amount, e.g. "12,400". */
  formatNumber: (value: number) => string;
  /** Formats a 0–1 share of the limit, e.g. "84%". */
  formatPercent: (value: number) => string;
  /** The amount line and the bar's value text, e.g. "12,400 of 50,000 tokens". */
  amount: (used: string, limit: string | undefined, unit: string) => string;
  /** Shown under a scope that has passed `warnAt`. */
  warning: (label: string, percent: string) => string;
  /** Shown under a scope that has reached its limit. */
  exceeded: (label: string) => string;
}

const DEFAULT_MESSAGES: AiUsageMeterMessages = {
  label: 'AI usage',
  unit: (unit, count) =>
    unit === 'tokens' ? (count === 1 ? 'token' : 'tokens') : count === 1 ? 'credit' : 'credits',
  formatNumber: (value) => new Intl.NumberFormat(undefined).format(value),
  formatPercent: (value) =>
    new Intl.NumberFormat(undefined, { style: 'percent', maximumFractionDigits: 0 }).format(value),
  amount: (used, limit, unit) =>
    limit === undefined ? `${used} ${unit}` : `${used} of ${limit} ${unit}`,
  warning: (label, percent) => `${label}: ${percent} of the limit used`,
  exceeded: (label) => `${label}: limit reached`,
};

/**
 * The state of one scope: `exceeded` at or over the limit, `warning` at or
 * above `warnAt` (a 0–1 share of the limit), otherwise `ok`. A scope with no
 * limit is always `ok`.
 */
export function aiUsageState(
  scope: Pick<AiUsageScope, 'used' | 'limit'>,
  warnAt = 0.8,
): AiUsageState {
  const { used, limit } = scope;
  if (limit === undefined || !(limit > 0)) return 'ok';
  const share = Math.max(0, used) / limit;
  if (share >= 1) return 'exceeded';
  if (share >= warnAt) return 'warning';
  return 'ok';
}

const RANK: Record<AiUsageState, number> = { ok: 0, warning: 1, exceeded: 2 };

export interface AiUsageMeterProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The quotas to show, in order, e.g. this turn, the conversation and this month. */
  scopes: readonly AiUsageScope[];
  /** Share of a limit (0–1) at which a scope warns. Default `0.8`. */
  warnAt?: number | undefined;
  /** Strings and number formatting, for localisation. */
  messages?: Partial<AiUsageMeterMessages> | undefined;
}

/**
 * How much AI the person has used against their quotas: one labelled meter
 * per scope (this turn, the conversation, this month) in tokens or credits.
 * A scope warns in text at `warnAt` and says when its limit is reached; the
 * most serious warning is announced politely.
 */
export const AiUsageMeter = forwardRef<HTMLDivElement, AiUsageMeterProps>(function AiUsageMeter(
  { scopes, warnAt = 0.8, messages: messagesProp, className, ...rest },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const rows = scopes.map((scope) => {
    const state = aiUsageState(scope, warnAt);
    const used = Math.max(0, scope.used);
    const hasLimit = scope.limit !== undefined && scope.limit > 0;
    const unit = m.unit(scope.unit, hasLimit ? (scope.limit as number) : used);
    const amount = m.amount(
      m.formatNumber(used),
      hasLimit ? m.formatNumber(scope.limit as number) : undefined,
      unit,
    );
    const percent = hasLimit ? m.formatPercent(used / (scope.limit as number)) : undefined;
    const notice =
      state === 'exceeded'
        ? m.exceeded(scope.label)
        : state === 'warning'
          ? m.warning(scope.label, percent ?? '')
          : undefined;
    return { scope, state, used, hasLimit, amount, notice };
  });
  const worst = rows.reduce<(typeof rows)[number] | undefined>(
    (acc, row) => (row.state !== 'ok' && (!acc || RANK[row.state] > RANK[acc.state]) ? row : acc),
    undefined,
  );

  return (
    <div
      ref={ref}
      role="group"
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-usage-meter', className)}
      data-state={worst?.state ?? 'ok'}
    >
      <ul className="mn-ai-usage-meter__list">
        {rows.map((row, i) => (
          <li
            // Scopes are positional; labels may repeat across units.
            key={i}
            className="mn-ai-usage-meter__scope"
            data-state={row.state}
            data-unit={row.scope.unit}
          >
            <div className="mn-ai-usage-meter__header">
              <span className="mn-ai-usage-meter__label">{row.scope.label}</span>
              <span className="mn-ai-usage-meter__amount">{row.amount}</span>
            </div>
            {row.hasLimit ? (
              <Progress
                semantics="meter"
                size="sm"
                label={row.scope.label}
                value={row.used}
                max={row.scope.limit as number}
                tone={
                  row.state === 'exceeded'
                    ? 'danger'
                    : row.state === 'warning'
                      ? 'warning'
                      : 'neutral'
                }
                valueText={row.amount}
                className="mn-ai-usage-meter__bar"
              />
            ) : null}
            {row.notice ? (
              <p className="mn-ai-usage-meter__notice">
                <span className="mn-ai-usage-meter__icon" aria-hidden>
                  {row.state === 'exceeded' ? <ErrorGlyph /> : <WarningGlyph />}
                </span>
                {row.notice}
              </p>
            ) : null}
          </li>
        ))}
      </ul>
      {/* One polite summary, so crossing a threshold is announced once, not per token. */}
      <div role="status" className="mn-visually-hidden">
        {worst?.notice ?? ''}
      </div>
    </div>
  );
});
