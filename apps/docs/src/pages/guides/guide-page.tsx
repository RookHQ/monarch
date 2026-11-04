import { marked } from 'marked';
import gettingStarted from '../../../../../docs/guides/getting-started.md?raw';
import theming from '../../../../../docs/guides/theming.md?raw';
import migration from '../../../../../docs/guides/migration.md?raw';
import marketing from '../../../../../docs/guides/marketing.md?raw';
import patterns from '../../../../../docs/guides/patterns.md?raw';
import conformance from '../../../../../docs/guides/conformance.md?raw';
import { PageFrame } from '../../layout/page-frame.js';
import { REPO_URL } from '../../layout/top-bar.js';

const GUIDES = {
  'getting-started': gettingStarted,
  theming,
  migration,
  marketing,
  patterns,
  conformance,
} as const;
export type GuideId = keyof typeof GUIDES;

/** Guides that have a page on this site; links to them stay on the site. */
const ROUTES: Record<string, string> = {
  'getting-started.md': '#/getting-started',
  'theming.md': '#/theming',
  'migration.md': '#/migration',
  'marketing.md': '#/marketing',
  'patterns.md': '#/patterns',
  'conformance.md': '#/conformance',
};

const slug = (text: string) =>
  text
    .toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/&[a-z]+;/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

/** Repository-relative links point at the file on GitHub, or at the page on this site. */
function rewriteHref(link: string): string {
  if (/^[a-z]+:|^#|^\/\//i.test(link)) return link;
  const [file = '', hash] = link.split('#');
  const name = file.split('/').pop() ?? '';
  if (ROUTES[name] && !file.includes('/')) return ROUTES[name];
  const url = new URL(file, `${REPO_URL}/blob/main/docs/guides/`).toString();
  return hash ? `${url}#${hash}` : url;
}

/**
 * Renders one Markdown guide from `docs/guides`, so the site cannot drift from
 * the repository. The source is trusted (this repo), which is why no HTML
 * sanitiser is needed; never feed user content through this.
 */
function render(md: string): { title: string; lead: string | null; html: string } {
  const [, title = '', rest = md] = /^#\s+(.+)\n([\s\S]*)$/.exec(md) ?? [];
  // The first paragraph becomes the page lead.
  const leadMatch = /^\s*([^\n#|`>-][^\n]*(?:\n[^\n#|`>-][^\n]*)*)\n/.exec(rest);
  const lead = leadMatch?.[1] ?? null;
  const body = lead && leadMatch ? rest.slice(leadMatch[0].length) : rest;
  let n = 0;
  const used = new Set<string>();
  const html = marked
    .parse(body, { async: false, gfm: true })
    .replace(/<h2>([\s\S]*?)<\/h2>/g, (_, inner: string) => {
      let id = slug(inner) || `section-${n}`;
      while (used.has(id)) id = `${id}-2`;
      used.add(id);
      return `<h2 id="${id}" data-toc>${inner}</h2>`;
    })
    .replace(/href="([^"]+)"/g, (_, link: string) => `href="${rewriteHref(link)}"`)
    // Code blocks can overflow horizontally; keyboard users must be able to
    // focus and scroll them (WCAG 2.1.1). Each gets a unique accessible name.
    .replace(/<pre>/g, () => `<pre tabindex="0" role="region" aria-label="Code example ${++n}">`)
    .replace(/<table>/g, '<div class="docs-table-scroll"><table>')
    .replace(/<\/table>/g, '</table></div>');
  return { title, lead: lead ? marked.parseInline(lead, { async: false }) : null, html };
}

export function GuidePage({ guide, eyebrow }: { guide: GuideId; eyebrow: string }) {
  const { title, lead, html } = render(GUIDES[guide]);
  return (
    <PageFrame
      eyebrow={eyebrow}
      title={title}
      lead={lead ? <span dangerouslySetInnerHTML={{ __html: lead }} /> : undefined}
    >
      <div className="docs-prose" dangerouslySetInnerHTML={{ __html: html }} />
    </PageFrame>
  );
}
