import SwiftUI

/// A form field that AI filled in: the app's control, marked with the AI
/// badge and a confidence badge, where the value came from, and Accept and
/// Clear.
///
/// ```swift
/// MonarchAiAutofillField(
///     state: companyState,
///     suggestion: "Rook Labs",
///     fieldName: "Company",
///     source: "invoice.pdf",
///     confidence: 0.92,
///     onAccept: { companyState = .accepted },
///     onClear: { company = ""; companyState = .cleared },
///     control: { MonarchTextField("Company", text: $company) }
/// )
/// .onChange(of: company) { if companyState == .suggested || companyState == .accepted { companyState = .edited } }
/// ```
///
/// The app owns the value and the state. Set `state` to `accepted` in
/// `onAccept`, empty the control and set `cleared` in `onClear`, and set
/// `edited` when the person changes the value: the field then says "Edited
/// by you" and shows what AI first suggested. While suggested, a bar in the
/// AI border colour runs beside the control.
///
/// Accessibility: the control keeps its own label and value. The status
/// line is one element read after it ("AI suggested, High confidence, From
/// invoice.pdf"). Accept and Clear are buttons named with the field
/// ("Accept suggested Company") with 44-point touch targets, and their
/// result is announced. Nothing depends on the bar's colour.
public struct MonarchAiAutofillField<Control: View>: View {
    private let state: MonarchAiAutofillState
    private let suggestion: String
    private let fieldName: String?
    private let source: String?
    private let confidence: Double?
    private let lowThreshold: Double
    private let highThreshold: Double
    private let confidenceExplanation: String?
    private let model: String?
    private let date: Date?
    private let labels: MonarchAiAutofillFieldLabels
    private let onAccept: @MainActor () -> Void
    private let onClear: @MainActor () -> Void
    private let control: Control

    @Environment(\.monarchTheme) private var theme

    /// A field AI filled in.
    ///
    /// - Parameters:
    ///   - state: Where the field stands. Drive it from the form's state.
    ///   - suggestion: The value AI suggested, shown as "AI suggested: …" once edited.
    ///   - fieldName: The field's name, so the actions are named "Accept suggested Company".
    ///   - source: Where the value came from, such as a document name. Shown as "From …".
    ///   - confidence: How sure the AI is, 0 through 1. Shown as a confidence badge while suggested.
    ///   - lowThreshold: Below it the confidence is low ("Needs check").
    ///   - highThreshold: From it the confidence is high.
    ///   - confidenceExplanation: Why the AI chose this value, offered with the confidence badge.
    ///   - model: The model that filled the field, offered with the AI badge.
    ///   - date: When the field was filled, offered with the AI badge.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAccept: The person accepted the value. Set `state` to `accepted`.
    ///   - onClear: The person cleared the value. Empty the control and set `state` to `cleared`.
    ///   - control: The form control, usually a ``MonarchTextField`` or a ``MonarchFormField``.
    public init(
        state: MonarchAiAutofillState,
        suggestion: String,
        fieldName: String? = nil,
        source: String? = nil,
        confidence: Double? = nil,
        lowThreshold: Double = 0.6,
        highThreshold: Double = 0.85,
        confidenceExplanation: String? = nil,
        model: String? = nil,
        date: Date? = nil,
        labels: MonarchAiAutofillFieldLabels = MonarchAiAutofillFieldLabels(),
        onAccept: @escaping @MainActor () -> Void,
        onClear: @escaping @MainActor () -> Void,
        @ViewBuilder control: () -> Control
    ) {
        self.state = state
        self.suggestion = suggestion
        self.fieldName = fieldName
        self.source = source
        self.confidence = confidence
        self.lowThreshold = lowThreshold
        self.highThreshold = highThreshold
        self.confidenceExplanation = confidenceExplanation
        self.model = model
        self.date = date
        self.labels = labels
        self.onAccept = onAccept
        self.onClear = onClear
        self.control = control()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiAutofillFieldTokens.spacing) {
            // While the value is suggested the box itself is tinted and bordered in the AI colours, as on the web.
            control
                .environment(\.monarchAiTone, MonarchAiAutofillFieldTokens.markerRole(state) != nil)
                .frame(maxWidth: .infinity, alignment: .leading)
            if MonarchAiAutofillFieldTokens.showsStatus(state) {
                MonarchFlowLayout(spacing: MonarchAiAutofillFieldTokens.statusSpacing) {
                    info
                    if MonarchAiAutofillFieldTokens.showsActions(state) {
                        actions
                    }
                }
            }
        }
    }

    private var info: some View {
        MonarchFlowLayout(spacing: MonarchAiAutofillFieldTokens.statusSpacing) {
            MonarchAiGeneratedBadge(
                MonarchAiAutofillFieldTokens.badgeKind(state),
                model: model,
                date: date,
                size: .sm,
                label: MonarchAiAutofillFieldTokens.badgeText(state, labels: labels)
            )
            if MonarchAiAutofillFieldTokens.showsConfidence(state, confidence: confidence), let confidence {
                MonarchAiConfidenceMeter(
                    value: confidence,
                    explanation: confidenceExplanation,
                    size: .sm,
                    lowThreshold: lowThreshold,
                    highThreshold: highThreshold
                )
            }
            if MonarchAiAutofillFieldTokens.showsSource(state, source: source), let source {
                Text(MonarchAiAutofillFieldTokens.sourceText(source, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if MonarchAiAutofillFieldTokens.showsOriginal(state) {
                Text(MonarchAiAutofillFieldTokens.originalText(suggestion, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var actions: some View {
        HStack(spacing: MonarchAiAutofillFieldTokens.statusSpacing) {
            MonarchButton(
                labels.accept ?? MonarchAiAutofillFieldStrings.accept,
                systemImage: MonarchAiAutofillFieldTokens.acceptSymbol,
                variant: .ghost,
                size: .sm
            ) {
                onAccept()
                AccessibilityNotification.Announcement(MonarchAiAutofillFieldTokens.acceptedAnnouncement(labels)).post()
            }
            .accessibilityLabel(MonarchAiAutofillFieldTokens.acceptLabel(fieldName: fieldName, labels: labels))
            MonarchButton(
                labels.clear ?? MonarchAiAutofillFieldStrings.clear,
                systemImage: MonarchAiAutofillFieldTokens.clearSymbol,
                variant: .ghost,
                size: .sm
            ) {
                onClear()
                AccessibilityNotification.Announcement(MonarchAiAutofillFieldTokens.clearedAnnouncement(labels)).post()
            }
            .accessibilityLabel(MonarchAiAutofillFieldTokens.clearLabel(fieldName: fieldName, labels: labels))
        }
    }
}

#if DEBUG
    #Preview("MonarchAiAutofillField") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchAiAutofillState.allCases, id: \.self) { state in
                    MonarchAiAutofillField(
                        state: state,
                        suggestion: "Rook Labs",
                        fieldName: "Company",
                        source: "invoice.pdf",
                        confidence: 0.92,
                        onAccept: {},
                        onClear: {},
                        control: { MonarchTextField("Company", text: .constant("Rook Labs")) }
                    )
                }
            }
        }
    }
#endif
