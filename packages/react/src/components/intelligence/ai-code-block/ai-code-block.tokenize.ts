/**
 * A deliberately small syntax tokenizer: keywords, strings, comments, numbers
 * (plus tags, attributes and properties for markup, JSON and CSS). It is not
 * a grammar; it only has to make common snippets easier to scan. Unknown
 * languages come back as one plain token.
 */

export type CodeTokenType =
  'plain' | 'keyword' | 'string' | 'comment' | 'number' | 'tag' | 'attr' | 'property';

export interface CodeToken {
  type: CodeTokenType;
  text: string;
}

interface CLike {
  kind: 'clike';
  keywords: ReadonlySet<string>;
  caseInsensitive?: boolean;
  line?: readonly string[];
  block?: readonly [string, string][];
  strings: readonly string[];
  /** Strings that may span lines (template literals, triple quotes, raw strings). */
  multiline?: readonly string[];
  /** Mark `"key":` strings as properties (JSON). */
  keys?: boolean;
  /** Mark `name:` inside braces as properties (CSS). */
  cssProps?: boolean;
  /** `#` starts a comment only at line start or after whitespace (shell). */
  hashAfterSpace?: boolean;
  /** `'` is a char literal only when closed within 4 characters (Rust lifetimes). */
  shortSingle?: boolean;
}

interface Markup {
  kind: 'markup';
}

type Grammar = CLike | Markup;

const words = (s: string) => new Set(s.split(/\s+/).filter(Boolean));

const JS_KEYWORDS = words(`
  abstract any as async await boolean break case catch class const constructor continue debugger declare
  default delete do else enum export extends false finally for from function get if implements import in
  infer instanceof interface is keyof let namespace never new null number of private protected public
  readonly return satisfies set static string super switch symbol this throw true try type typeof
  undefined unknown var void while with yield`);

const GRAMMARS: Record<string, Grammar> = {
  js: {
    kind: 'clike',
    keywords: JS_KEYWORDS,
    line: ['//'],
    block: [['/*', '*/']],
    strings: ["'", '"'],
    multiline: ['`'],
  },
  json: { kind: 'clike', keywords: words('true false null'), strings: ['"'], keys: true },
  python: {
    kind: 'clike',
    keywords: words(`
      False None True and as assert async await break class continue def del elif else except finally for
      from global if import in is lambda match case nonlocal not or pass raise return self try while with yield`),
    line: ['#'],
    strings: ["'", '"'],
    multiline: ['"""', "'''"],
  },
  bash: {
    kind: 'clike',
    keywords: words(`
      if then else elif fi for while until do done case esac function in return local export readonly
      declare unset shift source exit echo cd set true false`),
    line: ['#'],
    strings: ["'", '"'],
    hashAfterSpace: true,
  },
  css: {
    kind: 'clike',
    keywords: words('important inherit initial unset revert none auto'),
    block: [['/*', '*/']],
    strings: ["'", '"'],
    cssProps: true,
  },
  html: { kind: 'markup' },
  sql: {
    kind: 'clike',
    keywords: words(`
      select from where and or not null is in as on join left right inner outer full cross group by order
      having limit offset insert into values update set delete create table alter drop index view primary
      key foreign references default unique distinct union all case when then else end exists like between
      returning with asc desc count sum avg min max begin commit rollback transaction true false`),
    caseInsensitive: true,
    line: ['--'],
    block: [['/*', '*/']],
    strings: ["'"],
  },
  go: {
    kind: 'clike',
    keywords: words(`
      break case chan const continue default defer else fallthrough for func go goto if import interface
      map package range return select struct switch type var nil true false iota error string int bool
      byte rune float64 any`),
    line: ['//'],
    block: [['/*', '*/']],
    strings: ['"', "'"],
    multiline: ['`'],
  },
  rust: {
    kind: 'clike',
    keywords: words(`
      as async await break const continue crate dyn else enum extern false fn for if impl in let loop match
      mod move mut pub ref return self Self static struct super trait true type unsafe use where while
      Some None Ok Err String Vec Option Result bool u8 u16 u32 u64 usize i8 i16 i32 i64 isize f32 f64 str`),
    line: ['//'],
    block: [['/*', '*/']],
    strings: ['"'],
    shortSingle: true,
  },
};

const ALIASES: Record<string, string> = {
  javascript: 'js',
  jsx: 'js',
  mjs: 'js',
  cjs: 'js',
  ts: 'js',
  typescript: 'js',
  tsx: 'js',
  jsonc: 'json',
  py: 'python',
  sh: 'bash',
  shell: 'bash',
  zsh: 'bash',
  console: 'bash',
  scss: 'css',
  xml: 'html',
  svg: 'html',
  vue: 'html',
  postgres: 'sql',
  postgresql: 'sql',
  mysql: 'sql',
  sqlite: 'sql',
  golang: 'go',
  rs: 'rust',
};

/** The grammar id used for `language`, or null when there is none ("plain"). */
export function resolveCodeLanguage(language: string | undefined): string | null {
  const id = (language ?? '').trim().toLowerCase();
  if (!id) return null;
  const resolved = ALIASES[id] ?? id;
  return GRAMMARS[resolved] ? resolved : null;
}

const NUMBER =
  /^(?:0[xX][\da-fA-F_]+|0[bB][01_]+|(?:\d[\d_]*\.?[\d_]*|\.\d[\d_]*)(?:[eE][+-]?\d+)?)[a-zA-Z%]*/;
const IDENT = /^[A-Za-z_$][\w$]*/;

function push(out: CodeToken[], type: CodeTokenType, text: string) {
  if (!text) return;
  const last = out[out.length - 1];
  if (last && last.type === type) last.text += text;
  else out.push({ type, text });
}

function readString(code: string, i: number, quote: string, multiline: boolean): number {
  let j = i + quote.length;
  while (j < code.length) {
    if (code[j] === '\\') {
      j += 2;
      continue;
    }
    if (code.startsWith(quote, j)) return j + quote.length;
    if (!multiline && code[j] === '\n') return j;
    j += 1;
  }
  return code.length;
}

function tokenizeClike(code: string, g: CLike): CodeToken[] {
  const out: CodeToken[] = [];
  const delims = [
    ...(g.multiline ?? []).map((q) => [q, true] as const),
    ...g.strings.map((q) => [q, false] as const),
  ].sort((a, b) => b[0].length - a[0].length);
  let depth = 0;
  let i = 0;
  while (i < code.length) {
    const rest = code.slice(i, i + 4);
    const prev = i === 0 ? '\n' : (code[i - 1] as string);

    const block = g.block?.find(([open]) => code.startsWith(open, i));
    if (block) {
      const end = code.indexOf(block[1], i + block[0].length);
      const stop = end < 0 ? code.length : end + block[1].length;
      push(out, 'comment', code.slice(i, stop));
      i = stop;
      continue;
    }
    const line = g.line?.find((l) => code.startsWith(l, i));
    if (line && !(g.hashAfterSpace && !/\s/.test(prev))) {
      const end = code.indexOf('\n', i);
      const stop = end < 0 ? code.length : end;
      push(out, 'comment', code.slice(i, stop));
      i = stop;
      continue;
    }
    if (g.shortSingle && code[i] === "'") {
      const m = /^'(?:\\.|[^\\'\n]){1,4}'/.exec(code.slice(i, i + 8));
      if (m) {
        push(out, 'string', m[0]);
        i += m[0].length;
        continue;
      }
    }
    const delim = delims.find(([q]) => code.startsWith(q, i));
    if (delim) {
      const stop = readString(code, i, delim[0], delim[1]);
      const text = code.slice(i, stop);
      const isKey = g.keys && /^\s*:/.test(code.slice(stop, stop + 8));
      push(out, isKey ? 'property' : 'string', text);
      i = stop;
      continue;
    }
    if (/[\d.]/.test(rest[0] as string) && !/[\w$]/.test(prev)) {
      const m = NUMBER.exec(code.slice(i, i + 64));
      if (m && /\d/.test(m[0])) {
        push(out, 'number', m[0]);
        i += m[0].length;
        continue;
      }
    }
    if (g.cssProps && code[i] === '#' && depth > 0) {
      const m = /^#[\da-fA-F]{3,8}\b/.exec(code.slice(i, i + 10));
      if (m) {
        push(out, 'number', m[0]);
        i += m[0].length;
        continue;
      }
    }
    if (g.cssProps && code[i] === '@') {
      const m = /^@[\w-]+/.exec(code.slice(i, i + 32));
      if (m) {
        push(out, 'keyword', m[0]);
        i += m[0].length;
        continue;
      }
    }
    const ident = g.cssProps
      ? /^-{0,2}[A-Za-z_][\w-]*/.exec(code.slice(i, i + 128))
      : IDENT.exec(code.slice(i, i + 128));
    if (ident && !/[\w$]/.test(prev)) {
      const word = ident[0];
      const key = g.caseInsensitive ? word.toLowerCase() : word;
      if (g.cssProps && depth > 0 && /^\s*:/.test(code.slice(i + word.length, i + word.length + 8)))
        push(out, 'property', word);
      else push(out, g.keywords.has(key) ? 'keyword' : 'plain', word);
      i += word.length;
      continue;
    }
    if (code[i] === '{') depth += 1;
    if (code[i] === '}') depth = Math.max(0, depth - 1);
    push(out, 'plain', code[i] as string);
    i += 1;
  }
  return out;
}

function tokenizeMarkup(code: string): CodeToken[] {
  const out: CodeToken[] = [];
  let i = 0;
  while (i < code.length) {
    if (code.startsWith('<!--', i)) {
      const end = code.indexOf('-->', i + 4);
      const stop = end < 0 ? code.length : end + 3;
      push(out, 'comment', code.slice(i, stop));
      i = stop;
      continue;
    }
    const open = /^<\/?[A-Za-z][\w:.-]*/.exec(code.slice(i, i + 128));
    if (open) {
      push(out, 'tag', open[0]);
      i += open[0].length;
      // Attributes until the tag closes.
      while (i < code.length && code[i] !== '>') {
        if (code.startsWith('/>', i)) break;
        const q = code[i];
        if (q === '"' || q === "'") {
          const stop = readString(code, i, q, true);
          push(out, 'string', code.slice(i, stop));
          i = stop;
          continue;
        }
        const attr = /^[^\s=>"'/]+/.exec(code.slice(i, i + 128));
        if (attr) {
          push(out, 'attr', attr[0]);
          i += attr[0].length;
          continue;
        }
        push(out, 'plain', code[i] as string);
        i += 1;
      }
      const close = code.startsWith('/>', i) ? '/>' : code[i] === '>' ? '>' : '';
      push(out, 'tag', close);
      i += close.length;
      continue;
    }
    push(out, 'plain', code[i] as string);
    i += 1;
  }
  return out;
}

/** Splits code into coloured tokens for `language`; unknown languages are one plain token. */
export function tokenizeCode(code: string, language?: string): CodeToken[] {
  const id = resolveCodeLanguage(language);
  const grammar = id ? GRAMMARS[id] : undefined;
  if (!grammar) return code ? [{ type: 'plain', text: code }] : [];
  return grammar.kind === 'markup' ? tokenizeMarkup(code) : tokenizeClike(code, grammar);
}

/** Splits tokens at newlines: one token list per line. */
export function tokensToLines(tokens: readonly CodeToken[]): CodeToken[][] {
  const lines: CodeToken[][] = [[]];
  for (const token of tokens) {
    const parts = token.text.split('\n');
    parts.forEach((part, index) => {
      if (index > 0) lines.push([]);
      if (part) (lines[lines.length - 1] as CodeToken[]).push({ type: token.type, text: part });
    });
  }
  return lines;
}
