import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PostCard, truncateExcerpt, type PostCardProps } from './index.js';

const NOW = new Date('2026-05-10T12:00:00Z');

const BASE: PostCardProps = {
  title: 'How do you plan your week?',
  href: '/posts/42',
  excerpt: 'I keep a short list every Monday and review it on Friday.',
  author: { name: 'Sam Lee' },
  publishedAt: new Date('2026-05-10T09:00:00Z'),
  now: NOW,
};

describe('truncateExcerpt', () => {
  it('keeps short text and cuts long text at a word', () => {
    expect(truncateExcerpt('  short  ')).toBe('short');
    expect(truncateExcerpt('one two three four', 10)).toBe('one two…');
    expect(truncateExcerpt('abcdefghijklmnop', 5)).toBe('abcde…');
  });
});

describe('PostCard', () => {
  it('renders an article with a heading link, author, time and excerpt', async () => {
    const ref = createRef<HTMLElement>();
    render(<PostCard ref={ref} {...BASE} />);
    const article = screen.getByRole('article');
    expect(ref.current).toBe(article);
    expect(article).toHaveClass('mn-post-card');
    const heading = screen.getByRole('heading', { level: 2, name: 'How do you plan your week?' });
    expect(within(heading).getByRole('link')).toHaveAttribute('href', '/posts/42');
    expect(screen.getByText('Sam Lee')).toBeInTheDocument();
    expect(screen.getByText('3 hours ago')).toBeInTheDocument();
    expect(screen.getByText(/I keep a short list/)).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows markers and counts with spoken labels', async () => {
    render(
      <PostCard
        {...BASE}
        pinned
        answered
        tags={[{ label: 'Questions' }, { label: 'In review', tone: 'warning' }]}
        reactionCount={1}
        commentCount={4}
        headingLevel={3}
        excerpt={'word '.repeat(100)}
        excerptLength={30}
      />,
    );
    expect(screen.getByRole('article')).toHaveAttribute('data-pinned', 'true');
    expect(screen.getByText('Pinned')).toBeInTheDocument();
    expect(screen.getByText('Answered')).toBeInTheDocument();
    expect(screen.getByText('In review')).toBeInTheDocument();
    expect(screen.getByText('1 reaction')).toHaveClass('mn-visually-hidden');
    expect(screen.getByText('4 comments')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3 })).toBeInTheDocument();
    expect(screen.getByText(/…$/).textContent?.length).toBeLessThanOrEqual(31);
    await expectNoA11yViolations();
  });

  it('uses a custom link and keeps actions reachable', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <PostCard
        {...BASE}
        renderLink={({ children, ...p }) => (
          <a {...p} data-router="">
            {children}
          </a>
        )}
        actions={
          <button type="button" onClick={onSave}>
            Save post
          </button>
        }
        messages={{ comments: (n) => `${n} replies` }}
        commentCount={2}
      />,
    );
    expect(screen.getByRole('link')).toHaveAttribute('data-router');
    expect(screen.getByText('2 replies')).toBeInTheDocument();
    await user.tab();
    expect(screen.getByRole('link')).toHaveFocus();
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onSave).toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(<PostCard {...BASE} reactionCount={0} />);
    expect(html).toContain('mn-post-card');
    expect(html).toContain('href="/posts/42"');
  });
});
