import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Container } from './index.js';

describe('Container', () => {
  it('renders a content-width container with gutters by default', async () => {
    render(<Container data-testid="root">Page</Container>);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-container');
    expect(root).toHaveAttribute('data-size', 'content');
    expect(root).toHaveAttribute('data-gutter');
    await expectNoA11yViolations();
  });

  it('takes a size and can drop its gutters', () => {
    render(<Container data-testid="root" size="narrow" gutter={false} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-size', 'narrow');
    expect(root).not.toHaveAttribute('data-gutter');
  });

  it('renders as a landmark', async () => {
    render(<Container as="main">Content</Container>);
    expect(screen.getByRole('main')).toHaveClass('mn-container');
    await expectNoA11yViolations();
  });

  it('forwards its ref and className', () => {
    const ref = { current: null as HTMLElement | null };
    render(<Container ref={ref} className="extra" size="wide" />);
    expect(ref.current).toHaveClass('mn-container', 'extra');
    expect(ref.current).toHaveAttribute('data-size', 'wide');
  });

  it('renders on the server', () => {
    expect(renderToString(<Container size="full" />)).toContain('data-size="full"');
  });
});
