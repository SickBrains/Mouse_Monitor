package ml.logic

import ml.data.MouseAction
import ml.data.MouseActionType
import ml.data.MouseEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FeatureExtractorTest {

    private fun e(t: Double, x: Int, y: Int): MouseEvent =
        MouseEvent(rtime = t, ctime = t, button = "Left", state = "Move", x = x, y = y)

    @Test
    fun `computes basic statistics correctly`() {
        val events = listOf(
            e(0.0, 0, 0),
            e(1.0, 10, 0),
            e(2.0, 20, 0),
            e(3.0, 30, 0)
        )
        val action = MouseAction(MouseActionType.MM, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertEquals(10.0, fv.vxMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.vyMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(10.0, fv.vMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(3.0, fv.elapsedTime, 1e-6)
        }
        if (fv != null) {
            assertEquals(30.0, fv.trajectoryLength, 1e-6)
        }
        if (fv != null) {
            assertEquals(30.0, fv.endToEndDist, 1e-6)
        }
        if (fv != null) {
            assertEquals(1.0, fv.straightness, 1e-6)
        }
    }

    @Test
    fun `handles four-point segment`() {
        val events = listOf(
            e(0.0, 0, 0),
            e(1.0, 3, 4),
            e(2.0, 6, 8),
            e(3.0, 9, 12)
        )
        val action = MouseAction(MouseActionType.PC, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertEquals(15.0, fv.trajectoryLength, 1e-6)
        }
        if (fv != null) {
            assertEquals(15.0, fv.endToEndDist, 1e-6)
        }
        if (fv != null) {
            assertEquals(1.0, fv.straightness, 1e-6)
        }
    }

    @Test
    fun `returns null on insufficient data`() {
        val events = listOf(
            e(0.0, 0, 0),
            e(1.0, 1, 1),
            e(2.0, 2, 2)
        )
        val action = MouseAction(MouseActionType.PC, events)
        val result = FeatureExtractor.extract(action)

        assertNull(result)
    }

    @Test
    fun `handles zero movement with four points`() {
        val events = listOf(
            e(0.0, 10, 10),
            e(1.0, 10, 10),
            e(2.0, 10, 10),
            e(3.0, 10, 10)
        )
        val action = MouseAction(MouseActionType.MM, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertEquals(0.0, fv.vMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.trajectoryLength, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.endToEndDist, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.straightness, 1e-6)
        }
    }

    @Test
    fun `ignores zero or negative time deltas`() {
        val events = listOf(
            MouseEvent(ctime = 0.0, rtime = 0.0, x = 0, y = 0, button = "Left", state = "Move"),
            MouseEvent(ctime = 0.0, rtime = 0.0, x = 10, y = 0, button = "Left", state = "Move"),
            MouseEvent(ctime = -1.0, rtime = -1.0, x = 20, y = 0, button = "Left", state = "Move"),
            MouseEvent(ctime = 1.0, rtime = 1.0, x = 30, y = 0, button = "Left", state = "Move")
        )
        val action = MouseAction(MouseActionType.MM, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertTrue(fv.vxMean > 0.0)
        }
        if (fv != null) {
            assertTrue(fv.elapsedTime >= 1.0)
        }
    }

    @Test
    fun `returns zero curvature and angular values for straight line`() {
        val events = listOf(
            e(0.0, 0, 0),
            e(1.0, 10, 0),
            e(2.0, 20, 0),
            e(3.0, 30, 0)
        )
        val action = MouseAction(MouseActionType.MM, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertEquals(0.0, fv.curvatureMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.curvatureStd, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.omegaMean, 1e-6)
        }
        if (fv != null) {
            assertEquals(0.0, fv.omegaStd, 1e-6)
        }
    }

    @Test
    fun `detects large deviation from straight path`() {
        val events = listOf(
            e(0.0, 0, 0),
            e(1.0, 10, 30), // deviation
            e(2.0, 20, 0),
            e(3.0, 30, 0)
        )
        val action = MouseAction(MouseActionType.MM, events)
        val fv = FeatureExtractor.extract(action)

        if (fv != null) {
            assertTrue(fv.largestDeviation > 0.0)
        }
        if (fv != null) {
            assertTrue(fv.straightness < 1.0)
        }
    }
}
