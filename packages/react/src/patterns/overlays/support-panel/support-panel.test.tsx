import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  SupportPanel,
  type SupportChatClient,
  type SupportChatEvent,
  type SupportChatMessage,
  type SupportPanelProps,
} from './index.js';

const time = () => '10:00';

function fakeClient(over: Partial<SupportChatClient> = {}) {
  let listener: ((e: SupportChatEvent) => void) | null = null;
  let n = 0;
  const client = {
    connect: vi.fn(async () => ({
      messages: [
        {
          id: 'm1',
          author: 'agent' as const,
          authorName: 'Priya',
          text: 'Hi! How can we help?',
          createdAt: '2026-11-04T10:00:00Z',
        },
      ],
      availability: { online: true, expectedReply: 'Typically replies in a few minutes' },
    })),
    send: vi.fn(async (text: string): Promise<SupportChatMessage> => ({
      id: `s${++n}`,
      author: 'visitor',
      text,
      createdAt: '2026-11-04T10:01:00Z',
    })),
    subscribe: vi.fn((fn: (e: SupportChatEvent) => void) => {
      listener = fn;
      return () => {
        listener = null;
      };
    }),
    markRead: vi.fn(),
    ...over,
  };
  return { client, emit: (e: SupportChatEvent) => act(() => listener?.(e)) };
}

function Harness(props: Partial<SupportPanelProps>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" aria-expanded={open} onClick={() => setOpen(true)}>
        Help &amp; support
      </button>
      <SupportPanel formatTime={time} {...props} open={open} onOpenChange={setOpen} />
    </>
  );
}

describe('SupportPanel', () => {
  it('connects on first open (never before) and shows the conversation', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient();
    render(<Harness client={client} />);
    expect(client.connect).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    const dialog = screen.getByRole('dialog', { name: 'Help & support' });
    expect(dialog).toHaveAccessibleDescription('Chat with the support team.');
    expect(client.connect).toHaveBeenCalledTimes(1);
    const log = await within(dialog).findByRole('log', { name: 'Conversation with support' });
    expect(within(log).getByText('Hi! How can we help?')).toBeInTheDocument();
    expect(within(log).getByText('Priya')).toBeInTheDocument();
    expect(within(dialog).getByText('Typically replies in a few minutes')).toBeInTheDocument();
    await waitFor(() => expect(client.markRead).toHaveBeenCalledWith('m1'));
    await expectNoA11yViolations();
  });

  it('sends with Enter (Shift+Enter is a new line), optimistic then stored', async () => {
    const user = userEvent.setup();
    let release: () => void = () => {};
    const { client } = fakeClient();
    vi.mocked(client.send).mockImplementationOnce(
      (text: string) =>
        new Promise<SupportChatMessage>((r) => {
          release = () =>
            r({ id: 's9', author: 'visitor', text, createdAt: '2026-11-04T10:01:00Z' });
        }),
    );
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    const box = await screen.findByRole('textbox', { name: 'Message' });
    await user.type(box, 'Line one{Shift>}{Enter}{/Shift}line two');
    expect(box).toHaveValue('Line one\nline two');
    await user.keyboard('{Enter}');
    expect(client.send).toHaveBeenCalledWith('Line one\nline two', {
      clientMessageId: expect.stringMatching(/^c_/),
    });
    expect(box).toHaveValue('');
    expect(screen.getByText('Sending…')).toBeInTheDocument();
    act(() => release());
    await waitFor(() => expect(screen.queryByText('Sending…')).toBeNull());
    expect(screen.getByRole('log')).toHaveTextContent('Line one');
  });

  it('marks a failed send and retries it with the same client id', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient();
    vi.mocked(client.send).mockRejectedValueOnce(new Error('offline'));
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Hello{Enter}');
    expect(await screen.findByText('Not sent.')).toBeInTheDocument();
    const firstId = vi.mocked(client.send).mock.calls[0]?.[1].clientMessageId;
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(client.send).toHaveBeenCalledTimes(2));
    expect(vi.mocked(client.send).mock.calls[1]?.[1].clientMessageId).toBe(firstId);
    await waitFor(() => expect(screen.queryByText('Not sent.')).toBeNull());
  });

  it.each([
    ['echoes the client id', true],
    ['does not echo the client id', false],
  ])(
    'shows a message once when the live event arrives before send resolves (API %s)',
    async (_label, echo) => {
      const user = userEvent.setup();
      let release: () => void = () => {};
      const { client, emit } = fakeClient();
      vi.mocked(client.send).mockImplementationOnce(
        (text: string, { clientMessageId }) =>
          new Promise<SupportChatMessage>((r) => {
            release = () =>
              r({
                id: 's7',
                author: 'visitor',
                text,
                createdAt: '2026-11-04T10:01:00Z',
                clientMessageId,
              });
          }),
      );
      render(<Harness client={client} />);
      await user.click(screen.getByRole('button', { name: 'Help & support' }));
      await user.type(
        await screen.findByRole('textbox', { name: 'Message' }),
        'Hello there{Enter}',
      );
      const cid = vi.mocked(client.send).mock.calls[0]?.[1].clientMessageId;
      emit({
        type: 'message',
        message: {
          id: 's7',
          author: 'visitor',
          text: 'Hello there',
          createdAt: '2026-11-04T10:01:00Z',
          ...(echo ? { clientMessageId: cid } : {}),
        },
      });
      const log = screen.getByRole('log');
      expect(within(log).getAllByText('Hello there')).toHaveLength(1);
      expect(screen.queryByText('Sending…')).toBeNull();
      act(() => release());
      await waitFor(() => expect(within(log).getAllByText('Hello there')).toHaveLength(1));
    },
  );

  it('does not offer a retry for a message the live stream shows as stored', async () => {
    const user = userEvent.setup();
    let fail: () => void = () => {};
    const { client, emit } = fakeClient();
    vi.mocked(client.send).mockImplementationOnce(
      () =>
        new Promise<SupportChatMessage>((_r, reject) => {
          fail = () => reject(new Error('timeout'));
        }),
    );
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Saved{Enter}');
    const cid = vi.mocked(client.send).mock.calls[0]?.[1].clientMessageId;
    emit({
      type: 'message',
      message: {
        id: 's8',
        author: 'visitor',
        text: 'Saved',
        createdAt: '2026-11-04T10:01:00Z',
        clientMessageId: cid,
      },
    });
    await act(async () => fail());
    expect(screen.queryByText('Not sent.')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Try again' })).toBeNull();
    expect(client.send).toHaveBeenCalledTimes(1);
  });

  it('follows live events: replies, typing, availability and an ended session', async () => {
    const user = userEvent.setup();
    const { client, emit } = fakeClient();
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    await screen.findByRole('log');
    emit({ type: 'typing', typing: true, name: 'Priya' });
    expect(screen.getByText('Priya is typing…')).toBeInTheDocument();
    emit({
      type: 'message',
      message: {
        id: 'm2',
        author: 'agent',
        authorName: 'Priya',
        text: 'On it.',
        createdAt: '2026-11-04T10:02:00Z',
      },
    });
    emit({
      type: 'message',
      message: {
        id: 'm2',
        author: 'agent',
        authorName: 'Priya',
        text: 'On it.',
        createdAt: '2026-11-04T10:02:00Z',
      },
    });
    expect(screen.getAllByText('On it.')).toHaveLength(1);
    expect(screen.queryByText('Priya is typing…')).toBeNull();
    emit({ type: 'availability', availability: { online: false } });
    expect(screen.getByText(/The team is away/)).toBeInTheDocument();
    emit({ type: 'ended', reason: 'session' });
    expect(screen.getByRole('alert')).toHaveTextContent('Your session ended');
  });

  it('keeps an unsent message below the history after reconnecting, and lists a stored one once', async () => {
    const user = userEvent.setup();
    const { client, emit } = fakeClient();
    vi.mocked(client.send).mockRejectedValueOnce(new Error('offline'));
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Hello{Enter}');
    expect(await screen.findByText('Not sent.')).toBeInTheDocument();
    await emit({ type: 'ended', reason: 'session' });
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    const log = await screen.findByRole('log');
    const texts = () =>
      within(log)
        .getAllByRole('listitem')
        .map((li) => li.querySelector('.mn-support-panel__bubble')?.textContent);
    expect(texts()).toEqual(['Hi! How can we help?', 'Hello']);

    // The server did store it (the reply was lost): the next snapshot carries the client id.
    const clientMessageId = vi.mocked(client.send).mock.calls[0]?.[1].clientMessageId;
    vi.mocked(client.connect).mockResolvedValueOnce({
      messages: [
        {
          id: 'm1',
          author: 'agent',
          text: 'Hi! How can we help?',
          createdAt: '2026-11-04T10:00:00Z',
        },
        {
          id: 's9',
          author: 'visitor',
          text: 'Hello',
          createdAt: '2026-11-04T10:01:00Z',
          clientMessageId,
        },
      ],
      // The fake client's snapshot type is narrower than the real one.
    } as never);
    await emit({ type: 'ended', reason: 'session' });
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    const again = await screen.findByRole('log');
    expect(within(again).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.queryByText('Not sent.')).toBeNull();
  });

  it('shows an address that would run script as text, not as a link', async () => {
    const user = userEvent.setup();
    render(
      <Harness
        contacts={[
          { label: 'Call us', href: 'tel:+15551234567' },
          { label: 'Bad contact', href: 'java\nscript:alert(1)' },
        ]}
        helpHref="javascript:alert(1)"
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    expect(await screen.findByRole('link', { name: /Call us/ })).toHaveAttribute(
      'href',
      'tel:+15551234567',
    );
    expect(screen.getByText('Bad contact').closest('a')).toBeNull();
    expect(screen.getByText('Help Center').closest('a')).toBeNull();
  });

  it('scrolls to the newest message when the Chat tab is shown again', async () => {
    const user = userEvent.setup();
    const { client, emit } = fakeClient();
    render(<Harness client={client} articles={<p>Articles</p>} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    const log = await screen.findByRole('log');
    await user.click(screen.getByRole('tab', { name: 'Help' }));
    await emit({
      type: 'message',
      message: {
        id: 'm2',
        author: 'agent',
        text: 'Still there?',
        createdAt: '2026-11-04T10:02:00Z',
      },
    });
    Object.defineProperty(log, 'scrollHeight', { configurable: true, value: 480 });
    log.scrollTop = 0;
    await user.click(screen.getByRole('tab', { name: 'Chat' }));
    expect(log.scrollTop).toBe(480);
  });

  it('times out with Try again, which connects again', async () => {
    // A simulated clock, and synchronous clicks (user-event waits on timers): a real
    // short timeout races the click on a slow machine.
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    try {
      const { client } = fakeClient();
      vi.mocked(client.connect).mockImplementationOnce(() => new Promise<never>(() => {}));
      render(<Harness client={client} connectTimeout={10_000} />);
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Help & support' }));
      });
      expect(screen.getByRole('status')).toHaveTextContent('Connecting to support…');
      expect(screen.queryByRole('alert')).toBeNull();
      await act(async () => {
        vi.advanceTimersByTime(10_000);
      });
      expect(screen.getByRole('alert')).toHaveTextContent('taking too long');
      expect(vi.mocked(client.connect).mock.calls[0]?.[0]?.signal.aborted).toBe(true);
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
      });
      await act(async () => {
        vi.advanceTimersByTime(0);
      });
      expect(screen.getByRole('log')).toBeInTheDocument();
      expect(client.connect).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows a load failure', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient({ connect: vi.fn(() => Promise.reject(new Error('503'))) });
    render(<Harness client={client} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('We can’t load chat right now.');
  });

  it('keeps the conversation and the draft across close and reopen; focus returns to the trigger', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient();
    render(<Harness client={client} />);
    const trigger = screen.getByRole('button', { name: 'Help & support' });
    await user.click(trigger);
    await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Half a thought');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    await waitFor(() => expect(trigger).toHaveFocus());
    await user.click(trigger);
    expect(await screen.findByRole('textbox', { name: 'Message' })).toHaveValue('Half a thought');
    expect(screen.getByText('Hi! How can we help?')).toBeInTheDocument();
    expect(client.connect).toHaveBeenCalledTimes(1);
  });

  it('has Chat and Help tabs with articles, contacts and the status link', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient();
    render(
      <Harness
        client={client}
        articles={<a href="https://help.example.com/start">Getting started</a>}
        contacts={[
          {
            label: 'Email support',
            href: 'mailto:support@example.com',
            description: 'support@example.com',
          },
        ]}
        status={{
          href: 'https://status.example.com',
          tone: 'operational',
          text: 'All systems normal',
        }}
        helpHref="https://help.example.com"
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    const dialog = screen.getByRole('dialog');
    const chatTab = within(dialog).getByRole('tab', { name: 'Chat' });
    expect(chatTab).toHaveAttribute('aria-selected', 'true');
    const status = within(dialog).getByRole('link', { name: /System status: All systems normal/ });
    expect(status).toHaveAttribute('target', '_blank');
    expect(status).toHaveAccessibleName('System status: All systems normal (opens in a new tab)');
    expect(within(dialog).getByRole('link', { name: /Help Center/ })).toHaveAttribute(
      'href',
      'https://help.example.com',
    );

    chatTab.focus();
    await user.keyboard('{ArrowRight}');
    const help = within(dialog).getByRole('tab', { name: 'Help' });
    expect(help).toHaveAttribute('aria-selected', 'true');
    expect(within(dialog).getByRole('heading', { name: 'Help articles' })).toBeInTheDocument();
    expect(within(dialog).getByRole('link', { name: 'Getting started' })).toBeInTheDocument();
    const email = within(dialog).getByRole('link', { name: /Email support/ });
    expect(email).toHaveAttribute('href', 'mailto:support@example.com');
    expect(email).not.toHaveAttribute('target');
    await expectNoA11yViolations();
  });

  it('takes the product’s own chat surface, and works with help only', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<Harness chat={<div>Widget host</div>} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    expect(screen.getByText('Widget host')).toBeInTheDocument();
    expect(screen.queryByRole('tab')).toBeNull();
    rerender(<Harness contacts={[{ label: 'Call us', href: 'tel:+10000000000' }]} />);
    expect(screen.getByRole('link', { name: /Call us/ })).toBeInTheDocument();
  });

  it('is uncontrolled with defaultOpen, takes an id and an inset, and translates', () => {
    render(
      <SupportPanel
        defaultOpen
        id="support"
        side="left"
        inset="var(--mn-layout-rail-collapsed)"
        contacts={[{ label: 'Email', href: 'mailto:a@example.com' }]}
        messages={{ title: 'Aide', description: 'Écrivez-nous.' }}
      />,
    );
    const dialog = screen.getByRole('dialog', { name: 'Aide' });
    expect(dialog).toHaveAttribute('id', 'support');
    expect(dialog).toHaveAttribute('data-side', 'left');
    expect(dialog.style.getPropertyValue('--mn-support-panel-inset')).toBe(
      'var(--mn-layout-rail-collapsed)',
    );
  });

  it('renders on the server (closed: nothing in the document)', () => {
    const html = renderToString(<SupportPanel client={fakeClient().client} />);
    expect(html).toBe('');
  });
});
