import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AppShell } from './index.js';

function shell(props: Partial<Parameters<typeof AppShell>[0]> = {}) {
  return (
    <AppShell
      rail={
        <nav aria-label="Main">
          <a href="/inbox">Inbox</a>
        </nav>
      }
      header={
        <header>
          <span>Acme Tickets</span>
        </header>
      }
      {...props}
    >
      <h1>Inbox</h1>
      <p>Content</p>
    </AppShell>
  );
}

describe('AppShell', () => {
  it('renders the landmarks in order: navigation, banner, main', async () => {
    const { container } = render(shell());
    const nav = screen.getByRole('navigation', { name: 'Main' });
    const banner = screen.getByRole('banner');
    const main = screen.getByRole('main');
    expect(main).toHaveAttribute('id', 'main');
    expect(nav.compareDocumentPosition(banner) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(banner.compareDocumentPosition(main) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(container.firstElementChild).toHaveAttribute('data-scroll', 'main');
    await expectNoA11yViolations();
  });

  it('has a skip link as the first Tab stop that targets main', async () => {
    render(shell());
    const user = userEvent.setup();
    await user.tab();
    const skip = screen.getByRole('link', { name: 'Skip to content' });
    expect(skip).toHaveFocus();
    expect(skip).toHaveAttribute('href', '#main');
    // <main> can take focus from the skip link without becoming a Tab stop.
    expect(screen.getByRole('main')).toHaveAttribute('tabindex', '-1');
    await user.tab();
    expect(screen.getByRole('link', { name: 'Inbox' })).toHaveFocus();
  });

  it('takes a custom main id, skip text and main props', () => {
    render(
      shell({
        mainId: 'content',
        skipLinkLabel: 'Aller au contenu',
        mainProps: { 'aria-label': 'Inbox' },
      }),
    );
    expect(screen.getByRole('link', { name: 'Aller au contenu' })).toHaveAttribute(
      'href',
      '#content',
    );
    expect(screen.getByRole('main', { name: 'Inbox' })).toHaveAttribute('id', 'content');
  });

  it('page scrolling, and works without a rail or header', () => {
    const { container } = render(
      <AppShell scroll="page">
        <p>Only content</p>
      </AppShell>,
    );
    const root = container.firstElementChild!;
    expect(root).toHaveAttribute('data-scroll', 'page');
    expect(root).not.toHaveAttribute('data-rail');
    expect(root).not.toHaveAttribute('data-header');
    expect(screen.getByRole('main')).toHaveTextContent('Only content');
  });

  it('embeds as a named region when the page already has a main', () => {
    render(
      shell({ mainRole: 'region', mainId: 'preview', mainProps: { 'aria-label': 'Preview' } }),
    );
    expect(screen.queryByRole('main')).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Preview' })).toHaveAttribute('id', 'preview');
  });

  it('renders on the server', () => {
    const html = renderToString(shell());
    expect(html).toContain('mn-app-shell');
    expect(html).toContain('<main');
  });
});
