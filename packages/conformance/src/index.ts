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
export {
  toJson,
  toSarif,
  toText,
  ruleInfo,
  ruleDocsUrl,
  sarifFix,
  RULES_DOCS_URL,
  type SarifFix,
} from './report.js';
export { applyFixes, applyEdits, publicPathOf, colourTokenOf, type FixSummary } from './fixes.js';
export {
  loadManifest,
  findExport,
  type Manifest,
  type ManifestExport,
  type ManifestProp,
} from './manifest.js';
export { search, scoreExport, type SearchHit, type SearchOptions } from './search.js';
export { planAdd, exampleOf, writeCss, type AddPlan } from './add.js';
export { createTools, handleMessage, runStdio, PROTOCOL_VERSIONS, type McpContext } from './mcp.js';
export { initAgents, templatesDir, type InitAgentsResult } from './init-agents.js';
export {
  createProject,
  startersDir,
  isStarterTemplate,
  normaliseProjectName,
  CreateError,
  STARTER_TEMPLATES,
  type CreateOptions,
  type CreateResult,
  type StarterTemplate,
} from './create.js';
export {
  generateFigmaConnect,
  parseFigmaMap,
  codeConnectSource,
  FigmaConnectError,
  type FigmaMap,
  type FigmaMapEntry,
  type FigmaConnectResult,
} from './figma-connect.js';
export {
  scanImportPaths,
  movedImports,
  isMovedName,
  type ImportPathFinding,
  type ImportedName,
} from './import-paths.js';
export { main, HELP } from './commands.js';
export {
  BUNDLED_COMPATIBILITY,
  BUNDLED_DEPRECATIONS,
  BUNDLED_IMPORT_PATH_DEPRECATIONS,
  BUNDLED_PRODUCTS,
  COLOUR_TOKENS,
} from './bundled-data.js';
export {
  validateRegistry,
  verifyProduct,
  classifyAdoption,
  classifyCompatibility,
  detectFramework,
  statusMarkdown,
  loadProducts,
  loadCompatibility,
  loadDeprecations,
  loadImportPathDeprecations,
  type Product,
  type ProductStatus,
  type AdoptionStatus,
  type CompatibilityStatus,
} from './registry.js';
export { satisfies, minVersion, parseVersion, parseRange } from './semver.js';
export { MONARCH_COMPONENTS } from './monarch-components.js';
export { PUBLIC_MONARCH_VARIABLES, isPublicMonarchVariable } from './public-variables.js';
export { VERSION } from './version.js';
export type * from './types.js';
