/**
 * A validator for the JSON Schema subset the registry uses: type, required,
 * properties, additionalProperties: false, pattern, enum, minLength, minItems,
 * uniqueItems, items and format: date. Keeps registry/product.schema.json the
 * single source of truth without adding a runtime dependency.
 */
export interface Schema {
  type?: 'object' | 'array' | 'string' | 'number' | 'integer' | 'boolean';
  required?: string[];
  properties?: Record<string, Schema>;
  additionalProperties?: boolean;
  pattern?: string;
  enum?: unknown[];
  minLength?: number;
  minItems?: number;
  uniqueItems?: boolean;
  items?: Schema;
  format?: string;
}

const SUPPORTED = new Set([
  '$schema',
  '$id',
  'title',
  'description',
  'type',
  'required',
  'properties',
  'additionalProperties',
  'pattern',
  'enum',
  'minLength',
  'minItems',
  'uniqueItems',
  'items',
  'format',
]);

/** Fails loudly if the schema uses a keyword this validator would silently ignore. */
export function assertSupported(schema: unknown, path = '#'): void {
  if (!schema || typeof schema !== 'object') return;
  for (const [k, v] of Object.entries(schema as Record<string, unknown>)) {
    if (!SUPPORTED.has(k)) throw new Error(`Unsupported schema keyword ${path}/${k}`);
    if (k === 'properties')
      for (const [p, s] of Object.entries(v as Record<string, unknown>))
        assertSupported(s, `${path}/properties/${p}`);
    if (k === 'items') assertSupported(v, `${path}/items`);
  }
}

function typeOf(v: unknown): string {
  if (Array.isArray(v)) return 'array';
  if (v === null) return 'null';
  if (typeof v === 'number') return Number.isInteger(v) ? 'integer' : 'number';
  return typeof v;
}

export function validate(value: unknown, schema: Schema, path = ''): string[] {
  const errors: string[] = [];
  const at = path || '(root)';
  if (schema.enum && !schema.enum.some((e) => e === value)) {
    errors.push(`${at}: must be one of ${schema.enum.map((e) => JSON.stringify(e)).join(', ')}`);
    return errors;
  }
  if (schema.type) {
    const t = typeOf(value);
    const ok = t === schema.type || (schema.type === 'number' && t === 'integer');
    if (!ok) return [`${at}: expected ${schema.type}, got ${t}`];
  }
  if (typeof value === 'string') {
    if (schema.minLength !== undefined && value.length < schema.minLength)
      errors.push(`${at}: shorter than ${schema.minLength}`);
    if (schema.pattern && !new RegExp(schema.pattern).test(value))
      errors.push(`${at}: does not match ${schema.pattern}`);
    if (schema.format === 'date' && !isDate(value))
      errors.push(`${at}: not a valid YYYY-MM-DD date`);
  }
  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems)
      errors.push(`${at}: needs at least ${schema.minItems} item(s)`);
    if (schema.uniqueItems && new Set(value.map((v) => JSON.stringify(v))).size !== value.length)
      errors.push(`${at}: items must be unique`);
    if (schema.items)
      value.forEach((v, i) => errors.push(...validate(v, schema.items as Schema, `${path}[${i}]`)));
  }
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const obj = value as Record<string, unknown>;
    for (const r of schema.required ?? [])
      if (!(r in obj)) errors.push(`${path ? path + '.' : ''}${r}: required`);
    for (const [k, v] of Object.entries(obj)) {
      const sub = schema.properties?.[k];
      if (sub) errors.push(...validate(v, sub, path ? `${path}.${k}` : k));
      else if (schema.additionalProperties === false)
        errors.push(`${path ? path + '.' : ''}${k}: unknown property`);
    }
  }
  return errors;
}

export function isDate(s: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  const d = new Date(`${s}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === s;
}
