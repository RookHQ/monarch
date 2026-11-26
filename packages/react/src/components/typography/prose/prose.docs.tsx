import { Prose, ProseExclude, Text } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'prose',
  title: 'Prose',
  summary: 'Long-form styles for rendered Markdown and HTML.',
  sections: {
    purpose: {
      summary:
        'Styles content you do not write element by element (rendered Markdown, help-centre articles, release notes, legal pages) with Monarch tokens. Replaces the Tailwind Typography `prose prose-sm dark:prose-invert …` class strings. Needed by: Rook Stride (Markdown comments and documents), Rook Tickets (knowledge panel), Rook GTM, the rook.com legal and article pages, Rook Links.',
      useWhen: [
        'Showing Markdown or sanitised HTML whose elements you do not control.',
        'A long article or document page that needs comfortable reading width and rhythm.',
      ],
      avoidWhen: [
        'UI you build with components: use Heading and Text directly.',
        'Editable rich text: use RichTextEditor, which has its own content styles.',
      ],
    },
    designPrinciples: {
      items: [
        'Every rule is wrapped in `:where()`, so Prose has the weight of one class and app styles override it easily.',
        'Anything with an `mn-` class (a Monarch component placed in the content) keeps its own styles; ProseExclude opts out anything else.',
        'Headings, code and spacing are in `em`, so `size` scales the whole document together.',
        'Prose only styles. It never renders HTML strings, so it cannot introduce script injection.',
        'Logical properties throughout, so lists, quotes and tables mirror in right-to-left.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Prose` renders `as` (div, article or section) and forwards its ref; attributes pass through.',
        '`ProseExclude` renders a `<div class="mn-prose__exclude">` whose content Prose leaves alone.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind Typography: `prose` becomes `<Prose>`, `prose-sm` becomes `size="sm"`, `prose-lg` becomes `size="lg"`, `max-w-none` becomes `measure={false}`, `not-prose` becomes ProseExclude. `dark:prose-invert` is not needed: tokens follow the theme.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'base', 'lg'],
          description: 'Base font size; everything inside scales with it. Exposed as `data-size`.',
        },
        {
          prop: 'measure',
          values: ['true', 'false'],
          description:
            'Limits the width to `--mn-layout-reading-max` (`data-measure`). Default true.',
        },
        {
          prop: 'as',
          values: ['div', 'article', 'section'],
          description: 'The wrapper element. Default `div`.',
        },
      ],
    },
    states: {
      items: [],
      notes: [
        'No states of its own; links, `<pre>` and `<summary>` show the focus ring when focused.',
      ],
    },
    interaction: {
      items: [
        'Links, details/summary and any focusable content inside work natively.',
        'Wide tables and code blocks scroll sideways inside themselves.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through links and other focusable content in reading order.',
        },
      ],
      notes: ['Prose adds no key handling; content keeps native behaviour.'],
    },
    accessibility: {
      items: [
        'Keeps the content’s own semantics: headings, lists, tables and quotes are what the renderer produced.',
        'Links are underlined, not distinguished by colour alone.',
        'Headings get `scroll-margin` so in-page links and TableOfContents do not hide them under a sticky header.',
        'Give tables header cells (`th scope`) in the source; Prose cannot add them.',
        'Security: render Markdown with raw HTML disabled, or sanitise HTML on the server. Never pass untrusted HTML through `dangerouslySetInnerHTML`.',
      ],
    },
    responsive: {
      items: [
        'Long words and inline code break instead of widening the page at 390px.',
        'Tables and `<pre>` become their own horizontal scroll areas; images and video are capped at the container width.',
      ],
    },
    theme: {
      items: [
        'Text, link, border, code and mark colours are semantic tokens, so light, dark and more-contrast themes apply with no `invert` class.',
        'Forced colours: links use LinkText, code and quotes keep visible borders, `<mark>` uses Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Rendered article',
          stack: true,
          render: () => (
            <Prose>
              <h2>Getting started</h2>
              <p>
                This guide covers the basics. Read the <a href="#/components/prose">overview</a>{' '}
                first, then run <code>npm install</code>.
              </p>
              <ul>
                <li>Create a project</li>
                <li>Invite your team</li>
                <li>
                  Set up notifications
                  <ul>
                    <li>Email</li>
                    <li>Mobile</li>
                  </ul>
                </li>
              </ul>
              <blockquote>
                <p>Small steps every day add up.</p>
              </blockquote>
              <pre>
                <code>{'const total = items.reduce((sum, i) => sum + i.price, 0);'}</code>
              </pre>
              <h3>Options</h3>
              <table>
                <thead>
                  <tr>
                    <th scope="col">Option</th>
                    <th scope="col">Default</th>
                    <th scope="col">Description</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <code>size</code>
                    </td>
                    <td>base</td>
                    <td>Base font size of the content.</td>
                  </tr>
                  <tr>
                    <td>
                      <code>measure</code>
                    </td>
                    <td>true</td>
                    <td>Limit the line length.</td>
                  </tr>
                </tbody>
              </table>
              <hr />
              <p>
                Press <kbd>Enter</kbd> to save. <mark>Highlighted</mark> text stands out.
              </p>
            </Prose>
          ),
        },
        {
          title: 'Small size for comments',
          stack: true,
          render: () => (
            <Prose size="sm" measure={false}>
              <p>
                Looks good to me. One question about the <strong>second step</strong>: should it run
                before the import?
              </p>
              <ol>
                <li>Check the file</li>
                <li>Import it</li>
              </ol>
            </Prose>
          ),
        },
        {
          title: 'Excluding embedded content',
          stack: true,
          render: () => (
            <Prose>
              <p>Paragraph styled by Prose.</p>
              <ProseExclude>
                <p>This paragraph keeps the browser defaults.</p>
              </ProseExclude>
              <Text size="sm" tone="muted">
                Monarch components such as this Text keep their own styles.
              </Text>
            </Prose>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Sanitise or render safely before passing content in.',
        'Use `size="sm"` in comments, chat and side panels.',
        'Keep the reading measure for articles; turn it off inside narrow containers.',
      ],
      dont: [
        'Do not wrap whole app pages in Prose to style UI.',
        'Do not add `dark:` or invert classes; tokens already follow the theme.',
        'Do not nest Prose inside Prose.',
      ],
    },
    related: {
      items: [
        { id: 'heading', reason: 'Headings in hand-built UI.' },
        { id: 'text', reason: 'Body text in hand-built UI.' },
        { id: 'code-block', reason: 'Code with copy and wrap controls inside an article.' },
        {
          id: 'table-of-contents',
          reason: 'In-page navigation built from the article’s headings.',
        },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Wraps rendered content (headings, links, lists, quote, code, table, kbd) with default size and measure, forwarding ref and props; no axe violations.',
        'Renders as `article` with a size and without the measure.',
        'ProseExclude marks content to skip and forwards its ref.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
