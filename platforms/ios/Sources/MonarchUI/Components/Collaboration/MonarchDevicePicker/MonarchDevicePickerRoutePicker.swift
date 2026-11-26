#if os(iOS)
    // AVKit's route picker is a UIKit view: SwiftUI has no control that
    // opens the system audio route picker, and audio output must be chosen
    // there. It exists only on iOS.
    import AVFoundation
    import AVKit
    import Combine
    import SwiftUI
    import UIKit

    /// The system audio route picker, tinted with the theme.
    struct MonarchDevicePickerRoutePicker: UIViewRepresentable {
        let tint: Color
        let activeTint: Color

        func makeUIView(context: Context) -> AVRoutePickerView {
            let view = AVRoutePickerView()
            view.prioritizesVideoDevices = false
            update(view)
            return view
        }

        func updateUIView(_ view: AVRoutePickerView, context: Context) {
            update(view)
        }

        private func update(_ view: AVRoutePickerView) {
            view.tintColor = UIColor(tint)
            view.activeTintColor = UIColor(activeTint)
        }
    }

    /// The speaker row: its label, the current output and the route picker.
    struct MonarchDevicePickerSpeakerRow: View {
        let label: String
        let fallback: String
        let size: MonarchDevicePickerSize

        @State private var outputs: [String] = []
        @Environment(\.monarchTheme) private var theme

        private typealias Tokens = MonarchDevicePickerTokens

        var body: some View {
            let name = Tokens.outputName(outputs, fallback: fallback)
            VStack(alignment: .leading, spacing: Tokens.labelSpacing) {
                Text(label)
                    .monarchTextStyle(Tokens.labelStyle(size), weight: .semibold)
                    .foregroundStyle(theme.colors[role: Tokens.labelRole])
                    .accessibilityHidden(true)
                // A field like the other two: the current output and a chevron. The system route picker sits over it
                // with no icon of its own, so a tap anywhere on the field opens the system list of outputs.
                HStack(spacing: Tokens.labelSpacing) {
                    Text(name)
                        .monarchTextStyle(Tokens.labelStyle(size))
                        .foregroundStyle(theme.colors.textDefault)
                        .lineLimit(1)
                        .accessibilityHidden(true)
                    Spacer(minLength: 0)
                    MonarchIcon(systemName: "chevron.down", size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityHidden(true)
                }
                .padding(.horizontal, 12)
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .background(theme.colors.bgSurface, in: MonarchShapes.control)
                .overlay(
                    MonarchShapes.control.strokeBorder(
                        theme.colors[role: \.borderInput],
                        lineWidth: MonarchShapes.borderWidth
                    )
                )
                .overlay {
                    MonarchDevicePickerRoutePicker(tint: .clear, activeTint: .clear)
                        .accessibilityLabel(label)
                        .accessibilityValue(name)
                }
            }
            .onAppear(perform: refresh)
            .onReceive(
                NotificationCenter.default.publisher(for: AVAudioSession.routeChangeNotification).receive(
                    on: DispatchQueue.main
                )
            ) { _ in
                refresh()
            }
        }

        private func refresh() {
            // The phone's own speaker or receiver is the system default the web select shows; any other route
            // (headphones, AirPlay, a car) is named.
            outputs = AVAudioSession.sharedInstance().currentRoute.outputs
                .filter { $0.portType != .builtInSpeaker && $0.portType != .builtInReceiver }
                .map(\.portName)
        }
    }
#endif
