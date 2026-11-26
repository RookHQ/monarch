'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { formatNumber } from '../../../internal/format-number.js';
import { Button } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Input } from '../../forms/text-field/index.js';

/** One choice in a checkbox facet. */
export interface FacetOption {
  value: string;
  label: string;
  /** Matching items for this choice. Shown after the label and read with it. */
  count?: number | undefined;
  disabled?: boolean | undefined;
}

interface FacetBase {
  /** Key of this facet in the value object. */
  id: string;
  /** Group heading, such as "Status". */
  label: string;
  /** Start expanded. Default true. */
  defaultOpen?: boolean | undefined;
}

/** A list of checkboxes; the value is the array of checked option values. */
export interface CheckboxFacet extends FacetBase {
  type: 'checkbox';
  options: readonly FacetOption[];
  /** Options shown before "Show more". Default 6. */
  maxVisible?: number | undefined;
}

/** A minimum and maximum number; the value is `{ min?, max? }`. */
export interface RangeFacet extends FacetBase {
  type: 'range';
  /** Lowest allowed value (input `min`). */
  min?: number | undefined;
  /** Highest allowed value (input `max`). */
  max?: number | undefined;
  step?: number | undefined;
  /** Unit shown after the inputs and read with them, such as "days" or "MB". */
  unit?: string | undefined;
}

export type Facet = CheckboxFacet | RangeFacet;

/** A range facet's value. A missing bound means "no limit". */
export interface FacetRange {
  min?: number | undefined;
  max?: number | undefined;
}

/** The selected filters: option values for checkbox facets, a range for range facets. */
export type FacetValue = Record<string, readonly string[] | FacetRange | undefined>;

export interface FacetedFilterPanelMessages {
  /** Panel heading. Default "Filters". */
  title: string;
  /** Clear-all button. Default "Clear all". */
  clearAll: string;
  /** Visible per-group clear button. Default "Clear". */
  clearGroup: string;
  /** Accessible name of the per-group clear button. Default "Clear Status filter". */
  clearGroupLabel: (group: string) => string;
  /** Number of active filters in a group, shown on its toggle. Default "2 selected". */
  activeCount: (count: number) => string;
  /** Show-more button. Default "Show 4 more". */
  showMore: (hidden: number) => string;
  /** Show-fewer button. Default "Show fewer". */
  showFewer: string;
  /** Range minimum label. Default "Minimum". */
  min: string;
  /** Range maximum label. Default "Maximum". */
  max: string;
  /** Shown when the minimum is above the maximum. */
  rangeInvalid: string;
  /** Shown in a checkbox facet with no options. Default "No options". */
  noOptions: string;
}

const DEFAULT_MESSAGES: FacetedFilterPanelMessages = {
  title: 'Filters',
  clearAll: 'Clear all',
  clearGroup: 'Clear',
  clearGroupLabel: (group) => `Clear ${group} filter`,
  activeCount: (count) => `${count} selected`,
  showMore: (hidden) => `Show ${hidden} more`,
  showFewer: 'Show fewer',
  min: 'Minimum',
  max: 'Maximum',
  rangeInvalid: 'The minimum is above the maximum.',
  noOptions: 'No options',
};

export type FacetedFilterPanelHeadingLevel = 'h2' | 'h3' | 'h4' | 'h5' | 'h6';

export interface FacetedFilterPanelProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The facet groups, in display order. */
  facets: readonly Facet[];
  /** Selected filters (controlled). */
  value?: FacetValue | undefined;
  /** Initial filters (uncontrolled). */
  defaultValue?: FacetValue | undefined;
  /** Called with the whole new value after every change. */
  onValueChange?: ((value: FacetValue) => void) | undefined;
  /** Heading level of the panel title; group headings use the next level. Default `h2`. */
  headingLevel?: FacetedFilterPanelHeadingLevel | undefined;
  /** Hide the panel title row (keep the clear-all button). */
  hideTitle?: boolean | undefined;
  /** Locale for counts. Defaults to the runtime locale. */
  locale?: string | undefined;
  /** Text overrides. */
  messages?: Partial<FacetedFilterPanelMessages> | undefined;
  /** Extra content after the groups, such as an Apply button on mobile. */
  footer?: ReactNode;
}

/** Number of active filters in one facet. */
export function facetActiveCount(facet: Facet, value: FacetValue): number {
  const v = value[facet.id];
  if (!v) return 0;
  if (facet.type === 'checkbox') return Array.isArray(v) ? v.length : 0;
  const range = v as FacetRange;
  return range.min !== undefined || range.max !== undefined ? 1 : 0;
}

const NEXT: Record<FacetedFilterPanelHeadingLevel, 'h3' | 'h4' | 'h5' | 'h6'> = {
  h2: 'h3',
  h3: 'h4',
  h4: 'h5',
  h5: 'h6',
  h6: 'h6',
};

function parseBound(raw: string): number | undefined {
  if (raw.trim() === '') return undefined;
  const n = Number(raw);
  return Number.isFinite(n) ? n : undefined;
}

/**
 * A filter sidebar: collapsible facet groups of checkboxes with counts and
 * minimum/maximum ranges, with a clear button per group and a clear-all.
 * Controlled with `value`/`onValueChange` or uncontrolled with `defaultValue`.
 */
export const FacetedFilterPanel = forwardRef<HTMLDivElement, FacetedFilterPanelProps>(
  function FacetedFilterPanel(
    {
      facets,
      value: valueProp,
      defaultValue,
      onValueChange,
      headingLevel: Heading = 'h2',
      hideTitle = false,
      locale,
      messages: messagesProp,
      footer,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const uid = useId();
    const [inner, setInner] = useState<FacetValue>(defaultValue ?? {});
    const value = valueProp ?? inner;
    const GroupHeading = NEXT[Heading];
    const [open, setOpen] = useState<Record<string, boolean>>(() =>
      Object.fromEntries(facets.map((f) => [f.id, f.defaultOpen ?? true])),
    );
    const [expanded, setExpanded] = useState<Record<string, boolean>>({});

    const commit = (next: FacetValue) => {
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };
    const setFacet = (id: string, v: FacetValue[string]) => {
      const rest = Object.fromEntries(Object.entries(value).filter(([key]) => key !== id));
      commit(v === undefined ? rest : { ...rest, [id]: v });
    };

    const totalActive = facets.reduce((n, f) => n + facetActiveCount(f, value), 0);
    const titleId = `${uid}-title`;

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={hideTitle ? undefined : titleId}
        aria-label={hideTitle ? messages.title : undefined}
        {...rest}
        className={cx('mn-faceted-filter-panel', className)}
        data-active={totalActive > 0 ? '' : undefined}
      >
        <div className="mn-faceted-filter-panel__header">
          {hideTitle ? null : (
            <Heading id={titleId} className="mn-faceted-filter-panel__title">
              {messages.title}
            </Heading>
          )}
          <Button
            variant="ghost"
            size="sm"
            className="mn-faceted-filter-panel__clear-all"
            disabled={totalActive === 0}
            onClick={() => commit({})}
          >
            {messages.clearAll}
          </Button>
        </div>
        {facets.map((facet) => {
          const isOpen = open[facet.id] ?? facet.defaultOpen ?? true;
          const active = facetActiveCount(facet, value);
          const toggleId = `${uid}-${facet.id}-toggle`;
          const panelId = `${uid}-${facet.id}-panel`;
          return (
            <div
              key={facet.id}
              className="mn-faceted-filter-panel__group"
              data-state={isOpen ? 'open' : 'closed'}
              data-type={facet.type}
            >
              <div className="mn-faceted-filter-panel__group-header">
                <GroupHeading className="mn-faceted-filter-panel__group-heading">
                  <button
                    type="button"
                    id={toggleId}
                    className="mn-faceted-filter-panel__toggle"
                    aria-expanded={isOpen}
                    aria-controls={panelId}
                    onClick={() => setOpen((o) => ({ ...o, [facet.id]: !isOpen }))}
                  >
                    <ChevronDownGlyph className="mn-faceted-filter-panel__chevron" aria-hidden />
                    <span className="mn-faceted-filter-panel__group-label">{facet.label}</span>
                    {active > 0 ? (
                      <span className="mn-faceted-filter-panel__active">
                        {messages.activeCount(active)}
                      </span>
                    ) : null}
                  </button>
                </GroupHeading>
                {active > 0 ? (
                  <Button
                    variant="ghost"
                    size="xs"
                    className="mn-faceted-filter-panel__clear"
                    aria-label={messages.clearGroupLabel(facet.label)}
                    onClick={() => {
                      // This button goes away once the group is clear: focus moves to the
                      // group's own toggle instead of being dropped to the page.
                      document.getElementById(toggleId)?.focus();
                      setFacet(facet.id, undefined);
                    }}
                  >
                    {messages.clearGroup}
                  </Button>
                ) : null}
              </div>
              <div
                id={panelId}
                role="group"
                aria-labelledby={toggleId}
                className="mn-faceted-filter-panel__panel"
                hidden={!isOpen}
              >
                {facet.type === 'checkbox' ? (
                  <CheckboxGroup
                    facet={facet}
                    selected={(value[facet.id] as readonly string[] | undefined) ?? []}
                    expanded={expanded[facet.id] ?? false}
                    onExpandedChange={(e) => setExpanded((x) => ({ ...x, [facet.id]: e }))}
                    onChange={(next) => setFacet(facet.id, next.length ? next : undefined)}
                    locale={locale}
                    messages={messages}
                    idPrefix={`${uid}-${facet.id}`}
                  />
                ) : (
                  <RangeGroup
                    facet={facet}
                    range={(value[facet.id] as FacetRange | undefined) ?? {}}
                    onChange={(next) =>
                      setFacet(
                        facet.id,
                        next.min === undefined && next.max === undefined ? undefined : next,
                      )
                    }
                    messages={messages}
                    idPrefix={`${uid}-${facet.id}`}
                  />
                )}
              </div>
            </div>
          );
        })}
        {footer ? <div className="mn-faceted-filter-panel__footer">{footer}</div> : null}
      </div>
    );
  },
);

function CheckboxGroup({
  facet,
  selected,
  expanded,
  onExpandedChange,
  onChange,
  locale,
  messages,
  idPrefix,
}: {
  facet: CheckboxFacet;
  selected: readonly string[];
  expanded: boolean;
  onExpandedChange: (expanded: boolean) => void;
  onChange: (next: string[]) => void;
  locale: string | undefined;
  messages: FacetedFilterPanelMessages;
  idPrefix: string;
}) {
  if (!facet.options.length)
    return <p className="mn-faceted-filter-panel__empty">{messages.noOptions}</p>;
  const limit = facet.maxVisible ?? 6;
  // Checked options always stay visible, even beyond the limit.
  const visible = expanded
    ? facet.options
    : facet.options.filter((o, i) => i < limit || selected.includes(o.value));
  const hidden = facet.options.length - visible.length;
  // Position of each option in the full list (its id), looked up once rather than per row.
  const positions = new Map(facet.options.map((o, i) => [o, i]));
  return (
    <>
      <ul className="mn-faceted-filter-panel__options">
        {visible.map((option) => {
          const index = positions.get(option) ?? 0;
          const checked = selected.includes(option.value);
          return (
            <li
              key={option.value}
              className="mn-faceted-filter-panel__option"
              data-empty={option.count === 0 ? '' : undefined}
            >
              <Checkbox
                id={`${idPrefix}-option-${index}`}
                checked={checked}
                disabled={option.disabled}
                onCheckedChange={(c) =>
                  onChange(
                    c === true
                      ? [...selected, option.value]
                      : selected.filter((v) => v !== option.value),
                  )
                }
                label={
                  <span className="mn-faceted-filter-panel__option-label">
                    <span className="mn-faceted-filter-panel__option-text">{option.label}</span>
                    {option.count !== undefined ? (
                      <span className="mn-faceted-filter-panel__count">
                        {formatNumber(option.count, locale ? { locale } : {})}
                      </span>
                    ) : null}
                  </span>
                }
              />
            </li>
          );
        })}
      </ul>
      {hidden > 0 || expanded ? (
        <button
          type="button"
          className="mn-faceted-filter-panel__more"
          aria-expanded={expanded}
          onClick={() => onExpandedChange(!expanded)}
        >
          {expanded ? messages.showFewer : messages.showMore(hidden)}
        </button>
      ) : null}
    </>
  );
}

function RangeGroup({
  facet,
  range,
  onChange,
  messages,
  idPrefix,
}: {
  facet: RangeFacet;
  range: FacetRange;
  onChange: (next: FacetRange) => void;
  messages: FacetedFilterPanelMessages;
  idPrefix: string;
}) {
  // Drafts keep what the user typed (such as "-" or "1.") while it is not yet a number.
  const [draft, setDraft] = useState<{ min?: string; max?: string }>({});
  const invalid = range.min !== undefined && range.max !== undefined && range.min > range.max;
  const errorId = `${idPrefix}-error`;
  const unitId = facet.unit ? `${idPrefix}-unit` : undefined;
  const field = (bound: 'min' | 'max') => {
    const id = `${idPrefix}-${bound}`;
    const shown = draft[bound] ?? (range[bound] === undefined ? '' : String(range[bound]));
    return (
      <div className="mn-faceted-filter-panel__bound">
        <label htmlFor={id} className="mn-faceted-filter-panel__bound-label">
          {bound === 'min' ? messages.min : messages.max}
        </label>
        <Input
          id={id}
          type="number"
          inputMode="decimal"
          size="sm"
          min={facet.min}
          max={facet.max}
          step={facet.step}
          value={shown}
          invalid={invalid}
          aria-describedby={cx(unitId, invalid && errorId) || undefined}
          onChange={(e) => {
            const raw = e.target.value;
            setDraft((d) => ({ ...d, [bound]: raw }));
            onChange({ ...range, [bound]: parseBound(raw) });
          }}
          onBlur={() => setDraft((d) => ({ ...d, [bound]: undefined }))}
        />
      </div>
    );
  };
  return (
    <>
      <div className="mn-faceted-filter-panel__range">
        {field('min')}
        <span className="mn-faceted-filter-panel__range-dash" aria-hidden="true">
          –
        </span>
        {field('max')}
        {facet.unit ? (
          <span id={unitId} className="mn-faceted-filter-panel__unit">
            {facet.unit}
          </span>
        ) : null}
      </div>
      {invalid ? (
        <p id={errorId} className="mn-faceted-filter-panel__error" role="alert">
          {messages.rangeInvalid}
        </p>
      ) : null}
    </>
  );
}
