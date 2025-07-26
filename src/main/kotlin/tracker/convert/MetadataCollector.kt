package tracker.convert

import com.sun.jna.Memory
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import tracker.input.win.Native
import java.text.SimpleDateFormat
import java.util.*

object MetadataCollector {
    fun collect(): SystemMetadata {

        val screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN)
        val screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN)


        val dpi = java.awt.Toolkit.getDefaultToolkit().screenResolution

        val buffer = Memory(4)
        val ok = Native.user32.SystemParametersInfoW(0x0070, 0, buffer, 0)
        val mouseSpeed = if (ok) buffer.getInt(0) else -1

        val mouseDeviceId = "VID_0000&PID_0000"
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())

        return SystemMetadata(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            dpi = dpi,
            mouseSpeed = mouseSpeed,
            mouseDeviceId = mouseDeviceId,
            parquetVersion = "1.0",
            conversionTimestamp = timestamp
        )
    }
}
