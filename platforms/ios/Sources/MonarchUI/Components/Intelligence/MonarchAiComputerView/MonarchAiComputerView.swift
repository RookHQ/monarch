import MonarchTokens
import SwiftUI

/// A live view of an AI agent operating a browser or a computer for the
/// person: the latest screenshot with the agent's pointer and last action
/// marked, the status in words, a banner that says who is in control, a log
/// of every action, and buttons to pause, take over, hand back and stop.
///
/// ```swift
/// MonarchAiComputerView(
///     status: session.status,
///     frame: MonarchAiComputerFrame(url: session.screenshotURL, width: 1280, height: 800, capturedAt: session.capturedAt),
///     actions: session.actions,
///     currentURL: session.address,
///     onPauseChange: { paused in session.setPaused(paused) },
///     onTakeOver: { session.takeOver() },
///     onHandBack: { session.handBack() },
///     onStop: { session.stop() }
/// )
/// ```
///
/// It is a viewer: the app supplies screenshots and actions and performs all
/// control. Each button shows only when its handler is given and the status
/// allows it. Stop asks for confirmation in a system confirmation dialog.
/// A screenshot marked sensitive, or taken right after a sensitive action, is
/// never shown, and a sensitive action's label and detail are never shown or
/// announced. Tap the screenshot or Zoom to see it full screen in
/// ``MonarchLightbox``, where it can be pinched and zoomed.
///
/// Accessibility: the view is a container named "Assistant browser view".
/// The screenshot is one image element that reads its description and the
/// latest action, with a Zoom action. The status is read in words with an
/// icon, never by colour alone. Each status change is announced once; new
/// actions are announced politely, no more often than `announceInterval`,
/// and several at once are summed up ("3 new actions. Latest: …"). Log rows
/// are buttons read as "Click: Clicked Sign in" with the detail and time as
/// the value; the selected row has the selected trait and a check mark. All
/// buttons have 44-point touch targets. The screenshot and its markers keep
/// physical left-to-right positions in right-to-left layouts, because the
/// points are screen pixels.
@_spi(Experimental) public struct MonarchAiComputerView: View {
    private let status: MonarchAiComputerViewStatus
    private let frame: MonarchAiComputerFrame?
    private let actions: [MonarchAiComputerAction]
    private let currentURL: String?
    private let windowTitle: String?
    private let pointer: MonarchAiComputerPoint?
    private let surface: MonarchAiComputerViewSurface
    private let waitingReason: String?
    private let selectionBinding: Binding<String?>?
    private let showsTypedText: Bool
    private let isCompact: Bool
    private let visibleActions: Int
    private let announceInterval: Duration
    private let labels: MonarchAiComputerViewLabels
    private let onPauseChange: (@MainActor (Bool) -> Void)?
    private let onTakeOver: (@MainActor () -> Void)?
    private let onHandBack: (@MainActor () -> Void)?
    private let onStop: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @State private var selectionState: String?
    @State private var isConfirmingStop = false
    @State private var isZoomed = false
    @State private var zoomSelection = 0
    @State private var isLogExpanded = false
    @State private var showsAllActions = false
    @State private var knownActionIDs: Set<String>?
    @State private var pendingCount = 0
    @State private var pendingLabel = ""
    @State private var lastAnnouncement: ContinuousClock.Instant?
    @State private var flushTask: Task<Void, Never>?

    private typealias Tokens = MonarchAiComputerViewTokens

    /// A computer view.
    ///
    /// - Parameters:
    ///   - status: Where the session is. Each change is announced once.
    ///   - frame: The latest screenshot. Omit it until the first one arrives.
    ///   - actions: What the agent did, oldest first.
    ///   - currentURL: The address of the page the agent is on, shown as text (never a link) without credentials.
    ///   - windowTitle: The title of the window or page.
    ///   - pointer: Where the agent's pointer is, in screenshot pixels.
    ///   - surface: What is operated; only changes the wording.
    ///   - waitingReason: Why the agent waits, such as "Sign in to continue". Shown while waiting for you.
    ///   - selection: The id of the highlighted action, whose point is marked. Without it the view keeps it.
    ///   - showsTypedText: Shows the detail of typing actions. Sensitive actions stay hidden.
    ///   - isCompact: Puts the log behind a disclosure.
    ///   - visibleActions: How many of the newest actions the log shows before "Show earlier".
    ///   - announceInterval: The shortest time between two action announcements.
    ///   - labels: Replacements for the built-in strings.
    ///   - onPauseChange: Shows Pause while working and Resume while paused; receives the wanted paused state.
    ///   - onTakeOver: Shows Take over while the agent holds control.
    ///   - onHandBack: Shows Hand back while the person holds control.
    ///   - onStop: Shows Stop until the session is finished; runs after confirmation.
    public init(
        status: MonarchAiComputerViewStatus,
        frame: MonarchAiComputerFrame? = nil,
        actions: [MonarchAiComputerAction] = [],
        currentURL: String? = nil,
        windowTitle: String? = nil,
        pointer: MonarchAiComputerPoint? = nil,
        surface: MonarchAiComputerViewSurface = .browser,
        waitingReason: String? = nil,
        selection: Binding<String?>? = nil,
        showsTypedText: Bool = false,
        isCompact: Bool = false,
        visibleActions: Int = 200,
        announceInterval: Duration = .seconds(4),
        labels: MonarchAiComputerViewLabels = MonarchAiComputerViewLabels(),
        onPauseChange: (@MainActor (Bool) -> Void)? = nil,
        onTakeOver: (@MainActor () -> Void)? = nil,
        onHandBack: (@MainActor () -> Void)? = nil,
        onStop: (@MainActor () -> Void)? = nil
    ) {
        self.status = status
        self.frame = frame
        self.actions = actions
        self.currentURL = currentURL
        self.windowTitle = windowTitle
        self.pointer = pointer
        self.surface = surface
        self.waitingReason = waitingReason
        self.selectionBinding = selection
        self.showsTypedText = showsTypedText
        self.isCompact = isCompact
        self.visibleActions = visibleActions
        self.announceInterval = announceInterval
        self.labels = labels
        self.onPauseChange = onPauseChange
        self.onTakeOver = onTakeOver
        self.onHandBack = onHandBack
        self.onStop = onStop
    }

    private var selection: Binding<String?> {
        selectionBinding ?? $selectionState
    }

    /// Who is in control: the AI solid colour while the agent holds the session, a neutral outlined strip while
    /// the person does, as the web draws it.
    private func controlBanner(_ text: String, isPerson: Bool) -> some View {
        let shape = MonarchShapes.control
        return HStack(spacing: Tokens.innerSpacing) {
            MonarchIcon(systemName: isPerson ? Tokens.takeOverSymbol : (Tokens.symbol(.working) ?? "cpu"), size: .sm)
            Text(text).monarchTextStyle(.label).lineLimit(2)
            Spacer(minLength: 0)
        }
        .foregroundStyle(isPerson ? theme.colors.textDefault : theme.colors.aiOnSolid)
        .padding(.horizontal, MonarchTokens.Space.space3)
        .padding(.vertical, MonarchTokens.Space.space2)
        .background(shape.fill(isPerson ? theme.colors.bgMuted : theme.colors.aiSolid))
        .overlay { if isPerson { shape.strokeBorder(theme.colors.borderStrong, lineWidth: MonarchShapes.borderWidth) } }
        .accessibilityElement(children: .combine)
    }

    /// The content of the view.
    public var body: some View {
        let controls = Tokens.controls(
            status,
            hasPause: onPauseChange != nil,
            hasTakeOver: onTakeOver != nil,
            hasHandBack: onHandBack != nil,
            hasStop: onStop != nil
        )
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            header
            if let banner = Tokens.banner(status, surface: surface, labels: labels) {
                controlBanner(banner, isPerson: status == .takenOver)
            }
            MonarchAiComputerViewScreen(
                frame: frame,
                actions: actions,
                pointer: pointer,
                surface: surface,
                selectedID: selection.wrappedValue,
                isAgentActing: Tokens.isAgentActing(status),
                labels: labels,
                onZoom: { isZoomed = true }
            )
            if status == .waitingForYou {
                MonarchCalloutBanner(
                    labels.waitingTitle ?? MonarchAiComputerViewStrings.waitingTitle,
                    message: waitingReason ?? labels.waitingFallback ?? MonarchAiComputerViewStrings.waitingFallback,
                    tone: .warning
                )
            }
            if !controls.isEmpty {
                controlButtons(controls)
            }
            log
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label?(surface) ?? MonarchAiComputerViewStrings.label(surface))
        .monarchLightbox(isPresented: $isZoomed, items: lightboxItems, selection: $zoomSelection)
        .confirmationDialog(
            labels.stopQuestion ?? MonarchAiComputerViewStrings.stopQuestion,
            isPresented: $isConfirmingStop,
            titleVisibility: .visible
        ) {
            Button(labels.stopConfirm ?? MonarchAiComputerViewStrings.stopConfirm, role: .destructive) {
                onStop?()
            }
            Button(labels.stopCancel ?? MonarchAiComputerViewStrings.stopCancel, role: .cancel) {}
        }
        .onChange(of: status) { before, after in
            if Tokens.isFinished(after) { isConfirmingStop = false }
            if let text = Tokens.statusAnnouncement(from: before, to: after, surface: surface, labels: labels) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
        .onAppear {
            if knownActionIDs == nil { knownActionIDs = Set(actions.map(\.id)) }
        }
        .onChange(of: actions.map(\.id)) {
            noteNewActions()
        }
        .onDisappear {
            flushTask?.cancel()
            flushTask = nil
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
            HStack(spacing: Tokens.innerSpacing) {
                if let symbol = Tokens.symbol(status) {
                    MonarchIcon(systemName: symbol, size: .md)
                        .foregroundStyle(
                            theme.colors[role: MonarchTonePalette.palette(for: Tokens.tone(status)).strong]
                        )
                } else {
                    ProgressView().controlSize(.small)
                }
                Text(Tokens.statusName(status, labels: labels))
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
            }
            .accessibilityElement(children: .combine)
            if windowTitle != nil || currentURL != nil {
                VStack(alignment: .leading, spacing: Tokens.rowLineSpacing) {
                    if let windowTitle, !windowTitle.isEmpty {
                        Text(windowTitle)
                            .monarchTextStyle(.label)
                            .foregroundStyle(theme.colors.textDefault)
                            .lineLimit(2)
                    }
                    if let currentURL, !currentURL.isEmpty {
                        Text(Tokens.displayURL(currentURL))
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .accessibilityLabel(
                                "\(labels.address ?? MonarchAiComputerViewStrings.address): \(Tokens.displayURL(currentURL))"
                            )
                    }
                }
            }
        }
    }

    // MARK: - Controls

    private func controlButtons(_ controls: Tokens.Controls) -> some View {
        HStack(spacing: Tokens.innerSpacing) {
            if controls.canTakeOver {
                MonarchButton(
                    labels.takeOver ?? MonarchAiComputerViewStrings.takeOver,
                    systemImage: Tokens.takeOverSymbol,
                    variant: status == .waitingForYou ? .primary : .secondary,
                    size: .sm
                ) { onTakeOver?() }
            }
            if controls.canHandBack {
                MonarchButton(
                    labels.handBack ?? MonarchAiComputerViewStrings.handBack,
                    systemImage: Tokens.handBackSymbol,
                    variant: .primary,
                    size: .sm
                ) { onHandBack?() }
            }
            if controls.canPause {
                MonarchButton(
                    labels.pause ?? MonarchAiComputerViewStrings.pause,
                    systemImage: Tokens.pauseSymbol,
                    size: .sm
                ) { onPauseChange?(true) }
            }
            if controls.canResume {
                MonarchButton(
                    labels.resume ?? MonarchAiComputerViewStrings.resume,
                    systemImage: Tokens.resumeSymbol,
                    size: .sm
                ) { onPauseChange?(false) }
            }
            if controls.canStop {
                // Stop sits at the right as a pale danger button, as on the web.
                Spacer(minLength: 0)
                MonarchButton(
                    labels.stop ?? MonarchAiComputerViewStrings.stop,
                    systemImage: Tokens.stopSymbol,
                    variant: .dangerSubtle,
                    size: .sm
                ) { isConfirmingStop = true }
            }
        }
    }

    // MARK: - Log

    @ViewBuilder
    private var log: some View {
        if isCompact {
            DisclosureGroup(isExpanded: $isLogExpanded) {
                logRows.padding(.top, Tokens.innerSpacing)
            } label: {
                Text(labels.logToggle?(actions.count) ?? MonarchAiComputerViewStrings.logToggle(actions.count))
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
            }
            .tint(theme.colors.textMuted)
        } else {
            VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
                Text(labels.logLabel ?? MonarchAiComputerViewStrings.logLabel)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                logRows
            }
        }
    }

    private var logRows: some View {
        let hidden = Tokens.hiddenBefore(total: actions.count, visible: visibleActions, showsAll: showsAllActions)
        return VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
            if actions.isEmpty {
                Text(labels.empty ?? MonarchAiComputerViewStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                if hidden > 0 {
                    MonarchButton(
                        labels.showEarlier?(hidden) ?? MonarchAiComputerViewStrings.showEarlier(hidden),
                        variant: .ghost,
                        size: .sm
                    ) { showsAllActions = true }
                }
                ForEach(actions.dropFirst(hidden)) { action in
                    MonarchAiComputerViewRow(
                        action: action,
                        isSelected: action.id == selection.wrappedValue,
                        showsTypedText: showsTypedText,
                        time: Tokens.time(action.date, locale: locale, timeZone: timeZone),
                        labels: labels
                    ) {
                        selection.wrappedValue = action.id
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.logLabel ?? MonarchAiComputerViewStrings.logLabel)
    }

    // MARK: - Zoom

    private var lightboxItems: [MonarchLightboxItem] {
        guard let frame, Tokens.frameState(frame, latest: actions.last) == .shown else { return [] }
        let description = Tokens.frameDescription(
            frame,
            state: .shown,
            surface: surface,
            latest: nil,
            labels: labels
        )
        if let image = frame.image {
            return [MonarchLightboxItem(image: image, alt: description)]
        }
        return [MonarchLightboxItem(url: frame.url, alt: description)]
    }

    // MARK: - Announcements

    private func noteNewActions() {
        guard var known = knownActionIDs else {
            knownActionIDs = Set(actions.map(\.id))
            return
        }
        let fresh = Tokens.freshActions(actions, known: known)
        guard let newest = fresh.last else { return }
        known.formUnion(fresh.map(\.id))
        if known.count > max(Tokens.maxRemembered, actions.count * 2) {
            known = Set(actions.map(\.id))
        }
        knownActionIDs = known
        pendingCount += fresh.count
        pendingLabel = Tokens.actionLabel(newest, labels: labels)
        let now = ContinuousClock.now
        if let last = lastAnnouncement, now < last + announceInterval {
            guard flushTask == nil else { return }
            let deadline = last + announceInterval
            flushTask = Task { @MainActor in
                try? await Task.sleep(until: deadline, clock: .continuous)
                guard !Task.isCancelled else { return }
                flushAnnouncement()
            }
        } else {
            flushAnnouncement()
        }
    }

    private func flushAnnouncement() {
        flushTask = nil
        guard let text = Tokens.actionAnnouncement(count: pendingCount, latest: pendingLabel, labels: labels) else {
            return
        }
        pendingCount = 0
        pendingLabel = ""
        lastAnnouncement = ContinuousClock.now
        AccessibilityNotification.Announcement(text).post()
    }
}

#if DEBUG
    #Preview("MonarchAiComputerView") {
        MonarchPreviewMatrix {
            MonarchAiComputerView(
                status: .working,
                frame: MonarchAiComputerFrame(
                    image: Image(systemName: "rectangle.on.rectangle"),
                    width: 1280,
                    height: 800,
                    capturedAt: Date(timeIntervalSinceReferenceDate: 800_000_000)
                ),
                actions: [
                    MonarchAiComputerAction(
                        id: "1",
                        kind: .click,
                        label: "Clicked Sign in",
                        date: Date(timeIntervalSinceReferenceDate: 800_000_000),
                        point: MonarchAiComputerPoint(x: 900, y: 120)
                    )
                ],
                currentURL: "https://example.com/login",
                pointer: MonarchAiComputerPoint(x: 640, y: 400),
                onPauseChange: { _ in },
                onTakeOver: {},
                onStop: {}
            )
        }
    }
#endif
