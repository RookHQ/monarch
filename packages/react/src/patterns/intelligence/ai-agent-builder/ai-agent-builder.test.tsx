import { describe, expect, it, vi } from 'vitest';
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
import { AiAgentBuilder, type AiAgentBuilderProps, type AiAgentDefinition } from './index.js';

const TOOLS = [
  { id: 'search', name: 'Search documents', description: 'Read-only.' },
  { id: 'email', name: 'Send email' },
];
const SOURCES = [
  {
    id: 'space-1',
    label: 'Handbook',
    kind: 'space' as const,
    children: [
      { id: 'doc-1', label: 'Leave policy', kind: 'doc' as const },
      { id: 'doc-2', label: 'Travel policy', kind: 'doc' as const },
    ],
  },
];

function Harness({
  onRequest,
  ...props
}: Partial<AiAgentBuilderProps> & { onRequest?: (r: AiTransportRequest) => void }) {
  const chat = useAiConversation({
    transport: mockTransport({
      delayMs: 0,
      reply: (request) => {
        onRequest?.(request);
        return [...textDeltas('Test reply.'), { type: 'done' }];
      },
    }),
  });
  return <AiAgentBuilder tools={TOOLS} knowledgeSources={SOURCES} conversation={chat} {...props} />;
}

describe('AiAgentBuilder', () => {
  it('renders persona, instructions, tools, knowledge and a test pane', async () => {
    render(<Harness data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-agent-builder');
    expect(screen.getByRole('region', { name: 'Agent builder' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /^Name/ })).toBeRequired();
    expect(screen.getByRole('textbox', { name: 'Role' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Instructions' })).toHaveAttribute(
      'maxlength',
      '8000',
    );
    expect(screen.getByRole('group', { name: 'Tools' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Search documents' })).not.toBeChecked();
    expect(screen.getByRole('heading', { name: 'Knowledge' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Test' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Message the agent' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('edits the definition and reports every change', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn<(d: AiAgentDefinition) => void>();
    render(<Harness onChange={onChange} />);
    await user.type(screen.getByRole('textbox', { name: /^Name/ }), 'Helper');
    await user.type(screen.getByRole('textbox', { name: 'Role' }), 'Support');
    await user.type(screen.getByRole('textbox', { name: 'Instructions' }), 'Be brief.');
    expect(screen.getByText('9 / 8,000 characters')).toBeInTheDocument();
    await user.click(screen.getByRole('checkbox', { name: 'Search documents' }));
    expect(onChange).toHaveBeenLastCalledWith({
      name: 'Helper',
      role: 'Support',
      instructions: 'Be brief.',
      tools: ['search'],
      knowledge: [],
    });
    await user.click(screen.getByRole('checkbox', { name: 'Search documents' }));
    expect(onChange.mock.lastCall?.[0].tools).toEqual([]);
  });

  it('the test chat sends the unsaved definition and can restart', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn<(r: AiTransportRequest) => void>();
    render(
      <Harness
        onRequest={onRequest}
        defaultValue={{
          name: 'Helper',
          role: '',
          instructions: 'Be brief.',
          tools: ['search'],
          knowledge: [],
        }}
      />,
    );
    await user.type(screen.getByRole('textbox', { name: 'Message the agent' }), 'Hi{Enter}');
    await waitFor(() => expect(screen.getByText('Test reply.')).toBeInTheDocument());
    expect(onRequest.mock.calls[0]?.[0].body).toEqual({
      agent: expect.objectContaining({ name: 'Helper', instructions: 'Be brief.' }),
    });
    await user.click(screen.getByRole('button', { name: 'Restart test' }));
    expect(screen.queryByText('Test reply.')).not.toBeInTheDocument();
    expect(screen.getByText('Send a message to try the agent.')).toBeInTheDocument();
  });

  it('Save requires a name, then saves and reports errors', async () => {
    const user = userEvent.setup();
    const onSave = vi
      .fn<(d: AiAgentDefinition) => Promise<void>>()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error('Server busy.'));
    render(<Harness onSave={onSave} />);
    await user.click(screen.getByRole('button', { name: 'Save agent' }));
    expect(onSave).not.toHaveBeenCalled();
    const name = screen.getByRole('textbox', { name: /^Name/ });
    expect(name).toHaveFocus();
    expect(name).toHaveAccessibleDescription('Give the agent a name.');
    await user.type(name, 'Helper');
    await user.click(screen.getByRole('button', { name: 'Save agent' }));
    await waitFor(() => expect(screen.getByText('Agent saved.')).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Save agent' }));
    await waitFor(() =>
      expect(screen.getByText('Couldn’t save. Server busy.')).toBeInTheDocument(),
    );
  });

  it('hides the test pane and knowledge when not given', () => {
    render(<AiAgentBuilder tools={[]} />);
    expect(screen.queryByRole('region', { name: 'Test' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Knowledge' })).not.toBeInTheDocument();
    expect(screen.getByText('No tools available.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAgentBuilder tools={TOOLS} knowledgeSources={SOURCES} />);
    expect(html).toContain('mn-ai-agent-builder');
    expect(html).toContain('Search documents');
  });
});
