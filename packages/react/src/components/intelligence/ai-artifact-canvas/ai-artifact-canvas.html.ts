/**
 * Makes untrusted HTML inert for the artifact preview frame by tokenising
 * its tags (not by pattern-matching the text): `<meta http-equiv>`, `<base>`
 * and `<script>` are removed, links lose their targets (the destination is
 * kept as a tooltip and `data-href`), and event handlers, `action`,
 * `formaction`, `ping`, `srcdoc` and `target` are dropped. A sandboxed
 * frame without scripts can still navigate itself when a link is followed;
 * without targets there is nothing to follow. Deterministic, so the server
 * and the browser produce the same `srcdoc`.
 */

interface Attr {
  name: string;
  value: string | null;
}

/** Elements whose content is text, not markup. */
const RAW_TEXT = new Set([
  'script',
  'style',
  'textarea',
  'title',
  'xmp',
  'noscript',
  'iframe',
  'noembed',
  'noframes',
]);
/** Elements removed with their content. */
const DROP_WITH_CONTENT = new Set(['script', 'noscript', 'template']);
/** Elements removed (void or not: only the tag). */
const DROP_TAG = new Set(['base']);
/** Attributes that would navigate, submit, ping or embed a document. */
const DROP_ATTR = new Set(['action', 'formaction', 'ping', 'srcdoc', 'target', 'http-equiv']);
const LINK_ATTR = new Set(['href', 'xlink:href']);

const escapeAttr = (v: string) =>
  v.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/** Reads the attributes from `html[i]` up to the end of the tag. */
function readAttributes(
  html: string,
  start: number,
): { attrs: Attr[]; end: number; selfClosing: boolean } {
  const attrs: Attr[] = [];
  let i = start;
  let selfClosing = false;
  while (i < html.length) {
    const ch = html[i] as string;
    if (ch === '>') return { attrs, end: i + 1, selfClosing };
    if (/\s/.test(ch)) {
      i += 1;
      continue;
    }
    if (ch === '/') {
      selfClosing = html[i + 1] === '>';
      i += 1;
      continue;
    }
    let name = '';
    while (i < html.length && !/[\s/>=]/.test(html[i] as string)) name += html[i++];
    while (i < html.length && /\s/.test(html[i] as string)) i += 1;
    if (html[i] !== '=') {
      attrs.push({ name, value: null });
      continue;
    }
    i += 1;
    while (i < html.length && /\s/.test(html[i] as string)) i += 1;
    const quote = html[i];
    let value = '';
    if (quote === '"' || quote === "'") {
      const close = html.indexOf(quote, i + 1);
      value = html.slice(i + 1, close < 0 ? html.length : close);
      i = close < 0 ? html.length : close + 1;
    } else {
      while (i < html.length && !/[\s>]/.test(html[i] as string)) value += html[i++];
    }
    attrs.push({ name, value });
  }
  return { attrs, end: html.length, selfClosing };
}

/** Decodes the few entities an attribute value may use to hide a keyword. */
function decodeEntities(v: string): string {
  return v
    .replace(/&#x([0-9a-f]+);?/gi, (_, h: string) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);?/g, (_, d: string) => String.fromCodePoint(Number(d)))
    .replace(/&quot;/gi, '"')
    .replace(/&apos;/gi, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&amp;/gi, '&');
}

function rebuild(name: string, attrs: Attr[], selfClosing: boolean): string {
  const out: string[] = [];
  for (const a of attrs) {
    const key = a.name.toLowerCase();
    if (!key || key.startsWith('on') || DROP_ATTR.has(key)) continue;
    if (LINK_ATTR.has(key)) {
      if (a.value) {
        const dest = decodeEntities(a.value).trim();
        out.push(`data-href="${escapeAttr(dest)}"`, `title="${escapeAttr(dest)}"`);
      }
      continue;
    }
    out.push(a.value === null ? a.name : `${a.name}="${escapeAttr(decodeEntities(a.value))}"`);
  }
  return `<${name}${out.length ? ' ' + out.join(' ') : ''}${selfClosing ? ' /' : ''}>`;
}

/** Untrusted HTML with navigation, refresh, base, scripts and handlers removed. */
export function sanitizeArtifactHtml(html: string): string {
  let out = '';
  let i = 0;
  while (i < html.length) {
    const lt = html.indexOf('<', i);
    if (lt < 0) {
      out += html.slice(i);
      break;
    }
    out += html.slice(i, lt);
    i = lt;
    if (html.startsWith('<!--', i)) {
      const end = html.indexOf('-->', i + 4);
      i = end < 0 ? html.length : end + 3;
      continue;
    }
    if (html[i + 1] === '!' || html[i + 1] === '?') {
      const end = html.indexOf('>', i);
      i = end < 0 ? html.length : end + 1;
      continue;
    }
    const closing = html[i + 1] === '/';
    const nameMatch = /^[a-zA-Z][^\s/>]*/.exec(html.slice(i + (closing ? 2 : 1), i + 130));
    if (!nameMatch) {
      out += '&lt;';
      i += 1;
      continue;
    }
    const name = nameMatch[0];
    const lower = name.toLowerCase();
    const { attrs, end, selfClosing } = readAttributes(html, i + (closing ? 2 : 1) + name.length);
    i = end;
    if (closing) {
      if (!DROP_WITH_CONTENT.has(lower) && !DROP_TAG.has(lower)) out += `</${name}>`;
      continue;
    }
    if (DROP_TAG.has(lower)) continue;
    if (lower === 'meta' && attrs.some((a) => a.name.toLowerCase() === 'http-equiv')) continue;
    if (RAW_TEXT.has(lower) || DROP_WITH_CONTENT.has(lower)) {
      const closeAt = html.toLowerCase().indexOf(`</${lower}`, i);
      const content = html.slice(i, closeAt < 0 ? html.length : closeAt);
      const after = closeAt < 0 ? html.length : html.indexOf('>', closeAt);
      i = after < 0 ? html.length : after + 1;
      if (DROP_WITH_CONTENT.has(lower) || lower === 'iframe') continue;
      out += `${rebuild(name, attrs, false)}${content}</${name}>`;
      continue;
    }
    out += rebuild(name, attrs, selfClosing);
  }
  return out;
}
