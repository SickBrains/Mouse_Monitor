package tracker.ui

import java.awt.*
import java.io.File
import javafx.application.Platform
import net.sf.image4j.codec.ico.ICODecoder
import tracker.Util
import tracker.Util.exitCallback
import tracker.Util.icon
import tracker.Util.showAlert
import tracker.Util.tray
import tracker.logic.MainService
import kotlin.system.exitProcess


object Tray {

    fun init(onExitComplete: () -> Unit) {
        exitCallback = onExitComplete

        if (!SystemTray.isSupported()) {
            println("SystemTray not supported")
            return
        }

        val popup = PopupMenu().apply {
            add(MenuItem("Exit").apply {
                addActionListener { handleExit() }
            })
        }

        val iconImage = loadImage("/sick_brain2.ico")
        icon = TrayIcon(iconImage, "Mouse Tracker: Recording", popup).apply {
            isImageAutoSize = true
        }
        tray.add(icon)
    }

    private fun handleExit() {
        Thread {
            MainService().stopTrackingWithGuiAlert()
        }.start()
    }

    fun updateToStopped() {
        icon?.image = loadImage("/happy_brain2.ico")
        icon?.toolTip = "Mouse Tracker: Stopped"
    }

    fun loadImage(path: String): Image {
        val stream = Tray::class.java.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        val images = ICODecoder.read(stream)
        return images.firstOrNull()
            ?: throw IllegalArgumentException("ICO file contained no images")
    }
}
