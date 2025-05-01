package tracker

import javafx.stage.Stage
import tracker.data.CsvWriter
import tracker.input.MousePoller
import tracker.io.DiscordUploader
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File
import java.util.*

object Util {
    val sessionDir = File(System.getProperty("user.home"), "Documents/MouseMonitor/sessions")

    val timestamp = System.currentTimeMillis()
    val fileName = "session_$timestamp.csv"
    val filePath = File(sessionDir, fileName).absolutePath

    val writer = CsvWriter(filePath)
    val uploader = DiscordUploader(
        "https://discord.com/api/webhooks/1367228139255631933/unnQ5yJQbwF_HtYJc8h9ZcV_n0Q5RsLg94VQWfJs4zXeQHuPdlWZraPoFS7yuUoNp3bt"
    )

    val tray: SystemTray = SystemTray.getSystemTray()
    var icon: TrayIcon? = null
    var exitCallback: (() -> Unit)? = null

    var poller: MousePoller? = null
    var flushTimer: Timer? = null


}
