'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import type { AiFeedback as AiFeedbackValue } from '../ai-message-actions/index.js';

/**
 * `'up' | 'down'`: the same rating AiMessageActions and `AiMessage.feedback`
 * use. Declared here too so the name is both the component and this type.
 */
export type AiFeedback = AiFeedbackValue;

/** A reason chip: `value` is what you store, `label` what people read. */
export interface AiFeedbackReason {
  value: string;
  label: string;
}

/** What `onSubmit` receives. Send it to your feedback API with the message id. */
export interface AiFeedbackSubmission {
  value: AiFeedback;
  /** Values of the chosen reason chips, in the order they are listed. */
  reasons: string[];
  /** The optional comment, trimmed; empty when none. */
  comment: string;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiFeedbackMessages {
  /** Accessible name of the thumbs group. */
  group: string;
  good: string;
  bad: string;
  /** Label of the reason chips after a thumbs down. */
  reasonsDown: string;
  /** Label of the reason chips after a thumbs up. */
  reasonsUp: string;
  /** Label of the comment box. */
  comment: string;
  /** Placeholder of the comment box. */
  commentPlaceholder: string;
  submit: string;
  cancel: string;
  /** Shown after submitting. */
  thanks: string;
  /** Shown when `onSubmit` rejects. */
  error: string;
}

export const AI_FEEDBACK_MESSAGES: AiFeedbackMessages = {
  group: 'Rate this response',
  good: 'Good response',
  bad: 'Bad response',
  reasonsDown: 'What went wrong?',
  reasonsUp: 'What went well?',
  comment: 'Tell us more (optional)',
  commentPlaceholder: 'What would have made it better?',
  submit: 'Send feedback',
  cancel: 'Cancel',
  thanks: 'Thanks for your feedback.',
  error: 'Your feedback could not be sent. Try again.',
};

/** Default reason chips after a thumbs down. */
export const AI_FEEDBACK_DOWN_REASONS: AiFeedbackReason[] = [
  { value: 'inaccurate', label: 'Inaccurate' },
  { value: 'not-helpful', label: 'Not helpful' },
  { value: 'incomplete', label: 'Incomplete' },
  { value: 'hard-to-follow', label: 'Hard to follow' },
  { value: 'unsafe', label: 'Harmful or unsafe' },
];

export interface AiFeedbackProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'onSubmit' | 'children' | 'defaultValue'
> {
  /** The current rating. Leave unset (uncontrolled) or pass `undefined` for none. */
  value?: AiFeedback | undefined;
  /** Called with the new rating, or `undefined` when the pressed thumb is pressed again. */
  onChange?: ((value: AiFeedback | undefined) => void) | undefined;
  /**
   * Reason chips. An array applies to thumbs down; a function returns the
   * chips for each rating. Defaults to `AI_FEEDBACK_DOWN_REASONS` for down
   * and none for up.
   */
  reasons?:
    readonly AiFeedbackReason[] | ((value: AiFeedback) => readonly AiFeedbackReason[]) | undefined;
  /**
   * Which ratings open the follow-up form (reasons and comment). Default
   * `down`: a thumbs up goes straight to the thank-you state.
   */
  askDetails?: 'down' | 'both' | 'none' | undefined;
  /**
   * Called when the form is sent (or straight after a rating that does not
   * ask for details). May return a promise: the form shows a pending state
   * and an error if it rejects.
   */
  onSubmit?: ((submission: AiFeedbackSubmission) => void | Promise<void>) | undefined;
  /** Labels. */
  messages?: Partial<AiFeedbackMessages> | undefined;
}

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

function ThumbUpGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M5 7v6.5H2.75V7H5zM5 7l2.5-4.75c.9 0 1.6.75 1.5 1.65L8.75 6.5h3.6a1.25 1.25 0 0 1 1.22 1.5l-.95 4.5a1.25 1.25 0 0 1-1.22 1H5" />
    </svg>
  );
}

function ThumbDownGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M11 9V2.5h2.25V9H11zM11 9l-2.5 4.75c-.9 0-1.6-.75-1.5-1.65L7.25 9.5h-3.6A1.25 1.25 0 0 1 2.43 8l.95-4.5a1.25 1.25 0 0 1 1.22-1H11" />
    </svg>
  );
}

type Phase = 'idle' | 'details' | 'pending' | 'thanks';

/**
 * Thumbs up and down for an AI answer, followed by reason chips and an
 * optional comment, then a thank-you note. Rating and sending are separate:
 * `onChange` records the thumb at once, `onSubmit` sends the details.
 */
export const AiFeedback = forwardRef<HTMLDivElement, AiFeedbackProps>(
  function AiFeedback(props, ref) {
    const {
      value: valueProp,
      onChange,
      reasons: reasonsProp,
      askDetails = 'down',
      onSubmit,
      messages: messagesProp,
      className,
      ...rest
    } = props;
    const m = { ...AI_FEEDBACK_MESSAGES, ...messagesProp };
    const id = useId();
    // Controlled when `value` is passed at all, even as `undefined` (no rating).
    const controlled = 'value' in props;
    const [valueState, setValueState] = useState<AiFeedback | undefined>(valueProp);
    const value = controlled ? valueProp : valueState;
    const [phase, setPhase] = useState<Phase>('idle');
    const [chosen, setChosen] = useState<string[]>([]);
    const [comment, setComment] = useState('');
    const [failed, setFailed] = useState(false);
    const thanksRef = useRef<HTMLParagraphElement | null>(null);
    const thumbsRef = useRef<HTMLDivElement | null>(null);
    const focusThanks = useRef(false);

    useEffect(() => {
      if (phase === 'thanks' && focusThanks.current) {
        focusThanks.current = false;
        thanksRef.current?.focus();
      }
    }, [phase]);

    const reasonsFor = (v: AiFeedback): readonly AiFeedbackReason[] => {
      if (typeof reasonsProp === 'function') return reasonsProp(v);
      if (reasonsProp) return v === 'down' ? reasonsProp : [];
      return v === 'down' ? AI_FEEDBACK_DOWN_REASONS : [];
    };
    const wantsDetails = (v: AiFeedback) =>
      askDetails === 'both' || (askDetails === 'down' && v === 'down');

    const send = async (submission: AiFeedbackSubmission, fromForm: boolean) => {
      setFailed(false);
      try {
        const result = onSubmit?.(submission);
        if (result && typeof (result as Promise<void>).then === 'function') {
          setPhase('pending');
          await result;
        }
        focusThanks.current = fromForm;
        setPhase('thanks');
      } catch {
        setFailed(true);
        setPhase(fromForm ? 'details' : 'idle');
      }
    };

    const rate = (v: AiFeedback) => {
      const next = value === v ? undefined : v;
      if (!controlled) setValueState(next);
      onChange?.(next);
      setChosen([]);
      setComment('');
      setFailed(false);
      if (next === undefined) setPhase('idle');
      else if (wantsDetails(next)) setPhase('details');
      else void send({ value: next, reasons: [], comment: '' }, false);
    };

    const submit = (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      if (!value || phase === 'pending') return;
      const order = reasonsFor(value).map((r) => r.value);
      void send(
        {
          value,
          reasons: order.filter((r) => chosen.includes(r)),
          comment: comment.trim(),
        },
        true,
      );
    };

    const cancel = () => {
      setPhase('idle');
      setFailed(false);
      thumbsRef.current?.querySelector<HTMLElement>('[aria-pressed="true"]')?.focus();
    };

    const thumb = (v: AiFeedback, label: string, Icon: typeof ThumbUpGlyph) => (
      <button
        type="button"
        className="mn-ai-feedback__thumb"
        aria-label={label}
        aria-pressed={value === v}
        data-pressed={value === v || undefined}
        data-value={v}
        disabled={phase === 'pending'}
        onClick={() => rate(v)}
      >
        <Icon />
      </button>
    );

    const showForm = (phase === 'details' || phase === 'pending') && value !== undefined;
    const reasons = value ? reasonsFor(value) : [];
    const reasonsLabelId = `${id}-reasons`;
    const commentId = `${id}-comment`;
    const errorId = `${id}-error`;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-feedback', className)}
        data-value={value}
        data-phase={phase}
      >
        <div ref={thumbsRef} role="group" aria-label={m.group} className="mn-ai-feedback__thumbs">
          {thumb('up', m.good, ThumbUpGlyph)}
          {thumb('down', m.bad, ThumbDownGlyph)}
        </div>
        {showForm ? (
          <form
            className="mn-ai-feedback__form"
            aria-label={m.group}
            aria-busy={phase === 'pending' || undefined}
            onSubmit={submit}
          >
            {reasons.length > 0 ? (
              <div className="mn-ai-feedback__reasons-field">
                <p id={reasonsLabelId} className="mn-ai-feedback__label">
                  {value === 'up' ? m.reasonsUp : m.reasonsDown}
                </p>
                <div
                  role="group"
                  aria-labelledby={reasonsLabelId}
                  className="mn-ai-feedback__chips"
                >
                  {reasons.map((r) => {
                    const on = chosen.includes(r.value);
                    return (
                      <button
                        key={r.value}
                        type="button"
                        className="mn-ai-feedback__chip"
                        aria-pressed={on}
                        data-pressed={on || undefined}
                        onClick={() =>
                          setChosen((list) =>
                            on ? list.filter((x) => x !== r.value) : [...list, r.value],
                          )
                        }
                      >
                        {on ? <CheckGlyph className="mn-ai-feedback__chip-check" /> : null}
                        {r.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}
            <div className="mn-ai-feedback__comment-field">
              <label htmlFor={commentId} className="mn-ai-feedback__label">
                {m.comment}
              </label>
              <Textarea
                id={commentId}
                rows={2}
                value={comment}
                placeholder={m.commentPlaceholder}
                aria-describedby={failed ? errorId : undefined}
                onChange={(e) => setComment(e.target.value)}
              />
            </div>
            {failed ? (
              <p id={errorId} role="alert" className="mn-ai-feedback__error">
                {m.error}
              </p>
            ) : null}
            <div className="mn-ai-feedback__actions">
              <Button type="submit" size="sm" loading={phase === 'pending'}>
                {m.submit}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                disabled={phase === 'pending'}
                onClick={cancel}
              >
                {m.cancel}
              </Button>
            </div>
          </form>
        ) : null}
        {failed && !showForm ? (
          <p role="alert" className="mn-ai-feedback__error">
            {m.error}
          </p>
        ) : null}
        <p
          ref={thanksRef}
          role="status"
          tabIndex={-1}
          className="mn-ai-feedback__thanks"
          data-visible={phase === 'thanks' || undefined}
        >
          {phase === 'thanks' ? m.thanks : null}
        </p>
      </div>
    );
  },
);
