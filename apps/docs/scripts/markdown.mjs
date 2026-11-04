/**
 * Turns the docs site's own prerendered HTML into Markdown, for the `.md`
 * copy of every page that AI assistants and crawlers can read cheaply.
 *
 * Deliberately small: it understands the handful of elements the docs render
 * (headings, text, lists, code, tables, links) and drops everything that is
 * interface rather than content (navigation, buttons, live previews, icons).
 */

const VOID = new Set([
  'br',
  'hr',
  'img',
  'input',
  'meta',
  'link',
  'source',
  'wbr',
  'path',
  'circle',
  'rect',
  'line',
]);
const SKIP_TAGS = new Set([
  'script',
  'style',
  'svg',
  'button',
  'nav',
  'form',
  'input',
  'select',
  'textarea',
  'iframe',
  'template',
  'noscript',
]);

const ENTITIES = {
  amp: '&',
  lt: '<',
  gt: '>',
  quot: '"',
  apos: "'",
  nbsp: ' ',
  hellip: '…',
  mdash: '—',
  ndash: '–',
  middot: '·',
};
const decode = (s) =>
  s.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/gi, (m, e) => {
    if (e[0] === '#')
      return String.fromCodePoint(
        e[1] === 'x' || e[1] === 'X' ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10),
      );
    return ENTITIES[e.toLowerCase()] ?? m;
  });

function parse(html) {
  const root = { tag: '#root', attrs: {}, children: [] };
  const stack = [root];
  const re =
    /<!--[\s\S]*?-->|<\/([a-zA-Z][\w:-]*)\s*>|<([a-zA-Z][\w:-]*)((?:\s+[^\s=>/]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+))?)*)\s*(\/?)>|([^<]+)/g;
  for (let m = re.exec(html); m; m = re.exec(html)) {
    if (m[0].startsWith('<!--')) continue;
    if (m[5] !== undefined) {
      stack.at(-1).children.push({ text: m[5] });
    } else if (m[1]) {
      const tag = m[1].toLowerCase();
      const at = stack.map((n) => n.tag).lastIndexOf(tag);
      if (at > 0) stack.length = at;
    } else if (m[2]) {
      const tag = m[2].toLowerCase();
      const attrs = {};
      for (const a of m[3].matchAll(/([^\s=>/]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g))
        attrs[a[1]] = decode(a[2] ?? a[3] ?? a[4] ?? '');
      const node = { tag, attrs, children: [] };
      stack.at(-1).children.push(node);
      if (!m[4] && !VOID.has(tag)) stack.push(node);
    }
  }
  return root;
}

const classes = (n) => (n.attrs?.class ?? '').split(/\s+/);
const hidden = (n) =>
  n.attrs?.['aria-hidden'] === 'true' ||
  'hidden' in (n.attrs ?? {}) ||
  classes(n).some((c) => c.startsWith('docs-example__preview') || c === 'docs-example__canvas');

function inline(node, base) {
  if (node.text !== undefined) return decode(node.text).replace(/\s+/g, ' ');
  if (SKIP_TAGS.has(node.tag) || hidden(node)) return '';
  const inner = node.children.map((c) => inline(c, base)).join('');
  switch (node.tag) {
    case 'code':
      return inner.trim() ? '`' + inner.replace(/`/g, "'").trim() + '`' : '';
    case 'strong':
    case 'b':
      return inner.trim() ? `**${inner.trim()}**` : '';
    case 'em':
    case 'i':
      return inner.trim() ? `*${inner.trim()}*` : '';
    case 'br':
      return ' ';
    case 'a': {
      const href = node.attrs.href;
      if (!href || !inner.trim()) return inner;
      return `[${inner.trim()}](${new URL(href, base).toString()})`;
    }
    default:
      return inner;
  }
}

const text = (node, base) => inline(node, base).replace(/\s+/g, ' ').trim();

function plain(node) {
  if (node.text !== undefined) return decode(node.text);
  if (SKIP_TAGS.has(node.tag) && node.tag !== 'pre') return '';
  return node.children.map(plain).join('');
}

function blocks(node, base, out, depth = 0) {
  for (const child of node.children) {
    if (child.text !== undefined) {
      const t = decode(child.text).replace(/\s+/g, ' ').trim();
      if (t) out.push(t);
      continue;
    }
    if (SKIP_TAGS.has(child.tag) || hidden(child)) continue;
    switch (child.tag) {
      case 'h1':
      case 'h2':
      case 'h3':
      case 'h4': {
        const t = text(child, base);
        if (t) out.push(`${'#'.repeat(Number(child.tag[1]))} ${t}`);
        break;
      }
      case 'p': {
        const t = text(child, base);
        if (t) out.push(t);
        break;
      }
      case 'pre': {
        const code = plain(child).replace(/\n+$/, '');
        if (code.trim()) out.push('```tsx\n' + code + '\n```');
        break;
      }
      case 'ul':
      case 'ol': {
        const lines = [];
        child.children
          .filter((c) => c.tag === 'li')
          .forEach((li, i) => {
            const t = text(li, base);
            if (t)
              lines.push(`${'  '.repeat(depth)}${child.tag === 'ol' ? `${i + 1}.` : '-'} ${t}`);
          });
        if (lines.length) out.push(lines.join('\n'));
        break;
      }
      case 'table': {
        const rows = [];
        const walk = (n) => {
          for (const c of n.children) {
            if (c.tag === 'tr')
              rows.push(
                c.children
                  .filter((x) => x.tag === 'th' || x.tag === 'td')
                  .map((x) => text(x, base).replace(/\|/g, '\\|')),
              );
            else if (c.children) walk(c);
          }
        };
        walk(child);
        if (rows.length) {
          const width = Math.max(...rows.map((r) => r.length));
          const line = (r) => `| ${[...r, ...Array(width - r.length).fill('')].join(' | ')} |`;
          out.push(
            [line(rows[0]), line(Array(width).fill('---')), ...rows.slice(1).map(line)].join('\n'),
          );
        }
        break;
      }
      case 'blockquote': {
        const inner = [];
        blocks(child, base, inner, depth);
        if (inner.length) out.push(inner.join('\n\n').replace(/^/gm, '> '));
        break;
      }
      case 'hr':
        out.push('---');
        break;
      default:
        blocks(child, base, out, depth);
    }
  }
}

/** Markdown of the first element with this id in `html` (the page's `<main>`). */
export function mainToMarkdown(html, base, id = 'main') {
  const tree = parse(html);
  const find = (n) => {
    if (n.attrs?.id === id) return n;
    for (const c of n.children ?? []) {
      const f = find(c);
      if (f) return f;
    }
    return null;
  };
  const main = find(tree);
  if (!main) throw new Error(`markdown: no element with id="${id}"`);
  const out = [];
  blocks(main, base, out);
  return (
    out
      .join('\n\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim() + '\n'
  );
}
