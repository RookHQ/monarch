export {
  check,
  makeBaseline,
  walk,
  type CheckOptions,
  type CheckResult,
  type Baseline,
  type ExceptionReport,
} from './check.js';
export { RULES, RULE_IDS, scanFile, readSuppressions, type RuleId } from './rules.js';
export { toJson, toSarif, toText } from './report.js';
export {
  validateRegistry,
  verifyProduct,
  classifyAdoption,
  classifyCompatibility,
  detectFramework,
  statusMarkdown,
  loadProducts,
  type Product,
  type ProductStatus,
  type AdoptionStatus,
  type CompatibilityStatus,
} from './registry.js';
export { satisfies, minVersion, parseVersion, parseRange } from './semver.js';
export { MONARCH_COMPONENTS } from './monarch-components.js';
export { VERSION } from './version.js';
export type * from './types.js';
