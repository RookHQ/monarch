import { forwardRef, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import { Tooltip, TooltipProvider } from '../../overlays/tooltip/index.js';

/** How much of the content came from AI. */
export type AiGeneratedKind = 'generated' | 'assisted' | 'edited';

export interface AiGeneratedBadgeMessages {
  /** Visible label per kind. */
  labels: Record<AiGeneratedKind, string>;
  /**
   * The provenance line read after the label and shown in the tooltip, from
   * the model name and the formatted time (either may be missing). Return an
   * empty string to show nothing.
   */
  details: (info: { kind: AiGeneratedKind; model?: string; at?: string }) => string;
  /** Formats `at`. Default: `Intl.DateTimeFormat` medium date, short time. */
  formatDate: (date: Date) => string;
}

const DEFAULT_MESSAGES: AiGeneratedBadgeMessages = {
  labels: {
    generated: 'AI generated',
    assisted: 'AI assisted',
    edited: 'Edited after AI',
  },
  details: ({ model, at }) => {
    if (model && at) return `By ${model}, ${at}`;
    if (model) return `By ${model}`;
    if (at) return at;
    return '';
  },
  formatDate: (date) =>
    new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date),
};

/** The AI identity mark, for custom AI controls. */
export function AiBotGlyph(props: SVGProps<SVGSVGElement>) {
  return <BotGlyph {...props} />;
}

function toDate(at: string | number | Date | undefined): Date | undefined {
  if (at === undefined || at === '') return undefined;
  const date = at instanceof Date ? at : new Date(at);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

export interface AiGeneratedBadgeProps extends HTMLAttributes<HTMLSpanElement> {
  /**
   * `generated`: written by AI. `assisted`: written by a person with AI help.
   * `edited`: AI wrote it and a person has since changed it. Default `generated`.
   */
  kind?: AiGeneratedKind;
  /**
   * Extra supplementary text shown on hover and keyboard focus. Default: the
   * model and time, when either is given. Never put essential information here.
   */
  tooltip?: ReactNode | undefined;
  /** The model that produced the content, e.g. `gemini-2.5-pro`. */
  model?: string | undefined;
  /** When the content was produced: a `Date`, epoch milliseconds or an ISO 8601 string. */
  at?: string | number | Date | undefined;
  /** `sm` for table cells and dense lists. Default `md`. */
  size?: 'sm' | 'md';
  /** Shows only the Bot glyph; the label stays available to assistive technology and in the tooltip. */
  iconOnly?: boolean;
  /** Replaces the kind's label. */
  label?: string | undefined;
  /** Strings, for localisation. `labels` merges per kind. */
  messages?: Partial<Omit<AiGeneratedBadgeMessages, 'labels'>> & {
    labels?: Partial<Record<AiGeneratedKind, string>>;
  };
}

/**
 * A small provenance badge that marks content as produced by AI, written with
 * AI help, or edited by a person after AI wrote it. Shows the Bot glyph in the
 * AI colour with a text label, and the model and time on hover and focus.
 */
export const AiGeneratedBadge = forwardRef<HTMLSpanElement, AiGeneratedBadgeProps>(
  function AiGeneratedBadge(
    {
      kind = 'generated',
      tooltip,
      model,
      at,
      size = 'md',
      iconOnly = false,
      label: labelProp,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      labels: { ...DEFAULT_MESSAGES.labels, ...messagesProp?.labels },
    };
    const label = labelProp ?? messages.labels[kind];
    const date = toDate(at);
    const details = messages.details({
      kind,
      ...(model ? { model } : {}),
      ...(date ? { at: messages.formatDate(date) } : {}),
    });
    const tip: ReactNode =
      tooltip ?? (iconOnly ? (details ? `${label}. ${details}` : label) : details || null);
    const hasTip = tip !== null && tip !== undefined && tip !== '';

    const badge = (
      <span
        ref={ref}
        {...rest}
        // Focusable only so the tooltip can open from the keyboard.
        tabIndex={hasTip ? (rest.tabIndex ?? 0) : rest.tabIndex}
        className={cx('mn-ai-generated-badge', className)}
        data-kind={kind}
        data-size={size}
        data-icon-only={iconOnly || undefined}
      >
        <AiBotGlyph className="mn-ai-generated-badge__icon" />
        <span className={iconOnly ? 'mn-visually-hidden' : 'mn-ai-generated-badge__label'}>
          {label}
        </span>
        {details ? <span className="mn-visually-hidden">{`, ${details}`}</span> : null}
      </span>
    );

    if (!hasTip) return badge;
    return (
      <TooltipProvider>
        <Tooltip content={tip}>{badge}</Tooltip>
      </TooltipProvider>
    );
  },
);
