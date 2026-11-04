'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, PlusGlyph } from '../../../internal/icons.js';
import { SearchInput } from '../../search/search-input/index.js';

/** One template in the gallery. The product owns what applying it does. */
export interface TemplatePickerTemplate {
  /** Stable id, passed to `onValueChange`. */
  id: string;
  /** Short name, the card's accessible name. */
  name: string;
  /** One or two lines on what it is for. */
  description?: ReactNode;
  /** Id of a category in `categories`. */
  category?: string;
  /** Thumbnail drawn by the product (image or mini layout). Decorative: hidden from assistive technology. */
  preview?: ReactNode;
  /** Small marker such as "Built-in" or "New". Read as part of the description. */
  badge?: ReactNode;
  /** Extra words the search matches, besides the name and string descriptions. */
  keywords?: readonly string[];
  disabled?: boolean;
}

/** A category filter. */
export interface TemplatePickerCategory {
  id: string;
  label: string;
}

/** The "start from scratch" card, shown first. */
export interface TemplatePickerBlank {
  /** Default `blank`. */
  id?: string;
  /** Default "Blank". */
  name?: string;
  description?: ReactNode;
  /** Default: a plus mark. */
  preview?: ReactNode;
}

/** Visible and announced strings. Override them to translate. */
export interface TemplatePickerMessages {
  searchLabel: string;
  searchPlaceholder: string;
  /** Accessible name of the category filter group. */
  categoriesLabel: string;
  allCategories: string;
  blank: string;
  /** Visually hidden result count, announced as the filter changes. */
  results: (count: number) => string;
  noResults: (query: string) => string;
  /** Accessible name of the preview pane. */
  previewLabel: string;
}

const DEFAULT_MESSAGES: TemplatePickerMessages = {
  searchLabel: 'Search templates',
  searchPlaceholder: 'Search templates',
  categoriesLabel: 'Categories',
  allCategories: 'All',
  blank: 'Blank',
  results: (n) => (n === 1 ? '1 template' : `${n} templates`),
  noResults: (q) => `No templates match “${q}”.`,
  previewLabel: 'Preview',
};

const textOf = (node: ReactNode) => (typeof node === 'string' ? node : '');

/** Whether a template matches a search query (name, string description and keywords, any order). */
export function templateMatches(template: TemplatePickerTemplate, query: string): boolean {
  const words = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const hay = [template.name, textOf(template.description), ...(template.keywords ?? [])]
    .join(' ')
    .toLowerCase();
  return words.every((w) => hay.includes(w));
}

/** Columns in the first row of a rendered grid, from the items' positions. */
function columnsOf(items: HTMLElement[]): number {
  const first = items[0];
  if (!first) return 1;
  let n = 0;
  for (const el of items) {
    if (el.offsetTop !== first.offsetTop) break;
    n += 1;
  }
  return Math.max(1, n);
}

export interface TemplatePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The templates, from the product. */
  templates: readonly TemplatePickerTemplate[];
  /** Category filters, in order. Shown when given (with an "All" filter first). */
  categories?: readonly TemplatePickerCategory[];
  /** Selected template id (controlled); `null` for none. */
  value?: string | null;
  /** Initial selection (uncontrolled). */
  defaultValue?: string | null;
  /** Called with the chosen id and template (`null` for the blank card). */
  onValueChange?: (id: string, template: TemplatePickerTemplate | null) => void;
  /** Adds a "start from scratch" card first. `true` uses the defaults. */
  blank?: boolean | TemplatePickerBlank;
  /** Show the search box. Default true. */
  searchable?: boolean;
  /** Initial category id. Default: all. */
  defaultCategory?: string;
  /**
   * Larger preview of the selected template, shown beside the grid (below it
   * on narrow screens). Receives `null` for the blank card.
   */
  renderPreview?: (template: TemplatePickerTemplate | null) => ReactNode;
  /** Narrowest a card gets before the grid drops a column. Default `9rem`. */
  minCardWidth?: string;
  /** Override visible and announced strings. */
  messages?: Partial<TemplatePickerMessages>;
}

/**
 * Choose a template from a gallery: category filters, search, a blank
 * option and an optional preview pane. The cards are a radio group with one
 * Tab stop; arrow keys move through the grid by row and column and select as
 * they go. The product supplies the templates and applies the choice.
 */
export const TemplatePicker = forwardRef<HTMLDivElement, TemplatePickerProps>(
  function TemplatePicker(
    {
      templates,
      categories,
      value: valueProp,
      defaultValue = null,
      onValueChange,
      blank,
      searchable = true,
      defaultCategory,
      renderPreview,
      minCardWidth = '9rem',
      messages: messagesProp,
      className,
      style,
      'aria-label': ariaLabel = 'Templates',
      'aria-labelledby': ariaLabelledBy,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [inner, setInner] = useState<string | null>(defaultValue);
    const controlled = valueProp !== undefined;
    const value = controlled ? valueProp : inner;
    const [query, setQuery] = useState('');
    const [category, setCategory] = useState<string | null>(defaultCategory ?? null);
    const gridRef = useRef<HTMLDivElement>(null);
    const auto = useId();

    const blankCard:
      | (Required<Omit<TemplatePickerBlank, 'description'>> &
          Pick<TemplatePickerBlank, 'description'>)
      | null = blank
      ? {
          id: (typeof blank === 'object' && blank.id) || 'blank',
          name: (typeof blank === 'object' && blank.name) || messages.blank,
          description: typeof blank === 'object' ? blank.description : undefined,
          preview: (typeof blank === 'object' && blank.preview) || (
            <span className="mn-template-picker__blank-mark">
              <PlusGlyph />
            </span>
          ),
        }
      : null;

    const shown = templates.filter(
      (t) => (category === null || t.category === category) && templateMatches(t, query),
    );
    type Card = { id: string; template: TemplatePickerTemplate | null; disabled: boolean };
    const cards: Card[] = [
      ...(blankCard ? [{ id: blankCard.id, template: null, disabled: false }] : []),
      ...shown.map((t) => ({ id: t.id, template: t, disabled: Boolean(t.disabled) })),
    ];
    const enabled = cards.filter((c) => !c.disabled);
    const tabStop = enabled.find((c) => c.id === value)?.id ?? enabled[0]?.id;

    const select = (card: Card) => {
      if (card.disabled) return;
      if (!controlled) setInner(card.id);
      onValueChange?.(card.id, card.template);
    };

    const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
      const items = Array.from(
        gridRef.current?.querySelectorAll<HTMLElement>('.mn-template-picker__card') ?? [],
      );
      const cols = columnsOf(items);
      const rtl = getComputedStyle(event.currentTarget).direction === 'rtl';
      const step = (from: number, delta: number, wrap: boolean): number | null => {
        let i = from;
        for (let n = 0; n < cards.length; n += 1) {
          i += delta;
          if (wrap) i = (i + cards.length) % cards.length;
          if (i < 0 || i >= cards.length) return null;
          if (!cards[i]?.disabled) return i;
        }
        return null;
      };
      let next: number | null = null;
      switch (event.key) {
        case 'ArrowRight':
          next = step(index, rtl ? -1 : 1, true);
          break;
        case 'ArrowLeft':
          next = step(index, rtl ? 1 : -1, true);
          break;
        case 'ArrowDown':
          next = step(index, cols, false);
          break;
        case 'ArrowUp':
          next = step(index, -cols, false);
          break;
        case 'Home':
          next = step(-1, 1, false);
          break;
        case 'End':
          next = step(cards.length, -1, false);
          break;
        default:
          return;
      }
      event.preventDefault();
      if (next === null) return;
      const card = cards[next] as Card;
      select(card);
      items[next]?.focus();
    };

    const selected =
      value === null
        ? undefined
        : blankCard && value === blankCard.id
          ? null
          : templates.find((t) => t.id === value);

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-template-picker', className)}
        data-has-preview={renderPreview ? '' : undefined}
        style={{ ...style, ['--mn-template-picker-min' as string]: minCardWidth } as CSSProperties}
      >
        {searchable || categories?.length ? (
          <div className="mn-template-picker__filters">
            {searchable ? (
              <SearchInput
                size="sm"
                aria-label={messages.searchLabel}
                placeholder={messages.searchPlaceholder}
                value={query}
                onValueChange={setQuery}
                className="mn-template-picker__search"
              />
            ) : null}
            {categories?.length ? (
              <div
                role="group"
                aria-label={messages.categoriesLabel}
                className="mn-template-picker__categories"
              >
                {[{ id: null, label: messages.allCategories }, ...categories].map((c) => (
                  <button
                    key={c.id ?? '__all'}
                    type="button"
                    className="mn-template-picker__category"
                    aria-pressed={category === c.id}
                    onClick={() => setCategory(c.id)}
                  >
                    {c.label}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        ) : null}

        <p className="mn-visually-hidden" role="status">
          {query || category !== null ? messages.results(shown.length) : ''}
        </p>

        <div className="mn-template-picker__body">
          <div
            ref={gridRef}
            role="radiogroup"
            aria-label={ariaLabelledBy ? undefined : ariaLabel}
            aria-labelledby={ariaLabelledBy}
            className="mn-template-picker__grid"
          >
            {cards.map((card, index) => {
              const t = card.template;
              const name = t ? t.name : (blankCard?.name ?? '');
              const description = t ? t.description : blankCard?.description;
              const preview = t ? t.preview : blankCard?.preview;
              const checked = card.id === value;
              const nameId = `${auto}-${index}-name`;
              const descId = `${auto}-${index}-desc`;
              const hasDesc = Boolean(description) || Boolean(t?.badge);
              return (
                <button
                  key={card.id}
                  type="button"
                  role="radio"
                  aria-checked={checked}
                  aria-labelledby={nameId}
                  aria-describedby={hasDesc ? descId : undefined}
                  disabled={card.disabled}
                  tabIndex={card.id === tabStop ? 0 : -1}
                  className="mn-template-picker__card"
                  data-blank={t ? undefined : ''}
                  data-state={checked ? 'checked' : 'unchecked'}
                  onClick={() => select(card)}
                  onKeyDown={(e) => onKeyDown(e, index)}
                >
                  {preview ? (
                    <span className="mn-template-picker__media" aria-hidden>
                      {preview}
                    </span>
                  ) : null}
                  {checked ? (
                    <span className="mn-template-picker__check" aria-hidden>
                      <CheckGlyph />
                    </span>
                  ) : null}
                  <span className="mn-template-picker__text">
                    <span id={nameId} className="mn-template-picker__name">
                      {name}
                    </span>
                    {hasDesc ? (
                      <span id={descId} className="mn-template-picker__description">
                        {t?.badge ? (
                          <>
                            <span className="mn-template-picker__badge">{t.badge}</span>{' '}
                          </>
                        ) : null}
                        {description}
                      </span>
                    ) : null}
                  </span>
                </button>
              );
            })}
          </div>
          {renderPreview && selected !== undefined ? (
            <section className="mn-template-picker__preview" aria-label={messages.previewLabel}>
              {renderPreview(selected)}
            </section>
          ) : null}
        </div>

        {shown.length === 0 && (query || category !== null) ? (
          <p className="mn-template-picker__empty">
            {query ? messages.noResults(query.trim()) : messages.results(0)}
          </p>
        ) : null}
      </div>
    );
  },
);
