import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FileCard } from './index.js';

describe('FileCard', () => {
  it('links the name, describes it with type and size, and forwards the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<FileCard ref={ref} name="plan.pdf" size={2_097_152} href="/files/plan.pdf" />);
    expect(ref.current).toHaveClass('mn-file-card');
    expect(ref.current).toHaveAttribute('data-layout', 'card');
    expect(ref.current).toHaveAttribute('data-family', 'pdf');
    const link = screen.getByRole('link', { name: 'plan.pdf' });
    expect(link).toHaveAttribute('href', '/files/plan.pdf');
    expect(link).toHaveAccessibleDescription('PDF document 2 MB');
    await expectNoA11yViolations();
  });

  it('opens with a button from the keyboard when there is no href', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    render(<FileCard name="notes.txt" onOpen={onOpen} layout="row" />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'notes.txt' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('selects with a named checkbox and reports Shift', async () => {
    const user = userEvent.setup();
    const onSelectedChange = vi.fn();
    const { container, rerender } = render(
      <FileCard name="a.png" selectable onSelectedChange={onSelectedChange} />,
    );
    const box = screen.getByRole('checkbox', { name: 'Select a.png' });
    await user.click(box);
    expect(onSelectedChange).toHaveBeenLastCalledWith(true, { shiftKey: false });
    fireEvent.click(box, { shiftKey: true });
    expect(onSelectedChange).toHaveBeenLastCalledWith(true, { shiftKey: true });
    rerender(<FileCard name="a.png" selectable selected onSelectedChange={onSelectedChange} />);
    expect(container.firstElementChild).toHaveAttribute('data-selected');
    expect(screen.getByRole('checkbox')).toBeChecked();
    await expectNoA11yViolations();
  });

  it('shows thumbnails only from safe sources and falls back on error', () => {
    const { container, rerender } = render(
      <FileCard name="p.jpg" thumbnailSrc="https://example.com/p.jpg" />,
    );
    const img = container.querySelector('img') as HTMLImageElement;
    expect(img).toHaveAttribute('alt', '');
    fireEvent.error(img);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('.mn-file-type-icon')).not.toBeNull();
    rerender(<FileCard name="p.jpg" thumbnailSrc="javascript:alert(1)" />);
    expect(container.querySelector('img')).toBeNull();
  });

  it('groups actions, shows badges and meta, ignores unsafe links and disables', async () => {
    const { rerender } = render(
      <FileCard
        name="data.csv"
        href="javascript:void(0)"
        meta="Edited by Sam"
        badges={<span>Shared</span>}
        actions={<button type="button">Share</button>}
        messages={{ types: { spreadsheet: 'Tableur' } }}
      />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByRole('group', { name: 'Actions for data.csv' })).toContainElement(
      screen.getByRole('button', { name: 'Share' }),
    );
    expect(screen.getByText('Tableur')).toBeInTheDocument();
    expect(screen.getByText('Edited by Sam')).toBeInTheDocument();
    expect(screen.getByText('Shared')).toBeInTheDocument();
    rerender(<FileCard name="data.csv" onOpen={() => {}} disabled selectable />);
    expect(screen.getByRole('button', { name: 'data.csv' })).toBeDisabled();
    expect(screen.getByRole('checkbox')).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<FileCard name="deck.pptx" size={10} selectable />);
    expect(html).toContain('mn-file-card');
    expect(html).toContain('deck.pptx');
  });
});
