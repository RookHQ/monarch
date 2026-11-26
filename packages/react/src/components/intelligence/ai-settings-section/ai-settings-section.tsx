'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Field, FieldDescription, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';
import { Switch } from '../../forms/switch/index.js';
import { SwitchSettingsItem, SwitchSettingsList } from '../../forms/switch-settings-list/index.js';
import { SecretField } from '../../technical/secret-field/index.js';

/** Every string AiSettingsSection shows or announces. English defaults. */
export interface AiSettingsSectionMessages {
  title: string;
  description: string;
  appsTitle: string;
  appsDescription: string;
  dataSharing: string;
  dataSharingDescription: string;
  memory: string;
  memoryDescription: string;
  retention: string;
  retentionDescription: string;
  provider: string;
  providerDescription: string;
  /** The provider option that uses the workspace's built-in AI. */
  providerDefault: string;
  /** Label of the key field; also names its reveal button. */
  apiKey: string;
  apiKeyDescription: string;
  /** Shown while the draft differs from the saved settings. */
  dirty: string;
  save: string;
  discard: string;
  /** Announced after a successful save. */
  saved: string;
  /** Shown when saving fails, before the error's message. */
  saveError: string;
  /** Shown when `readOnly`. */
  readOnly: string;
}

const DEFAULT_MESSAGES: AiSettingsSectionMessages = {
  title: 'AI',
  description: 'Choose where AI is available in this workspace and how it handles your data.',
  appsTitle: 'Use AI in',
  appsDescription: 'Turn AI features on or off for each app.',
  dataSharing: 'Share data to improve AI',
  dataSharingDescription:
    'Allow prompts and responses to be used to improve AI quality. Off keeps them private to this workspace.',
  memory: 'Memory',
  memoryDescription: 'Let AI remember facts and preferences across conversations.',
  retention: 'Conversation history',
  retentionDescription: 'How long AI conversations are kept before they are deleted.',
  provider: 'AI provider',
  providerDescription: 'Use the built-in provider or bring your own key.',
  providerDefault: 'Built-in',
  apiKey: 'API key',
  apiKeyDescription: 'Stored encrypted. Leave blank to keep the saved key.',
  dirty: 'You have unsaved changes.',
  save: 'Save changes',
  discard: 'Discard',
  saved: 'Settings saved.',
  saveError: 'Couldn’t save.',
  readOnly: 'Only workspace admins can change these settings.',
};

/** An app that AI can be turned on or off for. */
export interface AiSettingsApp {
  id: string;
  label: ReactNode;
  description?: ReactNode | undefined;
}

/** A bring-your-own-key provider choice. */
export interface AiSettingsProvider {
  id: string;
  label: string;
}

/** A retention choice for conversation history. */
export interface AiSettingsRetentionOption {
  value: string;
  label: string;
}

/** The saved workspace AI settings. */
export interface AiSettingsValue {
  /** App id → AI on. Apps missing from the record count as off. */
  apps: Readonly<Record<string, boolean>>;
  dataSharing: boolean;
  memory: boolean;
  /** One of `retentionOptions`' values. */
  retention: string;
  /** A `providers` id, or `''` for the built-in provider. */
  provider: string;
  /** A key is stored for `provider`. It is never sent back to the page. */
  keyConfigured?: boolean | undefined;
}

/** What `onSave` receives: the full draft, plus a new key when one was typed. */
export interface AiSettingsDraft extends Omit<AiSettingsValue, 'keyConfigured'> {
  /** A new key to store. Absent when the field was left blank (keep the saved key). */
  apiKey?: string | undefined;
}

export const DEFAULT_AI_RETENTION_OPTIONS: readonly AiSettingsRetentionOption[] = [
  { value: '30d', label: '30 days' },
  { value: '90d', label: '90 days' },
  { value: '1y', label: '1 year' },
  { value: 'forever', label: 'Until deleted' },
];

export interface AiSettingsSectionProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'title' | 'onChange'
> {
  /** The saved settings. The section edits a draft of them. */
  value: AiSettingsValue;
  /**
   * Saves the draft. Nothing is saved before this is called by the Save
   * button. Return a promise to show progress; reject it to keep the draft
   * and show the error.
   */
  onSave: (draft: AiSettingsDraft) => void | Promise<void>;
  /** Apps listed under "Use AI in". Leave out to hide the group. */
  apps?: readonly AiSettingsApp[] | undefined;
  /** Bring-your-own-key providers. Leave out to hide provider and key. */
  providers?: readonly AiSettingsProvider[] | undefined;
  /** Conversation history choices. Default 30 days, 90 days, 1 year, until deleted. */
  retentionOptions?: readonly AiSettingsRetentionOption[] | undefined;
  /** Hide rows the product does not offer. All shown by default. */
  show?: Partial<Record<'dataSharing' | 'memory' | 'retention', boolean>> | undefined;
  /** Everything is shown but disabled, with `messages.readOnly`; no footer. */
  readOnly?: boolean | undefined;
  /** Section heading element. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | undefined;
  /** Called on every draft change, e.g. to guard unsaved changes on navigation. */
  onDirtyChange?: ((dirty: boolean) => void) | undefined;
  /** Turns a failed save into text shown after `messages.saveError`. Default: the Error's message. */
  getErrorMessage?: ((error: unknown) => ReactNode) | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiSettingsSectionMessages> | undefined;
}

function toDraft(v: AiSettingsValue): AiSettingsDraft {
  return {
    apps: { ...v.apps },
    dataSharing: v.dataSharing,
    memory: v.memory,
    retention: v.retention,
    provider: v.provider,
  };
}

function sameDraft(a: AiSettingsDraft, b: AiSettingsDraft, appIds: readonly string[]): boolean {
  if (a.dataSharing !== b.dataSharing || a.memory !== b.memory) return false;
  if (a.retention !== b.retention || a.provider !== b.provider) return false;
  if (a.apiKey) return false;
  return appIds.every((id) => Boolean(a.apps[id]) === Boolean(b.apps[id]));
}

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : null;
}

/**
 * Workspace AI settings in a settings card: AI on or off per app, data
 * sharing, memory, conversation retention and a bring-your-own provider key.
 * Every change stays a draft until Save; Discard returns to the saved
 * settings. The key is write-only: a saved key is shown as set, never sent
 * back.
 */
export const AiSettingsSection = forwardRef<HTMLElement, AiSettingsSectionProps>(
  function AiSettingsSection(
    {
      value,
      onSave,
      apps,
      providers,
      retentionOptions = DEFAULT_AI_RETENTION_OPTIONS,
      show,
      readOnly = false,
      headingLevel: H = 'h2',
      onDirtyChange,
      getErrorMessage = defaultErrorMessage,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const auto = useId().replace(/:/g, '');
    const titleId = `mn-ai-settings-${auto}-title`;
    const descriptionId = `mn-ai-settings-${auto}-description`;

    const saved = toDraft(value);
    const [edit, setEdit] = useState<AiSettingsDraft | null>(null);
    const draft = edit ?? saved;
    const appIds = (apps ?? []).map((a) => a.id);
    const dirty = edit !== null && !sameDraft(edit, saved, appIds);

    const [saving, setSaving] = useState(false);
    const [status, setStatus] = useState<'idle' | 'saved' | 'error'>('idle');
    const [error, setError] = useState<ReactNode>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const lastDirty = useRef(dirty);
    useEffect(() => {
      if (lastDirty.current === dirty) return;
      lastDirty.current = dirty;
      onDirtyChange?.(dirty);
    }, [dirty, onDirtyChange]);

    const update = (patch: Partial<AiSettingsDraft>) => {
      setStatus('idle');
      setEdit({ ...draft, ...patch });
    };

    const save = async () => {
      if (!dirty || saving) return;
      const next: AiSettingsDraft = { ...draft };
      if (!next.apiKey || !next.provider) delete next.apiKey;
      setSaving(true);
      setStatus('idle');
      setError(null);
      try {
        await onSave(next);
        if (!mounted.current) return;
        setEdit(null);
        setStatus('saved');
      } catch (err) {
        if (!mounted.current) return;
        setError(getErrorMessage(err));
        setStatus('error');
      } finally {
        if (mounted.current) setSaving(false);
      }
    };

    const discard = () => {
      setEdit(null);
      setStatus('idle');
      setError(null);
    };

    const showRow = (k: 'dataSharing' | 'memory' | 'retention') => show?.[k] !== false;
    const disabled = readOnly || saving;
    const provider = providers?.find((p) => p.id === draft.provider);

    const statusText =
      status === 'error' ? (
        <>
          {m.saveError}
          {error ? <> {error}</> : null}
        </>
      ) : dirty ? (
        m.dirty
      ) : status === 'saved' ? (
        m.saved
      ) : null;

    return (
      <section
        ref={ref}
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        aria-busy={saving || undefined}
        {...rest}
        className={cx('mn-ai-settings-section', className)}
        data-dirty={dirty || undefined}
        data-read-only={readOnly || undefined}
        data-status={status === 'idle' ? undefined : status}
      >
        <div className="mn-ai-settings-section__header">
          <H id={titleId} className="mn-ai-settings-section__title">
            <span className="mn-ai-settings-section__icon" aria-hidden>
              <BotGlyph />
            </span>
            {m.title}
          </H>
          <p id={descriptionId} className="mn-ai-settings-section__description">
            {m.description}
          </p>
          {readOnly ? <p className="mn-ai-settings-section__read-only">{m.readOnly}</p> : null}
        </div>
        <div className="mn-ai-settings-section__card">
          <div className="mn-ai-settings-section__body">
            {apps?.length ? (
              <SwitchSettingsList
                title={m.appsTitle}
                description={m.appsDescription}
                headingLevel={H === 'h2' ? 'h3' : 'h4'}
                className="mn-ai-settings-section__apps"
              >
                {apps.map((app) => (
                  <SwitchSettingsItem
                    key={app.id}
                    label={app.label}
                    description={app.description}
                    checked={Boolean(draft.apps[app.id])}
                    disabled={disabled}
                    onCheckedChange={(on) => update({ apps: { ...draft.apps, [app.id]: on } })}
                  />
                ))}
              </SwitchSettingsList>
            ) : null}
            {showRow('dataSharing') ? (
              <Field layout="row" disabled={disabled} className="mn-ai-settings-section__row">
                <FieldLabel>{m.dataSharing}</FieldLabel>
                <FieldDescription>{m.dataSharingDescription}</FieldDescription>
                <Switch
                  checked={draft.dataSharing}
                  onCheckedChange={(on) => update({ dataSharing: on })}
                />
              </Field>
            ) : null}
            {showRow('memory') ? (
              <Field layout="row" disabled={disabled} className="mn-ai-settings-section__row">
                <FieldLabel>{m.memory}</FieldLabel>
                <FieldDescription>{m.memoryDescription}</FieldDescription>
                <Switch checked={draft.memory} onCheckedChange={(on) => update({ memory: on })} />
              </Field>
            ) : null}
            {showRow('retention') ? (
              <Field layout="row" disabled={disabled} className="mn-ai-settings-section__row">
                <FieldLabel>{m.retention}</FieldLabel>
                <FieldDescription>{m.retentionDescription}</FieldDescription>
                <NativeSelect
                  options={retentionOptions}
                  value={draft.retention}
                  onChange={(e) => update({ retention: e.target.value })}
                />
              </Field>
            ) : null}
            {providers?.length ? (
              <div className="mn-ai-settings-section__provider">
                <Field layout="row" disabled={disabled} className="mn-ai-settings-section__row">
                  <FieldLabel>{m.provider}</FieldLabel>
                  <FieldDescription>{m.providerDescription}</FieldDescription>
                  <NativeSelect
                    options={[
                      { value: '', label: m.providerDefault },
                      ...providers.map((p) => ({ value: p.id, label: p.label })),
                    ]}
                    value={draft.provider}
                    onChange={(e) => update({ provider: e.target.value, apiKey: undefined })}
                  />
                </Field>
                {provider ? (
                  <Field disabled={disabled} className="mn-ai-settings-section__key">
                    <FieldLabel>{`${provider.label} ${m.apiKey}`}</FieldLabel>
                    <FieldDescription>{m.apiKeyDescription}</FieldDescription>
                    <SecretField
                      label={`${provider.label} ${m.apiKey}`}
                      isSet={Boolean(value.keyConfigured) && draft.provider === value.provider}
                      value={draft.apiKey ?? ''}
                      onChange={(e) => update({ apiKey: e.target.value })}
                    />
                  </Field>
                ) : null}
              </div>
            ) : null}
          </div>
          {readOnly ? null : (
            <div className="mn-ai-settings-section__footer">
              <p className="mn-ai-settings-section__status" role="status" aria-live="polite">
                {statusText}
              </p>
              <Button disabled={!dirty || saving} onClick={discard}>
                {m.discard}
              </Button>
              <Button
                variant="primary"
                disabled={!dirty}
                loading={saving}
                onClick={() => void save()}
              >
                {m.save}
              </Button>
            </div>
          )}
        </div>
      </section>
    );
  },
);
