import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import { AiSourcePreview, findQuoteRange } from './index.js';

const source: AiSourcePart = {
  type: 'source',
  id: 's1',
  title: 'Team handbook',
  url: 'https://example.com/handbook',
  kind: 'doc',
  product: 'Docs',
};

const text = 'Welcome to the team.\n\nRequests are answered within two working days.\n\nThanks.';

function Harness({ initial = false }: { initial?: boolean }) {
  const [open, setOpen] = useState(initial);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Preview
      </button>
      <AiSourcePreview
        source={source}
        quote="answered   within two working days"
        content={text}
        open={open}
        onOpenChange={setOpen}
      />
    </>
  );
}

describe('AiSourcePreview', () => {
  it('opens a dialog named by the source, highlights the quote and scrolls to it', async () => {
    const scroll = vi.fn();
    HTMLElement.prototype.scrollIntoView = scroll;
    render(<Harness initial />);
    const dialog = await screen.findByRole('dialog', { name: 'Team handbook' });
    expect(dialog).toHaveClass('mn-ai-source-preview');
    expect(dialog).toHaveAccessibleDescription('example.com · Docs · Document');
    const mark = dialog.querySelector('mark');
    expect(mark).toHaveTextContent('answered within two working days');
    expect(dialog.querySelectorAll('p')).not.toHaveLength(0);
    await waitFor(() => expect(scroll).toHaveBeenCalled());
    const link = screen.getByRole('link', { name: 'Open original (opens in a new tab)' });
    expect(link).toHaveAttribute('href', source.url);
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    await expectNoA11yViolations();
  });

  it('closes on Escape and returns focus to the opener', async () => {
    render(<Harness />);
    const user = userEvent.setup();
    const opener = screen.getByRole('button', { name: 'Preview' });
    await user.click(opener);
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(opener).toHaveFocus();
  });

  it('shows the quote on its own when the content does not contain it', async () => {
    render(
      <AiSourcePreview
        source={{ ...source, url: 'javascript:alert(1)' }}
        quote="Not in the text"
        content="Something else."
        open
        onOpenChange={() => {}}
      />,
    );
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Quoted passage');
    expect(dialog.querySelector('mark')).toHaveTextContent('Not in the text');
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('renders node content as is and says when there is nothing to preview', async () => {
    const { rerender } = render(
      <AiSourcePreview
        source={source}
        content={<section aria-label="Record">Status: open</section>}
        open
        onOpenChange={() => {}}
      />,
    );
    expect(await screen.findByRole('region', { name: 'Record' })).toBeInTheDocument();
    rerender(<AiSourcePreview source={source} open onOpenChange={() => {}} />);
    expect(screen.getByText('No preview is available for this source.')).toBeInTheDocument();
  });

  it('finds quotes ignoring case and whitespace', () => {
    expect(findQuoteRange('One  Two\nthree', 'one two three')).toEqual([0, 14]);
    expect(findQuoteRange('abc', 'a.c')).toBeUndefined();
    expect(findQuoteRange('abc', '  ')).toBeUndefined();
  });

  it('renders on the server without crashing', () => {
    expect(() =>
      renderToString(<AiSourcePreview source={source} open={false} onOpenChange={() => {}} />),
    ).not.toThrow();
  });
});
