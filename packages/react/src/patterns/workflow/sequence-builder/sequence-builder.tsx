'use client';

import { forwardRef, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph, CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../../components/actions/button/index.js';
import { EmptyState } from '../../../components/feedback/empty-state/index.js';
import { DurationInput, formatDuration } from '../../../components/forms/duration-input/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '../../../components/forms/form-field/index.js';
import { Input, Textarea } from '../../../components/forms/text-field/index.js';
import { ReorderableList } from '../../../components/interaction/reorderable-list/index.js';

/** A way to reach people, such as email or a chat app. */
export interface SequenceChannel {
  value: string;
  label: string;
  /** Messages on this channel have a subject line (email). */
  hasSubject?: boolean | undefined;
  /** Decorative icon before the channel name. */
  icon?: ReactNode;
  /** Longest message body, in characters. */
  maxLength?: number | undefined;
}

/** A value that can be inserted into messages as `{{token}}`. */
export interface SequenceVariable {
  /** The name between the braces, e.g. `firstName` or `product.name`. */
  token: string;
  /** Visible name, e.g. "First name". */
  label: string;
  /** Example value used by the preview. */
  sample?: string | undefined;
}

/** One step: wait, then send a message on a channel. */
export interface SequenceStep {
  /** Stable, unique id. */
  id: string;
  /** The `value` of a channel. */
  channel: string;
  /** Minutes to wait after the previous step (after the start for the first step). */
  delayMinutes: number;
  subject?: string | undefined;
  body: string;
}

/** Why a step cannot be saved. */
export type SequenceStepProblem = 'subject' | 'body' | 'length' | 'variable';

/** Labels. Override them to translate. */
export interface SequenceBuilderMessages {
  /** Accessible name of the list of steps. */
  label: string;
  step: (position: number, channel: string) => string;
  wait: string;
  waitHelp: string;
  /** Summary of when a step is sent. */
  sendsImmediately: string;
  sendsAfter: (duration: string) => string;
  subject: string;
  body: string;
  insertVariable: string;
  insert: (label: string) => string;
  preview: string;
  remove: (position: number) => string;
  addStep: (channel: string) => string;
  emptyTitle: string;
  emptyDescription: string;
  problems: Record<SequenceStepProblem, string>;
  unknownVariables: (tokens: string) => string;
  maxReached: (max: number) => string;
}

const DEFAULT_MESSAGES: SequenceBuilderMessages = {
  label: 'Sequence steps',
  step: (n, c) => `Step ${n}, ${c}`,
  wait: 'Wait before sending',
  waitHelp: 'For example 2d, 4h or 30m.',
  sendsImmediately: 'Sent straight away',
  sendsAfter: (d) => `Sent ${d} after the previous step`,
  subject: 'Subject',
  body: 'Message',
  insertVariable: 'Insert a variable',
  insert: (l) => `Insert ${l}`,
  preview: 'Preview with sample data',
  remove: (n) => `Remove step ${n}`,
  addStep: (c) => `Add ${c} step`,
  emptyTitle: 'No steps yet',
  emptyDescription: 'Add the first message of the sequence.',
  problems: {
    subject: 'Write a subject line.',
    body: 'Write the message.',
    length: 'The message is too long for this channel.',
    variable: 'Remove or fix the unknown variables.',
  },
  unknownVariables: (t) => `Unknown variables: ${t}`,
  maxReached: (max) => `A sequence can have up to ${max} steps.`,
};

const TOKEN = /\{\{\s*([\w.]+)\s*\}\}/g;

/** The variable names used in a template, in order of first use. */
export function findTemplateVariables(text: string): string[] {
  const out: string[] = [];
  for (const match of text.matchAll(TOKEN)) {
    const token = match[1];
    if (token && !out.includes(token)) out.push(token);
  }
  return out;
}

/** Replaces `{{token}}` with values; unknown tokens are left as written. */
export function renderSequenceTemplate(
  text: string,
  values: Readonly<Record<string, string | undefined>>,
): string {
  return text.replace(TOKEN, (whole, token: string) => values[token] ?? whole);
}

/** What is wrong with each step, keyed by step id. Empty when the sequence can be saved. */
export function validateSequence(
  steps: readonly SequenceStep[],
  channels: readonly SequenceChannel[],
  variables: readonly SequenceVariable[] = [],
): Record<string, SequenceStepProblem> {
  const known = new Set(variables.map((v) => v.token));
  const out: Record<string, SequenceStepProblem> = {};
  for (const step of steps) {
    const channel = channels.find((c) => c.value === step.channel);
    const used = [
      ...findTemplateVariables(step.subject ?? ''),
      ...findTemplateVariables(step.body),
    ];
    if (channel?.hasSubject && !step.subject?.trim()) out[step.id] = 'subject';
    else if (!step.body.trim()) out[step.id] = 'body';
    else if (channel?.maxLength !== undefined && step.body.length > channel.maxLength)
      out[step.id] = 'length';
    else if (variables.length > 0 && used.some((t) => !known.has(t))) out[step.id] = 'variable';
  }
  return out;
}

export interface SequenceBuilderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The steps, in order (controlled). */
  value: readonly SequenceStep[];
  onChange: (steps: SequenceStep[]) => void;
  /** Channels steps can use; each gets an "Add … step" button. */
  channels: readonly SequenceChannel[];
  /** Values people can insert into subjects and messages. */
  variables?: readonly SequenceVariable[] | undefined;
  /** Delay of a new step, in minutes. Default 1440 (one day). */
  defaultDelayMinutes?: number | undefined;
  /** Most steps. Default 20. */
  maxSteps?: number | undefined;
  /** Show what is missing on each step (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** Makes the id of a new step. Default: a generated id. */
  createId?: (() => string) | undefined;
  disabled?: boolean | undefined;
  /** Override labels. */
  messages?: Partial<SequenceBuilderMessages> | undefined;
}

/**
 * Builds a multi-step outreach sequence: each step waits a duration, then
 * sends a message on a channel (with a subject where the channel has one).
 * Variables are inserted as `{{token}}`, checked, and previewed with sample
 * values. The product sends the messages.
 */
export const SequenceBuilder = forwardRef<HTMLDivElement, SequenceBuilderProps>(
  function SequenceBuilder(
    {
      value: steps,
      onChange,
      channels,
      variables = [],
      defaultDelayMinutes = 1440,
      maxSteps = 20,
      showErrors = false,
      createId,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m: SequenceBuilderMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      problems: { ...DEFAULT_MESSAGES.problems, ...messagesProp?.problems },
    };
    const baseId = useId();
    const counter = useRef(0);
    // The text field each step last focused, so variables go where people are typing.
    const lastFocus = useRef(new Map<string, HTMLInputElement | HTMLTextAreaElement>());
    const problems = showErrors ? validateSequence(steps, channels, variables) : {};
    const known = new Set(variables.map((v) => v.token));
    const samples = Object.fromEntries(variables.map((v) => [v.token, v.sample ?? v.label]));
    const channelOf = (v: string) => channels.find((c) => c.value === v);
    const position = (s: SequenceStep) => steps.findIndex((x) => x.id === s.id) + 1;
    const update = (id: string, patch: Partial<SequenceStep>) =>
      onChange(steps.map((s) => (s.id === id ? { ...s, ...patch } : s)));

    const add = (channel: SequenceChannel) => {
      counter.current += 1;
      const step: SequenceStep = {
        id: createId?.() ?? `${baseId}-s${counter.current}`,
        channel: channel.value,
        delayMinutes: steps.length === 0 ? 0 : defaultDelayMinutes,
        body: '',
      };
      if (channel.hasSubject) step.subject = '';
      onChange([...steps, step]);
    };

    const insert = (step: SequenceStep, token: string) => {
      const text = `{{${token}}}`;
      const target = lastFocus.current.get(step.id);
      const key = target?.dataset.part === 'subject' ? 'subject' : 'body';
      const current = (key === 'subject' ? step.subject : step.body) ?? '';
      const start = target?.selectionStart ?? current.length;
      const end = target?.selectionEnd ?? current.length;
      update(step.id, { [key]: current.slice(0, start) + text + current.slice(end) });
      const caret = start + text.length;
      requestAnimationFrame(() => {
        if (!target?.isConnected) return;
        target.focus();
        target.setSelectionRange(caret, caret);
      });
    };

    const remember =
      (id: string) => (e: { currentTarget: HTMLInputElement | HTMLTextAreaElement }) => {
        lastFocus.current.set(id, e.currentTarget);
      };

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-sequence-builder', className)}
        data-disabled={disabled || undefined}
      >
        {steps.length === 0 ? (
          <EmptyState
            size="compact"
            headingLevel="p"
            icon={<ClockGlyph />}
            title={m.emptyTitle}
            description={m.emptyDescription}
          />
        ) : (
          <ReorderableList
            className="mn-sequence-builder__steps"
            aria-label={m.label}
            items={steps}
            getKey={(s) => s.id}
            getLabel={(s) => m.step(position(s), channelOf(s.channel)?.label ?? s.channel)}
            disabled={disabled}
            moveButtons
            onReorder={(next) => onChange(next)}
            renderItem={(step, { index }) => {
              const n = index + 1;
              const channel = channelOf(step.channel);
              const name = channel?.label ?? step.channel;
              const titleId = `${baseId}-${step.id}-title`;
              const problem = problems[step.id];
              const used = [
                ...findTemplateVariables(step.subject ?? ''),
                ...findTemplateVariables(step.body),
              ];
              const unknown = variables.length ? used.filter((t) => !known.has(t)) : [];
              return (
                <div
                  role="group"
                  aria-labelledby={titleId}
                  className="mn-sequence-builder__step"
                  data-channel={step.channel}
                  data-invalid={problem ? '' : undefined}
                >
                  <div className="mn-sequence-builder__step-head">
                    {channel?.icon ? (
                      <span className="mn-sequence-builder__icon" aria-hidden>
                        {channel.icon}
                      </span>
                    ) : null}
                    <span className="mn-sequence-builder__titles">
                      <span id={titleId} className="mn-sequence-builder__title">
                        {m.step(n, name)}
                      </span>
                      <span className="mn-sequence-builder__when">
                        {step.delayMinutes > 0
                          ? m.sendsAfter(formatDuration(step.delayMinutes, { largestUnit: 'd' }))
                          : m.sendsImmediately}
                      </span>
                    </span>
                    <IconButton
                      size="sm"
                      icon={<CloseGlyph />}
                      label={m.remove(n)}
                      disabled={disabled}
                      onClick={() => onChange(steps.filter((s) => s.id !== step.id))}
                    />
                  </div>
                  <Field className="mn-sequence-builder__wait" disabled={disabled}>
                    <FieldLabel>{m.wait}</FieldLabel>
                    <DurationInput
                      value={step.delayMinutes}
                      largestUnit="d"
                      step={60}
                      onValueChange={(v) => update(step.id, { delayMinutes: v ?? 0 })}
                    />
                    <FieldDescription>{m.waitHelp}</FieldDescription>
                  </Field>
                  {channel?.hasSubject ? (
                    <Field invalid={problem === 'subject'} required disabled={disabled}>
                      <FieldLabel>{m.subject}</FieldLabel>
                      <Input
                        data-part="subject"
                        value={step.subject ?? ''}
                        maxLength={200}
                        onFocus={remember(step.id)}
                        onSelect={remember(step.id)}
                        onChange={(e) => update(step.id, { subject: e.target.value })}
                      />
                      <FieldError>{problem === 'subject' ? m.problems.subject : null}</FieldError>
                    </Field>
                  ) : null}
                  <Field
                    invalid={problem === 'body' || problem === 'length' || problem === 'variable'}
                    required
                    disabled={disabled}
                  >
                    <FieldLabel>{m.body}</FieldLabel>
                    <Textarea
                      data-part="body"
                      rows={5}
                      value={step.body}
                      onFocus={remember(step.id)}
                      onSelect={remember(step.id)}
                      onChange={(e) => update(step.id, { body: e.target.value })}
                    />
                    {channel?.maxLength !== undefined ? (
                      <FieldDescription>{`${step.body.length} / ${channel.maxLength}`}</FieldDescription>
                    ) : null}
                    <FieldError>
                      {problem && problem !== 'subject' ? m.problems[problem] : null}
                    </FieldError>
                  </Field>
                  {unknown.length > 0 ? (
                    <p className="mn-sequence-builder__warning">
                      {m.unknownVariables(unknown.join(', '))}
                    </p>
                  ) : null}
                  {variables.length > 0 ? (
                    <div
                      role="group"
                      aria-label={m.insertVariable}
                      className="mn-sequence-builder__variables"
                    >
                      <span className="mn-sequence-builder__variables-label" aria-hidden>
                        {m.insertVariable}
                      </span>
                      {variables.map((v) => (
                        <Button
                          key={v.token}
                          size="xs"
                          variant="secondary"
                          disabled={disabled}
                          aria-label={m.insert(v.label)}
                          title={`{{${v.token}}}`}
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => insert(step, v.token)}
                        >
                          {v.label}
                        </Button>
                      ))}
                    </div>
                  ) : null}
                  {step.body.trim() ? (
                    <details className="mn-sequence-builder__preview">
                      <summary>{m.preview}</summary>
                      {channel?.hasSubject && step.subject ? (
                        <p className="mn-sequence-builder__preview-subject">
                          {renderSequenceTemplate(step.subject, samples)}
                        </p>
                      ) : null}
                      <p className="mn-sequence-builder__preview-body">
                        {renderSequenceTemplate(step.body, samples)}
                      </p>
                    </details>
                  ) : null}
                </div>
              );
            }}
          />
        )}
        {steps.length < maxSteps ? (
          <div className="mn-sequence-builder__add">
            {channels.map((c) => (
              <Button
                key={c.value}
                size="sm"
                variant="secondary"
                leadingIcon={<PlusGlyph />}
                disabled={disabled}
                onClick={() => add(c)}
              >
                {m.addStep(c.label)}
              </Button>
            ))}
          </div>
        ) : (
          <p className="mn-sequence-builder__note">{m.maxReached(maxSteps)}</p>
        )}
      </div>
    );
  },
);
