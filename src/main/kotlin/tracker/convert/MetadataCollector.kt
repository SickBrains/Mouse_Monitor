package tracker.convert


import tracker.input.win.Native
import java.awt.Toolkit
import java.text.SimpleDateFormat
import java.util.*
import com.sun.jna.Memory



object MetadataCollector {
    fun collect(): SystemMetadata {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val dpi = Toolkit.getDefaultToolkit().screenResolution


        val buffer = Memory(4)
        val ok = Native.user32.SystemParametersInfoW(0x0070, 0, buffer, 0)

        val mouseSpeed = if (ok) buffer.getInt(0) else -1



        val mouseDeviceId = "VID_0000&PID_0000"
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())

        return SystemMetadata(
            screenWidth = screenSize.width,
            screenHeight = screenSize.height,
            dpi = dpi,
            mouseSpeed = mouseSpeed,
            mouseDeviceId = mouseDeviceId,
            parquetVersion = "1.0",
            conversionTimestamp = timestamp
        )
    }
}
