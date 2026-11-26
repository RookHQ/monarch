import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ChangelogFeed, type ChangelogEntry } from './index.js';

const entries: ChangelogEntry[] = [
  {
    id: 'export',
    date: '2026-06-01',
    title: 'Export to CSV',
    version: '2.4.0',
    tags: [{ label: 'New', tone: 'accent' }, 'Reports'],
    body: <p>Every report can be exported.</p>,
    href: '/changelog/export',
  },
  { id: 'fix', date: new Date('2026-05-20T00:00:00Z'), title: 'Faster search', tags: ['Fix'] },
];

describe('ChangelogFeed', () => {
  it('is a section named by its title with an ordered list of dated entries', async () => {
    render(<ChangelogFeed title="Changelog" entries={entries} locale="en-US" />);
    const section = screen.getByRole('region', { name: 'Changelog' });
    expect(section).toHaveClass('mn-changelog-feed');
    const items = within(section)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement?.tagName === 'OL');
    expect(items).toHaveLength(2);
    const first = screen.getByRole('article', { name: 'Export to CSV' });
    expect(first).toHaveAttribute('id', 'export');
    const time = first.querySelector('time')!;
    expect(time).toHaveTextContent('June 1, 2026');
    expect(time).toHaveAttribute('datetime', '2026-06-01');
    expect(first).toHaveTextContent('2.4.0');
    expect(within(first).getByText('New')).toBeVisible();
    expect(within(first).getByText('Reports')).toBeVisible();
    expect(screen.getByRole('heading', { level: 3, name: 'Faster search' })).toBeVisible();
    await expectNoA11yViolations();
  });

  it('shows a safe RSS link and Read more links reachable by keyboard', async () => {
    const user = userEvent.setup();
    render(<ChangelogFeed title="Updates" entries={entries} rssHref="/changelog.xml" />);
    await user.tab();
    const rss = screen.getByRole('link', { name: 'RSS feed' });
    expect(rss).toHaveFocus();
    expect(rss).toHaveAttribute('href', '/changelog.xml');
    await user.tab();
    const more = screen.getByRole('link', { name: 'Read more' });
    expect(more).toHaveFocus();
    expect(more).toHaveAccessibleDescription('Export to CSV');
    await expectNoA11yViolations();
  });

  it('drops unsafe RSS and entry URLs, and accepts an RSS slot', () => {
    const { rerender } = render(
      <ChangelogFeed
        entries={[{ id: 'a', date: '2026-01-01', title: 'A', href: 'javascript:alert(1)' }]}
        rssHref="javascript:alert(1)"
      />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    rerender(<ChangelogFeed entries={[]} rssSlot={<button type="button">Subscribe</button>} />);
    expect(screen.getByRole('button', { name: 'Subscribe' })).toBeVisible();
  });

  it('shows the empty message, a footer, a fallback name and translated text', () => {
    const ref = createRef<HTMLElement>();
    render(
      <ChangelogFeed
        ref={ref}
        entries={[]}
        footer={<button type="button">Older</button>}
        messages={{ listLabel: 'Novedades', empty: 'Sin novedades.' }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Novedades' })).toBe(ref.current);
    expect(screen.getByText('Sin novedades.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Older' })).toBeVisible();
  });

  it('formats dates in the given locale and time zone and keeps unparseable dates as text', () => {
    render(
      <ChangelogFeed
        entries={[
          { id: 'a', date: '2026-03-31T23:30:00Z', title: 'A' },
          { id: 'b', date: 'Spring', title: 'B' },
        ]}
        locale="en-GB"
        timeZone="Asia/Kolkata"
        dateFormat={{ day: 'numeric', month: 'short', year: 'numeric' }}
        headingLevel={3}
      />,
    );
    expect(screen.getByRole('article', { name: 'A' })).toHaveTextContent('1 Apr 2026');
    expect(screen.getByRole('article', { name: 'B' })).toHaveTextContent('Spring');
    expect(screen.getByRole('heading', { level: 4, name: 'A' })).toBeVisible();
  });

  it('names each entry even when its id has a space, and survives an unknown time zone', () => {
    render(
      <ChangelogFeed
        entries={[{ id: 'spring release', date: '2026-03-02', title: 'Spring release' }]}
        locale="en-US"
        timeZone="Mars/Olympus"
      />,
    );
    expect(screen.getByRole('article', { name: 'Spring release' })).toBeInTheDocument();
    expect(screen.getByText('March 2, 2026')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ChangelogFeed title="Changelog" entries={entries} locale="en-US" />,
    );
    expect(html).toContain('mn-changelog-feed');
    expect(html).toContain('June 1, 2026');
  });
});
