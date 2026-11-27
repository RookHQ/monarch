/**
 * Vite plugin: `docs/guides/<name>.md?guide` imports a guide already rendered
 * to HTML at build time, `{ title, lead, html }`. The site ships the HTML, not
 * Markdown and a Markdown parser, and every page renders in one pass.
 *
 * The same renderer serves the native platform pages (plugins/native.ts):
 * guides outside `docs/guides`, the platform changelogs and sections of the
 * platform READMEs.
 *
 * The source is trusted (this repository), which is why no HTML sanitiser is
 * needed; never feed user content through this.
 */
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { marked } from 'marked';
import type { Plugin } from 'vite';
import { highlightNative, nativeLang } from '../src/features/code/highlight-native.js';

export interface RenderedGuide {
  title: string;
  /** The first paragraph, as inline HTML. */
  lead: string | null;
  html: string;
}

const REPO_URL = 'https://github.com/RookHQ/monarch';
const QUERY = '?guide';

/**
 * Repository files that have a page on this site (path without the base);
 * links to them stay on the site. A file that does not exist has no page, so
 * links to it go to the repository.
 */
export const GUIDE_ROUTES: Readonly<Record<string, string>> = {
  'docs/guides/getting-started.md': 'getting-started/',
  'docs/guides/theming.md': 'theming/',
  'docs/guides/migration.md': 'migration/',
  'docs/guides/marketing.md': 'marketing/',
  'docs/guides/patterns.md': 'patterns/',
  'docs/guides/conformance.md': 'conformance/',
  'docs/guides/native-adaptive-layout.md': 'native/adaptive-layout/',
  'docs/guides/native-accessibility-and-testing.md': 'native/accessibility/',
  'docs/guides/native-contributing.md': 'native/contributing/',
  'docs/architecture/NATIVE-COMPATIBILITY.md': 'native/compatibility/',
  'platforms/android/CHANGELOG.md': 'android/releases/',
  'platforms/ios/CHANGELOG.md': 'ios/releases/',
};

export interface RenderOptions {
  /** The site's base path, with a trailing slash. */
  base?: string;
  /** Repository-relative path of the Markdown file, for relative links. */
  source?: string;
  /** Repository root. When given, only files that exist count as pages of the site. */
  repoRoot?: string;
  /**
   * The HTML is a part of a file shown on another page (a README section), so
   * `#fragment` links point at the file in the repository, not at this page.
   */
  fragment?: boolean;
  /** Accessible name of the nth code block; names must be unique on a page. */
  codeLabel?: (n: number) => string;
}

export const slug = (text: string) =>
  text
    .toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/&[a-z]+;/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

const escapeHtml = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const unescapeHtml = (s: string) =>
  s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&');

/** Repository-relative links point at the file on GitHub, or at the page on this site. */
function rewriteHref(
  link: string,
  o: Required<Pick<RenderOptions, 'base' | 'source'>> & RenderOptions,
) {
  // `#/__marketing`: a screenshot route, which lives in the fragment of the site root.
  if (link.startsWith('#/')) return `${o.base}${link}`;
  if (/^[a-z]+:|^\/\//i.test(link)) return link;
  if (link.startsWith('#')) return o.fragment ? `${REPO_URL}/blob/main/${o.source}${link}` : link;
  const [file = '', hash] = link.split('#');
  const target = path.posix.normalize(path.posix.join(path.posix.dirname(o.source), file));
  const route = GUIDE_ROUTES[target];
  if (route && (!o.repoRoot || existsSync(path.join(o.repoRoot, target))))
    return `${o.base}${route}${hash ? `#${hash}` : ''}`;
  const url = `${REPO_URL}/blob/main/${target}`;
  return hash ? `${url}#${hash}` : url;
}

/**
 * Kotlin and Swift blocks are highlighted here, at build time, with the
 * highlighter of the site's code blocks; every block records its language
 * (`data-lang`) for the Markdown copy of the page.
 */
function codeBlocks(html: string): string {
  return html.replace(
    /<pre><code class="language-([\w-]+)">([\s\S]*?)<\/code><\/pre>/g,
    (_, lang: string, code: string) => {
      const native = nativeLang(lang);
      const body = native
        ? highlightNative(unescapeHtml(code), native)
            .map((t) =>
              t.kind
                ? `<span class="docs-tok" data-kind="${t.kind}">${escapeHtml(t.text)}</span>`
                : escapeHtml(t.text),
            )
            .join('')
        : code;
      return `<pre data-lang="${native ?? lang}"><code class="language-${lang}">${body}</code></pre>`;
    },
  );
}

/** Renders Markdown without its title: links rewritten, headings with ids, code blocks named. */
export function renderBody(body: string, options: RenderOptions = {}): string {
  const o = { base: '/', source: 'docs/guides/guide.md', ...options };
  let n = 0;
  const used = new Set<string>();
  const unique = (inner: string) => {
    let id = slug(inner) || `section-${n}`;
    while (used.has(id)) id = `${id}-2`;
    used.add(id);
    return id;
  };
  const label = o.codeLabel ?? ((i: number) => `Code example ${i}`);
  return (
    codeBlocks(
      marked
        .parse(body, { async: false, gfm: true })
        .replace(/href="([^"]+)"/g, (_, link: string) => `href="${rewriteHref(link, o)}"`)
        .replace(
          /<h2>([\s\S]*?)<\/h2>/g,
          (_, inner: string) => `<h2 id="${unique(inner)}" data-toc>${inner}</h2>`,
        )
        // Subsections can be linked to; only h2 headings are listed in "On this page".
        .replace(
          /<h3>([\s\S]*?)<\/h3>/g,
          (_, inner: string) => `<h3 id="${unique(inner)}">${inner}</h3>`,
        ),
    )
      // Code blocks can overflow horizontally; keyboard users must be able to
      // focus and scroll them (WCAG 2.1.1). Each gets a unique accessible name.
      .replace(/<pre/g, () => `<pre tabindex="0" role="region" aria-label="${label(++n)}"`)
      // A header row with an empty corner cell: an empty `th` has no name to
      // announce, so the corner is a plain cell.
      .replace(/<th><\/th>/g, '<td></td>')
      // Task lists (`- [ ]`) would be checkboxes nobody can use and without a
      // label: they are shown as plain list items, a done one with a tick.
      .replace(/<input (checked="" )?disabled="" type="checkbox">\s*/g, (_, done?: string) =>
        done ? '✓ ' : '',
      )
      .replace(/<table>/g, '<div class="docs-table-scroll"><table>')
      .replace(/<\/table>/g, '</table></div>')
  );
}

/** Renders one guide. Exported for tests. */
export function renderGuide(md: string, options: RenderOptions | string = {}): RenderedGuide {
  const o = typeof options === 'string' ? { base: options } : options;
  const [, title = '', rest = md] = /^#\s+(.+)\n([\s\S]*)$/.exec(md) ?? [];
  // The first paragraph becomes the page lead.
  const leadMatch = /^\s*([^\n#|`>-][^\n]*(?:\n[^\n#|`>-][^\n]*)*)\n/.exec(rest);
  const lead = leadMatch?.[1] ?? null;
  const body = lead && leadMatch ? rest.slice(leadMatch[0].length) : rest;
  const links = (html: string) =>
    html.replace(
      /href="([^"]+)"/g,
      (_, link: string) =>
        `href="${rewriteHref(link, { base: '/', source: 'docs/guides/guide.md', ...o })}"`,
    );
  return {
    title,
    lead: lead ? links(marked.parseInline(lead, { async: false })) : null,
    html: renderBody(body, o),
  };
}

export function guides({ repoRoot }: { repoRoot?: string } = {}): Plugin {
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
      const source = repoRoot
        ? path.relative(repoRoot, file).replaceAll('\\', '/')
        : `docs/guides/${path.basename(file)}`;
      return `export default ${JSON.stringify(
        renderGuide(readFileSync(file, 'utf8'), {
          base,
          source,
          ...(repoRoot ? { repoRoot } : {}),
        }),
      )};`;
    },
  };
}
