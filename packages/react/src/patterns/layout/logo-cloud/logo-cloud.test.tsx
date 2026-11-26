import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LogoCloud } from './index.js';

describe('LogoCloud', () => {
  it('is a section named by its title with a list of logos and wordmarks', async () => {
    render(
      <LogoCloud
        title="Teams that use it"
        logos={[
          { name: 'Company A', src: '/logos/a.svg', width: 120, height: 32 },
          { name: 'Company B' },
        ]}
      />,
    );
    const section = screen.getByRole('region', { name: 'Teams that use it' });
    expect(section).toHaveClass('mn-logo-cloud');
    const items = within(section).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(within(items[0]!).getByRole('img', { name: 'Company A' })).toHaveAttribute(
      'src',
      '/logos/a.svg',
    );
    expect(items[1]).toHaveTextContent('Company B');
    await expectNoA11yViolations();
  });

  it('drops unsafe image and link URLs, showing the name instead', () => {
    render(
      <LogoCloud
        logos={[
          { name: 'Company C', src: 'javascript:alert(1)', href: 'javascript:alert(1)' },
          { name: 'Company D', src: 'data:image/svg+xml,<svg/>' },
        ]}
      />,
    );
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText('Company C')).toBeVisible();
    expect(screen.getByText('Company D')).toBeVisible();
  });

  it('falls back to the wordmark when an image fails to load', () => {
    render(<LogoCloud logos={[{ name: 'Company E', src: 'https://example.com/e.png' }]} />);
    fireEvent.error(screen.getByRole('img', { name: 'Company E' }));
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.getByText('Company E')).toBeVisible();
  });

  it('links logos, says when a link opens a new tab, and is keyboard reachable', async () => {
    render(
      <LogoCloud
        logos={[
          { name: 'Company F', href: '/stories/f' },
          { name: 'Company G', href: 'https://example.com', external: true },
        ]}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Company F' })).toHaveFocus();
    await user.tab();
    const external = screen.getByRole('link', { name: 'Company G (opens in a new tab)' });
    expect(external).toHaveFocus();
    expect(external).toHaveAttribute('target', '_blank');
    expect(external).toHaveAttribute('rel', 'noopener noreferrer');
    await expectNoA11yViolations();
  });

  it('uses a fallback name without a title, sets layout data and forwards the ref', () => {
    const ref = createRef<HTMLElement>();
    render(
      <LogoCloud
        ref={ref}
        layout="grid"
        size="lg"
        monochrome={false}
        logos={[{ name: 'Company H' }]}
        messages={{ listLabel: 'Organizaciones' }}
      />,
    );
    const section = screen.getByRole('region', { name: 'Organizaciones' });
    expect(section).toBe(ref.current);
    expect(section).toHaveAttribute('data-layout', 'grid');
    expect(section).toHaveAttribute('data-size', 'lg');
    expect(section).not.toHaveAttribute('data-monochrome');
  });

  it('renders on the server', () => {
    const html = renderToString(<LogoCloud title="Logos" logos={[{ name: 'Company A' }]} />);
    expect(html).toContain('mn-logo-cloud');
    expect(html).toContain('Company A');
  });
});
