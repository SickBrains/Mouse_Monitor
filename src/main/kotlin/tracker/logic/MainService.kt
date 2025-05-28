package tracker.logic

import javafx.application.Platform
import javafx.scene.control.Alert
import tracker.Util
import tracker.Util.shouldRun
import tracker.input.MousePoller
import tracker.ui.Tray
import java.awt.TrayIcon
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class MainService {

    private var poller: MousePoller? = null
    private var flushTimer: Timer? = null

    fun startTracking(onExit: () -> Unit) {
        Tray.init(onExitComplete = onExit)

        poller = MousePoller().also {
            Util.poller = it
            it.start { snapshot -> Util.writer.writeCondensed(snapshot) }
        }

        flushTimer = Timer().also {
            Util.flushTimer = it
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    Util.writer.flush()
                }
            }, 1000, 1000)
        }
    }

    fun stopTrackingWithGuiAlert() {
        stopTracking()

        // Convert CSV to Parquet before showing alert or exiting
        val csvFile = File(Util.filePath)
        tracker.convert.CsvToParquetConverter.convert(csvFile)

        Platform.runLater {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Mouse Tracker"
            alert.headerText = null
            alert.contentText = "Session saved to:\n${Util.filePath}"
            alert.showAndWait()

            Tray.updateToStopped()
            Util.icon?.let { Util.tray.remove(it) }

            // HARD FORCE EXIT
            exitProcess(0)
        }
    }


    fun stopTracking() {
        shouldRun = false
        poller?.stop()              // 1. stop poller first
        flushTimer?.cancel()        // 2. stop timer
        Thread.sleep(50)            // 3. allow threads to wind down
        Util.writer.close()         // 4. now safe to close writer
        Util.uploader.uploadCsv(File(Util.filePath))
    }



}
