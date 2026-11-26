import { forwardRef, type CSSProperties, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** The kind of answer being waited for: paragraphs, a set of cards or a table. */
export type AiResponseSkeletonShape = 'text' | 'cards' | 'table';

export interface AiResponseSkeletonMessages {
  /** Read by screen readers while the placeholder shows. */
  label: string;
}

export const AI_RESPONSE_SKELETON_MESSAGES: AiResponseSkeletonMessages = {
  label: 'Generating response…',
};

export interface AiResponseSkeletonProps extends HTMLAttributes<HTMLDivElement> {
  /** Default `text`. Pick the shape of the answer you expect, to avoid layout shift. */
  shape?: AiResponseSkeletonShape | undefined;
  /**
   * Lines of text (`text`, default 3), cards (`cards`, default 3) or body rows
   * (`table`, default 4).
   */
  lines?: number | undefined;
  /** Columns for the `table` shape. Default 3. */
  columns?: number | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiResponseSkeletonMessages> | undefined;
}

// Fixed widths so the server and the client render the same thing.
const WIDTHS = ['100%', '94%', '97%', '88%', '91%', '96%', '85%'];
const DEFAULT_COUNT: Record<AiResponseSkeletonShape, number> = { text: 3, cards: 3, table: 4 };

function width(i: number, count: number): string {
  return i === count - 1 && count > 1 ? '62%' : (WIDTHS[i % WIDTHS.length] as string);
}

function Bar({ w, className }: { w?: string | undefined; className?: string | undefined }) {
  const style: CSSProperties | undefined = w ? { inlineSize: w } : undefined;
  return <span className={cx('mn-ai-response-skeleton__bar', className)} style={style} />;
}

/**
 * A placeholder shaped like the answer that is on its way (text, cards or a
 * table), shown between sending a prompt and the first streamed token. It
 * shimmers gently (still under reduced motion) and tells screen readers the
 * response is being generated.
 */
export const AiResponseSkeleton = forwardRef<HTMLDivElement, AiResponseSkeletonProps>(
  function AiResponseSkeleton(
    { shape = 'text', lines, columns = 3, messages, className, ...rest },
    ref,
  ) {
    const m = { ...AI_RESPONSE_SKELETON_MESSAGES, ...messages };
    const count = Math.max(1, Math.floor(lines ?? DEFAULT_COUNT[shape]));
    const cols = Math.max(1, Math.floor(columns));
    const indexes = Array.from({ length: count }, (_, i) => i);

    return (
      <div
        ref={ref}
        role="status"
        aria-busy="true"
        {...rest}
        className={cx('mn-ai-response-skeleton', className)}
        data-shape={shape}
      >
        <span className="mn-visually-hidden">{m.label}</span>
        <div className="mn-ai-response-skeleton__shapes" aria-hidden="true">
          {shape === 'text' ? indexes.map((i) => <Bar key={i} w={width(i, count)} />) : null}
          {shape === 'cards'
            ? indexes.map((i) => (
                <span key={i} className="mn-ai-response-skeleton__card">
                  <Bar w="55%" className="mn-ai-response-skeleton__bar--strong" />
                  <Bar w="100%" />
                  <Bar w="78%" />
                </span>
              ))
            : null}
          {shape === 'table' ? (
            <span
              className="mn-ai-response-skeleton__table"
              style={{ '--mn-ai-response-skeleton-columns': cols } as CSSProperties}
            >
              {[-1, ...indexes].map((row) => (
                <span
                  key={row}
                  className="mn-ai-response-skeleton__row"
                  data-header={row === -1 || undefined}
                >
                  {Array.from({ length: cols }, (_, c) => (
                    <Bar
                      key={c}
                      w={row === -1 ? '60%' : width((row + c) % 5, 6)}
                      className={row === -1 ? 'mn-ai-response-skeleton__bar--strong' : undefined}
                    />
                  ))}
                </span>
              ))}
            </span>
          ) : null}
        </div>
      </div>
    );
  },
);
