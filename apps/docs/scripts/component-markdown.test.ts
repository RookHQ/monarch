import { describe, expect, it } from 'vitest';
import {
  docComment,
  enrichComponentMarkdown,
  entryFileName,
  exportsOfRoute,
  propertiesMarkdown,
  stylesLine,
} from './component-markdown.mjs';
import { dropEmptyHeadings, mainToMarkdown } from './markdown.mjs';

const button = {
  name: 'Button',
  kind: 'component',
  entry: '.',
  description: 'The primary action.\n\n  <Button>Save</Button>',
  className: 'mn-button',
  css: ['@rookcom/monarch-tokens/css', '@rookcom/monarch-react/styles/core.css'],
  componentCss: '@rookcom/monarch-react/styles/components/button.css',
  docs: 'https://monarchui.dev/components/button/',
  props: [
    { name: 'label', type: 'string', required: true, description: 'Name.' },
    {
      name: 'variant',
      type: 'ButtonVariant',
      required: false,
      description: 'Weight | look.',
      default: "'secondary'",
      values: ['primary', 'secondary'],
    },
  ],
};
const manifest = {
  components: [button, { ...button, name: 'Other', docs: 'https://monarchui.dev/components/x/' }],
};

describe('component Markdown', () => {
  it('finds the exports of a route', () => {
    expect(exportsOfRoute(manifest, '/components/button').map((e) => e.name)).toEqual(['Button']);
  });

  it('writes the Styles line with the exact imports', () => {
    const line = stylesLine([button]);
    expect(line).toContain("`import '@rookcom/monarch-react/styles/core.css';`");
    expect(line).toContain("`import '@rookcom/monarch-react/styles/components/button.css';`");
  });

  it('renders defaults, values and fenced examples', () => {
    const md = propertiesMarkdown([button]);
    expect(md).toContain('| Prop | Type | Default | Values | Description |');
    expect(md).toContain(
      '| `variant` | `ButtonVariant` | `\'secondary\'` | `"primary"`, `"secondary"` | Weight \\| look. |',
    );
    expect(md).toContain('| `label` (required) |');
    expect(md).toContain('```tsx\n<Button>Save</Button>\n```');
    expect(md).toContain('Root class: `.mn-button`');
  });

  it('keeps plain doc comments as they are', () => {
    expect(docComment('One line.')).toBe('One line.');
  });

  it('replaces the properties section and adds the Styles line after the import', () => {
    const page = [
      '# Button',
      '',
      '## API reference',
      '',
      "```tsx\nimport { Button } from '@rookcom/monarch-react';\n```",
      '',
      '## Properties',
      '',
      'old table',
      '',
      '## Variants',
      '',
      'kept',
      '',
    ].join('\n');
    const out = enrichComponentMarkdown(page, [button]);
    expect(out).not.toContain('old table');
    expect(out).toContain('## Variants\n\nkept');
    expect(out.indexOf('**Styles:**')).toBeGreaterThan(
      out.indexOf("from '@rookcom/monarch-react'"),
    );
    expect(out.indexOf('**Styles:**')).toBeLessThan(out.indexOf('## Properties'));
  });

  it('names per-entry llms files', () => {
    expect(entryFileName('.')).toBe('core');
    expect(entryFileName('./ai')).toBe('ai');
  });
});

describe('HTML to Markdown', () => {
  it('drops headings with nothing under them', () => {
    expect(
      dropEmptyHeadings(['# T', '## Variants', '## Usage', '### A', '### B', '## Purpose', 'x']),
    ).toEqual(['# T', '## Purpose', 'x']);
  });

  it('keeps a definition list together', () => {
    const md = mainToMarkdown(
      '<main id="main"><dl><div><dt>Role</dt><dd>button</dd></div><div><dt>Accessible name</dt><dd>The <code>label</code></dd></div></dl></main>',
      'https://monarchui.dev/',
    );
    expect(md).toBe('- **Role**: button\n- **Accessible name**: The `label`\n');
  });

  it('keeps every tab panel and leaves out the tabs', () => {
    const md = mainToMarkdown(
      '<main id="main"><div class="docs-native-slot"><div role="tablist"><button role="tab">Web</button><button role="tab">iOS</button></div>' +
        '<div role="tabpanel" id="panel-ios" hidden><h2>iOS</h2><p>On iOS.</p></div></div>' +
        '<div role="tabpanel" id="panel-web"><h2>Examples</h2><p>On the web.</p></div>' +
        '<div hidden><p>Not content.</p></div></main>',
      'https://monarchui.dev/',
    );
    // The web panel first, as the page shows it, then the platform panels.
    expect(md).toBe('## Examples\n\nOn the web.\n\n## iOS\n\nOn iOS.\n');
  });
});
