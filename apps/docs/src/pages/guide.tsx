import { marked } from 'marked';
import gettingStarted from '../../../../docs/guides/getting-started.md?raw';
import theming from '../../../../docs/guides/theming.md?raw';
import migration from '../../../../docs/guides/migration.md?raw';
import marketing from '../../../../docs/guides/marketing.md?raw';

const GUIDES = { 'getting-started': gettingStarted, theming, migration, marketing } as const;

/**
 * Guides are rendered from the repository's own Markdown at build time, so the
 * site cannot drift from the docs. The source is trusted (this repo), which is
 * why no HTML sanitiser is needed; never feed user content through this.
 */
export function GuidePage({ guide }: { guide: keyof typeof GUIDES }) {
  let n = 0;
  const html = marked
    .parse(GUIDES[guide], { async: false, gfm: true })
    // Code blocks can overflow horizontally; keyboard users must be able to
    // focus and scroll them (WCAG 2.1.1). Each gets a unique accessible name.
    .replace(/<pre>/g, () => `<pre tabindex="0" role="region" aria-label="Code example ${++n}">`);
  return <article className="docs-prose" dangerouslySetInnerHTML={{ __html: html }} />;
}
