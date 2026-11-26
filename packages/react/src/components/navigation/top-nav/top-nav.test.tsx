import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRef, forwardRef, type AnchorHTMLAttributes } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TopNav, type TopNavItem } from './index.js';
import { fitItems } from '../../../internal/overflow.js';

const ITEMS: TopNavItem[] = [
  { key: 'overview', label: 'Overview', href: '/', exact: true },
  { key: 'projects', label: 'Projects', href: '/projects', badge: 2 },
  { key: 'reports', label: 'Reports', href: '/reports' },
  { key: 'people', label: 'People', href: '/people' },
  { key: 'docs', label: 'Docs', href: 'https://docs.example.com', external: true },
];

/** Gives the bar a width and every measured item a width, as a browser would. */
function mockLayout(barWidth: number, itemWidth = 100, moreWidth = 60) {
  const cw = vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get');
  cw.mockImplementation(function (this: HTMLElement) {
    return this.classList.contains('mn-top-nav__bar') ? barWidth : 0;
  });
  const rect = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect');
  rect.mockImplementation(function (this: HTMLElement) {
    const inMeasure = this.parentElement?.classList.contains('mn-top-nav__measure');
    const width = !inMeasure
      ? 0
      : this.classList.contains('mn-top-nav__more')
        ? moreWidth
        : itemWidth;
    return {
      width,
      height: 36,
      top: 0,
      left: 0,
      right: width,
      bottom: 36,
      x: 0,
      y: 0,
      toJSON() {},
    };
  });
}

describe('fitItems', () => {
  it('keeps everything when it fits, else cuts in order leaving room for More', () => {
    expect(fitItems([100, 100, 100], 300, 60, 0)).toEqual([0, 1, 2]);
    expect(fitItems([100, 100, 100, 100], 300, 60, 0)).toEqual([0, 1]);
    expect(fitItems([100, 100, 100, 100], 300, 60, 0, 3)).toEqual([0, 3]);
    expect(fitItems([100, 100, 100], 320, 60, 10)).toEqual([0, 1, 2]);
    expect(fitItems([100, 100, 100, 100], 100, 60, 0)).toEqual([]);
  });
});

describe('TopNav', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders every link in a named navigation and marks the current page', async () => {
    render(<TopNav items={ITEMS} currentPath="/reports/q3" start={<span>Brand</span>} />);
    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(nav).toHaveClass('mn-top-nav');
    expect(screen.getByRole('link', { name: 'Reports' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: /^Projects.*2 new$/ })).toBeInTheDocument();
    const docs = screen.getByRole('link', { name: 'Docs (opens in a new tab)' });
    expect(docs).toHaveAttribute('target', '_blank');
    expect(screen.queryByRole('button', { name: 'More' })).not.toBeInTheDocument();
    expect(screen.getByText('Brand')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  describe('overflow', () => {
    beforeEach(() => mockLayout(300));

    it('moves links that do not fit into a More menu, reachable by keyboard', async () => {
      render(<TopNav items={ITEMS} currentPath="/" />);
      const list = screen.getByRole('navigation').querySelector('.mn-top-nav__list')!;
      expect(
        within(list as HTMLElement)
          .getAllByRole('link')
          .map((l) => l.textContent),
      ).toEqual(['Overview', 'Projects2, 2 new']);
      const user = userEvent.setup();
      await user.tab();
      await user.tab();
      await user.tab();
      const more = screen.getByRole('button', { name: 'More' });
      expect(more).toHaveFocus();
      expect(more).toHaveAttribute('aria-haspopup', 'menu');
      await user.keyboard('{Enter}');
      const menu = await screen.findByRole('menu');
      const entries = within(menu).getAllByRole('menuitem');
      expect(entries.map((e) => e.textContent)).toEqual([
        'Reports',
        'People',
        'Docs (opens in a new tab)',
      ]);
      expect(entries[0]).toHaveFocus();
      await user.keyboard('{ArrowDown}');
      expect(entries[1]).toHaveFocus();
      await user.keyboard('{Escape}');
      expect(screen.queryByRole('menu')).not.toBeInTheDocument();
      expect(more).toHaveFocus();
    });

    it('keeps the current page visible', () => {
      render(<TopNav items={ITEMS} currentPath="/people" />);
      const list = screen.getByRole('navigation').querySelector('.mn-top-nav__list')!;
      expect(
        within(list as HTMLElement)
          .getAllByRole('link')
          .map((l) => l.textContent),
      ).toEqual(['Overview', 'People']);
      expect(screen.getByRole('link', { name: 'People' })).toHaveAttribute('aria-current', 'page');
    });

    it('calls onSelect and onNavigate for menu entries', async () => {
      const onNavigate = vi.fn();
      const onSelect = vi.fn();
      render(
        <TopNav
          onNavigate={onNavigate}
          items={[...ITEMS.slice(0, 3), { key: 'act', label: 'Action', onSelect }]}
          messages={{ more: 'Mehr' }}
        />,
      );
      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'Mehr' }));
      await user.click(await screen.findByRole('menuitem', { name: 'Action' }));
      expect(onSelect).toHaveBeenCalledTimes(1);
      expect(onNavigate).toHaveBeenCalledWith(expect.objectContaining({ key: 'act' }));
    });
  });

  it('uses linkAs, forwards its ref and ignores unsafe URLs', async () => {
    const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function RouterLink({ href, children, ...props }, ref) {
        return (
          <a ref={ref} data-router="" {...props} href={href}>
            {children}
          </a>
        );
      },
    );
    const ref = createRef<HTMLElement>();
    const onNavigate = vi.fn();
    render(
      <TopNav
        ref={ref}
        aria-label="Site"
        linkAs={RouterLink}
        onNavigate={onNavigate}
        end={<button type="button">Sign in</button>}
        items={[
          { key: 'a', label: 'Alpha', href: '#a', active: true },
          { key: 'b', label: 'Bravo', href: 'javascript:void(0)' },
        ]}
      />,
    );
    expect(screen.getByRole('navigation', { name: 'Site' })).toBe(ref.current);
    const alpha = screen.getByRole('link', { name: 'Alpha' });
    expect(alpha).toHaveAttribute('data-router');
    expect(alpha).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('button', { name: 'Bravo' })).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Bravo' }));
    expect(onNavigate).toHaveBeenCalledWith(expect.objectContaining({ key: 'b' }));
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('renders on the server with every item visible', () => {
    const html = renderToString(<TopNav items={ITEMS} currentPath="/" />);
    expect(html).toContain('mn-top-nav');
    expect(html).toContain('aria-current="page"');
    expect(html).not.toContain('aria-haspopup');
  });
});
