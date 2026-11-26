import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { PageOutline, collectOutline, type PageOutlineHeading } from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

const HEADINGS: PageOutlineHeading[] = [
  { id: 'intro', level: 2, text: 'Introduction' },
  { id: 'setup', level: 2, text: 'Setup' },
  { id: 'install', level: 3, text: 'Install' },
  { id: 'deep', level: 4, text: 'Too deep' },
  { id: 'faq', level: 2, text: 'Questions' },
];

let current: Editor | null = null;
function WithEditor({ content }: { content: string }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [KIT],
    content,
    editorProps: { attributes: { 'aria-label': 'Document', role: 'textbox' } },
  });
  current = editor;
  return (
    <>
      <PageOutline editor={editor} showStats />
      <EditorContent editor={editor} />
    </>
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('PageOutline', () => {
  it('is a named navigation landmark with a nested list, skipping levels past maxLevel', async () => {
    render(<PageOutline headings={HEADINGS} />);
    const nav = screen.getByRole('navigation', { name: 'On this page' });
    const links = within(nav).getAllByRole('link');
    expect(links.map((l) => l.textContent)).toEqual([
      'Introduction',
      'Setup',
      'Install',
      'Questions',
    ]);
    expect(links[2]?.closest('ul')).toHaveAttribute('data-depth', '1');
    expect(links[0]).toHaveAttribute('href', '#intro');
    await expectNoA11yViolations();
  });

  it('scrolls to and focuses the heading when a link is chosen, and marks it current', async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    render(
      <>
        <PageOutline headings={HEADINGS} onNavigate={onNavigate} />
        <h2 id="setup">Setup</h2>
      </>,
    );
    const target = screen.getByRole('heading', { name: 'Setup' });
    target.scrollIntoView = vi.fn();
    const link = screen.getByRole('link', { name: 'Setup' });
    link.focus();
    await user.keyboard('{Enter}');
    expect(target.scrollIntoView).toHaveBeenCalled();
    expect(target).toHaveFocus();
    expect(target).toHaveAttribute('tabindex', '-1');
    expect(link).toHaveAttribute('aria-current', 'location');
    expect(onNavigate).toHaveBeenCalledWith(HEADINGS[1]);
  });

  it('follows the controlled activeId', () => {
    render(<PageOutline headings={HEADINGS} activeId="faq" />);
    expect(screen.getByRole('link', { name: 'Questions' })).toHaveAttribute(
      'aria-current',
      'location',
    );
  });

  it('marks the section being read with scroll-spy', async () => {
    let callback: IntersectionObserverCallback = () => undefined;
    const observed: Element[] = [];
    vi.stubGlobal(
      'IntersectionObserver',
      class {
        constructor(cb: IntersectionObserverCallback) {
          callback = cb;
        }
        observe(el: Element) {
          observed.push(el);
        }
        disconnect() {}
        unobserve() {}
        takeRecords() {
          return [];
        }
      },
    );
    const onActiveChange = vi.fn();
    render(
      <>
        <PageOutline headings={HEADINGS.slice(0, 2)} onActiveChange={onActiveChange} />
        <h2 id="intro">Introduction</h2>
        <h2 id="setup">Setup</h2>
      </>,
    );
    expect(observed).toHaveLength(2);
    act(() =>
      callback(
        [
          {
            target: observed[1],
            isIntersecting: true,
            boundingClientRect: { top: 10 },
            rootBounds: { top: 0 },
          } as unknown as IntersectionObserverEntry,
        ],
        {} as IntersectionObserver,
      ),
    );
    expect(screen.getByRole('link', { name: 'Setup' })).toHaveAttribute('aria-current', 'location');
    expect(onActiveChange).toHaveBeenLastCalledWith('setup');
  });

  it('shows an empty message without headings and renders on the server', () => {
    render(<PageOutline headings={[]} messages={{ empty: 'No sections yet' }} />);
    expect(screen.getByText('No sections yet')).toBeInTheDocument();
    const html = renderToString(<PageOutline headings={HEADINGS} hideTitle />);
    expect(html).toContain('aria-label="On this page"');
    expect(html).toContain('href="#intro"');
  });

  it('follows a Tiptap editor: live headings, stats, and moves the caret on navigation', async () => {
    const user = userEvent.setup();
    render(
      <WithEditor content="<h2>Overview</h2><p>Some words here.</p><h3>Details</h3><p>More.</p>" />,
    );
    await waitFor(() => expect(current).not.toBeNull());
    expect(await screen.findByRole('link', { name: 'Overview' })).toBeInTheDocument();
    expect(screen.getByText('6 words · 1 min read')).toBeInTheDocument();
    act(() => {
      current
        ?.chain()
        .setTextSelection(current.state.doc.content.size)
        .insertContent('<h2>Next steps</h2>')
        .run();
    });
    expect(await screen.findByRole('link', { name: 'Next steps' })).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Details' }));
    expect(current?.state.selection.$from.parent.textContent).toBe('Details');
    expect(screen.getByRole('link', { name: 'Details' })).toHaveAttribute(
      'aria-current',
      'location',
    );
  });
});

describe('collectOutline', () => {
  it('skips empty headings and makes ids unique', async () => {
    render(<WithEditor content="<h2>Notes</h2><h2></h2><h2>Notes</h2><h4>Deep</h4>" />);
    await waitFor(() => expect(current).not.toBeNull());
    const list = collectOutline((current as Editor).state.doc);
    expect(list.map((h) => h.id)).toEqual(['notes', 'notes-2']);
    expect(collectOutline((current as Editor).state.doc, 4).map((h) => h.text)).toEqual([
      'Notes',
      'Notes',
      'Deep',
    ]);
  });

  it('keeps ids unique when a heading’s own text looks like a numbered duplicate', async () => {
    render(<WithEditor content="<h2>Intro</h2><h2>Intro</h2><h2>Intro 2</h2><h2>Intro</h2>" />);
    await waitFor(() => expect(current).not.toBeNull());
    const ids = collectOutline((current as Editor).state.doc).map((h) => h.id);
    expect(ids.slice(0, 2)).toEqual(['intro', 'intro-2']);
    expect(new Set(ids).size).toBe(4);
  });
});
