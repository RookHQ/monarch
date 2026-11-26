import SwiftUI

/// The decorative status bar of a device frame: a time and three signal bars.
struct MonarchDeviceFrameStatusBar: View {
    /// The time text, for example "9:41".
    let time: String

    var body: some View {
        HStack {
            Text(time)
                .monarchTextStyle(.caption, weight: MonarchDeviceFrameTokens.statusBarWeight)
                .monospacedDigit()
                .lineLimit(1)
            Spacer(minLength: 0)
            HStack(alignment: .bottom, spacing: MonarchDeviceFrameTokens.signalSpacing) {
                ForEach(MonarchDeviceFrameTokens.signalHeights, id: \.self) { ratio in
                    Capsule()
                        .frame(
                            width: MonarchDeviceFrameTokens.signalBarWidth,
                            height: MonarchDeviceFrameTokens.signalHeight * ratio
                        )
                }
            }
        }
        .padding(.horizontal, MonarchDeviceFrameTokens.statusBarPadding)
        .frame(minHeight: MonarchDeviceFrameTokens.statusBarHeight)
        // A drawing of a status bar: like the system one, it does not grow
        // with the accessibility text sizes.
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
        .accessibilityHidden(true)
    }
}
