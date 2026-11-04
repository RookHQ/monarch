/**
 * Search, answer-engine and AI-crawler files for the docs site: structured
 * data, llms.txt, robots.txt. Every fact comes from the site's own data
 * (counts, summaries, registry); nothing is typed in by hand except the
 * identity of the project itself.
 */

export const SITE = {
  name: 'Monarch',
  tagline: 'An open-source design system for everyone who builds.',
  description:
    'Monarch is an open-source design system: accessible React components, design tokens, light, dark and high-contrast themes, and your own brand colour. Free under the MIT License.',
  repo: 'https://github.com/RookHQ/monarch',
  license: 'https://opensource.org/license/mit',
  maker: { name: 'Rook', url: 'https://rookhq.com' },
  packages: [
    [
      '@rookcom/monarch-tokens',
      'Design tokens: colour, type, spacing, radius, elevation, motion; CSS variables and JSON.',
    ],
    [
      '@rookcom/monarch-react',
      'Accessible React components and patterns, plus the shell and editor subpaths.',
    ],
    ['@rookcom/monarch-conformance', 'Checks that an app follows the system.'],
  ],
  ogImage: {
    path: 'og.png',
    width: 1200,
    height: 630,
    alt: 'Meet Monarch. An open-source design system.',
  },
};

/** Escapes `<` so a JSON-LD block can never close its own script tag. */
const json = (o) => JSON.stringify(o).replace(/</g, '\\u003c');

/** The JSON-LD block of a page: identity, breadcrumbs and (below the home page) the article. */
export function jsonLd({ route, title, description, url, siteUrl, crumbs, modified, notFound }) {
  if (notFound) return '';
  const id = (frag) => `${siteUrl}#${frag}`;
  const org = {
    '@type': 'Organization',
    '@id': id('maker'),
    name: SITE.maker.name,
    url: SITE.maker.url,
    sameAs: ['https://github.com/RookHQ'],
  };
  const website = {
    '@type': 'WebSite',
    '@id': id('website'),
    url: siteUrl,
    name: SITE.name,
    description: SITE.description,
    inLanguage: 'en',
    publisher: { '@id': id('maker') },
  };
  const software = {
    '@type': 'SoftwareSourceCode',
    '@id': id('software'),
    name: SITE.name,
    description: SITE.description,
    codeRepository: SITE.repo,
    url: siteUrl,
    license: SITE.license,
    programmingLanguage: 'TypeScript',
    isAccessibleForFree: true,
    author: { '@id': id('maker') },
    publisher: { '@id': id('maker') },
    sameAs: SITE.packages.map(([name]) => `https://www.npmjs.com/package/${name}`),
  };
  const graph = [];
  if (route === '/') graph.push(org, website, software);
  else {
    graph.push({
      '@type': 'TechArticle',
      '@id': `${url}#article`,
      headline: title.replace(/ · Monarch$/, ''),
      description,
      url,
      inLanguage: 'en',
      dateModified: modified,
      isPartOf: { '@id': id('website') },
      about: { '@id': id('software') },
      author: { '@id': id('maker') },
      publisher: { '@id': id('maker') },
      mainEntityOfPage: url,
    });
  }
  if (crumbs.length > 1)
    graph.push({
      '@type': 'BreadcrumbList',
      itemListElement: crumbs.map((c, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        name: c.name,
        item: c.url,
      })),
    });
  return `<script type="application/ld+json">${json({ '@context': 'https://schema.org', '@graph': graph })}</script>`;
}

/** llms.txt (https://llmstxt.org): what the site is and where its Markdown pages are. */
export function llmsTxt({ siteUrl, mdUrl, data }) {
  const { facts, nav, components } = data;
  const list = (items) =>
    items.map(
      (i) =>
        `- [${i.label ?? i.title}](${mdUrl(i.path)})${i.description || i.summary ? `: ${i.description ?? i.summary}` : ''}`,
    );
  const sections = [];
  for (const g of nav) {
    if (g.heading === 'Components' || g.heading === 'Patterns' || g.heading === 'Marketing')
      continue;
    sections.push(`## ${g.heading}\n\n${list(g.items).join('\n')}`);
  }
  const byLayer = (layer) => components.filter((c) => c.layer === layer);
  sections.push(`## Components\n\n${list(byLayer('components')).join('\n')}`);
  sections.push(`## Patterns\n\n${list(byLayer('patterns')).join('\n')}`);
  const optional = [
    `- [Full documentation in one file](${new URL('llms-full.txt', siteUrl)}): every page below, as Markdown`,
    `- [Source code on GitHub](${SITE.repo})`,
    `- [Sitemap](${new URL('sitemap.xml', siteUrl)})`,
  ];
  return `# ${SITE.name}

> ${SITE.description}

Monarch has ${facts.components} components and ${facts.patterns} patterns, ${facts.themes} themes, and ${facts.examples} live examples. Install it with \`npm i @rookcom/monarch-tokens @rookcom/monarch-react\`. It is MIT licensed and made by [${SITE.maker.name}](${SITE.maker.url}). Every page has a Markdown copy at the same address ending in \`index.md\`, or ask for \`Accept: text/markdown\`.

## Packages

${SITE.packages.map(([n, d]) => `- [${n}](https://www.npmjs.com/package/${n}): ${d}`).join('\n')}

${sections.join('\n\n')}

## Optional

${optional.join('\n')}
`;
}

const AI_CRAWLERS = [
  'GPTBot',
  'OAI-SearchBot',
  'ChatGPT-User',
  'ClaudeBot',
  'Claude-SearchBot',
  'Claude-User',
  'PerplexityBot',
  'Perplexity-User',
  'Google-Extended',
  'Applebot-Extended',
  'Applebot',
  'Bingbot',
  'DuckAssistBot',
  'MistralAI-User',
  'Amazonbot',
  'CCBot',
  'cohere-ai',
  'Meta-ExternalAgent',
];

export function robotsTxt(siteUrl) {
  const named = AI_CRAWLERS.map((ua) => `User-agent: ${ua}\nAllow: /\n`).join('\n');
  return `# Monarch is open source and free to read, index, quote and learn from.
User-agent: *
Allow: /

# Search and AI assistants are welcome by name.
${named}
Sitemap: ${new URL('sitemap.xml', siteUrl)}
`;
}
