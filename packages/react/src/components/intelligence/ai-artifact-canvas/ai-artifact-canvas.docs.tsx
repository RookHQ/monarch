import { useState } from 'react';
import {
  AiArtifactCanvas,
  AiArtifactCard,
  type AiArtifactPart,
  type AiCanvasArtifact,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DOC_VERSIONS: Record<number, string> = {
  1: '# Q3 summary\n\nRevenue grew in every region.\n\n- New customers: 140\n- Churn: 2.1%',
  2: '# Q3 summary\n\nRevenue grew **12%**, led by the north region.\n\n- New customers: 140\n- Churn: 2.1%\n\n## Next steps\n\nReview pricing before the planning meeting.',
};

const PAGE: AiCanvasArtifact = {
  id: 'page',
  kind: 'html',
  title: 'Event landing page',
  version: 1,
  content:
    '<main style="font-family: system-ui; padding: 1rem"><h1>Team offsite</h1><p>Two days of planning and workshops.</p><button>Register</button><script>document.body.textContent = "Scripts never run here."</script></main>',
};

const CODE: AiCanvasArtifact = {
  id: 'code',
  kind: 'code',
  title: 'parse-invoice.ts',
  version: 1,
  language: 'ts',
  content:
    'export function parseTotal(text: string): number {\n  const match = /Total:\\s*([\\d.]+)/.exec(text);\n  return match ? Number(match[1]) : 0;\n}',
};

function SplitDemo() {
  const [open, setOpen] = useState(false);
  const [version, setVersion] = useState(2);
  const artifact: AiCanvasArtifact = {
    id: 'q3',
    kind: 'document',
    title: 'Q3 summary',
    version,
    content: DOC_VERSIONS[version] ?? '',
  };
  const part: AiArtifactPart = {
    type: 'artifact',
    id: 'q3',
    kind: 'document',
    title: 'Q3 summary',
    version: 2,
  };
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-4)',
        gridTemplateColumns: 'repeat(auto-fit, minmax(16rem, 1fr))',
        alignItems: 'start',
      }}
    >
      <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
        <p style={{ margin: 0 }}>Here is the summary you asked for.</p>
        <AiArtifactCard part={part} active={open} onOpen={() => setOpen(true)} />
      </div>
      <AiArtifactCanvas
        artifact={artifact}
        versions={[
          { version: 1, createdAt: '2026-03-12T09:00:00Z' },
          { version: 2, createdAt: '2026-03-12T09:20:00Z', label: 'Added next steps' },
        ]}
        onVersionChange={setVersion}
        open={open}
        onOpenChange={setOpen}
        onDownload={() => {}}
        onSave={() => new Promise((resolve) => setTimeout(resolve, 800))}
        saveLabel="Save to library"
      />
    </div>
  );
}

export default {
  id: 'ai-artifact-canvas',
  title: 'AiArtifactCanvas',
  summary:
    'A pane (or phone sheet) for an AI artifact: Preview / Code tabs, versions, copy, download and save.',
  sections: {
    purpose: {
      summary:
        'Long AI output (a document, code, a table, a web page) is easier to read and reuse beside the conversation than inside it. The canvas shows one artifact with its versions and the actions to take it elsewhere, and previews untrusted HTML safely.',
      useWhen: [
        'A reply produced an artifact and the person opened its AiArtifactCard.',
        'People need to copy, download or save AI output into the product.',
        'Previewing AI-made HTML: it only ever runs in a sandboxed frame.',
      ],
      avoidWhen: [
        'The output is short: show it in the message.',
        'Editing a real document with AI suggestions: use the editor with AiInlineDiff.',
      ],
    },
    designPrinciples: {
      items: [
        'Model output is untrusted: HTML previews only in an `<iframe sandbox="">` with `srcdoc` and a Content Security Policy that blocks all network requests. It is never injected into the page.',
        'Preview first, code one tab away, so people can check exactly what they are saving.',
        'The save action names its destination ("Save to library"), set by the app.',
        'Beside the thread on wide screens, full screen on phones.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native attributes pass through to the root (`<section>` inline, the sheet content otherwise) and the ref is forwarded to it.',
        'Built-in previews: `document` and `table` render Markdown through AiMarkdown (images off unless `allowImages`); `html` renders in the sandboxed frame; `image` shows `content` as an image URL only when `isSafeImageSrc` allows it; `code` and `chart` have no built-in preview, so only the Code tab shows unless `renderPreview` returns something.',
        '`aiArtifactSrcDoc(html)` returns the document the frame loads (CSP first, then the content).',
        'Copy uses the clipboard and then calls `onCopy`; Download and Save appear only when their callbacks are given. `onSave` may return a promise.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['auto', 'inline', 'sheet'],
          description:
            '`auto` is a pane, and a full-screen sheet below 640px. `inline` is always a pane; `sheet` is always a modal sheet. Exposed as `data-placement`.',
        },
        {
          prop: 'artifact.kind',
          values: ['document', 'code', 'table', 'chart', 'image', 'html'],
          description: 'Chooses the preview and the default code language. Exposed as `data-kind`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Closed', description: 'Renders nothing (inline) or a closed sheet.' },
        {
          name: 'Preview',
          attribute: 'aria-selected on the tab',
          description: 'The rendered artifact.',
        },
        {
          name: 'Code',
          description:
            'The source in an AiCodeBlock with line numbers; the only tab when there is no preview.',
        },
        {
          name: 'Saving',
          attribute: 'aria-busy on the save button',
          description: 'While `onSave` is pending.',
        },
        {
          name: 'Unsafe image',
          attribute: 'data-tone="warning"',
          description: 'The image URL is refused and a note is shown instead.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening the inline pane moves focus to it; closing returns focus to where it was (usually the card).',
        'Escape anywhere in the canvas closes it, unless a menu or select inside it is open.',
        'Changing a version calls `onVersionChange`; the app loads that version and passes it as `artifact`.',
        'The sheet traps focus while open, like every Monarch sheet.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through close, versions, actions, the tabs and the content.',
        },
        { keys: 'Arrow Left / Right', action: 'Moves between Preview and Code.' },
        { keys: 'Escape', action: 'Closes the canvas.' },
      ],
    },
    accessibility: {
      role: 'region (inline) or dialog (sheet)',
      name: '"Artifact: <title>" inline; the title heading in the sheet',
      items: [
        'The title is a level-2 heading; Markdown headings inside start at level 3.',
        'The HTML frame has a title ("Preview of <title>") and a visible note that scripts, forms and network are blocked.',
        'Copy, Download and Save are text buttons; Copy announces the result.',
      ],
    },
    responsive: {
      items: [
        'With `placement="auto"` it becomes a full-screen sheet below 640px.',
        'The toolbar wraps; the frame is 60% of the screen height on phones; code scrolls inside its block, so the page never scrolls sideways at 390px.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text tokens; the kind tile uses `--mn-color-ai-subtle` and `--mn-color-ai-strong`.',
        'The frame sits on `--mn-color-bg-surface`; the sandboxed page itself has no access to Monarch styles.',
        'In forced colours the pane and frame have CanvasText borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Beside a reply, with versions and save',
          stack: true,
          render: () => <SplitDemo />,
        },
        {
          title: 'Sandboxed HTML preview',
          stack: true,
          render: () => (
            <AiArtifactCanvas artifact={PAGE} open onOpenChange={() => {}} placement="inline" />
          ),
        },
        {
          title: 'Code',
          stack: true,
          render: () => (
            <AiArtifactCanvas artifact={CODE} open onOpenChange={() => {}} placement="inline" />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the save destination with `saveLabel`.',
        'Keep every version and pass them as `versions`.',
        'Use `renderPreview` for charts and rich tables, built from data, never from HTML.',
      ],
      dont: [
        'Do not render artifact HTML with `dangerouslySetInnerHTML` or a `renderPreview` that does.',
        'Do not add `allow-scripts` or `allow-same-origin` to the frame.',
        'Do not save automatically; saving is the person’s choice.',
      ],
    },
    related: {
      items: [
        { id: 'ai-artifact-card', reason: 'The card in the reply that opens the canvas.' },
        { id: 'ai-artifact-versions', reason: 'The version stepper and diff.' },
        { id: 'ai-code-block', reason: 'The Code tab.' },
        { id: 'ai-markdown', reason: 'The document preview.' },
        { id: 'sheet', reason: 'The phone layout.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'ai-artifact-card',
        'ai-artifact-versions',
        'ai-code-block',
        'ai-markdown',
        'button',
        'copy-button',
        'sheet',
        'tabs',
      ],
    },
    testing: {
      unit: [
        'Named region with heading, kind, version, Preview / Code tabs and a Markdown preview.',
        'HTML previews only in an iframe whose `sandbox` attribute is empty, with `srcdoc` and no `src`; hostile HTML never reaches the page (no script, img or injected id; nothing runs).',
        'The srcdoc starts with a CSP that blocks network requests; the Code tab shows HTML as text.',
        'Unsafe image URLs are refused; `renderPreview` replaces the preview and falls back when it returns undefined.',
        'Opening focuses the pane; Escape and the close button close it and return focus.',
        'Copy writes the content and calls `onCopy`; Download; Save shows progress until its promise settles.',
        'Version stepping; sheet placement is a named dialog that keeps the sandbox and closes on Escape.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
