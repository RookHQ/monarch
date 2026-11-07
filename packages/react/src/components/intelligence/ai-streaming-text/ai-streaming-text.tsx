'use client';

import { forwardRef, useEffect, useMemo, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  AiMarkdown,
  blockText,
  parseMarkdownBlocks,
  parseMarkdownIncremental,
  type AiMarkdownMessages,
  type AiMarkdownProps,
  type MarkdownBlock,
  type MarkdownParseState,
} from '../ai-markdown/index.js';

/** Every user-visible (spoken) string AiStreamingText adds. */
export interface AiStreamingTextMessages {
  /** Announced when streaming ends. Default "Response complete". */
  complete: string;
  /** Announced in place of a code block's contents. Default "Code block". */
  codeBlock: string;
  /** Announced in place of a table's contents. Default "Table". */
  table: string;
}

export const AI_STREAMING_TEXT_MESSAGES: AiStreamingTextMessages = {
  complete: 'Response complete',
  codeBlock: 'Code block',
  table: 'Table',
};

export interface AiStreamingTextProps
  extends
    Omit<HTMLAttributes<HTMLDivElement>, 'children'>,
    Pick<
      AiMarkdownProps,
      | 'headingLevelOffset'
      | 'allowImages'
      | 'externalLinksInNewTab'
      | 'renderCode'
      | 'codeBlockProps'
    > {
  /** The Markdown received so far. It normally only grows while `streaming`. */
  text: string;
  /** True while more text is arriving: shows the caret, sets `aria-busy` and announces sentences. */
  streaming: boolean;
  /**
   * Announce new complete sentences and paragraphs, then "Response complete",
   * through a polite live region. Turn it off when the surrounding thread
   * announces messages itself. Default true.
   */
  announce?: boolean;
  /** Minimum milliseconds between announcements. Values below 1000 are raised to 1000. Default 1000. */
  announceInterval?: number;
  /** Show the blinking caret at the end while streaming (static under reduced motion). Default true. */
  caret?: boolean;
  /** Replaces any of the spoken English strings. */
  messages?: Partial<AiStreamingTextMessages> | undefined;
  /** Passed to AiMarkdown as its `messages`. */
  markdownMessages?: Partial<AiMarkdownMessages> | undefined;
}

const ESCAPE_OR_CODE = /\\.|`+/g;

/** Delimiter runs of `*`, `_`, `~` outside code spans, with CommonMark flanking. */
function openEmphasis(src: string): string[] {
  const stack: { char: string; len: number }[] = [];
  const re = /\\.|(`+)|([*_~]+)/g;
  let codeRun = 0;
  for (let m = re.exec(src); m; m = re.exec(src)) {
    if (m[1]) {
      if (codeRun === 0) codeRun = m[1].length;
      else if (codeRun === m[1].length) codeRun = 0;
      continue;
    }
    if (!m[2] || codeRun) continue;
    // A run may mix characters ("*_"); handle the first character's run only.
    const char = m[2][0] as string;
    let len = 1;
    while (m[2][len] === char) len += 1;
    re.lastIndex = m.index + len;
    const before = m.index === 0 ? ' ' : (src[m.index - 1] as string);
    const after = src[m.index + len] ?? ' ';
    const space = /\s/;
    const punct = /[\p{P}\p{S}]/u;
    const left =
      !space.test(after) && (!punct.test(after) || space.test(before) || punct.test(before));
    const right =
      !space.test(before) && (!punct.test(before) || space.test(after) || punct.test(after));
    const canOpen = char === '_' ? left && (!right || punct.test(before)) : left;
    const canClose = char === '_' ? right && (!left || punct.test(after)) : right;
    if (char === '~' && len > 2) continue;
    const at = canClose ? stack.map((s) => s.char).lastIndexOf(char) : -1;
    if (at >= 0) stack.splice(at);
    else if (canOpen) stack.push({ char, len });
  }
  return stack.map((s) => s.char.repeat(s.len)).reverse();
}

/**
 * Makes the unfinished end of a streaming answer displayable without flashes:
 * closes open emphasis and code spans, hides a link whose target is still
 * arriving (its text shows plainly), hides a lone trailing `*`/`_`/`` ` ``,
 * and holds back table rows until the delimiter row makes them a table. Pass
 * only the last block's source; an unclosed code fence already renders as code.
 */
export function completeStreamingMarkdown(src: string): string {
  const firstLine = src.split('\n', 1)[0] ?? '';
  if (/^ {0,3}(`{3,}|~{3,})/.test(firstLine) || /^( {4}|\t)/.test(firstLine)) return src;

  let lines = src.split('\n');
  const parsed = parseMarkdownBlocks(src);
  if (!parsed.some((b) => b.type === 'table')) {
    // Table header (and partial delimiter row) not yet a table: hold them back.
    while (lines.length && /^\s*\|/.test(lines[lines.length - 1] as string))
      lines = lines.slice(0, -1);
  }
  let s = lines.join('\n');

  // A link or image whose target is still arriving shows its text only.
  s = s.replace(/!?\[([^\]\n]*)\]\([^)\s]*$/, '$1');
  s = s.replace(/!?\[([^\]\n^][^\]\n]*)\]$/, '$1');
  s = s.replace(/!?\[\^?[^\]\n]*$/, (m) => (m.startsWith('[^') ? '' : m.replace(/^!?\[/, '')));
  // A delimiter run with nothing after it yet.
  s = s.replace(/(^|\s)[*_~`]+$/, '$1');

  // Close an open code span.
  let open = 0;
  for (const m of s.matchAll(ESCAPE_OR_CODE)) {
    if (m[0][0] !== '`') continue;
    if (open === 0) open = m[0].length;
    else if (open === m[0].length) open = 0;
  }
  if (open) return `${s.replace(/\s+$/, '')}${'`'.repeat(open)}`;

  const closers = openEmphasis(s);
  return closers.length ? `${s.replace(/\s+$/, '')}${closers.join('')}` : s;
}

const SENTENCE_END = /[.!?…。！？][”’"')\]]*(?=\s)/g;

const CARET = <span className="mn-ai-streaming-text__caret" aria-hidden="true" />;

const spokenCache = new WeakMap<MarkdownBlock, string>();

/**
 * Renders a growing Markdown answer. Completed blocks are parsed once and
 * not re-rendered; only the last block updates as tokens arrive, with
 * half-finished syntax closed for display so nothing flashes. While
 * `streaming` it shows a caret and sets `aria-busy`, and a throttled polite
 * live region announces complete sentences (never single tokens), then
 * "Response complete".
 */
export const AiStreamingText = forwardRef<HTMLDivElement, AiStreamingTextProps>(
  function AiStreamingText(
    {
      text,
      streaming,
      announce = true,
      announceInterval = 1000,
      caret = true,
      messages,
      markdownMessages,
      headingLevelOffset,
      allowImages,
      externalLinksInNewTab,
      renderCode,
      codeBlockProps,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_STREAMING_TEXT_MESSAGES, ...messages };

    // Incremental parse: every block before the last keeps its identity.
    const parseRef = useRef<MarkdownParseState | null>(null);
    const parsed = parseMarkdownIncremental(text, parseRef.current);
    parseRef.current = parsed;

    const blocks = useMemo(() => {
      const all = parsed.blocks;
      const last = all[all.length - 1];
      if (!streaming || !last) return all;
      return [...all.slice(0, -1), ...parseMarkdownBlocks(completeStreamingMarkdown(last.raw))];
    }, [parsed, streaming]);

    // Spoken text: completed blocks are cached per block object.
    const { codeBlock, table } = m;
    const speakOptions = useMemo(
      () => ({ code: () => codeBlock, table: () => table }),
      [codeBlock, table],
    );
    const spokenOf = (b: MarkdownBlock, cache: boolean) => {
      const hit = cache ? spokenCache.get(b) : undefined;
      if (hit !== undefined) return hit;
      const value = blockText(b, speakOptions).trim();
      if (cache) spokenCache.set(b, value);
      return value;
    };
    const done = blocks
      .slice(0, streaming ? -1 : undefined)
      .map((b) => spokenOf(b, true))
      .filter(Boolean)
      .join('\n\n');
    const tailBlock = streaming ? blocks[blocks.length - 1] : undefined;
    const tail = tailBlock ? spokenOf(tailBlock, false) : '';
    const spoken = done && tail ? `${done}\n\n${tail}` : done || tail;

    const [live, setLive] = useState('');
    const latest = useRef({ spoken, done: done.length, streaming, complete: m.complete });
    latest.current = { spoken, done: done.length, streaming, complete: m.complete };
    const status = useRef({
      announced: 0,
      prefix: '',
      last: Number.NEGATIVE_INFINITY,
      timer: undefined as ReturnType<typeof setTimeout> | undefined,
      wasStreaming: false,
    });
    const interval = Math.max(1000, announceInterval);

    useEffect(() => {
      if (!announce) return;
      const s = status.current;
      if (streaming) s.wasStreaming = true;
      const flush = () => {
        const now = latest.current;
        // Text that was never streamed here (history) is not announced.
        if (!now.streaming && !s.wasStreaming) return;
        // A different answer (regenerate, reset): start over.
        if (!now.spoken.startsWith(s.prefix)) s.announced = 0;
        let boundary = now.spoken.length;
        if (now.streaming) {
          boundary = now.done;
          for (const match of now.spoken.slice(now.done).matchAll(SENTENCE_END)) {
            boundary = now.done + (match.index ?? 0) + match[0].length;
          }
        }
        const finished = !now.streaming && s.wasStreaming;
        const chunk = boundary > s.announced ? now.spoken.slice(s.announced, boundary).trim() : '';
        if (!chunk && !finished) return;
        const wait = s.last + interval - Date.now();
        if (wait > 0) {
          s.timer ??= setTimeout(() => {
            s.timer = undefined;
            flush();
          }, wait);
          return;
        }
        s.announced = Math.max(s.announced, boundary);
        s.prefix = now.spoken.slice(0, s.announced);
        s.last = Date.now();
        if (finished) s.wasStreaming = false;
        setLive(finished ? [chunk, now.complete].filter(Boolean).join('\n\n') : chunk);
      };
      flush();
    }, [announce, spoken, streaming, interval]);

    useEffect(
      () => () => {
        clearTimeout(status.current.timer);
      },
      [],
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-streaming-text', className)}
        data-streaming={streaming || undefined}
      >
        <AiMarkdown
          className="mn-ai-streaming-text__content"
          blocks={blocks}
          streaming={streaming}
          trailing={streaming && caret ? CARET : undefined}
          aria-busy={streaming || undefined}
          messages={markdownMessages}
          {...(headingLevelOffset !== undefined ? { headingLevelOffset } : {})}
          {...(allowImages !== undefined ? { allowImages } : {})}
          {...(externalLinksInNewTab !== undefined ? { externalLinksInNewTab } : {})}
          {...(renderCode ? { renderCode } : {})}
          {...(codeBlockProps ? { codeBlockProps } : {})}
        />
        {announce ? (
          // Outside the busy subtree, so screen readers do not hold it back.
          <div className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            {live}
          </div>
        ) : null}
      </div>
    );
  },
);
