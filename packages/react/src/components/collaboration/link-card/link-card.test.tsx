import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  LinkCard,
  LinkCardList,
  LinkCardSkeleton,
  linkCardImageRatio,
  type LinkCardPreview,
} from './index.js';

const PREVIEW: LinkCardPreview = {
  url: 'https://example.com/guide',
  title: 'Getting started guide',
  description: 'Everything you need for the first week.',
  siteName: 'Example Docs',
  image: { src: 'https://example.com/cover.png', width: 1200, height: 630 },
};

describe('linkCardImageRatio', () => {
  it('keeps the ratio between 1.2 and 2.2', () => {
    expect(linkCardImageRatio(1200, 630)).toBeCloseTo(1.905, 2);
    expect(linkCardImageRatio(100, 100)).toBe(1.2);
    expect(linkCardImageRatio(1000, 100)).toBe(2.2);
    expect(linkCardImageRatio()).toBe(2.2);
  });
});

describe('LinkCard', () => {
  it('is one link named by its title and description that opens a new tab', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<LinkCard ref={ref} {...PREVIEW} />);
    expect(ref.current).toHaveClass('mn-link-card');
    const link = screen.getByRole('link', { name: /Getting started guide/ });
    expect(link).toHaveAttribute('href', 'https://example.com/guide');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(link).toHaveAccessibleName(/opens in a new tab/);
    expect(screen.getByText('Example Docs')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('reserves the picture space while loading and drops it on error', () => {
    const { container } = render(<LinkCard {...PREVIEW} video />);
    const media = container.querySelector('.mn-link-card__media') as HTMLElement;
    expect(media).toHaveAttribute('data-state', 'loading');
    expect(media.style.aspectRatio).not.toBe('');
    const img = container.querySelector('img.mn-link-card__image') as HTMLImageElement;
    expect(img).toHaveAttribute('alt', '');
    fireEvent.load(img);
    expect(media).toHaveAttribute('data-state', 'loaded');
    expect(screen.getByRole('link')).toHaveAccessibleName(/Video/);
    fireEvent.error(img);
    expect(container.querySelector('.mn-link-card__media')).toBeNull();
  });

  it('falls back to the host name and never links unsafe URLs or images', () => {
    render(
      <LinkCard
        url="javascript:alert(1)"
        title="Odd link"
        image={{ src: 'javascript:alert(1)' }}
      />,
    );
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByText('Odd link')).toBeInTheDocument();
    expect(document.querySelector('img.mn-link-card__image')).toBeNull();

    render(<LinkCard url="https://news.example.org/a" title="Story" newTab={false} rel="" />);
    const link = screen.getByRole('link', { name: 'news.example.org Story' });
    expect(link).not.toHaveAttribute('target');
    expect(link).not.toHaveAttribute('rel');
  });

  it('removes the preview with a named button reachable by keyboard', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(
      <LinkCard {...PREVIEW} onDismiss={onDismiss} messages={{ dismiss: (t) => `Hide ${t}` }} />,
    );
    await user.tab();
    expect(screen.getByRole('link')).toHaveFocus();
    await user.tab();
    const button = screen.getByRole('button', { name: 'Hide Getting started guide' });
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });
});

describe('LinkCardList and LinkCardSkeleton', () => {
  it('stacks cards, passes the URL to onDismiss and shows a skeleton while pending', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    const { container } = render(
      <LinkCardList
        previews={[PREVIEW, { url: 'https://example.net/', title: 'Second' }]}
        size="sm"
        pending
        onDismiss={onDismiss}
      />,
    );
    expect(screen.getAllByRole('link')).toHaveLength(2);
    expect(container.querySelector('.mn-link-card--skeleton')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Remove the preview of Second' }));
    expect(onDismiss).toHaveBeenCalledWith('https://example.net/');
    await expectNoA11yViolations();
  });

  it('renders nothing without previews', () => {
    const { container } = render(<LinkCardList previews={[]} />);
    expect(container).toBeEmptyDOMElement();
    render(<LinkCardSkeleton data-testid="sk" size="sm" />);
    expect(screen.getByTestId('sk')).toHaveAttribute('data-size', 'sm');
  });

  it('renders on the server', () => {
    const html = renderToString(<LinkCardList previews={[PREVIEW]} pending />);
    expect(html).toContain('mn-link-card');
    expect(html).toContain('href="https://example.com/guide"');
  });
});
