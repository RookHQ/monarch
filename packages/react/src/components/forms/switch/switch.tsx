'use client';

import {
  forwardRef,
  useId,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type ReactNode,
} from 'react';
import * as SwitchPrimitive from '@radix-ui/react-switch';
import { cx } from '../../../internal/cx.js';
import { useFieldControl } from '../form-field/index.js';

export interface SwitchProps extends ComponentPropsWithoutRef<typeof SwitchPrimitive.Root> {
  label?: ReactNode;
  description?: ReactNode;
  size?: 'sm' | 'md';
}

/**
 * An on/off setting that applies immediately. For choices that are submitted
 * with a form, prefer Checkbox.
 */
export const Switch = forwardRef<ElementRef<typeof SwitchPrimitive.Root>, SwitchProps>(
  function Switch({ label, description, size = 'md', className, ...rest }, ref) {
    const auto = useId();
    const props = useFieldControl(rest);
    const id = props.id ?? `mn-switch-${auto}`;
    const descriptionId = description ? `${id}-description` : undefined;
    const control = (
      <SwitchPrimitive.Root
        ref={ref}
        {...props}
        id={id}
        aria-describedby={cx(props['aria-describedby'], descriptionId)}
        data-size={size}
        className={cx('mn-switch', label ? undefined : className)}
      >
        <SwitchPrimitive.Thumb className="mn-switch__thumb" />
      </SwitchPrimitive.Root>
    );
    if (!label) return control;
    return (
      <div
        className={cx('mn-choice', 'mn-choice--switch', className)}
        data-disabled={props.disabled || undefined}
      >
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
        {control}
      </div>
    );
  },
);
