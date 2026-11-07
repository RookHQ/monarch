import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiTransport,
  type UseAiConversationResult,
} from '../ai-conversation/index.js';
import { AiLauncher } from '../ai-launcher/index.js';
import { AiAssistantPanel, type AiAssistantPanelProps } from './index.js';

const quick = mockTransport({
  delayMs: 0,
  reply: () => [
    { type: 'agent', agent: { name: 'Helper', role: 'Writing assistant' } },
    ...textDeltas('Here is the answer.'),
    { type: 'done' },
  ],
});

/** A transport that never finishes until aborted. */
const hanging: AiTransport = {
  async *stream(request) {
    yield { type: 'text-delta', text: 'Working on it' };
    await new Promise<void>((resolve) => {
      request.signal?.addEventListener('abort', () => resolve());
    });
  },
};

function Harness({
  transport = quick,
  initialOpen = false,
  onChat,
  ...props
}: Partial<AiAssistantPanelProps> & {
  transport?: AiTransport;
  initialOpen?: boolean;
  onChat?: (chat: UseAiConversationResult) => void;
}) {
  const chat = useAiConversation({ transport });
  onChat?.(chat);
  const [open, setOpen] = useState(initialOpen);
  return (
    <>
      <AiLauncher open={open} onOpenChange={setOpen} aria-controls="panel" position="inline" />
      <AiAssistantPanel
        id="panel"
        conversation={chat}
        title="Assistant"
        open={open}
        onOpenChange={setOpen}
        {...props}
      />
    </>
  );
}

describe('AiAssistantPanel', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('is a labelled dialog that is hidden while closed', async () => {
    render(<Harness context={[{ label: 'Page', items: 'Quarterly plan' }]} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Assistant' }));
    const panel = screen.getByRole('dialog', { name: 'Assistant' });
    expect(panel).toHaveClass('mn-ai-assistant-panel');
    expect(panel).not.toHaveAttribute('aria-modal');
    expect(panel).toHaveAttribute('data-side', 'right');
    expect(screen.getByRole('list', { name: 'Context' })).toHaveTextContent('Page Quarterly plan');
    expect(screen.getByRole('log', { name: 'Conversation' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('focuses the input on open and returns focus to the launcher on close', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const launcher = screen.getByRole('button', { name: 'Assistant' });
    await user.click(launcher);
    expect(screen.getByRole('textbox', { name: 'Message the assistant' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Close assistant' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(launcher).toHaveFocus();
  });

  it('the launcher shortcut toggles it and Escape closes it when idle', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('sends with the prompt and shows the streamed reply', async () => {
    const user = userEvent.setup();
    render(<Harness initialOpen />);
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Hello{Enter}');
    await waitFor(() => expect(screen.getByText('Here is the answer.')).toBeInTheDocument());
    expect(screen.getByText('Helper · Writing assistant')).toBeInTheDocument();
  });

  it('starters send their prompt', async () => {
    const user = userEvent.setup();
    render(
      <Harness
        initialOpen
        starters={['Summarise this page', { label: 'Draft', prompt: 'Draft a reply' }]}
      />,
    );
    expect(screen.getByText('How can I help?')).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Suggested prompts' });
    expect(list).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Draft' }));
    await waitFor(() => expect(screen.getByText('Draft a reply')).toBeInTheDocument());
  });

  it('Escape stops a running reply instead of closing', async () => {
    const user = userEvent.setup();
    let chat: UseAiConversationResult | undefined;
    render(<Harness initialOpen transport={hanging} onChat={(c) => (chat = c)} />);
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Go{Enter}');
    await waitFor(() => expect(chat?.busy).toBe(true));
    await user.click(screen.getByRole('button', { name: 'Minimize assistant' }));
    await user.click(screen.getByRole('button', { name: 'Expand assistant' }));
    // Focus is on the header's expand button: Escape still stops.
    screen.getByRole('button', { name: 'Minimize assistant' }).focus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(chat?.status).toBe('stopped'));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('minimizes to its header and focuses the input when expanded', async () => {
    const user = userEvent.setup();
    const onMinimizedChange = vi.fn();
    render(<Harness initialOpen onMinimizedChange={onMinimizedChange} />);
    const toggle = screen.getByRole('button', { name: 'Minimize assistant' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await user.click(toggle);
    expect(onMinimizedChange).toHaveBeenCalledWith(true);
    expect(screen.getByRole('dialog')).toHaveAttribute('data-minimized');
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Expand assistant' }));
    expect(screen.getByRole('textbox', { name: 'Message the assistant' })).toHaveFocus();
  });

  it('is a modal sheet that keeps Tab inside below 640px', async () => {
    vi.spyOn(window, 'matchMedia').mockImplementation(
      (query: string) =>
        ({
          matches: true,
          media: query,
          addEventListener: () => {},
          removeEventListener: () => {},
        }) as unknown as MediaQueryList,
    );
    const user = userEvent.setup();
    render(<Harness initialOpen />);
    const panel = screen.getByRole('dialog');
    expect(panel).toHaveAttribute('aria-modal', 'true');
    expect(panel).toHaveAttribute('data-compact');
    screen.getByRole('button', { name: 'Minimize assistant' }).focus();
    await user.tab({ shift: true });
    expect(panel.contains(document.activeElement)).toBe(true);
  });

  it('can unmount while closed and accepts strings', () => {
    const { unmount } = render(<Harness keepMounted={false} />);
    expect(document.querySelector('.mn-ai-assistant-panel')).toBeNull();
    unmount();
    render(
      <Harness keepMounted={false} initialOpen messages={{ close: 'Schließen' }} side="left" />,
    );
    expect(screen.getByRole('button', { name: 'Schließen' })).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveAttribute('data-side', 'left');
  });

  it('renders on the server', () => {
    function Server() {
      const chat = useAiConversation({ transport: quick });
      return (
        <AiAssistantPanel conversation={chat} title="Assistant" open onOpenChange={() => {}} />
      );
    }
    const html = renderToString(<Server />);
    expect(html).toContain('mn-ai-assistant-panel');
    expect(html).toContain('role="dialog"');
  });
});
