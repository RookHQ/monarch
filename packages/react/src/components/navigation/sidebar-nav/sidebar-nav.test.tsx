import { describe, expect, it, vi } from 'vitest';
import { createRef, forwardRef, useState, type AnchorHTMLAttributes } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SidebarNav, type SidebarNavSection } from './index.js';

const Icon = () => <svg aria-hidden width="16" height="16" />;

const SECTIONS: SidebarNavSection[] = [
  {
    key: 'main',
    items: [
      { key: 'home', label: 'Home', icon: <Icon />, href: '/home', exact: true },
      { key: 'inbox', label: 'Inbox', icon: <Icon />, href: '/inbox', badge: 3 },
    ],
  },
  {
    key: 'teams',
    label: 'Teams',
    items: [
      {
        key: 'design',
        label: 'Design',
        icon: <Icon />,
        items: [
          { key: 'design-issues', label: 'Issues', href: '/teams/design/issues' },
          { key: 'design-backlog', label: 'Backlog', href: '/teams/design/backlog' },
        ],
      },
      {
        key: 'help',
        label: 'Help center',
        icon: <Icon />,
        href: 'https://help.example.com',
        external: true,
      },
      { key: 'bad', label: 'Bad link', icon: <Icon />, href: 'javascript:alert(1)' },
    ],
  },
];

describe('SidebarNav', () => {
  it('renders a named landmark with sections and marks the current page', async () => {
    render(<SidebarNav sections={SECTIONS} currentPath="/inbox/42" />);
    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(nav).toHaveClass('mn-sidebar-nav');
    const inbox = screen.getByRole('link', { name: /^Inbox.*3 new$/ });
    expect(inbox).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('list', { name: 'Teams' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('opens a group when it contains the current page, and toggles it with the keyboard', async () => {
    const { rerender } = render(<SidebarNav sections={SECTIONS} currentPath="/home" />);
    const user = userEvent.setup();
    const group = screen.getByRole('button', { name: 'Design' });
    expect(group).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('link', { name: 'Issues' })).not.toBeInTheDocument();
    group.focus();
    await user.keyboard('{Enter}');
    expect(group).toHaveAttribute('aria-expanded', 'true');
    const list = document.getElementById(group.getAttribute('aria-controls')!)!;
    expect(within(list).getByRole('link', { name: 'Issues' })).toBeInTheDocument();
    await user.keyboard(' ');
    expect(group).toHaveAttribute('aria-expanded', 'false');

    rerender(<SidebarNav sections={SECTIONS} currentPath="/teams/design/backlog" />);
    // The person closed it, so their choice wins.
    expect(screen.getByRole('button', { name: 'Design' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('auto-opens the group of the current page on first render', () => {
    render(<SidebarNav sections={SECTIONS} currentPath="/teams/design/backlog" />);
    expect(screen.getByRole('button', { name: 'Design' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('link', { name: 'Backlog' })).toHaveAttribute('aria-current', 'page');
  });

  it('opens external links in a new tab and ignores unsafe URLs', () => {
    render(<SidebarNav sections={SECTIONS} />);
    const help = screen.getByRole('link', { name: 'Help center (opens in a new tab)' });
    expect(help).toHaveAttribute('target', '_blank');
    expect(help).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.queryByRole('link', { name: 'Bad link' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bad link' })).toBeInTheDocument();
  });

  it('uses linkAs for router links and reports navigation', async () => {
    const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function RouterLink({ href, children, onClick, ...props }, ref) {
        return (
          <a
            ref={ref}
            data-router=""
            {...props}
            href={href}
            onClick={(e) => {
              e.preventDefault();
              onClick?.(e);
            }}
          >
            {children}
          </a>
        );
      },
    );
    const onNavigate = vi.fn();
    const onSelect = vi.fn();
    render(
      <SidebarNav
        linkAs={RouterLink}
        onNavigate={onNavigate}
        sections={[
          {
            key: 's',
            items: [
              { key: 'a', label: 'Alpha', href: '/a', onSelect },
              { key: 'b', label: 'Bravo', onSelect },
            ],
          },
        ]}
      />,
    );
    const user = userEvent.setup();
    const alpha = screen.getByRole('link', { name: 'Alpha' });
    expect(alpha).toHaveAttribute('data-router');
    await user.click(alpha);
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onNavigate).toHaveBeenCalledWith(expect.objectContaining({ key: 'a' }));
    await user.click(screen.getByRole('button', { name: 'Bravo' }));
    expect(onSelect).toHaveBeenCalledTimes(2);
  });

  it('draws a quiet count for neutral badges and a solid bubble for accent ones', async () => {
    const sections: SidebarNavSection[] = [
      {
        key: 's',
        items: [
          { key: 'unread', label: 'Inbox', icon: <Icon />, href: '/inbox', badge: 3 },
          {
            key: 'invoices',
            label: 'Invoices',
            icon: <Icon />,
            href: '/invoices',
            badge: 128,
            badgeTone: 'neutral',
          },
          {
            key: 'alerts',
            label: 'Alerts',
            icon: <Icon />,
            href: '/alerts',
            badge: 2,
            badgeTone: 'accent',
            badgeLabel: '2 unresolved',
          },
        ],
      },
    ];
    const { container, rerender } = render(<SidebarNav sections={sections} />);
    const inbox = screen.getByRole('link', { name: /^Inbox.*3 new$/ });
    expect(inbox.querySelector('.mn-sidebar-nav__badge')).toHaveAttribute('data-tone', 'accent');
    const invoices = screen.getByRole('link', { name: /^Invoices.*, 128$/ });
    expect(invoices).not.toHaveAccessibleName(/new/);
    const count = invoices.querySelector('.mn-sidebar-nav__badge');
    expect(count).toHaveAttribute('data-tone', 'neutral');
    expect(count).toHaveTextContent('99+');
    expect(screen.getByRole('link', { name: /^Alerts.*2 unresolved$/ })).toBeInTheDocument();
    await expectNoA11yViolations();

    rerender(<SidebarNav sections={sections} messages={{ count: (n) => `${n} Rechnungen` }} />);
    expect(screen.getByRole('link', { name: /128 Rechnungen/ })).toBeInTheDocument();
    expect(container.querySelectorAll('.mn-sidebar-nav__badge[data-tone]')).toHaveLength(3);
  });

  it('collapses to icons, keeps accessible names, and a group click expands it again', async () => {
    const onCollapsedChange = vi.fn();
    render(
      <SidebarNav
        sections={SECTIONS}
        collapsible
        currentPath="/home"
        onCollapsedChange={onCollapsedChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Collapse sidebar' }));
    expect(onCollapsedChange).toHaveBeenLastCalledWith(true);
    const nav = screen.getByRole('navigation');
    expect(nav).toHaveAttribute('data-collapsed');
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    const design = screen.getByRole('button', { name: 'Design' });
    expect(design).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
    await user.click(design);
    expect(onCollapsedChange).toHaveBeenLastCalledWith(false);
    expect(nav).not.toHaveAttribute('data-collapsed');
    expect(screen.getByRole('button', { name: 'Design' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('shows a tooltip with the label when collapsed and focused', async () => {
    render(<SidebarNav sections={SECTIONS} defaultCollapsed />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Home' })).toHaveFocus();
    expect(await screen.findByRole('tooltip')).toHaveTextContent('Home');
  });

  it('toggles collapse without a controlled/uncontrolled tooltip warning', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    try {
      render(<SidebarNav sections={SECTIONS} collapsible currentPath="/home" />);
      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'Collapse sidebar' }));
      await user.click(screen.getByRole('button', { name: 'Expand sidebar' }));
      await user.click(screen.getByRole('button', { name: 'Collapse sidebar' }));
      expect(error).not.toHaveBeenCalled();
      expect(warn).not.toHaveBeenCalled();
    } finally {
      error.mockRestore();
      warn.mockRestore();
    }
  });

  it('supports controlled collapse, slots, translation and a forwarded ref', () => {
    function Controlled() {
      const [collapsed, setCollapsed] = useState(true);
      return (
        <SidebarNav
          ref={ref}
          sections={SECTIONS}
          collapsible
          collapsed={collapsed}
          onCollapsedChange={setCollapsed}
          header={<span>Brand</span>}
          footer={<span>Footer</span>}
          aria-label="Workspace"
          messages={{ expand: 'Ausklappen', badge: (n) => `${n} ungelesen` }}
        />
      );
    }
    const ref = createRef<HTMLElement>();
    render(<Controlled />);
    expect(screen.getByRole('navigation', { name: 'Workspace' })).toBe(ref.current);
    expect(screen.getByRole('button', { name: 'Ausklappen' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /3 ungelesen/ })).toBeInTheDocument();
    expect(screen.getByText('Brand')).toBeInTheDocument();
    expect(screen.getByText('Footer')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SidebarNav sections={SECTIONS} currentPath="/home" />);
    expect(html).toContain('mn-sidebar-nav');
    expect(html).toContain('aria-current="page"');
  });
});
