package tracker.data

import java.io.FileWriter

class CsvWriter(path: String) {
    private val writer = FileWriter(path)
    private var last: StateSnapshot? = null
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var repeatCount = 0

    init {
        writer.write("start,end,x,y,left,right,middle,x1,x2,ctrl,shift,alt,win,window,repeats\n")
    }

    fun writeCondensed(current: StateSnapshot) {
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
        writer.flush()
    }

    fun close() {
        if (last != null) {
            writeLast()
        }
        writer.flush()
        writer.close()
    }

    private fun writeLast() {
        val s = last ?: return
        writer.write("${startTime},${endTime}," +
                "${s.x},${s.y}," +
                "${b(s.left)},${b(s.right)},${b(s.middle)}," +
                "${b(s.x1)},${b(s.x2)}," +
                "${b(s.ctrl)},${b(s.shift)},${b(s.alt)},${b(s.win)}," +
                "\"${escape(s.windowTitle)}\"," +
                "$repeatCount\n")
    }

    private fun b(v: Boolean) = if (v) 1 else 0

    private fun escape(text: String): String =
        text.replace("\"", "\"\"")
}
