import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Spinner } from './index.js';

describe('Spinner', () => {
  it('forwards its ref to the span', () => {
    const ref = createRef<HTMLSpanElement>();
    render(<Spinner ref={ref} label="Loading" />);
    expect(ref.current).toBeInstanceOf(HTMLSpanElement);
    expect(ref.current).toHaveClass('mn-spinner');
  });

  it('is a status only when labelled', () => {
    const { rerender, container } = render(<Spinner />);
    expect(container.firstChild).toHaveAttribute('aria-hidden', 'true');
    rerender(<Spinner label="Loading tickets" />);
    expect(screen.getByRole('status', { name: 'Loading tickets' })).toBeInTheDocument();
  });

  it('has no axe violations', async () => {
    render(<Spinner label="Loading tickets" />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Spinner label="Loading" />);
    expect(html).toContain('mn-spinner');
  });
});
