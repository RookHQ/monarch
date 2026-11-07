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
import { Button } from '../../../components/actions/button/index.js';
import { Field, FieldDescription, FieldLabel } from '../../../components/forms/form-field/index.js';
import { Textarea } from '../../../components/forms/text-field/index.js';
import { RatingPicker } from '../../../components/forms/rating-picker/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { LoadingState } from '../../../components/feedback/loading-state/index.js';
import { cx } from '../../../internal/cx.js';

/** `good` / `bad` on the two-choice scale, or 1–5 on the five-point scale. */
export type CSATRating = 'good' | 'bad' | number;

export interface CSATSurveyMessages {
  /** The question, the survey's heading. */
  question: string;
  /** Name of the rating group. */
  ratingLabel: string;
  good: string;
  bad: string;
  /** Labels for 1–5 on the five-point scale, lowest first. */
  scaleLabels: [string, string, string, string, string];
  commentLabel: string;
  /** After the comment label. */
  optional: string;
  /** Character count under the comment. */
  remaining: (left: number) => string;
  /** `instant` mode: saves the comment. */
  saveComment: string;
  /** `form` mode: sends rating and comment together. */
  submit: string;
  /** Before a rating is chosen. */
  chooseFirst: string;
  savingRating: string;
  ratingSaved: string;
  commentSaved: string;
  /** Shown when `onRate` or `onComment` rejects without a message. */
  saveFailed: string;
  /** `locked`: the answer can no longer be changed. */
  locked: string;
  /** `submitted`: the thank-you message. */
  thanks: string;
  loading: string;
  /** Heading over `error`. */
  loadFailed: string;
}

const DEFAULT_MESSAGES: CSATSurveyMessages = {
  question: 'How would you rate the support you received?',
  ratingLabel: 'Your rating',
  good: 'Good',
  bad: 'Bad',
  scaleLabels: ['Very poor', 'Poor', 'Okay', 'Good', 'Excellent'],
  commentLabel: 'Anything you’d like to add?',
  optional: '(optional)',
  remaining: (n) => `${n} characters left`,
  saveComment: 'Save comment',
  submit: 'Send feedback',
  chooseFirst: 'Choose an answer above. You can add a comment afterwards.',
  savingRating: 'Saving your answer…',
  ratingSaved: 'Thanks, your answer was recorded.',
  commentSaved: 'Thanks, your comment was saved.',
  saveFailed: 'That didn’t save. Try again.',
  locked: 'Thanks for your feedback. This answer can no longer be changed.',
  thanks: 'Thanks for your feedback.',
  loading: 'Loading survey',
  loadFailed: 'We couldn’t load this survey',
};

export interface CSATSurveyProps extends Omit<HTMLAttributes<HTMLElement>, 'onSubmit'> {
  /** `good-bad` (default): two large buttons. `five`: a 1–5 scale. */
  scale?: 'good-bad' | 'five' | undefined;
  /**
   * `instant` (default): choosing a rating saves it at once (a one-click
   * email link can do the same), and the comment is saved separately.
   * `form`: rating and comment are sent together with one button.
   */
  mode?: 'instant' | 'form' | undefined;
  /** The saved rating (controlled). Update it when `onRate` succeeds. */
  rating?: CSATRating | null | undefined;
  /** The saved comment, prefilled in the comment box. */
  comment?: string | null | undefined;
  /**
   * Save a rating (`instant` mode) or rating and comment (`form` mode). Return
   * a promise to show progress; a rejection shows `saveFailed` (or its message
   * when it is an Error with one).
   */
  onRate?: ((rating: CSATRating, comment: string) => unknown) | undefined;
  /** `instant` mode: save the comment for the current rating. Same promise rules. */
  onComment?: ((comment: string, rating: CSATRating) => unknown) | undefined;
  /** What was rated, above the question ("Request #1042 · Billing question"). */
  context?: ReactNode;
  /** The answer can no longer be changed: choices are disabled and a note says so. */
  locked?: boolean | undefined;
  /** Show only the thank-you message (after a `form` submission, or a rating already given). */
  submitted?: boolean | undefined;
  /** Loading the survey. */
  loading?: boolean | undefined;
  /** The survey could not be loaded (invalid or expired link): replaces the survey. */
  error?: ReactNode;
  /** Longest comment. Default 2000. */
  commentMaxLength?: number | undefined;
  /** Heading element for the question. Default `h1` (a survey page); `h2` inside a widget. */
  headingAs?: 'h1' | 'h2' | 'h3' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<CSATSurveyMessages> | undefined;
}

function isThenable(v: unknown): v is PromiseLike<unknown> {
  return !!v && typeof (v as { then?: unknown }).then === 'function';
}

function ThumbGlyph({ down }: { down?: boolean }) {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable="false"
      style={down ? { transform: 'rotate(180deg)' } : undefined}
    >
      <path d="M7 10v11H4a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1h3Z" />
      <path d="M7 10l4-7a2.5 2.5 0 0 1 2.5 2.5V9h5.2a2 2 0 0 1 2 2.3l-1.3 8a2 2 0 0 1-2 1.7H7" />
    </svg>
  );
}

/**
 * A customer satisfaction survey: the question, a Good / Bad (or 1–5) choice
 * and an optional comment, with saving, saved, error, locked and thank-you
 * states. Rating can save at once (one click from an email) or with the
 * comment in one submit. The app stores the answer; the survey only reports it.
 */
export const CSATSurvey = forwardRef<HTMLElement, CSATSurveyProps>(function CSATSurvey(
  {
    scale = 'good-bad',
    mode = 'instant',
    rating: ratingProp,
    comment: commentProp,
    onRate,
    onComment,
    context,
    locked = false,
    submitted = false,
    loading = false,
    error,
    commentMaxLength = 2000,
    headingAs: Heading = 'h1',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const titleId = `${id}-title`;
  const [draft, setDraft] = useState<CSATRating | null>(ratingProp ?? null);
  const [comment, setComment] = useState(commentProp ?? '');
  const [saving, setSaving] = useState<null | 'rating' | 'comment'>(null);
  const [status, setStatus] = useState<{ tone: 'success' | 'danger'; text: string } | null>(null);
  const [done, setDone] = useState(false);
  const alive = useRef(true);
  useEffect(
    () => () => {
      alive.current = false;
    },
    [],
  );
  // Follow the saved values when the app updates them.
  const [seen, setSeen] = useState({ rating: ratingProp, comment: commentProp });
  if (seen.rating !== ratingProp || seen.comment !== commentProp) {
    setSeen({ rating: ratingProp, comment: commentProp });
    if (seen.rating !== ratingProp) setDraft(ratingProp ?? null);
    if (seen.comment !== commentProp) setComment(commentProp ?? '');
  }

  const rating = draft;
  const run = (
    what: 'rating' | 'comment',
    result: unknown,
    okText: string,
    after?: () => void,
    onFail?: () => void,
  ) => {
    if (!isThenable(result)) {
      setStatus({ tone: 'success', text: okText });
      after?.();
      return;
    }
    setSaving(what);
    setStatus(null);
    Promise.resolve(result).then(
      () => {
        if (!alive.current) return;
        setSaving(null);
        setStatus({ tone: 'success', text: okText });
        after?.();
      },
      (err: unknown) => {
        if (!alive.current) return;
        setSaving(null);
        onFail?.();
        setStatus({
          tone: 'danger',
          text: err instanceof Error && err.message ? err.message : messages.saveFailed,
        });
      },
    );
  };

  const choose = (next: CSATRating) => {
    if (locked || saving) return;
    const previous = draft;
    setDraft(next);
    if (mode === 'instant')
      run('rating', onRate?.(next, comment.trim()), messages.ratingSaved, undefined, () =>
        setDraft(previous),
      );
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (rating === null || saving || comment.length > commentMaxLength) return;
    const text = comment.trim();
    if (mode === 'form')
      run('rating', onRate?.(rating, text), messages.thanks, () => setDone(true));
    else run('comment', onComment?.(text, rating), messages.commentSaved);
  };

  const heading = (
    <Heading id={titleId} className="mn-csat__question">
      {messages.question}
    </Heading>
  );

  let content: ReactNode;
  if (loading) {
    content = <LoadingState label={messages.loading} variant="spinner" />;
  } else if (error) {
    content = (
      <div className="mn-csat__message">
        <Heading id={titleId} className="mn-csat__question">
          {messages.loadFailed}
        </Heading>
        <p className="mn-csat__muted">{error}</p>
      </div>
    );
  } else if (submitted || done) {
    content = (
      <p className="mn-csat__thanks" role="status">
        {messages.thanks}
      </p>
    );
  } else {
    const showComment = rating !== null && !locked;
    content = (
      <>
        <div className="mn-csat__head">
          {context ? <p className="mn-csat__context">{context}</p> : null}
          {heading}
        </div>
        {scale === 'good-bad' ? (
          <div className="mn-csat__choices" role="group" aria-label={messages.ratingLabel}>
            {(['good', 'bad'] as const).map((value) => (
              <button
                key={value}
                type="button"
                className="mn-csat__choice"
                data-value={value}
                aria-pressed={rating === value}
                disabled={locked || saving !== null}
                onClick={() => choose(value)}
              >
                <ThumbGlyph down={value === 'bad'} />
                {value === 'good' ? messages.good : messages.bad}
              </button>
            ))}
          </div>
        ) : (
          <RatingPicker
            className="mn-csat__scale"
            aria-label={messages.ratingLabel}
            options={messages.scaleLabels.map((label, i) => ({ value: i + 1, label }))}
            value={typeof rating === 'number' ? rating : null}
            onValueChange={(v) => v !== null && choose(v)}
            disabled={locked || saving !== null}
          />
        )}
        <p className="mn-csat__status" role="status">
          {saving === 'rating' && mode === 'instant'
            ? messages.savingRating
            : locked
              ? messages.locked
              : rating === null
                ? messages.chooseFirst
                : ''}
        </p>
        {showComment || (mode === 'form' && !locked) ? (
          <form className="mn-csat__form" onSubmit={submit} aria-labelledby={titleId}>
            <Field invalid={comment.length > commentMaxLength}>
              <FieldLabel>
                {messages.commentLabel} <span className="mn-csat__muted">{messages.optional}</span>
              </FieldLabel>
              <Textarea
                rows={4}
                value={comment}
                maxLength={commentMaxLength}
                onChange={(e) => setComment(e.target.value)}
              />
              <FieldDescription>
                {messages.remaining(commentMaxLength - comment.length)}
              </FieldDescription>
            </Field>
            <Button
              type="submit"
              variant={mode === 'form' ? 'primary' : 'secondary'}
              loading={saving === (mode === 'form' ? 'rating' : 'comment')}
              disabled={saving !== null || rating === null}
            >
              {mode === 'form' ? messages.submit : messages.saveComment}
            </Button>
          </form>
        ) : null}
        {status ? (
          <Alert tone={status.tone} live="polite">
            {status.text}
          </Alert>
        ) : null}
      </>
    );
  }

  return (
    <section
      ref={ref}
      {...rest}
      aria-labelledby={loading ? undefined : titleId}
      aria-label={loading ? messages.loading : undefined}
      aria-busy={loading || saving !== null || undefined}
      className={cx('mn-csat', className)}
      data-scale={scale}
      data-locked={locked ? '' : undefined}
    >
      {content}
    </section>
  );
});
