import SwiftUI

/// The controls of a call: microphone, camera, screen sharing, raised hand,
/// reactions, a More menu and Leave.
///
/// ```swift
/// MonarchCallControls(
///     isMicOn: $call.isMicOn,
///     isCameraOn: $call.isCameraOn,
///     isHandRaised: $call.isHandRaised,
///     onReact: { emoji in call.send(emoji) },
///     onLeave: { call.leave() }
/// )
/// ```
///
/// Fully controlled and UI only: each control appears only when its binding
/// or action is given, and the app decides what the call does. The More menu
/// appears when it has at least one item: the layout, low data mode, the
/// device settings, the connection details or picture in picture.
///
/// Accessibility: the controls are one group named "Call controls". The
/// microphone, camera, sharing and hand controls are standard toggles to
/// VoiceOver, read as their name and on or off. A device that is off, a
/// share in progress and a raised hand change the symbol as well as the
/// colour. Each reaction is read as "React" and its emoji. Every control is
/// at least 44 points and the row wraps at large text sizes.
public struct MonarchCallControls: View {
    /// The reactions offered by default.
    public static let defaultReactions = ["👍", "👏", "😂", "❤️", "🎉", "😮"]

    private let isMicOn: Binding<Bool>?
    private let isCameraOn: Binding<Bool>?
    private let isSharing: Binding<Bool>?
    private let isHandRaised: Binding<Bool>?
    private let layout: Binding<MonarchCallControlsLayout>?
    private let isLowData: Binding<Bool>?
    private let reactions: [String]
    private let isMinimal: Bool
    private let labels: MonarchCallControlsLabels
    private let onReact: (@MainActor (String) -> Void)?
    private let onOpenDevices: (@MainActor () -> Void)?
    private let onOpenStats: (@MainActor () -> Void)?
    private let onPictureInPicture: (@MainActor () -> Void)?
    private let onLeave: (@MainActor () -> Void)?

    private typealias Tokens = MonarchCallControlsTokens

    /// Call controls.
    ///
    /// - Parameters:
    ///   - isMicOn: Whether the microphone is on. Shows the microphone toggle.
    ///   - isCameraOn: Whether the camera is on. Shows the camera toggle.
    ///   - isSharing: Whether the screen is shared. Shows the sharing toggle.
    ///   - isHandRaised: Whether the hand is raised. Shows the hand toggle.
    ///   - layout: The stage layout. Adds the layout choice to the More menu.
    ///   - isLowData: Whether video is off to save data. Adds the toggle to
    ///     the More menu.
    ///   - reactions: The emoji offered in the reactions menu.
    ///   - isMinimal: Leaves out the hand, reactions and More, for a small
    ///     window.
    ///   - labels: Replacements for the built-in strings.
    ///   - onReact: Sends a reaction. Shows the reactions menu.
    ///   - onOpenDevices: Opens the app's device settings. Adds an item to More.
    ///   - onOpenStats: Opens the app's connection details. Adds an item to More.
    ///   - onPictureInPicture: Continues the call in picture in picture. Adds
    ///     an item to More.
    ///   - onLeave: Leaves the call. Shows the Leave control.
    public init(
        isMicOn: Binding<Bool>? = nil,
        isCameraOn: Binding<Bool>? = nil,
        isSharing: Binding<Bool>? = nil,
        isHandRaised: Binding<Bool>? = nil,
        layout: Binding<MonarchCallControlsLayout>? = nil,
        isLowData: Binding<Bool>? = nil,
        reactions: [String] = MonarchCallControls.defaultReactions,
        isMinimal: Bool = false,
        labels: MonarchCallControlsLabels = MonarchCallControlsLabels(),
        onReact: (@MainActor (String) -> Void)? = nil,
        onOpenDevices: (@MainActor () -> Void)? = nil,
        onOpenStats: (@MainActor () -> Void)? = nil,
        onPictureInPicture: (@MainActor () -> Void)? = nil,
        onLeave: (@MainActor () -> Void)? = nil
    ) {
        self.isMicOn = isMicOn
        self.isCameraOn = isCameraOn
        self.isSharing = isSharing
        self.isHandRaised = isHandRaised
        self.layout = layout
        self.isLowData = isLowData
        self.reactions = reactions
        self.isMinimal = isMinimal
        self.labels = labels
        self.onReact = onReact
        self.onOpenDevices = onOpenDevices
        self.onOpenStats = onOpenStats
        self.onPictureInPicture = onPictureInPicture
        self.onLeave = onLeave
    }

    private var controls: [Tokens.Control] {
        Tokens.controls(
            hasMicrophone: isMicOn != nil,
            hasCamera: isCameraOn != nil,
            hasShare: isSharing != nil,
            hasHand: isHandRaised != nil,
            hasReactions: onReact != nil && !reactions.isEmpty,
            hasMoreItems: layout != nil || isLowData != nil || onOpenDevices != nil || onOpenStats != nil
                || onPictureInPicture != nil,
            hasLeave: onLeave != nil,
            isMinimal: isMinimal
        )
    }

    /// The content of the view.
    public var body: some View {
        MonarchFlowLayout(spacing: Tokens.spacing, justification: .center) {
            ForEach(controls, id: \.self) { control in
                view(for: control)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.toolbar ?? MonarchCallControlsStrings.toolbar)
    }

    @ViewBuilder private func view(for control: Tokens.Control) -> some View {
        switch control {
        case .microphone:
            toggle(.microphone, isOn: isMicOn, title: labels.microphone ?? MonarchCallControlsStrings.microphone)
        case .camera:
            toggle(.camera, isOn: isCameraOn, title: labels.camera ?? MonarchCallControlsStrings.camera)
        case .share:
            toggle(.share, isOn: isSharing, title: labels.shareScreen ?? MonarchCallControlsStrings.shareScreen)
        case .hand:
            toggle(.hand, isOn: isHandRaised, title: labels.raiseHand ?? MonarchCallControlsStrings.raiseHand)
        case .reactions:
            reactionsMenu
        case .more:
            moreMenu
        case .leave:
            Button {
                onLeave?()
            } label: {
                Label {
                    Text(labels.leave ?? MonarchCallControlsStrings.leave)
                } icon: {
                    // The phone with a slash through it, as the web's phone-off: SF Symbols has no such symbol.
                    ZStack {
                        ZStack {
                            Image(systemName: "phone.fill")
                            // A gap is cut along the slash, then the slash is drawn in the gap.
                            Capsule()
                                .frame(width: 6, height: 26)
                                .rotationEffect(.degrees(-45))
                                .blendMode(.destinationOut)
                        }
                        .compositingGroup()
                        Capsule()
                            .frame(width: 2, height: 24)
                            .rotationEffect(.degrees(-45))
                    }
                }
            }
            .buttonStyle(MonarchCallControlButtonStyle(tone: .danger, isWide: true))
        }
    }

    @ViewBuilder private func toggle(_ control: Tokens.Control, isOn: Binding<Bool>?, title: String) -> some View {
        if let isOn {
            Toggle(isOn: isOn) {
                Label(title, systemImage: Tokens.symbol(control, isOn: isOn.wrappedValue))
            }
            .toggleStyle(MonarchCallControlToggleStyle(control: control))
        }
    }

    private var reactionsMenu: some View {
        Menu {
            ForEach(reactions, id: \.self) { emoji in
                Button(emoji) { onReact?(emoji) }
                    .accessibilityLabel(Tokens.reactionLabel(emoji, labels: labels))
            }
        } label: {
            Label(labels.react ?? MonarchCallControlsStrings.react, systemImage: Tokens.symbol(.reactions, isOn: false))
        }
        .buttonStyle(MonarchCallControlButtonStyle(tone: .standard))
    }

    private var moreMenu: some View {
        Menu {
            if let layout {
                Picker(labels.layout ?? MonarchCallControlsStrings.layout, selection: layout) {
                    ForEach(MonarchCallControlsLayout.allCases, id: \.self) { option in
                        Text(Tokens.layoutName(option, labels: labels)).tag(option)
                    }
                }
                .pickerStyle(.inline)
            }
            if let isLowData {
                Toggle(labels.lowData ?? MonarchCallControlsStrings.lowData, isOn: isLowData)
            }
            if let onOpenDevices {
                Button(labels.devices ?? MonarchCallControlsStrings.devices, systemImage: "gearshape") {
                    onOpenDevices()
                }
            }
            if let onOpenStats {
                Button(labels.stats ?? MonarchCallControlsStrings.stats, systemImage: "waveform.path.ecg") {
                    onOpenStats()
                }
            }
            if let onPictureInPicture {
                Divider()
                Button(
                    labels.pictureInPicture ?? MonarchCallControlsStrings.pictureInPicture,
                    systemImage: "pip.enter"
                ) {
                    onPictureInPicture()
                }
            }
        } label: {
            Label(labels.more ?? MonarchCallControlsStrings.more, systemImage: Tokens.symbol(.more, isOn: false))
        }
        .buttonStyle(MonarchCallControlButtonStyle(tone: .standard))
    }
}

#if DEBUG
    #Preview("MonarchCallControls") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCallControls(
                    isMicOn: .constant(true),
                    isCameraOn: .constant(false),
                    isSharing: .constant(true),
                    isHandRaised: .constant(true),
                    layout: .constant(.grid),
                    onReact: { _ in },
                    onLeave: {}
                )
                MonarchCallControls(
                    isMicOn: .constant(false),
                    isCameraOn: .constant(true),
                    isMinimal: true,
                    onLeave: {}
                )
            }
        }
    }
#endif
