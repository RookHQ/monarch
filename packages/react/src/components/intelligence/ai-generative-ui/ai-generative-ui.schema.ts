/**
 * A small JSON Schema subset for generative UI props: enough to say what a
 * registered component accepts, check what the model sent, strip what it
 * may not send, and describe the registry to the model in a system prompt.
 * Supported: `type` (one or several), `enum`, `const`, `properties`,
 * `required`, `additionalProperties`, `items`, `minItems`, `maxItems`,
 * `minLength`, `maxLength`, `pattern`, `minimum`, `maximum`, `format: 'uri'`,
 * and `description`. Anything else is ignored.
 */
import { isSafeUrl } from '../../../internal/sanitize.js';

export type AiUiJsonType =
  'string' | 'number' | 'integer' | 'boolean' | 'object' | 'array' | 'null';

/** The JSON Schema subset generative UI understands. */
export interface AiUiJsonSchema {
  type?: AiUiJsonType | readonly AiUiJsonType[];
  description?: string;
  enum?: readonly unknown[];
  const?: unknown;
  properties?: Readonly<Record<string, AiUiJsonSchema>>;
  required?: readonly string[];
  /** `false` strips unknown keys (the default for generative UI); a schema checks them. */
  additionalProperties?: boolean | AiUiJsonSchema;
  items?: AiUiJsonSchema;
  minItems?: number;
  maxItems?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  minimum?: number;
  maximum?: number;
  /** `uri`: an http(s), mailto or relative URL only. */
  format?: 'uri';
}

export type AiUiSchemaResult = { ok: true; value: unknown } | { ok: false; errors: string[] };

export interface AiUiSchemaOptions {
  /**
   * Props still streaming: `required` is not enforced and `minItems` /
   * `minLength` are relaxed, but every prop that is there is checked.
   */
  partial?: boolean;
}

const typeOf = (v: unknown): AiUiJsonType =>
  v === null
    ? 'null'
    : Array.isArray(v)
      ? 'array'
      : typeof v === 'number'
        ? Number.isInteger(v)
          ? 'integer'
          : 'number'
        : (typeof v as AiUiJsonType);

function typeMatches(value: unknown, want: AiUiJsonType): boolean {
  const actual = typeOf(value);
  return actual === want || (want === 'number' && actual === 'integer');
}

const deepEqual = (a: unknown, b: unknown): boolean => JSON.stringify(a) === JSON.stringify(b);

function check(
  schema: AiUiJsonSchema,
  value: unknown,
  path: string,
  errors: string[],
  partial: boolean,
): unknown {
  const at = path || 'props';
  if (schema.type !== undefined) {
    const types = (Array.isArray(schema.type) ? schema.type : [schema.type]) as AiUiJsonType[];
    if (!types.some((t) => typeMatches(value, t))) {
      errors.push(`${at}: expected ${types.join(' or ')}`);
      return undefined;
    }
  }
  if (schema.const !== undefined && !deepEqual(schema.const, value)) {
    errors.push(`${at}: must be ${JSON.stringify(schema.const)}`);
    return undefined;
  }
  if (schema.enum && !schema.enum.some((e) => deepEqual(e, value))) {
    errors.push(`${at}: not one of the allowed values`);
    return undefined;
  }
  if (typeof value === 'string') {
    if (!partial && schema.minLength !== undefined && value.length < schema.minLength)
      errors.push(`${at}: shorter than ${schema.minLength}`);
    if (schema.maxLength !== undefined && value.length > schema.maxLength)
      errors.push(`${at}: longer than ${schema.maxLength}`);
    if (schema.pattern !== undefined) {
      let re: RegExp | null = null;
      try {
        re = new RegExp(schema.pattern, 'u');
      } catch {
        re = null;
      }
      if (!re || (!partial && !re.test(value))) errors.push(`${at}: does not match the pattern`);
    }
    if (schema.format === 'uri' && !isSafeUrl(value)) errors.push(`${at}: not an allowed URL`);
    return value;
  }
  if (typeof value === 'number') {
    if (schema.minimum !== undefined && value < schema.minimum)
      errors.push(`${at}: below ${schema.minimum}`);
    if (schema.maximum !== undefined && value > schema.maximum)
      errors.push(`${at}: above ${schema.maximum}`);
    return value;
  }
  if (Array.isArray(value)) {
    if (!partial && schema.minItems !== undefined && value.length < schema.minItems)
      errors.push(`${at}: fewer than ${schema.minItems} items`);
    if (schema.maxItems !== undefined && value.length > schema.maxItems)
      errors.push(`${at}: more than ${schema.maxItems} items`);
    const items = schema.items;
    return items ? value.map((v, i) => check(items, v, `${at}[${i}]`, errors, partial)) : value;
  }
  if (value && typeof value === 'object') {
    const obj = value as Record<string, unknown>;
    const out: Record<string, unknown> = {};
    if (!partial)
      for (const key of schema.required ?? [])
        if (!(key in obj)) errors.push(`${at}.${key}: required`);
    for (const [key, v] of Object.entries(obj)) {
      const sub = schema.properties?.[key];
      if (sub && Object.prototype.hasOwnProperty.call(schema.properties, key)) {
        out[key] = check(sub, v, `${at}.${key}`, errors, partial);
        continue;
      }
      const extra = schema.additionalProperties;
      // Unknown keys are stripped unless the schema allows them.
      if (extra === true || (extra === undefined && !schema.properties)) out[key] = v;
      else if (extra && typeof extra === 'object')
        out[key] = check(extra, v, `${at}.${key}`, errors, partial);
    }
    return out;
  }
  return value;
}

/**
 * Checks `value` against `schema`. Returns the value with unknown keys
 * stripped (where the schema lists `properties` and does not allow others),
 * or the reasons it does not fit.
 */
export function validateAiUiProps(
  schema: AiUiJsonSchema,
  value: unknown,
  options: AiUiSchemaOptions = {},
): AiUiSchemaResult {
  const errors: string[] = [];
  const out = check(schema, value, '', errors, options.partial === true);
  return errors.length ? { ok: false, errors } : { ok: true, value: out };
}

/** A one-line description of a schema, for a system prompt: `{title: string, rows?: array<…>}`. */
export function schemaSummary(schema: AiUiJsonSchema, depth = 0): string {
  if (depth > 4) return '…';
  if (schema.enum) return schema.enum.map((e) => JSON.stringify(e)).join(' | ');
  if (schema.const !== undefined) return JSON.stringify(schema.const);
  const types =
    schema.type === undefined ? [] : Array.isArray(schema.type) ? schema.type : [schema.type];
  if (types.includes('array') || schema.items)
    return `array<${schema.items ? schemaSummary(schema.items, depth + 1) : 'any'}>`;
  if (types.includes('object') || schema.properties) {
    const required = new Set(schema.required ?? []);
    const fields = Object.entries(schema.properties ?? {}).map(
      ([k, s]) => `${k}${required.has(k) ? '' : '?'}: ${schemaSummary(s, depth + 1)}`,
    );
    return `{${fields.join(', ')}}`;
  }
  return types.length ? types.join(' | ') : 'any';
}
