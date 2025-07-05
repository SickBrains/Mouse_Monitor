package tracker.data

import java.io.FileWriter

class CsvWriter(path: String) {

    private val writer = FileWriter(path)
    private var last: StateSnapshot? = null
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var repeatCount = 0
    private val lock = Any()
    private var closed = false

    init {
        synchronized(lock) {
            writer.write("start,end,x,y,left,right,middle,x1,x2,ctrl,shift,alt,win,window,repeats\n")
        }
    }

    fun writeCondensed(current: StateSnapshot) {
        synchronized(lock) {
            if (closed) return

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
    }

    fun flush() {
        synchronized(lock) {
            if (closed) return
            try {
                writer.flush()
            } catch (e: Exception) {
                println("Flush failed: ${e.message}")
            }
        }
    }

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true

            try {
                if (last != null) writeLast()
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                println("Close failed: ${e.message}")
            }
        }
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
