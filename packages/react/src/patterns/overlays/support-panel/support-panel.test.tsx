import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
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

  it('times out with Try again, which connects again', async () => {
    const user = userEvent.setup();
    const { client } = fakeClient();
    vi.mocked(client.connect).mockImplementationOnce(() => new Promise(() => {}));
    render(<Harness client={client} connectTimeout={30} />);
    await user.click(screen.getByRole('button', { name: 'Help & support' }));
    expect(screen.getByRole('status')).toHaveTextContent('Connecting to support…');
    expect(await screen.findByRole('alert')).toHaveTextContent('taking too long');
    expect(vi.mocked(client.connect).mock.calls[0]?.[0].signal.aborted).toBe(true);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('log')).toBeInTheDocument();
    expect(client.connect).toHaveBeenCalledTimes(2);
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
