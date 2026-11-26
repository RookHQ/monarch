'use client';

import { useMemo, useRef, type ReactNode } from 'react';
import type {
  SuggestionKeyDownProps,
  SuggestionOptions,
  SuggestionProps,
} from '@tiptap/suggestion';
import {
  MentionPicker,
  useMentionPicker,
  type MentionItem,
  type MentionQuery,
  type MentionSearch,
} from '../mention-picker/index.js';
import type { RichTextEditorMessages } from './rich-text-editor.toolbar.js';

/**
 * One mention suggestion: `MentionItem` from
 * `@rookcom/monarch-react/collaboration`, so one product search function
 * serves both the plain-text and the rich-text pickers.
 */
export type RichTextMentionItem = MentionItem;

/**
 * Finds suggestions for what was typed after the trigger (`MentionSearch`).
 * Return an array or a promise of one; abort when `signal` fires.
 */
export type RichTextMentionSearch = MentionSearch;

export interface RichTextMentionOptions {
  /** The product's people/channel search. */
  search: RichTextMentionSearch;
  /** Character that opens the list. Default `@`. */
  trigger?: string;
  /** Wait this long after typing before searching. Default 150 ms. */
  debounceMs?: number;
  /** Allow spaces in the query ("@Ann Lee"). Default false. */
  allowSpaces?: boolean;
}

export interface MentionBridge {
  /** Passed to the Mention extension. Stable for the editor's lifetime. */
  suggestion: Omit<SuggestionOptions, 'editor'>;
  /** True while the list is showing, so Enter picks a suggestion instead of sending. */
  isOpen: () => boolean;
  /** ARIA for the editing surface. */
  fieldAttributes: Record<string, string>;
  /** The suggestion list (portalled), or null. */
  list: ReactNode;
}

/**
 * Connects Tiptap's suggestion plugin to `useMentionPicker` and
 * `MentionPicker` from @rookcom/monarch-react/collaboration: debounced,
 * abortable search, the highlighted option and the APG listbox keys. Focus
 * stays in the editor, which points at the highlighted option with
 * aria-activedescendant.
 */
export function useMentionBridge(
  options: RichTextMentionOptions | undefined,
  messages: RichTextEditorMessages,
): MentionBridge | undefined {
  const trigger = options?.trigger ?? '@';
  const allowSpaces = options?.allowSpaces ?? false;
  const command = useRef<((item: MentionItem) => void) | null>(null);
  // Escape closes the list until this suggestion ends (the caret leaves it).
  const dismissedAt = useRef<number | null>(null);
  const picker = useMentionPicker({
    search: (text, ctx) => (options ? options.search(text, ctx) : []),
    onSelect: (item) => command.current?.(item),
    debounceMs: options?.debounceMs ?? 150,
  });
  const live = useRef(picker);
  live.current = picker;

  const suggestion = useMemo<Omit<SuggestionOptions, 'editor'>>(() => {
    const sync = (p: SuggestionProps) => {
      command.current = (item) => p.command({ id: item.id, label: item.label });
      if (dismissedAt.current === p.range.from) return;
      const r = p.clientRect?.();
      const query: MentionQuery = {
        trigger,
        text: p.query,
        start: p.range.from,
        end: p.range.to,
      };
      live.current.update(query, r ? { top: r.top, left: r.left, height: r.height } : null);
    };
    return {
      allowSpaces,
      items: () => [],
      render: () => ({
        onStart: sync,
        onUpdate: sync,
        onKeyDown: ({ event, range }: SuggestionKeyDownProps) => {
          const used = live.current.handleKeyDown(event);
          if (used && event.key === 'Escape') dismissedAt.current = range.from;
          return used;
        },
        onExit: () => {
          dismissedAt.current = null;
          command.current = null;
          live.current.close();
        },
      }),
    };
    // The suggestion config is read when the editor is created. RichTextEditor
    // keys its editor on `trigger` and `allowSpaces`, so a change here rebuilds
    // it (keeping the content) with the new config; nothing else changes it.
  }, [trigger, allowSpaces]);

  if (!options) return undefined;

  const fieldAttributes = Object.fromEntries(
    Object.entries(picker.fieldProps).filter(
      (e): e is [string, string] => typeof e[1] === 'string',
    ),
  );
  return {
    suggestion,
    isOpen: () => live.current.query !== null,
    fieldAttributes: picker.query !== null ? fieldAttributes : {},
    list: (
      <MentionPicker
        {...picker.pickerProps}
        messages={{
          label: messages.suggestions,
          loading: messages.suggestionsLoading,
          empty: messages.suggestionsEmpty,
          error: messages.suggestionsError,
        }}
      />
    ),
  };
}
