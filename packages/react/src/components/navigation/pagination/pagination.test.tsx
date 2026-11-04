import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Pagination, pageWindow } from './index.js';

describe('Pagination', () => {
  it('computes a stable window', () => {
    expect(pageWindow(1, 5)).toEqual([1, 2, 3, 4, 5]);
    expect(pageWindow(5, 20)).toEqual([1, 'gap', 4, 5, 6, 'gap', 20]);
    expect(pageWindow(1, 20)).toEqual([1, 2, 3, 4, 'gap', 20]);
    expect(pageWindow(20, 20)).toEqual([1, 'gap', 17, 18, 19, 20]);
  });

  it('is a labelled nav with current page and working arrows', async () => {
    const onPageChange = vi.fn();
    render(<Pagination page={3} pageCount={10} onPageChange={onPageChange} />);
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Page 3' })).toHaveAttribute('aria-current', 'page');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Next page' }));
    expect(onPageChange).toHaveBeenCalledWith(4);
    await user.click(screen.getByRole('button', { name: 'Page 10' }));
    expect(onPageChange).toHaveBeenCalledWith(10);
    await expectNoA11yViolations();
  });

  it('compact variant shows the item range and disables at the edges', () => {
    render(
      <Pagination
        variant="compact"
        page={1}
        pageCount={8}
        pageSize={20}
        totalItems={143}
        onPageChange={() => {}}
      />,
    );
    expect(screen.getByText('1–20 of 143')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<Pagination page={1} pageCount={3} onPageChange={() => {}} />);
    expect(html).toContain('mn-pagination');
  });
});
