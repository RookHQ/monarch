/**
 * A small GitHub-flavoured Markdown parser for model output. It produces plain
 * data (block and inline tokens) that `AiMarkdown` turns into React elements,
 * so no HTML string is ever built and raw HTML in the source stays text.
 *
 * Supported: ATX and setext headings, paragraphs, emphasis, strong,
 * strikethrough, inline code, links, images, autolinks (`<…>` and bare
 * `https://` / `www.`), ordered, unordered, nested and task lists,
 * blockquotes, fenced and indented code, tables with alignment, thematic
 * breaks, footnotes and hard breaks. Not supported: link reference
 * definitions and HTML blocks (both render as text).
 */

/** An inline token inside a paragraph, heading, table cell or link. */
export type MarkdownInline =
  | { type: 'text'; value: string }
  | { type: 'code'; value: string }
  | { type: 'em' | 'strong' | 'del'; children: MarkdownInline[] }
  | {
      type: 'link';
      href: string;
      title?: string;
      /** True for `<https://…>` and bare `https://…` / `www.…` links. */
      autolink?: boolean;
      children: MarkdownInline[];
    }
  | { type: 'image'; src: string; alt: string; title?: string }
  | { type: 'break' }
  | { type: 'footnoteRef'; label: string };

export type MarkdownAlign = 'left' | 'center' | 'right' | null;

export interface MarkdownListItem {
  /** `true` / `false` for a task list item (`- [x]`, `- [ ]`), otherwise `null`. */
  checked: boolean | null;
  children: MarkdownBlock[];
}

interface BlockBase {
  /** The source lines of the block, as written (container markers removed when nested). */
  raw: string;
}

/** A block token. Top-level blocks are independent, so a renderer can memoise each one. */
export type MarkdownBlock =
  | (BlockBase & { type: 'heading'; level: 1 | 2 | 3 | 4 | 5 | 6; content: MarkdownInline[] })
  | (BlockBase & { type: 'paragraph'; content: MarkdownInline[] })
  | (BlockBase & {
      type: 'code';
      /** First word of the info string, lower-cased ("ts"), or "". */
      lang: string;
      /** The whole info string after the fence ("ts title=app.ts"). */
      info: string;
      /** `title="…"` or `filename=…` from the info string, if any. */
      filename?: string;
      code: string;
      /** False when the closing fence has not arrived (yet). */
      closed: boolean;
    })
  | (BlockBase & { type: 'blockquote'; children: MarkdownBlock[] })
  | (BlockBase & {
      type: 'list';
      ordered: boolean;
      start: number;
      /** Loose lists wrap item text in paragraphs; tight lists do not. */
      loose: boolean;
      items: MarkdownListItem[];
    })
  | (BlockBase & {
      type: 'table';
      align: MarkdownAlign[];
      header: MarkdownInline[][];
      rows: MarkdownInline[][][];
    })
  | (BlockBase & { type: 'thematicBreak' })
  | (BlockBase & { type: 'footnoteDefinition'; label: string; children: MarkdownBlock[] });

/* ------------------------------------------------------------------ */
/* Lines and indentation                                               */
/* ------------------------------------------------------------------ */

const isBlank = (line: string) => /^[ \t]*$/.test(line);

/** Indentation in columns (tabs advance to the next multiple of 4). */
function indentOf(line: string): number {
  let col = 0;
  for (const ch of line) {
    if (ch === ' ') col += 1;
    else if (ch === '\t') col += 4 - (col % 4);
    else break;
  }
  return col;
}

/** Removes up to `cols` columns of leading whitespace, splitting a tab if needed. */
function stripCols(line: string, cols: number): string {
  let col = 0;
  let i = 0;
  while (i < line.length && col < cols) {
    const ch = line[i];
    if (ch === ' ') col += 1;
    else if (ch === '\t') {
      const width = 4 - (col % 4);
      if (col + width > cols) return ' '.repeat(col + width - cols) + line.slice(i + 1);
      col += width;
    } else break;
    i += 1;
  }
  return line.slice(i);
}

/* ------------------------------------------------------------------ */
/* Block patterns                                                      */
/* ------------------------------------------------------------------ */

const FENCE_OPEN = /^( {0,3})(`{3,}|~{3,})(.*)$/;
const ATX = /^ {0,3}(#{1,6})(?:[ \t]+(.*?))?[ \t]*$/;
const HR = /^ {0,3}(?:(?:\*[ \t]*){3,}|(?:-[ \t]*){3,}|(?:_[ \t]*){3,})$/;
const QUOTE = /^ {0,3}> ?/;
const FOOTNOTE_DEF = /^ {0,3}\[\^([^\]\s]+)\]:[ \t]*(.*)$/;
const SETEXT_1 = /^ {0,3}=+[ \t]*$/;
const SETEXT_2 = /^ {0,3}-+[ \t]*$/;
const TABLE_DELIM = /^ {0,3}\|?[ \t]*:?-+:?[ \t]*(?:\|[ \t]*:?-+:?[ \t]*)*\|?[ \t]*$/;
const MARKER = /^( {0,3})([-+*]|\d{1,9}[.)])(?=[ \t]|$)/;

function fenceOpen(line: string) {
  const m = FENCE_OPEN.exec(line);
  if (!m) return null;
  const fence = m[2] ?? '';
  const info = m[3] ?? '';
  // A backtick fence's info string may not contain a backtick (that is inline code).
  if (fence[0] === '`' && info.includes('`')) return null;
  return { indent: (m[1] ?? '').length, fence, info: info.trim() };
}

interface Marker {
  ordered: boolean;
  /** Bullet character or ordered delimiter; items with a different key start a new list. */
  key: string;
  start: number;
  /** Column where the item's content starts. */
  contentCol: number;
  /** The first line's content. */
  first: string;
  empty: boolean;
}

function listMarker(line: string): Marker | null {
  const m = MARKER.exec(line);
  if (!m) return null;
  const lead = (m[1] ?? '').length;
  const marker = m[2] ?? '';
  const markerEnd = lead + marker.length;
  const after = line.slice(m[0].length);
  // Columns of whitespace after the marker.
  const padded = ' '.repeat(markerEnd) + after;
  const spaces = indentOf(padded) - markerEnd;
  const empty = isBlank(after);
  const contentCol = empty || spaces > 4 ? markerEnd + 1 : markerEnd + spaces;
  const ordered = /\d/.test(marker[0] ?? '');
  return {
    ordered,
    key: ordered ? marker.slice(-1) : marker,
    start: ordered ? Number.parseInt(marker, 10) : 1,
    contentCol,
    first: empty ? '' : stripCols(padded, contentCol),
    empty,
  };
}

function hasUnescapedPipe(line: string): boolean {
  return /(^|[^\\])\|/.test(line);
}

/** True when `line` ends a paragraph by starting another block. */
function interruptsParagraph(line: string): boolean {
  if (fenceOpen(line) || ATX.test(line) || HR.test(line) || QUOTE.test(line)) return true;
  if (FOOTNOTE_DEF.test(line)) return true;
  const mk = listMarker(line);
  // Only a non-empty item interrupts a paragraph; an ordered one must start at 1.
  return Boolean(mk && !mk.empty && (!mk.ordered || mk.start === 1));
}

/** Splits a table row into raw cell strings, honouring `\|` and code spans. */
function splitRow(line: string): string[] {
  let s = line.trim();
  if (s.startsWith('|')) s = s.slice(1);
  if (s.endsWith('|') && !s.endsWith('\\|')) s = s.slice(0, -1);
  const cells: string[] = [];
  let cell = '';
  let tick = 0;
  for (let i = 0; i < s.length; i += 1) {
    const ch = s[i] as string;
    if (ch === '\\' && s[i + 1] === '|') {
      cell += '|';
      i += 1;
    } else if (ch === '`') {
      let run = 1;
      while (s[i + run] === '`') run += 1;
      if (tick === 0) tick = run;
      else if (tick === run) tick = 0;
      cell += '`'.repeat(run);
      i += run - 1;
    } else if (ch === '|' && tick === 0) {
      cells.push(cell.trim());
      cell = '';
    } else cell += ch;
  }
  cells.push(cell.trim());
  return cells;
}

function tableStart(lines: string[], i: number): MarkdownAlign[] | null {
  const head = lines[i];
  const delim = lines[i + 1];
  if (head === undefined || delim === undefined) return null;
  if (!hasUnescapedPipe(head) || !TABLE_DELIM.test(delim)) return null;
  const cells = splitRow(delim);
  if (cells.length !== splitRow(head).length) return null;
  return cells.map((c) => {
    const left = c.startsWith(':');
    const right = c.endsWith(':');
    return left && right ? 'center' : left ? 'left' : right ? 'right' : null;
  });
}

function codeInfo(info: string): { lang: string; filename?: string } {
  const lang = (info.split(/\s+/)[0] ?? '').toLowerCase();
  const m = /(?:title|filename|file)=(?:"([^"]*)"|'([^']*)'|(\S+))/.exec(info);
  const filename = m ? (m[1] ?? m[2] ?? m[3]) : undefined;
  // "ts:app.ts" is a common shorthand.
  const colon = /^([\w+#-]+):(\S+)$/.exec(info.split(/\s+/)[0] ?? '');
  if (colon && !filename) return { lang: (colon[1] ?? '').toLowerCase(), filename: colon[2] ?? '' };
  return filename ? { lang, filename } : { lang };
}

/* ------------------------------------------------------------------ */
/* Block parser                                                        */
/* ------------------------------------------------------------------ */

interface Parsed {
  blocks: MarkdownBlock[];
  /** First line index of each block. */
  starts: number[];
  /** Line index after each block. */
  ends: number[];
}

function parseLines(lines: string[]): Parsed {
  const out: Parsed = { blocks: [], starts: [], ends: [] };
  const push = (block: MarkdownBlock, start: number, end: number) => {
    out.blocks.push(block);
    out.starts.push(start);
    out.ends.push(end);
  };
  const raw = (start: number, end: number) => lines.slice(start, end).join('\n');
  let i = 0;

  while (i < lines.length) {
    const line = lines[i] as string;
    if (isBlank(line)) {
      i += 1;
      continue;
    }
    const start = i;

    // Fenced code. An unclosed fence runs to the end, so a half-streamed
    // block already renders as code.
    const fence = fenceOpen(line);
    if (fence) {
      const close = new RegExp(
        `^ {0,3}${fence.fence[0] === '`' ? '`' : '~'}{${fence.fence.length},}[ \\t]*$`,
      );
      const body: string[] = [];
      let closed = false;
      i += 1;
      while (i < lines.length) {
        const l = lines[i] as string;
        i += 1;
        if (close.test(l)) {
          closed = true;
          break;
        }
        body.push(stripCols(l, fence.indent));
      }
      push(
        {
          type: 'code',
          ...codeInfo(fence.info),
          info: fence.info,
          code: body.join('\n'),
          closed,
          raw: raw(start, i),
        },
        start,
        i,
      );
      continue;
    }

    // Indented code.
    if (indentOf(line) >= 4) {
      let end = i;
      while (
        end < lines.length &&
        (isBlank(lines[end] as string) || indentOf(lines[end] as string) >= 4)
      )
        end += 1;
      while (end > i && isBlank(lines[end - 1] as string)) end -= 1;
      const code = lines
        .slice(i, end)
        .map((l) => stripCols(l, 4))
        .join('\n');
      push({ type: 'code', lang: '', info: '', code, closed: true, raw: raw(i, end) }, i, end);
      i = end;
      continue;
    }

    const atx = ATX.exec(line);
    if (atx) {
      const text = (atx[2] ?? '').replace(/(?:^|[ \t]+)#+$/, '').trim();
      const level = (atx[1] ?? '#').length as 1 | 2 | 3 | 4 | 5 | 6;
      i += 1;
      push({ type: 'heading', level, content: parseInline(text), raw: raw(start, i) }, start, i);
      continue;
    }

    if (HR.test(line)) {
      i += 1;
      push({ type: 'thematicBreak', raw: line }, start, i);
      continue;
    }

    if (QUOTE.test(line)) {
      const inner: string[] = [];
      while (i < lines.length) {
        const l = lines[i] as string;
        if (QUOTE.test(l)) inner.push(l.replace(QUOTE, ''));
        else if (
          !isBlank(l) &&
          inner.length > 0 &&
          !isBlank(inner[inner.length - 1] as string) &&
          !interruptsParagraph(l)
        )
          inner.push(l); // lazy continuation
        else break;
        i += 1;
      }
      push(
        { type: 'blockquote', children: parseLines(inner).blocks, raw: raw(start, i) },
        start,
        i,
      );
      continue;
    }

    const fn = FOOTNOTE_DEF.exec(line);
    if (fn) {
      const inner = [fn[2] ?? ''];
      i += 1;
      while (i < lines.length) {
        const l = lines[i] as string;
        if (isBlank(l)) {
          const next = lines.slice(i).find((x) => !isBlank(x));
          if (next === undefined || indentOf(next) < 4) break;
          inner.push('');
        } else if (indentOf(l) >= 4) inner.push(stripCols(l, 4));
        else if (!isBlank(inner[inner.length - 1] as string) && !interruptsParagraph(l))
          inner.push(l);
        else break;
        i += 1;
      }
      push(
        {
          type: 'footnoteDefinition',
          label: normalizeLabel(fn[1] ?? ''),
          children: parseLines(inner).blocks,
          raw: raw(start, i),
        },
        start,
        i,
      );
      continue;
    }

    const marker = listMarker(line);
    if (marker) {
      const list = parseList(lines, i, marker);
      i = list.end;
      push({ ...list.block, raw: raw(start, i) }, start, i);
      continue;
    }

    const align = tableStart(lines, i);
    if (align) {
      const header = splitRow(line).map((c) => parseInline(c));
      const rows: MarkdownInline[][][] = [];
      i += 2;
      while (i < lines.length) {
        const l = lines[i] as string;
        if (isBlank(l) || interruptsParagraph(l)) break;
        const cells = splitRow(l);
        rows.push(align.map((_, c) => parseInline(cells[c] ?? '')));
        i += 1;
      }
      push({ type: 'table', align, header, rows, raw: raw(start, i) }, start, i);
      continue;
    }

    // Paragraph, possibly turned into a setext heading.
    const para: string[] = [line];
    let level: 1 | 2 | null = null;
    i += 1;
    while (i < lines.length) {
      const l = lines[i] as string;
      if (isBlank(l)) break;
      if (SETEXT_1.test(l) || SETEXT_2.test(l)) {
        level = SETEXT_1.test(l) ? 1 : 2;
        i += 1;
        break;
      }
      if (interruptsParagraph(l) || tableStart(lines, i)) break;
      para.push(l);
      i += 1;
    }
    const text = para
      .map((l) => l.replace(/^[ \t]+/, ''))
      .join('\n')
      .replace(/[ \t]+$/, '');
    push(
      level
        ? { type: 'heading', level, content: parseInline(text), raw: raw(start, i) }
        : { type: 'paragraph', content: parseInline(text), raw: raw(start, i) },
      start,
      i,
    );
  }
  return out;
}

function parseList(lines: string[], from: number, first: Marker) {
  const items: MarkdownListItem[] = [];
  let loose = false;
  let i = from;
  while (i < lines.length) {
    const line = lines[i] as string;
    if (HR.test(line)) break;
    const mk = listMarker(line);
    if (!mk || mk.key !== first.key || mk.ordered !== first.ordered) break;
    const body = [mk.first];
    i += 1;
    while (i < lines.length) {
      const l = lines[i] as string;
      if (isBlank(l)) {
        body.push('');
      } else if (indentOf(l) >= mk.contentCol) {
        body.push(stripCols(l, mk.contentCol));
      } else {
        const prev = body[body.length - 1] as string;
        const openFences = body.filter((b) => fenceOpen(b)).length % 2 === 1;
        // Lazy continuation of the item's paragraph.
        if (!isBlank(prev) && !openFences && !interruptsParagraph(l) && !listMarker(l))
          body.push(l.trim());
        else break;
      }
      i += 1;
    }
    // Trailing blank lines belong between items, not to this item.
    let trailing = 0;
    while (body.length > 1 && isBlank(body[body.length - 1] as string)) {
      body.pop();
      trailing += 1;
    }
    i -= trailing;
    let checked: boolean | null = null;
    const task = /^\[([ xX])\](?:[ \t]+|$)/.exec(body[0] ?? '');
    if (task) {
      checked = task[1] !== ' ';
      body[0] = (body[0] ?? '').slice(task[0].length);
    }
    const parsed = parseLines(body);
    for (let c = 1; c < parsed.blocks.length; c += 1) {
      for (let l = parsed.ends[c - 1] as number; l < (parsed.starts[c] as number); l += 1) {
        if (isBlank(body[l] as string)) loose = true;
      }
    }
    items.push({ checked, children: parsed.blocks });
    if (trailing > 0) {
      const next = lines[i + trailing];
      const nm = next === undefined ? null : listMarker(next);
      if (nm && nm.key === first.key && !HR.test(next as string)) {
        loose = true;
        i += trailing;
      } else break;
    }
  }
  return {
    end: i,
    block: { type: 'list' as const, ordered: first.ordered, start: first.start, loose, items },
  };
}

/* ------------------------------------------------------------------ */
/* Inline parser                                                       */
/* ------------------------------------------------------------------ */

const ASCII_PUNCT = /[!-/:-@[-`{-~]/;
const PUNCT = /[\p{P}\p{S}]/u;
const SPACE = /\s/;

interface Delim {
  type: 'delim';
  char: '*' | '_' | '~';
  count: number;
  orig: number;
  canOpen: boolean;
  canClose: boolean;
}
type Node = MarkdownInline | Delim;

const ENTITIES: Record<string, string> = {
  amp: '&',
  lt: '<',
  gt: '>',
  quot: '"',
  apos: "'",
  nbsp: ' ',
  copy: '©',
  reg: '®',
  trade: '™',
  hellip: '…',
  mdash: '—',
  ndash: '–',
  lsquo: '‘',
  rsquo: '’',
  ldquo: '“',
  rdquo: '”',
};

function codePoint(n: number): string {
  return n > 0 && n <= 0x10ffff && !(n >= 0xd800 && n <= 0xdfff) ? String.fromCodePoint(n) : '�';
}

/** Footnote labels match case-insensitively. */
export function normalizeLabel(label: string): string {
  return label.trim().toLowerCase();
}

function findBacktickRun(src: string, from: number, run: number): number {
  let i = from;
  while (i < src.length) {
    if (src[i] === '`') {
      let r = 1;
      while (src[i + r] === '`') r += 1;
      if (r === run) return i;
      i += r;
    } else i += 1;
  }
  return -1;
}

function findClosingBracket(src: string, open: number): number {
  let depth = 0;
  for (let i = open; i < src.length; i += 1) {
    const ch = src[i];
    if (ch === '\\') i += 1;
    else if (ch === '`') {
      let run = 1;
      while (src[i + run] === '`') run += 1;
      const close = findBacktickRun(src, i + run, run);
      i = close >= 0 ? close + run - 1 : i + run - 1;
    } else if (ch === '[') depth += 1;
    else if (ch === ']') {
      depth -= 1;
      if (depth === 0) return i;
    }
  }
  return -1;
}

const unescapeText = (s: string) => s.replace(/\\([!-/:-@[-`{-~])/g, '$1');

function parseLinkAt(src: string, open: number) {
  const close = findClosingBracket(src, open);
  if (close < 0 || src[close + 1] !== '(') return null;
  let j = close + 2;
  const skip = () => {
    while (j < src.length && SPACE.test(src[j] as string)) j += 1;
  };
  skip();
  let href = '';
  if (src[j] === '<') {
    const end = src.indexOf('>', j);
    if (end < 0 || src.slice(j, end).includes('\n')) return null;
    href = src.slice(j + 1, end);
    j = end + 1;
  } else {
    const begin = j;
    let depth = 0;
    while (j < src.length) {
      const ch = src[j] as string;
      if (ch === '\\' && j + 1 < src.length) {
        j += 2;
        continue;
      }
      if (SPACE.test(ch)) break;
      if (ch === '(') depth += 1;
      if (ch === ')') {
        if (depth === 0) break;
        depth -= 1;
      }
      j += 1;
    }
    href = src.slice(begin, j);
  }
  skip();
  let title: string | undefined;
  const q = src[j];
  if (q === '"' || q === "'" || q === '(') {
    const endChar = q === '(' ? ')' : q;
    const end = src.indexOf(endChar, j + 1);
    if (end < 0) return null;
    title = unescapeText(src.slice(j + 1, end));
    j = end + 1;
    skip();
  }
  if (src[j] !== ')') return null;
  return { text: src.slice(open + 1, close), href: unescapeText(href), title, end: j + 1 };
}

const AUTOLINK_URI = /^<([a-zA-Z][a-zA-Z0-9+.-]{1,31}:[^<>\s]*)>/;
const AUTOLINK_EMAIL =
  /^<([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>/;
const BARE_URL = /^(?:https?:\/\/[^\s<]+|www\.[a-zA-Z0-9-][^\s<]*)/i;

function trimBareUrl(url: string): string {
  let u = url;
  for (;;) {
    const last = u[u.length - 1];
    if (last && '?!.,:*_~\'";'.includes(last)) u = u.slice(0, -1);
    else if (last === ')' && (u.match(/\(/g) ?? []).length < (u.match(/\)/g) ?? []).length)
      u = u.slice(0, -1);
    else return u;
  }
}

/** Parses inline Markdown into tokens. `links: false` inside link text (links do not nest). */
export function parseInline(src: string, links = true): MarkdownInline[] {
  const nodes: Node[] = [];
  let text = '';
  const flush = () => {
    if (text) nodes.push({ type: 'text', value: text });
    text = '';
  };
  let i = 0;
  while (i < src.length) {
    const c = src[i] as string;

    if (c === '\\') {
      const next = src[i + 1];
      if (next === '\n') {
        flush();
        nodes.push({ type: 'break' });
        i += 2;
        continue;
      }
      if (next !== undefined && ASCII_PUNCT.test(next)) {
        text += next;
        i += 2;
        continue;
      }
      text += c;
      i += 1;
      continue;
    }

    if (c === '`') {
      let run = 1;
      while (src[i + run] === '`') run += 1;
      const close = findBacktickRun(src, i + run, run);
      if (close >= 0) {
        flush();
        let value = src.slice(i + run, close).replace(/\n/g, ' ');
        if (value.length > 1 && value.startsWith(' ') && value.endsWith(' ') && value.trim())
          value = value.slice(1, -1);
        nodes.push({ type: 'code', value });
        i = close + run;
      } else {
        text += '`'.repeat(run);
        i += run;
      }
      continue;
    }

    if (c === '\n') {
      const hard = / {2,}$/.exec(text);
      if (hard) {
        text = text.slice(0, hard.index);
        flush();
        nodes.push({ type: 'break' });
      } else text = text.replace(/ +$/, '') + '\n';
      i += 1;
      while (src[i] === ' ' || src[i] === '\t') i += 1;
      continue;
    }

    if (c === '!' && src[i + 1] === '[') {
      const link = parseLinkAt(src, i + 1);
      if (link) {
        flush();
        const image: MarkdownInline = {
          type: 'image',
          src: link.href,
          alt: inlineText(parseInline(link.text, false)),
        };
        if (link.title !== undefined) image.title = link.title;
        nodes.push(image);
        i = link.end;
        continue;
      }
      text += c;
      i += 1;
      continue;
    }

    if (c === '[') {
      const ref = /^\[\^([^\]\s]+)\]/.exec(src.slice(i, i + 200));
      if (ref) {
        flush();
        nodes.push({ type: 'footnoteRef', label: normalizeLabel(ref[1] ?? '') });
        i += ref[0].length;
        continue;
      }
      const link = links ? parseLinkAt(src, i) : null;
      if (link) {
        flush();
        const node: MarkdownInline = {
          type: 'link',
          href: link.href,
          children: parseInline(link.text, false),
        };
        if (link.title !== undefined) node.title = link.title;
        nodes.push(node);
        i = link.end;
        continue;
      }
      text += c;
      i += 1;
      continue;
    }

    if (c === '<' && links) {
      const rest = src.slice(i, i + 2048);
      const uri = AUTOLINK_URI.exec(rest);
      const email = uri ? null : AUTOLINK_EMAIL.exec(rest);
      const m = uri ?? email;
      if (m) {
        flush();
        const value = m[1] ?? '';
        nodes.push({
          type: 'link',
          href: email ? `mailto:${value}` : value,
          autolink: true,
          children: [{ type: 'text', value }],
        });
        i += m[0].length;
        continue;
      }
    }

    if (c === '*' || c === '_' || c === '~') {
      let run = 1;
      while (src[i + run] === c) run += 1;
      const before = i === 0 ? ' ' : (src[i - 1] as string);
      const after = src[i + run] ?? ' ';
      const left =
        !SPACE.test(after) && (!PUNCT.test(after) || SPACE.test(before) || PUNCT.test(before));
      const right =
        !SPACE.test(before) && (!PUNCT.test(before) || SPACE.test(after) || PUNCT.test(after));
      let canOpen = left;
      let canClose = right;
      if (c === '_') {
        canOpen = left && (!right || PUNCT.test(before));
        canClose = right && (!left || PUNCT.test(after));
      }
      if (c === '~' && run > 2) {
        text += c.repeat(run);
      } else {
        flush();
        nodes.push({ type: 'delim', char: c, count: run, orig: run, canOpen, canClose });
      }
      i += run;
      continue;
    }

    if (c === '&') {
      const m = /^&(?:#(\d{1,7})|#[xX]([\da-fA-F]{1,6})|([a-zA-Z]+));/.exec(src.slice(i, i + 12));
      const named = m?.[3] ? ENTITIES[m[3]] : undefined;
      if (m && (m[1] || m[2] || named)) {
        text += m[1]
          ? codePoint(Number.parseInt(m[1], 10))
          : m[2]
            ? codePoint(Number.parseInt(m[2], 16))
            : (named as string);
        i += m[0].length;
        continue;
      }
    }

    if (links && (c === 'h' || c === 'H' || c === 'w' || c === 'W')) {
      const before = i === 0 ? ' ' : (src[i - 1] as string);
      if (SPACE.test(before) || '*_~('.includes(before)) {
        const m = BARE_URL.exec(src.slice(i, i + 2048));
        if (m) {
          const url = trimBareUrl(m[0]);
          if (
            /^(?:https?:\/\/|www\.)[^./\s]+(?:\.[^./\s]+)*/i.test(url) &&
            !/^https?:\/\/$/i.test(url)
          ) {
            flush();
            nodes.push({
              type: 'link',
              href: /^www\./i.test(url) ? `https://${url}` : url,
              autolink: true,
              children: [{ type: 'text', value: url }],
            });
            i += url.length;
            continue;
          }
        }
      }
    }

    text += c;
    i += 1;
  }
  flush();
  return finalize(processEmphasis(nodes));
}

/** CommonMark's "process emphasis" over the delimiter runs, closest opener first. */
function processEmphasis(nodes: Node[]): Node[] {
  let i = 0;
  while (i < nodes.length) {
    const closer = nodes[i] as Node;
    if (closer.type !== 'delim' || !closer.canClose || closer.count === 0) {
      i += 1;
      continue;
    }
    let matched = false;
    for (let j = i - 1; j >= 0; j -= 1) {
      const opener = nodes[j] as Node;
      if (
        opener.type !== 'delim' ||
        opener.char !== closer.char ||
        !opener.canOpen ||
        opener.count === 0
      )
        continue;
      if (
        closer.char !== '~' &&
        (opener.canClose || closer.canOpen) &&
        (opener.orig + closer.orig) % 3 === 0 &&
        !(opener.orig % 3 === 0 && closer.orig % 3 === 0)
      )
        continue;
      if (closer.char === '~' && opener.count !== closer.count) continue;
      const use =
        closer.char === '~' ? opener.count : opener.count >= 2 && closer.count >= 2 ? 2 : 1;
      const type = closer.char === '~' ? 'del' : use === 2 ? 'strong' : 'em';
      opener.count -= use;
      closer.count -= use;
      const inner = finalize(nodes.slice(j + 1, i));
      nodes.splice(j + 1, i - j - 1, { type, children: inner });
      i = j + 2;
      matched = true;
      break;
    }
    if (!matched) i += 1;
  }
  return nodes;
}

function finalize(nodes: Node[]): MarkdownInline[] {
  const out: MarkdownInline[] = [];
  for (const node of nodes) {
    const value = node.type === 'delim' ? node.char.repeat(node.count) : null;
    const next: MarkdownInline | null =
      value === null ? (node as MarkdownInline) : value ? { type: 'text', value } : null;
    if (!next) continue;
    const last = out[out.length - 1];
    if (next.type === 'text' && last?.type === 'text')
      out[out.length - 1] = { type: 'text', value: last.value + next.value };
    else out.push(next);
  }
  return out;
}

/* ------------------------------------------------------------------ */
/* Public API and helpers                                              */
/* ------------------------------------------------------------------ */

function splitLines(text: string): { lines: string[]; offsets: number[] } {
  const lines = text.replace(/\r\n?/g, '\n').split('\n');
  const offsets: number[] = [];
  let at = 0;
  for (const line of lines) {
    offsets.push(at);
    at += line.length + 1;
  }
  return { lines, offsets };
}

/**
 * Parses Markdown into top-level block tokens. Each top-level block is
 * independent of the ones after it, so a streaming renderer can keep (and
 * memoise) every block except the last.
 */
export function parseMarkdownBlocks(text: string): MarkdownBlock[] {
  return parseLines(splitLines(text).lines).blocks;
}

/** The result of `parseMarkdownIncremental`; pass it back in on the next update. */
export interface MarkdownParseState {
  text: string;
  blocks: MarkdownBlock[];
  /** Character offset of each block's first line. */
  offsets: number[];
}

/**
 * Re-parses only what can have changed. When `text` extends `previous.text`,
 * every block before the last is kept as is (same object), and parsing
 * restarts at the last block's first line. Anything else is a full parse.
 */
export function parseMarkdownIncremental(
  text: string,
  previous?: MarkdownParseState | null,
): MarkdownParseState {
  if (previous && previous.text === text) return previous;
  let keep = 0;
  let from = 0;
  if (
    previous &&
    previous.blocks.length > 1 &&
    text.startsWith(previous.text) &&
    !previous.text.includes('\r')
  ) {
    keep = previous.blocks.length - 1;
    from = previous.offsets[keep] as number;
  }
  const { lines, offsets } = splitLines(text.slice(from));
  const parsed = parseLines(lines);
  return {
    text,
    blocks: [...(previous?.blocks.slice(0, keep) ?? []), ...parsed.blocks],
    offsets: [
      ...(previous?.offsets.slice(0, keep) ?? []),
      ...parsed.starts.map((s) => from + (offsets[s] as number)),
    ],
  };
}

/** Plain text of inline tokens (images give their alt text). */
export function inlineText(nodes: readonly MarkdownInline[]): string {
  let out = '';
  for (const n of nodes) {
    if (n.type === 'text' || n.type === 'code') out += n.value;
    else if (n.type === 'break') out += '\n';
    else if (n.type === 'image') out += n.alt;
    else if (n.type === 'footnoteRef') out += '';
    else out += inlineText(n.children);
  }
  return out;
}

export interface BlockTextOptions {
  /** Spoken in place of a code block. Default: the code itself. */
  code?: (block: Extract<MarkdownBlock, { type: 'code' }>) => string;
  /** Spoken in place of a table. Default: cells separated by spaces. */
  table?: (block: Extract<MarkdownBlock, { type: 'table' }>) => string;
}

/** Plain text of a block, paragraphs separated by blank lines. Used for announcements. */
export function blockText(block: MarkdownBlock, options: BlockTextOptions = {}): string {
  switch (block.type) {
    case 'heading':
    case 'paragraph':
      return inlineText(block.content);
    case 'code':
      return options.code ? options.code(block) : block.code;
    case 'table':
      return options.table
        ? options.table(block)
        : [block.header, ...block.rows].map((r) => r.map(inlineText).join(' ')).join('\n');
    case 'blockquote':
    case 'footnoteDefinition':
      return block.children.map((b) => blockText(b, options)).join('\n\n');
    case 'list':
      return block.items
        .map((it) => it.children.map((b) => blockText(b, options)).join('\n\n'))
        .join('\n\n');
    case 'thematicBreak':
      return '';
  }
}
