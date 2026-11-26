import { createRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AnnouncementBar } from './index.js';

describe('AnnouncementBar', () => {
  it('is a named region with the message, a link and a close button', async () => {
    render(
      <AnnouncementBar action={{ label: 'Read the notes', href: '/changelog' }}>
        Version 2 is out.
      </AnnouncementBar>,
    );
    const bar = screen.getByRole('region', { name: 'Announcement' });
    expect(bar).toHaveClass('mn-announcement-bar');
    expect(bar).toHaveAttribute('data-tone', 'accent');
    expect(bar).toHaveTextContent('Version 2 is out.');
    expect(screen.getByRole('link', { name: 'Read the notes' })).toHaveAttribute(
      'href',
      '/changelog',
    );
    expect(screen.getByRole('button', { name: 'Dismiss announcement' })).toBeVisible();
    await expectNoA11yViolations();
  });

  it('dismisses with the keyboard (uncontrolled) and reports it for persistence', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(
      <AnnouncementBar action={{ label: 'Details', href: '/x' }} onDismiss={onDismiss}>
        News
      </AnnouncementBar>,
    );
    await user.tab();
    expect(screen.getByRole('link', { name: 'Details' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Dismiss announcement' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('region')).toBeNull();
  });

  it('moves focus to the next control, or to <main>, when dismissed', async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <>
        <AnnouncementBar>Launch</AnnouncementBar>
        <a href="/next">Next link</a>
      </>,
    );
    await user.click(screen.getByRole('button', { name: /dismiss/i }));
    expect(screen.getByRole('link', { name: 'Next link' })).toHaveFocus();
    unmount();

    render(
      <>
        <AnnouncementBar>Launch</AnnouncementBar>
        <main>Content</main>
      </>,
    );
    await user.click(screen.getByRole('button', { name: /dismiss/i }));
    expect(screen.getByRole('main')).toHaveFocus();
    expect(document.activeElement).not.toBe(document.body);
  });

  it('follows the controlled dismissed prop and starts hidden with defaultDismissed', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [hidden, setHidden] = useState(false);
      return (
        <>
          <AnnouncementBar dismissed={hidden} onDismiss={() => setHidden(true)}>
            News
          </AnnouncementBar>
          <button type="button" onClick={() => setHidden(false)}>
            Show again
          </button>
        </>
      );
    }
    render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Dismiss announcement' }));
    expect(screen.queryByRole('region')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Show again' }));
    expect(screen.getByRole('region')).toBeVisible();
    const { container } = render(<AnnouncementBar defaultDismissed>Hidden</AnnouncementBar>);
    expect(container).toBeEmptyDOMElement();
  });

  it('drops unsafe links, marks new-tab links and accepts an action slot', () => {
    const { rerender } = render(
      <AnnouncementBar action={{ label: 'Bad', href: 'javascript:alert(1)' }}>
        News
      </AnnouncementBar>,
    );
    expect(screen.queryByRole('link')).toBeNull();
    rerender(
      <AnnouncementBar action={{ label: 'Docs', href: 'https://example.com', external: true }}>
        News
      </AnnouncementBar>,
    );
    const link = screen.getByRole('link', { name: 'Docs (opens in a new tab)' });
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    rerender(<AnnouncementBar actionSlot={<a href="/custom">Custom</a>}>News</AnnouncementBar>);
    expect(screen.getByRole('link', { name: 'Custom' })).toBeVisible();
  });

  it('supports tone, sticky, no close button, translations and ref forwarding', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AnnouncementBar
        ref={ref}
        tone="warning"
        sticky
        dismissible={false}
        messages={{ label: 'Aviso' }}
      >
        Mantenimiento programado.
      </AnnouncementBar>,
    );
    const bar = screen.getByRole('region', { name: 'Aviso' });
    expect(bar).toBe(ref.current);
    expect(bar).toHaveAttribute('data-tone', 'warning');
    expect(bar).toHaveAttribute('data-sticky');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<AnnouncementBar>Server news</AnnouncementBar>);
    expect(html).toContain('mn-announcement-bar');
    expect(html).toContain('Server news');
  });
});
