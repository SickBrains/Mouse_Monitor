package tracker

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.stage.Stage
import tracker.data.CsvWriter
import tracker.input.MousePoller
import tracker.io.DiscordUploader
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


    val uploader = DiscordUploader(
        "https://discord.com/api/webhooks/1367228139255631933/unnQ5yJQbwF_HtYJc8h9ZcV_n0Q5RsLg94VQWfJs4zXeQHuPdlWZraPoFS7yuUoNp3bt"
    )

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
