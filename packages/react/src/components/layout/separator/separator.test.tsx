import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Separator } from './index.js';

describe('Separator', () => {
  it('is decorative by default and semantic when asked', () => {
    const { container, rerender } = render(<Separator />);
    expect(container.querySelector('[role="separator"]')).toBeNull();
    rerender(<Separator decorative={false} />);
    expect(screen.getByRole('separator')).toBeInTheDocument();
  });

  it('has no axe violations', async () => {
    render(
      <main>
        <p>Above</p>
        <Separator decorative={false} />
        <p>Below</p>
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Separator />);
    expect(html).toContain('mn-separator');
  });
});
