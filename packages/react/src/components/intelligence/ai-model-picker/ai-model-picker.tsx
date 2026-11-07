'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ChevronDownGlyph, LockGlyph } from '../../../internal/icons.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';

/** What a model can do. */
export type AiModelCapability = 'vision' | 'tools' | 'reasoning' | 'web';

/** One model the person can pick. */
export interface AiModelOption {
  id: string;
  /** Provider or family the model is grouped under, e.g. "Rook" or a vendor name. */
  provider: string;
  /** Display name. */
  name: string;
  /** One short line, e.g. "Best for long documents". */
  description?: string | undefined;
  /** Capability badges. */
  capabilities: readonly AiModelCapability[];
  /** Context window in tokens, shown compactly ("200K context"). */
  contextTokens?: number | undefined;
  /** Relative speed. */
  speed?: 'fast' | 'balanced' | 'slow' | undefined;
  /** Relative cost, 1 (lowest) to 3 (highest), shown as $, $$ or $$$. */
  costTier?: 1 | 2 | 3 | undefined;
  /** The model runs on the workspace's own provider key (bring your own key). */
  requiresKey?: boolean | undefined;
  /** Whether that key is set up. A model that needs a missing key cannot be picked. */
  keyConfigured?: boolean | undefined;
  /** Not available right now (e.g. not in the plan). */
  disabled?: boolean | undefined;
}

export interface AiModelPickerMessages {
  /** Name of the trigger and the list. */
  label: string;
  /** Trigger text when nothing is selected. */
  placeholder: string;
  /** Capability badge labels. */
  capabilities: Record<AiModelCapability, string>;
  /** Context window, e.g. "200K context". Receives the compact number. */
  context: (formatted: string) => string;
  /** Speed labels. */
  speed: Record<'fast' | 'balanced' | 'slow', string>;
  /** Read for the cost tier, e.g. "Cost: medium". */
  cost: (tier: 1 | 2 | 3) => string;
  /** Badge when the model uses a configured workspace key. */
  ownKey: string;
  /** Badge when the model needs a key that is not configured. */
  keyNeeded: string;
  /** Description read on a key-needed option when `onConfigureKey` is set. */
  addKeyHint: string;
  /** Read on an option that cannot be picked. */
  unavailable: string;
}

export const AI_MODEL_PICKER_MESSAGES: AiModelPickerMessages = {
  label: 'Model',
  placeholder: 'Choose a model',
  capabilities: { vision: 'Vision', tools: 'Tools', reasoning: 'Reasoning', web: 'Web' },
  context: (n) => `${n} context`,
  speed: { fast: 'Fast', balanced: 'Balanced', slow: 'Slower' },
  cost: (tier) => `Cost: ${['low', 'medium', 'high'][tier - 1]}`,
  ownKey: 'Your key',
  keyNeeded: 'Key needed',
  addKeyHint: 'Select to add an API key.',
  unavailable: 'Unavailable',
};

export interface AiModelPickerProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'value' | 'onChange' | 'defaultValue'
> {
  /** The models, in display order. Grouped by `provider` in first-seen order. */
  models: readonly AiModelOption[];
  /** The selected model id. */
  value: string | undefined;
  /** Called with the picked model's id (and the model). */
  onChange: (id: string, model: AiModelOption) => void;
  /**
   * Called when someone picks a model whose key is not configured. Without it,
   * such models are shown but cannot be picked.
   */
  onConfigureKey?: ((model: AiModelOption) => void) | undefined;
  /** Controlled open state of the list. */
  open?: boolean | undefined;
  /** Called when the list opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Trigger size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Locale for compact numbers. Default: the runtime's. */
  locale?: string | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiModelPickerMessages> | undefined;
}

type OptionState = 'ok' | 'key' | 'blocked';

function optionState(model: AiModelOption, canConfigure: boolean): OptionState {
  if (model.disabled) return 'blocked';
  if (model.requiresKey && !model.keyConfigured) return canConfigure ? 'key' : 'blocked';
  return 'ok';
}

/**
 * Lets people choose which model answers: a compact trigger showing the
 * current model, opening a list grouped by provider with capability badges
 * (vision, tools, reasoning, web), context size, speed and cost, and the
 * state of bring-your-own provider keys. Models that need a missing key lead
 * to `onConfigureKey` instead of being picked.
 */
export const AiModelPicker = forwardRef<HTMLButtonElement, AiModelPickerProps>(
  function AiModelPicker(
    {
      models,
      value,
      onChange,
      onConfigureKey,
      open: openProp,
      onOpenChange,
      size = 'md',
      locale,
      messages: messagesProp,
      className,
      disabled,
      onKeyDown,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...AI_MODEL_PICKER_MESSAGES,
      ...messagesProp,
      capabilities: { ...AI_MODEL_PICKER_MESSAGES.capabilities, ...messagesProp?.capabilities },
      speed: { ...AI_MODEL_PICKER_MESSAGES.speed, ...messagesProp?.speed },
    };
    const baseId = useId();
    const labelId = `${baseId}-label`;
    const valueId = `${baseId}-value`;
    const listRef = useRef<HTMLDivElement>(null);
    const typeahead = useRef({ text: '', at: 0 });
    const [openState, setOpenState] = useState(false);
    const open = openProp ?? openState;
    const [active, setActive] = useState(-1);

    const selected = models.find((x) => x.id === value);
    const groups = useMemo(() => {
      const map = new Map<string, { model: AiModelOption; index: number }[]>();
      models.forEach((model, index) => {
        const list = map.get(model.provider) ?? [];
        list.push({ model, index });
        map.set(model.provider, list);
      });
      return [...map.entries()];
    }, [models]);
    const compact = useMemo(
      () => new Intl.NumberFormat(locale, { notation: 'compact', maximumFractionDigits: 1 }),
      [locale],
    );

    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    };

    // Highlight the selected model (or the first) whenever the list opens.
    useEffect(() => {
      if (!open) return;
      const i = models.findIndex((x) => x.id === value);
      setActive(i >= 0 ? i : models.length ? 0 : -1);
      // Only on open.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    useEffect(() => {
      if (!open || active < 0) return;
      const el = listRef.current?.querySelector<HTMLElement>(`[data-index="${active}"]`);
      el?.scrollIntoView?.({ block: 'nearest' });
    }, [open, active]);

    const choose = (index: number) => {
      const model = models[index];
      if (!model) return;
      const state = optionState(model, !!onConfigureKey);
      if (state === 'blocked') return;
      setOpen(false);
      if (state === 'key') onConfigureKey?.(model);
      else onChange(model.id, model);
    };

    const onListKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
      const last = models.length - 1;
      let next: number | null = null;
      switch (event.key) {
        case 'ArrowDown':
          next = Math.min(last, active + 1);
          break;
        case 'ArrowUp':
          next = Math.max(0, active - 1);
          break;
        case 'Home':
          next = 0;
          break;
        case 'End':
          next = last;
          break;
        case 'Enter':
        case ' ':
          event.preventDefault();
          choose(active);
          return;
        case 'Tab':
          setOpen(false);
          return;
        default:
          if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
            const now = Date.now();
            const t = typeahead.current;
            t.text = now - t.at > 700 ? event.key.toLowerCase() : t.text + event.key.toLowerCase();
            t.at = now;
            const order = models.map((_, i) => (active + 1 + i) % models.length);
            const hit = (t.text.length > 1 ? [active, ...order] : order).find((i) =>
              models[i]?.name.toLowerCase().startsWith(t.text),
            );
            if (hit !== undefined && hit >= 0) next = hit;
          }
      }
      if (next !== null) {
        event.preventDefault();
        setActive(next);
      }
    };

    const optionId = (i: number) => `${baseId}-opt-${i}`;

    return (
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            ref={ref}
            type="button"
            aria-haspopup="listbox"
            aria-labelledby={`${labelId} ${valueId}`}
            disabled={disabled}
            {...rest}
            onKeyDown={(event) => {
              onKeyDown?.(event);
              if (
                !event.defaultPrevented &&
                (event.key === 'ArrowDown' || event.key === 'ArrowUp')
              ) {
                event.preventDefault();
                setOpen(true);
              }
            }}
            className={cx('mn-ai-model-picker', className)}
            data-size={size}
          >
            <span id={labelId} className="mn-visually-hidden">
              {m.label}
            </span>
            <span id={valueId} className="mn-ai-model-picker__value">
              {selected ? (
                <>
                  <span className="mn-ai-model-picker__name">{selected.name}</span>
                  <span className="mn-ai-model-picker__provider">{selected.provider}</span>
                </>
              ) : (
                <span className="mn-ai-model-picker__placeholder">{m.placeholder}</span>
              )}
            </span>
            <ChevronDownGlyph aria-hidden="true" className="mn-ai-model-picker__chevron" />
          </button>
        </PopoverTrigger>
        <PopoverContent
          aria-label={m.label}
          className="mn-ai-model-picker__popover"
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            listRef.current?.focus();
          }}
        >
          <div
            ref={listRef}
            role="listbox"
            aria-label={m.label}
            tabIndex={0}
            aria-activedescendant={active >= 0 ? optionId(active) : undefined}
            className="mn-ai-model-picker__list"
            onKeyDown={onListKeyDown}
          >
            {groups.map(([provider, items]) => {
              const groupId = `${baseId}-group-${items[0]?.index ?? 0}`;
              return (
                <div
                  key={provider}
                  role="group"
                  aria-labelledby={groupId}
                  className="mn-ai-model-picker__group"
                >
                  <div id={groupId} className="mn-ai-model-picker__group-label" role="presentation">
                    {provider}
                  </div>
                  {items.map(({ model, index }) => {
                    const state = optionState(model, !!onConfigureKey);
                    const isSelected = model.id === value;
                    const descId = `${optionId(index)}-desc`;
                    const meta = [
                      model.contextTokens ? m.context(compact.format(model.contextTokens)) : null,
                      model.speed ? m.speed[model.speed] : null,
                    ].filter(Boolean);
                    return (
                      // Keyboard lives on the listbox (aria-activedescendant); options are not focusable.
                      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
                      <div
                        key={model.id}
                        id={optionId(index)}
                        role="option"
                        aria-selected={isSelected}
                        aria-disabled={state === 'blocked' || undefined}
                        aria-labelledby={`${optionId(index)}-name${model.requiresKey ? ` ${optionId(index)}-key` : ''}`}
                        aria-describedby={descId}
                        data-index={index}
                        data-active={index === active || undefined}
                        data-state={state}
                        className="mn-ai-model-picker__option"
                        onPointerMove={() => setActive(index)}
                        onClick={() => choose(index)}
                      >
                        <span className="mn-ai-model-picker__check" aria-hidden="true">
                          {isSelected ? <CheckGlyph /> : null}
                        </span>
                        <span className="mn-ai-model-picker__body">
                          <span className="mn-ai-model-picker__row">
                            <span
                              id={`${optionId(index)}-name`}
                              className="mn-ai-model-picker__option-name"
                            >
                              {model.name}
                            </span>
                            {model.requiresKey ? (
                              <span
                                id={`${optionId(index)}-key`}
                                className="mn-ai-model-picker__key"
                                data-configured={model.keyConfigured || undefined}
                              >
                                {model.keyConfigured ? null : <LockGlyph aria-hidden="true" />}
                                {model.keyConfigured ? m.ownKey : m.keyNeeded}
                              </span>
                            ) : null}
                          </span>
                          <span id={descId} className="mn-ai-model-picker__details">
                            {model.description ? (
                              <span className="mn-ai-model-picker__description">
                                {model.description}
                              </span>
                            ) : null}
                            {model.capabilities.length ? (
                              <span className="mn-ai-model-picker__caps">
                                {model.capabilities.map((c) => (
                                  <span
                                    key={c}
                                    className="mn-ai-model-picker__cap"
                                    data-capability={c}
                                  >
                                    {m.capabilities[c]}
                                  </span>
                                ))}
                              </span>
                            ) : null}
                            {meta.length || model.costTier ? (
                              <span className="mn-ai-model-picker__meta">
                                {meta.join(' · ')}
                                {model.costTier ? (
                                  <>
                                    {meta.length ? ' · ' : null}
                                    <span aria-hidden="true">{'$'.repeat(model.costTier)}</span>
                                    <span className="mn-visually-hidden">
                                      {m.cost(model.costTier)}
                                    </span>
                                  </>
                                ) : null}
                              </span>
                            ) : null}
                            {state === 'key' ? (
                              <span className="mn-visually-hidden">{m.addKeyHint}</span>
                            ) : state === 'blocked' ? (
                              <span className="mn-visually-hidden">{m.unavailable}</span>
                            ) : null}
                          </span>
                        </span>
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </PopoverContent>
      </Popover>
    );
  },
);
