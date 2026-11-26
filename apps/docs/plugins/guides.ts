/**
 * Vite plugin: `docs/guides/<name>.md?guide` imports a guide already rendered
 * to HTML at build time, `{ title, lead, html }`. The site ships the HTML, not
 * Markdown and a Markdown parser, and every page renders in one pass.
 *
 * The source is trusted (this repository), which is why no HTML sanitiser is
 * needed; never feed user content through this.
 */
import { readFileSync } from 'node:fs';
import { marked } from 'marked';
import type { Plugin } from 'vite';

export interface RenderedGuide {
  title: string;
  /** The first paragraph, as inline HTML. */
  lead: string | null;
  html: string;
}

const REPO_URL = 'https://github.com/RookHQ/monarch';
const QUERY = '?guide';

/** Guides that have a page on this site (path without the base); links to them stay on the site. */
const ROUTES: Record<string, string> = {
  'getting-started.md': 'getting-started/',
  'theming.md': 'theming/',
  'migration.md': 'migration/',
  'marketing.md': 'marketing/',
  'patterns.md': 'patterns/',
  'conformance.md': 'conformance/',
};

const slug = (text: string) =>
  text
    .toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/&[a-z]+;/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

/** Repository-relative links point at the file on GitHub, or at the page on this site. */
function rewriteHref(link: string, base: string): string {
  // `#/__marketing`: a screenshot route, which lives in the fragment of the site root.
  if (link.startsWith('#/')) return `${base}${link}`;
  if (/^[a-z]+:|^#|^\/\//i.test(link)) return link;
  const [file = '', hash] = link.split('#');
  const name = file.split('/').pop() ?? '';
  const route = ROUTES[name];
  if (route && !file.includes('/')) return `${base}${route}${hash ? `#${hash}` : ''}`;
  const url = new URL(file, `${REPO_URL}/blob/main/docs/guides/`).toString();
  return hash ? `${url}#${hash}` : url;
}

/** Renders one guide. Exported for tests. */
export function renderGuide(md: string, base = '/'): RenderedGuide {
  const [, title = '', rest = md] = /^#\s+(.+)\n([\s\S]*)$/.exec(md) ?? [];
  // The first paragraph becomes the page lead.
  const leadMatch = /^\s*([^\n#|`>-][^\n]*(?:\n[^\n#|`>-][^\n]*)*)\n/.exec(rest);
  const lead = leadMatch?.[1] ?? null;
  const body = lead && leadMatch ? rest.slice(leadMatch[0].length) : rest;
  let n = 0;
  const used = new Set<string>();
  const links = (html: string) =>
    html.replace(/href="([^"]+)"/g, (_, link: string) => `href="${rewriteHref(link, base)}"`);
  const html = links(
    marked
      .parse(body, { async: false, gfm: true })
      .replace(/<h2>([\s\S]*?)<\/h2>/g, (_, inner: string) => {
        let id = slug(inner) || `section-${n}`;
        while (used.has(id)) id = `${id}-2`;
        used.add(id);
        return `<h2 id="${id}" data-toc>${inner}</h2>`;
      }),
  )
    // Code blocks can overflow horizontally; keyboard users must be able to
    // focus and scroll them (WCAG 2.1.1). Each gets a unique accessible name.
    .replace(/<pre>/g, () => `<pre tabindex="0" role="region" aria-label="Code example ${++n}">`)
    .replace(/<table>/g, '<div class="docs-table-scroll"><table>')
    .replace(/<\/table>/g, '</table></div>');
  return { title, lead: lead ? links(marked.parseInline(lead, { async: false })) : null, html };
}

export function guides(): Plugin {
  let base = '/';
  return {
    name: 'monarch-guides',
    enforce: 'pre',
    configResolved(config) {
      base = config.base.endsWith('/') ? config.base : `${config.base}/`;
    },
    load(id) {
      if (!id.endsWith(`.md${QUERY}`)) return undefined;
      const file = id.slice(0, -QUERY.length);
      this.addWatchFile(file);
      return `export default ${JSON.stringify(renderGuide(readFileSync(file, 'utf8'), base))};`;
    },
  };
}
