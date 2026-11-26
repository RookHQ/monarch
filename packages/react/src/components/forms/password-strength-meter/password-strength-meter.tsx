import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, DotGlyph } from '../../../internal/icons.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** A requirement shown in the list, with its own test. */
export interface PasswordRule {
  /** Stable key. */
  id: string;
  /** What the password needs, e.g. "At least 8 characters". */
  label: string;
  /** Returns `true` when the password meets the rule. */
  test: (password: string) => boolean;
}

/** 0 (very weak) to 4 (strong). */
export type PasswordScore = 0 | 1 | 2 | 3 | 4;

/** The default requirements: at least 8 characters, mixed case, a number and a symbol. */
export const DEFAULT_PASSWORD_RULES: readonly PasswordRule[] = [
  { id: 'length', label: 'At least 8 characters', test: (p) => Array.from(p).length >= 8 },
  {
    id: 'case',
    label: 'Upper and lower case letters',
    test: (p) => /\p{Lu}/u.test(p) && /\p{Ll}/u.test(p),
  },
  { id: 'number', label: 'A number', test: (p) => /\p{Nd}/u.test(p) },
  { id: 'symbol', label: 'A symbol or space', test: (p) => /[^\p{L}\p{Nd}]/u.test(p) },
];

/**
 * The default score: the share of rules met, plus one point for 14 or more
 * characters once every rule is met. An empty password scores 0. Replace it
 * with a stronger estimator (for example zxcvbn) through `score`.
 */
export function scorePassword(
  password: string,
  rules: readonly PasswordRule[] = DEFAULT_PASSWORD_RULES,
): PasswordScore {
  if (password === '') return 0;
  const met = rules.filter((r) => r.test(password)).length;
  const ratio = rules.length === 0 ? 1 : met / rules.length;
  if (ratio === 1) return Array.from(password).length >= 14 ? 4 : 3;
  if (ratio >= 0.75) return 2;
  if (ratio >= 0.5) return 1;
  return 0;
}

/** Interface text, for translation. */
export interface PasswordStrengthMeterMessages {
  /** Strength names for scores 0 to 4. Default Very weak, Weak, Fair, Good, Strong. */
  levels: readonly [string, string, string, string, string];
  /** Shown instead of a level while the password is empty. Default "Not entered". */
  empty: string;
  /** The strength line. Default "Password strength: Good". */
  strength: (level: string) => string;
  /** Heading of the rules list for assistive technology. Default "Password requirements". */
  rulesLabel: string;
  /** Hidden prefix of a met rule. Default "Met:". */
  met: string;
  /** Hidden prefix of an unmet rule. Default "Not met:". */
  unmet: string;
}

export const PASSWORD_STRENGTH_METER_MESSAGES: PasswordStrengthMeterMessages = {
  levels: ['Very weak', 'Weak', 'Fair', 'Good', 'Strong'],
  empty: 'Not entered',
  strength: (level) => `Password strength: ${level}`,
  rulesLabel: 'Password requirements',
  met: 'Met:',
  unmet: 'Not met:',
};

export interface PasswordStrengthMeterProps extends HTMLAttributes<HTMLDivElement> {
  /** The password to rate. Nothing is stored or sent. */
  password: string;
  /** Requirements listed under the bar. Default `DEFAULT_PASSWORD_RULES`; `[]` hides the list. */
  rules?: readonly PasswordRule[] | undefined;
  /** Custom strength estimator returning 0–4. Default `scorePassword(password, rules)`. */
  score?: ((password: string) => PasswordScore) | undefined;
  /** Show the rules list. Default `true`. */
  showRules?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PasswordStrengthMeterMessages> | undefined;
}

/**
 * Shows how strong a password is while it is typed: a four-part bar, the
 * strength in words ("Password strength: Good"), and each requirement with
 * met or not met in text and icon, so nothing depends on colour. The strength
 * line is a polite live region. Give it an `id` and add that id to the
 * password input's `aria-describedby`.
 */
export const PasswordStrengthMeter = forwardRef<HTMLDivElement, PasswordStrengthMeterProps>(
  function PasswordStrengthMeter(
    {
      password,
      rules = DEFAULT_PASSWORD_RULES,
      score,
      showRules = true,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...PASSWORD_STRENGTH_METER_MESSAGES, ...messagesProp };
    const empty = password === '';
    const value: PasswordScore = empty ? 0 : (score ?? ((p) => scorePassword(p, rules)))(password);
    const level = empty ? messages.empty : messages.levels[value];
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-password-strength-meter', className)}
        data-score={empty ? undefined : value}
        data-empty={empty || undefined}
      >
        <div className="mn-password-strength-meter__bar" aria-hidden>
          {[1, 2, 3, 4].map((segment) => (
            <span
              key={segment}
              className="mn-password-strength-meter__segment"
              data-filled={!empty && segment <= Math.max(1, value) ? '' : undefined}
            />
          ))}
        </div>
        <p className="mn-password-strength-meter__label" aria-live="polite">
          {messages.strength(level)}
        </p>
        {showRules && rules.length > 0 ? (
          <ul className="mn-password-strength-meter__rules" aria-label={messages.rulesLabel}>
            {rules.map((rule) => {
              const met = !empty && rule.test(password);
              return (
                <li
                  key={rule.id}
                  className="mn-password-strength-meter__rule"
                  data-met={met || undefined}
                >
                  {met ? (
                    <CheckGlyph className="mn-password-strength-meter__icon" />
                  ) : (
                    <DotGlyph className="mn-password-strength-meter__icon" />
                  )}
                  <span>
                    <VisuallyHidden>{met ? messages.met : messages.unmet} </VisuallyHidden>
                    {rule.label}
                  </span>
                </li>
              );
            })}
          </ul>
        ) : null}
      </div>
    );
  },
);
