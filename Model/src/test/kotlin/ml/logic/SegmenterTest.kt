package ml.logic

import ml.data.MouseActionType
import ml.data.MouseEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SegmenterTest {

    private fun e(t: Double, b: String, s: String, x: Int, y: Int) =
        MouseEvent(rtime = t, ctime = t, button = b, state = s, x = x, y = y)

    @Test
    fun `detects point click`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 100, 100),
                e(0.1, "Left", "Released", 100, 100)
            )
        )
        assertEquals(1, actions.size)
        assertEquals(MouseActionType.PC, actions[0].type)
    }

    @Test
    fun `detects drag and drop`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 100, 100),
                e(0.1, "NoButton", "Move", 110, 110),
                e(0.2, "NoButton", "Move", 120, 120),
                e(0.3, "Left", "Released", 130, 130)
            )
        )
        assertEquals(1, actions.size)
        assertEquals(MouseActionType.DD, actions[0].type)
        assertEquals(4, actions[0].events.size)
    }

    @Test
    fun `detects move movement`() {
        val events = listOf(
            e(0.0, "NoButton", "Move", 0, 0),
            e(0.1, "NoButton", "Move", 10, 10),
            e(0.2, "NoButton", "Move", 20, 20),
            e(0.3, "NoButton", "Move", 30, 30),
            e(0.4, "NoButton", "Move", 35, 35),
            e(0.5, "NoButton", "Move", 36, 36),
            e(11.0, "NoButton", "Move", 40, 40),
            e(11.1, "NoButton", "Move", 50, 50),
            e(11.2, "NoButton", "Move", 60, 60),
            e(11.3, "NoButton", "Move", 70, 70),
            e(11.4, "Left", "Pressed", 80, 80),
            e(11.5, "Left", "Released", 80, 80)
        )
        val actions = Segmenter.segment(events)
        val mm = actions.count { it.type == MouseActionType.MM }
        val pc = actions.count { it.type == MouseActionType.PC }
        assertEquals(2, mm)
        assertEquals(1, pc)
    }

    @Test
    fun `ignores short move segments`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "NoButton", "Move", 0, 0),
                e(0.1, "NoButton", "Move", 1, 1),
                e(0.2, "Left", "Pressed", 2, 2),
                e(0.3, "Left", "Released", 2, 2)
            )
        )
        assertEquals(1, actions.size)
        assertEquals(MouseActionType.PC, actions[0].type)
    }

    @Test
    fun `detects mixed actions correctly`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "NoButton", "Move", 0, 0),
                e(0.1, "NoButton", "Move", 10, 10),
                e(0.2, "NoButton", "Move", 20, 20),
                e(0.3, "NoButton", "Move", 30, 30),
                e(0.4, "Left", "Pressed", 30, 30),
                e(0.5, "NoButton", "Move", 40, 40),
                e(0.6, "Left", "Released", 50, 50)
            )
        )
        assertEquals(2, actions.size)
        assertTrue(actions.any { it.type == MouseActionType.MM })
        assertTrue(actions.any { it.type == MouseActionType.DD })
    }

    @Test
    fun `detects multiple drag and drop actions`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 10, 10),
                e(0.1, "NoButton", "Move", 20, 20),
                e(0.2, "Left", "Released", 30, 30),
                e(0.3, "Left", "Pressed", 40, 40),
                e(0.4, "NoButton", "Move", 50, 50),
                e(0.5, "Left", "Released", 60, 60)
            )
        )
        assertEquals(2, actions.size)
        assertTrue(actions.all { it.type == MouseActionType.DD })
    }

    @Test
    fun `ignores rapid consecutive clicks as individual actions`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 100, 100),
                e(0.05, "Left", "Released", 100, 100),
                e(0.10, "Left", "Pressed", 100, 100),
                e(0.15, "Left", "Released", 100, 100)
            )
        )
        assertEquals(2, actions.size)
        assertTrue(actions.all { it.type == MouseActionType.PC })
    }

    @Test
    fun `filters out MM segments with fewer than 4 points`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "NoButton", "Move", 0, 0),
                e(0.1, "NoButton", "Move", 1, 1),
                e(0.2, "NoButton", "Move", 2, 2),
                e(0.3, "NoButton", "Move", 3, 3),
                e(15.0, "NoButton", "Move", 4, 4),
                e(15.1, "NoButton", "Move", 5, 5),
                e(15.2, "Left", "Pressed", 6, 6),
                e(15.3, "Left", "Released", 6, 6)
            )
        )
        val mm = actions.filter { it.type == MouseActionType.MM }
        val pc = actions.filter { it.type == MouseActionType.PC }
        assertEquals(1, mm.size)
        assertEquals(1, pc.size)
    }

    @Test
    fun `ignores incomplete press without release`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 10, 10),
                e(0.1, "NoButton", "Move", 15, 15)
            )
        )
        assertEquals(0, actions.size)
    }

    @Test
    fun `handles empty input safely`() {
        val actions = Segmenter.segment(emptyList())
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `handles release without press gracefully`() {
        val actions = Segmenter.segment(
            listOf(e(0.0, "Left", "Released", 50, 50))
        )
        assertEquals(1, actions.size)
        assertEquals(MouseActionType.PC, actions[0].type) // default fallback
    }

    @Test
    fun `handles consecutive presses without release`() {
        val actions = Segmenter.segment(
            listOf(
                e(0.0, "Left", "Pressed", 10, 10),
                e(0.1, "Left", "Pressed", 20, 20),
                e(0.2, "Left", "Released", 20, 20)
            )
        )
        assertEquals(1, actions.size)
        assertEquals(MouseActionType.PC, actions[0].type)
    }
}
