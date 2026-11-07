'use client';

import { forwardRef, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { DurationInput, formatDuration } from '../../forms/duration-input/index.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';

/** Someone or something a level can notify: a channel, a person, a rota. */
export interface EscalationTarget {
  value: string;
  label: string;
  /** Short extra text, such as the channel kind. */
  description?: string | undefined;
}

/** One level of the policy. */
export interface EscalationLevel {
  /** Stable, unique id. */
  id: string;
  /** Minutes after the incident or request started (or after the previous level, see `timing`). */
  afterMinutes: number;
  /** The `value`s of the targets notified at this level. */
  targets: readonly string[];
}

/** Why a level cannot be saved. */
export type EscalationLevelProblem = 'targets' | 'order';

/** Labels. Override them to translate. */
export interface EscalationPolicyEditorMessages {
  /** Accessible name of the list of levels. */
  label: string;
  level: (position: number) => string;
  after: string;
  notify: string;
  /** Summary of each level. */
  summary: (duration: string, names: string) => string;
  nobody: string;
  remove: (position: number) => string;
  add: string;
  /** Shown when there is nothing to notify. */
  noTargets: string;
  problems: Record<EscalationLevelProblem, string>;
  maxReached: (max: number) => string;
}

const DEFAULT_MESSAGES: EscalationPolicyEditorMessages = {
  label: 'Escalation levels',
  level: (n) => `Level ${n}`,
  after: 'After',
  notify: 'Notify',
  summary: (d, names) => `After ${d}, notify ${names}`,
  nobody: 'nobody yet',
  remove: (n) => `Remove level ${n}`,
  add: 'Add level',
  noTargets:
    'There is nothing to notify yet. Add a channel or a person first, or this policy cannot reach anyone.',
  problems: {
    targets: 'Choose at least one target. A level that notifies nobody does nothing.',
    order: 'This level must come later than the one before it.',
  },
  maxReached: (max) => `A policy can have up to ${max} levels.`,
};

/**
 * What is wrong with each level: no targets, or not later than the level
 * before (with `cumulative` timing). Keyed by level id; empty when valid.
 */
export function validateEscalationPolicy(
  levels: readonly EscalationLevel[],
  timing: 'cumulative' | 'interval' = 'cumulative',
): Record<string, EscalationLevelProblem> {
  const out: Record<string, EscalationLevelProblem> = {};
  let previous: EscalationLevel | undefined;
  for (const level of levels) {
    if (level.targets.length === 0) out[level.id] = 'targets';
    else if (timing === 'cumulative' && previous && level.afterMinutes <= previous.afterMinutes)
      out[level.id] = 'order';
    previous = level;
  }
  return out;
}

export interface EscalationPolicyEditorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange'
> {
  /** The levels, in order (controlled). */
  value: readonly EscalationLevel[];
  onChange: (levels: EscalationLevel[]) => void;
  /** What a level can notify. */
  targets: readonly EscalationTarget[];
  /**
   * `cumulative` (default): every time is measured from the start, so levels
   * must get later. `interval`: each time is measured from the previous level.
   */
  timing?: 'cumulative' | 'interval' | undefined;
  /** Help under the list, e.g. "Each level fires only if nobody has acknowledged yet." */
  description?: ReactNode;
  /** Minutes added to the last level for a new one. Default 15. */
  defaultStepMinutes?: number | undefined;
  /** Fewest levels. Default 1. */
  minLevels?: number | undefined;
  /** Most levels. Default 10. */
  maxLevels?: number | undefined;
  /** Show problems on each level (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** Makes the id of a new level. Default: a generated id. */
  createId?: (() => string) | undefined;
  disabled?: boolean | undefined;
  /** Override labels. */
  messages?: Partial<EscalationPolicyEditorMessages> | undefined;
}

/**
 * Edits an escalation ladder: at each level, after how long and whom to
 * notify, so an alert or request nobody acts on reaches someone else. The
 * product supplies the targets and runs the escalation.
 */
export const EscalationPolicyEditor = forwardRef<HTMLDivElement, EscalationPolicyEditorProps>(
  function EscalationPolicyEditor(
    {
      value: levels,
      onChange,
      targets,
      timing = 'cumulative',
      description,
      defaultStepMinutes = 15,
      minLevels = 1,
      maxLevels = 10,
      showErrors = false,
      createId,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m: EscalationPolicyEditorMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      problems: { ...DEFAULT_MESSAGES.problems, ...messagesProp?.problems },
    };
    const baseId = useId();
    const counter = useRef(0);
    const problems = showErrors ? validateEscalationPolicy(levels, timing) : {};
    const update = (id: string, patch: Partial<EscalationLevel>) =>
      onChange(levels.map((l) => (l.id === id ? { ...l, ...patch } : l)));
    const add = () => {
      counter.current += 1;
      const last = levels[levels.length - 1];
      onChange([
        ...levels,
        {
          id: createId?.() ?? `${baseId}-l${counter.current}`,
          afterMinutes:
            timing === 'cumulative'
              ? (last?.afterMinutes ?? 0) + defaultStepMinutes
              : defaultStepMinutes,
          targets: [],
        },
      ]);
    };
    const nameOf = (v: string) => targets.find((t) => t.value === v)?.label ?? v;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-escalation-policy-editor', className)}
        data-timing={timing}
        data-disabled={disabled || undefined}
      >
        {targets.length === 0 ? (
          <p className="mn-escalation-policy-editor__warning" role="note">
            <WarningGlyph />
            <span>{m.noTargets}</span>
          </p>
        ) : null}
        <ol className="mn-escalation-policy-editor__levels" aria-label={m.label}>
          {levels.map((level, index) => {
            const n = index + 1;
            const titleId = `${baseId}-${level.id}-title`;
            const errorId = `${baseId}-${level.id}-error`;
            const problem = problems[level.id];
            const names = level.targets.length ? level.targets.map(nameOf).join(', ') : m.nobody;
            return (
              <li
                key={level.id}
                className="mn-escalation-policy-editor__level"
                data-invalid={problem ? '' : undefined}
              >
                <span className="mn-escalation-policy-editor__marker" aria-hidden>
                  {n}
                </span>
                <div
                  role="group"
                  aria-labelledby={titleId}
                  aria-describedby={problem ? errorId : undefined}
                  className="mn-escalation-policy-editor__body"
                >
                  <div className="mn-escalation-policy-editor__head">
                    <span className="mn-escalation-policy-editor__titles">
                      <span id={titleId} className="mn-escalation-policy-editor__title">
                        {m.level(n)}
                      </span>
                      <span className="mn-escalation-policy-editor__summary">
                        {m.summary(formatDuration(level.afterMinutes, { largestUnit: 'd' }), names)}
                      </span>
                    </span>
                    <IconButton
                      size="sm"
                      icon={<CloseGlyph />}
                      label={m.remove(n)}
                      disabled={disabled || levels.length <= minLevels}
                      onClick={() => onChange(levels.filter((l) => l.id !== level.id))}
                    />
                  </div>
                  <Field
                    className="mn-escalation-policy-editor__after"
                    invalid={problem === 'order'}
                    disabled={disabled}
                  >
                    <FieldLabel>{m.after}</FieldLabel>
                    <DurationInput
                      value={level.afterMinutes}
                      min={1}
                      step={5}
                      largestUnit="d"
                      onValueChange={(v) => update(level.id, { afterMinutes: v ?? 1 })}
                    />
                  </Field>
                  <fieldset className="mn-escalation-policy-editor__targets" disabled={disabled}>
                    <legend className="mn-field__label">{m.notify}</legend>
                    <div className="mn-escalation-policy-editor__choices">
                      {targets.map((t) => (
                        <Checkbox
                          key={t.value}
                          label={t.label}
                          description={t.description}
                          checked={level.targets.includes(t.value)}
                          onCheckedChange={(on) =>
                            update(level.id, {
                              targets:
                                on === true
                                  ? targets
                                      .filter(
                                        (x) =>
                                          x.value === t.value || level.targets.includes(x.value),
                                      )
                                      .map((x) => x.value)
                                  : level.targets.filter((x) => x !== t.value),
                            })
                          }
                        />
                      ))}
                    </div>
                  </fieldset>
                  {problem ? (
                    <p id={errorId} className="mn-escalation-policy-editor__error">
                      {m.problems[problem]}
                    </p>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ol>
        {description ? (
          <p className="mn-escalation-policy-editor__description">{description}</p>
        ) : null}
        {levels.length < maxLevels ? (
          <Button
            size="sm"
            variant="secondary"
            leadingIcon={<PlusGlyph />}
            className="mn-escalation-policy-editor__add"
            disabled={disabled}
            onClick={add}
          >
            {m.add}
          </Button>
        ) : (
          <p className="mn-escalation-policy-editor__description">{m.maxReached(maxLevels)}</p>
        )}
      </div>
    );
  },
);
