import { AiMarkdown } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ANSWER = `# Release checklist

Here is what is left before the **Friday** release, based on the open tasks[^1].

1. Finish the migration review
   - [x] Schema changes approved
   - [ ] Backfill tested on a copy
2. Update the *changelog* and the ~~old~~ new screenshots
3. Tell support (see [the handover notes](https://example.com/notes))

> Releases after 16:00 need a second reviewer.

| Task | Owner | Days left |
| :-- | :-- | --: |
| Migration review | Sam | 2 |
| Changelog | Alex | 1 |

\`\`\`bash
npm run release -- --dry-run
\`\`\`

[^1]: Tasks tagged \`release\` in the sample project.`;

const UNTRUSTED = `Links are checked: [safe](https://example.com), [not safe](javascript:alert(1)).

Raw HTML stays text: <img src=x onerror=alert(1)> <b>not bold</b>

Images need allowImages: ![Quarterly chart](https://example.com/chart.png)`;

// A module-level renderer keeps completed blocks memoised while streaming.
function renderCode(lang: string, code: string) {
  if (lang !== 'math') return undefined;
  return (
    <figure style={{ margin: 0, fontFamily: 'var(--mn-font-mono)' }}>
      <figcaption style={{ fontSize: 'var(--mn-text-xs)' }}>
        Formula (plug in a maths renderer here)
      </figcaption>
      {code}
    </figure>
  );
}

export default {
  id: 'ai-markdown',
  title: 'AiMarkdown',
  summary:
    'Safe GitHub-flavoured Markdown for model output, rendered as React elements: tables, task lists, footnotes and code blocks.',
  sections: {
    purpose: {
      summary:
        'Turns Markdown written by a model into React elements without ever building an HTML string. It covers the GitHub-flavoured constructs answers use (tables, task lists, strikethrough, footnotes, autolinks) and treats the source as untrusted. Needed by: AiStreamingText, AiMessageView.',
      useWhen: [
        'Showing an AI answer, summary or tool result written in Markdown.',
        'Any untrusted Markdown from outside the product (imports, integrations).',
        'You need the block tokens (`parseMarkdownBlocks`) to build your own renderer or memoise blocks.',
      ],
      avoidWhen: [
        'The text is still arriving: use AiStreamingText, which memoises finished blocks and announces sentences.',
        'People write or edit the content: use RichTextEditor.',
        'Plain text with no formatting: render it as text.',
      ],
    },
    designPrinciples: {
      items: [
        'Model output is untrusted: no `dangerouslySetInnerHTML`, raw HTML shows as text, links must pass `isSafeUrl` and get `rel="noopener noreferrer nofollow"`.',
        'Images are off unless `allowImages`: an image becomes its alt text and no request is made to its URL.',
        'Answers never break the page outline: `#` renders as `<h3>` by default (`headingLevelOffset`).',
        'No bundled heavy features: diagrams or maths plug in through `renderCode`, loaded only by products that need them.',
        'Dependency-free and fast: blocks are memoised, so a streaming answer only re-renders its last block.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'AiMarkdown forwards its ref to the root `<div>`.',
        '`parseMarkdownBlocks(text)` returns top-level block tokens (`heading`, `paragraph`, `code`, `blockquote`, `list`, `table`, `thematicBreak`, `footnoteDefinition`), each with its `raw` source.',
        '`parseMarkdownIncremental(text, previous)` re-parses only from the last block when the text grew, keeping earlier block objects, so they can be memoised by identity.',
        '`parseInline(text)`, `inlineText(nodes)` and `blockText(block, options)` give inline tokens and plain text (used for announcements).',
        'Not supported (shown as text): link reference definitions and raw HTML blocks.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pass either `text` or pre-parsed `blocks`.',
        'Keep `renderCode`, `codeBlockProps` and `messages` stable (module level, `useCallback`, `useMemo`) so finished blocks are not re-rendered on each update.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevelOffset',
          values: ['0', '1', '2', '3'],
          description: 'Added to every heading level. Default 2 (`#` → h3). Levels stop at h6.',
        },
        {
          prop: 'allowImages',
          values: ['true', 'false'],
          description: 'Render images with alt text and a safe `http(s)` source. Default false.',
        },
        {
          prop: 'externalLinksInNewTab',
          values: ['true', 'false'],
          description:
            'Open links to other sites in a new tab, announced as "(opens in a new tab)". Default false.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          description:
            '`streaming` marks the last block as unfinished: an unclosed code block keeps Apply / Insert disabled (`data-incomplete` on the code block).',
        },
        {
          name: 'Task item',
          attribute: 'data-checked',
          description: 'List items from `- [x]` / `- [ ]`, with a read-only checkbox.',
        },
        {
          name: 'Task list',
          attribute: 'data-tasks',
          description: 'A list that contains task items (no bullets).',
        },
      ],
    },
    interaction: {
      items: [
        'Links behave as normal links; unsafe ones are plain text.',
        'Footnote references jump to the footnotes list, and each footnote links back to its first reference.',
        'Wide tables scroll inside a focusable region instead of widening the page.',
        'Code blocks have the AiCodeBlock actions (copy, wrap, and Apply / Insert through `codeBlockProps`).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through links, footnote links, table regions and code block controls in reading order.',
        },
        { keys: 'Arrow keys', action: 'Scroll a focused table or code region.' },
        { keys: 'Enter', action: 'Follows the focused link.' },
      ],
    },
    accessibility: {
      role: 'None on the root; headings, lists, tables, blockquotes and links keep their native roles.',
      name: 'The root has none; give the message that contains it its name (AiMessageView does).',
      items: [
        'Table scroll regions are labelled "Table" (`messages.table`) and focusable; header cells use `scope="col"`.',
        'Task checkboxes are disabled, read-only and named "Task"; their checked state is announced.',
        'Footnote references are named "Footnote 1"; back links "Back to reference 1". The footnotes list is named "Footnotes".',
        'Links that open a new tab say so in visually hidden text.',
        'Images, when allowed, always have their alt text; an image without alt text is not shown.',
      ],
    },
    responsive: {
      items: [
        'Tables and code scroll inside their own regions, so the answer never scrolls the page sideways at 390px.',
        'Long words and URLs wrap (`overflow-wrap`).',
        'Footnote links get a wider hit area on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Text, links (`--mn-color-text-link`), inline code (`--mn-color-bg-muted`) and table borders come from tokens, so every theme and contrast mode applies.',
        'Logical properties throughout: lists, quotes and tables mirror in right-to-left layouts.',
        'Forced colours: links use LinkText and borders CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'A full answer', stack: true, render: () => <AiMarkdown text={ANSWER} /> },
        { title: 'Untrusted input', stack: true, render: () => <AiMarkdown text={UNTRUSTED} /> },
        {
          title: 'Plugging in a renderer with renderCode',
          stack: true,
          render: () => (
            <AiMarkdown
              text={'The area of a circle:\n\n```math\nA = π r²\n```'}
              renderCode={renderCode}
            />
          ),
        },
        {
          title: 'Heading levels',
          stack: true,
          render: () => (
            <AiMarkdown
              text={'# Starts at h2 here\n\nWith `headingLevelOffset={1}`.'}
              headingLevelOffset={1}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use AiStreamingText while an answer streams, then keep rendering through it (or AiMarkdown) when done.',
        'Load diagram or maths libraries lazily inside `renderCode`, only for the languages you handle.',
        'Set `headingLevelOffset` to match where the answer sits in your page outline.',
      ],
      dont: [
        'Do not convert model output to HTML and inject it.',
        'Do not turn on `allowImages` for sources you do not trust to load images from.',
        'Do not pass a new `renderCode` function on every render while streaming.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-streaming-text',
          reason: 'Renders Markdown that is still arriving, with a caret and announcements.',
        },
        { id: 'ai-code-block', reason: 'The code block AiMarkdown uses for fenced code.' },
        {
          id: 'ai-message',
          reason: 'The message frame that identifies the answer as AI-generated.',
        },
        { id: 'rich-text-editor', reason: 'For content people write and edit.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-code-block'],
      notes: ['Uses `isSafeUrl` / `isSafeImageSrc` from the shared sanitiser.'],
    },
    testing: {
      unit: [
        'Parses every block construct, nested / ordered / loose / task lists and an unclosed fence.',
        'Parses emphasis, strong, strikethrough, code, links, autolinks, escapes, entities and hard breaks; intraword `_` and spaced `*` stay text.',
        'Incremental parsing keeps completed block objects.',
        'Headings start at h3 and follow `headingLevelOffset`.',
        'Safe links get rel; `javascript:` and `data:` links lose their href; new-tab links say so.',
        'Raw HTML is escaped (DOM and server output).',
        'Images only with `allowImages`, alt text and a safe source.',
        'Tables have alignment and a labelled focusable scroll region.',
        'Fenced code renders through AiCodeBlock or `renderCode`.',
        'Footnotes are numbered by first reference, with back links; undefined references stay text.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
