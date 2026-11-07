import { Button } from '@rookcom/monarch-react';
import { HelpCenterHome, type HelpArticle } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const articles: HelpArticle[] = [
  {
    id: 'a1',
    title: 'Reset your password',
    category: 'Account',
    href: '#reset',
    updated: 'Mar 2, 2026',
  },
  {
    id: 'a2',
    title: 'Change the email on your account',
    category: 'Account',
    href: '#email',
    updated: 'Feb 11, 2026',
  },
  {
    id: 'a3',
    title: 'Turn on two-step sign-in',
    category: 'Account',
    href: '#two-step',
    updated: 'Jan 28, 2026',
  },
  {
    id: 'a4',
    title: 'Download an invoice',
    category: 'Billing',
    href: '#invoice',
    updated: 'Mar 1, 2026',
  },
  {
    id: 'a5',
    title: 'Update your payment card',
    category: 'Billing',
    href: '#card',
    updated: 'Feb 20, 2026',
  },
  {
    id: 'a6',
    title: 'Invite people to your team',
    category: 'Teams',
    href: '#invite',
    updated: 'Feb 2, 2026',
  },
];

const contact = (
  <Button variant="secondary" asChild>
    <a href="#new-request">Submit a request</a>
  </Button>
);

export default {
  id: 'help-center-home',
  title: 'HelpCenterHome',
  summary: 'A public help center home: search as you type and articles grouped by category.',
  sections: {
    purpose: {
      summary:
        'Customers look for an answer before they ask. HelpCenterHome is the help center’s first page: a title, a search that filters as you type, and the published articles under their categories, with a way to contact support when nothing fits. Needed by: Tickets (customer portal help center).',
      useWhen: [
        'A public support site lists knowledge-base articles.',
        'A product docs index groups pages by topic with a quick filter.',
      ],
      avoidWhen: [
        'Searching thousands of articles on the server: keep this layout, but pass the server’s results as `articles` and control `query`.',
        'A single FAQ list on a marketing page: use FAQ.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in: the app loads the articles and builds each `href`; search runs on the list it is given.',
        'Every word must match, ignoring case and accents, so "cafe" finds "Café".',
        'Never a dead end: an empty search offers the contact action, and so does a site without a help center.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`. Also exports `filterHelpArticles(articles, query)` and `groupHelpArticles(articles, general?)` for server-side or custom lists.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`articles={null}` shows loading; `error` shows an alert; `unavailable` replaces the search with a message and `contact`.',
        'Migrating from the app copy (Tickets `components/portal/HelpCenter.tsx`): keep `useKnowledge()` and the portal context in the app. Map `KnowledgeArticleSummary` to `{ id, title, category, href: `${base}/help/${id}`, updated: formatDate(updatedAt) }`; `q.isLoading` → `articles={null}`; `q.error` → `error`; `!config.knowledgeEnabled` → `unavailable`; `template.layout.articles` → `layout` (`columns`, `rows`, `cards`, `dense`; `tiles` maps to `cards` and `magazine` stays in the app); the docs frame title → `messages.title`; `ContactCta` and the "Submit a request" link → `contact`; `DossierAttribution` → `footer`; `initialQuery` → `defaultQuery`. `ArticleView` (the article page) stays in the app.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['columns', 'rows', 'cards', 'dense'],
          description:
            'Category columns with a document icon; a ruled list with update dates; cards with "Read article"; a compact multi-column index.',
        },
        {
          prop: 'headingAs',
          values: ['h1', 'h2'],
          description: 'The page title level; categories are one level below.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy="true"',
          description: '`articles={null}`: a list skeleton named "Loading articles".',
        },
        {
          name: 'Error',
          attribute: 'role="status"',
          description: '`error`: a danger alert instead of the list.',
        },
        { name: 'Empty', description: 'No articles published yet.' },
        { name: 'No results', description: 'The search matched nothing: a message and `contact`.' },
        {
          name: 'Unavailable',
          description: 'The site has no help center: a message and `contact`.',
        },
        { name: 'Layout', attribute: 'data-layout', description: 'The list layout.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters immediately and calls `onQueryChange`; the clear button and Escape empty the search.',
        'Enter does not submit anywhere (the form only marks the search landmark).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'From the search to each article link in reading order.' },
        { keys: 'Escape', action: 'In the search: clear it.' },
        { keys: 'Enter', action: 'On an article: open it.' },
      ],
    },
    accessibility: {
      role: 'search landmark, then one region per category',
      name: 'The search is named "Search articles"; each category region is named by its heading.',
      items: [
        'The number of matches is announced in a status region while searching.',
        'Headings follow the page title: `h1` then `h2` categories (or `h2` then `h3`).',
        'Article links are named by their titles; icons and chevrons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Columns, cards and dense layouts reflow by available width down to one column at 390px.',
        'Rows hide the update date on narrow screens; every article link is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Display heading tokens for the title; surface, subtle and border tokens for cards and rules.',
        'Forced colours use LinkText for articles and CanvasText rules; reduced motion removes hover transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Columns with a contact action',
          stack: true,
          render: () => <HelpCenterHome articles={articles} headingAs="h2" contact={contact} />,
        },
        {
          title: 'Rows',
          stack: true,
          render: () => <HelpCenterHome articles={articles} layout="rows" headingAs="h2" />,
        },
        {
          title: 'Cards, filtered',
          stack: true,
          render: () => (
            <HelpCenterHome
              articles={articles}
              layout="cards"
              headingAs="h2"
              defaultQuery="account"
            />
          ),
        },
        {
          title: 'Loading',
          stack: true,
          render: () => <HelpCenterHome articles={null} headingAs="h2" />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `defaultQuery` from the URL so shared search links work.',
        'Give every site a way to contact support in `contact`.',
      ],
      dont: [
        'Do not render article bodies here; link to the article page.',
        'Do not show unpublished or internal articles.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-site-shell', reason: 'Header and footer around the help center.' },
        { id: 'search-input', reason: 'The search field.' },
        { id: 'csat-survey', reason: 'The other public support page.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['alert', 'empty-state', 'loading-state', 'search-input'],
    },
    testing: {
      unit: [
        'Title, search landmark and category regions with links.',
        'Filters as you type (accents ignored) and announces the count.',
        'No results with contact; loading, error, empty and unavailable.',
        'Rows, cards and dense layouts; controlled query; h2 heading; router link; translation; helpers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
