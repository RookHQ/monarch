/**
 * A small TSX highlighter for the docs code blocks. It only colours tokens;
 * the text is always exactly the source, so copy and screen readers get the
 * real code. Colours come from Monarch `*-strong` text tokens (AA on surfaces).
 */
export type TokenKind = 'comment' | 'string' | 'keyword' | 'tag' | 'attr' | 'number' | 'punct';
export interface Token {
  kind: TokenKind | null;
  text: string;
}

const KEYWORDS = new Set(
  'import from export default const let var function return if else type interface as new true false null undefined async await typeof extends satisfies'.split(
    ' ',
  ),
);

/** Order matters: comments and strings first, so their contents are never re-tokenised. */
const TOKEN_RE =
  /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|('(?:\\.|[^'\\\n])*'|"(?:\\.|[^"\\\n])*"|`(?:\\.|[^`\\])*`)|(<\/?)([A-Za-z][\w.]*)|([A-Za-z_$][\w$-]*)(?==)|([A-Za-z_$][\w$]*)|(\d+(?:\.\d+)?)|([{}()[\]<>/=;,.:?!&|+*-]+)/g;

export function highlight(code: string): Token[] {
  const out: Token[] = [];
  let last = 0;
  const push = (kind: TokenKind | null, text: string) => {
    if (!text) return;
    const prev = out[out.length - 1];
    if (prev && prev.kind === kind) prev.text += text;
    else out.push({ kind, text });
  };
  for (const m of code.matchAll(TOKEN_RE)) {
    const at = m.index ?? 0;
    push(null, code.slice(last, at));
    const [whole, comment, str, open, tag, attr, word, num, punct] = m;
    if (comment) push('comment', comment);
    else if (str) push('string', str);
    else if (open && tag) {
      push('punct', open);
      push('tag', tag);
    } else if (attr) push('attr', attr);
    else if (word) push(KEYWORDS.has(word) ? 'keyword' : null, word);
    else if (num) push('number', num);
    else if (punct) push('punct', punct);
    else push(null, whole);
    last = at + whole.length;
  }
  push(null, code.slice(last));
  return out;
}
