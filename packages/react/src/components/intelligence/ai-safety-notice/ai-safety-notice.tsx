import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { FilterGlyph, LockGlyph, PencilGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';

/**
 * What the safety system did. `blocked`: no answer was given. `modified`: the
 * answer was changed before it was shown. `filtered`: parts of the answer were
 * removed.
 */
export type AiSafetyNoticeKind = 'blocked' | 'modified' | 'filtered';

export interface AiSafetyNoticeMessages {
  /** Title when the request was blocked. */
  blockedTitle: string;
  /** Explanation when the request was blocked. */
  blockedBody: string;
  /** Title when the answer was changed. */
  modifiedTitle: string;
  /** Explanation when the answer was changed. */
  modifiedBody: string;
  /** Title when parts of the answer were removed. */
  filteredTitle: string;
  /** Explanation when parts of the answer were removed. */
  filteredBody: string;
  /** Label before the reason, e.g. "Reason:". */
  reason: string;
  /** Label of the button that puts the prompt back into the composer. */
  editPrompt: string;
  /** Text of the link to the guidelines (`learnMoreHref`). */
  learnMore: string;
}

export const AI_SAFETY_NOTICE_MESSAGES: AiSafetyNoticeMessages = {
  blockedTitle: 'This request wasn’t answered',
  blockedBody: 'It may go against the content guidelines. You can rephrase it and try again.',
  modifiedTitle: 'This response was adjusted',
  modifiedBody: 'Parts of the answer were changed to follow the content guidelines.',
  filteredTitle: 'Some content was hidden',
  filteredBody:
    'Parts of the answer were removed because they may go against the content guidelines.',
  reason: 'Reason:',
  editPrompt: 'Edit prompt',
  learnMore: 'Content guidelines',
};

export interface AiSafetyNoticeProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What the safety system did. Sets the icon, tone and default wording. */
  kind: AiSafetyNoticeKind;
  /** Why, in plain words, when the product knows (e.g. "Personal data"). */
  reason?: ReactNode | undefined;
  /** What the person can do next: a sentence, a list or links. */
  nextSteps?: ReactNode | undefined;
  /** Shows an "Edit prompt" button, e.g. to put the prompt back in the composer. */
  onEditPrompt?: (() => void) | undefined;
  /** Link to the content guidelines. Unsafe URLs (e.g. `javascript:`) are dropped. */
  learnMoreHref?: string | undefined;
  /** Replaces the default title for this instance. */
  title?: ReactNode | undefined;
  /** Replaces the default explanation for this instance. */
  children?: ReactNode;
  /** Strings, for localisation. */
  messages?: Partial<AiSafetyNoticeMessages> | undefined;
}

const ICONS = { blocked: LockGlyph, modified: PencilGlyph, filtered: FilterGlyph } as const;

/**
 * Says, calmly and specifically, that a safety system blocked a request,
 * changed an answer or removed parts of it, with an optional reason, next
 * steps and an "Edit prompt" action. It never blames the person.
 */
export const AiSafetyNotice = forwardRef<HTMLDivElement, AiSafetyNoticeProps>(
  function AiSafetyNotice(
    {
      kind,
      reason,
      nextSteps,
      onEditPrompt,
      learnMoreHref,
      title,
      children,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_SAFETY_NOTICE_MESSAGES, ...messagesProp };
    const titleId = useId();
    const Icon = ICONS[kind];
    const href = learnMoreHref && isSafeUrl(learnMoreHref) ? learnMoreHref : undefined;
    const defaults = {
      blocked: [m.blockedTitle, m.blockedBody],
      modified: [m.modifiedTitle, m.modifiedBody],
      filtered: [m.filteredTitle, m.filteredBody],
    }[kind];

    return (
      <div
        ref={ref}
        role="note"
        aria-labelledby={titleId}
        {...rest}
        className={cx('mn-ai-safety-notice', className)}
        data-kind={kind}
      >
        <Icon aria-hidden="true" className="mn-ai-safety-notice__icon" />
        <div className="mn-ai-safety-notice__content">
          <p id={titleId} className="mn-ai-safety-notice__title">
            {title ?? defaults[0]}
          </p>
          <div className="mn-ai-safety-notice__body">{children ?? defaults[1]}</div>
          {reason ? (
            <p className="mn-ai-safety-notice__reason">
              <span className="mn-ai-safety-notice__reason-label">{m.reason}</span> {reason}
            </p>
          ) : null}
          {nextSteps ? <div className="mn-ai-safety-notice__next">{nextSteps}</div> : null}
          {onEditPrompt || href ? (
            <div className="mn-ai-safety-notice__actions">
              {onEditPrompt ? (
                <Button
                  size="sm"
                  variant="secondary"
                  leadingIcon={<PencilGlyph />}
                  onClick={onEditPrompt}
                >
                  {m.editPrompt}
                </Button>
              ) : null}
              {href ? (
                <a className="mn-ai-safety-notice__link" href={href} rel="noopener noreferrer">
                  {m.learnMore}
                </a>
              ) : null}
            </div>
          ) : null}
        </div>
      </div>
    );
  },
);
