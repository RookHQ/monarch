import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
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
