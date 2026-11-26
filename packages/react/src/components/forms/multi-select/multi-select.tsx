'use client';

import { forwardRef, useCallback, useId, useMemo, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Combobox,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../combobox/index.js';
import { useFieldContext } from '../form-field/index.js';

/** One option of a MultiSelect. The same shape as a Combobox option. */
export type MultiSelectOption = ComboboxOption;

/** Interface text of MultiSelect. Override to translate. */
export interface MultiSelectMessages extends ComboboxMessages {
  /** Count under the field when `maxSelections` is set. Default `${count} of ${max} selected`. */
  limit: (count: number, max: number) => string;
}

const DEFAULT_LIMIT = (count: number, max: number) => `${count} of ${max} selected`;

export interface MultiSelectProps extends Omit<
  ComboboxProps,
  | 'multiple'
  | 'value'
  | 'defaultValue'
  | 'onValueChange'
  | 'values'
  | 'defaultValues'
  | 'onValuesChange'
  | 'clearOption'
  | 'messages'
  | 'name'
  | 'options'
> {
  /** The options. With `onSearch`, the options shown before the first search finishes. */
  options?: readonly MultiSelectOption[] | undefined;
  /** The selected values (controlled), in the order they were picked. */
  value?: readonly string[] | undefined;
  /** The initially selected values (uncontrolled). */
  defaultValue?: readonly string[] | undefined;
  /** Called with every selected value after each pick or removal. */
  onValueChange?: ((value: string[]) => void) | undefined;
  /**
   * Form field name. Each selected value submits under it as a hidden input
   * (read with `FormData.getAll`); the typed search text is never submitted.
   */
  name?: string | undefined;
  /**
   * Most values that can be picked. When reached, the other options are
   * disabled and a count ("2 of 3 selected") shows under the field.
   */
  maxSelections?: number | undefined;
  /** Interface text, for translation. */
  messages?: Partial<MultiSelectMessages> | undefined;
  /** Class for the wrapper `<div class="mn-multi-select">`; `className` goes on the Combobox box. */
  wrapperClassName?: string | undefined;
}

/**
 * Pick several values from a list: the picked values show as removable chips
 * in the field, typing filters the list, and the list stays open between
 * picks. Built on Combobox in `multiple` mode (WAI-ARIA APG combobox with a
 * multi-selectable listbox), adding array value props, native form
 * submission through `name`, and an optional `maxSelections` limit.
 */
export const MultiSelect = forwardRef<HTMLInputElement, MultiSelectProps>(function MultiSelect(
  {
    options = [],
    value: valueProp,
    defaultValue,
    onValueChange,
    name,
    maxSelections,
    messages,
    onCreate,
    wrapperClassName,
    disabled,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const field = useFieldContext();
  const off = disabled ?? field?.disabled ?? false;
  const countId = `mn-multi-select-${auto}-count`;
  const { limit = DEFAULT_LIMIT, ...comboboxMessages } = messages ?? {};

  const [inner, setInner] = useState<readonly string[]>(defaultValue ?? []);
  const controlled = valueProp !== undefined;
  const current = controlled ? valueProp : inner;
  const values = useMemo(() => [...current], [current]);
  const change = useCallback(
    (next: string[]) => {
      if (!controlled) setInner(next);
      onValueChange?.(next);
    },
    [controlled, onValueChange],
  );

  const full = maxSelections !== undefined && values.length >= maxSelections;
  const shownOptions = useMemo(() => {
    if (!full) return [...options];
    const picked = new Set(values);
    return options.map((o) => (picked.has(o.value) ? o : { ...o, disabled: true }));
  }, [options, values, full]);

  return (
    <div
      className={cx('mn-multi-select', wrapperClassName)}
      data-full={full || undefined}
      data-disabled={off || undefined}
    >
      <Combobox
        ref={ref}
        {...rest}
        disabled={disabled}
        aria-describedby={
          cx(rest['aria-describedby'], maxSelections !== undefined ? countId : undefined) ||
          undefined
        }
        multiple
        options={shownOptions}
        values={values}
        onValuesChange={change}
        onCreate={full ? undefined : onCreate}
        messages={comboboxMessages}
      />
      {maxSelections !== undefined ? (
        <p id={countId} className="mn-multi-select__count" aria-live="polite">
          {limit(values.length, maxSelections)}
        </p>
      ) : null}
      {name && !off
        ? values.map((v) => <input key={v} type="hidden" name={name} value={v} />)
        : null}
    </div>
  );
});
