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

    fun start(callback: (StateSnapshot) -> Unit) {
        timer = Timer("mouse-poll", true).also {
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    user32.GetCursorPos(point)

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
                        x1 = isDown(0x05),
                        x2 = isDown(0x06),
                        ctrl = isDown(0x11),
                        shift = isDown(0x10),
                        alt = isDown(0x12),
                        win = isDown(0x5B),
                        windowTitle = window
                    )
                    callback(snapshot)
                }
            }, 0L, 10L)
        }
    }

    fun stop() {
        timer?.cancel()
        Thread.sleep(20)
        timer = null
    }

    private fun isDown(vk: Int): Boolean =
        (user32.GetAsyncKeyState(vk).toInt() and 0x8000) != 0
}
