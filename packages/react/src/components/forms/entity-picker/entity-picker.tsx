'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';
import {
  Combobox,
  type ComboboxClearOption,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxOptionState,
  type ComboboxSearchContext,
} from '../combobox/index.js';

/** Passed to a `footer` function. */
export interface EntityPickerFooterContext {
  /** Closes the picker and returns focus to the trigger. */
  close: () => void;
  /** The text in the search box. */
  query: string;
}

export interface EntityPickerProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'value' | 'defaultValue' | 'onChange' | 'children' | 'type'
> {
  /** The records to pick from. With `onSearch`, shown until the first search answers. */
  options?: ComboboxOption[] | undefined;
  /** Load records from a server; see Combobox `onSearch`. Debounced, aborted when superseded. */
  onSearch?:
    ((query: string, context: ComboboxSearchContext) => Promise<ComboboxOption[]>) | undefined;
  /** Milliseconds to wait after typing before `onSearch`. Default 250. */
  searchDebounce?: number | undefined;
  /** Shortest query `onSearch` runs for. Default 0. */
  minSearchLength?: number | undefined;
  /** Selected record id (controlled). Single-select only. */
  value?: string | null | undefined;
  defaultValue?: string | null | undefined;
  onValueChange?: ((value: string | null) => void) | undefined;
  /** Pick several records; the list stays open after each pick. */
  multiple?: boolean | undefined;
  values?: string[] | undefined;
  defaultValues?: string[] | undefined;
  onValuesChange?: ((values: string[]) => void) | undefined;
  /** Records for the current value(s) when they may not be in `options` (async lists). Used for labels. */
  valueOptions?: ComboboxOption[] | undefined;
  /** Adds a "Create “query”" option; return the new record to select it. */
  onCreate?:
    | ((
        query: string,
      ) => ComboboxOption | null | undefined | Promise<ComboboxOption | null | undefined>)
    | undefined;
  /** An option at the top that clears the selection, such as "Unassigned" or "No project". */
  clearOption?: ComboboxClearOption | undefined;
  /** Custom option content. Default: media, label, description, hint and a check mark. */
  renderOption?: ((option: ComboboxOption, state: ComboboxOptionState) => ReactNode) | undefined;
  /** Custom filter for static `options`. Default: label, hint and keywords. */
  filter?: ((option: ComboboxOption, query: string) => boolean) | undefined;
  /** Rank matches whose label starts with the query first. Default true. */
  sortByMatch?: boolean | undefined;
  /** Render at most this many matches, with a message to keep typing. */
  limit?: number | undefined;
  /** Shown while records load. */
  loading?: boolean | undefined;
  /** Shown when nothing matches. */
  emptyMessage?: ReactNode | undefined;
  /** Trigger text when nothing is selected. Default "Select…". */
  placeholder?: string | undefined;
  /** Placeholder of the search box. Default "Search…". */
  searchPlaceholder?: string | undefined;
  /** Accessible name of the search box. Default: `searchPlaceholder`. */
  searchLabel?: string | undefined;
  /** Content under the list, such as "Manage projects". A function gets `close` and the query. */
  footer?: (ReactNode | ((context: EntityPickerFooterContext) => ReactNode)) | undefined;
  /**
   * A custom trigger element (a chip, an icon button). It must accept a ref and
   * name itself, for example `aria-label="Status: Open"`. Default: a field-style button.
   */
  trigger?: ReactElement | undefined;
  /** Content of the default trigger for the current selection. Default: media and label, or "first +N". */
  renderValue?: ((selected: ComboboxOption[]) => ReactNode) | undefined;
  /** Whether the picker is open (controlled). */
  open?: boolean | undefined;
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Popover alignment against the trigger. Default `start`. */
  align?: 'start' | 'center' | 'end' | undefined;
  size?: 'sm' | 'md' | 'lg' | undefined;
  invalid?: boolean | undefined;
  /** Close after a pick. Default: true, or false with `multiple`. */
  closeOnSelect?: boolean | undefined;
  /** Class on the popover panel. */
  contentClassName?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ComboboxMessages> | undefined;
}

/**
 * Pick a record (a project, contact, company, team…) from a searchable list in
 * a popover. The product supplies the records, search, rendering and creation;
 * EntityPicker supplies the trigger, the popover, keyboard support and the
 * single or multiple selection model. Built on Combobox (`inline`).
 */
export const EntityPicker = forwardRef<HTMLButtonElement, EntityPickerProps>(function EntityPicker(
  {
    options = [],
    onSearch,
    searchDebounce,
    minSearchLength,
    value: valueProp,
    defaultValue = null,
    onValueChange,
    multiple = false,
    values: valuesProp,
    defaultValues,
    onValuesChange,
    valueOptions,
    onCreate,
    clearOption,
    renderOption,
    filter,
    sortByMatch = true,
    limit,
    loading,
    emptyMessage,
    placeholder = 'Select…',
    searchPlaceholder = 'Search…',
    searchLabel,
    footer,
    trigger,
    renderValue,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    align = 'start',
    size = 'md',
    invalid,
    closeOnSelect,
    contentClassName,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const id = useId();
  const valueId = `mn-entity-picker-${id}-value`;
  const searchId = `mn-entity-picker-${id}-search`;
  const panelId = `mn-entity-picker-${id}-panel`;
  const field = useFieldContext();
  // A button takes no `required` attribute; `aria-required` from the Field stays.
  const { required: _required, ...fieldProps } = useFieldControl(
    rest as typeof rest & { required?: boolean },
    { invalid },
  );
  const triggerId = fieldProps.id ?? `mn-entity-picker-${id}-trigger`;
  const shouldClose = closeOnSelect ?? !multiple;

  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const setOpen = useCallback(
    (next: boolean) => {
      setOpenState(next);
      onOpenChange?.(next);
    },
    [onOpenChange],
  );

  const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
  const [internalValues, setInternalValues] = useState<string[]>(defaultValues ?? []);
  const value = valueProp !== undefined ? valueProp : internalValue;
  const values = valuesProp ?? internalValues;
  const [query, setQuery] = useState('');

  // Labels for the trigger: every record seen in options, value options, search results or creations.
  const known = useRef(new Map<string, ComboboxOption>());
  for (const o of valueOptions ?? []) known.current.set(o.value, o);
  for (const o of options) known.current.set(o.value, o);
  const optionFor = (v: string) =>
    options.find((o) => o.value === v) ?? known.current.get(v) ?? { value: v, label: v };
  const selectedValues = multiple ? values : value != null ? [value] : [];
  const selected = selectedValues.map(optionFor);

  const search = onSearch
    ? async (q: string, ctx: ComboboxSearchContext) => {
        const found = await onSearch(q, ctx);
        for (const o of found) known.current.set(o.value, o);
        return found;
      }
    : undefined;
  const create = onCreate
    ? async (q: string) => {
        const made = await onCreate(q);
        if (made) known.current.set(made.value, made);
        return made;
      }
    : undefined;

  const close = () => setOpen(false);

  let summary: ReactNode;
  if (selected.length === 0) summary = placeholder;
  else if (renderValue) summary = renderValue(selected);
  else if (selected.length === 1 && selected[0]) {
    const only = selected[0];
    summary = (
      <>
        {only.media ? (
          <span className="mn-entity-picker__media" aria-hidden>
            {only.media}
          </span>
        ) : null}
        <span className="mn-entity-picker__label">{only.label}</span>
      </>
    );
  } else {
    summary = (
      <>
        <span className="mn-entity-picker__label">{selected[0]?.label}</span>
        <span className="mn-entity-picker__more">+{selected.length - 1}</span>
      </>
    );
  }

  const ariaLabel = rest['aria-label'];
  // A select-only combobox (APG): named by the label, its value is the visible content.
  const labelledBy = rest['aria-labelledby'] ?? (field && !ariaLabel ? field.labelId : undefined);
  const dialogName = ariaLabel ?? (field ? undefined : placeholder);

  const defaultTrigger = (
    <button
      type="button"
      role="combobox"
      aria-expanded={open}
      aria-controls={open ? panelId : undefined}
      {...fieldProps}
      id={triggerId}
      aria-labelledby={labelledBy}
      className={cx('mn-input', 'mn-entity-picker', className)}
      data-size={size}
      data-placeholder={selected.length === 0 || undefined}
      data-invalid={fieldProps['aria-invalid'] ? true : undefined}
      data-disabled={fieldProps.disabled || undefined}
      onKeyDown={(e) => {
        fieldProps.onKeyDown?.(e);
        // APG select-only combobox: the arrow keys open the list too.
        if (!e.defaultPrevented && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
          e.preventDefault();
          setOpen(true);
        }
      }}
    >
      <span id={valueId} className="mn-entity-picker__value">
        {summary}
      </span>
      <ChevronDownGlyph className="mn-entity-picker__icon" />
    </button>
  );

  return (
    <PopoverPrimitive.Root
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) setQuery('');
      }}
    >
      <PopoverPrimitive.Trigger asChild ref={ref}>
        {trigger ?? defaultTrigger}
      </PopoverPrimitive.Trigger>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Content
          id={panelId}
          align={align}
          sideOffset={4}
          aria-label={dialogName}
          aria-labelledby={dialogName ? undefined : field?.labelId}
          className={cx('mn-menu', 'mn-entity-picker__content', contentClassName)}
        >
          <Combobox
            inline
            id={searchId}
            invalid={false}
            required={false}
            aria-label={searchLabel ?? searchPlaceholder}
            listLabel={dialogName ?? undefined}
            placeholder={searchPlaceholder}
            size={size === 'lg' ? 'md' : size}
            options={options}
            onSearch={search}
            searchDebounce={searchDebounce}
            minSearchLength={minSearchLength}
            multiple={multiple}
            value={multiple ? undefined : value}
            values={multiple ? values : undefined}
            valueOptions={selected}
            onValueChange={(next) => {
              if (valueProp === undefined) setInternalValue(next);
              onValueChange?.(next);
              if (shouldClose) close();
            }}
            onValuesChange={(next) => {
              if (valuesProp === undefined) setInternalValues(next);
              onValuesChange?.(next);
              if (shouldClose) close();
            }}
            onInputValueChange={setQuery}
            onCreate={create}
            clearOption={clearOption}
            renderOption={renderOption}
            filter={filter}
            sortByMatch={sortByMatch}
            limit={limit}
            loading={loading}
            emptyMessage={emptyMessage}
            messages={messages}
          />
          {footer ? (
            <div className="mn-entity-picker__footer">
              {typeof footer === 'function' ? footer({ close, query }) : footer}
            </div>
          ) : null}
        </PopoverPrimitive.Content>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
});
