'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph, DotGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import { Field, FieldLabel } from '../../../components/forms/form-field/index.js';
import { Textarea } from '../../../components/forms/text-field/index.js';
import { Kbd } from '../../../components/typography/kbd/index.js';
import { AiMarkdown } from '../../../components/intelligence/ai-markdown/index.js';
import { AiGeneratedBadge } from '../../../components/intelligence/ai-generated-badge/index.js';

/** Every string AiEvalReview shows or announces. English defaults. */
export interface AiEvalReviewMessages {
  /** Accessible name of the whole review. */
  label: string;
  /** Accessible name of the queue list. */
  queue: string;
  progress: (reviewed: number, total: number) => string;
  /** Fallback heading for an item without a title. */
  itemTitle: (index: number, total: number) => string;
  itemOf: (index: number, total: number) => string;
  prompt: string;
  output: string;
  reference: string;
  rubric: string;
  /** Name of one score choice, e.g. "3 of 5". */
  scoreOf: (score: number, scale: number) => string;
  /** Rubric total, e.g. "Score 9 / 15". */
  total: (score: number, max: number) => string;
  notes: string;
  verdict: string;
  pass: string;
  fail: string;
  pending: string;
  previous: string;
  next: string;
  /** Visible hint for the shortcuts. */
  shortcuts: string;
  empty: string;
}

const DEFAULT_MESSAGES: AiEvalReviewMessages = {
  label: 'Review AI outputs',
  queue: 'Review queue',
  progress: (r, t) => `${r} of ${t} reviewed`,
  itemTitle: (i) => `Output ${i}`,
  itemOf: (i, t) => `${i} of ${t}`,
  prompt: 'Prompt',
  output: 'Output',
  reference: 'Expected',
  rubric: 'Rubric',
  scoreOf: (s, scale) => `${s} of ${scale}`,
  total: (s, max) => `Score ${s} / ${max}`,
  notes: 'Notes',
  verdict: 'Verdict',
  pass: 'Pass',
  fail: 'Fail',
  pending: 'Not reviewed',
  previous: 'Previous',
  next: 'Next',
  shortcuts: 'previous / next, pass / fail',
  empty: 'Nothing to review.',
};

/** One AI output to review. */
export interface AiEvalItem {
  id: string;
  /** Short name in the queue and heading. Default "Output N". */
  title?: ReactNode | undefined;
  /** The input the model received. */
  prompt?: ReactNode | undefined;
  /** The AI output. A string is rendered as Markdown (safely); a node as is. */
  output: ReactNode;
  /** The expected or reference answer, when there is one. */
  reference?: ReactNode | undefined;
  /** Extra details under the heading, e.g. model and date. */
  meta?: ReactNode | undefined;
}

/** One rubric criterion, scored from 1 to `scale`. */
export interface AiEvalCriterion {
  id: string;
  label: string;
  description?: ReactNode | undefined;
  /** Highest score, 1 to 5. Default 5. */
  scale?: 1 | 2 | 3 | 4 | 5 | undefined;
}

export type AiEvalVerdict = 'pass' | 'fail';

/** A reviewer's judgement of one item. */
export interface AiEvalItemReview {
  /** Criterion id → score (1..scale). */
  scores: Readonly<Record<string, number>>;
  notes: string;
  verdict: AiEvalVerdict | null;
}

export const EMPTY_AI_EVAL_REVIEW: AiEvalItemReview = { scores: {}, notes: '', verdict: null };

export interface AiEvalReviewProps extends Omit<HTMLAttributes<HTMLElement>, 'onChange'> {
  /** The queue of outputs to review. */
  items: readonly AiEvalItem[];
  /** The rubric every item is scored on. */
  rubric: readonly AiEvalCriterion[];
  /** Reviews by item id (controlled). */
  reviews?: Readonly<Record<string, AiEvalItemReview>> | undefined;
  /** Initial reviews when uncontrolled. */
  defaultReviews?: Readonly<Record<string, AiEvalItemReview>> | undefined;
  /** Called on every score, note or verdict change. Persist it to keep work safe. */
  onReviewChange?:
    | ((itemId: string, review: AiEvalItemReview, all: Record<string, AiEvalItemReview>) => void)
    | undefined;
  /** The item shown (controlled). */
  activeId?: string | undefined;
  /** First item shown when uncontrolled. Default: the first item without a verdict. */
  defaultActiveId?: string | undefined;
  onActiveChange?: ((itemId: string) => void) | undefined;
  /** Single-key shortcuts (J/K, P/F) outside text fields. Default true. */
  shortcuts?: boolean | undefined;
  /** Move to the next item after a verdict is given. Default false. */
  advanceOnVerdict?: boolean | undefined;
  /** Content after the review form, e.g. a "Submit reviews" button. */
  footer?: ReactNode | undefined;
  /** Section heading level of the item title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiEvalReviewMessages> | undefined;
}

function isTextEntry(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  if (target.isContentEditable || target instanceof HTMLTextAreaElement) return true;
  if (target instanceof HTMLSelectElement) return true;
  return (
    target instanceof HTMLInputElement && !['radio', 'checkbox', 'button'].includes(target.type)
  );
}

/**
 * A review queue for AI outputs: a list of items with their status, and for
 * the current item its prompt, output and reference answer, a rubric scored
 * 1 to 5 per criterion, notes and a pass / fail verdict. Moves with buttons,
 * arrow keys in the queue and J / K, P / F shortcuts.
 */
export const AiEvalReview = forwardRef<HTMLElement, AiEvalReviewProps>(function AiEvalReview(
  {
    items,
    rubric,
    reviews: reviewsProp,
    defaultReviews,
    onReviewChange,
    activeId: activeProp,
    defaultActiveId,
    onActiveChange,
    shortcuts = true,
    advanceOnVerdict = false,
    footer,
    headingLevel: H = 'h2',
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const auto = useId().replace(/:/g, '');
  const base = `mn-ai-eval-${auto}`;
  const Sub = H === 'h2' ? 'h3' : 'h4';

  const [reviewsInner, setReviewsInner] = useState<Record<string, AiEvalItemReview>>(() => ({
    ...defaultReviews,
  }));
  const reviews = reviewsProp ?? reviewsInner;

  const firstOpen = () =>
    defaultActiveId ?? items.find((i) => !reviews[i.id]?.verdict)?.id ?? items[0]?.id;
  const [activeInner, setActiveInner] = useState<string | undefined>(firstOpen);
  const active = activeProp ?? activeInner;
  const index = Math.max(
    0,
    items.findIndex((i) => i.id === active),
  );
  const item = items[index];
  const review = (item && reviews[item.id]) || EMPTY_AI_EVAL_REVIEW;

  const headingRef = useRef<HTMLHeadingElement | null>(null);
  const queueRef = useRef<HTMLUListElement | null>(null);
  const focusHeading = useRef(false);

  const select = (next: number, opts: { focus?: 'heading' | 'queue' } = {}) => {
    const target = items[Math.min(Math.max(next, 0), items.length - 1)];
    if (!target) return;
    if (opts.focus === 'heading') focusHeading.current = true;
    if (activeProp === undefined) setActiveInner(target.id);
    onActiveChange?.(target.id);
    if (opts.focus === 'queue') {
      const buttons = queueRef.current?.querySelectorAll<HTMLButtonElement>('[data-item-id]');
      Array.from(buttons ?? [])
        .find((b) => b.dataset.itemId === target.id)
        ?.focus();
    }
  };
  useEffect(() => {
    if (!focusHeading.current) return;
    focusHeading.current = false;
    headingRef.current?.focus();
  }, [index]);

  const update = (patch: Partial<AiEvalItemReview>) => {
    if (!item) return;
    const next: AiEvalItemReview = { ...review, ...patch };
    const all = { ...reviews, [item.id]: next };
    if (reviewsProp === undefined) setReviewsInner(all);
    onReviewChange?.(item.id, next, all);
  };

  const setVerdict = (verdict: AiEvalVerdict) => {
    update({ verdict });
    if (advanceOnVerdict && index < items.length - 1) select(index + 1, { focus: 'heading' });
  };

  const reviewed = items.filter((i) => reviews[i.id]?.verdict).length;
  const maxTotal = rubric.reduce((sum, c) => sum + (c.scale ?? 5), 0);
  const scored = rubric.filter((c) => review.scores[c.id] !== undefined);
  const total = scored.reduce((sum, c) => sum + (review.scores[c.id] ?? 0), 0);

  const handleKeyDown = (e: ReactKeyboardEvent<HTMLElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented || !shortcuts) return;
    if (e.metaKey || e.ctrlKey || e.altKey || isTextEntry(e.target)) return;
    const key = e.key.toLowerCase();
    if (key === 'j') {
      e.preventDefault();
      select(index + 1, { focus: 'heading' });
    } else if (key === 'k') {
      e.preventDefault();
      select(index - 1, { focus: 'heading' });
    } else if (key === 'p') {
      e.preventDefault();
      setVerdict('pass');
    } else if (key === 'f') {
      e.preventDefault();
      setVerdict('fail');
    }
  };

  const handleQueueKeyDown = (e: ReactKeyboardEvent<HTMLUListElement>) => {
    const keys: Record<string, number> = {
      ArrowDown: index + 1,
      ArrowUp: index - 1,
      Home: 0,
      End: items.length - 1,
    };
    const next = keys[e.key];
    if (next === undefined) return;
    e.preventDefault();
    select(next, { focus: 'queue' });
  };

  const statusText = (v: AiEvalVerdict | null | undefined) =>
    v === 'pass' ? m.pass : v === 'fail' ? m.fail : m.pending;

  return (
    // Shortcut keys are handled on the region; every control inside is a real button or input.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <section
      ref={ref}
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-eval-review', className)}
      onKeyDown={handleKeyDown}
    >
      <div className="mn-ai-eval-review__queue">
        <p className="mn-ai-eval-review__progress">
          <span>{m.progress(reviewed, items.length)}</span>
          <span className="mn-ai-eval-review__bar" aria-hidden>
            <span
              className="mn-ai-eval-review__bar-fill"
              style={{ inlineSize: `${items.length ? (reviewed / items.length) * 100 : 0}%` }}
            />
          </span>
        </p>
        {/* Arrow keys move through the queue (selection follows focus). */}
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
        <ul
          ref={queueRef}
          className="mn-ai-eval-review__list"
          aria-label={m.queue}
          onKeyDown={handleQueueKeyDown}
        >
          {items.map((it, i) => {
            const v = reviews[it.id]?.verdict;
            const current = i === index;
            return (
              <li key={it.id}>
                <button
                  type="button"
                  className="mn-ai-eval-review__entry"
                  data-item-id={it.id}
                  data-verdict={v ?? 'pending'}
                  aria-current={current ? 'true' : undefined}
                  tabIndex={current ? 0 : -1}
                  onClick={() => select(i)}
                >
                  <span className="mn-ai-eval-review__entry-icon" aria-hidden>
                    {v === 'pass' ? <CheckGlyph /> : v === 'fail' ? <CloseGlyph /> : <DotGlyph />}
                  </span>
                  <span className="mn-ai-eval-review__entry-title">
                    {it.title ?? m.itemTitle(i + 1, items.length)}
                  </span>
                  <span className="mn-ai-eval-review__entry-status">{statusText(v)}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>

      {item ? (
        <div className="mn-ai-eval-review__detail">
          <div className="mn-ai-eval-review__head">
            <div className="mn-ai-eval-review__heading">
              <p className="mn-ai-eval-review__count">{m.itemOf(index + 1, items.length)}</p>
              <H ref={headingRef} tabIndex={-1} className="mn-ai-eval-review__title">
                {item.title ?? m.itemTitle(index + 1, items.length)}
              </H>
              {item.meta ? <div className="mn-ai-eval-review__meta">{item.meta}</div> : null}
            </div>
            <div className="mn-ai-eval-review__nav">
              <Button
                size="sm"
                disabled={index === 0}
                onClick={() => select(index - 1, { focus: 'heading' })}
              >
                {m.previous}
              </Button>
              <Button
                size="sm"
                disabled={index === items.length - 1}
                onClick={() => select(index + 1, { focus: 'heading' })}
              >
                {m.next}
              </Button>
            </div>
          </div>

          {item.prompt ? (
            <section className="mn-ai-eval-review__block" aria-labelledby={`${base}-prompt`}>
              <Sub id={`${base}-prompt`} className="mn-ai-eval-review__label">
                {m.prompt}
              </Sub>
              <div className="mn-ai-eval-review__content">{item.prompt}</div>
            </section>
          ) : null}
          <section
            className="mn-ai-eval-review__block"
            data-kind="output"
            aria-labelledby={`${base}-output`}
          >
            <div className="mn-ai-eval-review__block-head">
              <Sub id={`${base}-output`} className="mn-ai-eval-review__label">
                {m.output}
              </Sub>
              <AiGeneratedBadge size="sm" />
            </div>
            <div className="mn-ai-eval-review__content">
              {typeof item.output === 'string' ? <AiMarkdown text={item.output} /> : item.output}
            </div>
          </section>
          {item.reference ? (
            <section className="mn-ai-eval-review__block" aria-labelledby={`${base}-reference`}>
              <Sub id={`${base}-reference`} className="mn-ai-eval-review__label">
                {m.reference}
              </Sub>
              <div className="mn-ai-eval-review__content">{item.reference}</div>
            </section>
          ) : null}

          <div className="mn-ai-eval-review__form">
            {rubric.length ? (
              <div className="mn-ai-eval-review__rubric">
                <div className="mn-ai-eval-review__rubric-head">
                  <Sub className="mn-ai-eval-review__label">{m.rubric}</Sub>
                  <span className="mn-ai-eval-review__total">{m.total(total, maxTotal)}</span>
                </div>
                {rubric.map((c) => {
                  const scale = c.scale ?? 5;
                  const descId = `${base}-${c.id}-desc`;
                  return (
                    <fieldset
                      key={c.id}
                      className="mn-ai-eval-review__criterion"
                      aria-describedby={c.description ? descId : undefined}
                    >
                      <legend className="mn-ai-eval-review__criterion-label">{c.label}</legend>
                      {c.description ? (
                        <p id={descId} className="mn-ai-eval-review__criterion-description">
                          {c.description}
                        </p>
                      ) : null}
                      <div className="mn-ai-eval-review__scale">
                        {Array.from({ length: scale }, (_, k) => k + 1).map((n) => (
                          <label key={n} className="mn-ai-eval-review__choice">
                            <input
                              type="radio"
                              name={`${base}-${item.id}-${c.id}`}
                              value={n}
                              checked={review.scores[c.id] === n}
                              aria-label={m.scoreOf(n, scale)}
                              onChange={() => update({ scores: { ...review.scores, [c.id]: n } })}
                            />
                            <span aria-hidden>{n}</span>
                          </label>
                        ))}
                      </div>
                    </fieldset>
                  );
                })}
              </div>
            ) : null}

            <Field className="mn-ai-eval-review__notes">
              <FieldLabel>{m.notes}</FieldLabel>
              <Textarea
                rows={3}
                value={review.notes}
                onChange={(e) => update({ notes: e.target.value })}
              />
            </Field>

            <fieldset className="mn-ai-eval-review__verdict">
              <legend className="mn-ai-eval-review__criterion-label">{m.verdict}</legend>
              <div className="mn-ai-eval-review__scale">
                {(['pass', 'fail'] as const).map((v) => (
                  <label key={v} className="mn-ai-eval-review__choice" data-verdict={v}>
                    <input
                      type="radio"
                      name={`${base}-${item.id}-verdict`}
                      value={v}
                      checked={review.verdict === v}
                      onChange={() => setVerdict(v)}
                    />
                    <span aria-hidden className="mn-ai-eval-review__choice-icon">
                      {v === 'pass' ? <CheckGlyph /> : <CloseGlyph />}
                    </span>
                    <span>{v === 'pass' ? m.pass : m.fail}</span>
                  </label>
                ))}
              </div>
            </fieldset>
            {shortcuts ? (
              <p className="mn-ai-eval-review__shortcuts">
                <Kbd>J</Kbd> <Kbd>K</Kbd> <Kbd>P</Kbd> <Kbd>F</Kbd> {m.shortcuts}
              </p>
            ) : null}
            {footer}
          </div>
        </div>
      ) : (
        <p className="mn-ai-eval-review__empty">{m.empty}</p>
      )}
    </section>
  );
});
