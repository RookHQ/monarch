import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiSettingsSection,
  type AiSettingsDraft,
  type AiSettingsSectionProps,
  type AiSettingsValue,
} from './index.js';

const VALUE: AiSettingsValue = {
  apps: { docs: true, mail: false },
  dataSharing: false,
  memory: true,
  retention: '90d',
  provider: '',
};

const APPS = [
  { id: 'docs', label: 'Documents', description: 'Summaries and drafts.' },
  { id: 'mail', label: 'Mail' },
];
const PROVIDERS = [{ id: 'acme', label: 'Example AI' }];

function Harness({
  onSave,
  ...props
}: Partial<AiSettingsSectionProps> & { onSave?: AiSettingsSectionProps['onSave'] }) {
  const [value, setValue] = useState<AiSettingsValue>(props.value ?? VALUE);
  return (
    <AiSettingsSection
      apps={APPS}
      providers={PROVIDERS}
      {...props}
      value={value}
      onSave={async (draft) => {
        await onSave?.(draft);
        const { apiKey, ...rest } = draft;
        setValue({ ...rest, keyConfigured: Boolean(apiKey) || value.keyConfigured });
      }}
    />
  );
}

const footerStatus = () => document.querySelector('.mn-ai-settings-section__status') as HTMLElement;

describe('AiSettingsSection', () => {
  it('renders a labelled section with every setting', async () => {
    render(<Harness data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-settings-section');
    expect(screen.getByRole('region', { name: 'AI' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Use AI in' })).toBeInTheDocument();
    expect(screen.getByRole('switch', { name: 'Documents' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Mail' })).not.toBeChecked();
    expect(screen.getByRole('switch', { name: 'Share data to improve AI' })).not.toBeChecked();
    expect(screen.getByRole('switch', { name: 'Memory' })).toBeChecked();
    expect(screen.getByRole('combobox', { name: 'Conversation history' })).toHaveValue('90d');
    expect(screen.getByRole('combobox', { name: 'AI provider' })).toHaveValue('');
    expect(screen.queryByLabelText('Example AI API key')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('keeps changes as a draft until Save', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn<(d: AiSettingsDraft) => void>();
    render(<Harness onSave={onSave} />);
    await user.click(screen.getByRole('switch', { name: 'Mail' }));
    await user.click(screen.getByRole('switch', { name: 'Memory' }));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Conversation history' }), '1y');
    expect(onSave).not.toHaveBeenCalled();
    expect(footerStatus()).toHaveTextContent('You have unsaved changes.');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(onSave).toHaveBeenCalledWith({
      apps: { docs: true, mail: true },
      dataSharing: false,
      memory: false,
      retention: '1y',
      provider: '',
    });
    await waitFor(() => expect(footerStatus()).toHaveTextContent('Settings saved.'));
    expect(screen.getByRole('switch', { name: 'Mail' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('Discard returns to the saved settings; toggling back is not dirty', async () => {
    const user = userEvent.setup();
    const onDirtyChange = vi.fn();
    render(<Harness onDirtyChange={onDirtyChange} />);
    const sharing = screen.getByRole('switch', { name: 'Share data to improve AI' });
    await user.click(sharing);
    expect(onDirtyChange).toHaveBeenLastCalledWith(true);
    await user.click(sharing);
    expect(onDirtyChange).toHaveBeenLastCalledWith(false);
    await user.click(sharing);
    await user.click(screen.getByRole('button', { name: 'Discard' }));
    expect(sharing).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('bring-your-own key: shown for a provider, sent only when typed', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn<(d: AiSettingsDraft) => void>();
    render(<Harness onSave={onSave} />);
    await user.selectOptions(screen.getByRole('combobox', { name: 'AI provider' }), 'acme');
    const key = screen.getByLabelText('Example AI API key');
    expect(key).toHaveAttribute('type', 'password');
    await user.type(key, 'sk-123');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(onSave).toHaveBeenLastCalledWith(
      expect.objectContaining({ provider: 'acme', apiKey: 'sk-123' }),
    );
    // Saved: the key is now set and not sent back.
    await waitFor(() => expect(screen.getByLabelText('Example AI API key')).toHaveValue(''));
    await user.click(screen.getByRole('switch', { name: 'Memory' }));
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(onSave.mock.calls[1]?.[0]).not.toHaveProperty('apiKey');
  });

  it('keeps the draft and shows the error when saving fails', async () => {
    const user = userEvent.setup();
    render(
      <AiSettingsSection
        value={VALUE}
        apps={APPS}
        onSave={() => Promise.reject(new Error('Network down.'))}
      />,
    );
    await user.click(screen.getByRole('switch', { name: 'Mail' }));
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(footerStatus()).toHaveTextContent('Couldn’t save. Network down.'));
    expect(screen.getByRole('switch', { name: 'Mail' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled();
  });

  it('read-only disables everything and has no footer; rows can be hidden', () => {
    render(
      <AiSettingsSection
        value={VALUE}
        apps={APPS}
        readOnly
        show={{ memory: false, retention: false }}
        onSave={() => {}}
        messages={{ readOnly: 'Ask an admin.' }}
      />,
    );
    expect(screen.getByText('Ask an admin.')).toBeInTheDocument();
    expect(screen.getByRole('switch', { name: 'Documents' })).toBeDisabled();
    expect(screen.getByRole('switch', { name: 'Share data to improve AI' })).toBeDisabled();
    expect(screen.queryByRole('switch', { name: 'Memory' })).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSettingsSection value={VALUE} apps={APPS} providers={PROVIDERS} onSave={() => {}} />,
    );
    expect(html).toContain('mn-ai-settings-section');
    expect(html).toContain('Documents');
  });
});
