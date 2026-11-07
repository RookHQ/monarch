import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { expectNoA11yViolationsWithFrames } from '../../../../test/a11y-frames.js';
import { AiArtifactCanvas, aiArtifactSrcDoc, type AiCanvasArtifact } from './index.js';

const doc: AiCanvasArtifact = {
  id: 'a1',
  kind: 'document',
  title: 'Q3 summary',
  version: 2,
  content: '# Summary\n\nRevenue grew **12%**.',
};

const hostile =
  '<h1 id="injected">Hi</h1><script>window.__pwned = true</script>' +
  '<img src="x" onerror="window.__pwned = true">';
const page: AiCanvasArtifact = {
  id: 'a2',
  kind: 'html',
  title: 'Landing page',
  version: 1,
  content: hostile,
};

function Harness(props: Partial<React.ComponentProps<typeof AiArtifactCanvas>>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open artifact
      </button>
      <AiArtifactCanvas
        artifact={doc}
        open={open}
        onOpenChange={setOpen}
        placement="inline"
        {...props}
      />
    </>
  );
}

afterEach(() => {
  delete (window as unknown as Record<string, unknown>).__pwned;
});

describe('AiArtifactCanvas', () => {
  it('renders a named region with title, kind, version and Preview / Code tabs', async () => {
    render(<AiArtifactCanvas artifact={doc} open onOpenChange={() => {}} placement="inline" />);
    const region = screen.getByRole('region', { name: 'Artifact: Q3 summary' });
    expect(region).toHaveClass('mn-ai-artifact-canvas');
    expect(region).toHaveAttribute('data-kind', 'document');
    expect(
      within(region).getByRole('heading', { level: 2, name: 'Q3 summary' }),
    ).toBeInTheDocument();
    expect(region).toHaveTextContent('Document · v2');
    expect(screen.getByRole('tab', { name: 'Preview' })).toHaveAttribute('aria-selected', 'true');
    // Markdown preview through AiMarkdown.
    expect(within(region).getByRole('heading', { level: 3, name: 'Summary' })).toBeInTheDocument();
    expect(region.querySelector('strong')).toHaveTextContent('12%');
    await expectNoA11yViolations();
  });

  it('switches to the Code tab with the arrow keys and shows the source', async () => {
    const user = userEvent.setup();
    render(<AiArtifactCanvas artifact={doc} open onOpenChange={() => {}} placement="inline" />);
    await user.click(screen.getByRole('tab', { name: 'Preview' }));
    await user.keyboard('{ArrowRight}');
    const code = screen.getByRole('tab', { name: 'Code' });
    expect(code).toHaveFocus();
    expect(code).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('# Summary');
  });

  it('shows only the Code tab for code', () => {
    render(
      <AiArtifactCanvas
        artifact={{
          id: 'c',
          kind: 'code',
          title: 'sum.ts',
          version: 1,
          content: 'export const a = 1;',
          language: 'ts',
        }}
        open
        onOpenChange={() => {}}
        placement="inline"
      />,
    );
    expect(screen.queryByRole('tab', { name: 'Preview' })).toBeNull();
    expect(screen.getByRole('tab', { name: 'Code' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('export const a = 1;');
  });

  describe('HTML preview security', () => {
    it('previews HTML only in a srcdoc iframe with an empty sandbox attribute', async () => {
      render(<AiArtifactCanvas artifact={page} open onOpenChange={() => {}} placement="inline" />);
      const frame = screen.getByTitle('Preview of Landing page');
      expect(frame.tagName).toBe('IFRAME');
      expect(frame.hasAttribute('sandbox')).toBe(true);
      expect(frame.getAttribute('sandbox')).toBe('');
      expect(frame.hasAttribute('src')).toBe(false);
      expect(frame.getAttribute('srcdoc')).toBe(aiArtifactSrcDoc(hostile));
      expect(frame.getAttribute('referrerpolicy')).toBe('no-referrer');
      await expectNoA11yViolationsWithFrames();
    });

    it('never injects the HTML into the page', () => {
      const { container } = render(
        <AiArtifactCanvas artifact={page} open onOpenChange={() => {}} placement="inline" />,
      );
      expect(document.getElementById('injected')).toBeNull();
      expect(container.querySelector('script')).toBeNull();
      expect(container.querySelector('img')).toBeNull();
      expect(container.querySelector('[onerror]')).toBeNull();
      expect((window as unknown as Record<string, unknown>).__pwned).toBeUndefined();
      // The only element made from the content is the iframe.
      expect(container.querySelectorAll('iframe')).toHaveLength(1);
    });

    it('blocks network requests from inside the frame with a CSP', () => {
      const srcdoc = aiArtifactSrcDoc('<p>x</p>');
      expect(srcdoc).toMatch(/http-equiv="Content-Security-Policy"/);
      expect(srcdoc).toContain("default-src 'none'");
      expect(srcdoc.indexOf('Content-Security-Policy')).toBeLessThan(srcdoc.indexOf('<p>x</p>'));
      const refresh = aiArtifactSrcDoc(
        '<META HTTP-EQUIV=Refresh content="0;url=https://example.com"><p>y</p>',
      );
      expect(refresh).not.toMatch(/refresh/i);
      expect(refresh).toContain('<p>y</p>');
    });

    it('keeps the sandbox when renderPreview falls back', () => {
      render(
        <AiArtifactCanvas
          artifact={page}
          open
          onOpenChange={() => {}}
          placement="inline"
          renderPreview={() => undefined}
        />,
      );
      expect(screen.getByTitle('Preview of Landing page').getAttribute('sandbox')).toBe('');
    });

    it('shows the HTML source as text in the Code tab', async () => {
      const user = userEvent.setup();
      const { container } = render(
        <AiArtifactCanvas artifact={page} open onOpenChange={() => {}} placement="inline" />,
      );
      await user.click(screen.getByRole('tab', { name: 'Code' }));
      expect(screen.getByRole('tabpanel')).toHaveTextContent(
        '<script>window.__pwned = true</script>',
      );
      expect(container.querySelector('script')).toBeNull();
    });
  });

  it('refuses unsafe image URLs', () => {
    render(
      <AiArtifactCanvas
        artifact={{
          id: 'i',
          kind: 'image',
          title: 'Logo',
          version: 1,
          content: 'javascript:alert(1)',
        }}
        open
        onOpenChange={() => {}}
        placement="inline"
      />,
    );
    expect(screen.queryByRole('img')).toBeNull();
    expect(
      screen.getByText('This image address is not allowed, so it is not shown.'),
    ).toBeInTheDocument();
  });

  it('uses renderPreview when it returns something', () => {
    render(
      <AiArtifactCanvas
        artifact={{ ...doc, kind: 'chart', content: '{}' }}
        open
        onOpenChange={() => {}}
        placement="inline"
        renderPreview={(a) => <p>Chart for {a.title}</p>}
      />,
    );
    expect(screen.getByRole('tab', { name: 'Preview' })).toBeInTheDocument();
    expect(screen.getByText('Chart for Q3 summary')).toBeInTheDocument();
  });

  it('opens, focuses the pane, and Escape / the close button return focus', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const opener = screen.getByRole('button', { name: 'Open artifact' });
    await user.click(opener);
    const region = screen.getByRole('region', { name: 'Artifact: Q3 summary' });
    expect(region).toHaveFocus();
    await user.tab();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('region')).toBeNull();
    expect(opener).toHaveFocus();

    await user.click(opener);
    await user.click(screen.getByRole('button', { name: 'Close artifact' }));
    expect(screen.queryByRole('region')).toBeNull();
    expect(opener).toHaveFocus();
  });

  it('runs Copy, Download and Save with the artifact', async () => {
    const user = userEvent.setup();
    const onCopy = vi.fn();
    const onDownload = vi.fn();
    let finish: () => void = () => {};
    const onSave = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(
      <AiArtifactCanvas
        artifact={doc}
        open
        onOpenChange={() => {}}
        placement="inline"
        onCopy={onCopy}
        onDownload={onDownload}
        onSave={onSave}
        saveLabel="Save to library"
      />,
    );
    await user.click(screen.getByRole('button', { name: /^Copy/ }));
    expect(await navigator.clipboard.readText()).toBe(doc.content);
    expect(onCopy).toHaveBeenCalledWith(doc);
    await user.click(screen.getByRole('button', { name: 'Download' }));
    expect(onDownload).toHaveBeenCalledWith(doc);
    const save = screen.getByRole('button', { name: 'Save to library' });
    await user.click(save);
    expect(onSave).toHaveBeenCalledWith(doc);
    expect(save).toHaveAttribute('aria-busy', 'true');
    finish();
    await vi.waitFor(() => expect(save).not.toHaveAttribute('aria-busy', 'true'));
  });

  it('steps versions through AiArtifactVersions', async () => {
    const user = userEvent.setup();
    const onVersionChange = vi.fn();
    render(
      <AiArtifactCanvas
        artifact={doc}
        versions={[
          { version: 1, createdAt: '2026-03-01' },
          { version: 2, createdAt: '2026-03-02' },
        ]}
        onVersionChange={onVersionChange}
        open
        onOpenChange={() => {}}
        placement="inline"
      />,
    );
    expect(screen.getByText('Version 2 of 2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Previous version' }));
    expect(onVersionChange).toHaveBeenCalledWith(1);
  });

  it('renders as a modal sheet with placement="sheet"', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<AiArtifactCanvas artifact={page} open onOpenChange={onOpenChange} placement="sheet" />);
    const dialog = screen.getByRole('dialog', { name: 'Landing page' });
    expect(dialog).toHaveAttribute('data-placement', 'sheet');
    expect(within(dialog).getByTitle('Preview of Landing page').getAttribute('sandbox')).toBe('');
    await user.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('renders nothing while closed and renders on the server', () => {
    const { container } = render(
      <AiArtifactCanvas artifact={doc} open={false} onOpenChange={() => {}} placement="inline" />,
    );
    expect(container).toBeEmptyDOMElement();
    const html = renderToString(<AiArtifactCanvas artifact={page} open onOpenChange={() => {}} />);
    expect(html).toContain('mn-ai-artifact-canvas');
    expect(html).toContain('sandbox=""');
    expect(html).not.toContain('<script>');
  });
});
