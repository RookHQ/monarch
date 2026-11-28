package dev.monarchui.components.charts.forcegraph

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchForceGraph`. Every `null`
 * uses the built-in string of `strings_forcegraph.xml`. Sentences with values
 * are format strings with `%1$s`-style placeholders, in the order given for
 * each; counts are functions so they can follow the language's plural rules.
 *
 * @property frame the strings of the chart frame. Its `legend` names the group legend.
 * @property zoomIn the zoom in button: "Zoom in".
 * @property zoomOut the zoom out button: "Zoom out".
 * @property resetView the button that undoes zoom, pan and moved nodes: "Reset view".
 * @property node the table heading of the node column: "Node".
 * @property group the table heading of the group column: "Group".
 * @property connections the table heading of the linked nodes: "Connections".
 * @property noGroup the group of a node without one: "No group".
 * @property edgeLegend the spoken name of the legend of link kinds: "Link types".
 * @property nodeCount writes the number of nodes: "12 nodes".
 * @property linkCount writes the number of links: "15 links".
 * @property groupCount writes the number of groups: "3 groups".
 * @property connectionCount writes the number of a node's links: "2 connections".
 * @property size the first sentence of the summary, from the nodes and the links: "%1$s and %2$s".
 * @property inGroups the first sentence with the groups, from the size and the groups: "%1$s in %2$s".
 * @property mostConnected the sentence of the most connected node, from its label and its links:
 * "Most connected: %1$s (%2$s)".
 * @property describe what TalkBack reads for a node without links, from its label, group and connections:
 * "%1$s, %2$s. %3$s".
 * @property describeLinked what TalkBack reads for a linked node, from its label, group, connections and the
 * linked nodes: "%1$s, %2$s. %3$s: %4$s".
 * @property linkWithKind a linked node with the kind of the link: "%1$s (%2$s)".
 */
@Immutable
public class MonarchForceGraphLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val zoomIn: String? = null,
    public val zoomOut: String? = null,
    public val resetView: String? = null,
    public val node: String? = null,
    public val group: String? = null,
    public val connections: String? = null,
    public val noGroup: String? = null,
    public val edgeLegend: String? = null,
    public val nodeCount: ((nodes: Int) -> String)? = null,
    public val linkCount: ((links: Int) -> String)? = null,
    public val groupCount: ((groups: Int) -> String)? = null,
    public val connectionCount: ((connections: Int) -> String)? = null,
    public val size: String? = null,
    public val inGroups: String? = null,
    public val mostConnected: String? = null,
    public val describe: String? = null,
    public val describeLinked: String? = null,
    public val linkWithKind: String? = null,
)
