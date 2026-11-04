'use client';

import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as SelectPrimitive from '@radix-ui/react-select';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

/**
 * Single-value select built on Radix Select: typeahead, arrow-key navigation,
 * and screen-reader semantics come from the primitive.
 *
 *   <Select value={v} onValueChange={setV}>
 *     <SelectTrigger placeholder="Choose a status" />
 *     <SelectContent>
 *       <SelectItem value="open">Open</SelectItem>
 *     </SelectContent>
 *   </Select>
 */
export const Select = SelectPrimitive.Root;
export const SelectGroup = SelectPrimitive.Group;
export const SelectValue = SelectPrimitive.Value;

export interface SelectTriggerProps extends ComponentPropsWithoutRef<
  typeof SelectPrimitive.Trigger
> {
  placeholder?: string;
  size?: 'sm' | 'md' | 'lg';
  invalid?: boolean;
}

export const SelectTrigger = forwardRef<
  ElementRef<typeof SelectPrimitive.Trigger>,
  SelectTriggerProps
>(function SelectTrigger({ placeholder, size = 'md', invalid, className, children, ...rest }, ref) {
  // `required` is not a valid attribute on the trigger <button>; aria-required carries the meaning.
  const { required: _required, ...props } = useFieldControl(rest, { invalid }) as typeof rest & {
    required?: boolean;
    'aria-required'?: true;
    'aria-invalid'?: true;
  };
  return (
    <SelectPrimitive.Trigger
      ref={ref}
      {...props}
      data-size={size}
      className={cx('mn-input', 'mn-select', className)}
    >
      {children ?? <SelectPrimitive.Value placeholder={placeholder} />}
      <SelectPrimitive.Icon className="mn-select__icon">
        <ChevronDownGlyph />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  );
});

export const SelectContent = forwardRef<
  ElementRef<typeof SelectPrimitive.Content>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Content>
>(function SelectContent(
  { className, children, position = 'popper', sideOffset = 4, ...rest },
  ref,
) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        ref={ref}
        position={position}
        sideOffset={sideOffset}
        {...rest}
        className={cx('mn-menu', 'mn-select__content', className)}
      >
        <SelectPrimitive.Viewport className="mn-select__viewport">
          {children}
        </SelectPrimitive.Viewport>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  );
});

export const SelectItem = forwardRef<
  ElementRef<typeof SelectPrimitive.Item>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Item>
>(function SelectItem({ className, children, ...rest }, ref) {
  return (
    <SelectPrimitive.Item ref={ref} {...rest} className={cx('mn-menu__item', className)}>
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
      <SelectPrimitive.ItemIndicator className="mn-menu__indicator">
        <CheckGlyph />
      </SelectPrimitive.ItemIndicator>
    </SelectPrimitive.Item>
  );
});

export const SelectLabel = forwardRef<
  ElementRef<typeof SelectPrimitive.Label>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Label>
>(function SelectLabel({ className, ...rest }, ref) {
  return <SelectPrimitive.Label ref={ref} {...rest} className={cx('mn-menu__label', className)} />;
});

export const SelectSeparator = forwardRef<
  ElementRef<typeof SelectPrimitive.Separator>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Separator>
>(function SelectSeparator({ className, ...rest }, ref) {
  return (
    <SelectPrimitive.Separator
      ref={ref}
      {...rest}
      className={cx('mn-menu__separator', className)}
    />
  );
});
