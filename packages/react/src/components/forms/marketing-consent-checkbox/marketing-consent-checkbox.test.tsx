import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MarketingConsentCheckbox } from './index.js';

describe('MarketingConsentCheckbox', () => {
  it('is an unticked email checkbox with its notes as the description', async () => {
    render(
      <MarketingConsentCheckbox
        data-testid="root"
        description="Product updates, about twice a month."
        policyLink={{ label: 'How we use your data', href: '/privacy' }}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-marketing-consent-checkbox');
    const box = screen.getByRole('checkbox', { name: 'Send me product news by email' });
    expect(box).not.toBeChecked();
    expect(box).toHaveAccessibleDescription(
      'Product updates, about twice a month. You can unsubscribe at any time. How we use your data',
    );
    expect(screen.getByRole('link', { name: 'How we use your data' })).toHaveAttribute(
      'href',
      '/privacy',
    );
    await expectNoA11yViolations();
  });

  it('is never pre-ticked: ignores defaultChecked and warns once in development', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { unmount } = render(<MarketingConsentCheckbox defaultChecked />);
    expect(screen.getByRole('checkbox')).not.toBeChecked();
    unmount();
    render(<MarketingConsentCheckbox defaultChecked channels={['email', 'sms']} />);
    for (const box of screen.getAllByRole('checkbox')) expect(box).not.toBeChecked();
    expect(warn).toHaveBeenCalledOnce();
    expect(warn.mock.calls[0]?.[0]).toMatch(/defaultChecked/);
    warn.mockRestore();
  });

  it('reports the channels, the exact wording shown and the time', async () => {
    const onChange = vi.fn();
    render(
      <MarketingConsentCheckbox
        description="Product updates, about twice a month."
        onChange={onChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox'));
    expect(onChange).toHaveBeenCalledWith({
      channels: { email: true },
      text: 'Send me product news by email Product updates, about twice a month. You can unsubscribe at any time.',
      at: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
    });
    await user.keyboard(' ');
    expect(onChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ channels: { email: false } }),
    );
  });

  it('shows one separately unticked checkbox per channel in a labelled group', async () => {
    const onChange = vi.fn();
    render(
      <MarketingConsentCheckbox
        channels={['email', 'sms', 'post']}
        description="Offers and event invitations, at most once a week."
        unsubscribeNote={null}
        onChange={onChange}
      />,
    );
    const group = screen.getByRole('group', { name: 'Send me product news' });
    expect(group).toHaveAccessibleDescription('Offers and event invitations, at most once a week.');
    const boxes = screen.getAllByRole('checkbox');
    expect(boxes.map((b) => b.getAttribute('aria-checked'))).toEqual(['false', 'false', 'false']);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Text message (SMS)' }));
    expect(onChange).toHaveBeenLastCalledWith(
      expect.objectContaining({
        channels: { email: false, sms: true, post: false },
        text: 'Send me product news Offers and event invitations, at most once a week. Email Text message (SMS) Post',
      }),
    );
    expect(screen.getByRole('checkbox', { name: 'Email' })).not.toBeChecked();
    await user.tab();
    expect(screen.getByRole('checkbox', { name: 'Post' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('posts ticked channels and the wording with a plain form', async () => {
    render(
      <form aria-label="Contact">
        <MarketingConsentCheckbox
          name="marketing"
          channels={['email', 'sms']}
          label="Keep me posted"
          policyLink={{ label: 'Privacy notice', href: '/privacy', target: '_blank' }}
        />
      </form>,
    );
    const form = screen.getByRole('form') as HTMLFormElement;
    let data = new FormData(form);
    expect(data.getAll('marketing')).toEqual([]);
    expect(data.get('marketing_text')).toBe(
      'Keep me posted You can unsubscribe at any time. Privacy notice Email Text message (SMS)',
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Email' }));
    await user.click(screen.getByRole('checkbox', { name: 'Text message (SMS)' }));
    data = new FormData(form);
    expect(data.getAll('marketing')).toEqual(['email', 'sms']);
    await user.click(screen.getByRole('checkbox', { name: 'Email' }));
    expect(new FormData(form).getAll('marketing')).toEqual(['sms']);
    form.reset();
    await screen.findAllByRole('checkbox');
    expect(new FormData(form).getAll('marketing')).toEqual([]);
  });

  it('can be controlled to show an earlier choice, custom text name, and disabled', () => {
    const { rerender } = render(
      <form aria-label="Settings">
        <MarketingConsentCheckbox
          name="news"
          textName="news_wording"
          channels={['email', 'phone']}
          channelLabels={{ phone: 'Phone calls' }}
          checked={{ email: true }}
        />
      </form>,
    );
    expect(screen.getByRole('checkbox', { name: 'Email' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Phone calls' })).not.toBeChecked();
    const form = screen.getByRole('form') as HTMLFormElement;
    expect(new FormData(form).getAll('news')).toEqual(['email']);
    expect(new FormData(form).get('news_wording')).toContain('Phone calls');
    rerender(
      <form aria-label="Settings">
        <MarketingConsentCheckbox channels={['sms']} disabled />
      </form>,
    );
    expect(
      screen.getByRole('checkbox', { name: 'Send me product news by text message' }),
    ).toBeDisabled();
  });

  it('renders on the server, unticked', () => {
    const html = renderToString(<MarketingConsentCheckbox channels={['email', 'post']} />);
    expect(html).toContain('mn-marketing-consent-checkbox');
    expect(html).toContain('<fieldset');
    expect(html).not.toContain('aria-checked="true"');
  });
});
