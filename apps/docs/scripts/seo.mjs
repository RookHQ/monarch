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
    'Monarch is an open-source design system: accessible React components, design tokens, light, dark and high-contrast themes, and your own brand colour. Released under the Apache License 2.0.',
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

/**
 * The JSON-LD block of a page: identity, breadcrumbs and (below the home page)
 * the article. A platform landing page (`landing`, from
 * features/native/platform-landing-content.ts) also describes the platform
 * library as source code that is part of Monarch, and its FAQ.
 */
export function jsonLd({
  route,
  title,
  description,
  url,
  siteUrl,
  crumbs,
  modified,
  notFound,
  landing = null,
}) {
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
  if (landing) {
    graph.push({
      '@type': 'SoftwareSourceCode',
      '@id': `${url}#software`,
      name: landing.software.name,
      description: landing.software.description,
      codeRepository: SITE.repo,
      url,
      license: 'https://www.apache.org/licenses/LICENSE-2.0',
      programmingLanguage: landing.software.programmingLanguage,
      runtimePlatform: landing.software.runtimePlatform,
      ...(landing.software.version ? { version: landing.software.version } : {}),
      isPartOf: { '@id': id('software') },
      author: { '@id': id('maker') },
      publisher: { '@id': id('maker') },
    });
    graph.push({
      '@type': 'FAQPage',
      '@id': `${url}#faq`,
      url,
      inLanguage: 'en',
      mainEntity: landing.faq.map((f) => ({
        '@type': 'Question',
        name: f.question,
        acceptedAnswer: { '@type': 'Answer', text: f.answer },
      })),
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
/**
 * How an assistant works with Monarch in an app: the commands and entries of
 * \`@rookcom/monarch-conformance\`. Kept here, next to the text it is printed in.
 */
const TOOLS = [
  '- Find a component: `npx monarch search <query>`; its import, stylesheets and an example: `npx monarch add <Component>`. An MCP server gives the same answers: `npx monarch mcp`.',
  '- Check the app: `npx monarch check` (`--fix` applies safe fixes; `--format sarif` carries the fixes too).',
  "- Lint while writing: `import monarch from '@rookcom/monarch-conformance/eslint'` and add `monarch.configs.recommended`, or `monarch.configs.general` for teams outside Rook (ESLint 9 flat config, rules `monarch/*`).",
  '- Start a new app: `npx @rookcom/monarch-conformance create <dir> --template next-app|vite-react [--accent <hex>]`. It copies a starter and never runs an install.',
  '- Rules for agents in an app: `npx monarch init-agents` writes AGENTS.md, CLAUDE.md, Cursor and Copilot instructions.',
  '- Rook-specific exports (`scope: "rook"` in the manifest) are imported from `@rookcom/monarch-react/rook`. Outside Rook, use the general components.',
  '- Figma Code Connect files from the manifest: `npx monarch figma-connect --map <file.json>`. Monarch publishes no Figma library.',
];

export function llmsTxt({ siteUrl, mdUrl, data, llmsFiles = [] }) {
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
    ...llmsFiles.map(
      (f) => `- [${f.title}](${f.url}): ${f.pages} page${f.pages === 1 ? '' : 's'}, as Markdown`,
    ),
    `- [Component manifest](https://www.npmjs.com/package/@rookcom/monarch-react): \`@rookcom/monarch-react/manifest.json\`, every export with props, defaults, allowed values, import line and stylesheets ([JSON Schema](${new URL('schema/manifest.json', siteUrl)}))`,
    `- [Source code on GitHub](${SITE.repo})`,
    `- [Sitemap](${new URL('sitemap.xml', siteUrl)})`,
  ];
  return `# ${SITE.name}

> ${SITE.description}

Monarch has ${facts.components} components and ${facts.patterns} patterns, ${facts.themes} themes, and ${facts.examples} live examples. Install it with \`npm i @rookcom/monarch-tokens @rookcom/monarch-react\`. It is Apache-2.0 licensed and made by [${SITE.maker.name}](${SITE.maker.url}). Every page has a Markdown copy at the same address ending in \`index.md\`, or ask for \`Accept: text/markdown\`.

## Packages

${SITE.packages.map(([n, d]) => `- [${n}](https://www.npmjs.com/package/${n}): ${d}`).join('\n')}

${sections.join('\n\n')}

## Tools for coding agents

${TOOLS.join('\n')}

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
  return `# Monarch is open source: you may read, index, quote and learn from it.
User-agent: *
Allow: /

# Search and AI assistants are welcome by name.
${named}
Sitemap: ${new URL('sitemap.xml', siteUrl)}
`;
}
