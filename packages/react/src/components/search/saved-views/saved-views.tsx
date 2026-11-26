'use client';

import {
  forwardRef,
  Fragment,
  useCallback,
  useId,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, StarGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../../overlays/dialog/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';

export interface SavedView {
  id: string;
  name: string;
  /** Items in the view, shown as a count next to its name. */
  count?: number;
  /** Shared with other people. Shown as a marker. */
  shared?: boolean;
  /** Opens by default. Shown with a star. */
  isDefault?: boolean;
  /** Built-in or someone else's view: it cannot be updated, renamed or deleted. */
  readOnly?: boolean;
  /** Group heading in the menu, e.g. "Built-in" or "Shared with me". Views keep their order within a group. */
  group?: string;
}

export interface SavedViewsMessages {
  /** Visually hidden prefix of the trigger, e.g. "View:". */
  triggerPrefix: string;
  /** Trigger text when no view is selected. */
  noView: string;
  /** Marker for unsaved changes (visible next to the view name). */
  modified: string;
  shared: string;
  isDefault: string;
  saveChanges: (viewName: string) => string;
  saveAsNew: string;
  discardChanges: string;
  rename: string;
  setDefault: string;
  delete: string;
  /** Menu label above the views. */
  viewsLabel: string;
  saveTitle: string;
  renameTitle: string;
  nameLabel: string;
  save: string;
  cancel: string;
  nameRequired: string;
  nameTaken: string;
  deleteTitle: (viewName: string) => string;
  deleteDescription: string;
  deleteConfirm: string;
  /** Shown in the dialog when the save callback throws. */
  saveFailed: string;
}

export const defaultSavedViewsMessages: SavedViewsMessages = {
  triggerPrefix: 'View:',
  noView: 'All items',
  modified: 'Edited',
  shared: 'Shared',
  isDefault: 'Default view',
  saveChanges: (name) => `Save changes to “${name}”`,
  saveAsNew: 'Save as new view…',
  discardChanges: 'Discard changes',
  rename: 'Rename…',
  setDefault: 'Set as default',
  delete: 'Delete view…',
  viewsLabel: 'Views',
  saveTitle: 'Save view',
  renameTitle: 'Rename view',
  nameLabel: 'Name',
  save: 'Save',
  cancel: 'Cancel',
  nameRequired: 'Enter a name.',
  nameTaken: 'A view with this name already exists.',
  deleteTitle: (name) => `Delete “${name}”?`,
  deleteDescription: 'The view is removed for everyone who can see it. Items are not affected.',
  deleteConfirm: 'Delete view',
  saveFailed: 'The view could not be saved. Try again.',
};

export interface SavedViewsProps {
  views: readonly SavedView[];
  /** Selected view id (controlled). `null` for none. */
  value?: string | null;
  /** Initially selected view id (uncontrolled). */
  defaultValue?: string | null;
  /** A view was chosen. Apply its filters, sort and columns in the app. */
  onValueChange?: (id: string) => void;
  /** The current filters, sort or columns differ from the selected view. Shows "Edited". */
  modified?: boolean;
  /** "Save as new view…": the app stores the current state under this name and selects it. */
  onSaveNew?: (name: string) => unknown;
  /** "Save changes": the app overwrites the selected view with the current state. */
  onSaveChanges?: (id: string) => unknown;
  /** "Discard changes": the app reapplies the selected view. */
  onDiscardChanges?: () => void;
  onRename?: (id: string, name: string) => unknown;
  onDelete?: (id: string) => unknown;
  onSetDefault?: (id: string) => unknown;
  /** Longest allowed name. Default 80. */
  maxNameLength?: number;
  messages?: Partial<SavedViewsMessages>;
  size?: 'xs' | 'sm' | 'md';
  disabled?: boolean;
  className?: string;
}

type NameDialog = { mode: 'save' } | { mode: 'rename'; view: SavedView } | null;

/**
 * Switcher for named views of a list (filters, sort and columns), with save,
 * save as new, rename, delete, set default and an "Edited" marker. The app
 * persists views through the callbacks; the component holds no data.
 */
export const SavedViews = forwardRef<HTMLButtonElement, SavedViewsProps>(function SavedViews(
  {
    views,
    value: valueProp,
    defaultValue = null,
    onValueChange,
    modified = false,
    onSaveNew,
    onSaveChanges,
    onDiscardChanges,
    onRename,
    onDelete,
    onSetDefault,
    maxNameLength = 80,
    messages,
    size = 'sm',
    disabled = false,
    className,
  },
  ref,
) {
  const m = { ...defaultSavedViewsMessages, ...messages };
  const [inner, setInner] = useState<string | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : inner;
  const current = views.find((v) => v.id === value);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const setTrigger = useCallback(
    (el: HTMLButtonElement | null) => {
      triggerRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    },
    [ref],
  );
  const [nameDialog, setNameDialog] = useState<NameDialog>(null);
  const [deleting, setDeleting] = useState<SavedView | null>(null);

  const select = (id: string) => {
    if (valueProp === undefined) setInner(id);
    onValueChange?.(id);
  };
  const editable = !!current && !current.readOnly;

  // Views grouped in order of first appearance.
  const groups: { name: string | undefined; views: SavedView[] }[] = [];
  for (const v of views) {
    const g = groups.find((x) => x.name === v.group);
    if (g) g.views.push(v);
    else groups.push({ name: v.group, views: [v] });
  }

  const returnFocus = (e: Event) => {
    e.preventDefault();
    triggerRef.current?.focus();
  };

  const actions: ReactNode[] = [];
  if (modified && editable && onSaveChanges)
    actions.push(
      <DropdownMenuItem key="save" onSelect={() => void onSaveChanges(current.id)}>
        {m.saveChanges(current.name)}
      </DropdownMenuItem>,
    );
  if (onSaveNew)
    actions.push(
      <DropdownMenuItem key="new" onSelect={() => setNameDialog({ mode: 'save' })}>
        {m.saveAsNew}
      </DropdownMenuItem>,
    );
  if (modified && onDiscardChanges)
    actions.push(
      <DropdownMenuItem key="discard" onSelect={onDiscardChanges}>
        {m.discardChanges}
      </DropdownMenuItem>,
    );
  if (editable && onRename)
    actions.push(
      <DropdownMenuItem
        key="rename"
        onSelect={() => setNameDialog({ mode: 'rename', view: current })}
      >
        {m.rename}
      </DropdownMenuItem>,
    );
  if (current && !current.isDefault && onSetDefault)
    actions.push(
      <DropdownMenuItem key="default" onSelect={() => void onSetDefault(current.id)}>
        {m.setDefault}
      </DropdownMenuItem>,
    );
  if (editable && onDelete)
    actions.push(
      <DropdownMenuItem key="delete" tone="danger" onSelect={() => setDeleting(current)}>
        {m.delete}
      </DropdownMenuItem>,
    );

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild disabled={disabled}>
          <Button
            ref={setTrigger}
            size={size}
            variant="secondary"
            className={cx('mn-saved-views', className)}
            data-modified={modified || undefined}
            trailingIcon={<ChevronDownGlyph />}
          >
            <span className="mn-visually-hidden">{m.triggerPrefix} </span>
            <span className="mn-saved-views__name">{current?.name ?? m.noView}</span>{' '}
            {current?.count !== undefined ? (
              <span className="mn-saved-views__count">{current.count}</span>
            ) : null}{' '}
            {modified ? <span className="mn-saved-views__modified">{m.modified}</span> : null}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="mn-saved-views__menu">
          <DropdownMenuRadioGroup value={value ?? ''} onValueChange={select}>
            {groups.map((g, gi) => (
              <Fragment key={g.name ?? `group-${gi}`}>
                {gi > 0 ? <DropdownMenuSeparator /> : null}
                <DropdownMenuLabel>{g.name ?? m.viewsLabel}</DropdownMenuLabel>
                {g.views.map((v) => (
                  <DropdownMenuRadioItem key={v.id} value={v.id} className="mn-saved-views__item">
                    <span className="mn-saved-views__item-name">{v.name}</span>
                    {v.isDefault ? (
                      <span className="mn-saved-views__default">
                        <StarGlyph aria-hidden />
                        <span className="mn-visually-hidden">, {m.isDefault}</span>
                      </span>
                    ) : null}
                    {v.shared ? <span className="mn-saved-views__shared">{m.shared}</span> : null}
                    {v.count !== undefined ? (
                      <span className="mn-saved-views__item-count">{v.count}</span>
                    ) : null}
                  </DropdownMenuRadioItem>
                ))}
              </Fragment>
            ))}
          </DropdownMenuRadioGroup>
          {actions.length ? (
            <>
              <DropdownMenuSeparator />
              {actions}
            </>
          ) : null}
        </DropdownMenuContent>
      </DropdownMenu>

      <ViewNameDialog
        state={nameDialog}
        views={views}
        maxLength={maxNameLength}
        messages={m}
        onClose={() => setNameDialog(null)}
        onCloseAutoFocus={returnFocus}
        onSubmit={async (name) => {
          if (nameDialog?.mode === 'rename') await onRename?.(nameDialog.view.id, name);
          else await onSaveNew?.(name);
        }}
      />
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => {
          if (!open) setDeleting(null);
        }}
        tone="danger"
        title={m.deleteTitle(deleting?.name ?? '')}
        description={m.deleteDescription}
        confirmLabel={m.deleteConfirm}
        cancelLabel={m.cancel}
        onCloseAutoFocus={returnFocus}
        onConfirm={async () => {
          if (deleting) await onDelete?.(deleting.id);
        }}
      />
    </>
  );
});

function ViewNameDialog({
  state,
  views,
  maxLength,
  messages: m,
  onClose,
  onSubmit,
  onCloseAutoFocus,
}: {
  state: NameDialog;
  views: readonly SavedView[];
  maxLength: number;
  messages: SavedViewsMessages;
  onClose: () => void;
  onSubmit: (name: string) => Promise<void>;
  onCloseAutoFocus: (e: Event) => void;
}) {
  const open = state !== null;
  const formId = useId();
  const initial = state?.mode === 'rename' ? state.view.name : '';
  const [name, setName] = useState(initial);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [lastState, setLastState] = useState<NameDialog>(null);
  if (state !== lastState) {
    // Reset the form each time the dialog opens for a new purpose.
    setLastState(state);
    setName(initial);
    setError(null);
    setBusy(false);
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return setError(m.nameRequired);
    const clash = views.some(
      (v) =>
        v.name.trim().toLowerCase() === trimmed.toLowerCase() &&
        !(state?.mode === 'rename' && v.id === state.view.id),
    );
    if (clash) return setError(m.nameTaken);
    setBusy(true);
    setError(null);
    try {
      await onSubmit(trimmed);
      setBusy(false);
      onClose();
    } catch {
      setBusy(false);
      setError(m.saveFailed);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o) onClose();
      }}
    >
      <DialogContent
        size="sm"
        busy={busy}
        closeOnOutsideClick={false}
        onCloseAutoFocus={onCloseAutoFocus}
        aria-describedby={undefined}
        footer={
          <>
            <Button variant="ghost" disabled={busy} onClick={onClose}>
              {m.cancel}
            </Button>
            <Button variant="primary" type="submit" form={formId} loading={busy}>
              {m.save}
            </Button>
          </>
        }
      >
        <DialogHeader>
          <DialogTitle>{state?.mode === 'rename' ? m.renameTitle : m.saveTitle}</DialogTitle>
        </DialogHeader>
        <DialogBody>
          <form id={formId} noValidate onSubmit={submit}>
            <Field invalid={error !== null} required>
              <FieldLabel>{m.nameLabel}</FieldLabel>
              <Input
                value={name}
                maxLength={maxLength}
                autoComplete="off"
                onChange={(e) => {
                  setName(e.target.value);
                  if (error) setError(null);
                }}
              />
              {error ? <FieldError>{error}</FieldError> : null}
            </Field>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
