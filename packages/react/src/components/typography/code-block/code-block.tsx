'use client';

import { forwardRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  AiCodeBlock,
  type AiCodeBlockMessages,
  type AiCodeBlockProps,
} from '../../intelligence/ai-code-block/index.js';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../navigation/tabs/index.js';

/** Visible strings, for translation. */
export type CodeBlockMessages = Omit<AiCodeBlockMessages, 'apply' | 'insert'>;

export interface CodeBlockProps extends Omit<
  AiCodeBlockProps,
  'onApply' | 'onInsert' | 'incomplete' | 'messages' | 'filename' | 'title'
> {
  /**
   * Header text, e.g. a file name ("server.ts") or what the snippet does
   * ("Create a webhook"). It also names the scrolling region. Default: the
   * language name.
   */
  title?: string | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<CodeBlockMessages> | undefined;
}

/**
 * A code sample for documentation, settings and messages: a header with the
 * file name or language, copy, a wrap toggle, optional line numbers, light
 * syntax colouring and a diff mode. The code scrolls sideways inside its
 * own focusable region, so the page never does. The non-AI face of
 * AiCodeBlock, without Apply and Insert.
 */
export const CodeBlock = forwardRef<HTMLDivElement, CodeBlockProps>(function CodeBlock(
  { title, className, ...rest },
  ref,
) {
  return (
    <AiCodeBlock ref={ref} {...rest} filename={title} className={cx('mn-code-block', className)} />
  );
});

/** One sample in a CodeTabs set, e.g. the cURL version of a request. */
export interface CodeSample {
  /** Tab label, e.g. "cURL" or "Python". Also the default value. */
  label: string;
  /** The code, exactly as it should be copied. */
  code: string;
  /** Language id for highlighting, e.g. "bash" or "python". */
  language?: string;
  /** Stable value for controlled selection. Default `label`. */
  value?: string;
  /** Header text inside the block. Default: the tab label. */
  title?: string;
}

export interface CodeTabsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'defaultValue'> {
  /** The same example in several languages or tools. */
  samples: readonly CodeSample[];
  /** Accessible name of the tab list. Default "Code samples". */
  label?: string;
  /** Controlled selected sample (its `value` or `label`). */
  value?: string;
  /** Uncontrolled initial sample. Default the first. */
  defaultValue?: string;
  /** Called when another sample is chosen, e.g. to remember the reader's language. */
  onValueChange?: ((value: string) => void) | undefined;
  /** Passed to each CodeBlock. */
  lineNumbers?: boolean;
  /** Passed to each CodeBlock. Default true. */
  showWrapToggle?: boolean;
  /** Replaces any of the English strings of the blocks. */
  messages?: Partial<CodeBlockMessages> | undefined;
}

const valueOf = (s: CodeSample) => s.value ?? s.label;

/**
 * The same code example in several languages or tools (cURL, JavaScript,
 * Python…), one tab each. Each tab panel is a CodeBlock with its own copy
 * button, so people copy exactly the version they are reading.
 */
export const CodeTabs = forwardRef<HTMLDivElement, CodeTabsProps>(function CodeTabs(
  {
    samples,
    label = 'Code samples',
    value: valueProp,
    defaultValue,
    onValueChange,
    lineNumbers,
    showWrapToggle,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const first = samples[0] ? valueOf(samples[0]) : '';
  const [state, setState] = useState(defaultValue ?? first);
  const value = valueProp ?? state;
  return (
    <div ref={ref} {...rest} className={cx('mn-code-tabs', className)}>
      <Tabs
        value={value}
        onValueChange={(next) => {
          if (valueProp === undefined) setState(next);
          onValueChange?.(next);
        }}
      >
        <TabsList aria-label={label} scrollable className="mn-code-tabs__list">
          {samples.map((s) => (
            <TabsTrigger key={valueOf(s)} value={valueOf(s)}>
              {s.label}
            </TabsTrigger>
          ))}
        </TabsList>
        {samples.map((s) => (
          <TabsContent
            key={valueOf(s)}
            value={valueOf(s)}
            tabIndex={-1}
            className="mn-code-tabs__panel"
          >
            <CodeBlock
              code={s.code}
              language={s.language}
              title={s.title ?? s.label}
              {...(lineNumbers !== undefined ? { lineNumbers } : {})}
              {...(showWrapToggle !== undefined ? { showWrapToggle } : {})}
              messages={messages}
            />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
});
