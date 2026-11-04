'use client';

import { forwardRef, useMemo, useRef, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar, AvatarGroup, type AvatarStatus } from '../avatar/index.js';
import type {
  ComboboxMessages,
  ComboboxOption,
  ComboboxSearchContext,
} from '../../forms/combobox/index.js';
import { EntityPicker, type EntityPickerProps } from '../../forms/entity-picker/index.js';

/** A person the picker can assign. The product maps its own user records to this shape. */
export interface AssigneePerson {
  id: string;
  name: string;
  /** Shown under the name and matched by search. */
  email?: string;
  avatarUrl?: string | null;
  /** Presence dot on the avatar. */
  status?: AvatarStatus;
  /** Short trailing text, such as "3 open" or "Away". */
  hint?: string;
  /** Group heading, such as "In this team". */
  group?: string;
  disabled?: boolean;
}

/** Interface text, for translation. */
export interface AssigneePickerMessages {
  /** The clear option and the empty trigger. Default "Unassigned". */
  unassigned: string;
  /** The current user's label. Default `${name} (you)`. */
  you: (name: string) => string;
  /** Footer shortcut. Default "Assign to me". */
  assignToMe: string;
  /** Trigger text for several assignees. Default `${count} people`. */
  count: (count: number) => string;
  /** Search box placeholder. Default "Assign to…". */
  search: string;
}

const DEFAULT_MESSAGES: AssigneePickerMessages = {
  unassigned: 'Unassigned',
  you: (name) => `${name} (you)`,
  assignToMe: 'Assign to me',
  count: (n) => `${n} people`,
  search: 'Assign to…',
};

export interface AssigneePickerProps extends Omit<
  EntityPickerProps,
  'options' | 'onSearch' | 'valueOptions' | 'clearOption' | 'onCreate' | 'renderValue' | 'messages'
> {
  /** The people to choose from, such as workspace members. */
  people?: AssigneePerson[] | undefined;
  /** Search people on a server instead of filtering `people`. Debounced and aborted like Combobox `onSearch`. */
  onSearch?:
    ((query: string, context: ComboboxSearchContext) => Promise<AssigneePerson[]>) | undefined;
  /** People for the current value(s) when they may not be in `people`. Used for names and avatars. */
  valuePeople?: AssigneePerson[] | undefined;
  /** The signed-in user: listed first (within their group) as "(you)", with an "Assign to me" shortcut. */
  currentUserId?: string | null | undefined;
  /** Offer an "Unassigned" option that clears the selection. Default true. */
  allowUnassigned?: boolean | undefined;
  /** Show the "Assign to me" footer shortcut when `currentUserId` is set. Default true. */
  showAssignToMe?: boolean | undefined;
  /** Avatars shown in the trigger before a "+N" tile, with `multiple`. Default 3. */
  maxAvatars?: number | undefined;
  /** Interface text, for translation. */
  messages?: (Partial<AssigneePickerMessages> & Partial<ComboboxMessages>) | undefined;
}

function UnassignedGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeDasharray="2 2"
      aria-hidden
      focusable={false}
    >
      <circle cx="8" cy="8" r="6.25" />
    </svg>
  );
}

/**
 * People picker: assign one or several people from a searchable list with
 * avatars, the current user first, an "Unassigned" option and an "Assign to
 * me" shortcut. The trigger shows the assignee, or an avatar stack for
 * several. Built on EntityPicker and Avatar; the product supplies the people.
 */
export const AssigneePicker = forwardRef<HTMLButtonElement, AssigneePickerProps>(
  function AssigneePicker(
    {
      people = [],
      onSearch,
      valuePeople,
      currentUserId = null,
      allowUnassigned = true,
      showAssignToMe = true,
      maxAvatars = 3,
      messages: messagesProp,
      multiple = false,
      value: valueProp,
      defaultValue = null,
      onValueChange,
      values: valuesProp,
      defaultValues,
      onValuesChange,
      placeholder,
      searchPlaceholder,
      footer,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
    const [internalValues, setInternalValues] = useState<string[]>(defaultValues ?? []);
    const value = valueProp !== undefined ? valueProp : internalValue;
    const values = valuesProp ?? internalValues;

    // Everyone the picker has been given, for trigger names and avatars.
    const known = useRef(new Map<string, AssigneePerson>());
    for (const p of valuePeople ?? []) known.current.set(p.id, p);
    for (const p of people) known.current.set(p.id, p);

    const toOption = (p: AssigneePerson): ComboboxOption => ({
      value: p.id,
      label: p.id === currentUserId ? messages.you(p.name) : p.name,
      ...(p.email ? { description: p.email, keywords: `${p.name} ${p.email}` } : {}),
      ...(p.hint ? { hint: p.hint } : {}),
      ...(p.group ? { group: p.group } : {}),
      ...(p.disabled ? { disabled: true } : {}),
      media: (
        <Avatar
          name={p.name}
          src={p.avatarUrl ?? null}
          seed={p.id}
          size="xs"
          decorative
          {...(p.status ? { status: p.status } : {})}
        />
      ),
    });

    // The current user first within their group; everyone else keeps the product's order.
    const meFirst = (list: AssigneePerson[]) => {
      const me = list.find((p) => p.id === currentUserId);
      if (!me) return list;
      const rest = list.filter((p) => p !== me);
      const at = rest.findIndex((p) => (p.group ?? '') === (me.group ?? ''));
      return at < 0 ? [...rest, me] : [...rest.slice(0, at), me, ...rest.slice(at)];
    };

    const options = useMemo(
      () => meFirst(people).map(toOption),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [people, currentUserId, messages.you],
    );
    const valueOptions = useMemo(
      () => (valuePeople ?? []).map(toOption),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [valuePeople, currentUserId, messages.you],
    );
    const search = onSearch
      ? async (q: string, ctx: ComboboxSearchContext) => {
          const found = await onSearch(q, ctx);
          for (const p of found) known.current.set(p.id, p);
          return meFirst(found).map(toOption);
        }
      : undefined;

    const setValue = (next: string | null) => {
      if (valueProp === undefined) setInternalValue(next);
      onValueChange?.(next);
    };
    const setValues = (next: string[]) => {
      if (valuesProp === undefined) setInternalValues(next);
      onValuesChange?.(next);
    };

    const isMeSelected = currentUserId
      ? multiple
        ? values.includes(currentUserId)
        : value === currentUserId
      : true;

    const avatarOf = (o: ComboboxOption, size: 'xs' | undefined) => {
      const p = known.current.get(o.value);
      return (
        <Avatar
          key={o.value}
          name={p?.name ?? o.label}
          src={p?.avatarUrl ?? null}
          seed={o.value}
          decorative
          {...(size ? { size } : {})}
        />
      );
    };

    const renderValue = (selected: ComboboxOption[]): ReactNode => {
      const first = selected[0];
      if (selected.length === 1 && first) {
        return (
          <>
            <span className="mn-assignee-picker__avatars" aria-hidden>
              {avatarOf(first, 'xs')}
            </span>
            <span className="mn-assignee-picker__name">{first.label}</span>
          </>
        );
      }
      return (
        <>
          <span className="mn-assignee-picker__avatars" aria-hidden>
            <AvatarGroup label={messages.count(selected.length)} size="xs" max={maxAvatars}>
              {selected.map((o) => avatarOf(o, undefined))}
            </AvatarGroup>
          </span>
          <span className="mn-assignee-picker__name">{messages.count(selected.length)}</span>
        </>
      );
    };

    const meShortcut =
      currentUserId && showAssignToMe && !isMeSelected
        ? (close: () => void) => (
            <button
              type="button"
              className="mn-assignee-picker__me"
              onClick={() => {
                if (multiple) setValues([...values, currentUserId]);
                else {
                  setValue(currentUserId);
                  close();
                }
              }}
            >
              {messages.assignToMe}
            </button>
          )
        : null;

    return (
      <EntityPicker
        ref={ref}
        {...rest}
        className={cx('mn-assignee-picker', className)}
        multiple={multiple}
        value={multiple ? undefined : value}
        onValueChange={setValue}
        values={multiple ? values : undefined}
        onValuesChange={setValues}
        options={options}
        onSearch={search}
        valueOptions={valueOptions}
        clearOption={
          allowUnassigned ? { label: messages.unassigned, media: <UnassignedGlyph /> } : undefined
        }
        placeholder={placeholder ?? (allowUnassigned ? messages.unassigned : undefined)}
        searchPlaceholder={searchPlaceholder ?? messages.search}
        renderValue={renderValue}
        messages={messages}
        footer={
          meShortcut || footer
            ? (ctx) => (
                <>
                  {meShortcut ? meShortcut(ctx.close) : null}
                  {typeof footer === 'function' ? footer(ctx) : footer}
                </>
              )
            : undefined
        }
      />
    );
  },
);
