'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { WarningGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';

/** A saved record that looks like the one being created or viewed. */
export interface DuplicateMatch {
  id: string;
  /** Name of the record. */
  title: string;
  /** What kind of record it is, e.g. "Client", "Open lead". */
  kind?: string | undefined;
  /** Why it matched, in words: "Same email", "Same company domain". */
  reasons?: readonly string[] | undefined;
  /** A short extra fact, e.g. "Archived" or "Created 3 Mar". */
  meta?: ReactNode;
  /** Link to open the record. Unsafe URLs are ignored. */
  href?: string | undefined;
  /** How confident the match is, 0–1. Shown as a percentage when given. */
  score?: number | undefined;
}

export interface DuplicatePanelMessages {
  /** Heading: "Possible duplicate" / "Possible duplicates". */
  title: (count: number) => string;
  /** Joins the reasons: "Same email, same name". */
  reasons: (reasons: readonly string[]) => string;
  score: (percent: number) => string;
  open: string;
  /** Hidden end of the Open link's name: "Open Example Ltd". */
  openLabel: (title: string) => string;
  /** Opens in a new tab: hidden text after the link. */
  newTab: string;
  merge: string;
  mergeLabel: (title: string) => string;
  dismiss: string;
  dismissLabel: (title: string) => string;
}

export const defaultDuplicatePanelMessages: DuplicatePanelMessages = {
  title: (n) => (n === 1 ? 'Possible duplicate' : 'Possible duplicates'),
  reasons: (r) => r.join(', '),
  score: (p) => `${p}% match`,
  open: 'Open',
  openLabel: (t) => `Open ${t}`,
  newTab: '(opens in a new tab)',
  merge: 'Merge',
  mergeLabel: (t) => `Merge with ${t}`,
  dismiss: 'Not a duplicate',
  dismissLabel: (t) => `${t} is not a duplicate`,
};

export interface DuplicatePanelProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Likely duplicates. Nothing renders when the list is empty. */
  matches: readonly DuplicateMatch[];
  /** Adds a Merge button per match (open a MergeDialog from it). */
  onMerge?: ((match: DuplicateMatch) => void) | undefined;
  /** Adds a "Not a duplicate" button per match. */
  onDismiss?: ((match: DuplicateMatch) => void) | undefined;
  /** Open links in a new tab, so a half-filled form is not lost. Default true. */
  openInNewTab?: boolean;
  /** A closing line, e.g. "You can still create it." */
  footer?: ReactNode;
  /**
   * `hint` (default): a quiet warning box under a create form, announced
   * politely when it appears. `panel`: a plain section on a record page.
   */
  variant?: 'hint' | 'panel';
  /** Heading level for `panel`. Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'p';
  messages?: Partial<DuplicatePanelMessages>;
}

/**
 * Lists saved records that look like the one being created or viewed, why
 * each matched, and what to do: open it, merge, or mark it as not a
 * duplicate. It is advice: it never blocks saving.
 */
export const DuplicatePanel = forwardRef<HTMLElement, DuplicatePanelProps>(function DuplicatePanel(
  {
    matches,
    onMerge,
    onDismiss,
    openInNewTab = true,
    footer,
    variant = 'hint',
    headingLevel = 'h3',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultDuplicatePanelMessages, ...messages };
  // The live region stays mounted (even when empty) so the first matches are announced.
  const Heading = variant === 'hint' ? 'p' : headingLevel;
  return (
    <section
      ref={ref}
      {...(variant === 'hint' ? { role: 'status', 'aria-live': 'polite' as const } : {})}
      {...rest}
      hidden={matches.length === 0 || rest.hidden}
      data-variant={variant}
      className={cx('mn-duplicate-panel', className)}
    >
      {matches.length ? (
        <>
          <Heading className="mn-duplicate-panel__title">
            {variant === 'hint' ? (
              <span className="mn-duplicate-panel__icon" aria-hidden="true">
                <WarningGlyph />
              </span>
            ) : null}
            {m.title(matches.length)}
          </Heading>
          <ul className="mn-duplicate-panel__list">
            {matches.map((match) => {
              const href = match.href && isSafeUrl(match.href) ? match.href : undefined;
              const details = [
                match.kind,
                match.reasons?.length ? m.reasons(match.reasons) : null,
                match.score !== undefined ? m.score(Math.round(match.score * 100)) : null,
              ].filter(Boolean);
              return (
                <li key={match.id} className="mn-duplicate-panel__item">
                  <div className="mn-duplicate-panel__text">
                    <span className="mn-duplicate-panel__name">{match.title}</span>
                    {match.meta ? (
                      <span className="mn-duplicate-panel__meta">{match.meta}</span>
                    ) : null}
                    {details.length ? (
                      <span className="mn-duplicate-panel__details">{details.join(' · ')}</span>
                    ) : null}
                  </div>
                  <div className="mn-duplicate-panel__actions">
                    {href ? (
                      <a
                        className="mn-duplicate-panel__open"
                        href={href}
                        {...(openInNewTab ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                        aria-label={`${m.openLabel(match.title)}${openInNewTab ? ` ${m.newTab}` : ''}`}
                      >
                        {m.open}
                      </a>
                    ) : null}
                    {onMerge ? (
                      <Button
                        size="xs"
                        variant="secondary"
                        aria-label={m.mergeLabel(match.title)}
                        onClick={() => onMerge(match)}
                      >
                        {m.merge}
                      </Button>
                    ) : null}
                    {onDismiss ? (
                      <Button
                        size="xs"
                        variant="ghost"
                        aria-label={m.dismissLabel(match.title)}
                        onClick={() => onDismiss(match)}
                      >
                        {m.dismiss}
                      </Button>
                    ) : null}
                  </div>
                </li>
              );
            })}
          </ul>
          {footer ? <p className="mn-duplicate-panel__footer">{footer}</p> : null}
        </>
      ) : null}
    </section>
  );
});
