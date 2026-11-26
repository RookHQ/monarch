'use client';

import { forwardRef, useId, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  RadioGroup,
  RadioGroupItem,
  useRadioGroupContext,
  type RadioGroupProps,
} from '../radio-group/index.js';

/** One card of a RadioCardGroup. */
export interface RadioCardOption {
  /** Submitted with the form and passed to `onValueChange`. Unique within the group. */
  value: string;
  /** Card heading; it is the radio's accessible name. */
  title: ReactNode;
  /** Supporting text under the title, announced as the radio's description. */
  description?: ReactNode;
  /** Decorative icon at the start of the card. */
  icon?: ReactNode;
  /**
   * Short extra detail at the end of the title row, such as a price or a
   * count. It is part of the description, not the name.
   */
  meta?: ReactNode;
  /** The card cannot be picked. Arrow keys skip it. */
  disabled?: boolean;
}

export interface RadioCardGroupProps extends Omit<
  RadioGroupProps,
  'options' | 'children' | 'orientation'
> {
  /** The cards, in order. */
  options: readonly RadioCardOption[];
  /**
   * Cards per row on wide containers. On narrow screens (under 40rem) cards
   * always stack in one column. Default `1`.
   */
  columns?: 1 | 2 | 3 | undefined;
}

function RadioCard({ option }: { option: RadioCardOption }) {
  const group = useRadioGroupContext();
  const auto = useId();
  const titleId = `mn-radio-card-${auto}-title`;
  const metaId = option.meta ? `mn-radio-card-${auto}-meta` : undefined;
  const descriptionId = option.description ? `mn-radio-card-${auto}-description` : undefined;
  const checked = group?.value === option.value;
  const off = option.disabled === true || group?.disabled === true;
  return (
    <label
      className="mn-radio-card-group__card"
      data-state={checked ? 'checked' : 'unchecked'}
      data-disabled={off || undefined}
    >
      {option.icon ? (
        <span className="mn-radio-card-group__icon" aria-hidden>
          {option.icon}
        </span>
      ) : null}
      <span className="mn-radio-card-group__text">
        <span className="mn-radio-card-group__heading">
          <span id={titleId} className="mn-radio-card-group__title">
            {option.title}
          </span>
          {option.meta ? (
            <span id={metaId} className="mn-radio-card-group__meta">
              {option.meta}
            </span>
          ) : null}
        </span>
        {option.description ? (
          <span id={descriptionId} className="mn-radio-card-group__description">
            {option.description}
          </span>
        ) : null}
      </span>
      <RadioGroupItem
        value={option.value}
        disabled={option.disabled}
        aria-labelledby={titleId}
        aria-describedby={cx(metaId, descriptionId) || undefined}
      />
    </label>
  );
}

/**
 * A single choice shown as selectable cards, each with a title, an optional
 * description, icon and meta (such as a price). Built on RadioGroup, so it has
 * the same native radio keyboard model, form submission and Field wiring; the
 * whole card is the click target and shows a radio for the selection.
 */
export const RadioCardGroup = forwardRef<HTMLDivElement, RadioCardGroupProps>(
  function RadioCardGroup({ options, columns = 1, className, ...rest }, ref) {
    return (
      <RadioGroup
        ref={ref}
        {...rest}
        data-columns={columns}
        className={cx('mn-radio-card-group', className)}
      >
        {options.map((option) => (
          <RadioCard key={option.value} option={option} />
        ))}
      </RadioGroup>
    );
  },
);
