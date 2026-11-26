'use client';

import { forwardRef, useEffect, useRef, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CodeTabs,
  type CodeSample,
  type CodeTabsProps,
} from '../../typography/code-block/index.js';

/** One snippet: the same example in one language or tool. The same shape as CodeTabs' `CodeSample`. */
export type CodeSnippet = CodeSample;

export interface CodeSnippetTabsProps extends Omit<CodeTabsProps, 'samples'> {
  /** The same example in several languages or tools, one tab each. */
  snippets: readonly CodeSnippet[];
  /**
   * Keeps every CodeSnippetTabs with the same key on the same language, and
   * remembers the choice in this browser. Pick one key per site, e.g.
   * `"api-docs"`. Each set only follows a choice it has a tab for.
   */
  syncKey?: string | undefined;
}

type Listener = (value: string) => void;
const listeners = new Map<string, Set<Listener>>();
const storageKey = (key: string) => `mn-code-snippet-tabs:${key}`;

function readStored(key: string): string | null {
  try {
    return window.localStorage.getItem(storageKey(key));
  } catch {
    return null;
  }
}

function publish(key: string, value: string) {
  try {
    window.localStorage.setItem(storageKey(key), value);
  } catch {
    // Storage can be blocked; the in-page sync still works.
  }
  listeners.get(key)?.forEach((l) => l(value));
}

const valueOf = (s: CodeSnippet) => s.value ?? s.label;

/**
 * Multi-language code snippets with one tab per language and a copy button on
 * each. A thin wrapper over CodeTabs (from CodeBlock) that takes `snippets`
 * and can keep every snippet set on a page on the reader's language with
 * `syncKey`.
 */
export const CodeSnippetTabs = forwardRef<HTMLDivElement, CodeSnippetTabsProps>(
  function CodeSnippetTabs(
    { snippets, syncKey, value: valueProp, defaultValue, onValueChange, className, ...rest },
    ref,
  ) {
    const [own, setOwn] = useState(defaultValue ?? (snippets[0] ? valueOf(snippets[0]) : ''));
    const snippetsRef = useRef(snippets);
    snippetsRef.current = snippets;

    useEffect(() => {
      if (!syncKey) return undefined;
      const adopt = (v: string | null) => {
        if (v !== null && snippetsRef.current.some((s) => valueOf(s) === v)) setOwn(v);
      };
      adopt(readStored(syncKey));
      const set = listeners.get(syncKey) ?? new Set<Listener>();
      listeners.set(syncKey, set);
      set.add(adopt);
      return () => {
        set.delete(adopt);
        if (set.size === 0) listeners.delete(syncKey);
      };
    }, [syncKey]);

    const current = valueProp ?? own;

    return (
      <CodeTabs
        ref={ref}
        {...rest}
        samples={snippets}
        value={current}
        onValueChange={(next) => {
          if (valueProp === undefined) setOwn(next);
          if (syncKey) publish(syncKey, next);
          onValueChange?.(next);
        }}
        className={cx('mn-code-snippet-tabs', className)}
        data-synced={syncKey ? '' : undefined}
      />
    );
  },
);
