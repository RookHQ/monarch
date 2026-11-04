import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Link } from './index.js';

describe('Link', () => {
  it('Link adds rel and a new-tab note for external links', async () => {
    render(
      <Link href="https://rookhq.com" target="_blank">
        Rook
      </Link>,
    );
    const link = screen.getByRole('link', { name: /rook.*opens in a new tab/i });
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Link href="/x">x</Link>);
    expect(html).toContain('mn-link');
  });
});
