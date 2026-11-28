import SwiftUI

/// The screenshot box of ``MonarchAiComputerView``: the picture at its own
/// aspect ratio with the markers on top, a placeholder when none shows, the
/// capture time and the Zoom button.
struct MonarchAiComputerViewScreen: View {
    let frame: MonarchAiComputerFrame?
    let actions: [MonarchAiComputerAction]
    let pointer: MonarchAiComputerPoint?
    let surface: MonarchAiComputerViewSurface
    let selectedID: String?
    let isAgentActing: Bool
    let labels: MonarchAiComputerViewLabels
    let onZoom: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @ScaledMetric(relativeTo: .body) private var markerSize = MonarchAiComputerViewTokens.markerSize
    @ScaledMetric(relativeTo: .body) private var pointerSize = MonarchAiComputerViewTokens.pointerSize

    private typealias Tokens = MonarchAiComputerViewTokens

    var body: some View {
        let latest = actions.last
        let state = Tokens.frameState(frame, latest: latest)
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
            ZStack {
                shape.fill(theme.colors[role: Tokens.boxRole])
                if state == .shown, let frame {
                    picture(frame)
                    markers(frame)
                } else if let text = Tokens.placeholder(state, labels: labels) {
                    VStack(spacing: Tokens.innerSpacing) {
                        if state == .hidden {
                            MonarchIcon(systemName: Tokens.hiddenSymbol, size: .md)
                        }
                        Text(text)
                            .monarchTextStyle(.caption)
                            .multilineTextAlignment(.center)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(Tokens.innerSpacing)
                }
            }
            .aspectRatio(Tokens.aspectRatio(frame), contentMode: .fit)
            .frame(maxWidth: .infinity)
            .clipShape(shape)
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: isAgentActing ? Tokens.agentBoxBorderRole : Tokens.boxBorderRole],
                    lineWidth: isAgentActing ? MonarchShapes.borderWidth * 2 : MonarchShapes.borderWidth
                )
            )
            // Screenshot pixels count from the left edge in every writing direction.
            .environment(\.layoutDirection, .leftToRight)
            .contentShape(shape)
            .onTapGesture {
                if state == .shown { onZoom() }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                Tokens.frameDescription(frame, state: state, surface: surface, latest: latest, labels: labels)
            )
            .accessibilityAddTraits(state == .shown ? .isImage : [])
            .accessibilityAction(named: Text(labels.zoom ?? MonarchAiComputerViewStrings.zoom)) {
                if state == .shown { onZoom() }
            }

            HStack(spacing: Tokens.innerSpacing) {
                if state == .shown, let frame {
                    let time = Tokens.time(frame.capturedAt, locale: locale, timeZone: timeZone)
                    Text(labels.capturedAt?(time) ?? MonarchAiComputerViewStrings.capturedAt(time))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonarchButton(
                        labels.zoom ?? MonarchAiComputerViewStrings.zoom,
                        systemImage: Tokens.zoomSymbol,
                        variant: .ghost,
                        size: .sm
                    ) { onZoom() }
                }
            }
        }
    }

    @ViewBuilder
    private func picture(_ frame: MonarchAiComputerFrame) -> some View {
        if let image = frame.image {
            MonarchRemoteImage(image: image) { Color.clear }
        } else {
            MonarchRemoteImage(url: frame.url) {
                ProgressView().controlSize(.small)
            }
        }
    }

    private func markers(_ frame: MonarchAiComputerFrame) -> some View {
        let selected = actions.first { $0.id == selectedID && $0.point != nil }
        let last = Tokens.lastPointAction(actions)
        let selectedAt = Tokens.position(selected?.point, width: frame.width, height: frame.height)
        let lastAt =
            last?.id == selected?.id ? nil : Tokens.position(last?.point, width: frame.width, height: frame.height)
        let pointerAt = Tokens.position(pointer, width: frame.width, height: frame.height)
        return GeometryReader { proxy in
            ZStack(alignment: .topLeading) {
                if let lastAt {
                    marker(role: Tokens.lastMarkerRole, width: Tokens.markerWidth)
                        .position(x: lastAt.x * proxy.size.width, y: lastAt.y * proxy.size.height)
                }
                if let selectedAt {
                    marker(role: Tokens.selectedMarkerRole, width: Tokens.selectedMarkerWidth)
                        .position(x: selectedAt.x * proxy.size.width, y: selectedAt.y * proxy.size.height)
                }
                if let pointerAt {
                    // The tip of the arrow is the top-left corner of the glyph.
                    Image(systemName: Tokens.pointerSymbol)
                        .resizable()
                        .scaledToFit()
                        .frame(width: pointerSize, height: pointerSize)
                        .foregroundStyle(theme.colors[role: Tokens.pointerRole])
                        .shadow(color: theme.colors[role: Tokens.haloRole], radius: Tokens.markerWidth)
                        .offset(x: pointerAt.x * proxy.size.width, y: pointerAt.y * proxy.size.height)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .topLeading)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private func marker(role: MonarchColorRole, width: CGFloat) -> some View {
        Circle()
            .strokeBorder(theme.colors[role: role], lineWidth: width)
            .background(Circle().strokeBorder(theme.colors[role: Tokens.haloRole], lineWidth: width + Tokens.haloWidth))
            .frame(width: markerSize, height: markerSize)
    }
}
