import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiResponseSkeleton } from './index.js';

describe('AiResponseSkeleton', () => {
  it('is a busy status with a hidden label and decorative bars, forwarding ref and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiResponseSkeleton ref={ref} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-response-skeleton', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('status')).toHaveTextContent('Generating response…');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(root).toHaveAttribute('data-shape', 'text');
    const shapes = root.querySelector('.mn-ai-response-skeleton__shapes');
    expect(shapes).toHaveAttribute('aria-hidden', 'true');
    expect(root.querySelectorAll('.mn-ai-response-skeleton__bar')).toHaveLength(3);
    await expectNoA11yViolations();
  });

  it('renders the requested number of lines, cards and table rows', () => {
    const { rerender, container } = render(<AiResponseSkeleton lines={5} />);
    expect(container.querySelectorAll('.mn-ai-response-skeleton__bar')).toHaveLength(5);
    rerender(<AiResponseSkeleton shape="cards" lines={2} />);
    expect(container.querySelectorAll('.mn-ai-response-skeleton__card')).toHaveLength(2);
    rerender(<AiResponseSkeleton shape="table" lines={3} columns={4} />);
    const rows = container.querySelectorAll('.mn-ai-response-skeleton__row');
    expect(rows).toHaveLength(4); // header + 3
    expect(rows[0]).toHaveAttribute('data-header');
    expect(rows[1]?.querySelectorAll('.mn-ai-response-skeleton__bar')).toHaveLength(4);
  });

  it('localises the label', () => {
    render(<AiResponseSkeleton messages={{ label: 'Réponse en cours…' }} />);
    expect(screen.getByRole('status')).toHaveTextContent('Réponse en cours…');
  });

  it('renders identically on the server', () => {
    const html = renderToString(<AiResponseSkeleton shape="table" />);
    expect(html).toContain('mn-ai-response-skeleton');
    expect(html).toContain('data-shape="table"');
    expect(renderToString(<AiResponseSkeleton shape="table" />)).toBe(html);
  });
});
