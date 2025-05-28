package ml.logic

import ml.data.*

object Segmenter {


    fun segment(events: List<MouseEvent>): List<MouseAction> {
        val actions = mutableListOf<MouseAction>()
        val buffer = mutableListOf<MouseEvent>()

        for (i in events.indices) {
            buffer.add(events[i])

            if (isMouseRelease(events[i])) {
                val mmSegments = extractMMs(buffer.dropLast(1))
                actions.addAll(mmSegments)

                val type = classifyClickType(buffer)
                val action = MouseAction(type, buffer.toList())
                actions.add(action)

                buffer.clear()
            }
        }

        return actions.filter { it.type != MouseActionType.MM || it.events.size >= 4 }
    }

    private fun classifyClickType(buffer: List<MouseEvent>): MouseActionType {
        val pressedIndex = buffer.indexOfLast { isMousePress(it) }
        val releasedIndex = buffer.indexOfLast { isMouseRelease(it) }
        if (pressedIndex == -1 || releasedIndex == -1 || releasedIndex <= pressedIndex) return MouseActionType.PC

        val hasMoveBetween = buffer.subList(pressedIndex + 1, releasedIndex)
            .any { it.state == "Move" }

        return if (hasMoveBetween) MouseActionType.DD else MouseActionType.PC
    }

    private fun extractMMs(events: List<MouseEvent>, timeGap: Double = 10.0): List<MouseAction> {
        val segments = mutableListOf<MouseAction>()
        if (events.size < 2) return segments

        val current = mutableListOf<MouseEvent>()
        current.add(events.first())

        for (i in 1 until events.size) {
            val dt = events[i].ctime - events[i - 1].ctime
            if (dt > timeGap) {
                if (current.size >= 4)
                    segments.add(MouseAction(MouseActionType.MM, current.toList()))
                current.clear()
            }
            current.add(events[i])
        }

        if (current.size >= 4)
            segments.add(MouseAction(MouseActionType.MM, current.toList()))

        return segments
    }

    private fun isMousePress(event: MouseEvent) = event.state == "Pressed"
    private fun isMouseRelease(event: MouseEvent) = event.state == "Released"
}
