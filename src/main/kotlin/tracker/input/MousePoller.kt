package tracker.input

import com.sun.jna.platform.win32.WinDef
import java.util.Timer
import java.util.TimerTask
import tracker.input.win.Native.user32
import tracker.data.StateSnapshot

class MousePoller {
    private val point = WinDef.POINT()
    private val titleBuf = CharArray(1024)
    private var timer: Timer? = null

    var lastMovementTime: Long = System.currentTimeMillis()
        private set
    private var lastX = 0
    private var lastY = 0

    fun start(callback: (StateSnapshot) -> Unit) {
        timer = Timer("mouse-poll", true).also {
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    user32.GetCursorPos(point)

                    // Detect movement
                    if (point.x != lastX || point.y != lastY) {
                        lastMovementTime = System.currentTimeMillis()
                        lastX = point.x
                        lastY = point.y
                    }

                    val hwnd = user32.GetForegroundWindow()
                    user32.GetWindowTextW(hwnd, titleBuf, 1024)
                    val window = titleBuf.concatToString().trimEnd('\u0000')

                    val snapshot = StateSnapshot(
                        timestamp = System.currentTimeMillis(),
                        x = point.x,
                        y = point.y,
                        left = isDown(0x01),
                        right = isDown(0x02),
                        middle = isDown(0x04),
                        windowTitle = window
                    )
                    callback(snapshot)
                }
            }, 0L, 10L)
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }

    private fun isDown(vk: Int): Boolean =
        (user32.GetAsyncKeyState(vk).toInt() and 0x8000) != 0
}
