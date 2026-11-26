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

  it('keeps the child element with asChild on an external link, and translates the note', () => {
    render(
      <Link asChild target="_blank" newTabLabel="s’ouvre dans un nouvel onglet">
        <a href="https://example.com" data-router="">
          Docs
        </a>
      </Link>,
    );
    const link = screen.getByRole('link', { name: 'Docs (s’ouvre dans un nouvel onglet)' });
    expect(link).toHaveAttribute('data-router');
    expect(link).toHaveAttribute('href', 'https://example.com');
    expect(link).toHaveClass('mn-link');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(link.querySelector('.mn-link__external')).not.toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<Link href="/x">x</Link>);
    expect(html).toContain('mn-link');
  });
});
