'use client';

import { forwardRef, useState, type ElementRef, type ReactElement, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  type DialogContentProps,
} from '../../../components/overlays/dialog/index.js';
import {
  WorkspaceOnboardingForm,
  type WorkspaceOnboardingFormProps,
  type WorkspaceOnboardingValues,
} from '../workspace-onboarding-form/index.js';

/** Visible strings, for translation. */
export interface CreateWorkspaceDialogMessages {
  /** Dialog title. Default "Create new workspace". */
  title: string;
  /** Submit button. Default "Create workspace". */
  submit: string;
  /** Default "Cancel". */
  cancel: string;
  /** Close button label. Default "Close". */
  close: string;
}

export const defaultCreateWorkspaceDialogMessages: CreateWorkspaceDialogMessages = {
  title: 'Create new workspace',
  submit: 'Create workspace',
  cancel: 'Cancel',
  close: 'Close',
};

type FormProps = Pick<
  WorkspaceOnboardingFormProps,
  | 'onSubmit'
  | 'onSuccess'
  | 'checkUsername'
  | 'types'
  | 'defaultValues'
  | 'rules'
  | 'usernamePrefix'
  | 'errorMessage'
>;

export interface CreateWorkspaceDialogProps
  extends
    FormProps,
    Omit<DialogContentProps, 'title' | 'children' | 'footer' | 'busy' | 'onSubmit'> {
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement | undefined;
  /** Overrides the title. */
  title?: ReactNode;
  /** A line under the title. */
  description?: ReactNode;
  /** Close after `onSubmit` succeeds. Default `true`. */
  closeOnSuccess?: boolean | undefined;
  /** Strings for the form fields. */
  formMessages?: WorkspaceOnboardingFormProps['messages'];
  /** Strings for the dialog. */
  messages?: Partial<CreateWorkspaceDialogMessages> | undefined;
}

/**
 * A dialog that creates a workspace with WorkspaceOnboardingForm: name,
 * username with an availability check and type. It cannot be dismissed while
 * the workspace is being created, and closes when creation succeeds.
 */
export const CreateWorkspaceDialog = forwardRef<
  ElementRef<typeof DialogContent>,
  CreateWorkspaceDialogProps
>(function CreateWorkspaceDialog(
  {
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    trigger,
    title,
    description,
    closeOnSuccess = true,
    onSubmit,
    onSuccess,
    checkUsername,
    types,
    defaultValues,
    rules,
    usernamePrefix,
    errorMessage,
    formMessages,
    messages,
    size = 'md',
    closeOnOutsideClick = false,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultCreateWorkspaceDialogMessages, ...messages };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [pending, setPending] = useState(false);

  const setOpen = (next: boolean) => {
    if (!next && pending) return;
    if (openProp === undefined) setOpenState(next);
    onOpenChange?.(next);
  };

  const submit = async (values: WorkspaceOnboardingValues) => {
    const result = onSubmit(values);
    if (!isThenable(result)) return result;
    setPending(true);
    try {
      return await result;
    } finally {
      setPending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
      <DialogContent
        ref={ref}
        {...rest}
        size={size}
        busy={pending}
        closeLabel={m.close}
        closeOnOutsideClick={closeOnOutsideClick}
        className={cx('mn-create-workspace-dialog', className)}
      >
        <DialogHeader>
          <DialogTitle>{title ?? m.title}</DialogTitle>
          {description ? <DialogDescription>{description}</DialogDescription> : null}
        </DialogHeader>
        <DialogBody className="mn-create-workspace-dialog__body">
          <WorkspaceOnboardingForm
            onSubmit={submit}
            onSuccess={(result, values) => {
              onSuccess?.(result, values);
              if (closeOnSuccess) {
                if (openProp === undefined) setOpenState(false);
                onOpenChange?.(false);
              }
            }}
            {...(checkUsername ? { checkUsername } : {})}
            {...(types ? { types } : {})}
            {...(defaultValues ? { defaultValues } : {})}
            {...(rules ? { rules } : {})}
            {...(usernamePrefix !== undefined ? { usernamePrefix } : {})}
            {...(errorMessage ? { errorMessage } : {})}
            {...(formMessages ? { messages: formMessages } : {})}
            submitLabel={m.submit}
            secondaryAction={
              <DialogClose asChild>
                <Button disabled={pending}>{m.cancel}</Button>
              </DialogClose>
            }
          />
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
});
