package tracker.logic

import javafx.application.Platform
import javafx.scene.control.Alert
import tracker.Util
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
                    try {
                        Util.writer.flush()
                    } catch (e: Exception) {
                        println("Flush failed: ${e.message}")
                    }
                }
            }, 1000, 1000)
        }
    }

    fun stopTrackingWithGuiAlert() {
        stopTracking()

        // Convert CSV to Parquet before showing alert or exiting
        val csvFile = File(Util.filePath)
        try {
            tracker.convert.CsvToParquetConverter.convert(csvFile)
        } catch (e: Exception) {
            println("Conversion failed: ${e.message}")
        }

        Platform.runLater {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Mouse Tracker"
            alert.headerText = null
            alert.contentText = "Session saved to:\n${Util.filePath}"
            alert.showAndWait()

            Tray.updateToStopped()
            Util.icon?.let { Util.tray.remove(it) }

            exitProcess(0)
        }
    }

    fun stopTracking() {
        Util.shouldRun = false

        // 1. Stop poller thread
        poller?.stop()
        Thread.sleep(50)

        // 2. Cancel flush timer and give it time to exit
        flushTimer?.cancel()
        Thread.sleep(50)

        // 3. Close writer AFTER all threads are dead
        try {
            Util.writer.close()
        } catch (e: Exception) {
            println("Writer close failed: ${e.message}")
        }

        // 4. Upload after file is safely written
        try {
            Util.uploader.uploadCsv(File(Util.filePath))
        } catch (e: Exception) {
            println("Upload failed: ${e.message}")
        }
    }
}
