package tracker

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.stage.Stage
import tracker.data.CsvWriter
import tracker.input.MousePoller
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File
import java.io.FileWriter
import java.util.*

object Util {

    var shouldRun = true

    val sessionDir = File(System.getProperty("user.home"), "Documents/MouseMonitor/sessions")
    fun newSession() {
        sessionDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val fileName = "session_$timestamp.csv"
        val file = File(sessionDir, fileName)
        writer = CsvWriter(file.absolutePath)
        filePath = file.absolutePath
    }


    var filePath: String = ""
    lateinit var writer: CsvWriter

    init {
        newSession()
    }


    val tray: SystemTray = SystemTray.getSystemTray()
    var icon: TrayIcon? = null
    var exitCallback: (() -> Unit)? = null

    var poller: MousePoller? = null
    var flushTimer: Timer? = null


    fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.dialogPane.contentText = message
    }

    fun isWriterInitialized(): Boolean {
        return this::writer.isInitialized
    }

}
