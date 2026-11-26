import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, IconButton, type ButtonProps } from '../../actions/button/index.js';

/** Which side of the input an addon or button sits on (logical, so it flips in RTL). */
export type InputGroupAlign = 'inline-start' | 'inline-end';

export interface InputGroupProps extends HTMLAttributes<HTMLDivElement> {
  /** Height from `--mn-control-*`. Give the Input inside the same `size`. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Children: one `Input` plus any `InputGroupAddon` and `InputGroupButton`, in reading order. */
  children: ReactNode;
}

/**
 * Attaches text, icons and buttons to an Input inside one bordered box with
 * one focus ring: "https://" before a URL, "kg" after a weight, a clear
 * button in a search. Put the parts in reading order; the Input keeps its
 * Field wiring. Invalid and disabled styles follow the Input.
 */
export const InputGroup = forwardRef<HTMLDivElement, InputGroupProps>(function InputGroup(
  { size = 'md', className, children, ...rest },
  ref,
) {
  return (
    <div ref={ref} {...rest} className={cx('mn-input-group', className)} data-size={size}>
      {children}
    </div>
  );
});

export interface InputGroupAddonProps extends HTMLAttributes<HTMLSpanElement> {
  /** Side of the input. Default `inline-start`. Exposed as `data-align`. */
  align?: InputGroupAlign | undefined;
}

/**
 * Static text or an icon attached to the input ("https://", "@", "kg").
 * Text is visible but not part of the input's name: give the addon an `id`
 * and add it to the Input's `aria-describedby` when it carries meaning.
 * Mark decorative icons `aria-hidden`.
 */
export const InputGroupAddon = forwardRef<HTMLSpanElement, InputGroupAddonProps>(
  function InputGroupAddon({ align = 'inline-start', className, ...rest }, ref) {
    return (
      <span
        ref={ref}
        {...rest}
        className={cx('mn-input-group__addon', className)}
        data-align={align}
      />
    );
  },
);

export interface InputGroupButtonProps extends ButtonProps {
  /** Side of the input. Default `inline-end`. Exposed as `data-align`. */
  align?: InputGroupAlign | undefined;
  /** Icon for an icon-only button. Needs `label`. */
  icon?: ReactNode;
  /** Accessible name of an icon-only button (required with `icon` and no children). */
  label?: string | undefined;
}

/**
 * A Button attached inside the group (copy, clear, search, generate). With
 * `icon` and `label` and no children it renders an IconButton. Ghost and
 * small by default; it keeps its own focus ring.
 */
export const InputGroupButton = forwardRef<HTMLButtonElement, InputGroupButtonProps>(
  function InputGroupButton(
    {
      align = 'inline-end',
      icon,
      label,
      variant = 'ghost',
      size = 'sm',
      className,
      children,
      ...rest
    },
    ref,
  ) {
    const classes = cx('mn-input-group__button', className);
    if (icon && label && (children === undefined || children === null)) {
      return (
        <IconButton
          ref={ref}
          {...rest}
          icon={icon}
          label={label}
          variant={variant}
          size={size}
          className={classes}
          data-align={align}
        />
      );
    }
    return (
      <Button
        ref={ref}
        {...rest}
        variant={variant}
        size={size}
        className={classes}
        data-align={align}
      >
        {children}
      </Button>
    );
  },
);
