package tracker.data

import java.io.FileWriter

class CsvWriter(path: String) {
    private val writer = FileWriter(path)
    private var last: StateSnapshot? = null
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var repeatCount = 0
    private var writerClosed = false

    init {
        writer.write("start,end,x,y,left,right,middle,window,repeats\n")
    }

    fun writeCondensed(current: StateSnapshot) {
        if (writerClosed) return

        if (last == null) {
            last = current
            startTime = current.timestamp
            endTime = current.timestamp
            repeatCount = 1
            return
        }

        if (current.equivalentTo(last!!)) {
            endTime = current.timestamp
            repeatCount++
        } else {
            writeLast()
            last = current
            startTime = current.timestamp
            endTime = current.timestamp
            repeatCount = 1
        }
    }

    fun flush() {
        if (!writerClosed) {
            writer.flush()
        }
    }

    fun close() {
        if (writerClosed) return
        if (last != null) {
            writeLast()
        }
        writer.flush()
        writer.close()
        writerClosed = true
    }

    private fun writeLast() {
        if (writerClosed) return

        val s = last ?: return
        writer.write(
            "${startTime},${endTime}," +
                    "${s.x},${s.y}," +
                    "${b(s.left)},${b(s.right)},${b(s.middle)}," +
                    "\"${escape(s.windowTitle)}\"," +
                    "$repeatCount\n"
        )
    }

    private fun b(v: Boolean) = if (v) 1 else 0

    private fun escape(text: String): String =
        text.replace("\"", "\"\"")
}
