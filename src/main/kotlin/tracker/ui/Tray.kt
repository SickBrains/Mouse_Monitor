package tracker.ui

import java.awt.*
import java.awt.EventQueue
import net.sf.image4j.codec.ico.ICODecoder
import tracker.Util

object Tray {

    fun init(onExitComplete: () -> Unit) {
        Util.exitCallback = onExitComplete

        if (!SystemTray.isSupported()) {
            println("[Tray] SystemTray not supported")
            return
        }

        EventQueue.invokeLater {
            Util.icon?.let {
                try {
                    Util.tray.remove(it)
                    println("[Tray] Removed old tray icon")
                } catch (e: Exception) {
                    println("[Tray] Failed to remove old tray icon: ${e.message}")
                }
                Util.icon = null
            }

            val popup = PopupMenu().apply {
                add(MenuItem("Exit").apply {
                    addActionListener { handleExit() }
                })
            }

            val iconImage = loadImage("/sick_brain2.ico")
            Util.icon = TrayIcon(iconImage, "Mouse Tracker: Recording", popup).apply {
                isImageAutoSize = true
            }

            try {
                Util.tray.add(Util.icon)
                println("[Tray] New tray icon added")
            } catch (e: Exception) {
                println("[Tray] Failed to add tray icon: ${e.message}")
            }
        }
    }

    private fun handleExit() {
        println("[Tray] Exit menu clicked")
        Util.exitCallback?.invoke()
    }

    fun updateToStopped() {
        EventQueue.invokeLater {
            Util.icon?.image = loadImage("/happy_brain2.ico")
            Util.icon?.toolTip = "Mouse Tracker: Stopped"
        }
    }

    private fun loadImage(path: String): Image {
        val stream = Tray::class.java.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        val images = ICODecoder.read(stream)
        return images.firstOrNull()
            ?: throw IllegalArgumentException("ICO file contained no images")
    }
}
