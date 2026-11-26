import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  manualNetworkSource,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiTransportRequest,
} from '../../../components/intelligence/ai-conversation/index.js';
import { AiCopilotLayout, useAiSelectionContext, type AiCopilotLayoutProps } from './index.js';

function Page() {
  const { selection, setSelection } = useAiSelectionContext();
  return (
    <div>
      <h1>Quarterly plan</h1>
      <button
        type="button"
        onClick={() =>
          setSelection([{ label: 'Selected', items: '3 rows', data: { rowIds: ['a', 'b', 'c'] } }])
        }
      >
        Select rows
      </button>
      <button type="button" onClick={() => setSelection(null)}>
        Clear
      </button>
      <p>{selection.length} selected</p>
    </div>
  );
}

function Harness({
  onRequest,
  ...props
}: Partial<AiCopilotLayoutProps> & { onRequest?: (r: AiTransportRequest) => void }) {
  const chat = useAiConversation({
    transport: mockTransport({
      delayMs: 0,
      reply: (request) => {
        onRequest?.(request);
        return [...textDeltas('Done.'), { type: 'done' }];
      },
    }),
  });
  return (
    <AiCopilotLayout conversation={chat} title="Assistant" {...props}>
      <Page />
    </AiCopilotLayout>
  );
}

describe('AiCopilotLayout', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders content and a launcher; the panel is closed by default', async () => {
    render(<Harness data-testid="root" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-copilot-layout');
    expect(root).not.toHaveAttribute('data-open');
    expect(screen.getByRole('heading', { name: 'Quarterly plan' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('the launcher opens a docked panel and hides itself; closing returns focus', async () => {
    const user = userEvent.setup();
    render(<Harness data-testid="root" />);
    const launcher = screen.getByRole('button', { name: 'Assistant' });
    await user.click(launcher);
    expect(screen.getByTestId('root')).toHaveAttribute('data-open');
    const panel = screen.getByRole('dialog', { name: 'Assistant' });
    expect(panel).toHaveAttribute('data-placement', 'inline');
    expect(launcher).not.toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Close assistant' }));
    expect(launcher).toBeVisible();
    expect(launcher).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('shares the selection: the page writes it, the panel shows and sends it', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn<(r: AiTransportRequest) => void>();
    render(
      <Harness
        defaultOpen
        context={[{ label: 'Page', items: 'Quarterly plan' }]}
        onRequest={onRequest}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Select rows' }));
    expect(screen.getByRole('list', { name: 'Context' })).toHaveTextContent(
      'Page Quarterly planSelected 3 rows',
    );
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Sum{Enter}');
    await waitFor(() => expect(screen.getByText('Done.')).toBeInTheDocument());
    expect(onRequest.mock.calls[0]?.[0].body).toEqual({
      selection: [{ label: 'Selected', text: '3 rows', data: { rowIds: ['a', 'b', 'c'] } }],
    });
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(screen.getByRole('list', { name: 'Context' })).not.toHaveTextContent('Selected');
  });

  it('can clear the selection after sending and use another field', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn<(r: AiTransportRequest) => void>();
    render(
      <Harness defaultOpen clearSelectionOnSend selectionField="context" onRequest={onRequest} />,
    );
    await user.click(screen.getByRole('button', { name: 'Select rows' }));
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Go{Enter}');
    await waitFor(() => expect(screen.getByText('Done.')).toBeInTheDocument());
    expect(onRequest.mock.calls[0]?.[0].body).toHaveProperty('context');
    expect(screen.getByText('0 selected')).toBeInTheDocument();
  });

  it('sends attached files with the selection, and keeps the selection when they cannot be sent', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn<(r: AiTransportRequest) => void>();
    const { container } = render(
      <Harness
        defaultOpen
        clearSelectionOnSend
        onRequest={onRequest}
        panelProps={{ promptProps: { accept: '.txt' } }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Select rows' }));
    await user.upload(
      container.querySelector<HTMLInputElement>('input[type="file"]')!,
      new File(['x'], 'notes.txt', { type: 'text/plain' }),
    );
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Go{Enter}');
    // The harness conversation has no uploadFile: nothing is sent, nothing is cleared.
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: 'Message the assistant' })).toHaveValue('Go'),
    );
    expect(onRequest).not.toHaveBeenCalled();
    expect(screen.getByText('1 selected')).toBeInTheDocument();
  });

  it('becomes a full-screen sheet below 640px', async () => {
    vi.spyOn(window, 'matchMedia').mockImplementation(
      (query: string) =>
        ({
          matches: true,
          media: query,
          addEventListener: () => {},
          removeEventListener: () => {},
        }) as unknown as MediaQueryList,
    );
    render(<Harness defaultOpen data-testid="root" />);
    await waitFor(() => expect(screen.getByTestId('root')).toHaveAttribute('data-compact'));
    expect(screen.getByRole('dialog', { name: 'Assistant' })).toHaveAttribute(
      'data-placement',
      'fixed',
    );
  });

  it('useAiSelectionContext is inert outside the layout', () => {
    render(<Page />);
    expect(screen.getByText('0 selected')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Harness defaultOpen />)).toContain('mn-ai-copilot-layout');
  });
});

describe('AiCopilotLayout: offline', () => {
  it('shows the offline notice in the docked assistant and sends the queued prompt with its selection on reconnect', async () => {
    const user = userEvent.setup();
    const network = manualNetworkSource(false);
    const requests: AiTransportRequest[] = [];
    function Offline() {
      const chat = useAiConversation({
        network,
        transport: mockTransport({
          delayMs: 0,
          reply: (request) => {
            requests.push(request);
            return [...textDeltas('Done.'), { type: 'done' }];
          },
        }),
      });
      return (
        <AiCopilotLayout conversation={chat} title="Assistant" defaultOpen>
          <Page />
        </AiCopilotLayout>
      );
    }
    render(<Offline />);
    const notice = () => document.querySelector('.mn-ai-thread__notice');
    expect(notice()).toHaveTextContent('You are offline.');
    await user.click(screen.getByRole('button', { name: 'Select rows' }));
    await user.type(screen.getByRole('textbox', { name: 'Message the assistant' }), 'Sum{Enter}');
    expect(notice()).toHaveTextContent('1 message will send when you are back online.');
    expect(requests).toHaveLength(0);
    act(() => network.set(true));
    await waitFor(() => expect(screen.getByText('Done.')).toBeInTheDocument());
    // What was selected when the prompt was written goes with it.
    expect(requests[0]?.body).toEqual({
      selection: [{ label: 'Selected', text: '3 rows', data: { rowIds: ['a', 'b', 'c'] } }],
    });
    await waitFor(() => expect(notice()).toBeNull());
  });
});
