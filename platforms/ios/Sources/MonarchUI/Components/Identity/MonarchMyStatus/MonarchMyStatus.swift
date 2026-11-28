import SwiftUI

/// The signed-in person's avatar, presence and custom status, as one row
/// that opens their status settings, usually at the top of a sidebar or an
/// account screen.
///
/// ```swift
/// MonarchMyStatus("Ana Silva", url: me.photoURL, presence: .away, customStatus: status) {
///     isEditingStatus = true
/// }
/// .sheet(isPresented: $isEditingStatus) { StatusEditor() }
/// ```
///
/// The row shows the custom status (emoji and message) under the name. A
/// status whose `until` has passed is hidden; without a status the line is
/// the presence when it is away, busy or do not disturb, and otherwise
/// "Set a status". The app owns the data and the sheet the row opens.
///
/// Accessibility: the row is one button named by the person's name, with
/// the presence and the status line as its value ("Online, On holiday") and
/// the hint "Opens your status and settings". The presence is in words, so
/// it never depends on the dot's colour. The row is at least 44 points high
/// and the text wraps at large sizes.
public struct MonarchMyStatus: View {
    private let name: String
    private let image: Image?
    private let url: URL?
    private let usesImage: Bool
    private let presence: MonarchPresenceStatus
    private let customStatus: MonarchMyStatusCustomStatus?
    private let now: Date?
    private let labels: MonarchMyStatusLabels
    private let action: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    /// A status row with an avatar loaded from a URL with `AsyncImage`.
    ///
    /// - Parameters:
    ///   - name: The person's display name, already localised.
    ///   - url: The avatar's address, or `nil` for the initials.
    ///   - presence: The person's presence.
    ///   - customStatus: The person's own status, hidden once its `until` has passed.
    ///   - now: The time `until` is compared with. Without it the row uses the
    ///     current time and draws itself again when the status ends. Pass it
    ///     for a fixed rendering, such as a test or a snapshot.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: Opens the status settings, usually a sheet.
    public init(
        _ name: String,
        url: URL?,
        presence: MonarchPresenceStatus,
        customStatus: MonarchMyStatusCustomStatus? = nil,
        now: Date? = nil,
        labels: MonarchMyStatusLabels = MonarchMyStatusLabels(),
        action: @escaping @MainActor () -> Void
    ) {
        self.name = name
        self.image = nil
        self.url = url
        self.usesImage = false
        self.presence = presence
        self.customStatus = customStatus
        self.now = now
        self.labels = labels
        self.action = action
    }

    /// A status row with an avatar image the app already has.
    ///
    /// - Parameters:
    ///   - name: The person's display name, already localised.
    ///   - image: The avatar, or `nil` for the initials.
    ///   - presence: The person's presence.
    ///   - customStatus: The person's own status, hidden once its `until` has passed.
    ///   - now: The time `until` is compared with. Without it the row uses the
    ///     current time and draws itself again when the status ends.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: Opens the status settings, usually a sheet.
    public init(
        _ name: String,
        image: Image?,
        presence: MonarchPresenceStatus,
        customStatus: MonarchMyStatusCustomStatus? = nil,
        now: Date? = nil,
        labels: MonarchMyStatusLabels = MonarchMyStatusLabels(),
        action: @escaping @MainActor () -> Void
    ) {
        self.name = name
        self.image = image
        self.url = nil
        self.usesImage = true
        self.presence = presence
        self.customStatus = customStatus
        self.now = now
        self.labels = labels
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        Button(action: action) {
            if let now {
                row(at: now)
            } else {
                TimelineView(MonarchMyStatusSchedule(until: customStatus?.until)) { context in
                    row(at: context.date)
                }
            }
        }
        .buttonStyle(MonarchMyStatusButtonStyle())
    }

    private func row(at date: Date) -> some View {
        let line = MonarchMyStatusTokens.line(presence: presence, status: customStatus, now: date, labels: labels)
        return HStack(spacing: MonarchMyStatusTokens.spacing) {
            avatar
            VStack(alignment: .leading, spacing: MonarchMyStatusTokens.textSpacing) {
                Text(name)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                Text(line.text)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(
                        theme.colors[role: MonarchMyStatusTokens.lineRole(isPlaceholder: line.isPlaceholder)]
                    )
            }
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, MonarchMyStatusTokens.horizontalPadding)
        .padding(.vertical, MonarchMyStatusTokens.verticalPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(name)
        .accessibilityValue(
            MonarchMyStatusTokens.accessibilityValue(
                presence: presence,
                status: customStatus,
                now: date,
                labels: labels
            )
        )
        .accessibilityHint(labels.hint ?? MonarchMyStatusStrings.hint)
    }

    @ViewBuilder private var avatar: some View {
        let statusLabel = MonarchMyStatusTokens.presenceName(presence, labels: labels)
        if usesImage {
            MonarchAvatar(
                image: image,
                name: name,
                size: .sm,
                status: presence,
                isDecorative: true,
                statusLabel: statusLabel
            )
        } else {
            MonarchAvatar(
                url: url,
                name: name,
                size: .sm,
                status: presence,
                isDecorative: true,
                statusLabel: statusLabel
            )
        }
    }
}

/// The row's pressed background. The environment is read in an inner view,
/// because a style cannot read it.
private struct MonarchMyStatusButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchMyStatusButtonBody(configuration: configuration)
    }
}

private struct MonarchMyStatusButtonBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchMyStatusTokens.cornerRadius, style: .continuous)
        configuration.label
            .background {
                if let role = MonarchMyStatusTokens.background(isPressed: configuration.isPressed) {
                    shape.fill(theme.colors[role: role])
                }
            }
            .contentShape(shape)
    }
}

#if DEBUG
    #Preview("MonarchMyStatus") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMyStatus(
                    "Ana Silva",
                    image: nil,
                    presence: .online,
                    customStatus: MonarchMyStatusCustomStatus("On holiday", emoji: "🌴")
                ) {}
                MonarchMyStatus("Ben Okafor", image: nil, presence: .doNotDisturb) {}
                MonarchMyStatus("Chen Wei", image: nil, presence: .online) {}
            }
        }
    }
#endif
