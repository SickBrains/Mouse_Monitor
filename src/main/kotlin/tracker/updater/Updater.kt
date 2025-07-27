import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Updater {

    private const val INSTALLER_URL = "https://sickbrains.nl/MouseMonitor.msi"
    private val LOCAL_VERSION_FILE =
        File(System.getenv("LOCALAPPDATA"), "MouseMonitor/version.txt")
    private val TEMP_INSTALLER =
        File(System.getProperty("java.io.tmpdir"), "MouseMonitorInstaller.msi")

    data class UpdateResult(
        val success: Boolean,
        val message: String,
        val updated: Boolean,
        val version: String
    )


    fun checkForUpdate(onStatus: (String) -> Unit): UpdateResult {
        return try {
            onStatus("Checking for updates...")

            val remoteVersion = fetchRemoteVersion()
                ?: return UpdateResult(false, "Could not get version info", false, "Unknown")

            val localVersion = LOCAL_VERSION_FILE.takeIf { it.exists() }?.readText()?.trim() ?: ""

            if (remoteVersion != localVersion) {
                onStatus("Downloading version $remoteVersion...")
                downloadInstaller()

                onStatus("Installing update...")
                val installed = runInstaller()

                return if (installed) {
                    LOCAL_VERSION_FILE.parentFile.mkdirs()
                    LOCAL_VERSION_FILE.writeText(remoteVersion)
                    UpdateResult(true, "Installed new version $remoteVersion", true, remoteVersion)
                } else {
                    UpdateResult(false, "Failed to install update", false, localVersion)
                }
            } else {
                UpdateResult(true, "Already up to date", false, localVersion)
            }
        } catch (e: Exception) {
            UpdateResult(false, "Update failed: ${e.message}", false, "Unknown")
        }
    }


    private fun fetchRemoteVersion(): String? {
        return try {
            URL("https://sickbrains.nl/version.txt")
                .openStream()
                .bufferedReader()
                .readText()
                .trim()
        } catch (e: Exception) {
            null
        }
    }


    private fun downloadInstaller() {
        val conn = URL(INSTALLER_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        conn.inputStream.use { input ->
            Files.copy(input, TEMP_INSTALLER.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun runInstaller(): Boolean {
        val process = ProcessBuilder(
            "msiexec",
            "/i", TEMP_INSTALLER.absolutePath,
            "INSTALLDIR=${System.getenv("LOCALAPPDATA")}\\MouseMonitor",
            "/quiet", "/norestart"
        ).start()

        return process.waitFor() == 0
    }

    private fun showMessage(message: String) {
        Platform.runLater {
            Alert(AlertType.INFORMATION).apply {
                title = "MouseMonitor Updater"
                headerText = null
                contentText = message
                showAndWait()
            }
        }
    }
}
