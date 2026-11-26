import { forwardRef, type CSSProperties, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-stack';

/**
 * A step of the Monarch space scale (`--mn-space-*`): `'4'` is 1rem (16px),
 * `'2'` is 0.5rem, `'1-5'` is 0.375rem. Shared by Stack, Inline and Grid.
 */
export type LayoutGap =
  '0' | '0-5' | '1' | '1-5' | '2' | '2-5' | '3' | '4' | '5' | '6' | '8' | '10' | '12' | '16';

/** The elements a layout primitive can render as. */
export type LayoutElement =
  | 'div'
  | 'section'
  | 'article'
  | 'aside'
  | 'header'
  | 'footer'
  | 'main'
  | 'nav'
  | 'form'
  | 'fieldset'
  | 'ul'
  | 'ol'
  | 'li'
  | 'span';

/** The CSS value of a gap step, as a `var(--mn-space-*)` reference. */
export function layoutGap(gap: LayoutGap): string {
  return `var(--mn-space-${gap})`;
}

export interface StackProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `div`; `ul` and `ol` lose their bullets and indent. */
  as?: LayoutElement | undefined;
  /** Space between children, a step of the Monarch space scale. Default `'4'` (16px). */
  gap?: LayoutGap | undefined;
  /** Cross-axis (horizontal) alignment of children. Default `stretch` (full width). */
  align?: 'start' | 'center' | 'end' | 'stretch' | undefined;
  /** Main-axis (vertical) distribution when the stack is taller than its content. Default `start`. */
  justify?: 'start' | 'center' | 'end' | 'between' | undefined;
  /** Draw a subtle line between children (centred in the gap). */
  dividers?: boolean | undefined;
}

/**
 * Lays children out in a vertical column with even spacing from the Monarch
 * space scale. Use it instead of ad-hoc `flex flex-col gap-*` wrappers.
 */
export const Stack = forwardRef<HTMLElement, StackProps>(function Stack(
  {
    as: Tag = 'div',
    gap = '4',
    align = 'stretch',
    justify = 'start',
    dividers = false,
    className,
    style,
    ...rest
  },
  ref,
) {
  return (
    <Tag
      ref={ref as never}
      {...rest}
      className={cx(ROOT, className)}
      style={{ '--mn-stack-gap': layoutGap(gap), ...style } as CSSProperties}
      data-gap={gap}
      data-align={align}
      data-justify={justify}
      data-dividers={dividers ? '' : undefined}
    />
  );
});
