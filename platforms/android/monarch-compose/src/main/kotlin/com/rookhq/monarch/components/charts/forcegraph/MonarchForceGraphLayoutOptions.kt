package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.runtime.Immutable

/**
 * The forces of the built-in layout of a `MonarchForceGraph`, in the layout's
 * own units (the graph is then fitted to the canvas). The defaults are the
 * web's.
 *
 * @property linkDistance the rest length of a link.
 * @property charge the repulsion between every pair of nodes; negative pushes apart.
 * @property collide the smallest distance between node centres.
 * @property gravity the pull of every node towards the centre, from 0 to 1.
 * @property ticks the number of simulation steps. `null` uses 300 up to 300 nodes and fewer above.
 */
@Immutable
public class MonarchForceGraphLayoutOptions(
    public val linkDistance: Double = DefaultLinkDistance,
    public val charge: Double = DefaultCharge,
    public val collide: Double = DefaultCollide,
    public val gravity: Double = DefaultGravity,
    public val ticks: Int? = null,
)

/** The web's defaults: 90, -260, 40 and 0.04. */
private const val DefaultLinkDistance = 90.0
private const val DefaultCharge = -260.0
private const val DefaultCollide = 40.0
private const val DefaultGravity = 0.04
