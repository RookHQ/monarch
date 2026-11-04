/**
 * Input hygiene shared by the editor's link, image and paste handling.
 *
 * ProseMirror already drops anything its schema does not describe (scripts,
 * inline styles, event handlers, unknown tags), so pasted HTML can never add
 * markup the editor cannot produce itself. These helpers add the two things
 * the schema cannot decide: which URLs are allowed, and which clipboard
 * junk (Office conditional comments, style blocks) is removed before parsing.
 *
 * Products must still sanitise stored HTML on the server; the editor's job is
 * to never produce unsafe output, not to make untrusted input safe.
 */

/** Protocols a link may use. Relative links (`/docs`, `#top`) are allowed too. */
export const LINK_PROTOCOLS = ['http', 'https', 'mailto'] as const;

const SCHEME = /^([a-z][a-z0-9+.-]*):/i;

/**
 * True when `url` is a relative URL or uses an allowed protocol. Rejects
 * `javascript:`, `data:`, `vbscript:` and anything else.
 */
export function isSafeUrl(url: string, protocols: readonly string[] = LINK_PROTOCOLS): boolean {
  // Browsers ignore control characters and whitespace inside a scheme
  // ("java\nscript:"), so compare without them.
  const trimmed = url.trim();
  if (!trimmed) return false;
  // eslint-disable-next-line no-control-regex -- control characters are exactly what is removed.
  const compact = trimmed.replace(/[\u0000- \u007f-\u009f]/g, '');
  const scheme = SCHEME.exec(compact);
  if (!scheme) return !compact.startsWith('//') || protocols.includes('https');
  return protocols.includes((scheme[1] ?? '').toLowerCase());
}

/** Image sources allowed in the document: http(s) and same-origin paths, plus `blob:` previews. */
export function isSafeImageSrc(src: string): boolean {
  return isSafeUrl(src, ['http', 'https', 'blob']);
}

/**
 * Turns what someone typed into a link target: keeps safe URLs, adds
 * `https://` to a bare domain ("example.com/x"), `mailto:` to an address, and
 * returns null for anything unsafe or empty.
 */
export function normalizeLinkInput(input: string): string | null {
  const value = input.trim();
  if (!value) return null;
  if (SCHEME.test(value)) return isSafeUrl(value) ? value : null;
  if (/^[^\s@/]+@[^\s@/]+\.[^\s@/]+$/.test(value)) return `mailto:${value}`;
  if (/^(\/|#|\?|\.\/|\.\.\/)/.test(value)) return value;
  if (/^[^\s/]+\.[^\s/]+/.test(value)) return `https://${value}`;
  return null;
}

/**
 * Removes clipboard markup that is never content: Office conditional
 * comments, `<style>`/`<script>`/`<meta>`/`<link>` blocks and XML namespaces.
 * Runs before ProseMirror parses the HTML against the schema.
 */
export function cleanPastedHtml(html: string): string {
  return html
    .replace(/<!--\[if[\s\S]*?<!\[endif\]-->/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<(style|script|template|noscript|iframe|object|embed)\b[\s\S]*?<\/\1\s*>/gi, '')
    .replace(/<(meta|link|base)\b[^>]*>/gi, '')
    .replace(/<\/?(o|w|v|m):[^>]*>/gi, '');
}
