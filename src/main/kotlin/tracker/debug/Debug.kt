package tracker.debug

import javafx.application.Platform
import tracker.Util.shouldRun

object Debug {

    fun startFxStatusMonitor(intervalMillis: Long = 5000L) {
        Thread {
            while (shouldRun) {
                val isFxAlive = Platform.isFxApplicationThread()
                Platform.runLater {
                    println("[FX Monitor] FX Thread is active @ ${System.currentTimeMillis()}")
                }

                println("[FX Monitor] From current thread: $isFxAlive")
                Thread.sleep(intervalMillis)
            }
        }.apply {
            isDaemon = true
            name = "FXStatusMonitor"
            start()
        }
    }
}