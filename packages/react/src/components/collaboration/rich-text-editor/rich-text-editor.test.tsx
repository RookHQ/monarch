import { createRef, useState } from 'react';
import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import {
  RichTextEditor,
  type RichTextEditorHandle,
  type RichTextMentionItem,
  type RichTextValue,
} from './rich-text-editor.js';

/** Tiptap focuses in an animation frame; let it settle before moving focus. */
const frame = () => act(() => new Promise<void>((r) => requestAnimationFrame(() => r())));

const ready = async () => {
  const box = await screen.findByRole('textbox');
  return box;
};

function setup(ui: React.ReactElement) {
  const user = userEvent.setup();
  const utils = render(ui);
  return { user, ...utils };
}

describe('RichTextEditor', () => {
  it('renders a labelled multi-line textbox and an APG toolbar', async () => {
    setup(<RichTextEditor aria-label="Reply" placeholder="Write a reply" />);
    const box = await ready();
    expect(box).toHaveAttribute('aria-multiline', 'true');
    expect(box).toHaveAccessibleName('Reply');
    const toolbar = screen.getByRole('toolbar', { name: 'Formatting' });
    const buttons = Array.from(toolbar.querySelectorAll('button'));
    expect(buttons.map((b) => b.getAttribute('aria-label'))).toEqual([
      expect.stringMatching(/^Bold/),
      expect.stringMatching(/^Italic/),
      'Strikethrough',
      'Inline code',
      'Heading 2',
      'Heading 3',
      'Bulleted list',
      'Numbered list',
      'Checklist',
      expect.stringMatching(/^Link/),
      'Quote',
      'Code block',
    ]);
    // One Tab stop.
    expect(buttons.filter((b) => b.tabIndex === 0)).toHaveLength(1);
    expect(buttons.every((b) => b.hasAttribute('aria-pressed'))).toBe(true);
  });

  it('moves through the toolbar with arrow keys, Home and End (roving tabindex)', async () => {
    const { user } = setup(<RichTextEditor aria-label="Notes" />);
    await ready();
    const bold = screen.getByRole('button', { name: /^Bold/ });
    bold.focus();
    await user.keyboard('{ArrowRight}');
    const italic = screen.getByRole('button', { name: /^Italic/ });
    expect(italic).toHaveFocus();
    expect(italic.tabIndex).toBe(0);
    expect(bold.tabIndex).toBe(-1);
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Code block' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(bold).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Code block' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(bold).toHaveFocus();
  });

  it('toggles marks from the toolbar and reflects them in aria-pressed', async () => {
    const ref = createRef<RichTextEditorHandle>();
    const onValueChange = vi.fn();
    const { user } = setup(
      <RichTextEditor aria-label="Notes" editorRef={ref} onValueChange={onValueChange} />,
    );
    await ready();
    act(() => ref.current?.insertText('Hello'));
    act(() => ref.current?.editor?.commands.selectAll());
    const bold = screen.getByRole('button', { name: /^Bold/ });
    expect(bold).toHaveAttribute('aria-pressed', 'false');
    await user.click(bold);
    expect(bold).toHaveAttribute('aria-pressed', 'true');
    expect(onValueChange).toHaveBeenLastCalledWith(
      '<p><strong>Hello</strong></p>',
      expect.objectContaining({ text: 'Hello', isEmpty: false, characters: 5 }),
    );
    await user.click(screen.getByRole('button', { name: 'Heading 2' }));
    expect(ref.current?.getHTML()).toBe('<h2><strong>Hello</strong></h2>');
    expect(screen.getByRole('button', { name: 'Heading 2' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('applies Markdown shortcuts and keyboard shortcuts while typing', async () => {
    const ref = createRef<RichTextEditorHandle>();
    const { user } = setup(<RichTextEditor aria-label="Notes" editorRef={ref} />);
    const box = await ready();
    await user.click(box);
    await user.keyboard('## Title');
    expect(ref.current?.getHTML()).toBe('<h2>Title</h2>');
    act(() => ref.current?.clear());
    await user.keyboard('- item');
    expect(ref.current?.getHTML()).toBe('<ul><li><p>item</p></li></ul>');
    act(() => ref.current?.clear());
    await user.keyboard('{Control>}b{/Control}bold');
    expect(ref.current?.getHTML()).toBe('<p><strong>bold</strong></p>');
  });

  it('works uncontrolled from defaultValue and controlled from value', async () => {
    function Controlled() {
      const [value, setValue] = useState<RichTextValue>('<p>Start</p>');
      return (
        <>
          <RichTextEditor aria-label="Body" value={value} onValueChange={setValue} />
          <button type="button" onClick={() => setValue('<p>Reset by the product</p>')}>
            reset
          </button>
          <output data-testid="out">{String(value)}</output>
        </>
      );
    }
    const { user } = setup(<Controlled />);
    const box = await ready();
    expect(box).toHaveTextContent('Start');
    await user.click(screen.getByRole('button', { name: 'reset' }));
    await waitFor(() => expect(box).toHaveTextContent('Reset by the product'));
    expect(screen.getByTestId('out')).toHaveTextContent('<p>Reset by the product</p>');
  });

  it('emits JSON when format is json and accepts JSON values', async () => {
    const onValueChange = vi.fn();
    const ref = createRef<RichTextEditorHandle>();
    setup(
      <RichTextEditor
        aria-label="Body"
        format="json"
        editorRef={ref}
        defaultValue={{
          type: 'doc',
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'From JSON' }] }],
        }}
        onValueChange={onValueChange}
      />,
    );
    const box = await ready();
    expect(box).toHaveTextContent('From JSON');
    act(() => ref.current?.insertText('!'));
    const [json] = onValueChange.mock.lastCall ?? [];
    expect(json).toMatchObject({ type: 'doc', content: [{ type: 'paragraph' }] });
  });

  it('emits an empty string for an empty HTML document', async () => {
    const onValueChange = vi.fn();
    const ref = createRef<RichTextEditorHandle>();
    setup(
      <RichTextEditor
        aria-label="Body"
        editorRef={ref}
        defaultValue="<p>x</p>"
        onValueChange={onValueChange}
      />,
    );
    await ready();
    act(() => ref.current?.clear());
    expect(onValueChange).toHaveBeenLastCalledWith('', expect.objectContaining({ isEmpty: true }));
    expect(ref.current?.isEmpty()).toBe(true);
  });

  it('read-only shows the document without a toolbar or textbox', async () => {
    setup(
      <RichTextEditor aria-label="Message" readOnly defaultValue="<p>Shipped <em>today</em></p>" />,
    );
    const group = await screen.findByRole('group', { name: 'Message' });
    await waitFor(() => expect(group).toHaveTextContent('Shipped today'));
    expect(screen.queryByRole('toolbar')).toBeNull();
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(group.querySelector('[contenteditable="false"]')).not.toBeNull();
  });

  it('disabled keeps the document but makes it inert', async () => {
    const { user } = setup(
      <RichTextEditor aria-label="Body" disabled defaultValue="<p>Locked</p>" />,
    );
    const box = await ready();
    expect(box).toHaveAttribute('aria-disabled', 'true');
    expect(box).toHaveAttribute('contenteditable', 'false');
    const bold = screen.getByRole('button', { name: /^Bold/ });
    expect(bold).toHaveAttribute('aria-disabled', 'true');
    await user.click(bold);
    expect(box.querySelector('strong')).toBeNull();
  });

  it('shows the placeholder and marks invalid', async () => {
    setup(<RichTextEditor aria-label="Body" placeholder="Say something" invalid />);
    const box = await ready();
    expect(box).toHaveAttribute('aria-invalid', 'true');
    expect(box.querySelector('p')).toHaveAttribute('data-placeholder', 'Say something');
  });

  it('counts characters, enforces maxLength and links the counter', async () => {
    const ref = createRef<RichTextEditorHandle>();
    setup(<RichTextEditor aria-label="Body" maxLength={12} editorRef={ref} />);
    const box = await ready();
    const counter = screen.getByText('0 / 12');
    expect(box.getAttribute('aria-describedby')).toContain(counter.id);
    act(() => ref.current?.insertText('Hello world'));
    expect(screen.getByText('11 / 12')).toHaveAttribute('data-state', 'near');
    expect(screen.getByRole('status')).toHaveTextContent('1 character left');
    // An insertion that would pass the limit is refused as a whole.
    act(() => ref.current?.insertText('!!!'));
    expect(ref.current?.getText()).toBe('Hello world');
    act(() => ref.current?.insertText('!'));
    expect(screen.getByText('12 / 12')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('0 characters left');
  });

  it('opens a link popover with Mod+K, validates and applies the link', async () => {
    const ref = createRef<RichTextEditorHandle>();
    const { user } = setup(<RichTextEditor aria-label="Body" editorRef={ref} />);
    const box = await ready();
    act(() => ref.current?.insertText('docs'));
    act(() => ref.current?.editor?.commands.selectAll());
    await frame();
    box.focus();
    await user.keyboard('{Control>}k{/Control}');
    const input = await screen.findByLabelText('Link address');
    await waitFor(() => expect(input).toHaveFocus());
    await user.type(input, 'javascript:alert(1)');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText(/Enter a web address/)).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, 'example.com/guide{Enter}');
    await waitFor(() => expect(screen.queryByLabelText('Link address')).toBeNull());
    expect(ref.current?.getHTML()).toContain('href="https://example.com/guide"');
    expect(ref.current?.getHTML()).toContain('rel="noopener noreferrer nofollow"');
  });

  it('drops unsafe links and clipboard junk from pasted HTML', async () => {
    const ref = createRef<RichTextEditorHandle>();
    setup(<RichTextEditor aria-label="Body" editorRef={ref} />);
    await ready();
    const view = ref.current!.editor!.view;
    act(() => {
      view.pasteHTML(
        '<style>p{color:red}</style><!--[if gte mso 9]><xml>x</xml><![endif]--><p onclick="steal()">Hi <a href="javascript:alert(1)">bad</a> <a href="https://example.com">good</a><script>alert(1)</script></p><img src="javascript:alert(1)">',
      );
    });
    const out = ref.current!.getHTML();
    expect(out).not.toMatch(/script|onclick|javascript|style|mso/i);
    expect(out).toContain('href="https://example.com"');
    expect(out).toContain('bad');
  });

  it('hands pasted, dropped and picked files to onFiles and inserts what it returns', async () => {
    const ref = createRef<RichTextEditorHandle>();
    const onFiles = vi.fn(async (files: File[]) =>
      files.map((f) =>
        f.type.startsWith('image/')
          ? ({ type: 'image', src: `https://cdn.example.com/${f.name}`, alt: f.name } as const)
          : ({ type: 'link', href: `https://cdn.example.com/${f.name}`, text: f.name } as const),
      ),
    );
    const { user, container } = setup(
      <RichTextEditor
        aria-label="Body"
        editorRef={ref}
        onFiles={onFiles}
        fileAccept="image/*,.pdf"
      />,
    );
    await ready();
    const pick = screen.getByRole('button', { name: 'Insert image or file' });
    expect(pick).not.toHaveAttribute('aria-pressed');
    const input = container.querySelector<HTMLInputElement>('input[type="file"]')!;
    expect(input).toHaveAttribute('accept', 'image/*,.pdf');
    await user.upload(input, [
      new File(['x'], 'chart.png', { type: 'image/png' }),
      new File(['y'], 'brief.pdf', { type: 'application/pdf' }),
    ]);
    expect(onFiles).toHaveBeenCalledWith([expect.any(File), expect.any(File)], 'picker');
    await waitFor(() =>
      expect(ref.current?.getHTML()).toContain('src="https://cdn.example.com/chart.png"'),
    );
    expect(ref.current?.getHTML()).toContain('href="https://cdn.example.com/brief.pdf"');
  });

  it('suggests mentions from an injected search and inserts a mention node', async () => {
    const people: RichTextMentionItem[] = [
      { id: 'u1', label: 'Ana Silva', description: 'Design' },
      { id: 'u2', label: 'Ben Okafor', disabled: true },
      { id: 'u3', label: 'Chen Wei' },
    ];
    const search = vi.fn((text: string) =>
      people.filter((p) => p.label.toLowerCase().includes(text.toLowerCase())),
    );
    const ref = createRef<RichTextEditorHandle>();
    const { user } = setup(
      <RichTextEditor aria-label="Body" editorRef={ref} mentions={{ search, debounceMs: 0 }} />,
    );
    const box = await ready();
    await user.click(box);
    await user.keyboard('Hi @');
    const list = await screen.findByRole('listbox', { name: 'Suggestions' });
    await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(3));
    expect(box).toHaveAttribute('aria-controls', list.id);
    expect(box).toHaveAttribute('aria-activedescendant', screen.getAllByRole('option')[0]!.id);
    // ArrowDown skips the disabled option.
    await user.keyboard('{ArrowDown}');
    expect(box).toHaveAttribute('aria-activedescendant', screen.getAllByRole('option')[2]!.id);
    expect(screen.getAllByRole('option')[2]).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('{Enter}');
    await waitFor(() => expect(screen.queryByRole('listbox')).toBeNull());
    expect(ref.current?.getHTML()).toContain('data-type="mention"');
    expect(ref.current?.getHTML()).toContain('data-id="u3"');
    expect(ref.current?.getText()).toContain('@Chen Wei');
    expect(box).not.toHaveAttribute('aria-activedescendant');
  });

  it('Escape dismisses the mention list without inserting', async () => {
    const search = vi.fn(() => [{ id: 'u1', label: 'Ana Silva' }]);
    const ref = createRef<RichTextEditorHandle>();
    const { user } = setup(
      <RichTextEditor aria-label="Body" editorRef={ref} mentions={{ search, debounceMs: 0 }} />,
    );
    const box = await ready();
    await user.click(box);
    await user.keyboard('@an');
    await screen.findByRole('option');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(ref.current?.getHTML()).toBe('<p>@an</p>');
  });

  it('lets the product handle keys first through onEditorKeyDown', async () => {
    const onKey = vi.fn((e: KeyboardEvent) => e.key === 'Enter' && !e.shiftKey);
    const ref = createRef<RichTextEditorHandle>();
    const { user } = setup(
      <RichTextEditor aria-label="Body" editorRef={ref} onEditorKeyDown={onKey} />,
    );
    const box = await ready();
    await user.click(box);
    await user.keyboard('a{Enter}b');
    expect(ref.current?.getHTML()).toBe('<p>ab</p>');
    expect(onKey).toHaveBeenCalled();
  });

  it('submits the HTML with a form when named', async () => {
    const { container } = setup(
      <RichTextEditor aria-label="Body" name="body" defaultValue="<p>Hi</p>" />,
    );
    await ready();
    await waitFor(() =>
      expect(container.querySelector<HTMLInputElement>('input[name="body"]')?.value).toBe(
        '<p>Hi</p>',
      ),
    );
  });

  it('has no axe violations', async () => {
    const { container } = setup(
      <RichTextEditor
        aria-label="Reply"
        placeholder="Write a reply"
        maxLength={200}
        defaultValue="<p>Hi</p>"
      />,
    );
    await ready();
    await expectNoA11yViolations(container);
  });

  it('renders a static placeholder on the server', () => {
    const html = renderToString(<RichTextEditor aria-label="Reply" placeholder="Write a reply" />);
    expect(html).toContain('mn-rich-text-editor');
    expect(html).toContain('Write a reply');
    expect(html).toContain('aria-busy="true"');
    expect(html).not.toContain('contenteditable');
  });
});
