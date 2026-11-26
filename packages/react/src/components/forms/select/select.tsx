'use client';

import {
  createContext,
  forwardRef,
  Fragment,
  useContext,
  useState,
  type ComponentPropsWithoutRef,
  type ElementRef,
} from 'react';
import * as SelectPrimitive from '@radix-ui/react-select';
import { cx } from '../../../internal/cx.js';
import { useOverlayStage } from '../../../internal/overlay-stage.js';
import { CheckGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

export type SelectProps = ComponentPropsWithoutRef<typeof SelectPrimitive.Root>;

/**
 * An open Select in a docs overlay stage. Radix Select is always modal when
 * open (it hides the rest of the page from assistive technology, traps focus
 * and locks scrolling), so a stage shows a picture of the open list instead:
 * the same markup and styles, in the flow, with the page left alone. The first
 * time the trigger is used the picture gives way to the real list.
 */
const SelectPictureContext = createContext<{ value: string | undefined } | null>(null);

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
export function Select({ open, defaultOpen, onOpenChange, ...props }: SelectProps) {
  const staged = useOverlayStage();
  // Radix's open state is always held here, so a staged Select can start as a picture.
  const [pictured, setPictured] = useState(staged && Boolean(open ?? defaultOpen));
  const [ownOpen, setOwnOpen] = useState(!staged && Boolean(defaultOpen));
  return (
    <SelectPictureContext.Provider
      // Nothing can change the value before the picture gives way, so the initial one holds.
      value={pictured ? { value: props.value ?? props.defaultValue } : null}
    >
      <SelectPrimitive.Root
        {...props}
        open={!pictured && (open ?? ownOpen)}
        onOpenChange={(next) => {
          setPictured(false);
          setOwnOpen(next);
          onOpenChange?.(next);
        }}
      />
    </SelectPictureContext.Provider>
  );
}
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
  // In a docs preview stage the list renders in place; while the Select is a
  // picture (see Select) the list is drawn without Radix, which is always modal.
  const staged = useOverlayStage();
  const picture = useContext(SelectPictureContext);
  const Portal = staged ? Fragment : SelectPrimitive.Portal;
  if (picture) {
    return (
      <>
        {/* Closed, Radix keeps the items off the page so the trigger shows the value. */}
        <SelectPrimitive.Content>{children}</SelectPrimitive.Content>
        <div
          className={cx('mn-menu', 'mn-select__content', className)}
          data-mn-select-picture=""
          style={rest.style}
        >
          <div className="mn-select__viewport">
            <SelectItemPictureContext.Provider value={picture.value ?? null}>
              {children}
            </SelectItemPictureContext.Provider>
          </div>
        </div>
      </>
    );
  }
  return (
    <Portal>
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
    </Portal>
  );
});

/** The selected value, inside the picture of an open list (see Select). */
const SelectItemPictureContext = createContext<string | null | undefined>(undefined);

export const SelectItem = forwardRef<
  ElementRef<typeof SelectPrimitive.Item>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Item>
>(function SelectItem({ className, children, ...rest }, ref) {
  const pictured = useContext(SelectItemPictureContext);
  if (pictured !== undefined) {
    const checked = pictured === rest.value;
    return (
      <div
        ref={ref}
        className={cx('mn-menu__item', className)}
        data-state={checked ? 'checked' : 'unchecked'}
        {...(checked ? { 'data-highlighted': '' } : {})}
        {...(rest.disabled ? { 'data-disabled': '' } : {})}
      >
        <span>{children}</span>
        {checked ? (
          <span className="mn-menu__indicator" aria-hidden="true">
            <CheckGlyph />
          </span>
        ) : null}
      </div>
    );
  }
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
