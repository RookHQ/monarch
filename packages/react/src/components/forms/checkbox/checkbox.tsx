'use client';

import {
  forwardRef,
  useId,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type ReactNode,
} from 'react';
import * as CheckboxPrimitive from '@radix-ui/react-checkbox';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, DashGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

export interface CheckboxProps extends ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root> {
  /** Inline label. Omit when the checkbox sits inside a Field with a FieldLabel. */
  label?: ReactNode;
  /** Secondary text under an inline label. */
  description?: ReactNode;
  invalid?: boolean;
}

/**
 * A checkbox with optional inline label. Supports `checked="indeterminate"`.
 * Space toggles it; it participates in native forms via `name`/`value`.
 */
export const Checkbox = forwardRef<ElementRef<typeof CheckboxPrimitive.Root>, CheckboxProps>(
  function Checkbox({ label, description, invalid, className, ...rest }, ref) {
    const auto = useId();
    const props = useFieldControl(rest, { invalid });
    const id = props.id ?? `mn-checkbox-${auto}`;
    const descriptionId = description ? `${id}-description` : undefined;
    const describedBy = cx(props['aria-describedby'], descriptionId);
    const control = (
      <CheckboxPrimitive.Root
        ref={ref}
        {...props}
        id={id}
        aria-describedby={describedBy}
        className={cx('mn-checkbox', label ? undefined : className)}
      >
        <CheckboxPrimitive.Indicator className="mn-checkbox__indicator">
          {props.checked === 'indeterminate' ? <DashGlyph /> : <CheckGlyph />}
        </CheckboxPrimitive.Indicator>
      </CheckboxPrimitive.Root>
    );
    if (!label) return control;
    return (
      <div className={cx('mn-choice', className)} data-disabled={props.disabled || undefined}>
        {control}
        <div className="mn-choice__text">
          <label htmlFor={id} className="mn-choice__label">
            {label}
          </label>
          {description ? (
            <p id={descriptionId} className="mn-choice__description">
              {description}
            </p>
          ) : null}
        </div>
      </div>
    );
  },
);
