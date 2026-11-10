package com.wout.member.util

class ScoreCurve(private vararg val anchors: Pair<Double, Double>) {

    fun scoreAt(value: Double): Double {
        val (from, to) = anchors.asList().zipWithNext().firstOrNull { value <= it.second.first }
            ?: return anchors.last().second
        val position = ((value - from.first) / (to.first - from.first)).coerceIn(0.0, 1.0)
        return from.second + (to.second - from.second) * position
    }
}
