'use client';

import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type RefObject,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';

/** What a piece of context is. Sets the glyph and the spoken prefix ("File:"). */
export type AiContextChipKind = 'file' | 'selection' | 'page' | 'record' | 'app' | 'person';

/** How much of the context one chip takes, from its share of the total (or of `budget`). */
export type AiContextChipWeight = 'light' | 'medium' | 'heavy';

/** One thing the AI will see with the next prompt. */
export interface AiContextChip {
  /** Stable id, passed to `onRemove`. */
  id: string;
  kind: AiContextChipKind;
  /** Short name: a file name, "Selected text", a record title. Truncated visually, never for assistive tech. */
  label: string;
  /** Estimated tokens it adds. Shown as "1.2K tokens" with a weight marker. */
  tokens?: number | undefined;
  /** Longer text shown as a tooltip, e.g. a path or the first words of a selection. */
  description?: string | undefined;
  /** Set false to keep a chip that cannot be removed (e.g. the current page, always included). Default true. */
  removable?: boolean | undefined;
}

/** Every string AiContextChips shows or announces. */
export interface AiContextChipsMessages {
  /** Name of the list. */
  label: string;
  /** Spoken (visually hidden) prefix per kind. */
  kind: Record<AiContextChipKind, string>;
  /** A chip's token count; `formatted` is the compact number ("1.2k"). */
  tokens: (formatted: string, count: number) => string;
  /** The total after the chips. */
  total: (formatted: string, count: number) => string;
  /** Accessible name of a chip's remove button. */
  remove: (label: string) => string;
  /** Announced politely after a chip is removed. */
  removed: (label: string) => string;
}

export const AI_CONTEXT_CHIPS_MESSAGES: AiContextChipsMessages = {
  label: 'Context',
  kind: {
    file: 'File',
    selection: 'Selection',
    page: 'Page',
    record: 'Record',
    app: 'App',
    person: 'Person',
  },
  tokens: (formatted, n) => (n === 1 ? '1 token' : `${formatted} tokens`),
  total: (formatted, n) => (n === 1 ? '≈ 1 token in context' : `≈ ${formatted} tokens in context`),
  remove: (label) => `Remove ${label} from context`,
  removed: (label) => `${label} removed from context`,
};

export interface AiContextChipsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** What the AI will see, in the order it was added. */
  items: readonly AiContextChip[];
  /**
   * Shows a remove button on each removable chip. Take the chip out of
   * `items`; focus moves to the next chip, else the previous, else
   * `fallbackFocusRef` (or the list itself).
   */
  onRemove?: ((id: string) => void) | undefined;
  /** Show the total token estimate after the chips. Default true when any chip has `tokens`. */
  showTotal?: boolean | undefined;
  /**
   * Tokens the context may use. When set, each chip's weight is its share of
   * the budget instead of its share of the total.
   */
  budget?: number | undefined;
  /** Where focus goes when the last chip is removed, typically the prompt textarea. */
  fallbackFocusRef?: RefObject<HTMLElement | null> | undefined;
  /** Shown when `items` is empty. Default: nothing. */
  empty?: ReactNode | undefined;
  /** Locale for the compact token numbers. Default `en`, so server and client render the same text. */
  locale?: string | undefined;
  /** Override any string. `kind` merges per key. */
  messages?:
    | (Partial<Omit<AiContextChipsMessages, 'kind'>> & {
        kind?: Partial<Record<AiContextChipKind, string>>;
      })
    | undefined;
}

/** Formats a token count compactly: 950, 1.2K, 18K, 1.1M. */
export function formatAiContextTokens(count: number, locale = 'en'): string {
  if (!Number.isFinite(count)) return '0';
  return new Intl.NumberFormat(locale, {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(Math.max(0, Math.round(count)));
}

/** A chip's weight: heavy at half the reference or more, medium from a fifth, else light. */
export function aiContextChipWeight(tokens: number, reference: number): AiContextChipWeight {
  if (!(reference > 0) || !(tokens > 0)) return 'light';
  const share = tokens / reference;
  return share >= 0.5 ? 'heavy' : share >= 0.2 ? 'medium' : 'light';
}

const glyph = {
  width: 14,
  height: 14,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

/** The glyph for a context kind. Decorative. */
export function AiContextKindGlyph({
  kind,
  ...props
}: { kind: AiContextChipKind } & SVGProps<SVGSVGElement>) {
  switch (kind) {
    case 'file':
      return (
        <svg {...glyph} {...props}>
          <path d="M9 1.75H4.5a1 1 0 0 0-1 1v10.5a1 1 0 0 0 1 1h7a1 1 0 0 0 1-1V5.25Z" />
          <path d="M9 1.75v3.5h3.5" />
        </svg>
      );
    case 'selection':
      return (
        <svg {...glyph} {...props}>
          <path d="M3 3.5h10M3 8h6M3 12.5h8" />
          <path d="M12 6.5v5" />
        </svg>
      );
    case 'page':
      return (
        <svg {...glyph} {...props}>
          <rect x="2" y="2.5" width="12" height="11" rx="1.5" />
          <path d="M2 5.5h12M6 5.5v8" />
        </svg>
      );
    case 'record':
      return (
        <svg {...glyph} {...props}>
          <ellipse cx="8" cy="3.75" rx="5" ry="1.75" />
          <path d="M3 3.75v8.5c0 1 2.25 1.75 5 1.75s5-.75 5-1.75v-8.5M3 8c0 1 2.25 1.75 5 1.75S13 9 13 8" />
        </svg>
      );
    case 'app':
      return (
        <svg {...glyph} {...props}>
          <rect x="2.5" y="2.5" width="4.5" height="4.5" rx="1" />
          <rect x="9" y="2.5" width="4.5" height="4.5" rx="1" />
          <rect x="2.5" y="9" width="4.5" height="4.5" rx="1" />
          <rect x="9" y="9" width="4.5" height="4.5" rx="1" />
        </svg>
      );
    case 'person':
      return (
        <svg {...glyph} {...props}>
          <circle cx="8" cy="5.5" r="2.75" />
          <path d="M2.75 13.75c.75-2.5 2.75-3.75 5.25-3.75s4.5 1.25 5.25 3.75" />
        </svg>
      );
  }
}

/**
 * Shows what the AI will see with the next prompt: files, a text selection,
 * the current page, records, apps and people, each as a chip with its kind,
 * name and token weight, plus the total. Chips can be removed one by one;
 * focus stays in the list. Pair it with AiPromptInput and the `@` mention
 * menu (`useAiMentionContext`), which adds chips.
 */
export const AiContextChips = forwardRef<HTMLDivElement, AiContextChipsProps>(
  function AiContextChips(
    {
      items,
      onRemove,
      showTotal,
      budget,
      fallbackFocusRef,
      empty,
      locale = 'en',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...AI_CONTEXT_CHIPS_MESSAGES,
      ...messagesProp,
      kind: { ...AI_CONTEXT_CHIPS_MESSAGES.kind, ...messagesProp?.kind },
    };
    const root = useRef<HTMLDivElement>(null);
    useImperativeHandle(ref, () => root.current as HTMLDivElement, []);
    const buttons = useRef(new Map<string, HTMLButtonElement>());
    const removed = useRef<{ id: string; index: number; label: string } | null>(null);
    const [announcement, setAnnouncement] = useState('');

    const hasTokens = items.some((i) => typeof i.tokens === 'number');
    const total = items.reduce((sum, i) => sum + (i.tokens ?? 0), 0);
    const reference = budget ?? total;
    const totalShown = (showTotal ?? hasTokens) && items.length > 0;

    // When a removed chip leaves `items`, move focus so it is never lost.
    useEffect(() => {
      const r = removed.current;
      if (!r || items.some((i) => i.id === r.id)) return;
      removed.current = null;
      setAnnouncement(m.removed(r.label));
      const candidates = [...items.slice(r.index), ...items.slice(0, r.index).reverse()];
      const target = candidates.map((i) => buttons.current.get(i.id)).find(Boolean);
      if (target) target.focus();
      else (fallbackFocusRef?.current ?? root.current)?.focus();
      // m.removed is read at the time of removal; items drive the effect.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [items]);

    return (
      <div
        ref={root}
        tabIndex={-1}
        {...rest}
        className={cx('mn-ai-context-chips', className)}
        data-empty={items.length === 0 || undefined}
      >
        {items.length > 0 ? (
          <ul className="mn-ai-context-chips__list" aria-label={m.label}>
            {items.map((item, index) => {
              const weight =
                typeof item.tokens === 'number'
                  ? aiContextChipWeight(item.tokens, reference)
                  : undefined;
              const removable = onRemove && item.removable !== false;
              return (
                <li
                  key={item.id}
                  className="mn-ai-context-chips__chip"
                  data-kind={item.kind}
                  data-weight={weight}
                  title={item.description ?? item.label}
                >
                  <span className="mn-ai-context-chips__icon">
                    <AiContextKindGlyph kind={item.kind} />
                  </span>
                  <span className="mn-visually-hidden">{`${m.kind[item.kind]}: `}</span>
                  <span className="mn-ai-context-chips__label">{item.label}</span>
                  {typeof item.tokens === 'number' ? (
                    <>
                      <span className="mn-visually-hidden">, </span>
                      <span className="mn-ai-context-chips__tokens">
                        <span className="mn-ai-context-chips__weight" aria-hidden>
                          <span />
                          <span />
                          <span />
                        </span>
                        {m.tokens(formatAiContextTokens(item.tokens, locale), item.tokens)}
                      </span>
                    </>
                  ) : null}
                  {removable ? (
                    <IconButton
                      ref={(el) => {
                        if (el) buttons.current.set(item.id, el);
                        else buttons.current.delete(item.id);
                      }}
                      size="xs"
                      label={m.remove(item.label)}
                      icon={<CloseGlyph />}
                      className="mn-ai-context-chips__remove"
                      onClick={() => {
                        removed.current = { id: item.id, index, label: item.label };
                        onRemove(item.id);
                      }}
                    />
                  ) : null}
                </li>
              );
            })}
          </ul>
        ) : empty ? (
          <p className="mn-ai-context-chips__empty">{empty}</p>
        ) : null}
        {totalShown ? (
          <p className="mn-ai-context-chips__total">
            {m.total(formatAiContextTokens(total, locale), total)}
          </p>
        ) : null}
        <span role="status" className="mn-visually-hidden">
          {announcement}
        </span>
      </div>
    );
  },
);
