'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  type ForwardedRef,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { AiConfidenceMeter, type AiConfidenceThresholds } from '../ai-confidence-meter/index.js';
import { AiGeneratedBadge } from '../ai-generated-badge/index.js';

const useIsomorphicLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/**
 * - `suggested`: AI filled the field and nobody has checked it yet.
 * - `accepted`: the person accepted the AI value.
 * - `edited`: the person changed the AI value.
 * - `cleared`: the person removed the AI value.
 */
export type AiAutofillState = 'suggested' | 'accepted' | 'edited' | 'cleared';

/** Every string AiAutofillField shows or announces. English defaults. */
export interface AiAutofillFieldMessages {
  /** Badge per state (`cleared` shows no badge). */
  badge: Record<Exclude<AiAutofillState, 'cleared'>, string>;
  accept: string;
  clear: string;
  /** Accessible names of the actions when `label` is given. */
  acceptField: (label: string) => string;
  clearField: (label: string) => string;
  /** Prefix for `source`, e.g. "From invoice.pdf". */
  source: string;
  /** Shown once edited, before the original AI value. */
  original: string;
}

const DEFAULT_MESSAGES: AiAutofillFieldMessages = {
  badge: { suggested: 'AI suggested', accepted: 'AI filled', edited: 'Edited by you' },
  accept: 'Accept',
  clear: 'Clear',
  acceptField: (label) => `Accept suggested ${label}`,
  clearField: (label) => `Clear suggested ${label}`,
  source: 'From',
  original: 'AI suggested:',
};

/** Props handed to a render-function child, to spread on the form control. */
export interface AiAutofillControlProps {
  /** Links the control to the AI status line ("AI suggested", confidence, source). Absent once cleared. */
  'aria-describedby'?: string;
}

export interface AiAutofillFieldProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onInput'
> {
  /**
   * The form control, usually a `Field` with a label and an input. A function
   * receives `aria-describedby` for the control; with plain children the id is
   * added to the first input, textarea or select inside.
   */
  children: ReactNode | ((control: AiAutofillControlProps) => ReactNode);
  /** The value the AI suggested. Shown as "AI suggested: …" once the person edits it. */
  value: ReactNode;
  /** How sure the AI is, 0–1. Shown as a confidence badge while suggested; low reads "Needs check". */
  confidence?: number | undefined;
  /** Band edges for `confidence`. Default `{ low: 0.6, high: 0.85 }`. */
  confidenceThresholds?: Partial<AiConfidenceThresholds> | undefined;
  /** Why the AI chose this value, shown with the confidence badge. */
  confidenceExplanation?: ReactNode | undefined;
  /** Where the value came from, e.g. a document name. Shown as "From …". */
  source?: ReactNode | undefined;
  /** Where the field stands. Drive it from your form state. */
  state: AiAutofillState;
  /** The person accepted the suggestion. Set `state` to `accepted`. */
  onAccept: () => void;
  /** The person cleared the suggestion: empty the control and set `state` to `cleared`. */
  onClear: () => void;
  /** Called on the first change the person makes to a suggested or accepted value. Set `state` to `edited`. */
  onEdit?: (() => void) | undefined;
  /** The field's name, for unambiguous action names ("Accept suggested Company"). */
  label?: string | undefined;
  /** Model and time for the provenance badge's tooltip. */
  model?: string | undefined;
  at?: string | number | Date | undefined;
  /** Strings, for localisation. `badge` merges per state. */
  messages?:
    | (Partial<Omit<AiAutofillFieldMessages, 'badge'>> & {
        badge?: Partial<AiAutofillFieldMessages['badge']> | undefined;
      })
    | undefined;
}

const CONTROL =
  'input:not([type="hidden"]), textarea, select, [role="combobox"], [contenteditable="true"]';

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

/**
 * Wraps a form field that AI filled in: marks it with the AI badge and a
 * confidence badge (low reads "Needs check"), says where the value came from,
 * and offers Accept and Clear. When the person types, it becomes "Edited by
 * you" and shows what the AI originally suggested. The status line is linked
 * to the control with `aria-describedby`.
 */
export const AiAutofillField = forwardRef<HTMLDivElement, AiAutofillFieldProps>(
  function AiAutofillField(
    {
      children,
      value,
      confidence,
      confidenceThresholds,
      confidenceExplanation,
      source,
      state,
      onAccept,
      onClear,
      onEdit,
      label,
      model,
      at,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      badge: { ...DEFAULT_MESSAGES.badge, ...messagesProp?.badge },
    };
    const statusId = `mn-ai-autofill-${useId().replace(/:/g, '')}`;
    const rootRef = useRef<HTMLDivElement | null>(null);
    const showStatus = state !== 'cleared';

    // Plain children: link the first control to the status line.
    useIsomorphicLayoutEffect(() => {
      if (typeof children === 'function') return;
      const control = rootRef.current?.querySelector<HTMLElement>(CONTROL);
      if (!control) return;
      const ids = (control.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean);
      const has = ids.includes(statusId);
      if (showStatus && !has)
        control.setAttribute('aria-describedby', [...ids, statusId].join(' '));
      if (!showStatus && has) {
        const rest = ids.filter((id) => id !== statusId);
        if (rest.length) control.setAttribute('aria-describedby', rest.join(' '));
        else control.removeAttribute('aria-describedby');
      }
    });

    const focusControl = () => rootRef.current?.querySelector<HTMLElement>(CONTROL)?.focus();

    const handleInput = (e: FormEvent<HTMLDivElement>) => {
      if (!(e.target instanceof Element) || e.target.closest('.mn-ai-autofill-field__status'))
        return;
      if (state === 'suggested' || state === 'accepted') onEdit?.();
    };

    const control =
      typeof children === 'function'
        ? children(showStatus ? { 'aria-describedby': statusId } : {})
        : children;

    return (
      <div
        ref={(node) => {
          rootRef.current = node;
          setRef(ref, node);
        }}
        {...rest}
        className={cx('mn-ai-autofill-field', className)}
        data-state={state}
        // `input` bubbles from any control inside, native or custom.
        onInput={handleInput}
      >
        <div className="mn-ai-autofill-field__control">{control}</div>
        {showStatus ? (
          <div className="mn-ai-autofill-field__status">
            <div id={statusId} className="mn-ai-autofill-field__info">
              <AiGeneratedBadge
                size="sm"
                kind={state === 'edited' ? 'edited' : 'generated'}
                label={m.badge[state]}
                model={model}
                at={at}
                // The status line is read as the control's description; the
                // tooltip stays reachable by hover.
                tabIndex={-1}
              />
              {state === 'suggested' && confidence !== undefined ? (
                <AiConfidenceMeter
                  size="sm"
                  value={confidence}
                  thresholds={confidenceThresholds}
                  explanation={confidenceExplanation}
                />
              ) : null}
              {source && state !== 'edited' ? (
                <span className="mn-ai-autofill-field__source">
                  {m.source} {source}
                </span>
              ) : null}
              {state === 'edited' ? (
                <span className="mn-ai-autofill-field__original">
                  {m.original} <span className="mn-ai-autofill-field__value">{value}</span>
                </span>
              ) : null}
            </div>
            {state === 'suggested' ? (
              <div className="mn-ai-autofill-field__actions">
                <Button
                  size="xs"
                  variant="ghost"
                  leadingIcon={<CheckGlyph />}
                  aria-label={label ? m.acceptField(label) : undefined}
                  onClick={() => {
                    onAccept();
                    focusControl();
                  }}
                >
                  {m.accept}
                </Button>
                <Button
                  size="xs"
                  variant="ghost"
                  leadingIcon={<CloseGlyph />}
                  aria-label={label ? m.clearField(label) : undefined}
                  onClick={() => {
                    onClear();
                    focusControl();
                  }}
                >
                  {m.clear}
                </Button>
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  },
);
