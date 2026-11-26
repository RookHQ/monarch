import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import {
  DEFAULT_EMBED_PROVIDERS,
  EmbedBlock,
  EmbedBlockExtension,
  resolveEmbed,
  type EmbedProvider,
} from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

let current: Editor | null = null;

function Harness({ content, editable = true }: { content: string; editable?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    editable,
    extensions: [KIT, EmbedBlockExtension],
    content,
    editorProps: { attributes: { 'aria-label': 'Document', role: 'textbox' } },
  });
  current = editor;
  return <EditorContent editor={editor} />;
}

const ready = async () => {
  await waitFor(() => expect(current).not.toBeNull());
  await screen.findByRole('textbox', { name: 'Document' });
  return current as Editor;
};

describe('resolveEmbed', () => {
  it('maps allow-listed share URLs to the providers’ embed addresses', () => {
    expect(resolveEmbed('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toMatchObject({
      kind: 'frame',
      src: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
    });
    expect(resolveEmbed('https://youtu.be/dQw4w9WgXcQ')).toMatchObject({ kind: 'frame' });
    expect(resolveEmbed('https://vimeo.com/76979871')).toMatchObject({
      src: 'https://player.vimeo.com/video/76979871',
    });
    expect(resolveEmbed('https://www.loom.com/share/0123456789abcdef0123')).toMatchObject({
      src: 'https://www.loom.com/embed/0123456789abcdef0123',
    });
    expect(resolveEmbed('https://codepen.io/team/pen/abcDEF')).toMatchObject({
      src: 'https://codepen.io/team/embed/abcDEF?default-tab=result',
    });
    expect(resolveEmbed('https://www.figma.com/design/key/Name')).toMatchObject({
      kind: 'frame',
      provider: { id: 'figma' },
    });
  });

  it('never frames lookalike hosts, plain http, ports, credentials or unknown sites', () => {
    for (const url of [
      'https://youtube.com.example.net/watch?v=dQw4w9WgXcQ',
      'https://evil-youtube.com/watch?v=dQw4w9WgXcQ',
      'http://www.youtube.com/watch?v=dQw4w9WgXcQ',
      'https://www.youtube.com:8443/watch?v=dQw4w9WgXcQ',
      'https://example.com/page',
    ])
      expect(resolveEmbed(url).kind).toBe('link');
    expect(resolveEmbed('https://user:pw@www.youtube.com/watch?v=dQw4w9WgXcQ').kind).toBe(
      'invalid',
    );
    // A bad id gives a link, not a frame with attacker-controlled path.
    expect(resolveEmbed('https://www.youtube.com/watch?v=../../x').kind).toBe('link');
  });

  it('rejects unsafe and malformed URLs', () => {
    for (const url of [
      'javascript:alert(1)',
      'data:text/html,<p>x</p>',
      '/relative',
      'not a url',
      '',
    ])
      expect(resolveEmbed(url).kind).toBe('invalid');
  });

  it('uses a product’s own provider list', () => {
    const only: EmbedProvider[] = [
      {
        id: 'maps',
        name: 'Maps',
        hosts: ['maps.example.com'],
        toEmbedSrc: (u) => `https://maps.example.com/embed${u.pathname}`,
      },
    ];
    expect(resolveEmbed('https://maps.example.com/p/1', only)).toMatchObject({ kind: 'frame' });
    expect(resolveEmbed('https://youtu.be/dQw4w9WgXcQ', only).kind).toBe('link');
    expect(DEFAULT_EMBED_PROVIDERS.length).toBeGreaterThan(3);
  });
});

describe('EmbedBlock', () => {
  it('renders a sandboxed, titled, lazy frame for an allow-listed URL', async () => {
    render(<EmbedBlock url="https://youtu.be/dQw4w9WgXcQ" caption="Product tour" />);
    const frame = screen.getByTitle('Product tour');
    expect(frame.tagName).toBe('IFRAME');
    expect(frame).toHaveAttribute(
      'sandbox',
      'allow-scripts allow-same-origin allow-presentation allow-popups',
    );
    expect(frame).toHaveAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
    expect(frame).toHaveAttribute('loading', 'lazy');
    // Fullscreen through `allow` only: with `allowfullscreen` as well, browsers warn.
    expect(frame.getAttribute('allow')).toMatch(/\bfullscreen\b/);
    expect(frame).not.toHaveAttribute('allowfullscreen');
    expect(screen.getByText('Product tour').tagName).toBe('FIGCAPTION');
    // axe-core cannot run inside jsdom frames; the frame's title is asserted above.
  });

  it('renders a link card for other sites and a message for unsafe URLs', async () => {
    const { rerender } = render(<EmbedBlock url="https://example.com/article" />);
    const link = screen.getByRole('link', { name: /example\.com/ });
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(link).toHaveAttribute('target', '_blank');
    expect(document.querySelector('iframe')).toBeNull();
    await expectNoA11yViolations();
    rerender(<EmbedBlock url="javascript:alert(1)" />);
    expect(screen.getByText('This link can’t be shown.')).toBeInTheDocument();
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<EmbedBlock url="https://vimeo.com/1" />);
    expect(html).toContain('player.vimeo.com/video/1');
    expect(html).toContain('sandbox=');
  });
});

describe('EmbedBlockExtension', () => {
  it('asks for a URL in a labelled field, refuses unsafe input and embeds a valid one', async () => {
    const user = userEvent.setup();
    render(<Harness content="<p>Intro</p>" />);
    const editor = await ready();
    act(() => {
      editor.commands.insertEmbed();
    });
    const field = await screen.findByLabelText('Link to embed');
    await expectNoA11yViolations();
    await user.type(field, 'javascript:alert(1)');
    await user.click(screen.getByRole('button', { name: 'Embed' }));
    expect(
      await screen.findByText('Enter a web address that starts with https://.'),
    ).toBeInTheDocument();
    await user.clear(field);
    await user.type(field, 'https://youtu.be/dQw4w9WgXcQ{Enter}');
    await waitFor(() => expect(document.querySelector('iframe')).not.toBeNull());
    expect(editor.getHTML()).toContain(
      '<div data-url="https://youtu.be/dQw4w9WgXcQ" data-type="embed"><a href="https://youtu.be/dQw4w9WgXcQ"',
    );
    expect(editor.getHTML()).not.toContain('iframe');
    await user.type(screen.getByLabelText('Caption'), 'Demo');
    expect(editor.getJSON().content?.find((n) => n.type === 'embed')?.attrs).toEqual({
      url: 'https://youtu.be/dQw4w9WgXcQ',
      caption: 'Demo',
    });
  });

  it('drops unsafe stored URLs and never parses iframes', async () => {
    render(
      <Harness
        editable={false}
        content='<div data-type="embed" data-url="javascript:alert(1)"></div><iframe src="https://evil.example"></iframe>'
      />,
    );
    const editor = await ready().catch(() => current as Editor);
    expect(editor.getJSON().content?.[0]).toMatchObject({ type: 'embed', attrs: { url: '' } });
    expect(editor.getHTML()).not.toContain('evil');
  });
});
