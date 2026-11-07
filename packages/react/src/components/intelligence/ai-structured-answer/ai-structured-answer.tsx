'use client';

import {
  forwardRef,
  memo,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { DescriptionItem, DescriptionList } from '../../data-display/description-list/index.js';
import { AiMarkdown } from '../ai-markdown/index.js';

/** How a field's value is shown. */
export type AiStructuredFieldType = 'text' | 'list' | 'number' | 'badge' | 'markdown';

/** One field of the answer. */
export interface AiStructuredField {
  /** The key of the value in `data`. */
  key: string;
  /** The visible label (the `<dt>`). */
  label: ReactNode;
  /**
   * `text`: plain text. `list`: an array as a bulleted list. `number`: formatted
   * with `Intl.NumberFormat`. `badge`: one badge per value. `markdown`: rendered
   * with AiMarkdown (no raw HTML).
   */
  type: AiStructuredFieldType;
  /** Number formatting for `number` fields, e.g. `{ style: 'percent' }`. */
  format?: Intl.NumberFormatOptions | undefined;
  /** Badge tone for `badge` fields: a fixed tone, or one per value. Default `neutral`. */
  tone?: Tone | ((value: string) => Tone) | undefined;
  /** Span the full width in the `grid` layout. `markdown` and `list` fields always do. */
  wide?: boolean | undefined;
}

export interface AiStructuredAnswerMessages {
  /** Visible provenance label above the answer. */
  aiAnswer: string;
  /** Read for a field that has not arrived yet while streaming. */
  pending: string;
  /** Read for a field with no value once the answer is complete. */
  empty: string;
  /** Announced politely when streaming finishes. */
  complete: string;
}

export const AI_STRUCTURED_ANSWER_MESSAGES: AiStructuredAnswerMessages = {
  aiAnswer: 'AI answer',
  pending: 'Still generating',
  empty: 'Not provided',
  complete: 'Response complete.',
};

export interface AiStructuredAnswerProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The fields to show, in order. Values not in the schema are ignored. */
  schema: readonly AiStructuredField[];
  /** The values, usually parsed from the model's structured output. Treated as untrusted data. */
  data: Readonly<Record<string, unknown>>;
  /** The answer is still arriving: missing fields show placeholders and the answer is busy. */
  streaming?: boolean | undefined;
  /** Optional heading, e.g. "Interview brief". */
  title?: ReactNode | undefined;
  /** Heading level of the title. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | undefined;
  /** `list` (default): label beside value, one per row. `grid`: cards in responsive columns. */
  layout?: 'list' | 'grid' | undefined;
  /** Locale for numbers. Default: the runtime's. */
  locale?: string | undefined;
  /** Content after the fields, e.g. AiMessageActions or a Copy button. */
  actions?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiStructuredAnswerMessages> | undefined;
}

function isMissing(value: unknown): boolean {
  return (
    value === undefined ||
    value === null ||
    value === '' ||
    (Array.isArray(value) && value.length === 0)
  );
}

function asText(value: unknown): string {
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  if (value instanceof Date) return value.toISOString();
  try {
    return JSON.stringify(value) ?? '';
  } catch {
    return String(value);
  }
}

function asList(value: unknown): string[] {
  return (Array.isArray(value) ? value : [value]).filter((v) => !isMissing(v)).map(asText);
}

interface FieldValueProps {
  field: AiStructuredField;
  value: unknown;
  locale: string | undefined;
  headingOffset: number;
}

// Memoised per field, so completed fields are not re-rendered on every token.
const FieldValue = memo(function FieldValue({
  field,
  value,
  locale,
  headingOffset,
}: FieldValueProps) {
  switch (field.type) {
    case 'number': {
      const n = typeof value === 'number' ? value : Number(asText(value).replace(/,/g, ''));
      return (
        <span className="mn-ai-structured-answer__number">
          {Number.isFinite(n)
            ? new Intl.NumberFormat(locale, field.format).format(n)
            : asText(value)}
        </span>
      );
    }
    case 'list':
      return (
        <ul className="mn-ai-structured-answer__list">
          {asList(value).map((item, i) => (
            <li key={i}>{item}</li>
          ))}
        </ul>
      );
    case 'badge':
      return (
        <span className="mn-ai-structured-answer__badges">
          {asList(value).map((item, i) => (
            <Badge
              key={i}
              tone={typeof field.tone === 'function' ? field.tone(item) : (field.tone ?? 'neutral')}
            >
              {item}
            </Badge>
          ))}
        </span>
      );
    case 'markdown':
      return (
        <AiMarkdown
          className="mn-ai-structured-answer__markdown"
          text={asText(value)}
          headingLevelOffset={headingOffset}
        />
      );
    default:
      return <span className="mn-ai-structured-answer__text">{asText(value)}</span>;
  }
});

/**
 * A model's structured output shown as labelled fields (text, lists, numbers,
 * badges or Markdown) instead of a wall of prose: an interview brief, a lead
 * score summary, an extracted record. Fields fill in as they stream, with
 * placeholders for the ones still to come. Marked as an AI answer, visibly
 * and for assistive technology.
 */
export const AiStructuredAnswer = forwardRef<HTMLElement, AiStructuredAnswerProps>(
  function AiStructuredAnswer(
    {
      schema,
      data,
      streaming = false,
      title,
      headingLevel = 3,
      layout = 'list',
      locale,
      actions,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_STRUCTURED_ANSWER_MESSAGES, ...messagesProp };
    const labelId = useId();
    const titleId = useId();
    const Heading = `h${headingLevel}` as const;
    const [announcement, setAnnouncement] = useState('');
    const wasStreaming = useRef(streaming);

    useEffect(() => {
      if (wasStreaming.current && !streaming) setAnnouncement(m.complete);
      else if (streaming) setAnnouncement('');
      wasStreaming.current = streaming;
    }, [streaming, m.complete]);

    return (
      <section
        ref={ref}
        aria-labelledby={title ? `${labelId} ${titleId}` : labelId}
        aria-busy={streaming || undefined}
        {...rest}
        className={cx('mn-ai-structured-answer', className)}
        data-layout={layout}
        data-streaming={streaming || undefined}
      >
        <div className="mn-ai-structured-answer__header">
          <span id={labelId} className="mn-ai-structured-answer__eyebrow">
            <BotGlyph aria-hidden="true" />
            {m.aiAnswer}
          </span>
          {title ? (
            <Heading id={titleId} className="mn-ai-structured-answer__title">
              {title}
            </Heading>
          ) : null}
        </div>
        <DescriptionList
          className="mn-ai-structured-answer__fields"
          columns={layout === 'grid' ? 'auto' : 1}
          layout={layout === 'grid' ? 'stacked' : 'inline'}
        >
          {schema.map((field) => {
            const value = data[field.key];
            const missing = isMissing(value);
            const pending = missing && streaming;
            return (
              <DescriptionItem
                key={field.key}
                term={field.label}
                wide={field.wide ?? (field.type === 'markdown' || field.type === 'list')}
                className="mn-ai-structured-answer__field"
                emptyLabel={m.empty}
                placeholder={
                  pending ? (
                    <span className="mn-ai-structured-answer__pending">
                      <span className="mn-ai-structured-answer__pending-bar" aria-hidden="true" />
                      <span className="mn-visually-hidden">{m.pending}</span>
                    </span>
                  ) : undefined
                }
              >
                {missing ? null : (
                  <FieldValue
                    field={field}
                    value={value}
                    locale={locale}
                    headingOffset={title ? headingLevel : headingLevel - 1}
                  />
                )}
              </DescriptionItem>
            );
          })}
        </DescriptionList>
        {actions ? <div className="mn-ai-structured-answer__actions">{actions}</div> : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {announcement}
        </span>
      </section>
    );
  },
);
