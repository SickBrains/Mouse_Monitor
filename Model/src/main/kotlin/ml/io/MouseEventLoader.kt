package ml.io

import ml.data.MouseEvent
import java.io.File

object MouseEventLoader {
    fun load(filePath: String): List<MouseEvent> {
        return File(filePath).readLines()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.split(",")
                try {
                    MouseEvent(
                        rtime = parts[0].toDouble(),
                        ctime = parts[1].toDouble(),
                        button = parts[2],
                        state = parts[3],
                        x = parts[4].toInt(),
                        y = parts[5].toInt()
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    fun parseBalabitCsv(file: File): List<MouseEvent> {
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size != 6) return@mapNotNull null
                MouseEvent(
                    rtime = parts[0].toDouble(),
                    ctime = parts[1].toDouble(),
                    button = parts[2],
                    state = parts[3],
                    x = parts[4].toInt(),
                    y = parts[5].toInt()
                )
            }
    }

}
