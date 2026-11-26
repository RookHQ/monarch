'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CalendarGlyph, CheckGlyph, PlusGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';

/** What the tool did. Picks the icon and the small label above the title. */
export type AiToolResultKind =
  'record-created' | 'email-sent' | 'event-booked' | 'ticket-filed' | 'generic';

/** Every user-visible string in AiToolResultCard. */
export interface AiToolResultCardMessages {
  /** The label above the title per kind. */
  kinds: Record<AiToolResultKind, string>;
  /** The open link's visible text. Default "Open". */
  open: string;
  /** Undo button. Default "Undo". */
  undo: string;
  /** Shown after a successful undo. Default "Undone". */
  undone: string;
  /** Shown when `onUndo` throws or rejects. Default "Couldn’t undo. Try again." */
  undoFailed: string;
}

export const AI_TOOL_RESULT_CARD_MESSAGES: AiToolResultCardMessages = {
  kinds: {
    'record-created': 'Record created',
    'email-sent': 'Email sent',
    'event-booked': 'Event booked',
    'ticket-filed': 'Ticket filed',
    generic: 'Done',
  },
  open: 'Open',
  undo: 'Undo',
  undone: 'Undone',
  undoFailed: 'Couldn’t undo. Try again.',
};

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

function MailGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="2" y="3.5" width="12" height="9" rx="1.5" />
      <path d="M2.5 4.5 8 9l5.5-4.5" />
    </svg>
  );
}

function TicketGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M2 5.5V4h12v1.5a1.5 1.5 0 0 0 0 3V12H2V8.5a1.5 1.5 0 0 0 0-3Z" />
      <path d="M9.5 4v8" strokeDasharray="1.5 1.5" />
    </svg>
  );
}

const KIND_ICON: Record<AiToolResultKind, ReactNode> = {
  'record-created': <PlusGlyph />,
  'email-sent': <MailGlyph />,
  'event-booked': <CalendarGlyph />,
  'ticket-filed': <TicketGlyph />,
  generic: <CheckGlyph />,
};

export interface AiToolResultCardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What the tool did. Default `generic`. */
  kind?: AiToolResultKind;
  /** The thing it made or changed, e.g. the record's name or the email's subject. */
  title: ReactNode;
  /** One line of detail, e.g. "To 3 people" or "Tue 14 Oct, 10:00–10:30". */
  description?: ReactNode;
  /**
   * Opens the record. Only relative, http(s) and mailto URLs are linked;
   * anything else is dropped. The link gets `rel="noopener noreferrer nofollow"`.
   */
  href?: string | undefined;
  /** Opens `href` in a new tab. Default false. */
  external?: boolean;
  /** Short facts shown as a list of label/value pairs. */
  meta?: readonly { label: ReactNode; value: ReactNode }[] | undefined;
  /** Replaces the kind's icon. Decorative. */
  icon?: ReactNode;
  /**
   * Shows Undo. May return a promise: the button shows a spinner until it
   * settles, then the card shows "Undone" (or an error to try again).
   */
  onUndo?: (() => unknown) | undefined;
  /** Replaces any of the English strings. `kinds` merges per kind. */
  messages?: Partial<Omit<AiToolResultCardMessages, 'kinds'>> & {
    kinds?: Partial<Record<AiToolResultKind, string>>;
  };
}

/**
 * A rich preview of what a tool did (a record created, an email sent, an
 * event booked, a ticket filed): what kind of action it was, the title, a
 * line of detail, key facts, a link to open the result and an optional Undo.
 */
export const AiToolResultCard = forwardRef<HTMLDivElement, AiToolResultCardProps>(
  function AiToolResultCard(
    {
      kind = 'generic',
      title,
      description,
      href,
      external = false,
      meta,
      icon,
      onUndo,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages: AiToolResultCardMessages = {
      ...AI_TOOL_RESULT_CARD_MESSAGES,
      ...messagesProp,
      kinds: { ...AI_TOOL_RESULT_CARD_MESSAGES.kinds, ...messagesProp?.kinds },
    };
    const id = useId();
    const titleId = `${id}-title`;
    const [undo, setUndo] = useState<'idle' | 'pending' | 'done' | 'failed'>('idle');
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const link = href && isSafeUrl(href) ? href : undefined;
    const rootRef = useRef<HTMLDivElement | null>(null);
    // Undo disappears once it worked: keep focus in the card rather than on the body.
    useEffect(() => {
      if (
        undo === 'done' &&
        typeof document !== 'undefined' &&
        document.activeElement === document.body
      )
        rootRef.current?.focus();
    }, [undo]);

    const runUndo = () => {
      if (!onUndo || undo === 'pending' || undo === 'done') return;
      setUndo('pending');
      let result: unknown;
      try {
        result = onUndo();
      } catch {
        setUndo('failed');
        return;
      }
      Promise.resolve(result).then(
        () => mounted.current && setUndo('done'),
        () => mounted.current && setUndo('failed'),
      );
    };

    return (
      <div
        ref={(node) => {
          rootRef.current = node;
          if (typeof ref === 'function') ref(node);
          else if (ref) ref.current = node;
        }}
        role="group"
        tabIndex={undo === 'done' ? -1 : undefined}
        aria-labelledby={titleId}
        {...rest}
        className={cx('mn-ai-tool-result-card', className)}
        data-kind={kind}
        data-undone={undo === 'done' || undefined}
      >
        <span className="mn-ai-tool-result-card__icon" aria-hidden>
          {icon ?? KIND_ICON[kind]}
        </span>
        <div className="mn-ai-tool-result-card__content">
          <p className="mn-ai-tool-result-card__kind">{messages.kinds[kind]}</p>
          <p id={titleId} className="mn-ai-tool-result-card__title">
            {title}
          </p>
          {description ? (
            <div className="mn-ai-tool-result-card__description">{description}</div>
          ) : null}
          {meta && meta.length ? (
            <dl className="mn-ai-tool-result-card__meta">
              {meta.map((item, i) => (
                <div key={i} className="mn-ai-tool-result-card__meta-item">
                  <dt>{item.label}</dt>
                  <dd>{item.value}</dd>
                </div>
              ))}
            </dl>
          ) : null}
        </div>
        {link || onUndo ? (
          <div className="mn-ai-tool-result-card__actions">
            {link && undo !== 'done' ? (
              <a
                className="mn-ai-tool-result-card__open"
                href={link}
                rel="noopener noreferrer nofollow"
                {...(external ? { target: '_blank' } : {})}
                aria-describedby={titleId}
              >
                {messages.open}
              </a>
            ) : null}
            {onUndo && undo !== 'done' ? (
              <Button size="sm" variant="ghost" loading={undo === 'pending'} onClick={runUndo}>
                {messages.undo}
              </Button>
            ) : null}
          </div>
        ) : null}
        <span
          role="status"
          className={cx(
            'mn-ai-tool-result-card__status',
            undo === 'done' || undo === 'failed' ? undefined : 'mn-visually-hidden',
          )}
          data-status={undo}
        >
          {undo === 'done' ? messages.undone : undo === 'failed' ? messages.undoFailed : ''}
        </span>
      </div>
    );
  },
);
