/**
 * `@rookcom/monarch-react/patterns`: composed patterns such as steppers, record headers and onboarding checklists. Kept out of the root entry so apps that
 * do not need it never load it. Components are added here by wave 4, 6 and 8
 * (registry/roadmap-waves.json).
 */
export {};
export {
  Stepper,
  stepStatus,
  type StepperProps,
  type StepperStep,
  type StepperStepStatus,
  type StepperMessages,
} from '../components/workflow/stepper/index.js';
// Workflow (wave 6 group B): onboarding-checklist.
export {
  OnboardingChecklist,
  type OnboardingChecklistProps,
  type OnboardingStep,
  type OnboardingChecklistMessages,
} from '../components/workflow/onboarding-checklist/index.js';
// Flow patterns (wave 8, group B): custom-domain-connect, member-row, schema-driven-form, template-picker.
export {
  CustomDomainConnect,
  normalizeDomain,
  domainSyntaxError,
  type CustomDomainConnectProps,
  type CustomDomainStatus,
  type CustomDomainRecord,
  type CustomDomainProviderHint,
  type CustomDomainMessages,
  type CustomDomainMessagesInput,
  type DomainSyntaxError,
} from '../patterns/technical/custom-domain-connect/index.js';
export {
  MemberRow,
  MemberList,
  type MemberRowProps,
  type MemberListProps,
  type MemberStatus,
  type MemberRowMessages,
} from '../components/identity/member-row/index.js';
export {
  SchemaDrivenForm,
  schemaDefaultValues,
  isSchemaFieldVisible,
  validateSchemaField,
  validateSchemaForm,
  formatSchemaValue,
  type SchemaDrivenFormProps,
  type SchemaDrivenFormSchema,
  type SchemaDrivenFormSection,
  type SchemaDrivenFormField,
  type SchemaDrivenFormFieldType,
  type SchemaDrivenFormValue,
  type SchemaDrivenFormValues,
  type SchemaDrivenFormCondition,
  type SchemaDrivenFormMessages,
} from '../components/forms/schema-driven-form/index.js';
export {
  TemplatePicker,
  templateMatches,
  type TemplatePickerProps,
  type TemplatePickerTemplate,
  type TemplatePickerCategory,
  type TemplatePickerBlank,
  type TemplatePickerMessages,
} from '../components/forms/template-picker/index.js';
export {
  SectionCard,
  type SectionCardProps,
  type SectionCardState,
} from '../components/layout/section-card/index.js';
export {
  RecordHeader,
  type RecordHeaderProps,
  type RecordHeaderFact,
  type RecordHeaderNavigator,
} from '../patterns/crm/record-header/index.js';
export {
  SplitInboxLayout,
  SplitInboxContextToggle,
  useSplitInboxLayout,
  type SplitInboxLayoutProps,
  type SplitInboxContextToggleProps,
  type SplitInboxLayoutState,
  type SplitInboxPane,
} from '../patterns/layout/split-inbox-layout/index.js';
