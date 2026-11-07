import { forwardRef, useId, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { SuccessGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Skeleton } from '../skeleton/index.js';

/** What happens to the thing an item describes. */
export type ConsequenceKind = 'removed' | 'kept' | 'changed' | 'warning';

/** One consequence of an action. */
export interface ConsequenceItem {
  /** Stable key. Falls back to the item's position. */
  id?: string;
  /**
   * `removed` (deleted or lost, danger tone), `kept` (stays as it is),
   * `changed` (altered or moved) or `warning` (something to know first).
   */
  kind: ConsequenceKind;
  /** What happens, in plain words: "tasks will be deleted", "Members lose access immediately". */
  label: ReactNode;
  /** A second, quieter line with specifics. */
  detail?: ReactNode;
  /**
   * How many things are affected. Formatted with `Intl.NumberFormat` in
   * `locale` and shown in tabular numerals before the label.
   */
  count?: number;
}

/** Visible and spoken strings, for translation. */
export interface ConsequenceListMessages {
  /** Group heading for `removed` items. Default "Will be deleted". */
  groupRemoved: string;
  /** Group heading for `changed` items. Default "Will change". */
  groupChanged: string;
  /** Group heading for `warning` items. Default "Before you continue". */
  groupWarning: string;
  /** Group heading for `kept` items. Default "Will be kept". */
  groupKept: string;
  /** Spoken before an ungrouped `removed` item. Default "Removed". */
  kindRemoved: string;
  /** Spoken before an ungrouped `changed` item. Default "Changed". */
  kindChanged: string;
  /** Spoken before an ungrouped `warning` item. Default "Warning". */
  kindWarning: string;
  /** Spoken before an ungrouped `kept` item. Default "Kept". */
  kindKept: string;
  /** Announced while `loading`. Default "Checking what will change". */
  loading: string;
}

export const CONSEQUENCE_LIST_MESSAGES: ConsequenceListMessages = {
  groupRemoved: 'Will be deleted',
  groupChanged: 'Will change',
  groupWarning: 'Before you continue',
  groupKept: 'Will be kept',
  kindRemoved: 'Removed',
  kindChanged: 'Changed',
  kindWarning: 'Warning',
  kindKept: 'Kept',
  loading: 'Checking what will change',
};

type HeadingLevel = 'h2' | 'h3' | 'h4' | 'h5' | 'p';

export interface ConsequenceListProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The consequences, in the order they should be read. */
  items: ConsequenceItem[];
  /** Optional heading above the list, such as "What happens". Names the list. */
  heading?: ReactNode;
  /** Element for `heading`. Default `h3`. Group headings use the next level down. */
  headingLevel?: HeadingLevel;
  /**
   * `kind` splits the items into groups under sub-headings ("Will be deleted",
   * "Will change", "Before you continue", "Will be kept"), in that order.
   */
  groupBy?: 'kind';
  /** Locale for formatting `count`. Default: the runtime's locale. */
  locale?: string;
  /**
   * Shows skeleton rows instead of the items while impact counts load.
   * Sets `aria-busy` and announces `messages.loading`. Exposed as `data-state="loading"`.
   */
  loading?: boolean;
  /** Number of skeleton rows while `loading`. Default 3. */
  loadingRows?: number;
  /**
   * Shown instead of the items when the consequences could not be worked out,
   * for example an `InlineErrorRetry`. Ignored while `loading`. Exposed as
   * `data-state="error"`.
   */
  error?: ReactNode;
  /** Overrides for the default English strings. */
  messages?: Partial<ConsequenceListMessages>;
}

const GROUP_ORDER: ConsequenceKind[] = ['removed', 'changed', 'warning', 'kept'];

const NEXT_LEVEL: Record<HeadingLevel, HeadingLevel> = {
  h2: 'h3',
  h3: 'h4',
  h4: 'h5',
  h5: 'p',
  p: 'p',
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

/** A circle with a minus: "goes away". */
function RemovedGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M5.25 8h5.5" />
    </svg>
  );
}

/** A circle with two opposed arrows: "becomes something else". */
function ChangedGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M5 6.5h5.5l-1.5-1.5M11 9.5H5.5L7 11" />
    </svg>
  );
}

const GLYPHS: Record<ConsequenceKind, (p: SVGProps<SVGSVGElement>) => ReactNode> = {
  removed: RemovedGlyph,
  changed: ChangedGlyph,
  warning: WarningGlyph,
  kept: SuccessGlyph,
};

function kindName(kind: ConsequenceKind, m: ConsequenceListMessages): string {
  return {
    removed: m.kindRemoved,
    changed: m.kindChanged,
    warning: m.kindWarning,
    kept: m.kindKept,
  }[kind];
}

function groupName(kind: ConsequenceKind, m: ConsequenceListMessages): string {
  return {
    removed: m.groupRemoved,
    changed: m.groupChanged,
    warning: m.groupWarning,
    kept: m.groupKept,
  }[kind];
}

/**
 * States plainly what an action will do: what is deleted, what is kept, what
 * changes and what to know first. Used inside confirm dialogs and destructive
 * flows. Every item has an icon and text, so colour is never the only signal.
 */
export const ConsequenceList = forwardRef<HTMLDivElement, ConsequenceListProps>(
  function ConsequenceList(
    {
      items,
      heading,
      headingLevel = 'h3',
      groupBy,
      locale,
      loading = false,
      loadingRows = 3,
      error,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...CONSEQUENCE_LIST_MESSAGES, ...messagesProp };
    const id = useId();
    const headingId = `${id}-heading`;
    const H = headingLevel;
    const G = heading ? NEXT_LEVEL[headingLevel] : headingLevel;
    const hasError = !loading && error !== undefined && error !== null && error !== false;
    const state = loading ? 'loading' : hasError ? 'error' : undefined;
    const format = new Intl.NumberFormat(locale);

    const renderItem = (item: ConsequenceItem, index: number, grouped: boolean) => {
      const Icon = GLYPHS[item.kind];
      return (
        <li key={item.id ?? index} className="mn-consequence-list__item" data-kind={item.kind}>
          <span className="mn-consequence-list__icon">
            <Icon />
          </span>
          <span className="mn-consequence-list__text">
            <span className="mn-consequence-list__label">
              {grouped ? null : (
                <span className="mn-visually-hidden">{`${kindName(item.kind, m)}: `}</span>
              )}
              {item.count !== undefined ? (
                <>
                  <span className="mn-consequence-list__count">
                    {format.format(item.count)}
                  </span>{' '}
                </>
              ) : null}
              {item.label}
            </span>
            {item.detail ? (
              <span className="mn-consequence-list__detail">{item.detail}</span>
            ) : null}
          </span>
        </li>
      );
    };

    let body: ReactNode;
    if (loading) {
      body = (
        <>
          <span className="mn-visually-hidden" role="status">
            {m.loading}
          </span>
          <div className="mn-consequence-list__skeleton" aria-hidden>
            {Array.from({ length: Math.max(1, loadingRows) }, (_, i) => (
              <div key={i} className="mn-consequence-list__skeleton-row">
                <Skeleton shape="circle" width="1rem" height="1rem" />
                <Skeleton shape="text" width={`${70 - ((i * 17) % 35)}%`} />
              </div>
            ))}
          </div>
        </>
      );
    } else if (hasError) {
      body = <div className="mn-consequence-list__error">{error}</div>;
    } else if (groupBy === 'kind') {
      body = GROUP_ORDER.map((kind) => {
        const group = items.filter((item) => item.kind === kind);
        if (group.length === 0) return null;
        const groupId = `${id}-${kind}`;
        return (
          <div key={kind} className="mn-consequence-list__group" data-kind={kind}>
            <G id={groupId} className="mn-consequence-list__group-heading">
              {groupName(kind, m)}
            </G>
            <ul className="mn-consequence-list__items" aria-labelledby={groupId}>
              {group.map((item, index) => renderItem(item, index, true))}
            </ul>
          </div>
        );
      });
    } else if (items.length > 0) {
      body = (
        <ul
          className="mn-consequence-list__items"
          aria-labelledby={heading ? headingId : undefined}
        >
          {items.map((item, index) => renderItem(item, index, false))}
        </ul>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        aria-busy={loading || undefined}
        data-state={state}
        data-grouped={groupBy === 'kind' || undefined}
        className={cx('mn-consequence-list', className)}
      >
        {heading ? (
          <H id={headingId} className="mn-consequence-list__heading">
            {heading}
          </H>
        ) : null}
        {body}
      </div>
    );
  },
);
