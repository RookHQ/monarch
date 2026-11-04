'use client';

import type { ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '../../../components/data-display/accordion/index.js';
type HeadingLevel = 'h2' | 'h3' | 'h4';

export interface FAQItem {
  question: string;
  answer: ReactNode;
}

export interface FAQProps {
  items: FAQItem[];
  /** Heading element for each question. */
  headingAs?: HeadingLevel;
  /** Allow several answers open at once. Default false. */
  multiple?: boolean;
  /** Index of the question open on first render. */
  defaultOpen?: number;
  className?: string;
}

/**
 * Frequently asked questions. Answer the question a buyer actually has; never
 * internal notes. Each question is a heading, so the list appears in the outline.
 */
export function FAQ({
  items,
  headingAs = 'h3',
  multiple = false,
  defaultOpen,
  className,
}: FAQProps) {
  const value = (i: number) => `faq-${i}`;
  const open = defaultOpen === undefined ? undefined : value(defaultOpen);
  const body = items.map((item, i) => (
    <AccordionItem key={item.question} value={value(i)}>
      <AccordionTrigger headingAs={headingAs}>{item.question}</AccordionTrigger>
      <AccordionContent>{item.answer}</AccordionContent>
    </AccordionItem>
  ));
  return multiple ? (
    <Accordion
      type="multiple"
      {...(open ? { defaultValue: [open] } : {})}
      className={cx('mn-faq', className)}
    >
      {body}
    </Accordion>
  ) : (
    <Accordion
      type="single"
      collapsible
      {...(open ? { defaultValue: open } : {})}
      className={cx('mn-faq', className)}
    >
      {body}
    </Accordion>
  );
}
