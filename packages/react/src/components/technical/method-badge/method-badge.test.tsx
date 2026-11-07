import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MethodBadge, methodTone } from './index.js';

describe('MethodBadge', () => {
  it('shows the method as text with its tone', async () => {
    render(
      <ul>
        {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'].map((m) => (
          <li key={m}>
            <MethodBadge method={m} data-testid={m} />
          </li>
        ))}
      </ul>,
    );
    expect(screen.getByTestId('GET')).toHaveTextContent('GET');
    expect(screen.getByTestId('GET')).toHaveAttribute('data-tone', 'success');
    expect(screen.getByTestId('POST')).toHaveAttribute('data-tone', 'info');
    expect(screen.getByTestId('PUT')).toHaveAttribute('data-tone', 'warning');
    expect(screen.getByTestId('PATCH')).toHaveAttribute('data-tone', 'warning');
    expect(screen.getByTestId('DELETE')).toHaveAttribute('data-tone', 'danger');
    expect(screen.getByTestId('HEAD')).toHaveAttribute('data-tone', 'neutral');
    await expectNoA11yViolations();
  });

  it('upper-cases the method and treats unknown methods as neutral', () => {
    render(<MethodBadge method="delete" data-testid="a" />);
    const badge = screen.getByTestId('a');
    expect(badge).toHaveTextContent('DELETE');
    expect(badge).toHaveAttribute('data-method', 'delete');
    expect(badge).toHaveAttribute('data-tone', 'danger');
    expect(methodTone('TRACE')).toBe('neutral');
  });

  it('accepts a tone override, size, className and ref', () => {
    const ref = createRef<HTMLSpanElement>();
    render(
      <MethodBadge ref={ref} method="GET" tone="neutral" size="sm" className="x" title="Read" />,
    );
    expect(ref.current).toHaveClass('mn-method-badge', 'x');
    expect(ref.current).toHaveAttribute('data-tone', 'neutral');
    expect(ref.current).toHaveAttribute('data-size', 'sm');
    expect(ref.current).toHaveAttribute('title', 'Read');
  });

  it('renders on the server', () => {
    const html = renderToString(<MethodBadge method="post" />);
    expect(html).toContain('mn-method-badge');
    expect(html).toContain('POST');
  });
});
