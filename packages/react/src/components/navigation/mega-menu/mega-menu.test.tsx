import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import type { AnchorHTMLAttributes } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MegaMenu, type MegaMenuItem } from './index.js';

const items: MegaMenuItem[] = [
  {
    id: 'product',
    label: 'Product',
    sections: [
      {
        heading: 'Platform',
        links: [
          {
            label: 'Analytics',
            href: '/analytics',
            description: 'Dashboards and reports',
            badge: 'New',
          },
          { label: 'Integrations', href: '/integrations', description: 'Connect your tools' },
        ],
      },
      {
        heading: 'Resources',
        links: [{ label: 'Status', href: 'https://status.example.com', external: true }],
      },
    ],
    featured: {
      title: 'What changed this month',
      description: 'A short tour of the newest features.',
      href: '/changelog',
    },
  },
  {
    id: 'company',
    label: 'Company',
    sections: [{ links: [{ label: 'About', href: '/about', current: true }] }],
  },
  { id: 'pricing', label: 'Pricing', href: '/pricing' },
];

const button = (name: string) => screen.getByRole('button', { name });

// React derives pointerenter/leave from pointerover/out; jsdom's events lack pointerType.
function pointer(type: 'pointerover' | 'pointerout', target: Element, pointerType: string) {
  const event = new MouseEvent(type, { bubbles: true, relatedTarget: document.body });
  Object.defineProperty(event, 'pointerType', { value: pointerType });
  fireEvent(target, event);
}
// The test environment reports narrow screens; hover only opens panels in the wide layout.
function wide() {
  return vi.spyOn(window, 'matchMedia').mockImplementation(
    (query: string) =>
      ({
        matches: query.includes('min-width'),
        media: query,
        addEventListener() {},
        removeEventListener() {},
      }) as unknown as MediaQueryList,
  );
}
const hover = (target: Element, pointerType: string) => pointer('pointerover', target, pointerType);
const leave = (target: Element) => pointer('pointerout', target, 'mouse');

describe('MegaMenu', () => {
  it('is a named navigation of disclosure buttons and links', async () => {
    render(<MegaMenu items={items} />);
    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(nav).toHaveClass('mn-mega-menu');
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    expect(button('Company')).toHaveAttribute('aria-expanded', 'false');
    expect(within(nav).getByRole('link', { name: 'Pricing' })).toHaveAttribute('href', '/pricing');
    expect(within(nav).queryByRole('link', { name: /Analytics/ })).not.toBeInTheDocument();
    // No ARIA menu roles: links stay links.
    expect(within(nav).queryByRole('menu')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('opens a panel on click with grouped links and a featured card; Escape closes and returns focus', async () => {
    render(<MegaMenu items={items} />);
    const user = userEvent.setup();
    await user.click(button('Product'));
    expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
    const panel = document.getElementById(button('Product').getAttribute('aria-controls')!)!;
    expect(panel).toBeVisible();
    expect(within(panel).getByRole('list', { name: 'Platform' })).toBeInTheDocument();
    expect(
      within(panel).getByRole('link', { name: /Analytics New Dashboards and reports/ }),
    ).toBeInTheDocument();
    expect(
      within(panel).getByRole('link', { name: 'Status (opens in a new tab)' }),
    ).toHaveAttribute('target', '_blank');
    expect(within(panel).getByRole('link', { name: /What changed this month/ })).toHaveAttribute(
      'href',
      '/changelog',
    );
    await expectNoA11yViolations();
    within(panel)
      .getByRole('link', { name: /Integrations/ })
      .focus();
    await user.keyboard('{Escape}');
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    expect(button('Product')).toHaveFocus();
  });

  it('only one panel is open at a time and a second click closes it', async () => {
    render(<MegaMenu items={items} />);
    const user = userEvent.setup();
    await user.click(button('Product'));
    await user.click(button('Company'));
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    expect(button('Company')).toHaveAttribute('aria-expanded', 'true');
    await user.click(button('Company'));
    expect(button('Company')).toHaveAttribute('aria-expanded', 'false');
  });

  it('arrow keys move between entries and into and through a panel', async () => {
    render(<MegaMenu items={items} />);
    const user = userEvent.setup();
    button('Product').focus();
    await user.keyboard('{ArrowRight}');
    expect(button('Company')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('link', { name: 'Pricing' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(button('Product')).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('link', { name: 'Pricing' })).toHaveFocus();
    await user.keyboard('{Home}{ArrowDown}');
    expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
    await waitFor(() => expect(screen.getByRole('link', { name: /Analytics/ })).toHaveFocus());
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('link', { name: /Integrations/ })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('link', { name: /What changed/ })).toHaveFocus();
    await user.keyboard('{Home}{ArrowUp}');
    expect(button('Product')).toHaveFocus();
  });

  it('closes when focus or a click leaves the navigation', async () => {
    render(
      <>
        <MegaMenu items={items} />
        <button type="button">Outside</button>
      </>,
    );
    const user = userEvent.setup();
    await user.click(button('Product'));
    await user.click(button('Outside'));
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    await user.click(button('Product'));
    screen.getByRole('link', { name: 'Pricing' }).focus();
    // Focus still inside the navigation keeps the panel open.
    expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
    await user.tab();
    expect(button('Outside')).toHaveFocus();
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
  });

  it('opens on mouse hover and a click after hovering keeps it open', async () => {
    vi.useFakeTimers();
    const media = wide();
    try {
      render(<MegaMenu items={items} />);
      const item = button('Product').closest('li')!;
      hover(item, 'mouse');
      expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
      fireEvent.click(button('Product'));
      expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
      leave(item);
      act(() => vi.advanceTimersByTime(200));
      expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
      // Touch never opens on "hover".
      const company = button('Company').closest('li')!;
      hover(company, 'touch');
      expect(button('Company')).toHaveAttribute('aria-expanded', 'false');
    } finally {
      media.mockRestore();
      vi.useRealTimers();
    }
  });

  it('hover-opened panels close shortly after the pointer leaves; hover can be turned off', () => {
    vi.useFakeTimers();
    const media = wide();
    try {
      const { rerender } = render(<MegaMenu items={items} />);
      const item = button('Product').closest('li')!;
      hover(item, 'mouse');
      leave(item);
      expect(button('Product')).toHaveAttribute('aria-expanded', 'true');
      act(() => vi.advanceTimersByTime(200));
      expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
      rerender(<MegaMenu items={items} openOnHover={false} />);
      hover(item, 'mouse');
      expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    } finally {
      media.mockRestore();
      vi.useRealTimers();
    }
  });

  it('does not open on hover in the narrow layout', () => {
    render(<MegaMenu items={items} />);
    hover(button('Product').closest('li')!, 'mouse');
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
  });

  it('the small-screen Menu button toggles the list', async () => {
    render(<MegaMenu items={items} messages={{ menu: 'Navigation' }} />);
    const user = userEvent.setup();
    const toggle = button('Navigation');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAttribute('aria-controls', screen.getAllByRole('list')[0]!.id);
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('navigation')).toHaveAttribute('data-mobile-open');
    await user.keyboard('{Escape}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveFocus();
  });

  it('marks current pages, uses a custom link component and reports navigation', async () => {
    const onNavigate = vi.fn();
    function RouterLink(props: AnchorHTMLAttributes<HTMLAnchorElement>) {
      return (
        <a
          {...props}
          href={props.href}
          data-router=""
          onClick={(e) => {
            e.preventDefault();
            props.onClick?.(e);
          }}
        >
          {props.children}
        </a>
      );
    }
    render(<MegaMenu items={items} linkAs={RouterLink} onNavigate={onNavigate} />);
    const user = userEvent.setup();
    await user.click(button('Company'));
    const about = screen.getByRole('link', { name: 'About' });
    expect(about).toHaveAttribute('aria-current', 'page');
    expect(about).toHaveAttribute('data-router');
    await user.click(about);
    expect(onNavigate).toHaveBeenCalledWith('/about');
    expect(button('Company')).toHaveAttribute('aria-expanded', 'false');
  });

  it('can be controlled', async () => {
    const onOpenItemChange = vi.fn();
    const { rerender } = render(
      <MegaMenu items={items} openItem={null} onOpenItemChange={onOpenItemChange} />,
    );
    const user = userEvent.setup();
    await user.click(button('Product'));
    expect(onOpenItemChange).toHaveBeenCalledWith('product');
    expect(button('Product')).toHaveAttribute('aria-expanded', 'false');
    rerender(<MegaMenu items={items} openItem="company" onOpenItemChange={onOpenItemChange} />);
    expect(button('Company')).toHaveAttribute('aria-expanded', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<MegaMenu items={items} aria-label="Site" />);
    expect(html).toContain('mn-mega-menu');
    expect(html).toContain('aria-label="Site"');
  });
});
