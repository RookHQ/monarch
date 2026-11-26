'use client';

import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
type HeadingLevel = 'h2' | 'h3' | 'h4';

/**
 * Stacked disclosure sections. Enter/Space toggle, arrow keys move between
 * headers, Home/End jump to the first and last. Built on Radix Accordion.
 */
export const Accordion = forwardRef<
  ElementRef<typeof AccordionPrimitive.Root>,
  ComponentPropsWithoutRef<typeof AccordionPrimitive.Root>
>(function Accordion({ className, ...rest }, ref) {
  return <AccordionPrimitive.Root ref={ref} {...rest} className={cx('mn-accordion', className)} />;
});

export const AccordionItem = forwardRef<
  ElementRef<typeof AccordionPrimitive.Item>,
  ComponentPropsWithoutRef<typeof AccordionPrimitive.Item>
>(function AccordionItem({ className, ...rest }, ref) {
  return (
    <AccordionPrimitive.Item ref={ref} {...rest} className={cx('mn-accordion__item', className)} />
  );
});

export interface AccordionTriggerProps extends ComponentPropsWithoutRef<
  typeof AccordionPrimitive.Trigger
> {
  /** Heading element wrapping the trigger, so the item appears in the page outline. */
  headingAs?: HeadingLevel;
}

export const AccordionTrigger = forwardRef<
  ElementRef<typeof AccordionPrimitive.Trigger>,
  AccordionTriggerProps
>(function AccordionTrigger({ headingAs: Heading = 'h3', className, children, ...rest }, ref) {
  return (
    <AccordionPrimitive.Header asChild>
      <Heading className="mn-accordion__header">
        <AccordionPrimitive.Trigger
          ref={ref}
          {...rest}
          className={cx('mn-accordion__trigger', className)}
        >
          <span className="mn-accordion__label">{children}</span>
          <ChevronDownGlyph className="mn-accordion__chevron" />
        </AccordionPrimitive.Trigger>
      </Heading>
    </AccordionPrimitive.Header>
  );
});

export const AccordionContent = forwardRef<
  ElementRef<typeof AccordionPrimitive.Content>,
  ComponentPropsWithoutRef<typeof AccordionPrimitive.Content>
>(function AccordionContent({ className, children, ...rest }, ref) {
  return (
    <AccordionPrimitive.Content
      ref={ref}
      {...rest}
      className={cx('mn-accordion__content', className)}
    >
      <div className="mn-accordion__body">{children}</div>
    </AccordionPrimitive.Content>
  );
});
