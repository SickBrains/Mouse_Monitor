package ml.logic

import ml.data.*
import kotlin.math.*

object FeatureExtractor {
    fun extract(action: MouseAction): FeatureVector? {
        val points = action.events
        val n = points.size
        if (n < 4) return null

        val vx = mutableListOf<Double>()
        val vy = mutableListOf<Double>()
        val v = mutableListOf<Double>()
        val a = mutableListOf<Double>()
        val j = mutableListOf<Double>()
        val omega = mutableListOf<Double>()
        val curvature = mutableListOf<Double>()
        val angles = mutableListOf<Double>()
        val distances = mutableListOf<Double>()

        var prevV = 0.0
        var prevA = 0.0
        var prevAngle = 0.0
        var totalLength = 0.0
        var sumAngle = 0.0
        var maxDev = 0.0
        var accelStart = 0.0
        var foundAccel = false

        val timeList = points.map { it.ctime }

        for (i in 1 until n) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            val dt = max(points[i].ctime - points[i - 1].ctime, 1e-6)

            val vxVal = dx / dt
            val vyVal = dy / dt
            val vVal = hypot(vxVal, vyVal)

            vx.add(vxVal)
            vy.add(vyVal)
            v.add(vVal)

            val aVal = (vVal - prevV) / dt
            val jVal = (aVal - prevA) / dt
            a.add(aVal)
            j.add(jVal)
            prevV = vVal
            prevA = aVal

            val angle = atan2(dy.toDouble(), dx.toDouble())
            angles.add(angle)

            if (!foundAccel && aVal > 0) {
                accelStart += dt
            } else {
                foundAccel = true
            }

            totalLength += hypot(dx.toDouble(), dy.toDouble())
            distances.add(totalLength)

            if (i > 1) {
                val dAngle = angle - prevAngle
                val dTime = max(timeList[i] - timeList[i - 1], 1e-6)
                omega.add(dAngle / dTime)
                prevAngle = angle
            }
        }

        for (i in 1 until angles.size) {
            val da = angles[i] - angles[i - 1]
            val ds = distances.getOrNull(i)?.minus(distances.getOrNull(i - 1) ?: 0.0) ?: 1.0
            curvature.add(safeDiv(da, ds))
            sumAngle += abs(da)
        }

        val p1 = points.first()
        val pn = points.last()
        val dx = pn.x - p1.x
        val dy = pn.y - p1.y
        val endToEnd = hypot(dx.toDouble(), dy.toDouble())
        val straightness = if (totalLength == 0.0) 0.0 else endToEnd / totalLength

        for (pt in points) {
            val dev = pointLineDistance(
                pt.x.toDouble(), pt.y.toDouble(),
                p1.x.toDouble(), p1.y.toDouble(),
                pn.x.toDouble(), pn.y.toDouble()
            )
            if (dev > maxDev) maxDev = dev
        }

        return FeatureVector(
            vxMean = vx.avgSafe(), vxStd = vx.stdSafe(),
            vyMean = vy.avgSafe(), vyStd = vy.stdSafe(),
            vMean = v.avgSafe(), vStd = v.stdSafe(),
            aMean = a.avgSafe(), aStd = a.stdSafe(),
            jMean = j.avgSafe(), jStd = j.stdSafe(),
            omegaMean = omega.avgSafe(), omegaStd = omega.stdSafe(),
            curvatureMean = curvature.avgSafe(), curvatureStd = curvature.stdSafe(),
            elapsedTime = pn.ctime - p1.ctime,
            trajectoryLength = totalLength,
            endToEndDist = endToEnd,
            straightness = straightness,
            numPoints = n,
            sumOfAngles = sumAngle,
            largestDeviation = maxDev,
            numSharpAngles = countSharpAngles(angles),
            accelStartTime = accelStart,
            actionType = action.type.name
        )
    }

    private fun pointLineDistance(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val numerator = abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1)
        val denominator = sqrt((y2 - y1).pow(2) + (x2 - x1).pow(2))
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    private fun safeDiv(num: Double, denom: Double): Double = if (denom == 0.0 || denom.isNaN()) 0.0 else num / denom

    private fun List<Double>.avgSafe() = if (isNotEmpty()) average() else 0.0

    private fun List<Double>.stdSafe(): Double {
        if (size < 2) return 0.0
        val mean = avgSafe()
        return sqrt(map { (it - mean).pow(2) }.avgSafe())
    }

    private fun countSharpAngles(angles: List<Double>): Int {
        var count = 0
        for (i in 1 until angles.size) {
            if (abs(angles[i] - angles[i - 1]) > PI / 3) count++
        }
        return count
    }
}
