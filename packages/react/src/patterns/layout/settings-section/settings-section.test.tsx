import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { Switch } from '../../../components/forms/switch/index.js';
import { SettingsActionRow, SettingsRow, SettingsSection } from './index.js';

describe('Settings', () => {
  it('wires each row to its control', async () => {
    render(
      <SettingsSection
        title="Notifications"
        description="How we reach you."
        actions={<Button variant="primary">Save</Button>}
      >
        <SettingsRow label="Display name" description="Shown to teammates." required>
          <Input />
        </SettingsRow>
        <SettingsRow label="Email digest" error="Pick at least one day.">
          <Switch />
        </SettingsRow>
      </SettingsSection>,
    );
    expect(screen.getByRole('region', { name: 'Notifications' })).toBeInTheDocument();
    const name = screen.getByRole('textbox', { name: /display name/i });
    expect(name).toBeRequired();
    expect(name).toHaveAccessibleDescription('Shown to teammates.');
    expect(screen.getByRole('switch', { name: 'Email digest' })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('danger tone is exposed for styling', () => {
    const { container } = render(
      <SettingsSection title="Danger zone" tone="danger">
        <div>x</div>
      </SettingsSection>,
    );
    expect(container.querySelector('.mn-settings-section')).toHaveAttribute('data-tone', 'danger');
  });

  it('save footer: disabled until dirty, announces unsaved changes, saves and discards', async () => {
    const onSave = vi.fn();
    function Demo() {
      const [saved, setSaved] = useState('Acme');
      const [draft, setDraft] = useState('Acme');
      return (
        <SettingsSection
          title="Workspace"
          dirty={draft !== saved}
          onSave={() => {
            onSave(draft);
            setSaved(draft);
          }}
          onCancel={() => setDraft(saved)}
        >
          <SettingsRow label="Workspace name">
            <Input value={draft} onChange={(e) => setDraft(e.target.value)} />
          </SettingsRow>
        </SettingsSection>
      );
    }
    const { container } = render(<Demo />);
    const user = userEvent.setup();
    const save = screen.getByRole('button', { name: 'Save changes' });
    const discard = screen.getByRole('button', { name: 'Discard' });
    const status = screen.getByRole('status');
    expect(save).toBeDisabled();
    expect(discard).toBeDisabled();
    expect(status).toBeEmptyDOMElement();

    await user.type(screen.getByRole('textbox', { name: 'Workspace name' }), ' Inc');
    expect(status).toHaveTextContent('You have unsaved changes.');
    expect(container.querySelector('.mn-settings-section')).toHaveAttribute('data-dirty');
    await user.click(discard);
    expect(screen.getByRole('textbox')).toHaveValue('Acme');
    expect(save).toBeDisabled();

    await user.type(screen.getByRole('textbox'), '!');
    await user.tab();
    await user.tab();
    expect(save).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSave).toHaveBeenCalledWith('Acme!');
    expect(status).toBeEmptyDOMElement();
    await expectNoA11yViolations();
  });

  it('saving shows a busy Save and blocks Discard; custom labels and extra actions', () => {
    render(
      <SettingsSection
        title="Profile"
        dirty
        saving
        onSave={() => {}}
        onCancel={() => {}}
        saveLabel="Save profile"
        cancelLabel="Reset"
        dirtyMessage="2 unsaved changes"
        stickyFooter
        actions={<Button>Preview</Button>}
      >
        <div>row</div>
      </SettingsSection>,
    );
    expect(screen.getByRole('button', { name: /save profile/i })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    expect(screen.getByRole('button', { name: 'Reset' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Preview' })).toBeEnabled();
    expect(screen.getByRole('status')).toHaveTextContent('2 unsaved changes');
    const region = screen.getByRole('region', { name: 'Profile' });
    expect(region).toHaveAttribute('data-sticky-footer');
    expect(region).toHaveAttribute('aria-busy', 'true');
  });

  it('loading and no-access states replace the rows and the footer', async () => {
    const { rerender } = render(
      <SettingsSection title="Payroll" state="loading" onSave={() => {}}>
        <div>row content</div>
      </SettingsSection>,
    );
    const region = screen.getByRole('region', { name: 'Payroll' });
    expect(region).toHaveAttribute('aria-busy', 'true');
    expect(region).toHaveAttribute('data-state', 'loading');
    expect(screen.getByRole('status')).toHaveTextContent('Loading settings');
    expect(screen.queryByText('row content')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Save changes' })).toBeNull();

    rerender(
      <SettingsSection
        title="Payroll"
        state="no-access"
        noAccessDescription="Ask a workspace admin to change payroll settings."
        onSave={() => {}}
      >
        <div>row content</div>
      </SettingsSection>,
    );
    expect(screen.getByText('You don’t have access to these settings')).toBeInTheDocument();
    expect(screen.getByText(/ask a workspace admin/i)).toBeInTheDocument();
    expect(screen.queryByText('row content')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('danger zone: SettingsActionRow pairs a consequence with its button', async () => {
    const onDelete = vi.fn();
    render(
      <SettingsSection title="Danger zone" tone="danger">
        <SettingsActionRow
          title="Delete workspace"
          description="Removes every project and file. This can’t be undone."
          action={
            <Button variant="danger" onClick={onDelete}>
              Delete workspace
            </Button>
          }
        />
      </SettingsSection>,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onDelete).toHaveBeenCalledOnce();
    expect(screen.getByText(/can’t be undone/)).toHaveClass('mn-settings-action-row__description');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SettingsSection title="General">
        <SettingsRow label="Name">
          <Input />
        </SettingsRow>
      </SettingsSection>,
    );
    expect(html).toContain('mn-settings-section');
    expect(html).toContain('mn-settings-row');
    const saving = renderToString(
      <SettingsSection title="General" dirty onSave={() => {}} state="ready">
        <SettingsActionRow title="Leave" action={<Button>Leave</Button>} />
      </SettingsSection>,
    );
    expect(saving).toContain('You have unsaved changes.');
    expect(saving).toContain('mn-settings-action-row');
  });
});
