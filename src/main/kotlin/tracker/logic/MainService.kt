package tracker.logic

import javafx.application.Platform
import javafx.scene.control.Alert
import tracker.Util
import tracker.Util.shouldRun
import tracker.Util.writer
import tracker.input.MousePoller
import tracker.ui.Tray
import java.io.File
import java.lang.Thread.sleep
import java.util.*

class MainService {

    private var poller: MousePoller? = null
    private var flushTimer: Timer? = null

    private var idleMonitor: Thread? = null
    private var movementChecker: Thread? = null

    fun startTracking(onExit: () -> Unit) {
        println("[MainService] startTracking called")

        poller?.stop()
        flushTimer?.cancel()
        idleMonitor?.interrupt()
        movementChecker?.interrupt()

        poller = null
        flushTimer = null
        idleMonitor = null
        movementChecker = null

        Tray.init {
            println("[MainService] Exit callback triggered from tray")
            stopTrackingWithGuiAlert()
            sleep(5000)
            kotlin.system.exitProcess(0)
        }

        poller = MousePoller().also {
            println("[MainService] Creating new MousePoller")
            Util.poller = it
            it.start { snapshot ->
                synchronized(Util) {
                    if (Util.isWriterInitialized()) {
                        Util.writer.writeCondensed(snapshot)
                    }
                }
            }
        }

        flushTimer = Timer().also {
            println("[MainService] Creating new flush timer")
            Util.flushTimer = it
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        Util.writer.flush()
                    } catch (e: Exception) {
                        println("[MainService] Flush failed: ${e.message}")
                    }
                }
            }, 1000, 1000)
        }

        println("[MainService] Starting idle monitor thread")
        startIdleMonitor(onExit)
    }



    private fun startIdleMonitor(onExit: () -> Unit) {
        println("[MainService] startIdleMonitor() called")
        idleMonitor = Thread {
            try {
                while (!Thread.currentThread().isInterrupted && shouldRun) {
                    Thread.sleep(15_000)
                    val idleTime = System.currentTimeMillis() - (poller?.lastMovementTime ?: 0L)
                    println("[IdleMonitor] Current idle time: $idleTime ms")

                    if (idleTime >= 60_000) {
                        println("[IdleMonitor] Mouse idle threshold reached. Ending session.")
                        stopTrackingWithoutExit(keepAppRunning = true)

                        println("[IdleMonitor] Converting CSV to Parquet...")
                        val csvFile = File(Util.filePath)
                        tracker.convert.CsvToParquetConverter.convert(csvFile)

                        Platform.runLater {
                            println("[IdleMonitor] Showing idle alert to user")
                            val alert = Alert(Alert.AlertType.INFORMATION)
                            alert.title = "Mouse Tracker"
                            alert.headerText = null
                            alert.contentText = "Session ended due to inactivity:\n${Util.filePath}"
                            alert.show() // ✅ Non-blocking

                            Thread {
                                Thread.sleep(15_000)
                                Platform.runLater {
                                    if (alert.isShowing) {
                                        alert.close()
                                    }
                                }
                            }.start()
                        }


                        println("[IdleMonitor] Launching movement checker...")
                        waitForMovementAndRestart(onExit)
                        break
                    }
                }
                println("[IdleMonitor] Loop ended (shouldRun=$shouldRun, interrupted=${Thread.currentThread().isInterrupted})")
            } catch (e: InterruptedException) {
                println("[IdleMonitor] Thread interrupted")
            }
        }.apply {
            isDaemon = true
            name = "IdleMonitor"
            start()
            println("[MainService] IdleMonitor thread started")
        }
    }

    private fun waitForMovementAndRestart(onExit: () -> Unit) {
        println("[MainService] Starting movementChecker thread")

        movementChecker = Thread {
            try {
                println("[MovementChecker] Waiting for mouse movement before restart...")

                val point = com.sun.jna.platform.win32.WinDef.POINT()
                tracker.input.win.Native.user32.GetCursorPos(point)
                var lastX = point.x
                var lastY = point.y

                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(10_000)
                    println("[MovementChecker] Checking for mouse movement...")

                    tracker.input.win.Native.user32.GetCursorPos(point)

                    if (point.x != lastX || point.y != lastY) {
                        println("[MovementChecker] Mouse moved! Restarting session.")
                        shouldRun = true
                        Util.newSession()
                        startTracking(onExit)
                        break
                    }

                    lastX = point.x
                    lastY = point.y
                    println("[MovementChecker] Still idle, will check again later")
                }
            } catch (e: InterruptedException) {
                println("[MovementChecker] Thread interrupted")
            }
        }.apply {
            isDaemon = true
            name = "MovementChecker"
            start()
            println("[MainService] MovementChecker thread started")
        }
    }

    fun stopTrackingWithGuiAlert() {
        println("[MainService] stopTrackingWithGuiAlert() called")
        stopTrackingWithoutExit()

        val csvFile = File(Util.filePath)
        tracker.convert.CsvToParquetConverter.convert(csvFile)

        Platform.runLater {
            println("[MainService] Showing GUI alert for session saved")
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Mouse Tracker"
            alert.headerText = null
            alert.contentText = "Session saved to:\n${Util.filePath}"
            alert.show()

            Thread {
                Thread.sleep(15_000)
                Platform.runLater {
                    if (alert.isShowing) {
                        alert.close()
                    }
                }
            }.start()

            Tray.updateToStopped()
        }
    }


    private fun stopTrackingWithoutExit(keepAppRunning: Boolean = false) {
        println("[MainService] stopTrackingWithoutExit() called, keepAppRunning=$keepAppRunning")

        if (!keepAppRunning) {
            println("[MainService] Setting shouldRun=false")
            shouldRun = false
        }

        println("[MainService] Stopping poller and flush timer")
        poller?.stop()
        flushTimer?.cancel()
        poller = null
        flushTimer = null

        if (!keepAppRunning) {
            println("[MainService] Interrupting background threads")
            idleMonitor?.interrupt()
            movementChecker?.interrupt()
            idleMonitor = null
            movementChecker = null
        }

        Thread.sleep(50)
        println("[MainService] Closing writer")
        Util.writer.close()
    }

}
