import { describe, expect, it, vi } from 'vitest';
import { createRef, forwardRef, type AnchorHTMLAttributes } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BottomTabBar, type BottomTabBarItem } from './index.js';

const Icon = ({ name }: { name: string }) => <svg data-icon={name} width="20" height="20" />;

const ITEMS: BottomTabBarItem[] = [
  { key: 'home', label: 'Home', icon: <Icon name="home" />, href: '/', exact: true },
  {
    key: 'inbox',
    label: 'Inbox',
    icon: <Icon name="inbox" />,
    activeIcon: <Icon name="inbox-filled" />,
    href: '/inbox',
    badge: 120,
  },
  { key: 'search', label: 'Search', icon: <Icon name="search" />, href: '/search' },
];

describe('BottomTabBar', () => {
  it('renders a named navigation of links and marks the current one', async () => {
    const { container } = render(<BottomTabBar items={ITEMS} currentPath="/inbox/7" />);
    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(nav).toHaveClass('mn-bottom-tab-bar');
    expect(nav).toHaveAttribute('data-position', 'fixed');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    const inbox = screen.getByRole('link', { name: /^Inbox.*120 new$/ });
    expect(inbox).toHaveAttribute('aria-current', 'page');
    expect(inbox).toHaveTextContent('99+');
    expect(container.querySelector('[data-icon="inbox-filled"]')).not.toBeNull();
    expect(container.querySelector('[data-icon="inbox"]')).toBeNull();
    expect(screen.getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
    await expectNoA11yViolations();
  });

  it('moves between tabs with Tab and supports button tabs for actions', async () => {
    const onSelect = vi.fn();
    const onNavigate = vi.fn();
    render(
      <BottomTabBar
        items={[...ITEMS, { key: 'more', label: 'More', icon: <Icon name="more" />, onSelect }]}
        onNavigate={onNavigate}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Home' })).toHaveFocus();
    await user.tab();
    await user.tab();
    await user.tab();
    const more = screen.getByRole('button', { name: 'More' });
    expect(more).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onNavigate).toHaveBeenCalledWith(expect.objectContaining({ key: 'more' }));
  });

  it('uses linkAs, explicit active, options and translations; drops unsafe URLs', () => {
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
    render(
      <BottomTabBar
        ref={ref}
        linkAs={RouterLink}
        position="static"
        hideFrom="md"
        aria-label="Sections"
        messages={{ badge: (n) => `${n} ungelesen` }}
        items={[
          { key: 'a', label: 'Alpha', icon: <Icon name="a" />, href: '/a', active: true, badge: 2 },
          { key: 'b', label: 'Bravo', icon: <Icon name="b" />, href: 'javascript:void(0)' },
        ]}
      />,
    );
    const nav = screen.getByRole('navigation', { name: 'Sections' });
    expect(nav).toBe(ref.current);
    expect(nav).toHaveAttribute('data-position', 'static');
    expect(nav).toHaveAttribute('data-hide-from', 'md');
    const alpha = screen.getByRole('link', { name: /Alpha.*2 ungelesen/ });
    expect(alpha).toHaveAttribute('data-router');
    expect(alpha).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('button', { name: 'Bravo' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<BottomTabBar items={ITEMS} currentPath="/" />);
    expect(html).toContain('mn-bottom-tab-bar');
    expect(html).toContain('aria-current="page"');
  });
});
