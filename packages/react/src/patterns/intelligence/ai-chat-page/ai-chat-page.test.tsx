import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  mockTransport,
  textDeltas,
  useAiConversation,
} from '../../../components/intelligence/ai-conversation/index.js';
import { AiChatPage, type AiChatPageProps } from './index.js';

const NOW = new Date('2026-03-10T12:00:00Z');
const CONVERSATIONS = [
  { id: 'c1', title: 'Quarterly planning', updatedAt: '2026-03-10T09:00:00Z' },
  { id: 'c2', title: 'Release notes draft', updatedAt: '2026-03-08T09:00:00Z' },
];
const ARTIFACT = {
  id: 'a1',
  kind: 'document' as const,
  title: 'Q3 summary',
  version: 1,
  content: '# Q3 summary\n\nRevenue grew.',
};

function Harness(props: Partial<AiChatPageProps>) {
  const chat = useAiConversation({
    transport: mockTransport({
      delayMs: 0,
      reply: () => [...textDeltas('Here you go.'), { type: 'done' }],
    }),
  });
  return (
    <AiChatPage
      conversation={chat}
      conversations={CONVERSATIONS}
      activeId="c1"
      onSelectConversation={() => {}}
      historyProps={{ now: NOW }}
      {...props}
    />
  );
}

function mockCompact(matches: boolean) {
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query: string) =>
      ({
        matches,
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
      }) as unknown as MediaQueryList,
  );
}

describe('AiChatPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the history rail, header, thread and prompt', async () => {
    render(<Harness title="Quarterly planning" data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-chat-page');
    const history = screen.getByRole('navigation', { name: 'Conversation history' });
    expect(within(history).getByRole('button', { current: 'page' })).toHaveTextContent(
      'Quarterly planning',
    );
    expect(screen.getByRole('region', { name: 'Chat' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Quarterly planning' })).toBeInTheDocument();
    expect(screen.getByRole('log', { name: 'Conversation' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Message the assistant' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('selects and starts conversations from the rail', async () => {
    const user = userEvent.setup();
    const onSelectConversation = vi.fn();
    const onNewConversation = vi.fn();
    render(
      <Harness onSelectConversation={onSelectConversation} onNewConversation={onNewConversation} />,
    );
    await user.click(screen.getByRole('button', { name: /Release notes draft/ }));
    expect(onSelectConversation).toHaveBeenCalledWith('c2');
    await user.click(screen.getByRole('button', { name: 'New conversation' }));
    expect(onNewConversation).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('textbox', { name: 'Message the assistant' })).toHaveFocus();
  });

  it('sends through the conversation and shows the empty state first', async () => {
    const user = userEvent.setup();
    render(<Harness empty={<p>Ask anything.</p>} />);
    expect(screen.getByText('Ask anything.')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Hi{Enter}');
    await waitFor(() => expect(screen.getByText('Here you go.')).toBeInTheDocument());
  });

  it('shows the artifact canvas beside the thread and closes it', async () => {
    const user = userEvent.setup();
    const onArtifactOpenChange = vi.fn();
    render(
      <Harness
        data-testid="root"
        artifact={ARTIFACT}
        onArtifactOpenChange={onArtifactOpenChange}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-canvas');
    expect(screen.getByRole('region', { name: 'Artifact: Q3 summary' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Close artifact' }));
    expect(onArtifactOpenChange).toHaveBeenCalledWith(false);
    expect(screen.queryByRole('region', { name: 'Artifact: Q3 summary' })).not.toBeInTheDocument();
    expect(screen.getByTestId('root')).not.toHaveAttribute('data-canvas');
  });

  it('puts the history in a sheet below 768px', async () => {
    mockCompact(true);
    const user = userEvent.setup();
    const onSelectConversation = vi.fn();
    render(<Harness data-testid="root" onSelectConversation={onSelectConversation} />);
    await waitFor(() => expect(screen.getByTestId('root')).toHaveAttribute('data-compact'));
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    const menu = screen.getByRole('button', { name: 'Show conversation history' });
    await user.click(menu);
    const sheet = screen.getByRole('dialog', { name: 'Conversation history' });
    await user.click(within(sheet).getByRole('button', { name: /Release notes draft/ }));
    expect(onSelectConversation).toHaveBeenCalledWith('c2');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('takes strings', () => {
    render(<Harness messages={{ inputLabel: 'Ask', chat: 'Assistant chat' }} />);
    expect(screen.getByRole('textbox', { name: 'Ask' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Assistant chat' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Harness title="Quarterly planning" artifact={ARTIFACT} />)).toContain(
      'mn-ai-chat-page',
    );
  });
});
