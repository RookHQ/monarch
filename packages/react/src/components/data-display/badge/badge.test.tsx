import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Badge } from './index.js';

describe('Badge', () => {
  it('renders accessibly with a decorative dot', async () => {
    render(
      <main>
        <Badge tone="success" dot>
          Resolved
        </Badge>
      </main>,
    );
    const badge = screen.getByText('Resolved');
    expect(badge).toHaveAttribute('data-tone', 'success');
    expect(badge.querySelector('.mn-badge__dot')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Badge>New</Badge>);
    expect(html).toContain('mn-badge');
  });
});
