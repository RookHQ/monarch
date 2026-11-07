import { CodeBlock, CodeTabs, type CodeSample } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const handler = `export async function handleWebhook(request: Request) {
  const event = await request.json();
  if (event.type === 'invoice.paid') {
    await markInvoicePaid(event.data.id);
  }
  return new Response(null, { status: 204 });
}
`;

const samples: CodeSample[] = [
  {
    label: 'cURL',
    language: 'bash',
    code: `curl https://api.example.com/v1/projects \\
  -H "Authorization: Bearer $API_KEY"`,
  },
  {
    label: 'JavaScript',
    language: 'js',
    code: `const response = await fetch('https://api.example.com/v1/projects', {
  headers: { Authorization: \`Bearer \${process.env.API_KEY}\` },
});
const projects = await response.json();`,
  },
  {
    label: 'Python',
    language: 'python',
    code: `import os, requests

projects = requests.get(
    "https://api.example.com/v1/projects",
    headers={"Authorization": f"Bearer {os.environ['API_KEY']}"},
).json()`,
  },
];

export default {
  id: 'code-block',
  title: 'CodeBlock',
  summary:
    'Code sample with a header, copy, wrap, line numbers and light highlighting; CodeTabs for multi-language samples.',
  sections: {
    purpose: {
      summary:
        'Shows code people will read and copy: API examples, configuration, install commands, code in messages. It reuses AiCodeBlock’s tokenizer, copy and wrap without the AI-only Apply and Insert actions, and adds CodeTabs for the same example in several languages. Needed by: Developers (CodeBlock, CodeTabs), Chat (message code blocks), Dots (marketing snippets).',
      useWhen: [
        'A code sample in documentation, settings or an onboarding step.',
        'Code inside a chat message or comment.',
        'The same request in cURL, JavaScript and Python (`CodeTabs`).',
      ],
      avoidWhen: [
        'A single identifier inline in a sentence: use `<code>` or Kbd for keys.',
        'A secret the person must reveal on purpose: use SecretField.',
        'Code in an AI answer that the product can apply: use AiCodeBlock.',
      ],
    },
    designPrinciples: {
      items: [
        'Copy exactly what is shown: the copy button copies the `code` string as given.',
        'The page never scrolls sideways: long lines scroll inside the block, or wrap with the toggle.',
        'Highlighting is light and needs no extra package; unknown languages render as plain text.',
        'One block, one name: the header text names the scrolling region for screen readers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CodeBlock` renders AiCodeBlock with the `mn-code-block` class; its ref goes to the root div.',
        '`CodeTabs` renders Tabs with one CodeBlock per sample; its ref goes to the wrapper div.',
        'The tokenizer is exported from AiCodeBlock as `tokenizeCode` and `resolveCodeLanguage`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`title` replaces AiCodeBlock’s `filename`; without it the header shows the language name.',
        'Wrap: controlled (`wrap` + `onWrapChange`) or uncontrolled (`defaultWrap`); hide the toggle with `showWrapToggle={false}`.',
        '`CodeSample` is `{ label, code, language?, value?, title? }`.',
        'Migrating from the app copies: Developers `CodeBlock({ code, title, language })` maps one to one; Developers `CodeTabs({ samples, title })` → `CodeTabs samples={…} label={title}` (`samples` keep `label`, `language`, `code`). Chat `CodeBlock({ code, lang })` → `CodeBlock code={code} language={lang ?? undefined}`; the lazy lowlight highlighter is replaced by the built-in tokenizer. Dots `CodeBlock({ code, label, language })` → `title={label}`. The dark "ink" background of the app copies is not reproduced; the block follows the theme.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'diff',
          values: ['false', 'true'],
          description:
            'Diff colouring with + and − markers. Default true for `language="diff"`. Exposed as `data-diff`.',
        },
        {
          prop: 'lineNumbers',
          values: ['false', 'true'],
          description:
            'A line-number gutter, hidden from screen readers and selection. Exposed as `data-line-numbers`.',
        },
        {
          prop: 'highlight',
          values: ['true', 'false'],
          description: 'Syntax colouring for known languages.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Wrapped',
          attribute: 'data-wrap',
          description: 'Long lines wrap instead of scrolling.',
        },
        {
          name: 'Copied',
          description: 'The copy button confirms "Copied" (or "Couldn’t copy") and announces it.',
        },
        {
          name: 'Selected sample',
          attribute: 'data-state="active"',
          description: 'CodeTabs: the visible tab and panel.',
        },
      ],
    },
    interaction: {
      items: [
        'Copy writes the code to the clipboard and confirms in place.',
        'The wrap toggle switches between wrapping and sideways scrolling.',
        'In CodeTabs, choosing a tab shows that sample; `onValueChange` lets the product remember the choice.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the wrap toggle, the copy button and the code region.' },
        {
          keys: 'Arrow Left / Arrow Right',
          action:
            'In the code region: scrolls long lines. In the CodeTabs tab list: switches samples.',
        },
        { keys: 'Enter / Space', action: 'Activates wrap or copy.' },
      ],
    },
    accessibility: {
      role: 'region (the scrolling code), inside a tab panel for CodeTabs',
      name: 'The title, or "<Language> code", or "Code".',
      items: [
        'The code region is focusable so keyboard users can scroll it (WCAG 2.1.1).',
        'The copy button says what it copies and announces the result.',
        'Line numbers and diff markers are `aria-hidden`; diff lines are prefixed with a spoken "Added:" or "Removed:".',
        'CodeTabs uses the tabs pattern; the panel itself is not an extra tab stop because it contains controls.',
      ],
    },
    responsive: {
      items: [
        'The block never exceeds its container; long lines scroll inside it, so 390px screens have no page scroll.',
        'Header actions wrap under the title on narrow screens; the tab list scrolls sideways.',
      ],
    },
    theme: {
      items: [
        'Uses AiCodeBlock’s surface and token colours, which follow all four themes.',
        'Forced colours: inherits AiCodeBlock’s system colours; the tab panel focus ring uses Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A file',
          stack: true,
          render: () => <CodeBlock code={handler} language="ts" title="webhook.ts" lineNumbers />,
        },
        {
          title: 'An install command',
          stack: true,
          render: () => (
            <CodeBlock
              code="npm install @rookcom/monarch-react"
              language="bash"
              showWrapToggle={false}
            />
          ),
        },
        {
          title: 'The same request in three languages',
          stack: true,
          render: () => <CodeTabs samples={samples} label="List projects" />,
        },
        {
          title: 'A diff',
          stack: true,
          render: () => (
            <CodeBlock
              language="diff"
              title="config.yml"
              code={`@@ -1,3 +1,3 @@\n retries: 3\n-timeout: 10\n+timeout: 30\n region: eu`}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give a title when the code belongs to a file or does one named thing.',
        'Use CodeTabs when you show the same example in several languages.',
        'Keep placeholders obvious (`$API_KEY`) so copied code never contains real secrets.',
      ],
      dont: [
        'Do not put real keys or tokens in samples.',
        'Do not use it for one word inside a sentence.',
        'Do not pre-format code with HTML; pass the plain string.',
      ],
    },
    related: {
      items: [
        { id: 'ai-code-block', reason: 'Code in AI answers, with Apply and Insert actions.' },
        { id: 'kbd', reason: 'Keyboard keys inline.' },
        { id: 'copy-field', reason: 'A single value to copy, such as an id or URL.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-code-block', 'tabs'],
    },
    testing: {
      unit: [
        'Titled, focusable, named code region with highlighting, copy and wrap; no Apply or Insert; no axe violations.',
        'Name falls back to the language; copy writes the exact code.',
        'Wrap toggles from the keyboard; line numbers are hidden; diff mode with translated markers.',
        'Ref forwarding; renders on the server.',
        'CodeTabs: named tab list, one block per sample, arrow keys switch and report, Tab goes into the panel’s controls, controlled value, renders on the server; no axe violations.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
