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
import { Button } from '../../actions/button/index.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Textarea } from '../../forms/text-field/index.js';

/** One choice in a decision, such as Keep, Review or Let lapse. */
export interface DecisionOption {
  /** Stored value, passed to `onDecide`. */
  value: string;
  /** Short label on the choice and on the read-only badge. */
  label: string;
  /** One line under the label. */
  description?: string | undefined;
  /** Shown under the choices while this one is selected: what choosing it will do. */
  note?: ReactNode | undefined;
  /** Badge colour in the read-only view. Default `neutral`. */
  tone?: Tone | undefined;
  /** This choice needs a reason. Overrides `reasonRequired`. */
  reasonRequired?: boolean | undefined;
  /** Cannot be chosen. */
  disabled?: boolean | undefined;
}

/** What `onDecide` receives. */
export interface DecisionResult {
  /** The chosen value, or null for "no decision". */
  value: string | null;
  /** The trimmed reason, or null when empty. */
  reason: string | null;
}

/** Every string the decision card shows or announces. */
export interface DecisionCardMessages {
  /** Name of the group of choices. */
  choices: string;
  /** Label of the "no decision" choice (with `allowNone`). */
  none: string;
  /** Reason field label. */
  reason: string;
  /** Shown after the reason label when it is optional. */
  optional: string;
  /** Reason placeholder. */
  reasonPlaceholder: string;
  /** Shown when a required reason is missing. */
  reasonRequired: string;
  /** Characters left, shown near the limit. */
  charactersLeft: (left: number) => string;
  /** Save button. */
  save: string;
  /** Announced after saving. */
  saved: string;
  /** Before the time of the current decision. */
  decided: string;
  /** Before the person who decided. */
  decidedBy: (name: string) => string;
  /** Read-only view when nothing has been decided. */
  undecided: string;
  /** Fallback error when `onDecide` fails without a message. */
  failed: string;
}

export const DECISION_CARD_MESSAGES: DecisionCardMessages = {
  choices: 'Decision',
  none: 'No decision',
  reason: 'Reason',
  optional: 'optional',
  reasonPlaceholder: 'Why? Others will see this.',
  reasonRequired: 'Add a reason for this decision.',
  charactersLeft: (n) => (n === 1 ? '1 character left' : `${n} characters left`),
  save: 'Save decision',
  saved: 'Decision saved.',
  decided: 'Decided',
  decidedBy: (name) => `by ${name}`,
  undecided: 'No decision yet',
  failed: 'That did not work. Try again.',
};

const NONE = '__none__';

export interface DecisionCardProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'title' | 'onSubmit' | 'children'
> {
  /** Heading of the card, e.g. "Renewal decision". */
  title?: ReactNode | undefined;
  /** Heading level of the title. Default 3. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** The choices, in order. Two to five work best. */
  options: readonly DecisionOption[];
  /** The saved decision, or null for none. */
  value: string | null;
  /** The saved reason. */
  reason?: string | null | undefined;
  /** When the saved decision was made. */
  decidedAt?: DateInput | null | undefined;
  /** Who made the saved decision. */
  decidedBy?: string | null | undefined;
  /** Offer a "No decision" choice that clears it. Default true. */
  allowNone?: boolean | undefined;
  /** Every choice needs a reason. Default false; `DecisionOption.reasonRequired` overrides it per choice. */
  reasonRequired?: boolean | undefined;
  /** Longest reason accepted. Default 500. */
  reasonMaxLength?: number | undefined;
  /** Evidence for the decision, shown above the choices: usage, risk, cost. */
  context?: ReactNode | undefined;
  /** Show the decision as a badge and reason without the form, for people who cannot change it. */
  readOnly?: boolean | undefined;
  /**
   * Saves the decision. Return a promise to show the button busy; reject to
   * keep the choice and show the error. Not called when nothing changed.
   */
  onDecide?: ((result: DecisionResult) => unknown) | undefined;
  /** "Now" for the relative decision time, for tests and server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for the decision time. */
  locale?: string | undefined;
  /** Override any string. */
  messages?: Partial<DecisionCardMessages> | undefined;
}

/**
 * Records a decision about an item (keep, review, let lapse; approve, defer,
 * reject) with a reason, next to the evidence for it. Shows what the selected
 * choice will do before saving, who decided and when, and a read-only view
 * for people who cannot change it.
 */
export const DecisionCard = forwardRef<HTMLElement, DecisionCardProps>(function DecisionCard(
  {
    title,
    headingLevel = 3,
    options,
    value,
    reason,
    decidedAt,
    decidedBy,
    allowNone = true,
    reasonRequired = false,
    reasonMaxLength = 500,
    context,
    readOnly = false,
    onDecide,
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DECISION_CARD_MESSAGES, ...messagesProp };
  const uid = useId().replace(/:/g, '');
  const Heading = `h${headingLevel}` as 'h3';
  const saved = { choice: value ?? NONE, reason: reason ?? '' };
  const [choice, setChoice] = useState(saved.choice);
  const [text, setText] = useState(saved.reason);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showMissing, setShowMissing] = useState(false);
  const [status, setStatus] = useState('');
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // A new saved decision (from the product) resets the form.
  useEffect(() => {
    setChoice(value ?? NONE);
    setText(reason ?? '');
    setShowMissing(false);
  }, [value, reason]);

  const selected = options.find((o) => o.value === choice);
  const needsReason = choice !== NONE && (selected?.reasonRequired ?? reasonRequired);
  const trimmed = text.trim();
  const missing = needsReason && !trimmed;
  const dirty = choice !== saved.choice || trimmed !== saved.reason.trim();
  const savedOption = options.find((o) => o.value === value);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending || !dirty || !onDecide) return;
    if (missing) {
      setShowMissing(true);
      return;
    }
    setError(null);
    setStatus('');
    setPending(true);
    try {
      await onDecide({ value: choice === NONE ? null : choice, reason: trimmed || null });
      if (mounted.current) setStatus(m.saved);
    } catch (e) {
      if (mounted.current) setError(e instanceof Error && e.message ? e.message : m.failed);
    } finally {
      if (mounted.current) setPending(false);
    }
  };

  const meta =
    decidedAt || decidedBy ? (
      <p className="mn-decision-card__meta">
        {decidedAt ? (
          <>
            {m.decided}{' '}
            <RelativeTime
              date={decidedAt}
              {...(now !== undefined ? { now } : {})}
              {...(locale !== undefined ? { locale } : {})}
            />
          </>
        ) : null}
        {decidedBy ? <> {m.decidedBy(decidedBy)}</> : null}
      </p>
    ) : null;

  const choices: {
    value: string;
    label: string;
    description?: string | undefined;
    disabled?: boolean | undefined;
  }[] = [...options, ...(allowNone ? [{ value: NONE, label: m.none }] : [])];
  const reasonId = `${uid}-reason`;

  return (
    <section
      ref={ref}
      {...rest}
      aria-labelledby={title ? `${uid}-title` : rest['aria-labelledby']}
      className={cx('mn-decision-card', className)}
      data-readonly={readOnly || undefined}
      data-decision={value ?? undefined}
    >
      {title ? (
        <Heading id={`${uid}-title`} className="mn-decision-card__title">
          {title}
        </Heading>
      ) : null}
      {context ? <div className="mn-decision-card__context">{context}</div> : null}

      {readOnly || !onDecide ? (
        <div className="mn-decision-card__readonly">
          <p className="mn-decision-card__current">
            {savedOption ? (
              <Badge tone={savedOption.tone ?? 'neutral'}>{savedOption.label}</Badge>
            ) : (
              <span className="mn-decision-card__undecided">{m.undecided}</span>
            )}
            {reason ? <span className="mn-decision-card__reason">{reason}</span> : null}
          </p>
          {meta}
        </div>
      ) : (
        <form className="mn-decision-card__form" onSubmit={submit} noValidate>
          <fieldset className="mn-decision-card__fieldset" disabled={pending}>
            <legend className="mn-decision-card__legend">{m.choices}</legend>
            <div className="mn-decision-card__choices" data-count={choices.length}>
              {choices.map((option) => {
                const id = `${uid}-opt-${option.value.replace(/[^\w-]/g, '_')}`;
                const checked = choice === option.value;
                return (
                  <label
                    key={option.value}
                    htmlFor={id}
                    className="mn-decision-card__choice"
                    data-checked={checked || undefined}
                    data-disabled={option.disabled || undefined}
                    data-none={option.value === NONE || undefined}
                  >
                    <input
                      id={id}
                      type="radio"
                      name={`${uid}-decision`}
                      value={option.value}
                      checked={checked}
                      disabled={option.disabled}
                      aria-labelledby={`${id}-label`}
                      aria-describedby={option.description ? `${id}-desc` : undefined}
                      onChange={() => {
                        setChoice(option.value);
                        setShowMissing(false);
                        setStatus('');
                      }}
                      className="mn-decision-card__radio"
                    />
                    <span id={`${id}-label`} className="mn-decision-card__choice-label">
                      {option.label}
                    </span>
                    {option.description ? (
                      <span id={`${id}-desc`} className="mn-decision-card__choice-description">
                        {option.description}
                      </span>
                    ) : null}
                  </label>
                );
              })}
            </div>
          </fieldset>

          {selected?.note ? (
            <p className="mn-decision-card__note" aria-live="polite">
              {selected.note}
            </p>
          ) : null}

          <Field invalid={showMissing && missing} required={needsReason} id={reasonId}>
            <FieldLabel {...(needsReason ? {} : { optionalText: m.optional })}>
              {m.reason}
            </FieldLabel>
            <Textarea
              rows={2}
              value={text}
              maxLength={reasonMaxLength}
              placeholder={m.reasonPlaceholder}
              disabled={pending}
              onChange={(e) => {
                setText(e.target.value);
                setStatus('');
              }}
            />
            {reasonMaxLength - text.length <= 50 ? (
              <FieldDescription>{m.charactersLeft(reasonMaxLength - text.length)}</FieldDescription>
            ) : null}
            {showMissing && missing ? <FieldError>{m.reasonRequired}</FieldError> : null}
          </Field>

          {error ? (
            <p className="mn-decision-card__error" role="alert">
              {error}
            </p>
          ) : null}

          <div className="mn-decision-card__footer">
            {meta ?? <span />}
            <Button type="submit" size="sm" variant="primary" loading={pending} disabled={!dirty}>
              {m.save}
            </Button>
          </div>
        </form>
      )}
      <p role="status" className="mn-visually-hidden">
        {status}
      </p>
    </section>
  );
});
