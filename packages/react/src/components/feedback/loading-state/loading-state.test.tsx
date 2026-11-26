import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LoadingState } from './index.js';

describe('LoadingState', () => {
  it('renders semantically', async () => {
    render(
      <main>
        <LoadingState label="Loading deals" rows={2} />
      </main>,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Loading deals');
    await expectNoA11yViolations();
  });

  it('keeps the 0.3 markup and class names', () => {
    const ref = createRef<HTMLDivElement>();
    const { container } = render(<LoadingState ref={ref} label="Loading" />);
    expect(ref.current).toHaveClass('mn-loading-state');
    expect(ref.current).toHaveAttribute('aria-busy', 'true');
    expect(ref.current).toHaveAttribute('data-variant', 'list');
    expect(container.querySelectorAll('.mn-loading-state__row')).toHaveLength(4);
    expect(container.querySelectorAll('.mn-loading-state__lines')).toHaveLength(4);
    expect(screen.getByRole('status')).toHaveClass('mn-visually-hidden');
  });

  it('rows preset renders plain text rows without avatars', () => {
    const { container } = render(<LoadingState label="Loading invoices" variant="rows" rows={3} />);
    expect(container.querySelectorAll('.mn-loading-state__row')).toHaveLength(3);
    expect(container.querySelector('[data-shape="circle"]')).toBeNull();
    expect(container.querySelectorAll('.mn-loading-state__meta')).toHaveLength(3);
  });

  it('cards and detail variants', () => {
    const { container, rerender } = render(
      <LoadingState label="Loading" variant="cards" rows={2} />,
    );
    expect(container.querySelectorAll('.mn-loading-state__row')).toHaveLength(2);
    rerender(<LoadingState label="Loading" variant="detail" />);
    expect(container.querySelectorAll('.mn-skeleton')).toHaveLength(4);
  });

  it('spinner variant shows the label as visible status text', async () => {
    const { container } = render(
      <main>
        <LoadingState label="Loading rivals" variant="spinner" />
      </main>,
    );
    const status = screen.getByRole('status');
    expect(status).toHaveTextContent('Loading rivals');
    expect(status).not.toHaveClass('mn-visually-hidden');
    expect(container.querySelector('.mn-spinner')).toHaveAttribute('aria-hidden', 'true');
    expect(container.querySelector('.mn-skeleton')).toBeNull();
    await expectNoA11yViolations();
  });

  it('treats a negative or fractional rows count safely', () => {
    const { container } = render(<LoadingState label="Loading" rows={-2} />);
    expect(container.querySelectorAll('.mn-loading-state__row')).toHaveLength(0);
    expect(screen.getByRole('status')).toHaveTextContent('Loading');
  });

  it('renders on the server', () => {
    const html = renderToString(<LoadingState label="Loading" />);
    expect(html).toContain('mn-loading-state');
  });
});
