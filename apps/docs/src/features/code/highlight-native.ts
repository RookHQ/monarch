/**
 * Kotlin and Swift for the docs code blocks, beside the TSX highlighter
 * (highlight.ts). A module of its own, so pages without native code do not
 * load it: the native sections, the platform pages, and the build-time
 * renderer of guides and README sections (plugins/guides.ts) use it.
 */
import type { Token, TokenKind } from './highlight.js';

/** Languages with their own keywords; anything else is read as TSX. */
export type NativeLang = 'kotlin' | 'swift';

const NATIVE_KEYWORDS: Record<NativeLang, Set<string>> = {
  kotlin: new Set(
    'package import class interface object fun val var if else when for while do return break continue is in as null true false this super private public internal protected override open abstract sealed data enum companion by typealias suspend inline try catch finally throw lateinit const annotation operator infix out vararg'.split(
      ' ',
    ),
  ),
  swift: new Set(
    'import struct class enum protocol extension func var let if else guard return switch case default for in while repeat break continue static private public internal fileprivate final init self Self nil true false some any throws throw try catch async await where typealias associatedtype mutating override weak lazy do as is'.split(
      ' ',
    ),
  ),
};

/**
 * Kotlin and Swift: comments, strings (with Kotlin's `"""`), annotations and
 * property wrappers (`@Composable`, `@State`), words, numbers, punctuation.
 * No JSX rules, so `List<String>` is not read as a tag.
 */
const NATIVE_RE =
  /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|("""[\s\S]*?"""|"(?:\\.|[^"\\\n])*")|(@[A-Za-z_]\w*)|([A-Za-z_]\w*)|(\d+(?:\.\d+)?)|([{}()[\]<>/=;,.:?!&|+*-]+)/g;

/** `kotlin`, `kt`, `kts` and `swift` name a native language; anything else is TSX. */
export function nativeLang(lang: string | undefined): NativeLang | null {
  if (lang === 'kotlin' || lang === 'kt' || lang === 'kts') return 'kotlin';
  return lang === 'swift' ? 'swift' : null;
}

/** Tokens of Kotlin or Swift code. The text is exactly the source. */
export function highlightNative(code: string, lang: NativeLang): Token[] {
  const out: Token[] = [];
  const push = (kind: TokenKind | null, text: string) => {
    if (!text) return;
    const prev = out[out.length - 1];
    if (prev && prev.kind === kind) prev.text += text;
    else out.push({ kind, text });
  };
  const keywords = NATIVE_KEYWORDS[lang];
  let last = 0;
  for (const m of code.matchAll(NATIVE_RE)) {
    const at = m.index ?? 0;
    push(null, code.slice(last, at));
    const [whole, comment, str, annotation, word, num, punct] = m;
    if (comment) push('comment', comment);
    else if (str) push('string', str);
    else if (annotation) push('attr', annotation);
    // A capitalised word is a type or a composable, coloured like a JSX tag.
    else if (word) push(keywords.has(word) ? 'keyword' : /^[A-Z]/.test(word) ? 'tag' : null, word);
    else if (num) push('number', num);
    else if (punct) push('punct', punct);
    else push(null, whole);
    last = at + whole.length;
  }
  push(null, code.slice(last));
  return out;
}
